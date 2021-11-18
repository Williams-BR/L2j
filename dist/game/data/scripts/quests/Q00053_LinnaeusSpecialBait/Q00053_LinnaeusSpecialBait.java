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
package quests.Q00053_LinnaeusSpecialBait;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Linnaeus Special Bait (53)<br>
 * Original Jython script by Next and DooMita.
 * @author nonom
 */
public class Q00053_LinnaeusSpecialBait extends Quest
{
	// NPCs
	private static final int LINNAEUS = 31577;
	private static final int CRIMSON_DRAKE = 20670;
	// Items
	private static final int CRIMSON_DRAKE_HEART = 7624;
	private static final int FLAMING_FISHING_LURE = 7613;
	
	public Q00053_LinnaeusSpecialBait()
	{
		super(53);
		addStartNpc(LINNAEUS);
		addTalkId(LINNAEUS);
		addKillId(CRIMSON_DRAKE);
		registerQuestItems(CRIMSON_DRAKE_HEART);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		String htmltext = event;
		
		switch (event)
		{
			case "31577-1.htm":
			{
				qs.startQuest();
				break;
			}
			case "31577-3.htm":
			{
				if (qs.isCond(2) && (getQuestItemsCount(player, CRIMSON_DRAKE_HEART) >= 100))
				{
					giveItems(player, FLAMING_FISHING_LURE, 4);
					qs.exitQuest(false, true);
				}
				else
				{
					htmltext = "31577-5.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final PlayerInstance partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return null;
		}
		
		final QuestState qs = getQuestState(partyMember, false);
		if (getQuestItemsCount(player, CRIMSON_DRAKE_HEART) < 100)
		{
			final float chance = 33 * Config.RATE_QUEST_DROP;
			if (getRandom(100) < chance)
			{
				rewardItems(player, CRIMSON_DRAKE_HEART, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		
		if (getQuestItemsCount(player, CRIMSON_DRAKE_HEART) >= 100)
		{
			qs.setCond(2, true);
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
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
			case State.CREATED:
			{
				htmltext = (player.getLevel() > 59) ? "31577-0.htm" : "31577-0a.html";
				break;
			}
			case State.STARTED:
			{
				htmltext = (qs.isCond(1)) ? "31577-4.html" : "31577-2.html";
				break;
			}
		}
		return htmltext;
	}
}
