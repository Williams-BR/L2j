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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.instancemanager.CHSiegeManager;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.ClanHallManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.instancemanager.MapRegionManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.SiegeClan;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.SiegeFlagInstance;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.quest.Event;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.GameClient;

/**
 * @version $Revision: 1.7.2.3.2.6 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestRestartPoint implements IClientIncomingPacket
{
	protected int _requestedPointType;
	protected boolean _continuation;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_requestedPointType = packet.readD();
		return true;
	}
	
	class DeathTask implements Runnable
	{
		final PlayerInstance _player;
		
		DeathTask(PlayerInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			portPlayer(_player);
		}
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!player.canRevive())
		{
			return;
		}
		
		if (player.isFakeDeath())
		{
			player.stopFakeDeath(true);
			return;
		}
		else if (!player.isDead())
		{
			return;
		}
		
		// Custom event resurrection management.
		if (player.isOnEvent())
		{
			for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_KILL))
			{
				if (listener.getOwner() instanceof Event)
				{
					((Event) listener.getOwner()).notifyEvent("ResurrectPlayer", null, player);
					return;
				}
			}
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(player.getX(), player.getY(), player.getZ());
		if ((castle != null) && castle.getSiege().isInProgress() && (player.getClan() != null) && castle.getSiege().checkIsAttacker(player.getClan()))
		{
			// Schedule respawn delay for attacker
			ThreadPool.schedule(new DeathTask(player), castle.getSiege().getAttackerRespawnDelay());
			if (castle.getSiege().getAttackerRespawnDelay() > 0)
			{
				player.sendMessage("You will be re-spawned in " + (castle.getSiege().getAttackerRespawnDelay() / 1000) + " seconds");
			}
			return;
		}
		
		portPlayer(player);
	}
	
	protected final void portPlayer(PlayerInstance player)
	{
		Location loc = null;
		Castle castle = null;
		Fort fort = null;
		SiegableHall hall = null;
		final boolean isInDefense = false;
		int instanceId = 0;
		
		// force jail
		if (player.isJailed())
		{
			_requestedPointType = 27;
		}
		else if (player.isFestivalParticipant())
		{
			_requestedPointType = 5;
		}
		switch (_requestedPointType)
		{
			case 1: // to clanhall
			{
				if ((player.getClan() == null) || (player.getClan().getHideoutId() == 0))
				{
					LOGGER.warning("Player [" + player.getName() + "] called RestartPointPacket - To Clanhall and he doesn't have Clanhall!");
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.CLANHALL);
				if ((ClanHallManager.getInstance().getClanHallByOwner(player.getClan()) != null) && (ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null))
				{
					player.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
				}
				break;
			}
			case 2: // to castle
			{
				castle = CastleManager.getInstance().getCastle(player);
				if ((castle != null) && castle.getSiege().isInProgress())
				{
					// Siege in progress
					if (castle.getSiege().checkIsDefender(player.getClan()))
					{
						loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.CASTLE);
					}
					else if (castle.getSiege().checkIsAttacker(player.getClan()))
					{
						loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
					}
					else
					{
						LOGGER.warning("Player [" + player.getName() + "] called RestartPointPacket - To Castle and he doesn't have Castle!");
						return;
					}
				}
				else
				{
					if ((player.getClan() == null) || (player.getClan().getCastleId() == 0))
					{
						return;
					}
					loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.CASTLE);
				}
				if ((CastleManager.getInstance().getCastleByOwner(player.getClan()) != null) && (CastleManager.getInstance().getCastleByOwner(player.getClan()).getFunction(Castle.FUNC_RESTORE_EXP) != null))
				{
					player.restoreExp(CastleManager.getInstance().getCastleByOwner(player.getClan()).getFunction(Castle.FUNC_RESTORE_EXP).getLvl());
				}
				break;
			}
			case 3: // to fortress
			{
				if (((player.getClan() == null) || (player.getClan().getFortId() == 0)) && !isInDefense)
				{
					LOGGER.warning("Player [" + player.getName() + "] called RestartPointPacket - To Fortress and he doesn't have Fortress!");
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.FORTRESS);
				if ((FortManager.getInstance().getFortByOwner(player.getClan()) != null) && (FortManager.getInstance().getFortByOwner(player.getClan()).getFunction(Fort.FUNC_RESTORE_EXP) != null))
				{
					player.restoreExp(FortManager.getInstance().getFortByOwner(player.getClan()).getFunction(Fort.FUNC_RESTORE_EXP).getLvl());
				}
				break;
			}
			case 4: // to siege HQ
			{
				SiegeClan siegeClan = null;
				castle = CastleManager.getInstance().getCastle(player);
				fort = FortManager.getInstance().getFort(player);
				hall = CHSiegeManager.getInstance().getNearbyClanHall(player);
				final SiegeFlagInstance flag = TerritoryWarManager.getInstance().getHQForClan(player.getClan());
				if ((castle != null) && castle.getSiege().isInProgress())
				{
					siegeClan = castle.getSiege().getAttackerClan(player.getClan());
				}
				else if ((fort != null) && fort.getSiege().isInProgress())
				{
					siegeClan = fort.getSiege().getAttackerClan(player.getClan());
				}
				else if ((hall != null) && hall.isInSiege())
				{
					siegeClan = hall.getSiege().getAttackerClan(player.getClan());
				}
				if (((siegeClan == null) || siegeClan.getFlag().isEmpty()) && (flag == null))
				{
					// Check if clan hall has inner spawns loc
					if (hall != null)
					{
						loc = hall.getSiege().getInnerSpawnLoc(player);
						if (loc != null)
						{
							break;
						}
					}
					
					LOGGER.warning("Player [" + player.getName() + "] called RestartPointPacket - To Siege HQ and he doesn't have Siege HQ!");
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.SIEGEFLAG);
				break;
			}
			case 5: // Fixed or Player is a festival participant
			{
				if (!player.isGM() && !player.isFestivalParticipant() && !player.getInventory().haveItemForSelfResurrection())
				{
					LOGGER.warning("Player [" + player.getName() + "] called RestartPointPacket - Fixed and he isn't festival participant!");
					return;
				}
				if (player.isGM() || player.destroyItemByItemId("Feather", 10649, 1, player, false) || player.destroyItemByItemId("Feather", 13300, 1, player, false) || player.destroyItemByItemId("Feather", 13128, 1, player, false))
				{
					player.doRevive(100.00);
				}
				else
				// Festival Participant
				{
					instanceId = player.getInstanceId();
					loc = new Location(player);
				}
				break;
			}
			case 6: // TODO: agathion ress
			{
				break;
			}
			case 27: // to jail
			{
				if (!player.isJailed())
				{
					return;
				}
				loc = new Location(-114356, -249645, -2984);
				break;
			}
			default:
			{
				loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
				break;
			}
		}
		
		// Teleport and revive
		if (loc != null)
		{
			player.setInstanceId(instanceId);
			player.setIn7sDungeon(false);
			player.setIsPendingRevive(true);
			player.teleToLocation(loc, true);
		}
	}
}
