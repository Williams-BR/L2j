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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.EventDropHolder;
import org.l2jmobius.gameserver.model.quest.LongTimeEvent;

/**
 * @author Mobius
 */
public class EventDropManager
{
	private static final Map<LongTimeEvent, List<EventDropHolder>> EVENT_DROPS = new ConcurrentHashMap<>(1);
	
	public void addDrops(LongTimeEvent longTimeEvent, List<EventDropHolder> dropList)
	{
		EVENT_DROPS.put(longTimeEvent, dropList);
	}
	
	public void removeDrops(LongTimeEvent longTimeEvent)
	{
		EVENT_DROPS.remove(longTimeEvent);
	}
	
	public void doEventDrop(Creature attacker, Attackable attackable)
	{
		if (EVENT_DROPS.isEmpty())
		{
			return;
		}
		
		// Event items drop only for players.
		if ((attacker == null) || !attacker.isPlayable() || attackable.isFakePlayer())
		{
			return;
		}
		
		// Event items drop only within a 9 level difference.
		final PlayerInstance player = attacker.getActingPlayer();
		if ((player.getLevel() - attackable.getLevel()) > 9)
		{
			return;
		}
		
		for (List<EventDropHolder> eventDrops : EVENT_DROPS.values())
		{
			DROPS: for (EventDropHolder drop : eventDrops)
			{
				if (!drop.getMonsterIds().isEmpty() && !drop.getMonsterIds().contains(attackable.getId()))
				{
					continue DROPS;
				}
				
				final int monsterLevel = attackable.getLevel();
				if ((monsterLevel >= drop.getMinLevel()) && (monsterLevel <= drop.getMaxLevel()) && (Rnd.get(100d) < drop.getChance()))
				{
					final int itemId = drop.getItemId();
					final long itemCount = Rnd.get(drop.getMin(), drop.getMax());
					if (Config.AUTO_LOOT_ITEM_IDS.contains(itemId) || Config.AUTO_LOOT || attackable.isFlying())
					{
						player.doAutoLoot(attackable, itemId, itemCount); // Give the item to the player that has killed the attackable.
					}
					else
					{
						attackable.dropItem(player, itemId, itemCount); // Drop the item on the ground.
					}
				}
			}
		}
	}
	
	public static EventDropManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventDropManager INSTANCE = new EventDropManager();
	}
}
