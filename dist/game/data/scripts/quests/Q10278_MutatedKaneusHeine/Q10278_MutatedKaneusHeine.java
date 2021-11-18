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
package quests.Q10278_MutatedKaneusHeine;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Mutated Kaneus - Heine (10278)<br>
 * Original Jython script by Gnacik on 2010-06-29.
 * @author nonom
 */
public class Q10278_MutatedKaneusHeine extends Quest
{
	// NPCs
	private static final int GOSTA = 30916;
	private static final int MINEVIA = 30907;
	private static final int BLADE_OTIS = 18562;
	private static final int WEIRD_BUNEI = 18564;
	// Items
	private static final int TISSUE_BO = 13834;
	private static final int TISSUE_WB = 13835;
	
	public Q10278_MutatedKaneusHeine()
	{
		super(10278);
		addStartNpc(GOSTA);
		addTalkId(GOSTA, MINEVIA);
		addKillId(BLADE_OTIS, WEIRD_BUNEI);
		registerQuestItems(TISSUE_BO, TISSUE_WB);
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
			case "30916-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "30907-03.htm":
			{
				giveAdena(player, 50000, true);
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
				if ((qs != null) && qs.isStarted() && (((npcId == BLADE_OTIS) && !hasQuestItems(member, TISSUE_BO)) || ((npcId == WEIRD_BUNEI) && !hasQuestItems(member, TISSUE_WB))))
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
			case GOSTA:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() > 37) ? "30916-01.htm" : "30916-00.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_BO) && hasQuestItems(player, TISSUE_WB)) ? "30916-05.htm" : "30916-04.htm";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "30916-06.htm";
						break;
					}
				}
				break;
			}
			case MINEVIA:
			{
				switch (qs.getState())
				{
					case State.STARTED:
					{
						htmltext = (hasQuestItems(player, TISSUE_BO) && hasQuestItems(player, TISSUE_WB)) ? "30907-02.htm" : "30907-01.htm";
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
		if ((npcId == BLADE_OTIS) && !hasQuestItems(player, TISSUE_BO))
		{
			giveItems(player, TISSUE_BO, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if ((npcId == WEIRD_BUNEI) && !hasQuestItems(player, TISSUE_WB))
		{
			giveItems(player, TISSUE_WB, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
