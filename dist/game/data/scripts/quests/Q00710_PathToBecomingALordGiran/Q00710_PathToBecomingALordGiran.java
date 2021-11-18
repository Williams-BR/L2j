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
package quests.Q00710_PathToBecomingALordGiran;

import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Path to Becoming a Lord - Giran (710)
 * @author Sacrifice
 */
public final class Q00710_PathToBecomingALordGiran extends Quest
{
	private static final int SAUL = 35184;
	private static final int GESTO = 30511;
	private static final int FELTON = 30879;
	private static final int CARGO_BOX = 32243;
	
	private static final int FREIGHT_CHESTS_SEAL = 13014;
	private static final int GESTOS_BOX = 13013;
	
	private static final int[] MOBS =
	{
		20832, // Zaken's Pikeman
		20833, // Zaken's Archer
		20835, // Zaken's Seer
		21602, // Zaken's Pikeman
		21603, // Zaken's Pikeman
		21604, // Zaken's Elite Pikeman
		21605, // Zaken's Archer
		21606, // Zaken's Archer
		21607, // Zaken's Elite Archer
		21608, // Zaken's Watchman
		21609 // Zaken's Watchman
	};
	
	private static final int GIRAN_CASTLE = 3;
	
	public Q00710_PathToBecomingALordGiran()
	{
		super(710);
		addStartNpc(SAUL);
		addKillId(MOBS);
		addTalkId(SAUL, GESTO, FELTON, CARGO_BOX);
		_questItemIds = new int[]
		{
			FREIGHT_CHESTS_SEAL,
			GESTOS_BOX
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(GIRAN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		switch (event)
		{
			case "35184-03.html":
			{
				qs.startQuest();
				break;
			}
			case "30511-03.html":
			{
				qs.setCond(3);
				break;
			}
			case "30879-02.html":
			{
				qs.setCond(4);
				break;
			}
			case "35184-07.html":
			{
				if (castle.getOwner().getLeader().getPlayerInstance() != null)
				{
					final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_GIRAN_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_GIRAN);
					packet.addStringParameter(player.getName());
					npc.broadcastPacket(packet);
					qs.exitQuest(true, true);
				}
				break;
			}
		}
		return event;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = killer.getQuestState(getName());
		if ((qs != null) && qs.isCond(7))
		{
			if (getQuestItemsCount(killer, GESTOS_BOX) < 300)
			{
				giveItems(killer, GESTOS_BOX, 1);
			}
			
			if (getQuestItemsCount(killer, GESTOS_BOX) >= 300)
			{
				qs.setCond(8);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(GIRAN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case SAUL:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35184-01.html";
						}
						else
						{
							htmltext = "35184-00.html";
							qs.exitQuest(true);
						}
					}
					else
					{
						htmltext = "35184-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					qs.setCond(2);
					htmltext = "35184-04.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "35184-05.html";
				}
				else if (qs.isCond(9))
				{
					htmltext = "35184-06.html";
				}
				break;
			}
			case GESTO:
			{
				if (qs.isCond(2))
				{
					htmltext = "30511-01.html";
				}
				else if (qs.isCond(3) || qs.isCond(4))
				{
					htmltext = "30511-04.html";
				}
				else if (qs.isCond(5))
				{
					takeItems(talker, FREIGHT_CHESTS_SEAL, -1);
					qs.setCond(7);
					htmltext = "30511-05.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "30511-06.html";
				}
				else if (qs.isCond(8))
				{
					takeItems(talker, GESTOS_BOX, -1);
					qs.setCond(9);
					htmltext = "30511-07.html";
				}
				else if (qs.isCond(9))
				{
					htmltext = "30511-07.html";
				}
				break;
			}
			case FELTON:
			{
				if (qs.isCond(3))
				{
					htmltext = "30879-01.html";
				}
				else if (qs.isCond(4))
				{
					htmltext = "30879-03.html";
				}
				break;
			}
			case CARGO_BOX:
			{
				if (qs.isCond(4))
				{
					qs.setCond(5);
					giveItems(talker, FREIGHT_CHESTS_SEAL, 1);
					htmltext = "32243-01.html";
				}
				else if (qs.isCond(5))
				{
					htmltext = "32243-02.html";
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
			if (fortress.getContractedCastleId() == GIRAN_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
}