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
package quests.Q00341_HuntingForWildBeasts;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Hunting for Wild Beasts (341)
 * @author xban1x
 */
public class Q00341_HuntingForWildBeasts extends Quest
{
	// NPCs
	private static final int PANO = 30078;
	// Monsters
	private static final Map<Integer, Integer> MONSTERS = new HashMap<>();
	static
	{
		MONSTERS.put(20203, 99);
		MONSTERS.put(20310, 87);
		MONSTERS.put(20021, 83);
		MONSTERS.put(20335, 87);
	}
	// Items
	private static final int BEAR_SKIN = 4259;
	// Misc
	private static final int MIN_LEVEL = 20;
	private static final int ADENA_COUNT = 3710;
	private static final int REQUIRED_COUNT = 20;
	
	public Q00341_HuntingForWildBeasts()
	{
		super(341);
		addStartNpc(PANO);
		addTalkId(PANO);
		addKillId(MONSTERS.keySet());
		registerQuestItems(BEAR_SKIN);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs != null)
		{
			switch (event)
			{
				case "30078-03.htm":
				{
					htmltext = event;
					break;
				}
				case "30078-04.htm":
				{
					qs.startQuest();
					htmltext = event;
					break;
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
				htmltext = player.getLevel() >= MIN_LEVEL ? "30078-01.html" : "30078-02.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(2) && (getQuestItemsCount(player, BEAR_SKIN) >= REQUIRED_COUNT))
				{
					giveAdena(player, ADENA_COUNT, true);
					qs.exitQuest(true, true);
					htmltext = "30078-05.html";
				}
				else
				{
					htmltext = "30078-06.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isPet)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			long skins = getQuestItemsCount(player, BEAR_SKIN);
			if ((skins < REQUIRED_COUNT) && (getRandom(100) < MONSTERS.get(npc.getId())))
			{
				giveItems(player, BEAR_SKIN, 1);
				if ((++skins) < REQUIRED_COUNT)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				else
				{
					qs.setCond(2, true);
				}
			}
		}
		return super.onKill(npc, player, isPet);
	}
}
