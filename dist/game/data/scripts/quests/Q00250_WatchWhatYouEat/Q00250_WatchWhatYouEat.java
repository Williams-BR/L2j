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
package quests.Q00250_WatchWhatYouEat;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Watch What You Eat (250)
 * @author Gnacik
 * @version 2010-08-05 Based on Freya PTS
 */
public class Q00250_WatchWhatYouEat extends Quest
{
	// NPCs
	private static final int SALLY = 32743;
	// Mobs - Items
	private static final int[][] MOBS =
	{
		{
			18864,
			15493
		},
		{
			18865,
			15494
		},
		{
			18868,
			15495
		}
	};
	
	public Q00250_WatchWhatYouEat()
	{
		super(250);
		addStartNpc(SALLY);
		addFirstTalkId(SALLY);
		addTalkId(SALLY);
		for (int[] mob : MOBS)
		{
			addKillId(mob[0]);
		}
		registerQuestItems(15493, 15494, 15495);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == SALLY)
		{
			if (event.equalsIgnoreCase("32743-03.htm"))
			{
				qs.startQuest();
			}
			else if (event.equalsIgnoreCase("32743-end.htm"))
			{
				giveAdena(player, 135661, true);
				addExpAndSp(player, 698334, 76369);
				qs.exitQuest(false, true);
			}
			else if (event.equalsIgnoreCase("32743-22.html") && qs.isCompleted())
			{
				htmltext = "32743-23.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		if (npc.getId() == SALLY)
		{
			return "32743-20.html";
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		if (qs.isStarted() && qs.isCond(1))
		{
			for (int[] mob : MOBS)
			{
				if ((npc.getId() == mob[0]) && !hasQuestItems(player, mob[1]))
				{
					giveItems(player, mob[1], 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
			if (hasQuestItems(player, MOBS[0][1]) && hasQuestItems(player, MOBS[1][1]) && hasQuestItems(player, MOBS[2][1]))
			{
				qs.setCond(2, true);
			}
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (npc.getId() == SALLY)
		{
			switch (qs.getState())
			{
				case State.CREATED:
				{
					htmltext = (player.getLevel() >= 82) ? "32743-01.htm" : "32743-00.htm";
					break;
				}
				case State.STARTED:
				{
					if (qs.isCond(1))
					{
						htmltext = "32743-04.htm";
					}
					else if (qs.isCond(2))
					{
						if (hasQuestItems(player, MOBS[0][1]) && hasQuestItems(player, MOBS[1][1]) && hasQuestItems(player, MOBS[2][1]))
						{
							htmltext = "32743-05.htm";
							for (int[] items : MOBS)
							{
								takeItems(player, items[1], -1);
							}
						}
						else
						{
							htmltext = "32743-06.htm";
						}
					}
					break;
				}
				case State.COMPLETED:
				{
					htmltext = "32743-done.htm";
					break;
				}
			}
		}
		return htmltext;
	}
}
