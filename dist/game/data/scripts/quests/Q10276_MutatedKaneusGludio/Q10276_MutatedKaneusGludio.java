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
package quests.Q10276_MutatedKaneusGludio;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Mutated Kaneus - Gludio (10276)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10276_MutatedKaneusGludio extends Quest
{
	// NPCs
	private static final int BATHIS = 30332;
	private static final int ROHMER = 30344;
	private static final int TOMLAN_KAMOS = 18554;
	private static final int OL_ARIOSH = 18555;
	// Items
	private static final int TISSUE_TK = 13830;
	private static final int TISSUE_OA = 13831;
	
	public Q10276_MutatedKaneusGludio()
	{
		super(10276);
		addStartNpc(BATHIS);
		addTalkId(BATHIS, ROHMER);
		addKillId(TOMLAN_KAMOS, OL_ARIOSH);
		registerQuestItems(TISSUE_TK, TISSUE_OA);
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
			case "30332-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "30344-03.htm":
			{
				giveAdena(player, 8500, true);
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
		
		final int npcId = npc.getId();
		if (killer.getParty() != null)
		{
			final List<PlayerInstance> partyMembers = new ArrayList<>();
			for (PlayerInstance member : killer.getParty().getMembers())
			{
				qs = getQuestState(member, false);
				if ((qs != null) && qs.isStarted() && (((npcId == TOMLAN_KAMOS) && !hasQuestItems(member, TISSUE_TK)) || ((npcId == OL_ARIOSH) && !hasQuestItems(member, TISSUE_OA))))
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
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case BATHIS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 17) ? "30332-01.htm" : "30332-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_TK) && hasQuestItems(player, TISSUE_OA)) ? "30332-05.htm" : "30332-04.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "30332-06.htm";
						break;
					}
				}
				break;
			}
			case ROHMER:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_TK) && hasQuestItems(player, TISSUE_OA)) ? "30344-02.htm" : "30344-01.htm";
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
		if ((npcId == TOMLAN_KAMOS) && !hasQuestItems(player, TISSUE_TK))
		{
			giveItems(player, TISSUE_TK, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if ((npcId == OL_ARIOSH) && !hasQuestItems(player, TISSUE_OA))
		{
			giveItems(player, TISSUE_OA, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
