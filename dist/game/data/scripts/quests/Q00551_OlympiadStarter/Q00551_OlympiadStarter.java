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
package quests.Q00551_OlympiadStarter;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.enums.QuestType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.olympiad.CompetitionType;
import org.l2jmobius.gameserver.model.olympiad.Participant;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Olympiad Starter (551)
 * @author Gnacik, Adry_85
 */
public class Q00551_OlympiadStarter extends Quest
{
	private static final int MANAGER = 31688;
	
	private static final int CERT_3 = 17238;
	private static final int CERT_5 = 17239;
	private static final int CERT_10 = 17240;
	
	private static final int OLY_CHEST = 17169;
	private static final int MEDAL_OF_GLORY = 21874;
	
	public Q00551_OlympiadStarter()
	{
		super(551);
		addStartNpc(MANAGER);
		addTalkId(MANAGER);
		registerQuestItems(CERT_3, CERT_5, CERT_10);
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
		final String htmltext = event;
		
		switch (event)
		{
			case "31688-03.html":
			{
				qs.startQuest();
				qs.setMemoState(1);
				qs.setMemoStateEx(1, 0);
				break;
			}
			case "31688-04.html":
			{
				if ((getQuestItemsCount(player, CERT_3) + getQuestItemsCount(player, CERT_5)) > 0)
				{
					if (hasQuestItems(player, CERT_3))
					{
						giveItems(player, OLY_CHEST, 1);
						takeItems(player, CERT_3, -1);
					}
					
					if (hasQuestItems(player, CERT_5))
					{
						giveItems(player, OLY_CHEST, 1);
						giveItems(player, MEDAL_OF_GLORY, 3);
						takeItems(player, CERT_5, -1);
					}
					
					qs.exitQuest(QuestType.DAILY, true);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onOlympiadLose(PlayerInstance loser, CompetitionType type)
	{
		if (loser != null)
		{
			final QuestState qs = getQuestState(loser, false);
			if ((qs != null) && qs.isStarted() && qs.isMemoState(1))
			{
				final int memoStateEx = qs.getMemoStateEx(1);
				if (memoStateEx == 9)
				{
					qs.setMemoStateEx(1, qs.getMemoStateEx(1) + 1);
					qs.setMemoState(2);
					qs.setCond(2, true);
					giveItems(loser, CERT_10, 1);
				}
				else if (memoStateEx < 9)
				{
					if (qs.isMemoStateEx(1, 2))
					{
						giveItems(loser, CERT_3, 1);
					}
					else if (qs.isMemoStateEx(1, 4))
					{
						giveItems(loser, CERT_5, 1);
					}
					
					qs.setMemoStateEx(1, qs.getMemoStateEx(1) + 1);
					playSound(loser, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
		}
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
			if ((qs != null) && qs.isStarted() && qs.isMemoState(1))
			{
				final int memoStateEx = qs.getMemoStateEx(1);
				if (memoStateEx == 9)
				{
					qs.setMemoStateEx(1, qs.getMemoStateEx(1) + 1);
					qs.setMemoState(2);
					qs.setCond(2, true);
					giveItems(player, CERT_10, 1);
				}
				else if (memoStateEx < 9)
				{
					if (qs.isMemoStateEx(1, 2))
					{
						giveItems(player, CERT_3, 1);
					}
					else if (qs.isMemoStateEx(1, 4))
					{
						giveItems(player, CERT_5, 1);
					}
					
					qs.setMemoStateEx(1, qs.getMemoStateEx(1) + 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
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
			if ((qs != null) && qs.isStarted() && qs.isMemoState(1))
			{
				final int memoStateEx = qs.getMemoStateEx(1);
				if (memoStateEx == 9)
				{
					qs.setMemoStateEx(1, qs.getMemoStateEx(1) + 1);
					qs.setMemoState(2);
					qs.setCond(2, true);
					giveItems(player, CERT_10, 1);
				}
				else if (memoStateEx < 9)
				{
					if (qs.isMemoStateEx(1, 2))
					{
						giveItems(player, CERT_3, 1);
					}
					else if (qs.isMemoStateEx(1, 4))
					{
						giveItems(player, CERT_5, 1);
					}
					
					qs.setMemoStateEx(1, qs.getMemoStateEx(1) + 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
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
			if (qs.isMemoState(1))
			{
				htmltext = (((getQuestItemsCount(player, CERT_3) + getQuestItemsCount(player, CERT_5) + getQuestItemsCount(player, CERT_10)) > 0) ? "31688-07.html" : "31688-06.html");
			}
			else if (qs.isMemoState(2))
			{
				giveItems(player, OLY_CHEST, 4);
				giveItems(player, MEDAL_OF_GLORY, 5);
				qs.exitQuest(QuestType.DAILY, true);
				htmltext = "31688-04.html";
			}
		}
		return htmltext;
	}
}
