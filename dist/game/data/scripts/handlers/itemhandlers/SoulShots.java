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

import org.l2jmobius.commons.util.Rnd;
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

public class SoulShots implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		final PlayerInstance player = playable.getActingPlayer();
		final ItemInstance weaponInst = player.getActiveWeaponInstance();
		final Weapon weaponItem = player.getActiveWeaponItem();
		final SkillHolder[] skills = item.getItem().getSkills();
		final int itemId = item.getId();
		if (skills == null)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		// Check if Soul shot can be used
		if ((weaponInst == null) || (weaponItem.getSoulShotCount() == 0))
		{
			if (!player.getAutoSoulShot().contains(itemId))
			{
				player.sendPacket(SystemMessageId.CANNOT_USE_SOULSHOTS);
			}
			return false;
		}
		
		final boolean gradeCheck = item.isEtcItem() && (item.getEtcItem().getDefaultAction() == ActionType.SOULSHOT) && (weaponInst.getItem().getCrystalTypePlus() == item.getItem().getCrystalTypePlus());
		if (!gradeCheck)
		{
			if (!player.getAutoSoulShot().contains(itemId))
			{
				player.sendPacket(SystemMessageId.THE_SOULSHOT_YOU_ARE_ATTEMPTING_TO_USE_DOES_NOT_MATCH_THE_GRADE_OF_YOUR_EQUIPPED_WEAPON);
			}
			return false;
		}
		
		// Check if Soul shot is already active
		if (player.isChargedShot(ShotType.SOULSHOTS))
		{
			return false;
		}
		
		// Consume Soul shots if player has enough of them
		int SSCount = weaponItem.getSoulShotCount();
		if ((weaponItem.getReducedSoulShot() > 0) && (Rnd.get(100) < weaponItem.getReducedSoulShotChance()))
		{
			SSCount = weaponItem.getReducedSoulShot();
		}
		
		if (!player.destroyItemWithoutTrace("Consume", item.getObjectId(), SSCount, null, false))
		{
			if (!player.disableAutoShot(itemId))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SOULSHOTS_FOR_THAT);
			}
			return false;
		}
		
		// Charge soul shot
		weaponInst.setChargedShot(ShotType.SOULSHOTS, true);
		
		// Send message to client
		player.sendPacket(SystemMessageId.YOUR_SOULSHOTS_ARE_ENABLED);
		Broadcast.toSelfAndKnownPlayersInRadius(player, new MagicSkillUse(player, player, skills[0].getSkillId(), skills[0].getSkillLevel(), 0, 0), 600);
		return true;
	}
}
