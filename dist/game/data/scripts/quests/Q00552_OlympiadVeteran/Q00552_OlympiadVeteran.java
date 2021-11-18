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
package quests.Q00552_OlympiadVeteran;

import org.l2jmobius.gameserver.enums.QuestType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.olympiad.CompetitionType;
import org.l2jmobius.gameserver.model.olympiad.Participant;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Olympiad Veteran (552)
 * @author lion
 */
public class Q00552_OlympiadVeteran extends Quest
{
	// NPC
	private static final int MANAGER = 31688;
	// Items
	private static final int TEAM_EVENT_CERTIFICATE = 17241;
	private static final int CLASS_FREE_BATTLE_CERTIFICATE = 17242;
	private static final int CLASS_BATTLE_CERTIFICATE = 17243;
	private static final int OLY_CHEST = 17169;
	
	public Q00552_OlympiadVeteran()
	{
		super(552);
		addStartNpc(MANAGER);
		addTalkId(MANAGER);
		registerQuestItems(TEAM_EVENT_CERTIFICATE, CLASS_FREE_BATTLE_CERTIFICATE, CLASS_BATTLE_CERTIFICATE);
		addOlympiadMatchFinishId();
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		String htmltext = event;
		if (event.equalsIgnoreCase("31688-03.html"))
		{
			qs.startQuest();
		}
		else if (event.equalsIgnoreCase("31688-04.html"))
		{
			final long count = getQuestItemsCount(player, TEAM_EVENT_CERTIFICATE) + getQuestItemsCount(player, CLASS_FREE_BATTLE_CERTIFICATE) + getQuestItemsCount(player, CLASS_BATTLE_CERTIFICATE);
			if (count > 0)
			{
				giveItems(player, OLY_CHEST, count);
				qs.exitQuest(QuestType.DAILY, true);
			}
			else
			{
				htmltext = getNoQuestMsg(player);
			}
		}
		return htmltext;
	}
	
	@Override
	public void onOlympiadMatchFinish(Participant winner, Participant looser, CompetitionType type)
	{
		if (winner != null)
		{
			final PlayerInstance player = winner.getPlayer();
			if (player == null)
			{
				return;
			}
			
			final QuestState qs = getQuestState(player, false);
			if ((qs != null) && qs.isStarted())
			{
				int matches;
				switch (type)
				{
					case CLASSED:
					{
						matches = qs.getInt("classed") + 1;
						qs.set("classed", String.valueOf(matches));
						if ((matches == 5) && !hasQuestItems(player, CLASS_BATTLE_CERTIFICATE))
						{
							giveItems(player, CLASS_BATTLE_CERTIFICATE, 1);
						}
						break;
					}
					case NON_CLASSED:
					{
						matches = qs.getInt("nonclassed") + 1;
						qs.set("nonclassed", String.valueOf(matches));
						if ((matches == 5) && !hasQuestItems(player, CLASS_FREE_BATTLE_CERTIFICATE))
						{
							giveItems(player, CLASS_FREE_BATTLE_CERTIFICATE, 1);
						}
						break;
					}
					case TEAMS:
					{
						matches = qs.getInt("teams") + 1;
						qs.set("teams", String.valueOf(matches));
						if ((matches == 5) && !hasQuestItems(player, TEAM_EVENT_CERTIFICATE))
						{
							giveItems(player, TEAM_EVENT_CERTIFICATE, 1);
						}
						break;
					}
				}
			}
		}
		
		if (looser != null)
		{
			final PlayerInstance player = looser.getPlayer();
			if (player == null)
			{
				return;
			}
			final QuestState qs = getQuestState(player, false);
			if ((qs != null) && qs.isStarted())
			{
				int matches;
				switch (type)
				{
					case CLASSED:
					{
						matches = qs.getInt("classed") + 1;
						qs.set("classed", String.valueOf(matches));
						if (matches == 5)
						{
							giveItems(player, CLASS_BATTLE_CERTIFICATE, 1);
						}
						break;
					}
					case NON_CLASSED:
					{
						matches = qs.getInt("nonclassed") + 1;
						qs.set("nonclassed", String.valueOf(matches));
						if (matches == 5)
						{
							giveItems(player, CLASS_FREE_BATTLE_CERTIFICATE, 1);
						}
						break;
					}
					case TEAMS:
					{
						matches = qs.getInt("teams") + 1;
						qs.set("teams", String.valueOf(matches));
						if (matches == 5)
						{
							giveItems(player, TEAM_EVENT_CERTIFICATE, 1);
						}
						break;
					}
				}
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if ((player.getLevel() < 75) || !player.isNoble())
		{
			htmltext = "31688-00.htm";
		}
		else if (qs.isCreated())
		{
			htmltext = "31688-01.htm";
		}
		else if (qs.isCompleted())
		{
			if (qs.isNowAvailable())
			{
				qs.setState(State.CREATED);
				htmltext = (player.getLevel() < 75) || !player.isNoble() ? "31688-00.htm" : "31688-01.htm";
			}
			else
			{
				htmltext = "31688-05.html";
			}
		}
		else if (qs.isStarted())
		{
			final long count = getQuestItemsCount(player, TEAM_EVENT_CERTIFICATE) + getQuestItemsCount(player, CLASS_FREE_BATTLE_CERTIFICATE) + getQuestItemsCount(player, CLASS_BATTLE_CERTIFICATE);
			if (count == 3)
			{
				htmltext = "31688-04.html";
				giveItems(player, OLY_CHEST, 4);
				qs.exitQuest(QuestType.DAILY, true);
			}
			else
			{
				htmltext = "31688-s" + count + ".html";
			}
		}
		return htmltext;
	}
}
