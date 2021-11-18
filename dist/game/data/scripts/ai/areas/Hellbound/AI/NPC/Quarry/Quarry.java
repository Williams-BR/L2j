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
package ai.areas.Hellbound.AI.NPC.Quarry;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.QuestGuardInstance;
import org.l2jmobius.gameserver.model.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Quarry AI.
 * @author DS, GKR
 */
public class Quarry extends AbstractNpcAI
{
	// NPCs
	private static final int SLAVE = 32299;
	// Items
	protected static final ItemChanceHolder[] DROP_LIST =
	{
		new ItemChanceHolder(9628, 261), // Leonard
		new ItemChanceHolder(9630, 175), // Orichalcum
		new ItemChanceHolder(9629, 145), // Adamantine
		new ItemChanceHolder(1876, 6667), // Mithril ore
		new ItemChanceHolder(1877, 1333), // Adamantine nugget
		new ItemChanceHolder(1874, 2222), // Oriharukon ore
	};
	// Zone
	private static final int ZONE = 40107;
	// Misc
	private static final int TRUST = 50;
	
	public Quarry()
	{
		addSpawnId(SLAVE);
		addFirstTalkId(SLAVE);
		addStartNpc(SLAVE);
		addTalkId(SLAVE);
		addKillId(SLAVE);
		addEnterZoneId(ZONE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		switch (event)
		{
			case "FollowMe":
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, player);
				npc.setTarget(player);
				npc.setAutoAttackable(true);
				npc.setRHandId(9136);
				npc.setWalking();
				
				if (getQuestTimer("TIME_LIMIT", npc, null) == null)
				{
					startQuestTimer("TIME_LIMIT", 900000, npc, null); // 15 min limit for save
				}
				htmltext = "32299-02.htm";
				break;
			}
			case "TIME_LIMIT":
			{
				for (ZoneType zone : ZoneManager.getInstance().getZones(npc))
				{
					if (zone.getId() == 40108)
					{
						npc.setTarget(null);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						npc.setAutoAttackable(false);
						npc.setRHandId(0);
						npc.teleToLocation(npc.getSpawn().getLocation());
						return null;
					}
				}
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.HUN_HUNGRY);
				npc.doDie(npc);
				break;
			}
			case "DECAY":
			{
				if ((npc != null) && !npc.isDead())
				{
					if (npc.getTarget().isPlayer())
					{
						for (ItemChanceHolder item : DROP_LIST)
						{
							if (getRandom(10000) < item.getChance())
							{
								npc.dropItem((PlayerInstance) npc.getTarget(), item.getId(), (int) (item.getCount() * Config.RATE_QUEST_DROP));
								break;
							}
						}
					}
					npc.setAutoAttackable(false);
					npc.getSpawn().decreaseCount(npc);
					HellboundEngine.getInstance().updateTrust(TRUST, true);
					npc.deleteMe();
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setAutoAttackable(false);
		if (npc instanceof QuestGuardInstance)
		{
			((QuestGuardInstance) npc).setPassive(true);
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		if (HellboundEngine.getInstance().getLevel() != 5)
		{
			return "32299.htm";
		}
		return "32299-01.htm";
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		npc.setAutoAttackable(false);
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isAttackable())
		{
			final Attackable npc = (Attackable) creature;
			if ((npc.getId() == SLAVE) && !npc.isDead() && !npc.isDecayed() && (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_FOLLOW) && (HellboundEngine.getInstance().getLevel() == 5))
			{
				startQuestTimer("DECAY", 1000, npc, null);
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THANK_YOU_FOR_THE_RESCUE_IT_S_A_SMALL_GIFT);
			}
		}
		return super.onEnterZone(creature, zone);
	}
}