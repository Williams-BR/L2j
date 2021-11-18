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
package org.l2jmobius.gameserver.taskmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.model.buylist.Product;

/**
 * @author Mobius
 */
public class BuyListTaskManager
{
	private static final Map<Product, Long> PRODUCTS = new ConcurrentHashMap<>();
	private static final List<Product> PENDING_UPDATES = new ArrayList<>();
	private static boolean _workingProducts = false;
	private static boolean _workingSaves = false;
	
	protected BuyListTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(new BuyListProductTask(), 1000, 60000);
		ThreadPool.scheduleAtFixedRate(new BuyListSaveTask(), 50, 50);
	}
	
	protected class BuyListProductTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_workingProducts)
			{
				return;
			}
			_workingProducts = true;
			
			final long currentTime = Chronos.currentTimeMillis();
			for (Entry<Product, Long> entry : PRODUCTS.entrySet())
			{
				if (currentTime > entry.getValue().longValue())
				{
					final Product product = entry.getKey();
					PRODUCTS.remove(product);
					synchronized (PENDING_UPDATES)
					{
						if (!PENDING_UPDATES.contains(product))
						{
							PENDING_UPDATES.add(product);
						}
					}
				}
			}
			
			_workingProducts = false;
		}
	}
	
	protected class BuyListSaveTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_workingSaves)
			{
				return;
			}
			_workingSaves = true;
			
			if (!PENDING_UPDATES.isEmpty())
			{
				final Product product;
				synchronized (PENDING_UPDATES)
				{
					product = PENDING_UPDATES.get(0);
					PENDING_UPDATES.remove(product);
				}
				product.restock();
			}
			
			_workingSaves = false;
		}
	}
	
	public void add(Product product, long endTime)
	{
		if (!PRODUCTS.containsKey(product))
		{
			PRODUCTS.put(product, endTime);
		}
	}
	
	public void update(Product product, long endTime)
	{
		PRODUCTS.put(product, endTime);
	}
	
	public long getRestockDelay(Product product)
	{
		return PRODUCTS.getOrDefault(product, 0L);
	}
	
	public static BuyListTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BuyListTaskManager INSTANCE = new BuyListTaskManager();
	}
}
