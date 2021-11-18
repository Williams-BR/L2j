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
package handlers.bypasshandlers;

import java.util.StringTokenizer;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.MerchantInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;

public class RentPet implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"RentPet"
	};
	
	@Override
	public boolean useBypass(String command, PlayerInstance player, Creature target)
	{
		if (!(target instanceof MerchantInstance))
		{
			return false;
		}
		
		if (!Config.ALLOW_RENTPET)
		{
			return false;
		}
		
		if (!Config.LIST_PET_RENT_NPC.contains(target.getId()))
		{
			return false;
		}
		
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() < 1)
			{
				final NpcHtmlMessage msg = new NpcHtmlMessage(((Npc) target).getObjectId());
				msg.setHtml("<html><body>Pet Manager:<br>You can rent a wyvern or strider for adena.<br>My prices:<br1><table border=0><tr><td>Ride</td></tr><tr><td>Wyvern</td><td>Strider</td></tr><tr><td><a action=\"bypass -h npc_%objectId%_RentPet 1\">30 sec/1800 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 11\">30 sec/900 adena</a></td></tr><tr><td><a action=\"bypass -h npc_%objectId%_RentPet 2\">1 min/7200 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 12\">1 min/3600 adena</a></td></tr><tr><td><a action=\"bypass -h npc_%objectId%_RentPet 3\">10 min/720000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 13\">10 min/360000 adena</a></td></tr><tr><td><a action=\"bypass -h npc_%objectId%_RentPet 4\">30 min/6480000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 14\">30 min/3240000 adena</a></td></tr></table></body></html>");
				msg.replace("%objectId%", String.valueOf(((Npc) target).getObjectId()));
				player.sendPacket(msg);
			}
			else
			{
				tryRentPet(player, Integer.parseInt(st.nextToken()));
			}
			
			return true;
		}
		catch (Exception e)
		{
			LOGGER.info("Exception in " + getClass().getSimpleName());
		}
		return false;
	}
	
	public static void tryRentPet(PlayerInstance player, int petValue)
	{
		if ((player == null) || player.hasSummon() || player.isMounted() || player.isRentedPet() || player.isTransformed() || player.isCursedWeaponEquipped())
		{
			return;
		}
		if (!player.disarmWeapons())
		{
			return;
		}
		
		int petId;
		double price = 1;
		final int[] cost =
		{
			1800,
			7200,
			720000,
			6480000
		};
		final int[] ridetime =
		{
			30,
			60,
			600,
			1800
		};
		
		int value = petValue;
		if (value > 10)
		{
			petId = 12526;
			value -= 10;
			price /= 2;
		}
		else
		{
			petId = 12621;
		}
		
		if ((value < 1) || (value > 4))
		{
			return;
		}
		
		price *= cost[value - 1];
		final int time = ridetime[value - 1];
		if (!player.reduceAdena("Rent", (long) price, player.getLastFolkNPC(), true))
		{
			return;
		}
		
		player.mount(petId, 0, false);
		player.sendPacket(new SetupGauge(player.getObjectId(), 3, time * 1000));
		player.startRentPet(time);
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
