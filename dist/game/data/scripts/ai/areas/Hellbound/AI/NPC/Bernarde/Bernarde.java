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
package ai.areas.Hellbound.AI.NPC.Bernarde;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Bernarde AI.
 * @author DS
 */
public class Bernarde extends AbstractNpcAI
{
	// NPCs
	private static final int BERNARDE = 32300;
	// Misc
	private static final int NATIVE_TRANSFORM = 101;
	// Items
	private static final int HOLY_WATER = 9673;
	private static final int DARION_BADGE = 9674;
	private static final int TREASURE = 9684;
	
	public Bernarde()
	{
		addFirstTalkId(BERNARDE);
		addStartNpc(BERNARDE);
		addTalkId(BERNARDE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "HolyWater":
			{
				if ((HellboundEngine.getInstance().getLevel() == 2) //
					&& (player.getInventory().getInventoryItemCount(DARION_BADGE, -1, false) >= 5) //
					&& player.exchangeItemsById("Quest", npc, DARION_BADGE, 5, HOLY_WATER, 1, true))
				{
					return "32300-02b.htm";
				}
				return "32300-02c.htm";
			}
			case "Treasure":
			{
				if ((HellboundEngine.getInstance().getLevel() == 3) && hasQuestItems(player, TREASURE))
				{
					HellboundEngine.getInstance().updateTrust((int) (getQuestItemsCount(player, TREASURE) * 1000), true);
					takeItems(player, TREASURE, -1);
					return "32300-02d.htm";
				}
				return "32300-02e.htm";
			}
			case "rumors":
			{
				return "32300-" + HellboundEngine.getInstance().getLevel() + "r.htm";
			}
		}
		return event;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		switch (HellboundEngine.getInstance().getLevel())
		{
			case 0:
			case 1:
			{
				return isTransformed(player) ? "32300-01a.htm" : "32300-01.htm";
			}
			case 2:
			{
				return isTransformed(player) ? "32300-02.htm" : "32300-03.htm";
			}
			case 3:
			{
				return isTransformed(player) ? "32300-01c.htm" : "32300-03.htm";
			}
			case 4:
			{
				return isTransformed(player) ? "32300-01d.htm" : "32300-03.htm";
			}
			default:
			{
				return isTransformed(player) ? "32300-01f.htm" : "32300-03.htm";
			}
		}
	}
	
	private static boolean isTransformed(PlayerInstance player)
	{
		return player.isTransformed() && (player.getTransformation().getId() == NATIVE_TRANSFORM);
	}
}