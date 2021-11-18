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
package quests.Q00279_TargetOfOpportunity;

import java.util.Arrays;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Target of Opportunity (279)
 * @author GKR
 */
public class Q00279_TargetOfOpportunity extends Quest
{
	// NPCs
	private static final int JERIAN = 32302;
	private static final int[] MONSTERS =
	{
		22373,
		22374,
		22375,
		22376
	};
	// Items
	private static final int[] SEAL_COMPONENTS =
	{
		15517,
		15518,
		15519,
		15520
	};
	private static final int[] SEAL_BREAKERS =
	{
		15515,
		15516
	};
	
	public Q00279_TargetOfOpportunity()
	{
		super(279);
		addStartNpc(JERIAN);
		addTalkId(JERIAN);
		addKillId(MONSTERS);
		registerQuestItems(SEAL_COMPONENTS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if ((qs == null) || (player.getLevel() < 82))
		{
			return getNoQuestMsg(player);
		}
		
		if (event.equalsIgnoreCase("32302-05.html"))
		{
			qs.startQuest();
			qs.set("progress", "1");
		}
		else if (event.equalsIgnoreCase("32302-08.html") && (qs.getInt("progress") == 1) && hasQuestItems(player, SEAL_COMPONENTS[0]) && hasQuestItems(player, SEAL_COMPONENTS[1]) && hasQuestItems(player, SEAL_COMPONENTS[2]) && hasQuestItems(player, SEAL_COMPONENTS[3]))
		{
			giveItems(player, SEAL_BREAKERS[0], 1);
			giveItems(player, SEAL_BREAKERS[1], 1);
			qs.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final PlayerInstance pl = getRandomPartyMember(player, "progress", "1");
		final int idx = Arrays.binarySearch(MONSTERS, npc.getId());
		if ((pl == null) || (idx < 0))
		{
			return null;
		}
		
		final QuestState qs = getQuestState(pl, false);
		if ((getRandom(1000) < (int) (311 * Config.RATE_QUEST_DROP)) && !hasQuestItems(player, SEAL_COMPONENTS[idx]))
		{
			giveItems(player, SEAL_COMPONENTS[idx], 1);
			if (haveAllExceptThis(player, idx))
			{
				qs.setCond(2, true);
			}
			else
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.getState() == State.CREATED)
		{
			htmltext = (player.getLevel() >= 82) ? "32302-01.htm" : "32302-02.html";
		}
		else if ((qs.getState() == State.STARTED) && (qs.getInt("progress") == 1))
		{
			htmltext = (hasQuestItems(player, SEAL_COMPONENTS[0]) && hasQuestItems(player, SEAL_COMPONENTS[1]) && hasQuestItems(player, SEAL_COMPONENTS[2]) && hasQuestItems(player, SEAL_COMPONENTS[3])) ? "32302-07.html" : "32302-06.html";
		}
		return htmltext;
	}
	
	private static boolean haveAllExceptThis(PlayerInstance player, int idx)
	{
		for (int i = 0; i < SEAL_COMPONENTS.length; i++)
		{
			if (i == idx)
			{
				continue;
			}
			
			if (!hasQuestItems(player, SEAL_COMPONENTS[i]))
			{
				return false;
			}
		}
		return true;
	}
}
