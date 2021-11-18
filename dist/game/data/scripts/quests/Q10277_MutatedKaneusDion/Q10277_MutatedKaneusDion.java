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
package quests.Q10277_MutatedKaneusDion;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Mutated Kaneus - Dion (10277)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10277_MutatedKaneusDion extends Quest
{
	// NPCs
	private static final int LUKAS = 30071;
	private static final int MIRIEN = 30461;
	private static final int CRIMSON_HATU = 18558;
	private static final int SEER_FLOUROS = 18559;
	// Items
	private static final int TISSUE_CH = 13832;
	private static final int TISSUE_SF = 13833;
	
	public Q10277_MutatedKaneusDion()
	{
		super(10277);
		addStartNpc(LUKAS);
		addTalkId(LUKAS, MIRIEN);
		addKillId(CRIMSON_HATU, SEER_FLOUROS);
		registerQuestItems(TISSUE_CH, TISSUE_SF);
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
			case "30071-03.html":
			{
				qs.startQuest();
				break;
			}
			case "30461-03.html":
			{
				giveAdena(player, 20000, true);
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
			return super.onKill(npc, killer, isSummon);
		}
		
		final int npcId = npc.getId();
		if (killer.getParty() != null)
		{
			final List<PlayerInstance> partyMembers = new ArrayList<>();
			for (PlayerInstance member : killer.getParty().getMembers())
			{
				qs = getQuestState(member, false);
				if ((qs != null) && qs.isStarted() && (((npcId == CRIMSON_HATU) && !hasQuestItems(member, TISSUE_CH)) || ((npcId == SEER_FLOUROS) && !hasQuestItems(member, TISSUE_SF))))
				{
					partyMembers.add(member);
				}
			}
			
			if (!partyMembers.isEmpty())
			{
				rewardItem(npcId, getRandomEntry(partyMembers));
			}
		}
		else if (qs.isStarted())
		{
			rewardItem(npcId, killer);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case LUKAS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 27) ? "30071-01.htm" : "30071-00.html";
						break;
					}
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_CH) && hasQuestItems(player, TISSUE_SF)) ? "30071-05.html" : "30071-04.html";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "30071-06.html";
						break;
					}
				}
				break;
			}
			case MIRIEN:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_CH) && hasQuestItems(player, TISSUE_SF)) ? "30461-02.html" : "30461-01.html";
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
	
	/**
	 * @param npcId the ID of the killed monster
	 * @param player
	 */
	private final void rewardItem(int npcId, PlayerInstance player)
	{
		if ((npcId == CRIMSON_HATU) && !hasQuestItems(player, TISSUE_CH))
		{
			giveItems(player, TISSUE_CH, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if ((npcId == SEER_FLOUROS) && !hasQuestItems(player, TISSUE_SF))
		{
			giveItems(player, TISSUE_SF, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
