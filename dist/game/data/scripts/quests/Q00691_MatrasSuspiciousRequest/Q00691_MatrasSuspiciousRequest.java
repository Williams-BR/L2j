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
package quests.Q00691_MatrasSuspiciousRequest;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Matras' Suspicious Request (691)
 * @author GKR
 */
public class Q00691_MatrasSuspiciousRequest extends Quest
{
	// NPC
	private static final int MATRAS = 32245;
	// Items
	private static final int RED_GEM = 10372;
	private static final int DYNASTY_SOUL_II = 10413;
	// Reward
	private static final Map<Integer, Integer> REWARD_CHANCES = new HashMap<>();
	static
	{
		REWARD_CHANCES.put(22363, 890);
		REWARD_CHANCES.put(22364, 261);
		REWARD_CHANCES.put(22365, 560);
		REWARD_CHANCES.put(22366, 560);
		REWARD_CHANCES.put(22367, 190);
		REWARD_CHANCES.put(22368, 129);
		REWARD_CHANCES.put(22369, 210);
		REWARD_CHANCES.put(22370, 787);
		REWARD_CHANCES.put(22371, 257);
		REWARD_CHANCES.put(22372, 656);
	}
	// Misc
	private static final int MIN_LEVEL = 76;
	
	public Q00691_MatrasSuspiciousRequest()
	{
		super(691);
		addStartNpc(MATRAS);
		addTalkId(MATRAS);
		addKillId(REWARD_CHANCES.keySet());
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
			case "32245-02.htm":
			case "32245-11.html":
			{
				htmltext = event;
				break;
			}
			case "32245-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "take_reward":
			{
				if (qs.isStarted())
				{
					final int gemsCount = qs.getInt("submitted_gems");
					if (gemsCount >= 744)
					{
						qs.set("submitted_gems", Integer.toString(gemsCount - 744));
						giveItems(player, DYNASTY_SOUL_II, 1);
						htmltext = "32245-09.html";
					}
					else
					{
						htmltext = getHtm(player, "32245-10.html").replace("%itemcount%", qs.get("submitted_gems"));
					}
				}
				break;
			}
			case "32245-08.html":
			{
				if (qs.isStarted())
				{
					final int submittedCount = qs.getInt("submitted_gems");
					final int broughtCount = (int) getQuestItemsCount(player, RED_GEM);
					final int finalCount = submittedCount + broughtCount;
					takeItems(player, RED_GEM, broughtCount);
					qs.set("submitted_gems", Integer.toString(finalCount));
					htmltext = getHtm(player, "32245-08.html").replace("%itemcount%", Integer.toString(finalCount));
				}
				break;
			}
			case "32245-12.html":
			{
				if (qs.isStarted())
				{
					giveAdena(player, (qs.getInt("submitted_gems") * 10000), true);
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
		final PlayerInstance pl = getRandomPartyMember(player, 1);
		if (pl == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		int chance = (int) (Config.RATE_QUEST_DROP * REWARD_CHANCES.get(npc.getId()));
		final int numItems = Math.max((chance / 1000), 1);
		chance = chance % 1000;
		if (getRandom(1000) <= chance)
		{
			giveItems(player, RED_GEM, numItems);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
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
			case State.CREATED:
			{
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "32245-01.htm" : "32245-03.html";
				break;
			}
			case State.STARTED:
			{
				if (hasQuestItems(player, RED_GEM))
				{
					htmltext = "32245-05.html";
				}
				else if (qs.getInt("submitted_gems") > 0)
				{
					htmltext = getHtm(player, "32245-07.html").replace("%itemcount%", qs.get("submitted_gems"));
				}
				else
				{
					htmltext = "32245-06.html";
				}
				break;
			}
		}
		return htmltext;
	}
}