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
package org.l2jmobius.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import org.l2jmobius.gameserver.model.actor.instance.OlympiadManagerInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadGameTask;
import org.l2jmobius.gameserver.model.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneRespawn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * An olympiad stadium
 * @author durgus, DS
 */
public class OlympiadStadiumZone extends ZoneRespawn
{
	private List<Location> _spectatorLocations;
	
	public OlympiadStadiumZone(int id)
	{
		super(id);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
		
		_checkAffected = true;
	}
	
	public class Settings extends AbstractZoneSettings
	{
		private OlympiadGameTask _task = null;
		
		protected Settings()
		{
		}
		
		public OlympiadGameTask getOlympiadTask()
		{
			return _task;
		}
		
		protected void setTask(OlympiadGameTask task)
		{
			_task = task;
		}
		
		@Override
		public void clear()
		{
			_task = null;
		}
	}
	
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	public void registerTask(OlympiadGameTask task)
	{
		getSettings().setTask(task);
	}
	
	public void openDoors()
	{
		for (DoorInstance door : InstanceManager.getInstance().getInstance(getInstanceId()).getDoors())
		{
			if ((door != null) && !door.isOpen())
			{
				door.openMe();
			}
		}
	}
	
	public void closeDoors()
	{
		for (DoorInstance door : InstanceManager.getInstance().getInstance(getInstanceId()).getDoors())
		{
			if ((door != null) && door.isOpen())
			{
				door.closeMe();
			}
		}
	}
	
	public void spawnBuffers()
	{
		for (Npc buffer : InstanceManager.getInstance().getInstance(getInstanceId()).getNpcs())
		{
			if ((buffer instanceof OlympiadManagerInstance) && !buffer.isSpawned())
			{
				buffer.spawnMe();
			}
		}
	}
	
	public void deleteBuffers()
	{
		for (Npc buffer : InstanceManager.getInstance().getInstance(getInstanceId()).getNpcs())
		{
			if ((buffer instanceof OlympiadManagerInstance) && buffer.isSpawned())
			{
				buffer.decayMe();
			}
		}
	}
	
	public void broadcastStatusUpdate(PlayerInstance player)
	{
		final ExOlympiadUserInfo packet = new ExOlympiadUserInfo(player);
		for (PlayerInstance target : getPlayersInside())
		{
			if ((target != null) && (target.inObserverMode() || (target.getOlympiadSide() != player.getOlympiadSide())) && (target.getInstanceId() == player.getInstanceId()))
			{
				target.sendPacket(packet);
			}
		}
	}
	
	public void broadcastPacketToObservers(IClientOutgoingPacket packet)
	{
		for (PlayerInstance creature : getPlayersInside())
		{
			if ((creature != null) && creature.inObserverMode() && (creature.getInstanceId() == getInstanceId()))
			{
				creature.sendPacket(packet);
			}
		}
	}
	
	@Override
	public void broadcastPacket(IClientOutgoingPacket packet)
	{
		for (PlayerInstance creature : getPlayersInside())
		{
			if ((creature != null) && (creature.getInstanceId() == getInstanceId()))
			{
				creature.sendPacket(packet);
			}
		}
	}
	
	@Override
	protected boolean isAffected(Creature creature)
	{
		if (super.isAffected(creature))
		{
			if (creature.getInstanceId() != getInstanceId())
			{
				return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	protected final void onEnter(Creature creature)
	{
		if ((getSettings().getOlympiadTask() != null) && getSettings().getOlympiadTask().isBattleStarted())
		{
			creature.setInsideZone(ZoneId.PVP, true);
			if (creature.isPlayer())
			{
				creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
				getSettings().getOlympiadTask().getGame().sendOlympiadInfo(creature);
			}
		}
		
		if (!creature.isPlayable())
		{
			return;
		}
		final PlayerInstance player = creature.getActingPlayer();
		if (player != null)
		{
			// only participants, observers and GMs allowed
			if (!player.canOverrideCond(PlayerCondOverride.ZONE_CONDITIONS) && !player.isInOlympiadMode() && !player.inObserverMode())
			{
				ThreadPool.execute(new KickPlayer(player));
			}
			else
			{
				// check for pet
				if (player.hasPet())
				{
					player.getSummon().unSummon(player);
				}
			}
		}
	}
	
	@Override
	protected final void onExit(Creature creature)
	{
		if ((getSettings().getOlympiadTask() != null) && getSettings().getOlympiadTask().isBattleStarted())
		{
			creature.setInsideZone(ZoneId.PVP, false);
			if (creature.isPlayer())
			{
				creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
				creature.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
			}
		}
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (getSettings().getOlympiadTask() == null)
		{
			return;
		}
		
		final boolean battleStarted = getSettings().getOlympiadTask().isBattleStarted();
		final SystemMessage sm = battleStarted ? new SystemMessage(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE) : new SystemMessage(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
		for (Creature creature : getCharactersInside())
		{
			if (creature == null)
			{
				continue;
			}
			if (creature.getInstanceId() != getInstanceId())
			{
				continue;
			}
			
			if (battleStarted)
			{
				creature.setInsideZone(ZoneId.PVP, true);
				if (creature.isPlayer())
				{
					creature.sendPacket(sm);
				}
			}
			else
			{
				creature.setInsideZone(ZoneId.PVP, false);
				if (creature.isPlayer())
				{
					creature.sendPacket(sm);
					creature.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
				}
			}
			creature.broadcastInfo();
		}
	}
	
	private static final class KickPlayer implements Runnable
	{
		private PlayerInstance _player;
		
		public KickPlayer(PlayerInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player == null)
			{
				return;
			}
			
			if (_player.hasSummon())
			{
				_player.getSummon().unSummon(_player);
			}
			
			_player.teleToLocation(TeleportWhereType.TOWN);
			_player.setInstanceId(0);
			_player = null;
		}
	}
	
	@Override
	public void parseLoc(int x, int y, int z, String type)
	{
		if ((type != null) && type.equals("spectatorSpawn"))
		{
			if (_spectatorLocations == null)
			{
				_spectatorLocations = new ArrayList<>();
			}
			_spectatorLocations.add(new Location(x, y, z));
		}
		else
		{
			super.parseLoc(x, y, z, type);
		}
	}
	
	public List<Location> getSpectatorSpawns()
	{
		return _spectatorLocations;
	}
}