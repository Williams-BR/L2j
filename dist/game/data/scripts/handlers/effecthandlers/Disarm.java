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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Disarm effect implementation.
 * @author nBd
 */
public class Disarm extends AbstractEffect
{
	private static final Map<Integer, Integer> _disarmedPlayers = new ConcurrentHashMap<>();
	
	public Disarm(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return info.getEffected().isPlayer();
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.DISARMED.getMask();
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		final PlayerInstance player = info.getEffected().getActingPlayer();
		if (player == null)
		{
			return;
		}
		
		final ItemInstance itemToDisarm = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (itemToDisarm == null)
		{
			return;
		}
		
		final int slot = player.getInventory().getSlotFromItem(itemToDisarm);
		player.getInventory().unEquipItemInBodySlot(slot);
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(itemToDisarm);
		player.sendPacket(iu);
		player.broadcastUserInfo();
		
		_disarmedPlayers.put(player.getObjectId(), itemToDisarm.getObjectId());
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		final PlayerInstance player = info.getEffected().getActingPlayer();
		if (player == null)
		{
			return;
		}
		
		final Integer itemObjectId = _disarmedPlayers.remove(player.getObjectId());
		if (itemObjectId == null)
		{
			return;
		}
		
		final ItemInstance item = player.getInventory().getItemByObjectId(itemObjectId);
		if (item == null)
		{
			return;
		}
		
		player.getInventory().equipItem(item);
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendPacket(iu);
	}
}
