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
package quests.Q00283_TheFewTheProudTheBrave;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q00261_CollectorsDream.Q00261_CollectorsDream;

/**
 * The Few, The Proud, The Brave (283)
 * @author xban1x
 */
public class Q00283_TheFewTheProudTheBrave extends Quest
{
	// NPC
	private static final int PERWAN = 32133;
	// Item
	private static final int CRIMSON_SPIDER_CLAW = 9747;
	// Monster
	private static final int CRIMSON_SPIDER = 22244;
	// Misc
	private static final int CLAW_PRICE = 45;
	private static final int BONUS = 2187;
	private static final int MIN_LEVEL = 15;
	
	public Q00283_TheFewTheProudTheBrave()
	{
		super(283);
		addKillId(CRIMSON_SPIDER);
		addStartNpc(PERWAN);
		addTalkId(PERWAN);
		registerQuestItems(CRIMSON_SPIDER_CLAW);
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
			case "32133-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32133-06.html":
			{
				htmltext = event;
				break;
			}
			case "32133-08.html":
			{
				if (hasQuestItems(player, CRIMSON_SPIDER_CLAW))
				{
					final long claws = getQuestItemsCount(player, CRIMSON_SPIDER_CLAW);
					giveAdena(player, (claws * CLAW_PRICE) + ((claws >= 10) ? BONUS : 0), true);
					takeItems(player, CRIMSON_SPIDER_CLAW, -1);
					Q00261_CollectorsDream.giveNewbieReward(player);
					htmltext = event;
				}
				else
				{
					htmltext = "32133-07.html";
				}
				break;
			}
			case "32133-09.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs != null)
		{
			giveItemRandomly(killer, npc, CRIMSON_SPIDER_CLAW, 1, 0, 0.6, true);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (talker.getLevel() >= MIN_LEVEL) ? "32133-01.htm" : "32133-02.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = hasQuestItems(talker, CRIMSON_SPIDER_CLAW) ? "32133-04.html" : "32133-05.html";
				break;
			}
		}
		return htmltext;
	}
}
