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
package quests.Q00008_AnAdventureBegins;

import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * An Adventure Begins (8)
 * @author malyelfik
 */
public class Q00008_AnAdventureBegins extends Quest
{
	// NPCs
	private static final int JASMINE = 30134;
	private static final int ROSELYN = 30355;
	private static final int HARNE = 30144;
	// Items
	private static final int ROSELYNS_NOTE = 7573;
	private static final int SCROLL_OF_ESCAPE_GIRAN = 7559;
	private static final int MARK_OF_TRAVELER = 7570;
	// Misc
	private static final int MIN_LEVEL = 3;
	
	public Q00008_AnAdventureBegins()
	{
		super(8);
		addStartNpc(JASMINE);
		addTalkId(JASMINE, ROSELYN, HARNE);
		registerQuestItems(ROSELYNS_NOTE);
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
			case "30134-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "30134-06.html":
			{
				giveItems(player, SCROLL_OF_ESCAPE_GIRAN, 1);
				giveItems(player, MARK_OF_TRAVELER, 1);
				qs.exitQuest(false, true);
				break;
			}
			case "30355-02.html":
			{
				qs.setCond(2, true);
				giveItems(player, ROSELYNS_NOTE, 1);
				break;
			}
			case "30144-02.html":
			{
				if (!hasQuestItems(player, ROSELYNS_NOTE))
				{
					return "30144-03.html";
				}
				takeItems(player, ROSELYNS_NOTE, -1);
				qs.setCond(3, true);
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
			case JASMINE:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = ((player.getRace() == Race.DARK_ELF) && (player.getLevel() >= MIN_LEVEL)) ? "30134-02.htm" : "30134-01.html";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "30134-04.html";
						}
						else if (qs.isCond(3))
						{
							htmltext = "30134-05.html";
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
			case ROSELYN:
			{
				if (qs.isStarted())
				{
					if (qs.isCond(1))
					{
						htmltext = "30355-01.html";
					}
					else if (qs.isCond(2))
					{
						htmltext = "30355-03.html";
					}
				}
				break;
			}
			case HARNE:
			{
				if (qs.isStarted())
				{
					if (qs.isCond(2))
					{
						htmltext = "30144-01.html";
					}
					else if (qs.isCond(3))
					{
						htmltext = "30144-04.html";
					}
				}
				break;
			}
		}
		return htmltext;
	}
}