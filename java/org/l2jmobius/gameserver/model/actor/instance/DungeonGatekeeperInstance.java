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

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.sql.TeleportLocationTable;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.TeleportLocation;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class DungeonGatekeeperInstance extends Npc
{
	/**
	 * Creates a dungeon gatekeeper.
	 * @param template the dungeon gatekeeper NPC template
	 */
	public DungeonGatekeeperInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.DungeonGatekeeperInstance);
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		final int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		final int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
		final boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
		if (actualCommand.startsWith("necro"))
		{
			boolean canPort = true;
			if (isSealValidationPeriod)
			{
				if ((compWinner == SevenSigns.CABAL_DAWN) && ((playerCabal != SevenSigns.CABAL_DAWN) || (sealAvariceOwner != SevenSigns.CABAL_DAWN)))
				{
					player.sendPacket(SystemMessageId.ONLY_A_LORD_OF_DAWN_MAY_USE_THIS);
					canPort = false;
				}
				else if ((compWinner == SevenSigns.CABAL_DUSK) && ((playerCabal != SevenSigns.CABAL_DUSK) || (sealAvariceOwner != SevenSigns.CABAL_DUSK)))
				{
					player.sendPacket(SystemMessageId.ONLY_A_REVOLUTIONARY_OF_DUSK_MAY_USE_THIS);
					canPort = false;
				}
				else if ((compWinner == SevenSigns.CABAL_NULL) && (playerCabal != SevenSigns.CABAL_NULL))
				{
					canPort = true;
				}
				else if (playerCabal == SevenSigns.CABAL_NULL)
				{
					canPort = false;
				}
			}
			else
			{
				if (playerCabal == SevenSigns.CABAL_NULL)
				{
					canPort = false;
				}
			}
			
			if (!canPort)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				filename += "necro_no.htm";
				html.setFile(player, filename);
				player.sendPacket(html);
			}
			else
			{
				doTeleport(player, Integer.parseInt(st.nextToken()));
				player.setIn7sDungeon(true);
			}
		}
		else if (actualCommand.startsWith("cata"))
		{
			boolean canPort = true;
			if (isSealValidationPeriod)
			{
				if ((compWinner == SevenSigns.CABAL_DAWN) && ((playerCabal != SevenSigns.CABAL_DAWN) || (sealGnosisOwner != SevenSigns.CABAL_DAWN)))
				{
					player.sendPacket(SystemMessageId.ONLY_A_LORD_OF_DAWN_MAY_USE_THIS);
					canPort = false;
				}
				else if ((compWinner == SevenSigns.CABAL_DUSK) && ((playerCabal != SevenSigns.CABAL_DUSK) || (sealGnosisOwner != SevenSigns.CABAL_DUSK)))
				{
					player.sendPacket(SystemMessageId.ONLY_A_REVOLUTIONARY_OF_DUSK_MAY_USE_THIS);
					canPort = false;
				}
				else if ((compWinner == SevenSigns.CABAL_NULL) && (playerCabal != SevenSigns.CABAL_NULL))
				{
					canPort = true;
				}
				else if (playerCabal == SevenSigns.CABAL_NULL)
				{
					canPort = false;
				}
			}
			else
			{
				if (playerCabal == SevenSigns.CABAL_NULL)
				{
					canPort = false;
				}
			}
			
			if (!canPort)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				filename += "cata_no.htm";
				html.setFile(player, filename);
				player.sendPacket(html);
			}
			else
			{
				doTeleport(player, Integer.parseInt(st.nextToken()));
				player.setIn7sDungeon(true);
			}
		}
		else if (actualCommand.startsWith("exit"))
		{
			doTeleport(player, Integer.parseInt(st.nextToken()));
			player.setIn7sDungeon(false);
		}
		else if (actualCommand.startsWith("goto"))
		{
			doTeleport(player, Integer.parseInt(st.nextToken()));
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	private void doTeleport(PlayerInstance player, int value)
	{
		final TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(value);
		if (list != null)
		{
			if (player.isAlikeDead())
			{
				return;
			}
			
			player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
		}
		else
		{
			LOGGER.warning("No teleport destination with id:" + value);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
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
}