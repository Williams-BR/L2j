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

import java.nio.BufferUnderflowException;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerMoveRequest;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.util.Util;

public class MoveBackwardToLocation implements IClientIncomingPacket
{
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	private int _originX;
	private int _originY;
	private int _originZ;
	private int _movementMode;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_targetX = packet.readD();
		_targetY = packet.readD();
		_targetZ = packet.readD();
		_originX = packet.readD();
		_originY = packet.readD();
		_originZ = packet.readD();
		try
		{
			_movementMode = packet.readD(); // is 0 if cursor keys are used 1 if mouse is used
		}
		catch (BufferUnderflowException e)
		{
			if (Config.L2WALKER_PROTECTION)
			{
				final PlayerInstance player = client.getPlayer();
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is trying to use L2Walker and got kicked.", Config.DEFAULT_PUNISH);
			}
		}
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
		
		if ((Config.PLAYER_MOVEMENT_BLOCK_TIME > 0) && !player.isGM() && (player.getNotMoveUntil() > Chronos.currentTimeMillis()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SPEAKING_TO_AN_NPC_ONE_MOMENT_PLEASE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if ((_targetX == _originX) && (_targetY == _originY) && (_targetZ == _originZ))
		{
			player.sendPacket(new StopMove(player));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check for possible door logout and move over exploit. Also checked at ValidatePosition.
		if (DoorData.getInstance().checkIfDoorsBetween(player.getX(), player.getY(), player.getZ(), _targetX, _targetY, _targetZ, player.getInstanceId(), false))
		{
			player.stopMove(player.getLastServerPosition());
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Prevent player force moving in or out siege area.
		// final int teleMode = player.getTeleMode();
		// if (!player.isFlying() && (teleMode == 0))
		// {
		// final boolean siegable = player.isInsideZone(ZoneId.CASTLE) || player.isInsideZone(ZoneId.FORT);
		// boolean waterContact = player.isInsideZone(ZoneId.WATER);
		// if (siegable && !waterContact) // Need to know if player is over water only when siegable.
		// {
		// for (ZoneType zone : ZoneManager.getInstance().getZones(_originX, _originY))
		// {
		// if ((zone instanceof WaterZone) && ((zone.getZone().getHighZ() + player.getCollisionHeight()) > _originZ))
		// {
		// waterContact = true;
		// break;
		// }
		// }
		// }
		// if (player.isInsideZone(ZoneId.HQ) || (siegable && waterContact))
		// {
		// boolean limited = false;
		// boolean water = false;
		// for (ZoneType zone : ZoneManager.getInstance().getZones(_targetX, _targetY, _targetZ))
		// {
		// if ((zone instanceof CastleZone) || (zone instanceof FortZone))
		// {
		// if (Util.calculateDistance(_originX, _originY, _originZ, _targetX, _targetY, _targetZ, false, false) > 1000)
		// {
		// player.stopMove(player.getLastServerPosition());
		// player.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		// limited = true;
		// }
		// if (zone instanceof WaterZone)
		// {
		// water = true;
		// }
		// }
		// if (limited && !water && !GeoEngine.getInstance().canSeeTarget(player, new Location(_targetX, _targetY, _targetZ)))
		// {
		// player.stopMove(player.getLastServerPosition());
		// player.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		// }
		// else if (siegable)
		// {
		// for (ZoneType zone : ZoneManager.getInstance().getZones(_targetX, _targetY, _targetZ))
		// {
		// if ((zone instanceof WaterZone) || (zone instanceof HqZone))
		// {
		// if ((Math.abs(_targetZ - _originZ) > 250) || !GeoEngine.getInstance().canSeeTarget(player, new Location(_targetX, _targetY, _targetZ)))
		// {
		// player.stopMove(player.getLastServerPosition());
		// player.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		// }
		// else if ((zone instanceof CastleZone) || (zone instanceof FortZone))
		// {
		// if (((Math.abs(_targetZ - _originZ) < 100) || (Util.calculateDistance(_originX, _originY, _originZ, _targetX, _targetY, _targetZ, false, false) > 2000)) && !GeoEngine.getInstance().canMoveToTarget(_originX, _originY, _originZ, _targetX, _targetY, _targetZ, player.getInstanceId()))
		// {
		// player.stopMove(player.getLastServerPosition());
		// player.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		// }
		// }
		// }
		// }
		
		// Correcting targetZ from floor level to head level (?)
		// Client is giving floor level as targetZ but that floor level doesn't
		// match our current geodata and teleport coords as good as head level!
		// L2J uses floor, not head level as char coordinates. This is some
		// sort of incompatibility fix.
		// Validate position packets sends head level.
		_targetZ += player.getTemplate().getCollisionHeight();
		
		if (_movementMode == 1)
		{
			player.setCursorKeyMovement(false);
			final TerminateReturn terminate = EventDispatcher.getInstance().notifyEvent(new OnPlayerMoveRequest(player, new Location(_targetX, _targetY, _targetZ)), player, TerminateReturn.class);
			if ((terminate != null) && terminate.terminate())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		else // 0
		{
			if (!Config.ENABLE_KEYBOARD_MOVEMENT)
			{
				return;
			}
			player.setCursorKeyMovement(true);
		}
		
		final int teleMode = player.getTeleMode();
		if (teleMode > 0)
		{
			if (teleMode == 1)
			{
				player.setTeleMode(0);
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.teleToLocation(new Location(_targetX, _targetY, _targetZ));
			return;
		}
		
		final double dx = _targetX - player.getX();
		final double dy = _targetY - player.getY();
		// Can't move if character is confused, or trying to move a huge distance
		if (player.isOutOfControl() || (((dx * dx) + (dy * dy)) > 98010000)) // 9900*9900
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_targetX, _targetY, _targetZ));
		
		// Mobius: Check spawn protections.
		player.onActionRequest();
	}
}
