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
package quests.Q00268_TracesOfEvil;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Traces of Evil (268)
 * @author xban1x
 */
public class Q00268_TracesOfEvil extends Quest
{
	// NPC
	private static final int KUNAI = 30559;
	// Item
	private static final int CONTAMINATED_KASHA_SPIDER_VENOM = 10869;
	// Monsters
	private static final int[] MONSTERS = new int[]
	{
		20474, // Kasha Spider
		20476, // Kasha Fang Spider
		20478, // Kasha Blade Spider
	};
	// Misc
	private static final int MIN_LEVEL = 15;
	
	public Q00268_TracesOfEvil()
	{
		super(268);
		addStartNpc(KUNAI);
		addTalkId(KUNAI);
		addKillId(MONSTERS);
		registerQuestItems(CONTAMINATED_KASHA_SPIDER_VENOM);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equalsIgnoreCase("30559-03.htm"))
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
			giveItems(killer, CONTAMINATED_KASHA_SPIDER_VENOM, 1);
			if (getQuestItemsCount(killer, CONTAMINATED_KASHA_SPIDER_VENOM) >= 30)
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
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "30559-02.htm" : "30559-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = (!hasQuestItems(player, CONTAMINATED_KASHA_SPIDER_VENOM)) ? "30559-04.html" : "30559-05.html";
						break;
					}
					case 2:
					{
						if (getQuestItemsCount(player, CONTAMINATED_KASHA_SPIDER_VENOM) >= 30)
						{
							giveAdena(player, 2474, true);
							addExpAndSp(player, 8738, 409);
							qs.exitQuest(true, true);
							htmltext = "30559-06.html";
						}
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
