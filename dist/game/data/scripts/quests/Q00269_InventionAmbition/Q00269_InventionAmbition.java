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
package quests.Q00269_InventionAmbition;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;

/**
 * Invention Ambition (269)
 * @author xban1x
 */
public class Q00269_InventionAmbition extends Quest
{
	// NPC
	private static final int INVENTOR_MARU = 32486;
	// Items
	private static final int ENERGY_ORE = 10866;
	// Monsters
	private static final Map<Integer, Double> MONSTERS = new HashMap<>();
	static
	{
		MONSTERS.put(21124, 0.46); // Red Eye Barbed Bat
		MONSTERS.put(21125, 0.48); // Northern Trimden
		MONSTERS.put(21126, 0.5); // Kerope Werewolf
		MONSTERS.put(21127, 0.64); // Northern Goblin
		MONSTERS.put(21128, 0.66); // Spine Golem
		MONSTERS.put(21129, 0.68); // Kerope Werewolf Chief
		MONSTERS.put(21130, 0.76); // Northern Goblin Leader
		MONSTERS.put(21131, 0.78); // Enchanted Spine Golem
	}
	// Misc
	private static final int MIN_LEVEL = 18;
	
	public Q00269_InventionAmbition()
	{
		super(269);
		addStartNpc(INVENTOR_MARU);
		addTalkId(INVENTOR_MARU);
		addKillId(MONSTERS.keySet());
		registerQuestItems(ENERGY_ORE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "32486-03.htm":
			{
				if (player.getLevel() >= MIN_LEVEL)
				{
					htmltext = event;
				}
				break;
			}
			case "32486-04.htm":
			{
				if (player.getLevel() >= MIN_LEVEL)
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "32486-07.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "32486-08.html":
			{
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs != null)
		{
			giveItemRandomly(qs.getPlayer(), npc, ENERGY_ORE, 1, 0, MONSTERS.get(npc.getId()), true);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			htmltext = (player.getLevel() >= MIN_LEVEL) ? "32486-01.htm" : "32486-02.html";
		}
		else if (qs.isStarted())
		{
			if (hasQuestItems(player, ENERGY_ORE))
			{
				final long count = getQuestItemsCount(player, ENERGY_ORE);
				giveAdena(player, (count * 50) + (count >= 10 ? 2044 : 0), true);
				takeItems(player, ENERGY_ORE, -1);
				htmltext = "32486-06.html";
			}
			else
			{
				htmltext = "32486-05.html";
			}
		}
		return htmltext;
	}
}
