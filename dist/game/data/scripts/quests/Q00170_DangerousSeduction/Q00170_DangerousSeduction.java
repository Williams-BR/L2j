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
package quests.Q00170_DangerousSeduction;

import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Dangerous Seduction (170)
 * @author malyelfik
 */
public class Q00170_DangerousSeduction extends Quest
{
	// NPC
	private static final int VELLIOR = 30305;
	
	// Monster
	private static final int MERKENIS = 27022;
	
	// Item
	private static final int NIGHTMARE_CRYSTAL = 1046;
	
	// Misc
	private static final int MIN_LEVEL = 21;
	
	public Q00170_DangerousSeduction()
	{
		super(170);
		addStartNpc(VELLIOR);
		addTalkId(VELLIOR);
		addKillId(MERKENIS);
		
		registerQuestItems(NIGHTMARE_CRYSTAL);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("30305-04.htm"))
		{
			qs.startQuest();
			return event;
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			qs.setCond(2, true);
			giveItems(player, NIGHTMARE_CRYSTAL, 1);
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.SEND_MY_SOUL_TO_LICH_KING_ICARUS));
		}
		return super.onKill(npc, player, isSummon);
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
				htmltext = (player.getRace() == Race.DARK_ELF) ? (player.getLevel() >= MIN_LEVEL) ? "30305-01.htm" : "30305-02.htm" : "30305-03.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "30305-05.html";
				}
				else
				{
					giveAdena(player, 102680, true);
					addExpAndSp(player, 38607, 4018);
					qs.exitQuest(false, true);
					htmltext = "30305-06.html";
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