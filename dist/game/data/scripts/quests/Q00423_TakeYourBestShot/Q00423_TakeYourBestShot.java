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
package quests.Q00423_TakeYourBestShot;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q00249_PoisonedPlainsOfTheLizardmen.Q00249_PoisonedPlainsOfTheLizardmen;

/**
 * Take Your Best Shot (423)
 * @author Gnacik
 * @version 2010-06-26 Based on official server Franz
 */
public class Q00423_TakeYourBestShot extends Quest
{
	// NPCs
	private static final int BATRACOS = 32740;
	private static final int JOHNNY = 32744;
	
	// Monster
	private static final int TANTA_GUARD = 18862;
	
	// Item
	private static final int SEER_UGOROS_PASS = 15496;
	
	// Misc
	private static final int MIN_LEVEL = 82;
	
	public Q00423_TakeYourBestShot()
	{
		super(423);
		addStartNpc(JOHNNY, BATRACOS);
		addTalkId(JOHNNY, BATRACOS);
		addFirstTalkId(BATRACOS);
		addKillId(TANTA_GUARD);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "32740.html":
			case "32740-01.html":
			case "32744-02.html":
			case "32744-03.htm":
			{
				break;
			}
			case "32744-04.htm":
			{
				qs.startQuest();
				break;
			}
			case "32744-quit.html":
			{
				qs.exitQuest(true);
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		if (npc.isInsideRadius2D(96782, 85918, 0, 100))
		{
			return "32740-ugoros.html";
		}
		return "32740.html";
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1))
		{
			qs.setCond(2, true);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case JOHNNY:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						final QuestState prev = player.getQuestState(Q00249_PoisonedPlainsOfTheLizardmen.class.getSimpleName());
						if ((prev != null) && prev.isCompleted() && (player.getLevel() >= MIN_LEVEL))
						{
							htmltext = (hasQuestItems(player, SEER_UGOROS_PASS)) ? "32744-07.htm" : "32744-01.htm";
						}
						else
						{
							htmltext = "32744-00.htm";
						}
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "32744-05.html";
						}
						else if (qs.isCond(2))
						{
							htmltext = "32744-06.html";
						}
						break;
					}
				}
				break;
			}
			case BATRACOS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (hasQuestItems(player, SEER_UGOROS_PASS)) ? "32740-05.html" : "32740-00.html";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "32740-02.html";
						}
						else if (qs.isCond(2))
						{
							giveItems(player, SEER_UGOROS_PASS, 1);
							qs.exitQuest(true, true);
							htmltext = "32740-04.html";
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