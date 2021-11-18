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
package quests.Q00132_MatrasCuriosity;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;

/**
 * Matras' Curiosity (132)
 * @author GKR, Gladicek
 */
public class Q00132_MatrasCuriosity extends Quest
{
	// NPCs
	private static final int MATRAS = 32245;
	private static final int DEMON_PRINCE = 25540;
	private static final int RANKU = 25542;
	// Items
	private static final int FIRE = 10521;
	private static final int WATER = 10522;
	private static final int EARTH = 10523;
	private static final int WIND = 10524;
	private static final int DARKNESS = 10525;
	private static final int DIVINITY = 10526;
	private static final int BLUEPRINT_RANKU = 9800;
	private static final int BLUEPRINT_PRINCE = 9801;
	
	public Q00132_MatrasCuriosity()
	{
		super(132);
		addStartNpc(MATRAS);
		addTalkId(MATRAS);
		addKillId(RANKU, DEMON_PRINCE);
		registerQuestItems(BLUEPRINT_RANKU, BLUEPRINT_PRINCE);
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
		if (event.equalsIgnoreCase("32245-03.htm") && (player.getLevel() >= 76) && !qs.isCompleted())
		{
			if (qs.isCreated())
			{
				qs.startQuest();
				qs.set("rewarded_prince", "1");
				qs.set("rewarded_ranku", "1");
			}
			else
			{
				htmltext = "32245-03a.htm";
			}
		}
		else if (event.equalsIgnoreCase("32245-07.htm") && qs.isCond(3) && !qs.isCompleted())
		{
			giveAdena(player, 65884, true);
			addExpAndSp(player, 50541, 5094);
			giveItems(player, FIRE, 1);
			giveItems(player, WATER, 1);
			giveItems(player, EARTH, 1);
			giveItems(player, WIND, 1);
			giveItems(player, DARKNESS, 1);
			giveItems(player, DIVINITY, 1);
			qs.exitQuest(false, true);
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		PlayerInstance pl = null;
		switch (npc.getId())
		{
			case DEMON_PRINCE:
			{
				pl = getRandomPartyMember(player, "rewarded_prince", "1");
				if (pl != null)
				{
					final QuestState qs = getQuestState(pl, false);
					giveItems(player, BLUEPRINT_PRINCE, 1);
					qs.set("rewarded_prince", "2");
					if (hasQuestItems(player, BLUEPRINT_RANKU))
					{
						qs.setCond(2, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case RANKU:
			{
				pl = getRandomPartyMember(player, "rewarded_ranku", "1");
				if (pl != null)
				{
					final QuestState qs = getQuestState(pl, false);
					giveItems(player, BLUEPRINT_RANKU, 1);
					qs.set("rewarded_ranku", "2");
					if (hasQuestItems(player, BLUEPRINT_PRINCE))
					{
						qs.setCond(2, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			htmltext = (player.getLevel() >= 76) ? "32245-01.htm" : "32245-02.htm";
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (qs.isStarted())
		{
			switch (qs.getCond())
			{
				case 1:
				case 2:
				{
					if (hasQuestItems(player, BLUEPRINT_RANKU) && hasQuestItems(player, BLUEPRINT_PRINCE))
					{
						takeItems(player, BLUEPRINT_RANKU, -1);
						takeItems(player, BLUEPRINT_PRINCE, -1);
						qs.setCond(3, true);
						htmltext = "32245-05.htm";
					}
					else
					{
						htmltext = "32245-04.htm";
					}
					break;
				}
				case 3:
				{
					htmltext = "32245-06.htm";
				}
			}
		}
		return htmltext;
	}
}
