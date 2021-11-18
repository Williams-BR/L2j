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
package ai.areas.Hellbound.AI.NPC.Warpgate;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.zone.ZoneType;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;
import quests.Q00130_PathToHellbound.Q00130_PathToHellbound;
import quests.Q00133_ThatsBloodyHot.Q00133_ThatsBloodyHot;

/**
 * Warpgate teleport AI.
 * @author _DS_
 */
public class Warpgate extends AbstractNpcAI
{
	// NPCs
	private static final int[] WARPGATES =
	{
		32314,
		32315,
		32316,
		32317,
		32318,
		32319,
	};
	// Locations
	private static final Location ENTER_LOC = new Location(-11272, 236464, -3248);
	private static final Location REMOVE_LOC = new Location(-16555, 209375, -3670);
	// Item
	private static final int MAP = 9994;
	// Misc
	private static final int ZONE = 40101;
	
	public Warpgate()
	{
		addStartNpc(WARPGATES);
		addFirstTalkId(WARPGATES);
		addTalkId(WARPGATES);
		addEnterZoneId(ZONE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equals("enter"))
		{
			if (canEnter(player))
			{
				player.teleToLocation(ENTER_LOC, true);
			}
			else
			{
				return "Warpgate-03.html";
			}
		}
		else if (event.equals("TELEPORT"))
		{
			player.teleToLocation(REMOVE_LOC, true);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		return HellboundEngine.getInstance().isLocked() ? "Warpgate-01.html" : "Warpgate-02.html";
	}
	
	@Override
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final PlayerInstance player = creature.getActingPlayer();
			if (!canEnter(player) && !player.canOverrideCond(PlayerCondOverride.ZONE_CONDITIONS) && !player.isOnEvent())
			{
				startQuestTimer("TELEPORT", 1000, null, player);
			}
			else if (!player.isMinimapAllowed() && hasAtLeastOneQuestItem(player, MAP))
			{
				player.setMinimapAllowed(true);
			}
		}
		return super.onEnterZone(creature, zone);
	}
	
	private static boolean canEnter(PlayerInstance player)
	{
		if (player.isFlying())
		{
			return false;
		}
		
		if (Config.HELLBOUND_WITHOUT_QUEST)
		{
			return true;
		}
		
		final QuestState qs1 = player.getQuestState(Q00130_PathToHellbound.class.getSimpleName());
		final QuestState qs2 = player.getQuestState(Q00133_ThatsBloodyHot.class.getSimpleName());
		return (((qs1 != null) && qs1.isCompleted()) || ((qs2 != null) && qs2.isCompleted()));
	}
}