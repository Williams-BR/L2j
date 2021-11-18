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
package quests.Q10271_TheEnvelopingDarkness;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10269_ToTheSeedOfDestruction.Q10269_ToTheSeedOfDestruction;

/**
 * The Enveloping Darkness (10271)
 * @author Gladicek
 */
public class Q10271_TheEnvelopingDarkness extends Quest
{
	private static final int ORBYU = 32560;
	private static final int EL = 32556;
	private static final int MEDIBAL_CORPSE = 32528;
	private static final int MEDIBAL_DOCUMENT = 13852;
	
	public Q10271_TheEnvelopingDarkness()
	{
		super(10271);
		addStartNpc(ORBYU);
		addTalkId(ORBYU, EL, MEDIBAL_CORPSE);
		registerQuestItems(MEDIBAL_DOCUMENT);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		switch (event)
		{
			case "32560-05.html":
			{
				qs.startQuest();
				break;
			}
			case "32556-06.html":
			{
				qs.setCond(2, true);
				break;
			}
			case "32556-09.html":
			{
				if (hasQuestItems(player, MEDIBAL_DOCUMENT))
				{
					takeItems(player, MEDIBAL_DOCUMENT, -1);
					qs.setCond(4, true);
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState qs = getQuestState(player, true);
		
		switch (npc.getId())
		{
			case ORBYU:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						qs = player.getQuestState(Q10269_ToTheSeedOfDestruction.class.getSimpleName());
						htmltext = ((player.getLevel() >= 75) && (qs != null) && qs.isCompleted()) ? "32560-01.htm" : "32560-02.html";
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 1:
							{
								htmltext = "32560-05.html"; // TODO this html should most probably be different
								break;
							}
							case 2:
							{
								htmltext = "32560-06.html";
								break;
							}
							case 3:
							{
								htmltext = "32560-07.html";
								break;
							}
							case 4:
							{
								htmltext = "32560-08.html";
								giveAdena(player, 62516, true);
								addExpAndSp(player, 377403, 37867);
								qs.exitQuest(false, true);
								break;
							}
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = "32560-03.html";
						break;
					}
				}
				break;
			}
			case EL:
			{
				if (qs.isCompleted())
				{
					htmltext = "32556-02.html";
				}
				else if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "32556-01.html";
							break;
						}
						case 2:
						{
							htmltext = "32556-07.html";
							break;
						}
						case 3:
						{
							htmltext = "32556-08.html";
							break;
						}
						case 4:
						{
							htmltext = "32556-09.html";
							break;
						}
					}
				}
				break;
			}
			case MEDIBAL_CORPSE:
			{
				if (qs.isCompleted())
				{
					htmltext = "32528-02.html";
				}
				else if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 2:
						{
							htmltext = "32528-01.html";
							qs.setCond(3, true);
							giveItems(player, MEDIBAL_DOCUMENT, 1);
							break;
						}
						case 3:
						case 4:
						{
							htmltext = "32528-03.html";
							break;
						}
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
