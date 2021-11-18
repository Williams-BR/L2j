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
package quests.Q00021_HiddenTruth;

import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.instancemanager.WalkingManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

import quests.Q00022_TragedyInVonHellmannForest.Q00022_TragedyInVonHellmannForest;

/**
 * Hidden Truth (21)
 * @author xban1x
 */
public class Q00021_HiddenTruth extends Quest
{
	// NPCs
	private static final int INNOCENTIN = 31328;
	private static final int AGRIPEL = 31348;
	private static final int BENEDICT = 31349;
	private static final int DOMINIC = 31350;
	private static final int MYSTERIOUS_WIZARD = 31522;
	private static final int TOMBSTONE = 31523;
	private static final int GHOST_OF_VON_HELLMAN = 31524;
	private static final int GHOST_OF_VON_HELLMANS_PAGE = 31525;
	private static final int BROKEN_BOOKSHELF = 31526;
	// Location
	private static final Location GHOST_LOC = new Location(51432, -54570, -3136, 0);
	private static final Location PAGE_LOC = new Location(51446, -54514, -3136, 0);
	// Items
	private static final int CROSS_OF_EINHASAD = 7140;
	private static final int CROSS_OF_EINHASAD2 = 7141;
	// Misc
	private static final int MIN_LEVEL = 63;
	private static final String PAGE_ROUTE_NAME = "rune_ghost1b";
	private static int PAGE_COUNT = 0;
	private static boolean GHOST_SPAWNED = false;
	private boolean PAGE_SPAWNED = false;
	private boolean MOVE_ENDED = false;
	
	public Q00021_HiddenTruth()
	{
		super(21);
		addStartNpc(MYSTERIOUS_WIZARD);
		addTalkId(MYSTERIOUS_WIZARD, TOMBSTONE, GHOST_OF_VON_HELLMAN, GHOST_OF_VON_HELLMANS_PAGE, BROKEN_BOOKSHELF, AGRIPEL, BENEDICT, DOMINIC, INNOCENTIN);
		addRouteFinishedId(GHOST_OF_VON_HELLMANS_PAGE);
		addCreatureSeeId(GHOST_OF_VON_HELLMANS_PAGE);
		registerQuestItems(CROSS_OF_EINHASAD);
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
				case "31328-02.html":
				case "31328-03.html":
				case "31328-04.html":
				case "31522-01.htm":
				case "31522-04.html":
				case "31523-02.html":
				case "31524-02.html":
				case "31524-03.html":
				case "31524-04.html":
				case "31524-05.html":
				case "31526-01.html":
				case "31526-02.html":
				case "31526-04.html":
				case "31526-05.html":
				case "31526-06.html":
				case "31526-12.html":
				case "31526-13.html":
				{
					htmltext = event;
					break;
				}
				case "31328-05.html":
				{
					if (qs.isCond(7))
					{
						giveItems(player, CROSS_OF_EINHASAD2, 1);
						addExpAndSp(player, 131228, 11978);
						qs.exitQuest(false, true);
						htmltext = event;
					}
					break;
				}
				case "31522-02.htm":
				{
					if (player.getLevel() < MIN_LEVEL)
					{
						htmltext = "31522-03.htm";
					}
					else
					{
						qs.startQuest();
						htmltext = event;
					}
					break;
				}
				case "31523-03.html":
				{
					if (GHOST_SPAWNED)
					{
						htmltext = "31523-04.html";
						playSound(player, QuestSound.SKILLSOUND_HORROR_2);
					}
					else
					{
						final Npc ghost = addSpawn(GHOST_OF_VON_HELLMAN, GHOST_LOC, false, 0);
						ghost.broadcastPacket(new NpcSay(ghost.getObjectId(), ChatType.GENERAL, ghost.getId(), NpcStringId.WHO_AWOKE_ME));
						GHOST_SPAWNED = true;
						startQuestTimer("DESPAWN_GHOST", 1000 * 300, ghost, player);
						qs.setCond(2);
						playSound(player, QuestSound.SKILLSOUND_HORROR_2);
						htmltext = event;
					}
					break;
				}
				case "31524-06.html":
				{
					if (PAGE_COUNT < 5)
					{
						final Npc page = addSpawn(GHOST_OF_VON_HELLMANS_PAGE, PAGE_LOC, false, 0);
						page.setScriptValue(player.getObjectId());
						page.broadcastPacket(new NpcSay(page.getObjectId(), ChatType.NPC_GENERAL, page.getId(), NpcStringId.MY_MASTER_HAS_INSTRUCTED_ME_TO_BE_YOUR_GUIDE_S1).addStringParameter(player.getName()));
						WalkingManager.getInstance().startMoving(page, PAGE_ROUTE_NAME);
						PAGE_COUNT++;
						qs.setCond(3);
						htmltext = event;
					}
					else
					{
						htmltext = "31524-06a.html";
					}
					break;
				}
				case "31526-03.html":
				{
					playSound(player, QuestSound.ITEMSOUND_ARMOR_CLOTH);
					htmltext = event;
					break;
				}
				case "31526-07.html":
				{
					qs.setCond(4);
					htmltext = event;
					break;
				}
				case "31526-08.html":
				{
					if (!qs.isCond(5))
					{
						playSound(player, QuestSound.AMDSOUND_ED_CHIMES);
						qs.setCond(5);
						htmltext = event;
					}
					else
					{
						htmltext = "31526-09.html";
					}
					break;
				}
				case "31526-14.html":
				{
					giveItems(player, CROSS_OF_EINHASAD, 1);
					qs.setCond(6);
					htmltext = event;
					break;
				}
				case "DESPAWN_GHOST":
				{
					GHOST_SPAWNED = false;
					npc.deleteMe();
					break;
				}
				case "DESPAWN":
				{
					PAGE_COUNT--;
					npc.deleteMe();
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case MYSTERIOUS_WIZARD:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = "31522-01.htm";
						break;
					}
					case State.STARTED:
					{
						htmltext = "31522-05.html";
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
			case TOMBSTONE:
			{
				htmltext = "31523-01.html";
				break;
			}
			case GHOST_OF_VON_HELLMAN:
			{
				switch (qs.getCond())
				{
					case 2:
					{
						htmltext = "31524-01.html";
						break;
					}
					case 3:
					{
						if (PAGE_SPAWNED)
						{
							htmltext = "31524-07b.html";
						}
						else
						{
							if (PAGE_COUNT < 5)
							{
								final Npc page = addSpawn(GHOST_OF_VON_HELLMANS_PAGE, PAGE_LOC, true, 0);
								PAGE_COUNT++;
								PAGE_SPAWNED = true;
								page.setScriptValue(player.getObjectId());
								WalkingManager.getInstance().startMoving(page, PAGE_ROUTE_NAME);
								htmltext = "31524-07.html";
							}
							else
							{
								htmltext = "31524-07a.html";
							}
						}
						break;
					}
					case 4:
					{
						htmltext = "31524-07c.html";
						break;
					}
				}
				break;
			}
			case GHOST_OF_VON_HELLMANS_PAGE:
			{
				if (qs.isCond(3))
				{
					if (MOVE_ENDED)
					{
						htmltext = "31525-02.html";
						startQuestTimer("DESPAWN", 3000, npc, player);
					}
					else
					{
						htmltext = "31525-01.html";
					}
				}
				break;
			}
			case BROKEN_BOOKSHELF:
			{
				switch (qs.getCond())
				{
					case 3:
					{
						htmltext = "31526-01.html";
						break;
					}
					case 4:
					{
						qs.setCond(5);
						playSound(player, QuestSound.AMDSOUND_ED_CHIMES);
						htmltext = "31526-10.html";
						break;
					}
					case 5:
					{
						htmltext = "31526-11.html";
						break;
					}
					case 6:
					{
						htmltext = "31526-15.html";
						break;
					}
				}
				break;
			}
			case AGRIPEL:
			{
				if (hasQuestItems(player, CROSS_OF_EINHASAD) && qs.isCond(6))
				{
					qs.set("AGRIPEL", "1");
					if ((qs.getInt("AGRIPEL") == 1) && (qs.getInt("DOMINIC") == 1) && (qs.getInt("BENEDICT") == 1))
					{
						htmltext = "31348-03.html";
						qs.setCond(7);
					}
					else if ((qs.getInt("DOMINIC") == 1) || (qs.getInt("BENEDICT") == 1))
					{
						htmltext = "31348-02.html";
					}
					else
					{
						htmltext = "31348-01.html";
					}
				}
				else if (qs.isCond(7))
				{
					htmltext = "31348-03.html";
				}
				break;
			}
			
			case BENEDICT:
			{
				if (hasQuestItems(player, CROSS_OF_EINHASAD) && qs.isCond(6))
				{
					qs.set("BENEDICT", "1");
					if ((qs.getInt("AGRIPEL") == 1) && (qs.getInt("DOMINIC") == 1) && (qs.getInt("BENEDICT") == 1))
					{
						htmltext = "31349-03.html";
						qs.setCond(7);
					}
					else if ((qs.getInt("AGRIPEL") == 1) || (qs.getInt("DOMINIC") == 1))
					{
						htmltext = "31349-02.html";
					}
					else
					{
						htmltext = "31349-01.html";
					}
				}
				else if (qs.isCond(7))
				{
					htmltext = "31349-03.html";
				}
				break;
			}
			case DOMINIC:
			{
				if (hasQuestItems(player, CROSS_OF_EINHASAD) && qs.isCond(6))
				{
					qs.set("DOMINIC", "1");
					if ((qs.getInt("AGRIPEL") == 1) && (qs.getInt("DOMINIC") == 1) && (qs.getInt("BENEDICT") == 1))
					{
						htmltext = "31350-03.html";
						qs.setCond(7);
					}
					else if ((qs.getInt("AGRIPEL") == 1) || (qs.getInt("BENEDICT") == 1))
					{
						htmltext = "31350-02.html";
					}
					else
					{
						htmltext = "31350-01.html";
					}
				}
				else if (qs.isCond(7))
				{
					htmltext = "31350-03.html";
				}
				break;
			}
			case INNOCENTIN:
			{
				if (qs.isCond(7) && hasQuestItems(player, CROSS_OF_EINHASAD))
				{
					htmltext = "31328-01.html";
				}
				else if (qs.isCompleted())
				{
					qs = player.getQuestState(Q00022_TragedyInVonHellmannForest.class.getSimpleName());
					if (qs == null)
					{
						htmltext = "31328-06.html";
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			playSound((PlayerInstance) creature, QuestSound.HORROR_01);
		}
		return super.onCreatureSee(npc, creature);
	}
	
	@Override
	public void onRouteFinished(Npc npc)
	{
		final QuestState qs = World.getInstance().getPlayer(npc.getScriptValue()).getQuestState(getName());
		if (qs != null)
		{
			startQuestTimer("DESPAWN", 15000, npc, null);
			MOVE_ENDED = true;
		}
	}
}
