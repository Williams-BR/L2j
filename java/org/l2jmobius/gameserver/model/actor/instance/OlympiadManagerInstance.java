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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;

/**
 * Olympiad NPCs Instance
 * @author godson
 */
public class OlympiadManagerInstance extends Npc
{
	/**
	 * Creates an olympiad manager.
	 * @param template the olympiad manager NPC template
	 */
	public OlympiadManagerInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.OlympiadManagerInstance);
	}
	
	public void showChatWindow(PlayerInstance player, int value, String suffix)
	{
		String filename = Olympiad.OLYMPIAD_HTML_PATH;
		filename += "noble_desc" + value;
		filename += (suffix != null) ? suffix + ".htm" : ".htm";
		if (filename.equals(Olympiad.OLYMPIAD_HTML_PATH + "noble_desc0.htm"))
		{
			filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
		}
		
		showChatWindow(player, filename);
	}
}
