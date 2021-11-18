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
package quests.Q00241_PossessorOfAPreciousSoul1;

import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Possessor Of A PreciousSoul part 1 (241)<br>
 * Original Jython script by disKret.
 * @author Joxit
 */
public class Q00241_PossessorOfAPreciousSoul1 extends Quest
{
	// NPCs
	private static final int STEDMIEL = 30692;
	private static final int GABRIELLE = 30753;
	private static final int GILMORE = 30754;
	private static final int KANTABILON = 31042;
	private static final int RAHORAKTI = 31336;
	private static final int TALIEN = 31739;
	private static final int CARADINE = 31740;
	private static final int VIRGIL = 31742;
	private static final int KASSANDRA = 31743;
	private static final int OGMAR = 31744;
	// Mobs
	private static final int BARAHAM = 27113;
	private static final int MALRUK_SUCCUBUS_1 = 20244;
	private static final int MALRUK_SUCCUBUS_TUREN_1 = 20245;
	private static final int MALRUK_SUCCUBUS_2 = 20283;
	private static final int MALRUK_SUCCUBUS_TUREN_2 = 20284;
	private static final int TAIK_ORC_SUPPLY_LEADER = 20669;
	// Items
	private static final int LEGEND_OF_SEVENTEEN = 7587;
	private static final int MALRUK_SUCCUBUS_CLAW = 7597;
	private static final int ECHO_CRYSTAL = 7589;
	private static final int POETRY_BOOK = 7588;
	private static final int CRIMSON_MOSS = 7598;
	private static final int RAHORAKTIS_MEDICINE = 7599;
	private static final int VIRGILS_LETTER = 7677;
	// Rewards
	private static final int CRIMSON_MOSS_CHANCE = 30;
	private static final int MALRUK_SUCCUBUS_CLAW_CHANCE = 60;
	
	public Q00241_PossessorOfAPreciousSoul1()
	{
		super(241);
		addStartNpc(TALIEN);
		addTalkId(TALIEN, STEDMIEL, GABRIELLE, GILMORE, KANTABILON, RAHORAKTI, CARADINE, KASSANDRA, VIRGIL, OGMAR);
		addKillId(BARAHAM, MALRUK_SUCCUBUS_1, MALRUK_SUCCUBUS_TUREN_1, MALRUK_SUCCUBUS_2, MALRUK_SUCCUBUS_TUREN_2, TAIK_ORC_SUPPLY_LEADER);
		registerQuestItems(LEGEND_OF_SEVENTEEN, MALRUK_SUCCUBUS_CLAW, ECHO_CRYSTAL, POETRY_BOOK, CRIMSON_MOSS, RAHORAKTIS_MEDICINE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		if (!player.isSubClassActive())
		{
			return "no_sub.html";
		}
		
		switch (event)
		{
			case "31739-02.html":
			{
				qs.startQuest();
				break;
			}
			case "30753-02.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
				}
				break;
			}
			case "30754-02.html":
			{
				if (qs.isCond(2))
				{
					qs.setCond(3, true);
				}
				break;
			}
			case "31739-05.html":
			{
				if (qs.isCond(4) && hasQuestItems(player, LEGEND_OF_SEVENTEEN))
				{
					takeItems(player, LEGEND_OF_SEVENTEEN, -1);
					qs.setCond(5, true);
				}
				break;
			}
			case "31042-02.html":
			{
				if (qs.isCond(5))
				{
					qs.setCond(6, true);
				}
				break;
			}
			case "31042-05.html":
			{
				if (qs.isCond(7) && (getQuestItemsCount(player, MALRUK_SUCCUBUS_CLAW) >= 10))
				{
					takeItems(player, MALRUK_SUCCUBUS_CLAW, -1);
					giveItems(player, ECHO_CRYSTAL, 1);
					qs.setCond(8, true);
				}
				break;
			}
			case "31739-08.html":
			{
				if (qs.isCond(8) && hasQuestItems(player, ECHO_CRYSTAL))
				{
					takeItems(player, ECHO_CRYSTAL, -1);
					qs.setCond(9, true);
				}
				break;
			}
			case "30692-02.html":
			{
				if (qs.isCond(9) && !hasQuestItems(player, POETRY_BOOK))
				{
					giveItems(player, POETRY_BOOK, 1);
					qs.setCond(10, true);
				}
				break;
			}
			case "31739-11.html":
			{
				if (qs.isCond(10) && hasQuestItems(player, POETRY_BOOK))
				{
					takeItems(player, POETRY_BOOK, -1);
					qs.setCond(11, true);
				}
				break;
			}
			case "31742-02.html":
			{
				if (qs.isCond(11))
				{
					qs.setCond(12, true);
				}
				break;
			}
			case "31744-02.html":
			{
				if (qs.isCond(12))
				{
					qs.setCond(13, true);
				}
				break;
			}
			case "31336-02.html":
			{
				if (qs.isCond(13))
				{
					qs.setCond(14, true);
				}
				break;
			}
			case "31336-05.html":
			{
				if (qs.isCond(15) && (getQuestItemsCount(player, CRIMSON_MOSS) >= 5))
				{
					takeItems(player, CRIMSON_MOSS, -1);
					giveItems(player, RAHORAKTIS_MEDICINE, 1);
					qs.setCond(16, true);
				}
				break;
			}
			case "31743-02.html":
			{
				if (qs.isCond(16) && hasQuestItems(player, RAHORAKTIS_MEDICINE))
				{
					takeItems(player, RAHORAKTIS_MEDICINE, -1);
					qs.setCond(17, true);
				}
				break;
			}
			case "31742-05.html":
			{
				if (qs.isCond(17))
				{
					qs.setCond(18, true);
				}
				break;
			}
			case "31740-05.html":
			{
				if (qs.getCond() >= 18)
				{
					giveItems(player, VIRGILS_LETTER, 1);
					addExpAndSp(player, 263043, 0);
					qs.exitQuest(false, true);
				}
				break;
			}
		}
		return event;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final PlayerInstance partyMember;
		final QuestState qs;
		switch (npc.getId())
		{
			case BARAHAM:
			{
				partyMember = getRandomPartyMember(player, 3);
				if (partyMember == null)
				{
					return null;
				}
				
				qs = getQuestState(partyMember, false);
				giveItems(player, LEGEND_OF_SEVENTEEN, 1);
				qs.setCond(4, true);
				break;
			}
			case MALRUK_SUCCUBUS_1:
			case MALRUK_SUCCUBUS_TUREN_1:
			case MALRUK_SUCCUBUS_2:
			case MALRUK_SUCCUBUS_TUREN_2:
			{
				partyMember = getRandomPartyMember(player, 6);
				if (partyMember == null)
				{
					return null;
				}
				qs = getQuestState(partyMember, false);
				if ((MALRUK_SUCCUBUS_CLAW_CHANCE >= getRandom(100)) && (getQuestItemsCount(partyMember, MALRUK_SUCCUBUS_CLAW) < 10))
				{
					giveItems(partyMember, MALRUK_SUCCUBUS_CLAW, 1);
					if (getQuestItemsCount(partyMember, MALRUK_SUCCUBUS_CLAW) == 10)
					{
						qs.setCond(7, true);
					}
					else
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case TAIK_ORC_SUPPLY_LEADER:
			{
				partyMember = getRandomPartyMember(player, 14);
				if (partyMember == null)
				{
					return null;
				}
				qs = getQuestState(partyMember, false);
				if ((CRIMSON_MOSS_CHANCE >= getRandom(100)) && (getQuestItemsCount(partyMember, CRIMSON_MOSS) < 5))
				{
					giveItems(partyMember, CRIMSON_MOSS, 1);
					if (getQuestItemsCount(partyMember, CRIMSON_MOSS) == 5)
					{
						qs.setCond(15, true);
					}
					else
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isStarted() && !player.isSubClassActive())
		{
			return "no_sub.html";
		}
		
		switch (npc.getId())
		{
			case TALIEN:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (((player.getLevel() >= 50) && player.isSubClassActive()) ? "31739-01.htm" : "31739-00.htm");
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 1:
							{
								htmltext = "31739-03.html";
								break;
							}
							case 4:
							{
								if (hasQuestItems(player, LEGEND_OF_SEVENTEEN))
								{
									htmltext = "31739-04.html";
								}
								break;
							}
							case 5:
							{
								htmltext = "31739-06.html";
								break;
							}
							case 8:
							{
								if (hasQuestItems(player, ECHO_CRYSTAL))
								{
									htmltext = "31739-07.html";
								}
								break;
							}
							case 9:
							{
								htmltext = "31739-09.html";
								break;
							}
							case 10:
							{
								if (hasQuestItems(player, POETRY_BOOK))
								{
									htmltext = "31739-10.html";
								}
								break;
							}
							case 11:
							{
								htmltext = "31739-12.html";
								break;
							}
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
			case GABRIELLE:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30753-01.html";
						break;
					}
					case 2:
					{
						htmltext = "30753-03.html";
						break;
					}
				}
				break;
			}
			case GILMORE:
			{
				switch (qs.getCond())
				{
					case 2:
					{
						htmltext = "30754-01.html";
						break;
					}
					case 3:
					{
						htmltext = "30754-03.html";
						break;
					}
				}
				break;
			}
			case KANTABILON:
			{
				switch (qs.getCond())
				{
					case 5:
					{
						htmltext = "31042-01.html";
						break;
					}
					case 6:
					{
						htmltext = "31042-04.html";
						break;
					}
					case 7:
					{
						if (getQuestItemsCount(player, MALRUK_SUCCUBUS_CLAW) >= 10)
						{
							htmltext = "31042-03.html";
						}
						break;
					}
					case 8:
					{
						htmltext = "31042-06.html";
						break;
					}
				}
				break;
			}
			case STEDMIEL:
			{
				switch (qs.getCond())
				{
					case 9:
					{
						htmltext = "30692-01.html";
						break;
					}
					case 10:
					{
						htmltext = "30692-03.html";
						break;
					}
				}
				break;
			}
			case VIRGIL:
			{
				switch (qs.getCond())
				{
					case 11:
					{
						htmltext = "31742-01.html";
						break;
					}
					case 12:
					{
						htmltext = "31742-03.html";
						break;
					}
					case 17:
					{
						htmltext = "31742-04.html";
						break;
					}
					case 18:
					{
						htmltext = "31742-06.html";
						break;
					}
				}
				break;
			}
			case OGMAR:
			{
				switch (qs.getCond())
				{
					case 12:
					{
						htmltext = "31744-01.html";
						break;
					}
					case 13:
					{
						htmltext = "31744-03.html";
						break;
					}
				}
				break;
			}
			case RAHORAKTI:
			{
				switch (qs.getCond())
				{
					case 13:
					{
						htmltext = "31336-01.html";
						break;
					}
					case 14:
					{
						htmltext = "31336-04.html";
						break;
					}
					case 15:
					{
						if (getQuestItemsCount(player, CRIMSON_MOSS) >= 5)
						{
							htmltext = "31336-03.html";
						}
						break;
					}
					case 16:
					{
						htmltext = "31336-06.html";
						break;
					}
				}
				break;
			}
			case KASSANDRA:
			{
				switch (qs.getCond())
				{
					case 16:
					{
						if (hasQuestItems(player, RAHORAKTIS_MEDICINE))
						{
							htmltext = "31743-01.html";
						}
						break;
					}
					case 17:
					{
						htmltext = "31743-03.html";
						break;
					}
				}
				break;
			}
			case CARADINE:
			{
				if (qs.getCond() >= 18)
				{
					htmltext = "31740-01.html";
				}
				break;
			}
		}
		return htmltext;
	}
}
