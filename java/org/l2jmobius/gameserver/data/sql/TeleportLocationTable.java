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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.TeleportLocation;

public class TeleportLocationTable
{
	private static final Logger LOGGER = Logger.getLogger(TeleportLocationTable.class.getName());
	
	private final Map<Integer, TeleportLocation> _teleports = new HashMap<>();
	
	protected TeleportLocationTable()
	{
		reloadAll();
	}
	
	public void reloadAll()
	{
		_teleports.clear();
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id, loc_x, loc_y, loc_z, price, fornoble, itemId FROM teleport"))
		{
			TeleportLocation teleport;
			while (rs.next())
			{
				teleport = new TeleportLocation();
				teleport.setTeleId(rs.getInt("id"));
				teleport.setLocX(rs.getInt("loc_x"));
				teleport.setLocY(rs.getInt("loc_y"));
				teleport.setLocZ(rs.getInt("loc_z"));
				teleport.setPrice(rs.getInt("price"));
				teleport.setForNoble(rs.getInt("fornoble") == 1);
				teleport.setItemId(rs.getInt("itemId"));
				
				_teleports.put(teleport.getTeleId(), teleport);
			}
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _teleports.size() + " teleport location templates.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error loading Teleport Table.", e);
		}
		
		if (!Config.CUSTOM_TELEPORT_TABLE)
		{
			return;
		}
		
		int cTeleCount = _teleports.size();
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id, loc_x, loc_y, loc_z, price, fornoble, itemId FROM custom_teleport"))
		{
			TeleportLocation teleport;
			while (rs.next())
			{
				teleport = new TeleportLocation();
				teleport.setTeleId(rs.getInt("id"));
				teleport.setLocX(rs.getInt("loc_x"));
				teleport.setLocY(rs.getInt("loc_y"));
				teleport.setLocZ(rs.getInt("loc_z"));
				teleport.setPrice(rs.getInt("price"));
				teleport.setForNoble(rs.getInt("fornoble") == 1);
				teleport.setItemId(rs.getInt("itemId"));
				
				_teleports.put(teleport.getTeleId(), teleport);
			}
			cTeleCount = _teleports.size() - cTeleCount;
			if (cTeleCount > 0)
			{
				LOGGER.info(getClass().getSimpleName() + ": Loaded " + cTeleCount + " custom teleport location templates.");
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while creating custom teleport table " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param id
	 * @return
	 */
	public TeleportLocation getTemplate(int id)
	{
		return _teleports.get(id);
	}
	
	public static TeleportLocationTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TeleportLocationTable INSTANCE = new TeleportLocationTable();
	}
}
