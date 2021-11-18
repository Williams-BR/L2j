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
package quests.Q00608_SlayTheEnemyCommanderKetra;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.Util;

/**
 * Slay the Enemy Commander! (Ketra) (608)
 * @author malyelfik
 */
public class Q00608_SlayTheEnemyCommanderKetra extends Quest
{
	// NPC
	private static final int KADUN = 31370;
	// Monster
	private static final int MOS = 25312;
	// Items
	private static final int MOS_HEAD = 7236;
	private static final int WISDOM_TOTEM = 7220;
	private static final int KETRA_ALLIANCE_FOUR = 7214;
	// Misc
	private static final int MIN_LEVEL = 75;
	
	public Q00608_SlayTheEnemyCommanderKetra()
	{
		super(608);
		addStartNpc(KADUN);
		addTalkId(KADUN);
		addKillId(MOS);
		registerQuestItems(MOS_HEAD);
	}
	
	@Override
	public void actionForEachPlayer(PlayerInstance player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && Util.checkIfInRange(Config.ALT_PARTY_RANGE, npc, player, false))
		{
			giveItems(player, MOS_HEAD, 1);
			qs.setCond(2, true);
		}
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
			case "31370-04.htm":
			{
				qs.startQuest();
				break;
			}
			case "31370-07.html":
			{
				if (hasQuestItems(player, MOS_HEAD) && qs.isCond(2))
				{
					giveItems(player, WISDOM_TOTEM, 1);
					addExpAndSp(player, 10000, 0);
					qs.exitQuest(true, true);
				}
				else
				{
					htmltext = getNoQuestMsg(player);
				}
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
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, false);
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? (hasQuestItems(player, KETRA_ALLIANCE_FOUR)) ? "31370-01.htm" : "31370-02.htm" : "31370-03.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (qs.isCond(2) && hasQuestItems(player, MOS_HEAD)) ? "31370-05.html" : "31370-06.html";
				break;
			}
		}
		return htmltext;
	}
}