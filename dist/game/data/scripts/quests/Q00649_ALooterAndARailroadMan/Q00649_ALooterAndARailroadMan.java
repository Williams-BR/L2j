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
package quests.Q00649_ALooterAndARailroadMan;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.Util;

/**
 * A Looter And A Railroad Man (649)
 * @author netvirus
 */
public class Q00649_ALooterAndARailroadMan extends Quest
{
	// Npc
	private static final int RAILMAN_OBI = 32052;
	// Item
	private static final int THIEF_GUILD_MARK = 8099;
	// Misc
	private static final int MIN_LEVEL = 30;
	// Monsters
	private static final Map<Integer, Integer> MONSTERS = new HashMap<>();
	static
	{
		MONSTERS.put(22017, 529); // Bandit Sweeper
		MONSTERS.put(22018, 452); // Bandit Hound
		MONSTERS.put(22019, 606); // Bandit Watchman
		MONSTERS.put(22021, 615); // Bandit Undertaker
		MONSTERS.put(22022, 721); // Bandit Assassin
		MONSTERS.put(22023, 827); // Bandit Warrior
		MONSTERS.put(22024, 779); // Bandit Inspector
		MONSTERS.put(22026, 1000); // Bandit Captain
	}
	
	public Q00649_ALooterAndARailroadMan()
	{
		super(649);
		addStartNpc(RAILMAN_OBI);
		addTalkId(RAILMAN_OBI);
		addKillId(MONSTERS.keySet());
		registerQuestItems(THIEF_GUILD_MARK);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "32052-03.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "32052-06.html":
			{
				if (qs.isCond(2) && hasQuestItems(player, THIEF_GUILD_MARK))
				{
					giveAdena(player, 21698, true);
					qs.exitQuest(true, true);
					htmltext = event;
				}
			}
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "32052-01.htm" : "32052-02.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (getQuestItemsCount(player, THIEF_GUILD_MARK) == 200) ? "32052-04.html" : "32052-05.html";
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1) && Util.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, false) && (getRandom(1000) < MONSTERS.get(npc.getId())))
		{
			giveItems(killer, THIEF_GUILD_MARK, 1);
			if (getQuestItemsCount(killer, THIEF_GUILD_MARK) == 200)
			{
				qs.setCond(2, true);
			}
			else
			{
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
}
