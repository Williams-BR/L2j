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

import java.util.Arrays;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.instancemanager.ClanHallManager;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Evolve;

public class ClanHallDoormanInstance extends DoormanInstance
{
	private volatile boolean _init = false;
	private ClanHall _clanHall = null;
	private boolean _hasEvolve = false;
	
	// list of clan halls with evolve function, should be sorted
	private static final int[] CH_WITH_EVOLVE =
	{
		36,
		37,
		38,
		39,
		40,
		41,
		51,
		52,
		53,
		54,
		55,
		56,
		57
	};
	
	/**
	 * Creates a clan hall doorman.
	 * @param template the doorman NPC template
	 */
	public ClanHallDoormanInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.ClanHallDoormanInstance);
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		if (_hasEvolve && command.startsWith("evolve") && isOwnerClan(player))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			if (st.countTokens() < 2)
			{
				return;
			}
			
			st.nextToken();
			boolean ok = false;
			switch (Integer.parseInt(st.nextToken()))
			{
				case 1:
				{
					ok = Evolve.doEvolve(player, this, 9882, 10307, 55);
					break;
				}
				case 2:
				{
					ok = Evolve.doEvolve(player, this, 4422, 10308, 55);
					break;
				}
				case 3:
				{
					ok = Evolve.doEvolve(player, this, 4423, 10309, 55);
					break;
				}
				case 4:
				{
					ok = Evolve.doEvolve(player, this, 4424, 10310, 55);
					break;
				}
				case 5:
				{
					ok = Evolve.doEvolve(player, this, 10426, 10611, 70);
					break;
				}
			}
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (ok)
			{
				html.setFile(player, "data/html/clanHallDoorman/evolve-ok.htm");
			}
			else
			{
				html.setFile(player, "data/html/clanHallDoorman/evolve-no.htm");
			}
			player.sendPacket(html);
			return;
		}
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(PlayerInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (getClanHall() != null)
		{
			final Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
			if (isOwnerClan(player))
			{
				if (_hasEvolve)
				{
					html.setFile(player, "data/html/clanHallDoorman/doorman2.htm");
					html.replace("%clanname%", owner.getName());
				}
				else
				{
					html.setFile(player, "data/html/clanHallDoorman/doorman1.htm");
					html.replace("%clanname%", owner.getName());
				}
			}
			else
			{
				if ((owner != null) && (owner.getLeader() != null))
				{
					html.setFile(player, "data/html/clanHallDoorman/doorman-no.htm");
					html.replace("%leadername%", owner.getLeaderName());
					html.replace("%clanname%", owner.getName());
				}
				else
				{
					html.setFile(player, "data/html/clanHallDoorman/emptyowner.htm");
					html.replace("%hallname%", getClanHall().getName());
				}
			}
		}
		else
		{
			return;
		}
		
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	@Override
	protected final void openDoors(PlayerInstance player, String command)
	{
		getClanHall().openCloseDoors(true);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, "data/html/clanHallDoorman/doorman-opened.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	@Override
	protected final void closeDoors(PlayerInstance player, String command)
	{
		getClanHall().openCloseDoors(false);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, "data/html/clanHallDoorman/doorman-closed.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	private final ClanHall getClanHall()
	{
		if (!_init)
		{
			synchronized (this)
			{
				if (!_init)
				{
					_clanHall = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);
					if (_clanHall != null)
					{
						_hasEvolve = Arrays.binarySearch(CH_WITH_EVOLVE, _clanHall.getId()) >= 0;
					}
					_init = true;
				}
			}
		}
		return _clanHall;
	}
	
	@Override
	protected final boolean isOwnerClan(PlayerInstance player)
	{
		return (player.getClan() != null) && (getClanHall() != null) && (player.getClanId() == getClanHall().getOwnerId());
	}
}