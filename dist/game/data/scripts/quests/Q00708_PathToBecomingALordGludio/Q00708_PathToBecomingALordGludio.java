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
package quests.Q00708_PathToBecomingALordGludio;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Path to Becoming a Lord - Gludio (708)
 * @author Sacrifice
 */
public final class Q00708_PathToBecomingALordGludio extends Quest
{
	private static final int SAYRES = 35100;
	private static final int PINTER = 30298;
	private static final int BATHIS = 30332;
	private static final int HEADLESS_KNIGHT = 20280;
	
	private static final int COKES = 1879;
	private static final int IRON_ORE = 1869;
	private static final int ANIMAL_SKIN = 1867;
	private static final int VARNISH = 1865;
	private static final int HEADLESS_KNIGHT_ARMOR = 13848;
	
	private static final int[] MOBS =
	{
		20045, // Skeleton Scout
		20051, // Skeleton Bowman
		20099, // Skeleton
		HEADLESS_KNIGHT
	};
	
	private static final int GLUDIO_CASTLE = 1;
	
	public Q00708_PathToBecomingALordGludio()
	{
		super(708);
		addStartNpc(SAYRES);
		addKillId(MOBS);
		addTalkId(SAYRES, PINTER, BATHIS);
		_questItemIds = new int[]
		{
			HEADLESS_KNIGHT_ARMOR
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = event;
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(GLUDIO_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		switch (event)
		{
			case "35100-03.html":
			{
				qs.startQuest();
				break;
			}
			case "35100-05.html":
			{
				qs.setCond(2);
				break;
			}
			case "35100-08.html":
			{
				if (isLordAvailable(2, qs))
				{
					castleOwner.getQuestState(getName()).set("confidant", String.valueOf(qs.getPlayer().getObjectId()));
					castleOwner.getQuestState(getName()).setCond(3);
					qs.setState(State.STARTED);
				}
				else
				{
					htmltext = "35100-05a.html";
				}
				break;
			}
			case "30298-03.html":
			{
				if (isLordAvailable(3, qs))
				{
					castleOwner.getQuestState(getName()).setCond(4);
				}
				else
				{
					htmltext = "30298-03a.html";
				}
				break;
			}
			case "30332-02.html":
			{
				qs.setCond(6);
				break;
			}
			case "30332-05.html":
			{
				takeItems(player, HEADLESS_KNIGHT_ARMOR, 1);
				qs.setCond(8);
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), NpcStringId.LISTEN_YOU_VILLAGERS_OUR_LIEGE_WHO_WILL_SOON_BECOME_A_LORD_HAS_DEFEATED_THE_HEADLESS_KNIGHT_YOU_CAN_NOW_REST_EASY));
				break;
			}
			case "30298-05.html":
			{
				if (isLordAvailable(8, qs))
				{
					takeItems(player, ANIMAL_SKIN, 100);
					takeItems(player, VARNISH, 100);
					takeItems(player, IRON_ORE, 100);
					takeItems(player, COKES, 50);
					castleOwner.getQuestState(getName()).setCond(9);
				}
				else
				{
					htmltext = "30298-03a.html";
				}
				break;
			}
			case "35100-12.html":
			{
				if (castleOwner != null)
				{
					final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_LORD_OF_THE_TOWN_OF_GLUDIO_LONG_MAY_HE_REIGN);
					packet.addStringParameter(player.getName());
					npc.broadcastPacket(packet);
					qs.exitQuest(true, true);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = killer.getQuestState(getName());
		if ((qs != null) && qs.isCond(6))
		{
			if ((npc.getId() != HEADLESS_KNIGHT) && (Rnd.get(9) == 0))
			{
				addSpawn(HEADLESS_KNIGHT, npc, true, 300000);
			}
			else if (npc.getId() == HEADLESS_KNIGHT)
			{
				giveItems(killer, HEADLESS_KNIGHT_ARMOR, 1);
				qs.setCond(7);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(GLUDIO_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case SAYRES:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35100-01.html";
						}
						else
						{
							htmltext = "35100-00.html";
							qs.exitQuest(true);
						}
					}
					else if (isLordAvailable(2, qs))
					{
						if (castleOwner.calculateDistance2D(npc) <= 200)
						{
							htmltext = "35100-07.html";
						}
						else
						{
							htmltext = "35100-05a.html";
						}
					}
					else if (qs.getState() == State.STARTED)
					{
						htmltext = "35100-08a.html";
					}
					else
					{
						htmltext = "35100-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					htmltext = "35100-04.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "35100-06.html";
				}
				else if (qs.isCond(4))
				{
					qs.set("cond", "5");
					htmltext = "35100-09.html";
				}
				else if (qs.isCond(5))
				{
					htmltext = "35100-10.html";
				}
				else if ((qs.getCond() > 5) && (qs.getCond() < 9))
				{
					htmltext = "35100-08.html";
				}
				else if (qs.isCond(9))
				{
					htmltext = "35100-11.html";
				}
				break;
			}
			case PINTER:
			{
				if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(3, qs))
				{
					if (castleOwner.getQuestState(getName()).getInt("confidant") == qs.getPlayer().getObjectId())
					{
						htmltext = "30298-01.html";
					}
				}
				else if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(8, qs))
				{
					if ((getQuestItemsCount(talker, ANIMAL_SKIN) >= 100) && (getQuestItemsCount(talker, VARNISH) >= 100) && (getQuestItemsCount(talker, IRON_ORE) >= 100) && (getQuestItemsCount(talker, COKES) >= 50))
					{
						htmltext = "30298-04.html";
					}
					else
					{
						htmltext = "30298-04a.html";
					}
				}
				else if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(9, qs))
				{
					htmltext = "30298-06.html";
				}
				break;
			}
			case BATHIS:
			{
				if (qs.isCond(5))
				{
					htmltext = "30332-01.html";
				}
				else if (qs.isCond(6))
				{
					htmltext = "30332-03.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "30332-04.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	private boolean hasFort()
	{
		for (Fort fortress : FortManager.getInstance().getForts())
		{
			if (fortress.getContractedCastleId() == GLUDIO_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isLordAvailable(int cond, QuestState qs)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(GLUDIO_CASTLE);
		final Clan owner = castle.getOwner();
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		return (owner != null) && (castleOwner != null) && (castleOwner != qs.getPlayer()) && (owner == qs.getPlayer().getClan()) && (castleOwner.getQuestState(getName()) != null) && castleOwner.getQuestState(getName()).isCond(cond);
	}
}