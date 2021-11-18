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
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.ActionType;
import org.l2jmobius.gameserver.model.items.type.WeaponType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author -Nemesiss-
 */
public class FishShots implements IItemHandler
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
		if ((weaponInst == null) || (weaponItem.getItemType() != WeaponType.FISHINGROD))
		{
			return false;
		}
		
		if (player.isChargedShot(ShotType.FISH_SOULSHOTS))
		{
			return false;
		}
		
		final long count = item.getCount();
		final SkillHolder[] skills = item.getItem().getSkills();
		if (skills == null)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		final boolean gradeCheck = item.isEtcItem() && (item.getEtcItem().getDefaultAction() == ActionType.FISHINGSHOT) && (weaponInst.getItem().getCrystalTypePlus() == item.getItem().getCrystalTypePlus());
		if (!gradeCheck)
		{
			player.sendPacket(SystemMessageId.THAT_IS_THE_WRONG_GRADE_OF_SOULSHOT_FOR_THAT_FISHING_POLE);
			return false;
		}
		
		if (count < 1)
		{
			return false;
		}
		
		player.setChargedShot(ShotType.FISH_SOULSHOTS, true);
		player.destroyItemWithoutTrace("Consume", item.getObjectId(), 1, null, false);
		final WorldObject oldTarget = player.getTarget();
		player.setTarget(player);
		
		Broadcast.toSelfAndKnownPlayers(player, new MagicSkillUse(player, skills[0].getSkillId(), skills[0].getSkillLevel(), 0, 0));
		player.setTarget(oldTarget);
		return true;
	}
}
