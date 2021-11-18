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
package quests.Q00167_DwarvenKinship;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Dwarven Kinship (167)
 * @author xban1x
 */
public class Q00167_DwarvenKinship extends Quest
{
	// NPCs
	private static final int NORMAN = 30210;
	private static final int HAPROCK = 30255;
	private static final int CARLON = 30350;
	// Items
	private static final int CARLONS_LETTER = 1076;
	private static final int NORMANS_LETTER = 1106;
	// Misc
	private static final int MIN_LEVEL = 15;
	
	public Q00167_DwarvenKinship()
	{
		super(167);
		addStartNpc(CARLON);
		addTalkId(CARLON, NORMAN, HAPROCK);
		registerQuestItems(CARLONS_LETTER, NORMANS_LETTER);
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
				case "30210-02.html":
				{
					if (qs.isCond(2) && hasQuestItems(player, NORMANS_LETTER))
					{
						giveAdena(player, 20000, true);
						qs.exitQuest(false, true);
						htmltext = event;
					}
					break;
				}
				case "30255-02.html":
				{
					htmltext = event;
					break;
				}
				case "30255-03.html":
				{
					if (qs.isCond(1) && hasQuestItems(player, CARLONS_LETTER))
					{
						takeItems(player, CARLONS_LETTER, -1);
						giveItems(player, NORMANS_LETTER, 1);
						giveAdena(player, 2000, true);
						qs.setCond(2);
						htmltext = event;
					}
					break;
				}
				case "30255-04.html":
				{
					if (qs.isCond(1) && hasQuestItems(player, CARLONS_LETTER))
					{
						giveAdena(player, 15000, true);
						qs.exitQuest(false, true);
						htmltext = event;
					}
					break;
				}
				case "30350-03.htm":
				{
					qs.startQuest();
					giveItems(player, CARLONS_LETTER, 1);
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
		switch (npc.getId())
		{
			case CARLON:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? "30350-02.htm" : "30350-01.htm";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1) && hasQuestItems(player, CARLONS_LETTER))
						{
							htmltext = "30350-04.html";
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
						break;
					}
				}
				break;
			}
			case HAPROCK:
			{
				if (qs.isCond(1) && hasQuestItems(player, CARLONS_LETTER))
				{
					htmltext = "30255-01.html";
				}
				else if (qs.isCond(2) && hasQuestItems(player, NORMANS_LETTER))
				{
					htmltext = "30255-05.html";
				}
				break;
			}
			case NORMAN:
			{
				if (qs.isCond(2) && hasQuestItems(player, NORMANS_LETTER))
				{
					htmltext = "30210-01.html";
				}
				break;
			}
		}
		return htmltext;
	}
}