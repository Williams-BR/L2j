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
package quests.Q00716_PathToBecomingALordRune;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

import quests.Q00021_HiddenTruth.Q00021_HiddenTruth;
import quests.Q00025_HidingBehindTheTruth.Q00025_HidingBehindTheTruth;

/**
 * Path to Becoming a Lord - Rune (716)
 * @author Sacrifice
 */
public final class Q00716_PathToBecomingALordRune extends Quest
{
	private static final int FREDERICK = 35509;
	private static final int AGRIPEL = 31348;
	private static final int INNOCENTIN = 31328;
	
	private static final List<Integer> PAGANS = new ArrayList<>();
	static
	{
		for (int i = 22138; i <= 22176; i++)
		{
			PAGANS.add(i);
		}
		for (int i = 22188; i <= 22195; i++)
		{
			PAGANS.add(i);
		}
	}
	
	private static final int RUNE_CASTLE = 8;
	
	public Q00716_PathToBecomingALordRune()
	{
		super(716);
		addStartNpc(FREDERICK);
		addKillId(PAGANS);
		addTalkId(FREDERICK, AGRIPEL, INNOCENTIN);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			return null;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(RUNE_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		switch (event)
		{
			case "35509-03.html":
			{
				qs.startQuest();
				break;
			}
			case "31348-03.html":
			{
				qs.setCond(3);
				break;
			}
			case "35509-08.html":
			{
				castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).set("confidant", String.valueOf(qs.getPlayer().getObjectId()));
				castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).setCond(5);
				qs.setState(State.STARTED);
				break;
			}
			case "31328-03.html":
			{
				if ((castleOwner != null) && (castleOwner != qs.getPlayer()) && (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()) != null) && (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).isCond(5)))
				{
					castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).setCond(6);
				}
				break;
			}
			case "31348-08.html":
			{
				qs.setCond(8);
				break;
			}
		}
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final Castle castle = CastleManager.getInstance().getCastleById(RUNE_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord.";
		}
		
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case FREDERICK:
			{
				switch (qs.getCond())
				{
					case 0:
					{
						if (castleOwner == qs.getPlayer())
						{
							if (!hasFort())
							{
								htmltext = "35509-01.html";
							}
							else
							{
								htmltext = "35509-00.html";
								qs.exitQuest(true);
							}
						}
						else if ((castleOwner != null) && (castleOwner != qs.getPlayer()) && (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()) != null) && (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).isCond(4)))
						{
							if (castleOwner.calculateDistance2D(npc) <= 200)
							{
								htmltext = "35509-07.html";
							}
							else
							{
								htmltext = "35509-07a.html";
							}
						}
						else if (qs.getState() == State.STARTED)
						{
							htmltext = "35509-00b.html";
						}
						else
						{
							htmltext = "35509-00a.html";
							qs.exitQuest(true);
						}
						break;
					}
					case 1:
					{
						final QuestState hidingBehindTheTruth = qs.getPlayer().getQuestState(Q00025_HidingBehindTheTruth.class.getSimpleName());
						final QuestState hiddenTruth = qs.getPlayer().getQuestState(Q00021_HiddenTruth.class.getSimpleName());
						if ((hidingBehindTheTruth != null) && hidingBehindTheTruth.isCompleted() && (hiddenTruth != null) && hiddenTruth.isCompleted())
						{
							qs.setCond(2);
							htmltext = "35509-04.html";
						}
						else
						{
							htmltext = "35509-03.html";
						}
						break;
					}
					case 2:
					{
						htmltext = "35509-04a.html";
						break;
					}
					case 3:
					{
						qs.setCond(4);
						htmltext = "35509-05.html";
						break;
					}
					case 4:
					{
						htmltext = "35509-06.html";
						break;
					}
					case 5:
					{
						htmltext = "35509-09.html";
						break;
					}
					case 6:
					{
						qs.setCond(7);
						htmltext = "35509-10.html";
						break;
					}
					case 7:
					{
						htmltext = "35509-11.html";
						break;
					}
					case 8:
					{
						if (castleOwner != null)
						{
							final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_RUNE_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_RUNE);
							packet.addStringParameter(player.getName());
							npc.broadcastPacket(packet);
							htmltext = "35509-12.html";
							qs.exitQuest(true, true);
						}
						break;
					}
				}
				break;
			}
			case AGRIPEL:
			{
				if (qs.isCond(2))
				{
					htmltext = "31348-01.html";
				}
				else if (qs.isCond(7))
				{
					if ((qs.get("paganCount") != null) && (qs.getInt("paganCount") >= 100))
					{
						htmltext = "31348-07.html";
					}
					else
					{
						htmltext = "31348-04.html";
					}
				}
				else if (qs.isCond(8))
				{
					htmltext = "31348-09.html";
				}
				break;
			}
			case INNOCENTIN:
			{
				if ((qs.getState() == State.STARTED) && qs.isCond(0))
				{
					if ((castleOwner != null) && (castleOwner != qs.getPlayer()) && (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()) != null) && (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).isCond(5)))
					{
						if (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).getInt("confidant") == qs.getPlayer().getObjectId())
						{
							htmltext = "31328-01.html";
						}
						else
						{
							htmltext = "31328-00.html";
						}
					}
					else
					{
						htmltext = "31328-00a.html";
					}
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
		if (qs == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		if (qs.getState() != State.STARTED)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(RUNE_CASTLE);
		final PlayerInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if ((qs.getState() == State.STARTED) && qs.isCond(0) && (castleOwner != null) && (castleOwner != qs.getPlayer()) && (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()) != null) && (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).isCond(7)))
		{
			if (castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).get("paganCount") != null)
			{
				castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).set("paganCount", String.valueOf(castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).getInt("paganCount") + 1));
			}
			else
			{
				castleOwner.getQuestState(Q00716_PathToBecomingALordRune.class.getSimpleName()).set("paganCount", "1");
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private boolean hasFort()
	{
		for (Fort fortress : FortManager.getInstance().getForts())
		{
			if (fortress.getContractedCastleId() == RUNE_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
}