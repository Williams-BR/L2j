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
package handlers.actionshifthandlers;

import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.StaticObject;

public class DoorInstanceActionShift implements IActionShiftHandler
{
	@Override
	public boolean action(PlayerInstance player, WorldObject target, boolean interact)
	{
		if (player.isGM())
		{
			player.setTarget(target);
			final DoorInstance door = (DoorInstance) target;
			player.sendPacket(new StaticObject(door, player.isGM()));
			
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setFile(player, "data/html/admin/doorinfo.htm");
			html.replace("%class%", target.getClass().getSimpleName());
			html.replace("%hp%", String.valueOf((int) door.getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(door.getMaxHp()));
			html.replace("%objid%", String.valueOf(target.getObjectId()));
			html.replace("%doorid%", String.valueOf(door.getId()));
			html.replace("%minx%", String.valueOf(door.getX(0)));
			html.replace("%miny%", String.valueOf(door.getY(0)));
			html.replace("%minz%", String.valueOf(door.getZMin()));
			html.replace("%maxx%", String.valueOf(door.getX(2)));
			html.replace("%maxy%", String.valueOf(door.getY(2)));
			html.replace("%maxz%", String.valueOf(door.getZMax()));
			html.replace("%unlock%", door.isOpenableBySkill() ? "<font color=00FF00>YES<font>" : "<font color=FF0000>NO</font>");
			player.sendPacket(html);
		}
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.DoorInstance;
	}
}