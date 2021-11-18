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
package quests.Q10280_MutatedKaneusSchuttgart;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Mutated Kaneus - Schuttgart (10280)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10280_MutatedKaneusSchuttgart extends Quest
{
	// NPCs
	private static final int VISHOTSKY = 31981;
	private static final int ATRAXIA = 31972;
	private static final int VENOMOUS_STORACE = 18571;
	private static final int KEL_BILETTE = 18573;
	// Items
	private static final int TISSUE_VS = 13838;
	private static final int TISSUE_KB = 13839;
	
	public Q10280_MutatedKaneusSchuttgart()
	{
		super(10280);
		addStartNpc(VISHOTSKY);
		addTalkId(VISHOTSKY, ATRAXIA);
		addKillId(VENOMOUS_STORACE, KEL_BILETTE);
		registerQuestItems(TISSUE_VS, TISSUE_KB);
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
			case "31981-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "31972-03.htm":
			{
				giveAdena(player, 210000, true);
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
				if ((qs != null) && qs.isStarted() && (((npcId == VENOMOUS_STORACE) && !hasQuestItems(member, TISSUE_VS)) || ((npcId == KEL_BILETTE) && !hasQuestItems(member, TISSUE_KB))))
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
			case VISHOTSKY:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 57) ? "31981-01.htm" : "31981-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_VS) && hasQuestItems(player, TISSUE_KB)) ? "31981-05.htm" : "31981-04.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "31981-06.htm";
						break;
					}
				}
				break;
			}
			case ATRAXIA:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_VS) && hasQuestItems(player, TISSUE_KB)) ? "31972-02.htm" : "31972-01.htm";
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
		if ((npcId == VENOMOUS_STORACE) && !hasQuestItems(player, TISSUE_VS))
		{
			giveItems(player, TISSUE_VS, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if ((npcId == KEL_BILETTE) && !hasQuestItems(player, TISSUE_KB))
		{
			giveItems(player, TISSUE_KB, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
