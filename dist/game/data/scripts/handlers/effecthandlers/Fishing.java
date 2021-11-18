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
package handlers.effecthandlers;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.EtcItemType;
import org.l2jmobius.gameserver.model.items.type.WeaponType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.FishingZone;
import org.l2jmobius.gameserver.model.zone.type.WaterZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.Util;

/**
 * Fishing effect implementation.
 * @author UnAfraid
 */
public class Fishing extends AbstractEffect
{
	private static final int MIN_BAIT_DISTANCE = 90;
	private static final int MAX_BAIT_DISTANCE = 250;
	
	public Fishing(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.FISHING_START;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		final Creature creature = info.getEffector();
		if (!creature.isPlayer())
		{
			return;
		}
		
		final PlayerInstance player = creature.getActingPlayer();
		if (!Config.ALLOW_FISHING && !player.canOverrideCond(PlayerCondOverride.SKILL_CONDITIONS))
		{
			player.sendMessage("Fishing is disabled!");
			return;
		}
		
		if (player.isFishing())
		{
			if (player.getFishCombat() != null)
			{
				player.getFishCombat().doDie(false);
			}
			else
			{
				player.endFishing(false);
			}
			
			player.sendPacket(SystemMessageId.YOUR_ATTEMPT_AT_FISHING_HAS_BEEN_CANCELLED);
			return;
		}
		
		// check for equipped fishing rod
		final Weapon equipedWeapon = player.getActiveWeaponItem();
		if (((equipedWeapon == null) || (equipedWeapon.getItemType() != WeaponType.FISHINGROD)))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_FISHING_POLE_EQUIPPED);
			return;
		}
		
		// check for equipped lure
		final ItemInstance equipedLeftHand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if ((equipedLeftHand == null) || (equipedLeftHand.getItemType() != EtcItemType.LURE))
		{
			player.sendPacket(SystemMessageId.YOU_MUST_PUT_BAIT_ON_YOUR_HOOK_BEFORE_YOU_CAN_FISH);
			return;
		}
		
		if (player.isInBoat())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHILE_RIDING_AS_A_PASSENGER_OF_A_BOAT_IT_S_AGAINST_THE_RULES);
			return;
		}
		
		if (player.isCrafting() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHILE_USING_A_RECIPE_BOOK_PRIVATE_MANUFACTURE_OR_PRIVATE_STORE);
			return;
		}
		
		if (player.isInsideZone(ZoneId.WATER))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHILE_UNDER_WATER);
			return;
		}
		
		// calculate a position in front of the player with a random distance
		int distance = Rnd.get(MIN_BAIT_DISTANCE, MAX_BAIT_DISTANCE);
		final double angle = Util.convertHeadingToDegree(player.getHeading());
		final double radian = Math.toRadians(angle);
		final double sin = Math.sin(radian);
		final double cos = Math.cos(radian);
		int baitX = (int) (player.getX() + (cos * distance));
		int baitY = (int) (player.getY() + (sin * distance));
		
		// search for fishing and water zone
		FishingZone fishingZone = null;
		WaterZone waterZone = null;
		for (ZoneType zone : ZoneManager.getInstance().getZones(baitX, baitY))
		{
			if (zone instanceof FishingZone)
			{
				fishingZone = (FishingZone) zone;
			}
			else if (zone instanceof WaterZone)
			{
				waterZone = (WaterZone) zone;
			}
			
			if ((fishingZone != null) && (waterZone != null))
			{
				break;
			}
		}
		
		int baitZ = computeBaitZ(player, baitX, baitY, fishingZone, waterZone);
		if (baitZ == Integer.MIN_VALUE)
		{
			for (distance = MAX_BAIT_DISTANCE; distance >= MIN_BAIT_DISTANCE; --distance)
			{
				baitX = (int) (player.getX() + (cos * distance));
				baitY = (int) (player.getY() + (sin * distance));
				
				// search for fishing and water zone again
				fishingZone = null;
				waterZone = null;
				for (ZoneType zone : ZoneManager.getInstance().getZones(baitX, baitY))
				{
					if (zone instanceof FishingZone)
					{
						fishingZone = (FishingZone) zone;
					}
					else if (zone instanceof WaterZone)
					{
						waterZone = (WaterZone) zone;
					}
					
					if ((fishingZone != null) && (waterZone != null))
					{
						break;
					}
				}
				
				baitZ = computeBaitZ(player, baitX, baitY, fishingZone, waterZone);
				if (baitZ != Integer.MIN_VALUE)
				{
					break;
				}
			}
			
			if (baitZ == Integer.MIN_VALUE)
			{
				player.sendPacket(SystemMessageId.YOU_CAN_T_FISH_HERE);
				return;
			}
		}
		
		if (!player.destroyItem("Fishing", equipedLeftHand, 1, null, false))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_BAIT);
			return;
		}
		
		player.setLure(equipedLeftHand);
		player.startFishing(baitX, baitY, baitZ);
	}
	
	/**
	 * Computes the Z of the bait.
	 * @param player the player
	 * @param baitX the bait x
	 * @param baitY the bait y
	 * @param fishingZone the fishing zone
	 * @param waterZone the water zone
	 * @return the bait z or {@link Integer#MIN_VALUE} when you cannot fish here
	 */
	private static int computeBaitZ(PlayerInstance player, int baitX, int baitY, FishingZone fishingZone, WaterZone waterZone)
	{
		if ((fishingZone == null))
		{
			return Integer.MIN_VALUE;
		}
		
		if ((waterZone == null))
		{
			return Integer.MIN_VALUE;
		}
		
		// always use water zone, fishing zone high z is high in the air...
		final int baitZ = waterZone.getWaterZ();
		if (!GeoEngine.getInstance().canSeeLocation(player, new Location(baitX, baitY, baitZ)))
		{
			return Integer.MIN_VALUE;
		}
		
		if (GeoEngine.getInstance().hasGeo(baitX, baitY))
		{
			if (GeoEngine.getInstance().getHeight(baitX, baitY, baitZ) > baitZ)
			{
				return Integer.MIN_VALUE;
			}
			
			if (GeoEngine.getInstance().getHeight(baitX, baitY, player.getZ()) > baitZ)
			{
				return Integer.MIN_VALUE;
			}
		}
		
		return baitZ;
	}
}
