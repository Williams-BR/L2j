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
package handlers.admincommandhandlers;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.BuilderUtil;

/**
 * Admin game point commands.
 * @author Mobius
 */
public class AdminGamePoints implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_add_game_points",
		"admin_count_game_points",
		"admin_gamepoints",
		"admin_set_game_points",
		"admin_subtract_game_points"
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		if (command.startsWith("admin_add_game_points"))
		{
			try
			{
				final String val = command.substring(22);
				if (!addGamePoints(activeChar, val))
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //add_game_points count");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing parameter
				BuilderUtil.sendSysMessage(activeChar, "Usage: //add_game_points count");
			}
		}
		else if (command.equals("admin_count_game_points"))
		{
			if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
			{
				final PlayerInstance target = (PlayerInstance) activeChar.getTarget();
				activeChar.sendMessage(target.getName() + " has a total of " + target.getGamePoints() + " game points.");
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "You must select a player first.");
			}
		}
		else if (command.equals("admin_gamepoints"))
		{
			openGamePointsMenu(activeChar);
		}
		else if (command.startsWith("admin_set_game_points"))
		{
			try
			{
				final String val = command.substring(22);
				if (!setGamePoints(activeChar, val))
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //set_game_points count");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing parameter
				BuilderUtil.sendSysMessage(activeChar, "Usage: //set_game_points count");
			}
		}
		else if (command.startsWith("admin_subtract_game_points"))
		{
			try
			{
				final String val = command.substring(27);
				if (!subtractGamePoints(activeChar, val))
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //subtract_game_points count");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing parameter
				BuilderUtil.sendSysMessage(activeChar, "Usage: //subtract_game_points count");
			}
		}
		return true;
	}
	
	private void openGamePointsMenu(PlayerInstance activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/game_points.htm");
		activeChar.sendPacket(html);
	}
	
	private boolean addGamePoints(PlayerInstance admin, String value)
	{
		final WorldObject target = admin.getTarget();
		PlayerInstance player = null;
		if (target.isPlayer())
		{
			player = (PlayerInstance) target;
		}
		else
		{
			admin.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return false;
		}
		
		final Long points = Long.parseLong(value);
		if (points < 1)
		{
			admin.sendMessage("Invalid game point count.");
			return false;
		}
		
		final long currentPoints = player.getGamePoints();
		if (currentPoints < 1)
		{
			player.setGamePoints(points);
		}
		else
		{
			player.setGamePoints(currentPoints + points);
		}
		
		admin.sendMessage("Added " + points + " game points to " + player.getName() + ".");
		admin.sendMessage(player.getName() + " has now a total of " + player.getGamePoints() + " game points.");
		return true;
	}
	
	private boolean setGamePoints(PlayerInstance admin, String value)
	{
		final WorldObject target = admin.getTarget();
		PlayerInstance player = null;
		if (target.isPlayer())
		{
			player = (PlayerInstance) target;
		}
		else
		{
			admin.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return false;
		}
		
		final Long points = Long.parseLong(value);
		if (points < 0)
		{
			admin.sendMessage("Invalid game point count.");
			return false;
		}
		
		player.setGamePoints(points);
		admin.sendMessage(player.getName() + " has now a total of " + points + " game points.");
		return true;
	}
	
	private boolean subtractGamePoints(PlayerInstance admin, String value)
	{
		final WorldObject target = admin.getTarget();
		PlayerInstance player = null;
		if (target.isPlayer())
		{
			player = (PlayerInstance) target;
		}
		else
		{
			admin.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return false;
		}
		
		final Long points = Long.parseLong(value);
		if (points < 1)
		{
			admin.sendMessage("Invalid game point count.");
			return false;
		}
		
		final long currentPoints = player.getGamePoints();
		if (currentPoints <= points)
		{
			player.setGamePoints(0);
		}
		else
		{
			player.setGamePoints(currentPoints - points);
		}
		admin.sendMessage(player.getName() + " has now a total of " + player.getGamePoints() + " game points.");
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
