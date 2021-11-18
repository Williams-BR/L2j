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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.instancemanager.RecipeManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.util.Util;

/**
 * @author Administrator
 */
public class RequestRecipeShopMakeItem implements IClientIncomingPacket
{
	private int _id;
	private int _recipeId;
	@SuppressWarnings("unused")
	private long _unknown;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_id = packet.readD();
		_recipeId = packet.readD();
		_unknown = packet.readQ();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!client.getFloodProtectors().getManufacture().tryPerformAction("RecipeShopMake"))
		{
			return;
		}
		
		final PlayerInstance manufacturer = World.getInstance().getPlayer(_id);
		if (manufacturer == null)
		{
			return;
		}
		
		if ((manufacturer.getInstanceId() != player.getInstanceId()) && (player.getInstanceId() != -1))
		{
			return;
		}
		
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			player.sendMessage("You cannot create items while trading.");
			return;
		}
		if (manufacturer.getPrivateStoreType() != PrivateStoreType.MANUFACTURE)
		{
			// player.sendMessage("You cannot create items while trading.");
			return;
		}
		
		if (player.isCrafting() || manufacturer.isCrafting())
		{
			player.sendMessage("You are currently in Craft Mode.");
			return;
		}
		if (Util.checkIfInRange(150, player, manufacturer, true))
		{
			RecipeManager.getInstance().requestManufactureItem(manufacturer, _recipeId, player);
		}
	}
}
