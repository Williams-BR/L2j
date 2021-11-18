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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.GMViewPledgeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.BuilderUtil;

/**
 * <b>Pledge Manipulation:</b><br>
 * <li>With target in a character without clan:<br>
 * //pledge create clanname
 * <li>With target in a clan leader:<br>
 * //pledge info<br>
 * //pledge dismiss<br>
 * //pledge setlevel level<br>
 * //pledge rep reputation_points
 */
public class AdminPledge implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pledge"
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		PlayerInstance player = null;
		if ((target != null) && target.isPlayer())
		{
			player = (PlayerInstance) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			showMainPage(activeChar);
			return false;
		}
		final String name = player.getName();
		if (command.startsWith("admin_pledge"))
		{
			String action = null;
			String parameter = null;
			final StringTokenizer st = new StringTokenizer(command);
			try
			{
				st.nextToken();
				action = st.nextToken(); // create|info|dismiss|setlevel|rep
				parameter = st.nextToken(); // clanname|nothing|nothing|level|rep_points
			}
			catch (NoSuchElementException nse)
			{
				// TODO: Send some message.
				return false;
			}
			if (action.equals("create"))
			{
				final long cet = player.getClanCreateExpiryTime();
				player.setClanCreateExpiryTime(0);
				final Clan clan = ClanTable.getInstance().createClan(player, parameter);
				if (clan != null)
				{
					BuilderUtil.sendSysMessage(activeChar, "Clan " + parameter + " created. Leader: " + player.getName());
				}
				else
				{
					player.setClanCreateExpiryTime(cet);
					BuilderUtil.sendSysMessage(activeChar, "There was a problem while creating the clan.");
				}
			}
			else if (!player.isClanLeader())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
				sm.addString(name);
				activeChar.sendPacket(sm);
				showMainPage(activeChar);
				return false;
			}
			else if (action.equals("dismiss"))
			{
				ClanTable.getInstance().destroyClan(player.getClanId());
				final Clan clan = player.getClan();
				if (clan == null)
				{
					BuilderUtil.sendSysMessage(activeChar, "Clan disbanded.");
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "There was a problem while destroying the clan.");
				}
			}
			else if (action.equals("info"))
			{
				activeChar.sendPacket(new GMViewPledgeInfo(player.getClan(), player));
			}
			else if (parameter == null)
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge <setlevel|rep> <number>");
			}
			else if (action.equals("setlevel"))
			{
				final int level = Integer.parseInt(parameter);
				if ((level >= 0) && (level < 12))
				{
					player.getClan().changeLevel(level);
					BuilderUtil.sendSysMessage(activeChar, "You set level " + level + " for clan " + player.getClan().getName());
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "Level incorrect.");
				}
			}
			else if (action.startsWith("rep"))
			{
				try
				{
					final int points = Integer.parseInt(parameter);
					final Clan clan = player.getClan();
					if (clan.getLevel() < 5)
					{
						BuilderUtil.sendSysMessage(activeChar, "Only clans of level 5 or above may receive reputation points.");
						showMainPage(activeChar);
						return false;
					}
					clan.addReputationScore(points, true);
					BuilderUtil.sendSysMessage(activeChar, "You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Their current score is " + clan.getReputationScore());
				}
				catch (Exception e)
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //pledge <rep> <number>");
				}
			}
		}
		showMainPage(activeChar);
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showMainPage(PlayerInstance activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "game_menu.htm");
	}
}
