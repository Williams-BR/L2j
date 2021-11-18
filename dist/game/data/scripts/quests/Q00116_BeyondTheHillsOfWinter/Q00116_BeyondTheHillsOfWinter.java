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
package quests.Q00116_BeyondTheHillsOfWinter;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Beyond the Hills of Winter (116)
 * @author Adry_85
 */
public class Q00116_BeyondTheHillsOfWinter extends Quest
{
	// NPCs
	private static final int FILAUR = 30535;
	private static final int OBI = 32052;
	// Items
	private static final ItemHolder THIEF_KEY = new ItemHolder(1661, 10);
	private static final ItemHolder BANDAGE = new ItemHolder(1833, 20);
	private static final ItemHolder ENERGY_STONE = new ItemHolder(5589, 5);
	private static final int SUPPLYING_GOODS = 8098;
	// Reward
	private static final int SOULSHOT_D = 1463;
	// Misc
	private static final int MIN_LEVEL = 30;
	
	public Q00116_BeyondTheHillsOfWinter()
	{
		super(116);
		addStartNpc(FILAUR);
		addTalkId(FILAUR, OBI);
		registerQuestItems(SUPPLYING_GOODS);
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
			case "30535-02.htm":
			{
				qs.startQuest();
				qs.setMemoState(1);
				htmltext = event;
				break;
			}
			case "30535-05.html":
			{
				if (qs.isMemoState(1))
				{
					qs.setMemoState(2);
					qs.setCond(2, true);
					giveItems(player, SUPPLYING_GOODS, 1);
					htmltext = event;
				}
				break;
			}
			case "32052-02.html":
			{
				if (qs.isMemoState(2))
				{
					htmltext = event;
				}
				break;
			}
			case "MATERIAL":
			{
				if (qs.isMemoState(2))
				{
					rewardItems(player, SOULSHOT_D, 1740);
					addExpAndSp(player, 82792, 4981);
					qs.exitQuest(false, true);
					htmltext = "32052-03.html";
				}
				break;
			}
			case "ADENA":
			{
				if (qs.isMemoState(2))
				{
					giveAdena(player, 17387, true);
					addExpAndSp(player, 82792, 4981);
					qs.exitQuest(false, true);
					htmltext = "32052-03.html";
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
			case State.COMPLETED:
			{
				if (npc.getId() == FILAUR)
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
			case State.CREATED:
			{
				if (npc.getId() == FILAUR)
				{
					htmltext = (player.getLevel() >= MIN_LEVEL) ? "30535-01.htm" : "30535-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case FILAUR:
					{
						if (qs.isMemoState(1))
						{
							htmltext = (hasAllItems(player, true, THIEF_KEY, BANDAGE, ENERGY_STONE)) ? "30535-04.html" : "30535-06.html";
						}
						else if (qs.isMemoState(2))
						{
							htmltext = "30535-07.html";
						}
						break;
					}
					case OBI:
					{
						if (qs.isMemoState(2) && hasQuestItems(player, SUPPLYING_GOODS))
						{
							htmltext = "32052-01.html";
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
