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
package quests.Q00009_IntoTheCityOfHumans;

import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Into the City of Humans (9)
 * @author malyelfik
 */
public class Q00009_IntoTheCityOfHumans extends Quest
{
	// NPCs
	private static final int PETUKAI = 30583;
	private static final int TANAPI = 30571;
	private static final int TAMIL = 30576;
	// Items
	private static final int SCROLL_OF_ESCAPE_GIRAN = 7559;
	private static final int MARK_OF_TRAVELER = 7570;
	// Misc
	private static final int MIN_LEVEL = 3;
	
	public Q00009_IntoTheCityOfHumans()
	{
		super(9);
		addStartNpc(PETUKAI);
		addTalkId(PETUKAI, TANAPI, TAMIL);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "30583-04.htm":
			{
				qs.startQuest();
				break;
			}
			case "30576-02.html":
			{
				giveItems(player, MARK_OF_TRAVELER, 1);
				giveItems(player, SCROLL_OF_ESCAPE_GIRAN, 1);
				qs.exitQuest(false, true);
				break;
			}
			case "30571-02.html":
			{
				qs.setCond(2, true);
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case PETUKAI:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? (player.getRace() == Race.ORC) ? "30583-01.htm" : "30583-02.html" : "30583-03.html";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "30583-05.html";
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
						break;
					}
				}
				break;
			}
			case TANAPI:
			{
				if (qs.isStarted())
				{
					htmltext = (qs.isCond(1)) ? "30571-01.html" : "30571-03.html";
				}
				break;
			}
			case TAMIL:
			{
				if (qs.isStarted() && qs.isCond(2))
				{
					htmltext = "30576-01.html";
				}
				break;
			}
		}
		return htmltext;
	}
}