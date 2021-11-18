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
package org.l2jmobius.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Fort;

public class FortManager
{
	protected static final Logger LOGGER = Logger.getLogger(FortManager.class.getName());
	
	private static final List<Fort> _forts = new CopyOnWriteArrayList<>();
	
	public int findNearestFortIndex(WorldObject obj)
	{
		return findNearestFortIndex(obj, Long.MAX_VALUE);
	}
	
	public int findNearestFortIndex(WorldObject obj, long maxDistanceValue)
	{
		int index = getFortIndex(obj);
		if (index < 0)
		{
			Fort fort;
			double distance;
			long maxDistance = maxDistanceValue;
			for (int i = 0; i < _forts.size(); i++)
			{
				fort = _forts.get(i);
				if (fort == null)
				{
					continue;
				}
				distance = fort.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					index = i;
				}
			}
		}
		return index;
	}
	
	public Fort getFortById(int fortId)
	{
		for (Fort f : _forts)
		{
			if (f.getResidenceId() == fortId)
			{
				return f;
			}
		}
		return null;
	}
	
	public Fort getFortByOwner(Clan clan)
	{
		for (Fort f : _forts)
		{
			if (f.getOwnerClan() == clan)
			{
				return f;
			}
		}
		return null;
	}
	
	public Fort getFort(String name)
	{
		for (Fort f : _forts)
		{
			if (f.getName().equalsIgnoreCase(name.trim()))
			{
				return f;
			}
		}
		return null;
	}
	
	public Fort getFort(int x, int y, int z)
	{
		for (Fort f : _forts)
		{
			if (f.checkIfInZone(x, y, z))
			{
				return f;
			}
		}
		return null;
	}
	
	public Fort getFort(WorldObject activeObject)
	{
		return getFort(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public int getFortIndex(int fortId)
	{
		Fort fort;
		for (int i = 0; i < _forts.size(); i++)
		{
			fort = _forts.get(i);
			if ((fort != null) && (fort.getResidenceId() == fortId))
			{
				return i;
			}
		}
		return -1;
	}
	
	public int getFortIndex(WorldObject activeObject)
	{
		return getFortIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public int getFortIndex(int x, int y, int z)
	{
		Fort fort;
		for (int i = 0; i < _forts.size(); i++)
		{
			fort = _forts.get(i);
			if ((fort != null) && fort.checkIfInZone(x, y, z))
			{
				return i;
			}
		}
		return -1;
	}
	
	public List<Fort> getForts()
	{
		return _forts;
	}
	
	public void loadInstances()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM fort ORDER BY id"))
		{
			while (rs.next())
			{
				_forts.add(new Fort(rs.getInt("id")));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _forts.size() + " fortress");
			for (Fort fort : _forts)
			{
				fort.getSiege().getSiegeGuardManager().loadSiegeGuard();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadFortData(): " + e.getMessage(), e);
		}
	}
	
	public void activateInstances()
	{
		for (Fort fort : _forts)
		{
			fort.activateInstance();
		}
	}
	
	public static FortManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FortManager INSTANCE = new FortManager();
	}
}
