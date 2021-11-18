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

import java.util.logging.Level;

import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.instancemanager.DimensionalRiftManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

public class Rift implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"enterrift",
		"changeriftroom",
		"exitrift"
	};
	
	@Override
	public boolean useBypass(String command, PlayerInstance player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		if (command.toLowerCase().startsWith(COMMANDS[0])) // EnterRift
		{
			try
			{
				final Byte b1 = Byte.parseByte(command.substring(10)); // Selected Area: Recruit, Soldier etc
				DimensionalRiftManager.getInstance().start(player, b1, (Npc) target);
				return true;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
			}
		}
		else
		{
			final boolean inRift = player.isInParty() && player.getParty().isInDimensionalRift();
			if (command.toLowerCase().startsWith(COMMANDS[1])) // ChangeRiftRoom
			{
				if (inRift)
				{
					player.getParty().getDimensionalRift().manualTeleport(player, (Npc) target);
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(player, (Npc) target);
				}
				
				return true;
			}
			else if (command.toLowerCase().startsWith(COMMANDS[2])) // ExitRift
			{
				if (inRift)
				{
					player.getParty().getDimensionalRift().manualExitRift(player, (Npc) target);
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(player, (Npc) target);
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
