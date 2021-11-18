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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Tower;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;

/**
 * Class for Control Tower instance.
 */
public class ControlTowerInstance extends Tower
{
	private Collection<Spawn> _guards;
	
	/**
	 * Creates a control tower.
	 * @param template the control tower NPC template
	 */
	public ControlTowerInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.ControlTowerInstance);
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (getCastle().getSiege().isInProgress())
		{
			getCastle().getSiege().killedCT();
			
			if ((_guards != null) && !_guards.isEmpty())
			{
				for (Spawn spawn : _guards)
				{
					try
					{
						spawn.stopRespawn();
						// spawn.getLastSpawn().doDie(spawn.getLastSpawn());
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, "Error at ControlTowerInstance", e);
					}
				}
				_guards.clear();
			}
		}
		return super.doDie(killer);
	}
	
	public void registerGuard(Spawn guard)
	{
		getGuards().add(guard);
	}
	
	private final Collection<Spawn> getGuards()
	{
		if (_guards == null)
		{
			synchronized (this)
			{
				if (_guards == null)
				{
					_guards = ConcurrentHashMap.newKeySet();
				}
			}
		}
		return _guards;
	}
}
