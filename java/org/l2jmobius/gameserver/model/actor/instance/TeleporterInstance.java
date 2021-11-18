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

import java.util.Calendar;
import java.util.StringTokenizer;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.TeleportLocationTable;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.SiegeManager;
import org.l2jmobius.gameserver.instancemanager.TownManager;
import org.l2jmobius.gameserver.model.TeleportLocation;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author NightMarez
 */
public class TeleporterInstance extends Npc
{
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_OWNER = 2;
	private static final int COND_REGULAR = 3;
	
	/**
	 * Creates a teleporter.
	 * @param template the teleporter NPC template
	 */
	public TeleporterInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.TeleporterInstance);
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final int condition = validateCondition(player);
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		if (player.isAffectedBySkill(6201) || player.isAffectedBySkill(6202) || player.isAffectedBySkill(6203))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			final String filename = "data/html/teleporter/epictransformed.htm";
			html.setFile(player, filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcname%", getName());
			player.sendPacket(html);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("goto"))
		{
			final int npcId = getId();
			
			switch (npcId)
			{
				case 32534: // Seed of Infinity
				case 32539:
				{
					if (player.isFlyingMounted())
					{
						player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_A_SEED_WHILE_IN_A_FLYING_TRANSFORMATION_STATE);
						return;
					}
					break;
				}
			}
			
			if (st.countTokens() <= 0)
			{
				return;
			}
			
			final int whereTo = Integer.parseInt(st.nextToken());
			if (condition == COND_REGULAR)
			{
				doTeleport(player, whereTo);
				return;
			}
			else if (condition == COND_OWNER)
			{
				// TODO: Replace 0 with highest level when privilege level is implemented
				int minPrivilegeLevel = 0;
				if (st.countTokens() >= 1)
				{
					minPrivilegeLevel = Integer.parseInt(st.nextToken());
				}
				
				// TODO: Replace 10 with privilege level of player
				if (10 >= minPrivilegeLevel)
				{
					doTeleport(player, whereTo);
				}
				else
				{
					player.sendMessage("You don't have the sufficient access level to teleport there.");
				}
				return;
			}
		}
		else if (command.startsWith("Chat"))
		{
			final Calendar cal = Calendar.getInstance();
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			
			if ((val == 1) && (player.getLevel() < 41))
			{
				showNewbieHtml(player);
				return;
			}
			else if ((val == 1) && (cal.get(Calendar.HOUR_OF_DAY) >= 20) && (cal.get(Calendar.HOUR_OF_DAY) <= 23) && ((cal.get(Calendar.DAY_OF_WEEK) == 1) || (cal.get(Calendar.DAY_OF_WEEK) == 7)))
			{
				showHalfPriceHtml(player);
				return;
			}
			showChatWindow(player, val);
		}
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public String getHtmlPath(int npcId, int value)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		return "data/html/teleporter/" + pom + ".htm";
	}
	
	private void showNewbieHtml(PlayerInstance player)
	{
		if (player == null)
		{
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String filename = "data/html/teleporter/free/" + getTemplate().getId() + ".htm";
		if (!HtmCache.getInstance().isLoadable(filename))
		{
			filename = "data/html/teleporter/" + getTemplate().getId() + "-1.htm";
		}
		
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	private void showHalfPriceHtml(PlayerInstance player)
	{
		if (player == null)
		{
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String filename = "data/html/teleporter/half/" + getId() + ".htm";
		if (!HtmCache.getInstance().isLoadable(filename))
		{
			filename = "data/html/teleporter/" + getId() + "-1.htm";
		}
		
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	@Override
	public void showChatWindow(PlayerInstance player)
	{
		String filename = "data/html/teleporter/castleteleporter-no.htm";
		
		final int condition = validateCondition(player);
		if (condition == COND_REGULAR)
		{
			super.showChatWindow(player);
			return;
		}
		else if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/teleporter/castleteleporter-busy.htm"; // Busy because of siege
			}
			else if (condition == COND_OWNER) // Clan owns castle
			{
				filename = getHtmlPath(getId(), 0); // Owner message window
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	private void doTeleport(PlayerInstance player, int value)
	{
		final TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(value);
		if (list != null)
		{
			// you cannot teleport to village that is in siege
			if (SiegeManager.getInstance().getSiege(list.getLocX(), list.getLocY(), list.getLocZ()) != null)
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
				return;
			}
			else if (TownManager.townHasCastleInSiege(list.getLocX(), list.getLocY()) && isInsideZone(ZoneId.TOWN))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
				return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (player.getKarma() > 0)) // karma
			{
				player.sendMessage("Go away, you're not welcome here.");
				return;
			}
			else if (player.isCombatFlagEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
				return;
			}
			else if (list.isForNoble() && !player.isNoble())
			{
				final String filename = "data/html/teleporter/nobleteleporter-no.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				return;
			}
			else if (player.isAlikeDead())
			{
				return;
			}
			
			int price = list.getPrice();
			if (player.getLevel() < 41)
			{
				price = 0;
			}
			else if (!list.isForNoble())
			{
				final Calendar cal = Calendar.getInstance();
				if ((cal.get(Calendar.HOUR_OF_DAY) >= 20) && (cal.get(Calendar.HOUR_OF_DAY) <= 23) && ((cal.get(Calendar.DAY_OF_WEEK) == 1) || (cal.get(Calendar.DAY_OF_WEEK) == 7)))
				{
					price /= 2;
				}
			}
			
			if (Config.FREE_TELEPORTING || player.destroyItemByItemId("Teleport " + (list.isForNoble() ? " nobless" : ""), list.getItemId(), price, this, true))
			{
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), player.getHeading(), -1);
			}
		}
		else
		{
			LOGGER.warning("No teleport destination with id:" + value);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private int validateCondition(PlayerInstance player)
	{
		// Teleporter isn't on castle ground
		if (CastleManager.getInstance().getCastleIndex(this) < 0)
		{
			return COND_REGULAR; // Regular access
		}
		// Teleporter is on castle ground and siege is in progress
		else if (getCastle().getSiege().isInProgress())
		{
			return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
		}
		// Teleporter is on castle ground and player is in a clan
		else if (player.getClan() != null)
		{
			if (getCastle().getOwnerId() == player.getClanId())
			{
				return COND_OWNER; // Owner
			}
		}
		return COND_ALL_FALSE;
	}
}
