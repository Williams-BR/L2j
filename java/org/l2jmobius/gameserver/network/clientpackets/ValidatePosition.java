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
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;

public class ValidatePosition implements IClientIncomingPacket
{
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_x = packet.readD();
		_y = packet.readD();
		_z = packet.readD();
		_heading = packet.readD();
		packet.readD(); // vehicle id
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if ((player == null) || player.isTeleporting() || player.inObserverMode() || player.isCastingNow())
		{
			return;
		}
		
		final int realX = player.getX();
		final int realY = player.getY();
		int realZ = player.getZ();
		if ((_x == 0) && (_y == 0) && (realX != 0))
		{
			return;
		}
		
		if (player.isInVehicle())
		{
			return;
		}
		
		if (player.isFalling(_z))
		{
			return; // Disable validations during fall to avoid "jumping".
		}
		
		// Don't allow flying transformations outside gracia area!
		if (player.isFlyingMounted() && (_x > World.GRACIA_MAX_X))
		{
			player.untransform();
		}
		
		final int dx = _x - realX;
		final int dy = _y - realY;
		final int dz = _z - realZ;
		final double diffSq = ((dx * dx) + (dy * dy));
		if (player.isFlying() || player.isInsideZone(ZoneId.WATER))
		{
			player.setXYZ(realX, realY, _z);
			if (diffSq > 90000)
			{
				player.sendPacket(new ValidateLocation(player));
			}
		}
		else if (diffSq < 360000) // If too large, messes observation.
		{
			if ((diffSq > 250000) || (Math.abs(dz) > 200))
			{
				if ((Math.abs(dz) > 200) && (Math.abs(dz) < 1500) && (Math.abs(_z - player.getClientZ()) < 800))
				{
					player.setXYZ(realX, realY, _z);
					realZ = _z;
				}
				else
				{
					player.sendPacket(new ValidateLocation(player));
				}
			}
		}
		
		// Check out of sync.
		if (player.calculateDistance3D(_x, _y, _z) > player.getStat().getMoveSpeed())
		{
			if (player.isBlinkActive())
			{
				player.setBlinkActive(false);
			}
			else
			{
				player.setXYZ(_x, _y, _z);
			}
		}
		
		player.setClientX(_x);
		player.setClientY(_y);
		player.setClientZ(_z);
		player.setClientHeading(_heading); // No real need to validate heading.
		
		// Mobius: Check for possible door logout and move over exploit. Also checked at MoveBackwardToLocation.
		if (!DoorData.getInstance().checkIfDoorsBetween(realX, realY, realZ, _x, _y, _z, player.getInstanceId(), false))
		{
			player.setLastServerPosition(realX, realY, realZ);
		}
	}
}
