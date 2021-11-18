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
package quests.Q10269_ToTheSeedOfDestruction;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * To the Seed of Destruction (10269)<br>
 * Original Jython script by Kerberos.
 * @author nonom
 */
public class Q10269_ToTheSeedOfDestruction extends Quest
{
	// NPCs
	private static final int KEUCEREUS = 32548;
	private static final int ALLENOS = 32526;
	// Item
	private static final int INTRODUCTION = 13812;
	
	public Q10269_ToTheSeedOfDestruction()
	{
		super(10269);
		addStartNpc(KEUCEREUS);
		addTalkId(KEUCEREUS, ALLENOS);
		registerQuestItems(INTRODUCTION);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		if (event.equals("32548-05.html"))
		{
			qs.startQuest();
			giveItems(player, INTRODUCTION, 1);
		}
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case KEUCEREUS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() < 75) ? "32548-00.html" : "32548-01.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = "32548-06.html";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "32548-0a.html";
						break;
					}
				}
				break;
			}
			case ALLENOS:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = "32526-01.html";
						giveAdena(player, 29174, true);
						addExpAndSp(player, 176121, 7671);
						qs.exitQuest(false, true);
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "32526-02.html";
						break;
					}
					default:
					{
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
