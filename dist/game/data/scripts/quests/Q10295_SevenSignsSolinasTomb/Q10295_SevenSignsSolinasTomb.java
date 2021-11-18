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
package quests.Q10295_SevenSignsSolinasTomb;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;

import quests.Q10294_SevenSignsToTheMonasteryOfSilence.Q10294_SevenSignsToTheMonasteryOfSilence;

/**
 * Seven Signs, Solina's Tomb (10295)
 * @author Adry_85
 * @since 2.6.0.0
 */
public class Q10295_SevenSignsSolinasTomb extends Quest
{
	// NPCs
	private static final int ELCADIA = 32787;
	private static final int ERISS_EVIL_THOUGHTS = 32792;
	private static final int SOLINAS_EVIL_THOUGHTS = 32793;
	private static final int SOLINA = 32794;
	private static final int ERIS = 32795;
	private static final int ANAIS = 32796;
	private static final int JUDE_VAN_ETINA = 32797;
	private static final int TELEPORT_CONTROL_DEVICE_1 = 32837;
	private static final int POWERFUL_DEVICE_1 = 32838;
	private static final int POWERFUL_DEVICE_2 = 32839;
	private static final int POWERFUL_DEVICE_3 = 32840;
	private static final int POWERFUL_DEVICE_4 = 32841;
	private static final int TELEPORT_CONTROL_DEVICE_2 = 32842;
	private static final int TOMB_OF_THE_SAINTESS = 32843;
	private static final int TELEPORT_CONTROL_DEVICE_3 = 32844;
	private static final int ALTAR_OF_HALLOWS_1 = 32857;
	private static final int ALTAR_OF_HALLOWS_2 = 32858;
	private static final int ALTAR_OF_HALLOWS_3 = 32859;
	private static final int ALTAR_OF_HALLOWS_4 = 32860;
	// Items
	private static final int SCROLL_OF_ABSTINENCE = 17228;
	private static final int SHIELD_OF_SACRIFICE = 17229;
	private static final int SWORD_OF_HOLY_SPIRIT = 17230;
	private static final int STAFF_OF_BLESSING = 17231;
	// Misc
	private static final int MIN_LEVEL = 81;
	
	public Q10295_SevenSignsSolinasTomb()
	{
		super(10295);
		addStartNpc(ERISS_EVIL_THOUGHTS);
		addTalkId(ERISS_EVIL_THOUGHTS, SOLINAS_EVIL_THOUGHTS, SOLINA, ERIS, ANAIS, JUDE_VAN_ETINA, TELEPORT_CONTROL_DEVICE_1, POWERFUL_DEVICE_1, POWERFUL_DEVICE_2, POWERFUL_DEVICE_3, POWERFUL_DEVICE_4, TELEPORT_CONTROL_DEVICE_2, TOMB_OF_THE_SAINTESS, TELEPORT_CONTROL_DEVICE_3, ALTAR_OF_HALLOWS_1, ALTAR_OF_HALLOWS_2, ALTAR_OF_HALLOWS_3, ALTAR_OF_HALLOWS_4, ELCADIA);
		registerQuestItems(SCROLL_OF_ABSTINENCE, SHIELD_OF_SACRIFICE, SWORD_OF_HOLY_SPIRIT, STAFF_OF_BLESSING);
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
			case "32792-02.htm":
			case "32792-04.htm":
			case "32792-05.htm":
			case "32792-06.htm":
			case "32793-06.html":
			{
				htmltext = event;
				break;
			}
			case "32792-03.htm":
			{
				qs.startQuest();
				qs.setMemoState(1);
				htmltext = event;
				break;
			}
			case "32793-02.html":
			case "32793-03.html":
			{
				if (qs.isMemoState(3))
				{
					htmltext = event;
				}
				break;
			}
			case "32793-04.html":
			{
				if (qs.isMemoState(3))
				{
					qs.setMemoState(4);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "32793-05.html":
			case "32794-02.html":
			{
				if (qs.isMemoState(4))
				{
					qs.setMemoState(5);
					htmltext = event;
				}
				break;
			}
			case "32793-07.html":
			{
				if (qs.isMemoState(5))
				{
					htmltext = event;
				}
				break;
			}
			case "32793-08.html":
			{
				if (qs.isMemoState(5))
				{
					qs.setMemoState(6);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "32837-02.html":
			{
				if (qs.getMemoState() > 1)
				{
					takeItems(player, -1, SCROLL_OF_ABSTINENCE, SHIELD_OF_SACRIFICE, SWORD_OF_HOLY_SPIRIT, STAFF_OF_BLESSING);
					htmltext = event;
				}
				break;
			}
			case "32838-02.html":
			{
				if (qs.isMemoState(1))
				{
					if (hasQuestItems(player, SCROLL_OF_ABSTINENCE))
					{
						htmltext = event;
					}
					else
					{
						htmltext = "32838-03.html";
					}
				}
				break;
			}
			case "32839-02.html":
			{
				if (qs.isMemoState(1))
				{
					if (hasQuestItems(player, SHIELD_OF_SACRIFICE))
					{
						htmltext = event;
					}
					else
					{
						htmltext = "32839-03.html";
					}
				}
				break;
			}
			case "32840-02.html":
			{
				if (qs.isMemoState(1))
				{
					if (hasQuestItems(player, SWORD_OF_HOLY_SPIRIT))
					{
						htmltext = event;
					}
					else
					{
						htmltext = "32840-03.html";
					}
				}
				break;
			}
			case "32841-02.html":
			{
				if (qs.isMemoState(1))
				{
					if (hasQuestItems(player, STAFF_OF_BLESSING))
					{
						htmltext = event;
					}
					else
					{
						htmltext = "32841-03.html";
					}
				}
				break;
			}
			case "32857-02.html":
			{
				if (qs.isMemoState(1))
				{
					if (hasQuestItems(player, STAFF_OF_BLESSING))
					{
						htmltext = event;
					}
					else
					{
						giveItems(player, STAFF_OF_BLESSING, 1);
						htmltext = "32857-03.html";
					}
				}
				break;
			}
			case "32858-02.html":
			{
				if (qs.isMemoState(1))
				{
					if (hasQuestItems(player, SWORD_OF_HOLY_SPIRIT))
					{
						htmltext = event;
					}
					else
					{
						giveItems(player, SWORD_OF_HOLY_SPIRIT, 1);
						htmltext = "32858-03.html";
					}
				}
				break;
			}
			case "32859-02.html":
			{
				if (qs.isMemoState(1))
				{
					if (hasQuestItems(player, SCROLL_OF_ABSTINENCE))
					{
						htmltext = event;
					}
					else
					{
						giveItems(player, SCROLL_OF_ABSTINENCE, 1);
						htmltext = "32859-03.html";
					}
				}
				break;
			}
			case "32860-02.html":
			{
				if (qs.isMemoState(1))
				{
					if (hasQuestItems(player, SHIELD_OF_SACRIFICE))
					{
						htmltext = event;
					}
					else
					{
						giveItems(player, SHIELD_OF_SACRIFICE, 1);
						htmltext = "32860-03.html";
					}
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
		if (qs.isCompleted())
		{
			if (npc.getId() == ERISS_EVIL_THOUGHTS)
			{
				htmltext = "32792-07.html";
			}
		}
		else if (qs.isCreated())
		{
			final QuestState st1 = player.getQuestState(Q10294_SevenSignsToTheMonasteryOfSilence.class.getSimpleName());
			if ((player.getLevel() >= MIN_LEVEL) && (st1 != null) && (st1.isCompleted()))
			{
				htmltext = "32792-01.htm";
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case ERISS_EVIL_THOUGHTS:
				{
					final int memoState = qs.getMemoState();
					if (memoState == 1)
					{
						htmltext = "32792-12.html";
					}
					else if (memoState == 2)
					{
						htmltext = "32792-08.html";
					}
					else if ((memoState > 2) && (memoState < 6))
					{
						htmltext = "32792-09.html";
					}
					else if (memoState == 6)
					{
						if (player.isSubClassActive())
						{
							htmltext = "32792-10.html";
						}
						else
						{
							addExpAndSp(player, 125000000, 12500000);
							qs.exitQuest(false, true);
							htmltext = "32792-11.html";
						}
					}
					break;
				}
				case SOLINAS_EVIL_THOUGHTS:
				{
					switch (qs.getMemoState())
					{
						case 3:
						{
							htmltext = "32793-01.html";
							break;
						}
						case 4:
						{
							htmltext = "32793-09.html";
							break;
						}
						case 5:
						{
							htmltext = "32793-10.html";
							break;
						}
						case 6:
						{
							htmltext = "32793-11.html";
							break;
						}
					}
					break;
				}
				case SOLINA:
				{
					if (qs.isMemoState(4))
					{
						htmltext = "32794-01.html";
					}
					else if (qs.isMemoState(5))
					{
						htmltext = "32794-03.html";
					}
					break;
				}
				case ERIS:
				{
					if (qs.isMemoState(4))
					{
						htmltext = "32795-01.html";
					}
					else if (qs.isMemoState(5))
					{
						htmltext = "32795-02.html";
					}
					break;
				}
				case ANAIS:
				{
					if (qs.isMemoState(4))
					{
						htmltext = "32796-01.html";
					}
					else if (qs.isMemoState(5))
					{
						htmltext = "32796-02.html";
					}
					break;
				}
				case JUDE_VAN_ETINA:
				{
					if (qs.isMemoState(4))
					{
						htmltext = "32797-01.html";
					}
					else if (qs.isMemoState(5))
					{
						htmltext = "32797-02.html";
					}
					break;
				}
				case TELEPORT_CONTROL_DEVICE_1:
				{
					if (qs.getMemoState() > 1)
					{
						takeItems(player, -1, SCROLL_OF_ABSTINENCE, SHIELD_OF_SACRIFICE, SWORD_OF_HOLY_SPIRIT, STAFF_OF_BLESSING);
						htmltext = "32837-01.html";
					}
					else if (qs.isMemoState(1))
					{
						htmltext = "32837-03.html";
					}
					break;
				}
				case POWERFUL_DEVICE_1:
				{
					if (qs.isMemoState(1))
					{
						htmltext = "32838-01.html";
					}
					break;
				}
				case POWERFUL_DEVICE_2:
				{
					if (qs.isMemoState(1))
					{
						htmltext = "32839-01.html";
					}
					break;
				}
				case POWERFUL_DEVICE_3:
				{
					if (qs.isMemoState(1))
					{
						htmltext = "32840-01.html";
					}
					break;
				}
				case POWERFUL_DEVICE_4:
				{
					if (qs.isMemoState(1))
					{
						htmltext = "32841-01.html";
					}
					break;
				}
				case TELEPORT_CONTROL_DEVICE_2:
				{
					if (qs.getMemoState() > 2)
					{
						htmltext = "32842-01.html";
					}
					break;
				}
				case TOMB_OF_THE_SAINTESS:
				{
					if (qs.isMemoState(2))
					{
						htmltext = "32843-01.html";
					}
					else if (qs.getMemoState() > 2)
					{
						htmltext = "32843-02.html";
					}
					break;
				}
				case TELEPORT_CONTROL_DEVICE_3:
				{
					if (qs.getMemoState() > 2)
					{
						htmltext = "32844-01.html";
					}
					break;
				}
				case ALTAR_OF_HALLOWS_1:
				{
					if (qs.isMemoState(1))
					{
						htmltext = "32857-01.html";
					}
					break;
				}
				case ALTAR_OF_HALLOWS_2:
				{
					if (qs.isMemoState(1))
					{
						htmltext = "32858-01.html";
					}
					break;
				}
				case ALTAR_OF_HALLOWS_3:
				{
					if (qs.isMemoState(1))
					{
						htmltext = "32859-01.html";
					}
					break;
				}
				case ALTAR_OF_HALLOWS_4:
				{
					if (qs.isMemoState(1))
					{
						htmltext = "32860-01.html";
					}
					break;
				}
				case ELCADIA:
				{
					final int memoState = qs.getMemoState();
					if (memoState < 1)
					{
						htmltext = "32787-01.html";
					}
					else
					{
						switch (memoState)
						{
							case 1:
							{
								htmltext = "32787-02.html";
								break;
							}
							case 2:
							{
								htmltext = "32787-03.html";
								break;
							}
							case 3:
							{
								htmltext = "32787-04.html";
								break;
							}
							case 4:
							{
								htmltext = "32787-05.html";
								break;
							}
							case 5:
							{
								htmltext = "32787-06.html";
								break;
							}
						}
					}
					break;
				}
			}
		}
		return htmltext;
	}
}
