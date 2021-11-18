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
package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.MercTicketManager;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class MercTicket implements IItemHandler
{
	/**
	 * handler for using mercenary tickets. Things to do: 1) Check constraints: 1.a) Tickets may only be used in a castle 1.b) Only specific tickets may be used in each castle (different tickets for each castle) 1.c) only the owner of that castle may use them 1.d) tickets cannot be used during siege
	 * 1.e) Check if max number of tickets has been reached 1.f) Check if max number of tickets from this ticket's TYPE has been reached 2) If allowed, call the MercTicketManager to add the item and spawn in the world 3) Remove the item from the person's inventory
	 */
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		final int itemId = item.getId();
		final PlayerInstance player = (PlayerInstance) playable;
		final Castle castle = CastleManager.getInstance().getCastle(player);
		int castleId = -1;
		if (castle != null)
		{
			castleId = castle.getResidenceId();
		}
		
		// add check that certain tickets can only be placed in certain castles
		if (MercTicketManager.getInstance().getTicketCastleId(itemId) != castleId)
		{
			player.sendPacket(SystemMessageId.MERCENARIES_CANNOT_BE_POSITIONED_HERE);
			return false;
		}
		else if (!player.isCastleLord(castleId))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_AUTHORITY_TO_POSITION_MERCENARIES);
			return false;
		}
		else if ((castle != null) && castle.getSiege().isInProgress())
		{
			player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return false;
		}
		
		// Checking Seven Signs Quest Period
		if (SevenSigns.getInstance().getCurrentPeriod() != SevenSigns.PERIOD_SEAL_VALIDATION)
		{
			// LOGGER.warning("Someone has tried to spawn a guardian during Quest Event Period of The Seven Signs.");
			player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return false;
		}
		// Checking the Seal of Strife status
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_NULL:
			{
				if (SevenSigns.getInstance().checkIsDawnPostingTicket(itemId))
				{
					// LOGGER.warning("Someone has tried to spawn a Dawn Mercenary though the Seal of Strife is not controlled by anyone.");
					player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
					return false;
				}
				break;
			}
			case SevenSigns.CABAL_DUSK:
			{
				if (!SevenSigns.getInstance().checkIsRookiePostingTicket(itemId))
				{
					// LOGGER.warning("Someone has tried to spawn a non-Rookie Mercenary though the Seal of Strife is controlled by Revolutionaries of Dusk.");
					player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
					return false;
				}
				break;
			}
			case SevenSigns.CABAL_DAWN:
			{
				break;
			}
		}
		
		if (MercTicketManager.getInstance().isAtCasleLimit(item.getId()))
		{
			player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return false;
		}
		else if (MercTicketManager.getInstance().isAtTypeLimit(item.getId()))
		{
			player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return false;
		}
		else if (MercTicketManager.getInstance().isTooCloseToAnotherTicket(player.getX(), player.getY(), player.getZ()))
		{
			player.sendPacket(SystemMessageId.POSITIONING_CANNOT_BE_DONE_HERE_BECAUSE_THE_DISTANCE_BETWEEN_MERCENARIES_IS_TOO_SHORT);
			return false;
		}
		
		MercTicketManager.getInstance().addTicket(item.getId(), player);
		player.destroyItem("Consume", item.getObjectId(), 1, null, false); // Remove item from char's inventory
		player.sendPacket(SystemMessageId.PLACE_S1_IN_THE_CURRENT_LOCATION_AND_DIRECTION_DO_YOU_WISH_TO_CONTINUE);
		return true;
	}
}
