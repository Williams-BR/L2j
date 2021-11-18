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
package org.l2jmobius.commons.threads;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import org.l2jmobius.Config;

/**
 * @author NB4L1
 */
public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler
{
	private static final Logger LOGGER = Logger.getLogger(RejectedExecutionHandlerImpl.class.getName());
	
	@Override
	public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor)
	{
		if (executor.isShutdown())
		{
			return;
		}
		
		LOGGER.warning(runnable.getClass().getSimpleName() + Config.EOL + runnable + " from " + executor + " " + new RejectedExecutionException());
		
		if (Thread.currentThread().getPriority() > Thread.NORM_PRIORITY)
		{
			new Thread(runnable).start();
		}
		else
		{
			runnable.run();
		}
	}
}
