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
package org.l2jmobius.commons.database;

import java.sql.Connection;
import java.util.logging.Logger;

import org.mariadb.jdbc.MariaDbPoolDataSource;

import org.l2jmobius.Config;

/**
 * @author Mobius
 * @version November 10th 2018
 */
public class DatabaseFactory
{
	private static final Logger LOGGER = Logger.getLogger(DatabaseFactory.class.getName());
	
	private static final MariaDbPoolDataSource DATABASE_POOL = new MariaDbPoolDataSource(Config.DATABASE_URL + "&user=" + Config.DATABASE_LOGIN + "&password=" + Config.DATABASE_PASSWORD + "&maxPoolSize=" + Config.DATABASE_MAX_CONNECTIONS);
	
	public static void init()
	{
		// Test if connection is valid.
		try
		{
			DATABASE_POOL.getConnection().close();
			LOGGER.info("Database: Initialized.");
		}
		catch (Exception e)
		{
			LOGGER.info("Database: Problem on initialize. " + e);
		}
	}
	
	public static Connection getConnection()
	{
		Connection con = null;
		while (con == null)
		{
			try
			{
				con = DATABASE_POOL.getConnection();
			}
			catch (Exception e)
			{
				LOGGER.severe("DatabaseFactory: Cound not get a connection. " + e);
			}
		}
		return con;
	}
	
	public static void close()
	{
		try
		{
			DATABASE_POOL.close();
		}
		catch (Exception e)
		{
			LOGGER.severe("DatabaseFactory: There was a problem closing the data source. " + e);
		}
	}
}
