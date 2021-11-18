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

import java.util.logging.Level;

import org.l2jmobius.gameserver.enums.ShotType;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.ActionType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.Broadcast;

public class SpiritShot implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		final PlayerInstance player = (PlayerInstance) playable;
		final ItemInstance weaponInst = player.getActiveWeaponInstance();
		final Weapon weaponItem = player.getActiveWeaponItem();
		final SkillHolder[] skills = item.getItem().getSkills();
		final int itemId = item.getId();
		if (skills == null)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		// Check if Spirit shot can be used
		if ((weaponInst == null) || (weaponItem.getSpiritShotCount() == 0))
		{
			if (!player.getAutoSoulShot().contains(itemId))
			{
				player.sendPacket(SystemMessageId.YOU_MAY_NOT_USE_SPIRITSHOTS);
			}
			return false;
		}
		
		// Check if Spirit shot is already active
		if (player.isChargedShot(ShotType.SPIRITSHOTS))
		{
			return false;
		}
		
		final boolean gradeCheck = item.isEtcItem() && (item.getEtcItem().getDefaultAction() == ActionType.SPIRITSHOT) && (weaponInst.getItem().getCrystalTypePlus() == item.getItem().getCrystalTypePlus());
		if (!gradeCheck)
		{
			if (!player.getAutoSoulShot().contains(itemId))
			{
				player.sendPacket(SystemMessageId.YOUR_SPIRITSHOT_DOES_NOT_MATCH_THE_WEAPON_S_GRADE);
			}
			
			return false;
		}
		
		// Consume Spirit shot if player has enough of them
		if (!player.destroyItemWithoutTrace("Consume", item.getObjectId(), weaponItem.getSpiritShotCount(), null, false))
		{
			if (!player.disableAutoShot(itemId))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SPIRITSHOT_FOR_THAT);
			}
			return false;
		}
		
		// Charge Spirit shot
		player.setChargedShot(ShotType.SPIRITSHOTS, true);
		
		// Send message to client
		player.sendPacket(SystemMessageId.YOUR_SPIRITSHOT_HAS_BEEN_ENABLED);
		Broadcast.toSelfAndKnownPlayersInRadius(player, new MagicSkillUse(player, player, skills[0].getSkillId(), skills[0].getSkillLevel(), 0, 0), 600);
		return true;
	}
}
