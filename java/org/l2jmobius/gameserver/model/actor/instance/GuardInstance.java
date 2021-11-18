/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * This class manages all Guards in the world. It inherits all methods from Attackable and adds some more such as tracking PK and aggressive MonsterInstance.
 */
public class GuardInstance extends Attackable
{
	/**
	 * Creates a guard.
	 * @param template the guard NPC template
	 */
	public GuardInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.GuardInstance);
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isMonster() && !attacker.isFakePlayer())
		{
			return true;
		}
		if (Config.FACTION_SYSTEM_ENABLED && Config.FACTION_GUARDS_ENABLED && attacker.isPlayable())
		{
			final PlayerInstance player = attacker.getActingPlayer();
			if ((player.isGood() && getTemplate().isClan(Config.FACTION_EVIL_TEAM_NAME)) || (player.isEvil() && getTemplate().isClan(Config.FACTION_GOOD_TEAM_NAME)))
			{
				return true;
			}
		}
		return super.isAutoAttackable(attacker);
	}
	
	@Override
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		super.addDamage(attacker, damage, skill);
		getAI().startFollow(attacker);
		addDamageHate(attacker, 0, 10);
		World.getInstance().forEachVisibleObjectInRange(this, GuardInstance.class, 500, guard ->
		{
			guard.getAI().startFollow(attacker);
			guard.addDamageHate(attacker, 0, 10);
		});
	}
	
	/**
	 * Set the home location of its GuardInstance.
	 */
	@Override
	public void onSpawn()
	{
		setRandomWalking(false);
		super.onSpawn();
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		// check the region where this mob is, do not activate the AI if region is inactive.
		// final WorldRegion region = World.getInstance().getRegion(this);
		// if ((region != null) && (!region.isActive()))
		// {
		// getAI().stopAITask();
		// }
	}
	
	/**
	 * Return the pathfile of the selected HTML file in function of the GuardInstance Identifier and of the page number.<br>
	 * <br>
	 * <b><u>Format of the pathfile</u>:</b>
	 * <ul>
	 * <li>if page number = 0 : <b>data/html/guard/12006.htm</b> (npcId-page number)</li>
	 * <li>if page number > 0 : <b>data/html/guard/12006-1.htm</b> (npcId-page number)</li>
	 * </ul>
	 * @param npcId The Identifier of the NpcInstance whose text must be display
	 * @param value The number of the page to display
	 */
	@Override
	public String getHtmlPath(int npcId, int value)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		return "data/html/guard/" + pom + ".htm";
	}
	
	/**
	 * Manage actions when a player click on the GuardInstance.<br>
	 * <br>
	 * <b><u>Actions on first click on the GuardInstance (Select it)</u>:</b>
	 * <ul>
	 * <li>Set the GuardInstance as target of the PlayerInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the PlayerInstance player (display the select window)</li>
	 * <li>Set the PlayerInstance Intention to AI_INTENTION_IDLE</li>
	 * <li>Send a Server->Client packet ValidateLocation to correct the GuardInstance position and heading on the client</li>
	 * </ul>
	 * <br>
	 * <b><u>Actions on second click on the GuardInstance (Attack it/Interact with it)</u>:</b>
	 * <ul>
	 * <li>If PlayerInstance is in the _aggroList of the GuardInstance, set the PlayerInstance Intention to AI_INTENTION_ATTACK</li>
	 * <li>If PlayerInstance is NOT in the _aggroList of the GuardInstance, set the PlayerInstance Intention to AI_INTENTION_INTERACT (after a distance verification) and show message</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Client packet : Action, AttackRequest</li>
	 * </ul>
	 * @param player The PlayerInstance that start an action on the GuardInstance
	 */
	@Override
	public void onAction(PlayerInstance player, boolean interactValue)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		boolean interact = interactValue;
		if (Config.FACTION_SYSTEM_ENABLED && Config.FACTION_GUARDS_ENABLED && ((player.isGood() && getTemplate().isClan(Config.FACTION_EVIL_TEAM_NAME)) || (player.isEvil() && getTemplate().isClan(Config.FACTION_GOOD_TEAM_NAME))))
		{
			interact = false;
			// TODO: Fix normal targeting
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
		}
		
		if (isFakePlayer() && isInCombat())
		{
			interact = false;
			// TODO: Fix normal targeting
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
		}
		
		// Check if the PlayerInstance already target the GuardInstance
		if (getObjectId() != player.getTargetId())
		{
			// Set the target of the PlayerInstance player
			player.setTarget(this);
		}
		else if (interact)
		{
			// Check if the PlayerInstance is in the _aggroList of the GuardInstance
			if (isInAggroList(player))
			{
				// Set the PlayerInstance Intention to AI_INTENTION_ATTACK
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				// Calculate the distance between the PlayerInstance and the NpcInstance
				if (!canInteract(player))
				{
					// Set the PlayerInstance Intention to AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					player.setLastFolkNPC(this);
					
					// Open a chat window on client with the text of the GuardInstance
					if (hasListener(EventType.ON_NPC_QUEST_START))
					{
						player.setLastQuestNpcObject(getObjectId());
					}
					
					if (hasListener(EventType.ON_NPC_FIRST_TALK))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnNpcFirstTalk(this, player), this);
					}
					else
					{
						showChatWindow(player, 0);
					}
				}
			}
		}
		
		// Send a Server->Client ActionFailed to the PlayerInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
