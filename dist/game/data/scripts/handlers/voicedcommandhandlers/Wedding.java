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
package handlers.voicedcommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.PlayerAction;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.instancemanager.CoupleManager;
import org.l2jmobius.gameserver.instancemanager.GrandBossManager;
import org.l2jmobius.gameserver.instancemanager.SiegeManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.skills.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Wedding voiced commands handler.
 * @author evill33t
 */
public class Wedding implements IVoicedCommandHandler
{
	static final Logger LOGGER = Logger.getLogger(Wedding.class.getName());
	private static final String[] _voicedCommands =
	{
		"divorce",
		"engage",
		"gotolove"
	};
	
	@Override
	public boolean useVoicedCommand(String command, PlayerInstance activeChar, String params)
	{
		if (activeChar == null)
		{
			return false;
		}
		if (command.startsWith("engage"))
		{
			return engage(activeChar);
		}
		else if (command.startsWith("divorce"))
		{
			return divorce(activeChar);
		}
		else if (command.startsWith("gotolove"))
		{
			return goToLove(activeChar);
		}
		return false;
	}
	
	public boolean divorce(PlayerInstance activeChar)
	{
		if (activeChar.getPartnerId() == 0)
		{
			return false;
		}
		
		final int partnerId = activeChar.getPartnerId();
		final int coupleId = activeChar.getCoupleId();
		long adenaAmount = 0;
		if (activeChar.isMarried())
		{
			activeChar.sendMessage("You are now divorced.");
			adenaAmount = (activeChar.getAdena() / 100) * Config.WEDDING_DIVORCE_COSTS;
			activeChar.getInventory().reduceAdena("Wedding", adenaAmount, activeChar, null);
		}
		else
		{
			activeChar.sendMessage("You have broken up as a couple.");
		}
		
		final PlayerInstance partner = World.getInstance().getPlayer(partnerId);
		if (partner != null)
		{
			partner.setPartnerId(0);
			if (partner.isMarried())
			{
				partner.sendMessage("Your spouse has decided to divorce you.");
			}
			else
			{
				partner.sendMessage("Your fiance has decided to break the engagement with you.");
			}
			
			// give adena
			if (adenaAmount > 0)
			{
				partner.addAdena("WEDDING", adenaAmount, null, false);
			}
		}
		CoupleManager.getInstance().deleteCouple(coupleId);
		return true;
	}
	
	public boolean engage(PlayerInstance activeChar)
	{
		if (activeChar.getTarget() == null)
		{
			activeChar.sendMessage("You have no one targeted.");
			return false;
		}
		else if (!activeChar.getTarget().isPlayer())
		{
			activeChar.sendMessage("You can only ask another player to engage you.");
			return false;
		}
		else if (activeChar.getPartnerId() != 0)
		{
			activeChar.sendMessage("You are already engaged.");
			if (Config.WEDDING_PUNISH_INFIDELITY)
			{
				activeChar.startAbnormalVisualEffect(true, AbnormalVisualEffect.BIG_HEAD); // give player a Big Head
				// lets recycle the sevensigns debuffs
				int skillId;
				int skillLevel = 1;
				if (activeChar.getLevel() > 40)
				{
					skillLevel = 2;
				}
				
				if (activeChar.isMageClass())
				{
					skillId = 4362;
				}
				else
				{
					skillId = 4361;
				}
				
				final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
				if (!activeChar.isAffectedBySkill(skillId))
				{
					skill.applyEffects(activeChar, activeChar);
				}
			}
			return false;
		}
		final PlayerInstance ptarget = (PlayerInstance) activeChar.getTarget();
		// check if player target himself
		if (ptarget.getObjectId() == activeChar.getObjectId())
		{
			activeChar.sendMessage("Is there something wrong with you, are you trying to go out with youself?");
			return false;
		}
		
		if (ptarget.isMarried())
		{
			activeChar.sendMessage("Player already married.");
			return false;
		}
		
		if (ptarget.isEngageRequest())
		{
			activeChar.sendMessage("Player already asked by someone else.");
			return false;
		}
		
		if (ptarget.getPartnerId() != 0)
		{
			activeChar.sendMessage("Player already engaged with someone else.");
			return false;
		}
		
		if ((ptarget.getAppearance().isFemale() == activeChar.getAppearance().isFemale()) && !Config.WEDDING_SAMESEX)
		{
			activeChar.sendMessage("Gay marriage is not allowed on this server!");
			return false;
		}
		
		// check if target has player on friendlist
		boolean foundOnFriendList = false;
		int objectId;
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=?");
			statement.setInt(1, ptarget.getObjectId());
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				objectId = rset.getInt("friendId");
				if (objectId == activeChar.getObjectId())
				{
					foundOnFriendList = true;
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning("could not read friend data:" + e);
		}
		
		if (!foundOnFriendList)
		{
			activeChar.sendMessage("The player you want to ask is not on your friends list, you must first be on each others friends list before you choose to engage.");
			return false;
		}
		
		ptarget.setEngageRequest(true, activeChar.getObjectId());
		ptarget.addAction(PlayerAction.USER_ENGAGE);
		
		final ConfirmDlg dlg = new ConfirmDlg(activeChar.getName() + " is asking to engage you. Do you want to start a new relationship?");
		dlg.addTime(15 * 1000);
		ptarget.sendPacket(dlg);
		return true;
	}
	
	public boolean goToLove(PlayerInstance activeChar)
	{
		if (!activeChar.isMarried())
		{
			activeChar.sendMessage("You're not married.");
			return false;
		}
		
		if (activeChar.getPartnerId() == 0)
		{
			activeChar.sendMessage("Couldn't find your fiance in the Database - Inform a Gamemaster.");
			LOGGER.severe("Married but couldn't find parter for " + activeChar.getName());
			return false;
		}
		
		if (GrandBossManager.getInstance().getZone(activeChar) != null)
		{
			activeChar.sendMessage("You are inside a Boss Zone.");
			return false;
		}
		
		if (activeChar.isCombatFlagEquipped())
		{
			activeChar.sendMessage("While you are holding a Combat Flag or Territory Ward you can't go to your love!");
			return false;
		}
		
		if (activeChar.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("While you are holding a Cursed Weapon you can't go to your love!");
			return false;
		}
		
		if (GrandBossManager.getInstance().getZone(activeChar) != null)
		{
			activeChar.sendMessage("You are inside a Boss Zone.");
			return false;
		}
		
		if (activeChar.isJailed())
		{
			activeChar.sendMessage("You are in Jail!");
			return false;
		}
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("You are in the Olympiad now.");
			return false;
		}
		
		if (activeChar.isRegisteredOnEvent())
		{
			activeChar.sendMessage("You are registered in an event.");
			return false;
		}
		
		if (activeChar.isInDuel())
		{
			activeChar.sendMessage("You are in a duel!");
			return false;
		}
		
		if (activeChar.inObserverMode())
		{
			activeChar.sendMessage("You are in the observation.");
			return false;
		}
		
		if ((SiegeManager.getInstance().getSiege(activeChar) != null) && SiegeManager.getInstance().getSiege(activeChar).isInProgress())
		{
			activeChar.sendMessage("You are in a siege, you cannot go to your partner.");
			return false;
		}
		
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You are in a festival.");
			return false;
		}
		
		if (activeChar.isInParty() && activeChar.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage("You are in the dimensional rift.");
			return false;
		}
		
		if (activeChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("You are in area which blocks summoning.");
			return false;
		}
		
		final PlayerInstance partner = World.getInstance().getPlayer(activeChar.getPartnerId());
		if ((partner == null) || !partner.isOnline())
		{
			activeChar.sendMessage("Your partner is not online.");
			return false;
		}
		
		if (activeChar.getInstanceId() != partner.getInstanceId())
		{
			activeChar.sendMessage("Your partner is in another World!");
			return false;
		}
		
		if (partner.isJailed())
		{
			activeChar.sendMessage("Your partner is in Jail.");
			return false;
		}
		
		if (partner.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("Your partner is holding a Cursed Weapon and you can't go to your love!");
			return false;
		}
		
		if (GrandBossManager.getInstance().getZone(partner) != null)
		{
			activeChar.sendMessage("Your partner is inside a Boss Zone.");
			return false;
		}
		
		if (partner.isInOlympiadMode())
		{
			activeChar.sendMessage("Your partner is in the Olympiad now.");
			return false;
		}
		
		if (partner.isRegisteredOnEvent())
		{
			activeChar.sendMessage("Your partner is registered in an event.");
			return false;
		}
		
		if (partner.isInDuel())
		{
			activeChar.sendMessage("Your partner is in a duel.");
			return false;
		}
		
		if (partner.isFestivalParticipant())
		{
			activeChar.sendMessage("Your partner is in a festival.");
			return false;
		}
		
		if (partner.isInParty() && partner.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage("Your partner is in dimensional rift.");
			return false;
		}
		
		if (partner.inObserverMode())
		{
			activeChar.sendMessage("Your partner is in the observation.");
			return false;
		}
		
		if ((SiegeManager.getInstance().getSiege(partner) != null) && SiegeManager.getInstance().getSiege(partner).isInProgress())
		{
			activeChar.sendMessage("Your partner is in a siege, you cannot go to your partner.");
			return false;
		}
		
		if (partner.isIn7sDungeon() && !activeChar.isIn7sDungeon())
		{
			final int playerCabal = SevenSigns.getInstance().getPlayerCabal(activeChar.getObjectId());
			final boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
			final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
			if (isSealValidationPeriod)
			{
				if (playerCabal != compWinner)
				{
					activeChar.sendMessage("Your Partner is in a Seven Signs Dungeon and you are not in the winner Cabal!");
					return false;
				}
			}
			else
			{
				if (playerCabal == SevenSigns.CABAL_NULL)
				{
					activeChar.sendMessage("Your Partner is in a Seven Signs Dungeon and you are not registered!");
					return false;
				}
			}
		}
		
		if (partner.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("Your partner is in area which blocks summoning.");
			return false;
		}
		
		final int teleportTimer = Config.WEDDING_TELEPORT_DURATION * 1000;
		activeChar.sendMessage("After " + (teleportTimer / 60000) + " min. you will be teleported to your partner.");
		activeChar.getInventory().reduceAdena("Wedding", Config.WEDDING_TELEPORT_PRICE, activeChar, null);
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		// SoE Animation section
		activeChar.setTarget(activeChar);
		activeChar.disableAllSkills();
		
		Broadcast.toSelfAndKnownPlayersInRadius(activeChar, new MagicSkillUse(activeChar, 1050, 1, teleportTimer, 0), 900);
		activeChar.sendPacket(new SetupGauge(activeChar.getObjectId(), 0, teleportTimer));
		// End SoE Animation section
		
		final EscapeFinalizer ef = new EscapeFinalizer(activeChar, partner.getLocation(), partner.isIn7sDungeon());
		// continue execution later
		activeChar.setSkillCast(ThreadPool.schedule(ef, teleportTimer));
		activeChar.forceIsCasting(GameTimeTaskManager.getInstance().getGameTicks() + (teleportTimer / GameTimeTaskManager.MILLIS_IN_TICK));
		return true;
	}
	
	static class EscapeFinalizer implements Runnable
	{
		private final PlayerInstance _player;
		private final Location _partnerLoc;
		private final boolean _to7sDungeon;
		
		EscapeFinalizer(PlayerInstance activeChar, Location loc, boolean to7sDungeon)
		{
			_player = activeChar;
			_partnerLoc = loc;
			_to7sDungeon = to7sDungeon;
		}
		
		@Override
		public void run()
		{
			if (_player.isDead())
			{
				return;
			}
			
			if ((SiegeManager.getInstance().getSiege(_partnerLoc) != null) && SiegeManager.getInstance().getSiege(_partnerLoc).isInProgress())
			{
				_player.sendMessage("Your partner is in siege, you can't go to your partner.");
				return;
			}
			
			_player.setIn7sDungeon(_to7sDungeon);
			_player.enableAllSkills();
			_player.setCastingNow(false);
			
			try
			{
				_player.teleToLocation(_partnerLoc);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}
