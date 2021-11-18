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
package org.l2jmobius.gameserver.communitybbs.Manager;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;

public abstract class BaseBBSManager
{
	public abstract void parsecmd(String command, PlayerInstance player);
	
	public abstract void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, PlayerInstance player);
	
	/**
	 * @param html
	 * @param acha
	 */
	protected void send1001(String html, PlayerInstance acha)
	{
		if (html.length() < 8192)
		{
			acha.sendPacket(new ShowBoard(html, "1001"));
		}
	}
	
	/**
	 * @param acha
	 */
	protected void send1002(PlayerInstance acha)
	{
		send1002(acha, " ", " ", "0");
	}
	
	/**
	 * @param player
	 * @param string
	 * @param string2
	 * @param string3
	 */
	protected void send1002(PlayerInstance player, String string, String string2, String string3)
	{
		final List<String> arg = new ArrayList<>(20);
		arg.add("0");
		arg.add("0");
		arg.add("0");
		arg.add("0");
		arg.add("0");
		arg.add("0");
		arg.add(player.getName());
		arg.add(Integer.toString(player.getObjectId()));
		arg.add(player.getAccountName());
		arg.add("9");
		arg.add(string2); // subject?
		arg.add(string2); // subject?
		arg.add(string); // text
		arg.add(string3); // date?
		arg.add(string3); // date?
		arg.add("0");
		arg.add("0");
		player.sendPacket(new ShowBoard(arg));
	}
}
