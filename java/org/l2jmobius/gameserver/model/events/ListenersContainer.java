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
package org.l2jmobius.gameserver.model.events;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;

import org.l2jmobius.commons.util.EmptyQueue;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;

/**
 * @author UnAfraid
 */
public class ListenersContainer
{
	private Map<EventType, Queue<AbstractEventListener>> _listeners = null;
	
	/**
	 * Registers listener for a callback when specified event is executed.
	 * @param listener
	 * @return
	 */
	public AbstractEventListener addListener(AbstractEventListener listener)
	{
		if (listener == null)
		{
			throw new NullPointerException("Listener cannot be null!");
		}
		getListeners().computeIfAbsent(listener.getType(), k -> new PriorityBlockingQueue<>()).add(listener);
		return listener;
	}
	
	/**
	 * Unregisters listener for a callback when specified event is executed.
	 * @param listener
	 * @return
	 */
	public AbstractEventListener removeListener(AbstractEventListener listener)
	{
		if (listener == null)
		{
			throw new NullPointerException("Listener cannot be null!");
		}
		else if (_listeners == null)
		{
			throw new NullPointerException("Listeners container is not initialized!");
		}
		else if (!_listeners.containsKey(listener.getType()))
		{
			throw new IllegalAccessError("Listeners container doesn't had " + listener.getType() + " event type added!");
		}
		
		_listeners.get(listener.getType()).remove(listener);
		return listener;
	}
	
	public void removeListenerIf(EventType type, Predicate<? super AbstractEventListener> filter)
	{
		if (_listeners == null)
		{
			return;
		}
		
		for (AbstractEventListener listener : getListeners(type))
		{
			if (filter.test(listener))
			{
				listener.unregisterMe();
			}
		}
	}
	
	public void removeListenerIf(Predicate<? super AbstractEventListener> filter)
	{
		if (_listeners == null)
		{
			return;
		}
		
		for (Queue<AbstractEventListener> queue : getListeners().values())
		{
			for (AbstractEventListener listener : queue)
			{
				if (filter.test(listener))
				{
					listener.unregisterMe();
				}
			}
		}
	}
	
	public boolean hasListener(EventType type)
	{
		// TODO: return (_listeners != null) && !getListeners(type).isEmpty();
		return !getListeners(type).isEmpty();
	}
	
	/**
	 * @param type
	 * @return {@code List} of {@link AbstractEventListener} by the specified type
	 */
	public Queue<AbstractEventListener> getListeners(EventType type)
	{
		return (_listeners != null) && _listeners.containsKey(type) ? _listeners.get(type) : EmptyQueue.emptyQueue();
	}
	
	/**
	 * Creates the listeners container map if doesn't exists.
	 * @return the listeners container map.
	 */
	private Map<EventType, Queue<AbstractEventListener>> getListeners()
	{
		if (_listeners == null)
		{
			synchronized (this)
			{
				if (_listeners == null)
				{
					_listeners = new ConcurrentHashMap<>();
				}
			}
		}
		return _listeners;
	}
}
