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
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.Henna;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author Zoey76
 */
public class RequestHennaRemove implements IClientIncomingPacket
{
	private int _symbolId;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_symbolId = packet.readD();
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
		
		if (!client.getFloodProtectors().getTransaction().tryPerformAction("HennaRemove"))
		{
			client.sendActionFailed();
			return;
		}
		
		Henna henna;
		boolean found = false;
		for (int i = 1; i <= 3; i++)
		{
			henna = player.getHenna(i);
			if ((henna != null) && (henna.getDyeId() == _symbolId))
			{
				if (player.getAdena() >= henna.getCancelFee())
				{
					player.removeHenna(i);
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
					client.sendActionFailed();
				}
				found = true;
				break;
			}
		}
		// TODO: Test.
		if (!found)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Player " + player + " requested Henna Draw remove without any henna.");
			client.sendActionFailed();
		}
	}
}
