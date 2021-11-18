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
package quests.Q00148_PathtoBecominganExaltedMercenary;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q00147_PathtoBecominganEliteMercenary.Q00147_PathtoBecominganEliteMercenary;

/**
 * Path to Becoming an Exalted Mercenary (148)
 * @author Gnacik
 * @version 2010-09-30 Based on official server Franz
 */
public class Q00148_PathtoBecominganExaltedMercenary extends Quest
{
	// NPCs
	private static final int[] MERC =
	{
		36481,
		36482,
		36483,
		36484,
		36485,
		36486,
		36487,
		36488,
		36489
	};
	// Items
	private static final int ELITE_CERTIFICATE = 13767;
	private static final int TOP_ELITE_CERTIFICATE = 13768;
	
	public Q00148_PathtoBecominganExaltedMercenary()
	{
		super(148);
		addStartNpc(MERC);
		addTalkId(MERC);
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
		
		if (event.equalsIgnoreCase("exalted-00b.htm"))
		{
			giveItems(player, ELITE_CERTIFICATE, 1);
		}
		else if (event.equalsIgnoreCase("exalted-03.htm"))
		{
			qs.startQuest();
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				final QuestState prev = player.getQuestState(Q00147_PathtoBecominganEliteMercenary.class.getSimpleName());
				if ((player.getClan() != null) && (player.getClan().getCastleId() > 0))
				{
					htmltext = "castle.htm";
				}
				else if (hasQuestItems(player, ELITE_CERTIFICATE))
				{
					htmltext = "exalted-01.htm";
				}
				else
				{
					if ((prev != null) && prev.isCompleted())
					{
						htmltext = "exalted-00a.htm";
					}
					else
					{
						htmltext = "exalted-00.htm";
					}
				}
				break;
			}
			case State.STARTED:
			{
				if (qs.getCond() < 4)
				{
					htmltext = "exalted-04.htm";
				}
				else if (qs.isCond(4))
				{
					takeItems(player, ELITE_CERTIFICATE, -1);
					giveItems(player, TOP_ELITE_CERTIFICATE, 1);
					qs.exitQuest(false);
					htmltext = "exalted-05.htm";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		return htmltext;
	}
}
