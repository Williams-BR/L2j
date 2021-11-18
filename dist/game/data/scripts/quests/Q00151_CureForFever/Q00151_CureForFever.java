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
package quests.Q00151_CureForFever;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Cure for Fever (151)
 * @author malyelfik
 */
public class Q00151_CureForFever extends Quest
{
	// NPCs
	private static final int ELLIAS = 30050;
	private static final int YOHANES = 30032;
	// Monsters
	private static final int[] MOBS =
	{
		20103, // Giant Spider
		20106, // Talon Spider
		20108, // Blade Spider
	};
	// Items
	private static final int ROUND_SHIELD = 102;
	private static final int POISON_SAC = 703;
	private static final int FEVER_MEDICINE = 704;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int CHANCE = 0;
	
	public Q00151_CureForFever()
	{
		super(151);
		addStartNpc(ELLIAS);
		addTalkId(ELLIAS, YOHANES);
		addKillId(MOBS);
		registerQuestItems(POISON_SAC, FEVER_MEDICINE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equalsIgnoreCase("30050-03.htm"))
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
		if ((qs != null) && qs.isCond(1) && (getRandom(5) == CHANCE))
		{
			giveItems(killer, POISON_SAC, 1);
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
			case ELLIAS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? "30050-02.htm" : "30050-01.htm";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(3) && hasQuestItems(player, FEVER_MEDICINE))
						{
							giveItems(player, ROUND_SHIELD, 1);
							addExpAndSp(player, 13106, 613);
							qs.exitQuest(false, true);
							showOnScreenMsg(player, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000); // TODO: Newbie Guide
							htmltext = "30050-06.html";
						}
						else if (qs.isCond(2) && hasQuestItems(player, POISON_SAC))
						{
							htmltext = "30050-05.html";
						}
						else
						{
							htmltext = "30050-04.html";
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
			case YOHANES:
			{
				if (qs.isStarted())
				{
					if (qs.isCond(2) && hasQuestItems(player, POISON_SAC))
					{
						qs.setCond(3, true);
						takeItems(player, POISON_SAC, -1);
						giveItems(player, FEVER_MEDICINE, 1);
						htmltext = "30032-01.html";
					}
					else if (qs.isCond(3) && hasQuestItems(player, FEVER_MEDICINE))
					{
						htmltext = "30032-02.html";
					}
				}
				break;
			}
		}
		return htmltext;
	}
}