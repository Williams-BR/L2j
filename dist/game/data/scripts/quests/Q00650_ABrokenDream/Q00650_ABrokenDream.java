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
package quests.Q00650_ABrokenDream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.Util;

import quests.Q00117_TheOceanOfDistantStars.Q00117_TheOceanOfDistantStars;

/**
 * A Broken Dream (650)
 * @author netvirus
 */
public class Q00650_ABrokenDream extends Quest
{
	// Npc
	private static final int GHOST_OF_A_RAILROAD_ENGINEER = 32054;
	// Item
	private static final int REMNANTS_OF_OLD_DWARVES_DREAMS = 8514;
	// Misc
	private static final int MIN_LEVEL = 39;
	// Monsters
	private static final Map<Integer, Integer> MONSTER_DROP_CHANCES = new HashMap<>();
	static
	{
		MONSTER_DROP_CHANCES.put(22027, 575); // Forgotten Crewman
		MONSTER_DROP_CHANCES.put(22028, 515); // Vagabond of the Ruins
	}
	
	public Q00650_ABrokenDream()
	{
		super(650);
		addStartNpc(GHOST_OF_A_RAILROAD_ENGINEER);
		addTalkId(GHOST_OF_A_RAILROAD_ENGINEER);
		addKillId(MONSTER_DROP_CHANCES.keySet());
		registerQuestItems(REMNANTS_OF_OLD_DWARVES_DREAMS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "32054-03.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "32054-07.html":
			case "32054-08.html":
			{
				if (qs.isStarted())
				{
					htmltext = event;
				}
				break;
			}
			case "32054-09.html":
			{
				if (qs.isStarted())
				{
					qs.exitQuest(true, true);
					htmltext = event;
				}
				break;
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
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "32054-02.htm";
				}
				else
				{
					final QuestState q117 = player.getQuestState(Q00117_TheOceanOfDistantStars.class.getSimpleName());
					htmltext = (q117 != null) && q117.isCompleted() ? "32054-01.htm" : "32054-04.htm";
				}
				break;
			}
			case State.STARTED:
			{
				htmltext = hasQuestItems(player, REMNANTS_OF_OLD_DWARVES_DREAMS) ? "32054-05.html" : "32054-06.html";
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final List<PlayerInstance> randomList = new ArrayList<>();
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isStarted())
		{
			randomList.add(killer);
			randomList.add(killer);
		}
		
		final int monsterChance = MONSTER_DROP_CHANCES.get(npc.getId());
		if (killer.isInParty())
		{
			for (PlayerInstance member : killer.getParty().getMembers())
			{
				final QuestState qst = getQuestState(member, false);
				if ((qst != null) && qst.isStarted())
				{
					randomList.add(member);
				}
			}
		}
		
		if (!randomList.isEmpty())
		{
			final PlayerInstance player = randomList.get(getRandom(randomList.size()));
			if ((getRandom(1000) < monsterChance) && Util.checkIfInRange(Config.ALT_PARTY_RANGE, npc, player, true))
			{
				giveItems(player, REMNANTS_OF_OLD_DWARVES_DREAMS, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
}
