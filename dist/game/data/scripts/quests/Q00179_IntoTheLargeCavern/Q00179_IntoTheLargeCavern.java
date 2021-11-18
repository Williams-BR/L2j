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
package quests.Q00179_IntoTheLargeCavern;

import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q00178_IconicTrinity.Q00178_IconicTrinity;

/**
 * Into the Large Cavern (179)
 * @author Gnacik
 * @version 2010-10-15 Based on official server Naia
 */
public class Q00179_IntoTheLargeCavern extends Quest
{
	// NPCs
	private static final int KEKROPUS = 32138;
	private static final int MENACING_MACHINE = 32258;
	// Misc
	private static final int MIN_LEVEL = 17;
	private static final int MAX_LEVEL = 21;
	
	public Q00179_IntoTheLargeCavern()
	{
		super(179);
		addStartNpc(KEKROPUS);
		addTalkId(KEKROPUS, MENACING_MACHINE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == KEKROPUS)
		{
			if (event.equalsIgnoreCase("32138-03.html"))
			{
				qs.startQuest();
			}
		}
		else if (npc.getId() == MENACING_MACHINE)
		{
			if (event.equalsIgnoreCase("32258-08.html"))
			{
				giveItems(player, 391, 1);
				giveItems(player, 413, 1);
				qs.exitQuest(false, true);
			}
			else if (event.equalsIgnoreCase("32258-09.html"))
			{
				giveItems(player, 847, 2);
				giveItems(player, 890, 2);
				giveItems(player, 910, 1);
				qs.exitQuest(false, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (npc.getId() == KEKROPUS)
		{
			switch (qs.getState())
			{
				case State.CREATED:
				{
					if (player.getRace() != Race.KAMAEL)
					{
						htmltext = "32138-00b.html";
					}
					else
					{
						final QuestState prev = player.getQuestState(Q00178_IconicTrinity.class.getSimpleName());
						final int level = player.getLevel();
						if ((prev != null) && prev.isCompleted() && (level >= MIN_LEVEL) && (level <= MAX_LEVEL) && (player.getClassId().level() == 0))
						{
							htmltext = "32138-01.htm";
						}
						else if (level < 17)
						{
							htmltext = "32138-00.html";
						}
						else
						{
							htmltext = "32138-00c.html";
						}
					}
					break;
				}
				case State.STARTED:
				{
					if (qs.isCond(1))
					{
						htmltext = "32138-03.htm";
					}
					break;
				}
				case State.COMPLETED:
				{
					htmltext = getAlreadyCompletedMsg(player);
					break;
				}
			}
		}
		else if ((npc.getId() == MENACING_MACHINE) && (qs.getState() == State.STARTED))
		{
			htmltext = "32258-01.html";
		}
		return htmltext;
	}
}
