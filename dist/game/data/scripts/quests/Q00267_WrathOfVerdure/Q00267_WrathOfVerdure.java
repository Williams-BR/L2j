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
package quests.Q00267_WrathOfVerdure;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Wrath of Verdure (267)
 * @author xban1x
 */
public class Q00267_WrathOfVerdure extends Quest
{
	// NPC
	private static final int TREANT_BREMEC = 31853;
	// Item
	private static final int GOBLIN_CLUB = 1335;
	// Monster
	private static final int GOBLIN_RAIDER = 20325;
	// Reward
	private static final int SILVERY_LEAF = 1340;
	// Misc
	private static final int MIN_LEVEL = 4;
	
	public Q00267_WrathOfVerdure()
	{
		super(267);
		addStartNpc(TREANT_BREMEC);
		addTalkId(TREANT_BREMEC);
		addKillId(GOBLIN_RAIDER);
		registerQuestItems(GOBLIN_CLUB);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs != null)
		{
			switch (event)
			{
				case "31853-04.htm":
				{
					qs.startQuest();
					htmltext = event;
					break;
				}
				case "31853-07.html":
				{
					qs.exitQuest(true, true);
					htmltext = event;
					break;
				}
				case "31853-08.html":
				{
					htmltext = event;
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && (getRandom(10) < 5))
		{
			giveItems(killer, GOBLIN_CLUB, 1);
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getRace() == Race.ELF) ? (player.getLevel() >= MIN_LEVEL) ? "31853-03.htm" : "31853-02.htm" : "31853-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (hasQuestItems(player, GOBLIN_CLUB))
				{
					final long count = getQuestItemsCount(player, GOBLIN_CLUB);
					rewardItems(player, SILVERY_LEAF, count);
					if (count >= 10)
					{
						giveAdena(player, 600, true);
					}
					takeItems(player, GOBLIN_CLUB, -1);
					htmltext = "31853-06.html";
				}
				else
				{
					htmltext = "31853-05.html";
				}
				break;
			}
		}
		return htmltext;
	}
}
