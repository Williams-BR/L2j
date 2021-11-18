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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * A Peace Zone
 * @author durgus
 */
public class PeaceZone extends ZoneType
{
	public PeaceZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			final PlayerInstance player = creature.getActingPlayer();
			if (player.isCombatFlagEquipped() && TerritoryWarManager.getInstance().isTWInProgress())
			{
				TerritoryWarManager.getInstance().dropCombatFlag(player, false, true);
			}
			
			// PVP possible during siege, now for siege participants only
			// Could also check if this town is in siege, or if any siege is going on
			if ((player.getSiegeState() != 0) && (Config.PEACE_ZONE_MODE == 1))
			{
				return;
			}
			
			/* Nevit Turn Off on Peace Zone */
			if (Config.NEVIT_ENABLED)
			{
				player.getNevitSystem().stopAdventTask(true);
				
				// Stop Nevit's Hourglass
				player.storeRecommendations(true);
				player.getStat().setPausedNevitHourglassStatus(true);
			}
		}
		
		if (Config.PEACE_ZONE_MODE != 2)
		{
			creature.setInsideZone(ZoneId.PEACE, true);
		}
		
		if (!getAllowStore())
		{
			creature.setInsideZone(ZoneId.NO_STORE, true);
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (Config.PEACE_ZONE_MODE != 2)
		{
			creature.setInsideZone(ZoneId.PEACE, false);
		}
		
		if (!getAllowStore())
		{
			creature.setInsideZone(ZoneId.NO_STORE, false);
		}
	}
}
