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
package quests.TerritoryWarScripts;

import java.util.Calendar;

import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager.TerritoryNPCSpawn;
import org.l2jmobius.gameserver.model.TerritoryWard;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.Util;

import quests.Q00147_PathtoBecominganEliteMercenary.Q00147_PathtoBecominganEliteMercenary;
import quests.Q00148_PathtoBecominganExaltedMercenary.Q00148_PathtoBecominganExaltedMercenary;
import quests.Q00176_StepsForHonor.Q00176_StepsForHonor;

/**
 * Territory War quests superclass.
 * @author Gigiikun
 */
public class TerritoryWarSuperClass extends Quest
{
	// "For the Sake of the Territory ..." quests variables
	public int CATAPULT_ID;
	public int TERRITORY_ID;
	public int[] LEADER_IDS;
	public int[] GUARD_IDS;
	public NpcStringId[] npcString = {};
	// "Protect the ..." quests variables
	public int[] NPC_IDS;
	// "Kill The ..."
	public int[] CLASS_IDS;
	public int RANDOM_MIN;
	public int RANDOM_MAX;
	
	public TerritoryWarSuperClass(int questId)
	{
		super(questId);
		
		if (questId < 0)
		{
			// Outpost and Ward handled by the Super Class script.
			addSkillSeeId(36590);
			
			// Calculate next TW date.
			TerritoryWarManager.getInstance().setNextTWDate();
			LOGGER.info(getClass().getSimpleName() + ": Siege date: " + TerritoryWarManager.getInstance().getTWStart().getTime());
		}
	}
	
	public int getTerritoryIdForThisNPCId(int npcid)
	{
		return 0; // TODO: Implement this.
	}
	
	private void handleKillTheQuest(PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		int kill = 1;
		int max = 10;
		if (!qs.isCompleted())
		{
			if (!qs.isStarted())
			{
				qs.setState(State.STARTED);
				qs.setCond(1);
				qs.set("kill", "1");
				max = getRandom(RANDOM_MIN, RANDOM_MAX);
				qs.set("max", String.valueOf(max));
			}
			else
			{
				kill = qs.getInt("kill") + 1;
				max = qs.getInt("max");
			}
			if (kill >= max)
			{
				TerritoryWarManager.getInstance().giveTWQuestPoint(player);
				addExpAndSp(player, 534000, 51000);
				qs.set("doneDate", String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)));
				qs.exitQuest(true);
				player.sendPacket(new ExShowScreenMessage(npcString[1], 2, 10000));
			}
			else
			{
				qs.set("kill", String.valueOf(kill));
				
				final ExShowScreenMessage message = new ExShowScreenMessage(npcString[0], 2, 10000);
				message.addStringParameter(String.valueOf(max));
				message.addStringParameter(String.valueOf(kill));
				player.sendPacket(message);
				
			}
		}
		else if (qs.getInt("doneDate") != Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
		{
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.set("kill", "1");
			max = getRandom(RANDOM_MIN, RANDOM_MAX);
			qs.set("max", String.valueOf(max));
			
			final ExShowScreenMessage message = new ExShowScreenMessage(npcString[0], 2, 10000);
			message.addStringParameter(String.valueOf(max));
			message.addStringParameter(String.valueOf(kill));
			player.sendPacket(message);
		}
		else if (player.isGM())
		{
			// just for test
			player.sendMessage("Cleaning " + getName() + " Territory War quest by force!");
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.set("kill", "1");
			max = getRandom(RANDOM_MIN, RANDOM_MAX);
			qs.set("max", String.valueOf(max));
			
			final ExShowScreenMessage message = new ExShowScreenMessage(npcString[0], 2, 10000);
			message.addStringParameter(String.valueOf(max));
			message.addStringParameter(String.valueOf(kill));
			player.sendPacket(message);
		}
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance player, int damage, boolean isSummon)
	{
		if ((npc.getCurrentHp() == npc.getMaxHp()) && CommonUtil.contains(NPC_IDS, npc.getId()))
		{
			final int territoryId = getTerritoryIdForThisNPCId(npc.getId());
			if ((territoryId >= 81) && (territoryId <= 89))
			{
				for (PlayerInstance pl : World.getInstance().getPlayers())
				{
					if (pl.getSiegeSide() == territoryId)
					{
						QuestState qs = pl.getQuestState(getName());
						if (qs == null)
						{
							qs = newQuestState(pl);
						}
						if (!qs.isStarted())
						{
							qs.setState(State.STARTED, false);
							qs.setCond(1);
						}
					}
				}
			}
		}
		return super.onAttack(npc, player, damage, isSummon);
	}
	
	@Override
	public String onDeath(Creature killer, Creature victim, QuestState qs)
	{
		if ((killer == victim) || !victim.isPlayer() || (victim.getLevel() < 61))
		{
			return "";
		}
		final PlayerInstance actingPlayer = killer.getActingPlayer();
		if ((actingPlayer != null) && (qs.getPlayer() != null))
		{
			if (actingPlayer.getParty() != null)
			{
				for (PlayerInstance pl : actingPlayer.getParty().getMembers())
				{
					if ((pl.getSiegeSide() == qs.getPlayer().getSiegeSide()) || (pl.getSiegeSide() == 0) || !Util.checkIfInRange(2000, killer, pl, false))
					{
						continue;
					}
					if (pl == actingPlayer)
					{
						handleStepsForHonor(actingPlayer);
						handleBecomeMercenaryQuest(actingPlayer, false);
					}
					handleKillTheQuest(pl);
				}
			}
			else if ((actingPlayer.getSiegeSide() != qs.getPlayer().getSiegeSide()) && (actingPlayer.getSiegeSide() > 0))
			{
				handleKillTheQuest(actingPlayer);
				handleStepsForHonor(actingPlayer);
				handleBecomeMercenaryQuest(actingPlayer, false);
			}
			TerritoryWarManager.getInstance().giveTWPoint(actingPlayer, qs.getPlayer().getSiegeSide(), 1);
		}
		return "";
	}
	
	@Override
	public String onEnterWorld(PlayerInstance player)
	{
		final int territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(player);
		if (territoryId > 0)
		{
			// register Territory Quest
			final TerritoryWarSuperClass territoryQuest = TerritoryWarSuperClassLoader.getForTheSakeScripts().get(territoryId);
			QuestState qs = player.getQuestState(territoryQuest.getName());
			if (qs == null)
			{
				qs = territoryQuest.newQuestState(player);
			}
			qs.setState(State.STARTED, false);
			qs.setCond(1);
			
			// register player on Death
			if (player.getLevel() >= 61)
			{
				final TerritoryWarSuperClass killthe = TerritoryWarSuperClassLoader.getKillTheScripts().get(player.getClassId().getId());
				if (killthe != null)
				{
					qs = player.getQuestState(killthe.getName());
					if (qs == null)
					{
						qs = killthe.newQuestState(player);
					}
					player.addNotifyQuestOfDeath(qs);
				}
				else
				{
					LOGGER.warning("TerritoryWar: Missing Kill the quest for player " + player.getName() + " whose class id: " + player.getClassId().getId());
				}
			}
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final TerritoryWarManager manager = TerritoryWarManager.getInstance();
		if (npc.getId() == CATAPULT_ID)
		{
			manager.territoryCatapultDestroyed(TERRITORY_ID - 80);
			manager.giveTWPoint(killer, TERRITORY_ID, 4);
			manager.announceToParticipants(new ExShowScreenMessage(npcString[0], 2, 10000), 135000, 13500);
			handleBecomeMercenaryQuest(killer, true);
		}
		else if (CommonUtil.contains(LEADER_IDS, npc.getId()))
		{
			manager.giveTWPoint(killer, TERRITORY_ID, 3);
		}
		
		if ((killer.getSiegeSide() != TERRITORY_ID) && (TerritoryWarManager.getInstance().getTerritory(killer.getSiegeSide() - 80) != null))
		{
			manager.getTerritory(killer.getSiegeSide() - 80).getQuestDone()[0]++;
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSkillSee(Npc npc, PlayerInstance caster, Skill skill, WorldObject[] targets, boolean isSummon)
	{
		if (CommonUtil.contains(targets, npc))
		{
			if (skill.getId() == 845)
			{
				if (TerritoryWarManager.getInstance().getHQForClan(caster.getClan()) != npc)
				{
					return super.onSkillSee(npc, caster, skill, targets, isSummon);
				}
				npc.deleteMe();
				TerritoryWarManager.getInstance().setHQForClan(caster.getClan(), null);
			}
			else if (skill.getId() == 847)
			{
				if (TerritoryWarManager.getInstance().getHQForTerritory(caster.getSiegeSide()) != npc)
				{
					return super.onSkillSee(npc, caster, skill, targets, isSummon);
				}
				final TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(caster);
				if (ward == null)
				{
					return super.onSkillSee(npc, caster, skill, targets, isSummon);
				}
				if ((caster.getSiegeSide() - 80) == ward.getOwnerCastleId())
				{
					for (TerritoryNPCSpawn wardSpawn : TerritoryWarManager.getInstance().getTerritory(ward.getOwnerCastleId()).getOwnedWard())
					{
						if (wardSpawn.getId() == ward.getTerritoryId())
						{
							wardSpawn.setNPC(wardSpawn.getNpc().getSpawn().doSpawn());
							ward.unSpawnMe();
							ward.setNpc(wardSpawn.getNpc());
						}
					}
				}
				else
				{
					ward.unSpawnMe();
					ward.setNpc(TerritoryWarManager.getInstance().addTerritoryWard(ward.getTerritoryId(), caster.getSiegeSide() - 80, ward.getOwnerCastleId(), true));
					ward.setOwnerCastleId(caster.getSiegeSide() - 80);
					TerritoryWarManager.getInstance().getTerritory(caster.getSiegeSide() - 80).getQuestDone()[1]++;
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}
	
	// Used to register NPCs "For the Sake of the Territory ..." quests
	public void registerKillIds()
	{
		addKillId(CATAPULT_ID);
		for (int mobid : LEADER_IDS)
		{
			addKillId(mobid);
		}
		for (int mobid : GUARD_IDS)
		{
			addKillId(mobid);
		}
	}
	
	@Override
	public void setOnEnterWorld(boolean value)
	{
		super.setOnEnterWorld(value);
		
		for (PlayerInstance player : World.getInstance().getPlayers())
		{
			if (player.getSiegeSide() > 0)
			{
				final TerritoryWarSuperClass territoryQuest = TerritoryWarSuperClassLoader.getForTheSakeScripts().get(player.getSiegeSide());
				if (territoryQuest == null)
				{
					continue;
				}
				
				QuestState qs = player.hasQuestState(territoryQuest.getName()) ? player.getQuestState(territoryQuest.getName()) : territoryQuest.newQuestState(player);
				if (value)
				{
					qs.setState(State.STARTED, false);
					qs.setCond(1);
					// register player on Death
					if (player.getLevel() >= 61)
					{
						final TerritoryWarSuperClass killthe = TerritoryWarSuperClassLoader.getKillTheScripts().get(player.getClassId().getId());
						if (killthe != null)
						{
							qs = player.getQuestState(killthe.getName());
							if (qs == null)
							{
								qs = killthe.newQuestState(player);
							}
							player.addNotifyQuestOfDeath(qs);
						}
						else
						{
							LOGGER.warning("TerritoryWar: Missing Kill the quest for player " + player.getName() + " whose class id: " + player.getClassId().getId());
						}
					}
				}
				else
				{
					qs.exitQuest(false);
					for (Quest q : TerritoryWarSuperClassLoader.getProtectTheScripts().values())
					{
						qs = player.getQuestState(q.getName());
						if (qs != null)
						{
							qs.exitQuest(false);
						}
					}
					// unregister player on Death
					final TerritoryWarSuperClass killthe = TerritoryWarSuperClassLoader.getKillTheScripts().get(player.getClassIndex());
					if (killthe != null)
					{
						qs = player.getQuestState(killthe.getName());
						if (qs != null)
						{
							player.removeNotifyQuestOfDeath(qs);
						}
					}
				}
			}
		}
	}
	
	private void handleBecomeMercenaryQuest(PlayerInstance player, boolean catapult)
	{
		int enemyCount = 10;
		int catapultCount = 1;
		QuestState qs = player.getQuestState(Q00147_PathtoBecominganEliteMercenary.class.getSimpleName());
		if ((qs != null) && qs.isCompleted())
		{
			qs = player.getQuestState(Q00148_PathtoBecominganExaltedMercenary.class.getSimpleName());
			enemyCount = 30;
			catapultCount = 2;
		}
		
		if ((qs != null) && qs.isStarted())
		{
			final int cond = qs.getCond();
			if (catapult)
			{
				if ((cond == 1) || (cond == 2))
				{
					final int count = qs.getInt("catapult") + 1;
					qs.set("catapult", String.valueOf(count));
					if (count >= catapultCount)
					{
						qs.setCond((cond == 1) ? 3 : 4);
					}
				}
			}
			else if ((cond == 1) || (cond == 3))
			{
				final int kills = qs.getInt("kills") + 1;
				qs.set("kills", Integer.toString(kills));
				if (kills >= enemyCount)
				{
					qs.setCond((cond == 1) ? 2 : 4);
				}
			}
		}
	}
	
	private void handleStepsForHonor(PlayerInstance player)
	{
		final QuestState _sfh = player.getQuestState(Q00176_StepsForHonor.class.getSimpleName());
		if ((_sfh != null) && _sfh.isStarted())
		{
			final int cond = _sfh.getCond();
			if ((cond == 1) || (cond == 3) || (cond == 5) || (cond == 7))
			{
				final int kills = _sfh.getInt("kills") + 1;
				_sfh.set("kills", kills);
				if ((cond == 1) && (kills >= 9))
				{
					_sfh.setCond(2);
					_sfh.set("kills", "0");
				}
				else if ((cond == 3) && (kills >= 18))
				{
					_sfh.setCond(4);
					_sfh.set("kills", "0");
				}
				else if ((cond == 5) && (kills >= 27))
				{
					_sfh.setCond(6);
					_sfh.set("kills", "0");
				}
				else if ((cond == 7) && (kills >= 36))
				{
					_sfh.setCond(8);
					_sfh.unset("kills");
				}
			}
		}
	}
}
