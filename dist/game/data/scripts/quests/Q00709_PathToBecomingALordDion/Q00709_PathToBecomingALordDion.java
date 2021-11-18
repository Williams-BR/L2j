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
package quests.Q00709_PathToBecomingALordDion;

import org.l2jmobius.commons.util.CommonUtil;
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
 * Path to Becoming a Lord - Dion (709)
 * @author Sacrifice
 */
public final class Q00709_PathToBecomingALordDion extends Quest
{
	private static final int CROSBY = 35142;
	private static final int ROUKE = 31418;
	private static final int SOPHYA = 30735;
	private static final int BLOODY_AXE_AIDE = 27392;
	
	private static final int MANDRAGORA_ROOT = 13849;
	private static final int BLOODY_AXE_BLACK_EPAULETTE = 13850;
	
	private static final int[] OL_MAHUMS =
	{
		20208, // Ol Mahum Raider
		20209, // Ol Mahum Marksman
		20210, // Ol Mahum Sergeant
		20211, // Ol Mahum Captain
		BLOODY_AXE_AIDE
	};
	
	private static final int[] MANDRAGORAS =
	{
		20154, // Mandragora Sprout
		20155, // Mandragora Sapling
		20156 // Mandragora Blossom
	};
	
	private static final int DION_CASTLE = 2;
	
	public Q00709_PathToBecomingALordDion()
	{
		super(709);
		addStartNpc(CROSBY);
		addKillId(OL_MAHUMS);
		addKillId(MANDRAGORAS);
		addTalkId(CROSBY, SOPHYA, ROUKE);
		_questItemIds = new int[]
		{
			BLOODY_AXE_BLACK_EPAULETTE,
			MANDRAGORA_ROOT
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = event;
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(DION_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		switch (event)
		{
			case "35142-03.html":
			{
				qs.startQuest();
				break;
			}
			case "35142-06.html":
			{
				if (isLordAvailable(2, qs))
				{
					castleOwner.getQuestState(getName()).set("confidant", String.valueOf(qs.getPlayer().getObjectId()));
					castleOwner.getQuestState(getName()).setCond(3);
					qs.setState(State.STARTED);
				}
				else
				{
					htmltext = "35142-05a.html";
				}
				break;
			}
			case "31418-03.html":
			{
				if (isLordAvailable(3, qs))
				{
					castleOwner.getQuestState(getName()).setCond(4);
				}
				else
				{
					htmltext = "35142-05a.html";
				}
				break;
			}
			case "30735-02.html":
			{
				qs.set("cond", "6");
				break;
			}
			case "30735-05.html":
			{
				takeItems(player, BLOODY_AXE_BLACK_EPAULETTE, 1);
				qs.set("cond", "8");
				break;
			}
			case "31418-05.html":
			{
				if (isLordAvailable(8, qs))
				{
					takeItems(player, MANDRAGORA_ROOT, -1);
					castleOwner.getQuestState(getName()).setCond(9);
				}
				break;
			}
			case "35142-10.html":
			{
				if (castleOwner != null)
				{
					final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_LORD_OF_THE_TOWN_OF_DION_LONG_MAY_HE_REIGN);
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
		if ((qs != null) && qs.isCond(6) && CommonUtil.contains(OL_MAHUMS, npc.getId()))
		{
			if ((npc.getId() != BLOODY_AXE_AIDE) && (Rnd.get(9) == 0))
			{
				addSpawn(BLOODY_AXE_AIDE, npc, true, 300000);
			}
			else if (npc.getId() == BLOODY_AXE_AIDE)
			{
				giveItems(killer, BLOODY_AXE_BLACK_EPAULETTE, 1);
				qs.setCond(7);
			}
		}
		
		if ((qs != null) && (qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(8, qs) && CommonUtil.contains(MANDRAGORAS, npc.getId()) && (getQuestItemsCount(killer, MANDRAGORA_ROOT) < 100))
		{
			giveItems(killer, MANDRAGORA_ROOT, 1);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(DION_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case CROSBY:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35142-01.html";
						}
						else
						{
							htmltext = "35142-00.html";
							qs.exitQuest(true);
						}
					}
					else if (isLordAvailable(2, qs))
					{
						if (castleOwner.calculateDistance2D(npc) <= 200)
						{
							htmltext = "35142-05.html";
						}
						else
						{
							htmltext = "35142-05a.html";
						}
					}
					else
					{
						htmltext = "35142-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					qs.set("cond", "2");
					htmltext = "35142-04.html";
				}
				else if (qs.isCond(2) || qs.isCond(3))
				{
					htmltext = "35142-04a.html";
				}
				else if (qs.isCond(4))
				{
					qs.set("cond", "5");
					htmltext = "35142-07.html";
				}
				else if (qs.isCond(5))
				{
					htmltext = "35142-07.html";
				}
				else if ((qs.getCond() > 5) && (qs.getCond() < 9))
				{
					htmltext = "35142-08.html";
				}
				else if (qs.isCond(9))
				{
					htmltext = "35142-09.html";
				}
				break;
			}
			case ROUKE:
			{
				if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(3, qs))
				{
					if (castleOwner.getQuestState(getName()).getInt("confidant") == qs.getPlayer().getObjectId())
					{
						htmltext = "31418-01.html";
					}
				}
				else if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(8, qs))
				{
					if (getQuestItemsCount(talker, MANDRAGORA_ROOT) >= 100)
					{
						htmltext = "31418-04.html";
					}
					else
					{
						htmltext = "31418-04a.html";
					}
				}
				else if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(9, qs))
				{
					htmltext = "31418-06.html";
				}
				break;
			}
			case SOPHYA:
			{
				if (qs.isCond(5))
				{
					htmltext = "30735-01.html";
				}
				else if (qs.isCond(6))
				{
					htmltext = "30735-03.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "30735-04.html";
				}
				else if (qs.isCond(8))
				{
					htmltext = "30735-06.html";
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
			if (fortress.getContractedCastleId() == DION_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isLordAvailable(int cond, QuestState qs)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(DION_CASTLE);
		final Clan owner = castle.getOwner();
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		return (owner != null) && (castleOwner != null) && (castleOwner != qs.getPlayer()) && (owner == qs.getPlayer().getClan()) && (castleOwner.getQuestState(getName()) != null) && castleOwner.getQuestState(getName()).isCond(cond);
	}
}