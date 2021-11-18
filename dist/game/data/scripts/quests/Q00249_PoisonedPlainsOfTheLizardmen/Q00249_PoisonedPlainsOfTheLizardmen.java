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
package quests.Q00249_PoisonedPlainsOfTheLizardmen;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Poisoned Plains of the Lizardmen (249)
 * @author Gnacik
 * @version 2010-08-04 Based on Freya PTS
 */
public class Q00249_PoisonedPlainsOfTheLizardmen extends Quest
{
	// NPCs
	private static final int MOUEN = 30196;
	private static final int JOHNNY = 32744;
	
	public Q00249_PoisonedPlainsOfTheLizardmen()
	{
		super(249);
		addStartNpc(MOUEN);
		addTalkId(MOUEN, JOHNNY);
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
		
		if (npc.getId() == MOUEN)
		{
			if (event.equalsIgnoreCase("30196-03.htm"))
			{
				qs.startQuest();
			}
		}
		else if ((npc.getId() == JOHNNY) && event.equalsIgnoreCase("32744-03.htm"))
		{
			giveAdena(player, 83056, true);
			addExpAndSp(player, 477496, 58743);
			qs.exitQuest(false, true);
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (npc.getId() == MOUEN)
		{
			switch (qs.getState())
			{
				case State.CREATED:
				{
					htmltext = (player.getLevel() >= 82) ? "30196-01.htm" : "30196-00.htm";
					break;
				}
				case State.STARTED:
				{
					if (qs.isCond(1))
					{
						htmltext = "30196-04.htm";
					}
					break;
				}
				case State.COMPLETED:
				{
					htmltext = "30196-05.htm";
					break;
				}
			}
		}
		else if (npc.getId() == JOHNNY)
		{
			if (qs.isCond(1))
			{
				htmltext = "32744-01.htm";
			}
			else if (qs.isCompleted())
			{
				htmltext = "32744-04.htm";
			}
		}
		return htmltext;
	}
}
