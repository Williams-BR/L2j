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
package quests.Q10281_MutatedKaneusRune;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Mutated Kaneus - Rune (10281)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10281_MutatedKaneusRune extends Quest
{
	// NPCs
	private static final int MATHIAS = 31340;
	private static final int KAYAN = 31335;
	private static final int WHITE_ALLOSCE = 18577;
	// Item
	private static final int TISSUE_WA = 13840;
	
	public Q10281_MutatedKaneusRune()
	{
		super(10281);
		addStartNpc(MATHIAS);
		addTalkId(MATHIAS, KAYAN);
		addKillId(WHITE_ALLOSCE);
		registerQuestItems(TISSUE_WA);
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
			case "31340-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "31335-03.htm":
			{
				giveAdena(player, 360000, true);
				qs.exitQuest(false, true);
				break;
			}
		}
		return event;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		QuestState qs = getQuestState(killer, false);
		if (qs == null)
		{
			return null;
		}
		
		if (killer.getParty() != null)
		{
			final List<PlayerInstance> partyMembers = new ArrayList<>();
			for (PlayerInstance member : killer.getParty().getMembers())
			{
				qs = getQuestState(member, false);
				if ((qs != null) && qs.isStarted() && !hasQuestItems(member, TISSUE_WA))
				{
					partyMembers.add(member);
				}
			}
			
			if (!partyMembers.isEmpty())
			{
				rewardItem(getRandomEntry(partyMembers));
			}
		}
		else if (qs.isStarted() && !hasQuestItems(killer, TISSUE_WA))
		{
			rewardItem(killer);
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
			case MATHIAS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 67) ? "31340-01.htm" : "31340-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = hasQuestItems(player, TISSUE_WA) ? "31340-05.htm" : "31340-04.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "31340-06.htm";
						break;
					}
				}
				break;
			}
			case KAYAN:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = hasQuestItems(player, TISSUE_WA) ? "31335-02.htm" : "31335-01.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
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
	
	private final void rewardItem(PlayerInstance player)
	{
		giveItems(player, TISSUE_WA, 1);
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
	}
}
