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
package quests.Q00155_FindSirWindawood;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Find Sir Windawood (155)
 * @author malyelfik
 */
public class Q00155_FindSirWindawood extends Quest
{
	// NPCs
	private static final int ABELLOS = 30042;
	private static final int SIR_COLLIN_WINDAWOOD = 30311;
	// Items
	private static final int OFFICIAL_LETTER = 1019;
	private static final int HASTE_POTION = 734;
	// Misc
	private static final int MIN_LEVEL = 3;
	
	public Q00155_FindSirWindawood()
	{
		super(155);
		addStartNpc(ABELLOS);
		addTalkId(ABELLOS, SIR_COLLIN_WINDAWOOD);
		registerQuestItems(OFFICIAL_LETTER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equalsIgnoreCase("30042-03.htm"))
		{
			qs.startQuest();
			giveItems(player, OFFICIAL_LETTER, 1);
			return event;
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case ABELLOS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? "30042-02.htm" : "30042-01.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = "30042-04.html";
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
			case SIR_COLLIN_WINDAWOOD:
			{
				if (qs.isStarted() && hasQuestItems(player, OFFICIAL_LETTER))
				{
					giveItems(player, HASTE_POTION, 1);
					qs.exitQuest(false, true);
					htmltext = "30311-01.html";
				}
				break;
			}
		}
		return htmltext;
	}
}