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

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.StartRotation;

/**
 * @version $Revision: 1.1.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class StartRotating implements IClientIncomingPacket
{
	private int _degree;
	private int _side;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_degree = packet.readD();
		_side = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		if (!Config.ENABLE_KEYBOARD_MOVEMENT)
		{
			return;
		}
		
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final StartRotation br;
		if (player.isInAirShip() && player.getAirShip().isCaptain(player))
		{
			br = new StartRotation(player.getAirShip().getObjectId(), _degree, _side, 0);
			player.getAirShip().broadcastPacket(br);
		}
		else
		{
			br = new StartRotation(player.getObjectId(), _degree, _side, 0);
			player.broadcastPacket(br);
		}
	}
}
