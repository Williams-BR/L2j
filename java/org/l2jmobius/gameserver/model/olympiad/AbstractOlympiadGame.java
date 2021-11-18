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
package org.l2jmobius.gameserver.model.olympiad;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.instancemanager.AntiFeedManager;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.Party.MessageType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.type.OlympiadStadiumZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMode;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author godson, GodKratos, Pere, DS
 */
public abstract class AbstractOlympiadGame
{
	protected static final Logger LOGGER = Logger.getLogger(AbstractOlympiadGame.class.getName());
	protected static final Logger LOGGER_OLYMPIAD = Logger.getLogger("olympiad");
	
	protected static final String POINTS = "olympiad_points";
	protected static final String COMP_DONE = "competitions_done";
	protected static final String COMP_WON = "competitions_won";
	protected static final String COMP_LOST = "competitions_lost";
	protected static final String COMP_DRAWN = "competitions_drawn";
	protected static final String COMP_DONE_WEEK = "competitions_done_week";
	protected static final String COMP_DONE_WEEK_CLASSED = "competitions_done_week_classed";
	protected static final String COMP_DONE_WEEK_NON_CLASSED = "competitions_done_week_non_classed";
	protected static final String COMP_DONE_WEEK_TEAM = "competitions_done_week_team";
	
	protected long _startTime = 0;
	protected boolean _aborted = false;
	protected final int _stadiumID;
	
	protected AbstractOlympiadGame(int id)
	{
		_stadiumID = id;
	}
	
	public boolean isAborted()
	{
		return _aborted;
	}
	
	public int getStadiumId()
	{
		return _stadiumID;
	}
	
	protected boolean makeCompetitionStart()
	{
		_startTime = Chronos.currentTimeMillis();
		return !_aborted;
	}
	
	protected final void addPointsToParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, points);
		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
		sm.addString(par.getName());
		sm.addInt(points);
		broadcastPacket(sm);
	}
	
	protected final void removePointsFromParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, -points);
		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
		sm.addString(par.getName());
		sm.addInt(points);
		broadcastPacket(sm);
	}
	
	/**
	 * Function return null if player passed all checks or SystemMessage with reason for broadcast to opponent(s).
	 * @param player
	 * @return
	 */
	protected static SystemMessage checkDefaulted(PlayerInstance player)
	{
		if ((player == null) || !player.isOnline())
		{
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		if ((player.getClient() == null) || player.getClient().isDetached())
		{
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		// safety precautions
		if (player.inObserverMode() || player.isRegisteredOnEvent())
		{
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		SystemMessage sm;
		if (player.isDead())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_DEAD_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(player);
			player.sendPacket(sm);
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		if (player.isSubClassActive())
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOU_HAVE_CHANGED_TO_YOUR_SUB_CLASS);
			sm.addPcName(player);
			player.sendPacket(sm);
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		if (player.isCursedWeaponEquipped())
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_THE_OWNER_OF_S2_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(player);
			sm.addItemName(player.getCursedWeaponEquippedId());
			player.sendPacket(sm);
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		if (!player.isInventoryUnder90(true))
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOUR_INVENTORY_SLOT_EXCEEDS_80);
			sm.addPcName(player);
			player.sendPacket(sm);
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		return null;
	}
	
	protected static boolean portPlayerToArena(Participant par, Location loc, int id)
	{
		final PlayerInstance player = par.getPlayer();
		if ((player == null) || !player.isOnline())
		{
			return false;
		}
		
		try
		{
			player.setLastLocation();
			if (player.isSitting())
			{
				player.standUp();
			}
			player.setTarget(null);
			
			player.setOlympiadGameId(id);
			player.setInOlympiadMode(true);
			player.setOlympiadStart(false);
			player.setOlympiadSide(par.getSide());
			player.setOlympiadBuffCount(Config.ALT_OLY_MAX_BUFFS);
			loc.setInstanceId(OlympiadGameManager.getInstance().getOlympiadTask(id).getZone().getInstanceId());
			player.teleToLocation(loc, false);
			player.sendPacket(new ExOlympiadMode(2));
			// FIXME: Make sure player has no buffs.
			cleanEffects(player);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	protected void removals(PlayerInstance player, boolean removeParty)
	{
		try
		{
			if (player == null)
			{
				return;
			}
			
			// Remove Buffs
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			
			// Remove Clan Skills
			if (player.getClan() != null)
			{
				player.getClan().removeSkillEffects(player);
				if (player.getClan().getCastleId() > 0)
				{
					CastleManager.getInstance().getCastleByOwner(player.getClan()).removeResidentialSkills(player);
				}
				if (player.getClan().getFortId() > 0)
				{
					FortManager.getInstance().getFortByOwner(player.getClan()).removeResidentialSkills(player);
				}
			}
			// Abort casting if player casting
			player.abortAttack();
			player.abortCast();
			
			// Force the character to be visible
			player.setInvisible(false);
			
			// Heal Player fully
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			
			// Remove Summon's Buffs
			if (player.hasSummon())
			{
				final Summon summon = player.getSummon();
				summon.stopAllEffectsExceptThoseThatLastThroughDeath();
				summon.abortAttack();
				summon.abortCast();
				
				if (summon.isPet())
				{
					summon.unSummon(player);
				}
			}
			
			// stop any cubic that has been given by other player.
			player.stopCubicsByOthers();
			
			// Remove player from his party
			if (removeParty)
			{
				final Party party = player.getParty();
				if (party != null)
				{
					party.removePartyMember(player, MessageType.EXPELLED);
				}
			}
			// Remove Agathion
			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
				player.broadcastUserInfo();
			}
			
			player.checkItemRestriction();
			
			// Remove shot automation
			player.disableAutoShotsAll();
			
			// Discharge any active shots
			final ItemInstance item = player.getActiveWeaponInstance();
			if (item != null)
			{
				item.unChargeAllShots();
			}
			
			// enable skills with cool time <= 15 minutes
			for (Skill skill : player.getAllSkills())
			{
				if (skill.getReuseDelay() <= 900000)
				{
					player.enableSkill(skill);
				}
			}
			
			player.sendSkillList();
			player.sendPacket(new SkillCoolTime(player));
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	protected static void cleanEffects(PlayerInstance player)
	{
		try
		{
			// prevent players kill each other
			player.setOlympiadStart(false);
			player.setTarget(null);
			player.abortAttack();
			player.abortCast();
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			
			if (player.isDead())
			{
				player.setDead(false);
			}
			
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			player.clearSouls();
			player.clearCharges();
			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
			}
			final Summon summon = player.getSummon();
			if ((summon != null) && !summon.isDead())
			{
				summon.setTarget(null);
				summon.abortAttack();
				summon.abortCast();
				summon.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				summon.stopAllEffectsExceptThoseThatLastThroughDeath();
			}
			
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.getStatus().startHpMpRegeneration();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	protected void playerStatusBack(PlayerInstance player)
	{
		try
		{
			if (player.isTransformed())
			{
				player.untransform();
			}
			
			if (player.isInOlympiadMode())
			{
				player.sendPacket(new ExOlympiadMode(0));
			}
			
			player.setInOlympiadMode(false);
			player.setOlympiadStart(false);
			player.setOlympiadSide(-1);
			player.setOlympiadGameId(-1);
			
			// Add Clan Skills
			if (player.getClan() != null)
			{
				player.getClan().addSkillEffects(player);
				if (player.getClan().getCastleId() > 0)
				{
					CastleManager.getInstance().getCastleByOwner(player.getClan()).giveResidentialSkills(player);
				}
				if (player.getClan().getFortId() > 0)
				{
					FortManager.getInstance().getFortByOwner(player.getClan()).giveResidentialSkills(player);
				}
				player.sendSkillList();
			}
			
			// heal again after adding clan skills
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.getStatus().startHpMpRegeneration();
			
			if (Config.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, player);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "portPlayersToArena()", e);
		}
	}
	
	protected void portPlayerBack(PlayerInstance player)
	{
		if (player == null)
		{
			return;
		}
		final Location loc = player.getLastLocation();
		if ((loc.getX() == 0) && (loc.getY() == 0))
		{
			return;
		}
		player.setIsPendingRevive(false);
		player.setInstanceId(0);
		player.teleToLocation(loc);
		player.unsetLastLocation();
	}
	
	public static void rewardParticipant(PlayerInstance player, int[][] reward)
	{
		if ((player == null) || !player.isOnline() || (reward == null))
		{
			return;
		}
		
		try
		{
			SystemMessage sm;
			ItemInstance item;
			final InventoryUpdate iu = new InventoryUpdate();
			for (int[] it : reward)
			{
				if ((it == null) || (it.length != 2))
				{
					continue;
				}
				
				item = player.getInventory().addItem("Olympiad", it[0], it[1], player, null);
				if (item == null)
				{
					continue;
				}
				
				iu.addModifiedItem(item);
				sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
				sm.addItemName(it[0]);
				sm.addInt(it[1]);
				player.sendPacket(sm);
			}
			player.sendPacket(iu);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	public abstract CompetitionType getType();
	
	public abstract String[] getPlayerNames();
	
	public abstract boolean containsParticipant(int playerId);
	
	public abstract void sendOlympiadInfo(Creature creature);
	
	public abstract void broadcastOlympiadInfo(OlympiadStadiumZone stadium);
	
	protected abstract void broadcastPacket(IClientOutgoingPacket packet);
	
	protected abstract boolean needBuffers();
	
	protected abstract boolean checkDefaulted();
	
	protected abstract void removals();
	
	protected abstract boolean portPlayersToArena(List<Location> spawns);
	
	protected abstract void cleanEffects();
	
	protected abstract void portPlayersBack();
	
	protected abstract void playersStatusBack();
	
	protected abstract void clearPlayers();
	
	protected abstract void handleDisconnect(PlayerInstance player);
	
	protected abstract void resetDamage();
	
	protected abstract void addDamage(PlayerInstance player, int damage);
	
	protected abstract boolean checkBattleStatus();
	
	protected abstract boolean haveWinner();
	
	protected abstract void validateWinner(OlympiadStadiumZone stadium);
	
	protected abstract int getDivider();
	
	protected abstract int[][] getReward();
	
	protected abstract String getWeeklyMatchType();
	
	protected abstract void makePlayersInvul();
	
	protected abstract void removePlayersInvul();
}