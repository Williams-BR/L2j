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

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.BuilderUtil;

/**
 * @author NosBit
 */
public class AdminScan implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_scan",
		"admin_deleteNpcByObjectId"
	};
	
	private static final int DEFAULT_RADIUS = 500;
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "admin_scan":
			{
				int radius = DEFAULT_RADIUS;
				if (st.hasMoreElements())
				{
					try
					{
						radius = Integer.parseInt(st.nextToken());
					}
					catch (NumberFormatException e)
					{
						BuilderUtil.sendSysMessage(activeChar, "Usage: //scan [radius]");
						return false;
					}
				}
				
				sendNpcList(activeChar, radius);
				break;
			}
			case "admin_deletenpcbyobjectid":
			{
				if (!st.hasMoreElements())
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //deletenpcbyobjectid <object_id>");
					return false;
				}
				
				try
				{
					final int objectId = Integer.parseInt(st.nextToken());
					final WorldObject target = World.getInstance().findObject(objectId);
					final Npc npc = target instanceof Npc ? (Npc) target : null;
					if (npc == null)
					{
						BuilderUtil.sendSysMessage(activeChar, "NPC does not exist or object_id does not belong to an NPC");
						return false;
					}
					
					npc.deleteMe();
					
					final Spawn spawn = npc.getSpawn();
					if (spawn != null)
					{
						spawn.stopRespawn();
						
						if (RaidBossSpawnManager.getInstance().isDefined(spawn.getId()))
						{
							RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
						}
						else
						{
							SpawnTable.getInstance().deleteSpawn(spawn, true);
						}
					}
					
					activeChar.sendMessage(npc.getName() + " have been deleted.");
				}
				catch (NumberFormatException e)
				{
					BuilderUtil.sendSysMessage(activeChar, "object_id must be a number.");
					return false;
				}
				
				sendNpcList(activeChar, DEFAULT_RADIUS);
				break;
			}
		}
		return true;
	}
	
	private void sendNpcList(PlayerInstance activeChar, int radius)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/scan.htm");
		final StringBuilder sb = new StringBuilder();
		for (Creature creature : World.getInstance().getVisibleObjectsInRange(activeChar, Creature.class, radius))
		{
			if (creature instanceof Npc)
			{
				sb.append("<tr>");
				sb.append("<td width=\"54\">" + creature.getId() + "</td>");
				sb.append("<td width=\"54\">" + creature.getName() + "</td>");
				sb.append("<td width=\"54\">" + Math.round(activeChar.calculateDistance2D(creature)) + "</td>");
				sb.append("<td width=\"54\"><a action=\"bypass -h admin_deleteNpcByObjectId " + creature.getObjectId() + "\"><font color=\"LEVEL\">Delete</font></a></td>");
				sb.append("<td width=\"54\"><a action=\"bypass -h admin_move_to " + creature.getX() + " " + creature.getY() + " " + creature.getZ() + "\"><font color=\"LEVEL\">Go to</font></a></td>");
				sb.append("</tr>");
			}
		}
		html.replace("%data%", sb.toString());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
