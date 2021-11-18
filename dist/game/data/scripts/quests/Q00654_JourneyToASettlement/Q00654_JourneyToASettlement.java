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
package quests.Q00654_JourneyToASettlement;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;

import quests.Q00119_LastImperialPrince.Q00119_LastImperialPrince;

/**
 * Journey to a Settlement (654)
 * @author Adry_85
 */
public class Q00654_JourneyToASettlement extends Quest
{
	// NPC
	private static final int NAMELESS_SPIRIT = 31453;
	// Items
	private static final int ANTELOPE_SKIN = 8072;
	private static final int FRINTEZZAS_SCROLL = 8073;
	// Misc
	private static final int MIN_LEVEL = 74;
	
	private static final Map<Integer, Double> MOBS_SKIN = new HashMap<>();
	static
	{
		MOBS_SKIN.put(21294, 0.840); // Canyon Antelope
		MOBS_SKIN.put(21295, 0.893); // Canyon Antelope Slave
	}
	
	public Q00654_JourneyToASettlement()
	{
		super(654);
		addStartNpc(NAMELESS_SPIRIT);
		addTalkId(NAMELESS_SPIRIT);
		addKillId(MOBS_SKIN.keySet());
		registerQuestItems(ANTELOPE_SKIN);
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
			case "31453-02.htm":
			{
				qs.startQuest();
				qs.setMemoState(1);
				htmltext = event;
				break;
			}
			case "31453-03.html":
			{
				if (qs.isMemoState(1))
				{
					qs.setMemoState(2);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "31453-07.html":
			{
				if (qs.isMemoState(2) && hasQuestItems(player, ANTELOPE_SKIN))
				{
					giveItems(player, FRINTEZZAS_SCROLL, 1);
					qs.exitQuest(true, true);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(player, 2, 3, npc);
		if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, ANTELOPE_SKIN, 1, 1, MOBS_SKIN.get(npc.getId()), true))
		{
			qs.setCond(3);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			qs = player.getQuestState(Q00119_LastImperialPrince.class.getSimpleName());
			htmltext = ((player.getLevel() >= MIN_LEVEL) && (qs != null) && (qs.isCompleted())) ? "31453-01.htm" : "31453-04.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isMemoState(1))
			{
				qs.setMemoState(2);
				qs.setCond(2, true);
				htmltext = "31453-03.html";
			}
			else if (qs.isMemoState(2))
			{
				htmltext = (hasQuestItems(player, ANTELOPE_SKIN) ? "31453-06.html" : "31453-05.html");
			}
		}
		return htmltext;
	}
}
