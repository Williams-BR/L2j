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
package org.l2jmobius.gameserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.taskmanager.Task;
import org.l2jmobius.gameserver.taskmanager.TaskManager;
import org.l2jmobius.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.l2jmobius.gameserver.taskmanager.TaskTypes;

/**
 * @author Layane
 */
public class TaskRecom extends Task
{
	private static final String NAME = "recommendations";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("UPDATE character_reco_bonus SET rec_left=?, time_left=?, rec_have=0 WHERE rec_have <=  20"))
			{
				ps.setInt(1, 20); // Rec left = 20
				ps.setInt(2, 3600000); // Timer = 1 hour
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("UPDATE character_reco_bonus SET rec_left=?, time_left=?, rec_have=GREATEST(rec_have-20,0) WHERE rec_have > 20"))
			{
				ps.setInt(1, 20); // Rec left = 20
				ps.setInt(2, 3600000); // Timer = 1 hour
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": Could not reset Recommendations System: " + e);
		}
		
		// Refresh reco bonus for online players
		if (Config.NEVIT_ENABLED)
		{
			for (PlayerInstance player : World.getInstance().getPlayers())
			{
				if (player != null)
				{
					player.stopNevitHourglassTask();
					player.startNevitHourglassTask();
				}
			}
		}
		LOGGER.info("Recommendations System reseted.");
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}
}