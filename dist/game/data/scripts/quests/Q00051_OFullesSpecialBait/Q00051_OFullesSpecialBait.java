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
package quests.Q00051_OFullesSpecialBait;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * O'Fulle's Special Bait (51)<br>
 * Original Jython script by Kilkenny.
 * @author nonom
 */
public class Q00051_OFullesSpecialBait extends Quest
{
	// NPCs
	private static final int OFULLE = 31572;
	private static final int FETTERED_SOUL = 20552;
	// Items
	private static final int LOST_BAIT = 7622;
	private static final int ICY_AIR_LURE = 7611;
	
	public Q00051_OFullesSpecialBait()
	{
		super(51);
		addStartNpc(OFULLE);
		addTalkId(OFULLE);
		addKillId(FETTERED_SOUL);
		registerQuestItems(LOST_BAIT);
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
			case "31572-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "31572-07.html":
			{
				if ((qs.isCond(2)) && (getQuestItemsCount(player, LOST_BAIT) >= 100))
				{
					htmltext = "31572-06.htm";
					giveItems(player, ICY_AIR_LURE, 4);
					qs.exitQuest(false, true);
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
		if (getQuestItemsCount(player, LOST_BAIT) < 100)
		{
			final float chance = 33 * Config.RATE_QUEST_DROP;
			if (getRandom(100) < chance)
			{
				rewardItems(player, LOST_BAIT, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		
		if (getQuestItemsCount(player, LOST_BAIT) >= 100)
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
				htmltext = (player.getLevel() >= 36) ? "31572-01.htm" : "31572-02.html";
				break;
			}
			case State.STARTED:
			{
				htmltext = (qs.isCond(1)) ? "31572-05.html" : "31572-04.html";
				break;
			}
		}
		return htmltext;
	}
}
