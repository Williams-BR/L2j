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
package quests.Q00713_PathToBecomingALordAden;

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
 * Path to Becoming a Lord - Aden (713)
 * @author Sacrifice
 */
public final class Q00713_PathToBecomingALordAden extends Quest
{
	private static final int LOGAN = 35274;
	private static final int ORVEN = 30857;
	
	private static final int[] MOBS =
	{
		20669, // Taik Orc Supply Leader
		20665 // Taik Orc Supply
	};
	
	private static final int ADEN_CASTLE = 5;
	
	public Q00713_PathToBecomingALordAden()
	{
		super(713);
		addStartNpc(LOGAN);
		addKillId(MOBS);
		addTalkId(LOGAN, ORVEN);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(ADEN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		switch (event)
		{
			case "35274-02.html":
			{
				qs.startQuest();
				break;
			}
			case "30857-03.html":
			{
				qs.setCond(2);
				break;
			}
			case "35274-05.html":
			{
				if (castle.getOwner().getLeader().getPlayerInstance() != null)
				{
					final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_ADEN_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_ADEN);
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
		if ((qs != null) && qs.isCond(4))
		{
			if (qs.getInt("mobs") < 100)
			{
				qs.set("mobs", String.valueOf(qs.getInt("mobs") + 1));
			}
			else
			{
				qs.setCond(5);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final Castle castle = CastleManager.getInstance().getCastleById(ADEN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case LOGAN:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35274-01.html";
						}
						else
						{
							htmltext = "35274-00.html";
							qs.exitQuest(true);
						}
					}
					else
					{
						htmltext = "35274-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					htmltext = "35274-03.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "35274-04.html";
				}
				break;
			}
			case ORVEN:
			{
				if (qs.isCond(1))
				{
					htmltext = "30857-01.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "30857-04.html";
				}
				else if (qs.isCond(4))
				{
					htmltext = "30857-05.html";
				}
				else if (qs.isCond(5))
				{
					qs.setCond(7);
					htmltext = "30857-06.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "30857-06.html";
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
			if (fortress.getContractedCastleId() == ADEN_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
}