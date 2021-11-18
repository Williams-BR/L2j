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
package quests.Q00164_BloodFiend;

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
 * Blood Fiend (164)
 * @author xban1x
 */
public class Q00164_BloodFiend extends Quest
{
	// NPC
	private static final int CREAMEES = 30149;
	// Monster
	private static final int KIRUNAK = 27021;
	// Item
	private static final int KIRUNAK_SKULL = 1044;
	// Misc
	private static final int MIN_LEVEL = 21;
	
	public Q00164_BloodFiend()
	{
		super(164);
		addStartNpc(CREAMEES);
		addTalkId(CREAMEES);
		addKillId(KIRUNAK);
		registerQuestItems(KIRUNAK_SKULL);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equals("30149-04.htm"))
		{
			qs.startQuest();
			return event;
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1))
		{
			npc.broadcastPacket(new NpcSay(npc, ChatType.GENERAL, NpcStringId.I_HAVE_FULFILLED_MY_CONTRACT_WITH_TRADER_CREAMEES));
			giveItems(killer, KIRUNAK_SKULL, 1);
			qs.setCond(2, true);
		}
		return super.onKill(npc, killer, isSummon);
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
				htmltext = (player.getRace() != Race.DARK_ELF) ? player.getLevel() >= MIN_LEVEL ? "30149-03.htm" : "30149-02.htm" : "30149-00.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(2) && hasQuestItems(player, KIRUNAK_SKULL))
				{
					giveAdena(player, 42130, true);
					addExpAndSp(player, 35637, 1854);
					qs.exitQuest(false, true);
					htmltext = "30149-06.html";
				}
				else
				{
					htmltext = "30149-05.html";
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
