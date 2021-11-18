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
package quests.Q00121_PavelTheGiant;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Pavel the Giants (121)<br>
 * Original Jython script by Ethernaly.
 * @author malyelfik
 */
public class Q00121_PavelTheGiant extends Quest
{
	// NPCs
	private static final int NEWYEAR = 31961;
	private static final int YUMI = 32041;
	
	public Q00121_PavelTheGiant()
	{
		super(121);
		addStartNpc(NEWYEAR);
		addTalkId(NEWYEAR, YUMI);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		switch (event)
		{
			case "31961-02.htm":
			{
				qs.startQuest();
				break;
			}
			case "32041-02.html":
			{
				addExpAndSp(player, 346320, 26069);
				qs.exitQuest(false, true);
				break;
			}
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
			case NEWYEAR:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= 70) ? "31961-01.htm" : "31961-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = "31961-03.html";
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
			case YUMI:
			{
				if (qs.isStarted())
				{
					htmltext = "32041-01.html";
				}
				break;
			}
		}
		return htmltext;
	}
}