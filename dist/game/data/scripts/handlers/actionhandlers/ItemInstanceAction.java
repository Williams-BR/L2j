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
package handlers.actionhandlers;

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.handler.IActionHandler;
import org.l2jmobius.gameserver.instancemanager.MercTicketManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

public class ItemInstanceAction implements IActionHandler
{
	@Override
	public boolean action(PlayerInstance player, WorldObject target, boolean interact)
	{
		// this causes the validate position handler to do the pickup if the location is reached.
		// mercenary tickets can only be picked up by the castle owner.
		final int castleId = MercTicketManager.getInstance().getTicketCastleId(target.getId());
		if ((castleId > 0) && (!player.isCastleLord(castleId) || player.isInParty()))
		{
			if (player.isInParty())
			{
				player.sendMessage("You cannot pickup mercenaries while in a party.");
			}
			else
			{
				player.sendMessage("Only the castle lord can pickup mercenaries.");
			}
			
			player.setTarget(target);
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		else if (!player.isFlying())
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, target);
		}
		
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.ItemInstance;
	}
}