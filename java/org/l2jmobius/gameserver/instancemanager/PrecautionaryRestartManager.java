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

import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.GrandBossInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.RaidBossInstance;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Mobius
 */
public class PrecautionaryRestartManager
{
	private static final Logger LOGGER = Logger.getLogger(PrecautionaryRestartManager.class.getName());
	
	private static final String SYSTEM_CPU_LOAD_VAR = "SystemCpuLoad";
	private static final String PROCESS_CPU_LOAD_VAR = "ProcessCpuLoad";
	
	private static boolean _restarting = false;
	
	protected PrecautionaryRestartManager()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			if (_restarting)
			{
				return;
			}
			
			if (Config.PRECAUTIONARY_RESTART_CPU && (getCpuLoad(SYSTEM_CPU_LOAD_VAR) > Config.PRECAUTIONARY_RESTART_PERCENTAGE))
			{
				if (serverBizzy())
				{
					return;
				}
				
				LOGGER.info("PrecautionaryRestartManager: CPU usage over " + Config.PRECAUTIONARY_RESTART_PERCENTAGE + "%.");
				LOGGER.info("PrecautionaryRestartManager: Server is using " + getCpuLoad(PROCESS_CPU_LOAD_VAR) + "%.");
				Broadcast.toAllOnlinePlayers("Server will restart in 10 minutes.", false);
				Shutdown.getInstance().startShutdown(null, 600, true);
			}
			
			if (Config.PRECAUTIONARY_RESTART_MEMORY && (getProcessRamLoad() > Config.PRECAUTIONARY_RESTART_PERCENTAGE))
			{
				if (serverBizzy())
				{
					return;
				}
				
				LOGGER.info("PrecautionaryRestartManager: Memory usage over " + Config.PRECAUTIONARY_RESTART_PERCENTAGE + "%.");
				Broadcast.toAllOnlinePlayers("Server will restart in 10 minutes.", false);
				Shutdown.getInstance().startShutdown(null, 600, true);
			}
		}, Config.PRECAUTIONARY_RESTART_DELAY, Config.PRECAUTIONARY_RESTART_DELAY);
	}
	
	private static double getCpuLoad(String var)
	{
		try
		{
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			final ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
			final AttributeList list = mbs.getAttributes(name, new String[]
			{
				var
			});
			
			if (list.isEmpty())
			{
				return 0;
			}
			
			final Attribute att = (Attribute) list.get(0);
			final Double value = (Double) att.getValue();
			if (value == -1)
			{
				return 0;
			}
			
			return (value * 1000) / 10d;
		}
		catch (Exception e)
		{
		}
		
		return 0;
	}
	
	private static double getProcessRamLoad()
	{
		final Runtime runTime = Runtime.getRuntime();
		final long totalMemory = runTime.maxMemory();
		final long usedMemory = totalMemory - ((totalMemory - runTime.totalMemory()) + runTime.freeMemory());
		return (usedMemory * 100) / totalMemory;
	}
	
	private boolean serverBizzy()
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if ((castle != null) && castle.getSiege().isInProgress())
			{
				return true;
			}
		}
		
		for (Fort fort : FortManager.getInstance().getForts())
		{
			if ((fort != null) && fort.getSiege().isInProgress())
			{
				return true;
			}
		}
		
		for (PlayerInstance player : World.getInstance().getPlayers())
		{
			if ((player == null) || player.isInOfflineMode())
			{
				continue;
			}
			
			if (player.isInOlympiadMode())
			{
				return true;
			}
			
			if (player.isOnEvent())
			{
				return true;
			}
			
			if (player.getInstanceId() > 0)
			{
				return true;
			}
			
			final WorldObject target = player.getTarget();
			if ((target instanceof RaidBossInstance) || (target instanceof GrandBossInstance))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void restartEnabled()
	{
		_restarting = true;
	}
	
	public void restartAborted()
	{
		_restarting = false;
	}
	
	public static PrecautionaryRestartManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PrecautionaryRestartManager INSTANCE = new PrecautionaryRestartManager();
	}
}