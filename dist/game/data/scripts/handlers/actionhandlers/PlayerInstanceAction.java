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
package handlers.actionhandlers;

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IActionHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class PlayerInstanceAction implements IActionHandler
{
	private static final int CURSED_WEAPON_VICTIM_MIN_LEVEL = 21;
	
	/**
	 * Manage actions when a player click on this PlayerInstance.<br>
	 * <br>
	 * <b><u>Actions on first click on the PlayerInstance (Select it)</u>:</b><br>
	 * <li>Set the target of the player</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li><br>
	 * <br>
	 * <b><u>Actions on second click on the PlayerInstance (Follow it/Attack it/Intercat with it)</u>:</b><br>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li>
	 * <li>If target PlayerInstance has a Private Store, notify the player AI with AI_INTENTION_INTERACT</li>
	 * <li>If target PlayerInstance is autoAttackable, notify the player AI with AI_INTENTION_ATTACK</li>
	 * <li>If target PlayerInstance is NOT autoAttackable, notify the player AI with AI_INTENTION_FOLLOW</li><br>
	 * <br>
	 * <b><u>Example of use</u>:</b><br>
	 * <li>Client packet : Action, AttackRequest</li><br>
	 * @param player The player that start an action on target PlayerInstance
	 */
	@Override
	public boolean action(PlayerInstance player, WorldObject target, boolean interact)
	{
		// Check if the PlayerInstance is confused
		if (player.isOutOfControl())
		{
			return false;
		}
		
		// Aggression target lock effect
		if (player.isLockedTarget() && (player.getLockedTarget() != target))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ATTACK_TARGET);
			return false;
		}
		
		// Check if the player already target this PlayerInstance
		if (player.getTarget() != target)
		{
			// Set the target of the player
			player.setTarget(target);
		}
		else if (interact)
		{
			// Check if this PlayerInstance has a Private Store
			final PlayerInstance targetPlayer = target.getActingPlayer();
			if (targetPlayer.getPrivateStoreType() != PrivateStoreType.NONE)
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, target);
			}
			else
			{
				// Check if this PlayerInstance is autoAttackable
				if (target.isAutoAttackable(player))
				{
					// Player with level < 21 can't attack a cursed weapon holder
					// And a cursed weapon holder can't attack players with level < 21
					if ((targetPlayer.isCursedWeaponEquipped() && (player.getLevel() < CURSED_WEAPON_VICTIM_MIN_LEVEL)) //
						|| (player.isCursedWeaponEquipped() && (targetPlayer.getLevel() < CURSED_WEAPON_VICTIM_MIN_LEVEL)))
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{
						if (GeoEngine.getInstance().canSeeTarget(player, target))
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
							player.onActionRequest();
						}
					}
				}
				else
				{
					// This Action Failed packet avoids player getting stuck when clicking three or more times
					player.sendPacket(ActionFailed.STATIC_PACKET);
					if (GeoEngine.getInstance().canSeeTarget(player, target))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.PlayerInstance;
	}
}
