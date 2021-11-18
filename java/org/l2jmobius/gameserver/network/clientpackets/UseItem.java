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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CtrlEvent;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.EtcItem;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.ArmorType;
import org.l2jmobius.gameserver.model.items.type.WeaponType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExUseSharedGroupItem;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class UseItem implements IClientIncomingPacket
{
	private int _objectId;
	private boolean _ctrlPressed;
	private int _itemId;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_objectId = packet.readD();
		_ctrlPressed = packet.readD() != 0;
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Flood protect UseItem
		if (!client.getFloodProtectors().getUseItem().tryPerformAction("use item"))
		{
			return;
		}
		
		if (player.isInsideZone(ZoneId.JAIL))
		{
			player.sendMessage("You cannot use items while jailed.");
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.cancelActiveTrade();
		}
		
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			return;
		}
		
		if (item.getItem().getType2() == Item.TYPE2_QUEST)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_QUEST_ITEMS);
			return;
		}
		
		// No UseItem is allowed while the player is in special conditions
		if (player.isStunned() || player.isParalyzed() || player.isSleeping() || player.isAfraid() || player.isAlikeDead())
		{
			return;
		}
		
		_itemId = item.getId();
		
		// Char cannot use item when dead
		if (player.isDead() || !player.getInventory().canManipulateWithItemId(_itemId))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addItemName(item);
			player.sendPacket(sm);
			return;
		}
		
		if (!item.isEquipped() && !item.getItem().checkCondition(player, player, true))
		{
			return;
		}
		
		if (player.isFishing() && ((_itemId < 6535) || (_itemId > 6540)))
		{
			// You cannot do anything else while fishing
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
			return;
		}
		
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (player.getKarma() > 0))
		{
			final SkillHolder[] skills = item.getItem().getSkills();
			if (skills != null)
			{
				for (SkillHolder sHolder : skills)
				{
					final Skill skill = sHolder.getSkill();
					if ((skill != null) && skill.hasEffectType(EffectType.TELEPORT))
					{
						return;
					}
				}
			}
		}
		
		// If the item has reuse time and it has not passed.
		// Message from reuse delay must come from item.
		final int reuseDelay = item.getReuseDelay();
		final int sharedReuseGroup = item.getSharedReuseGroup();
		if (reuseDelay > 0)
		{
			final long reuse = player.getItemRemainingReuseTime(item.getObjectId());
			if (reuse > 0)
			{
				reuseData(player, item, reuse);
				sendSharedGroupUpdate(player, sharedReuseGroup, reuse, reuseDelay);
				return;
			}
			
			final long reuseOnGroup = player.getReuseDelayOnGroup(sharedReuseGroup);
			if (reuseOnGroup > 0)
			{
				reuseData(player, item, reuseOnGroup);
				sendSharedGroupUpdate(player, sharedReuseGroup, reuseOnGroup, reuseDelay);
				return;
			}
		}
		
		player.onActionRequest();
		
		if (item.isEquipable())
		{
			// Don't allow to put formal wear while a cursed weapon is equipped.
			if (player.isCursedWeaponEquipped() && (_itemId == 6408))
			{
				return;
			}
			
			// Equip or unEquip
			if (FortSiegeManager.getInstance().isCombat(_itemId))
			{
				return; // no message
			}
			
			if (player.isCombatFlagEquipped())
			{
				return;
			}
			
			switch (item.getItem().getBodyPart())
			{
				case Item.SLOT_LR_HAND:
				case Item.SLOT_L_HAND:
				case Item.SLOT_R_HAND:
				{
					// Prevent players to equip weapon while wearing combat flag
					if ((player.getActiveWeaponItem() != null) && (player.getActiveWeaponItem().getId() == 9819))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					
					if (player.isMounted())
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					if (player.isDisarmed())
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					
					// Don't allow weapon/shield equipment if a cursed weapon is equipped.
					if (player.isCursedWeaponEquipped())
					{
						return;
					}
					
					// Don't allow other Race to Wear Kamael exclusive Weapons.
					if (!item.isEquipped() && item.isWeapon() && !player.canOverrideCond(PlayerCondOverride.ITEM_CONDITIONS))
					{
						final Weapon wpn = (Weapon) item.getItem();
						
						switch (player.getRace())
						{
							case KAMAEL:
							{
								switch (wpn.getItemType())
								{
									case NONE:
									{
										player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
										return;
									}
								}
								break;
							}
							case HUMAN:
							case DWARF:
							case ELF:
							case DARK_ELF:
							case ORC:
							{
								switch (wpn.getItemType())
								{
									case RAPIER:
									case CROSSBOW:
									case ANCIENTSWORD:
									{
										player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
										return;
									}
								}
								break;
							}
						}
					}
					break;
				}
				case Item.SLOT_CHEST:
				case Item.SLOT_BACK:
				case Item.SLOT_GLOVES:
				case Item.SLOT_FEET:
				case Item.SLOT_HEAD:
				case Item.SLOT_FULL_ARMOR:
				case Item.SLOT_LEGS:
				{
					if ((player.getRace() == Race.KAMAEL) && ((item.getItem().getItemType() == ArmorType.HEAVY) || (item.getItem().getItemType() == ArmorType.MAGIC)))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					break;
				}
				case Item.SLOT_DECO:
				{
					if (!item.isEquipped() && (player.getInventory().getTalismanSlots() == 0))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
				}
			}
			
			if (player.isCastingNow() || player.isCastingSimultaneouslyNow())
			{
				// Creating next action class.
				final NextAction nextAction = new NextAction(CtrlEvent.EVT_FINISH_CASTING, CtrlIntention.AI_INTENTION_CAST, () -> player.useEquippableItem(item, true));
				
				// Binding next action to AI.
				player.getAI().setNextAction(nextAction);
			}
			else if (player.isAttackingNow())
			{
				// Equip or unEquip.
				ThreadPool.schedule(() -> player.useEquippableItem(item, false), TimeUnit.MILLISECONDS.convert(player.getAttackEndTime() - System.nanoTime(), TimeUnit.NANOSECONDS));
			}
			else
			{
				player.useEquippableItem(item, true);
			}
		}
		else
		{
			final Weapon weaponItem = player.getActiveWeaponItem();
			if (((weaponItem != null) && (weaponItem.getItemType() == WeaponType.FISHINGROD)) && (((_itemId >= 6519) && (_itemId <= 6527)) || ((_itemId >= 7610) && (_itemId <= 7613)) || ((_itemId >= 7807) && (_itemId <= 7809)) || ((_itemId >= 8484) && (_itemId <= 8486)) || ((_itemId >= 8505) && (_itemId <= 8513))))
			{
				player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
				player.broadcastUserInfo();
				// Send a Server->Client packet ItemList to this PlayerInstance to update left hand equipment.
				client.sendPacket(new ItemList(player, false));
				return;
			}
			
			final EtcItem etcItem = item.getEtcItem();
			final IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
			if (handler == null)
			{
				if ((etcItem != null) && (etcItem.getHandlerName() != null))
				{
					LOGGER.log(Level.WARNING, "Unmanaged Item handler: " + etcItem.getHandlerName() + " for Item Id: " + _itemId + "!");
				}
				return;
			}
			
			// Item reuse time should be added if the item is successfully used.
			// Skill reuse delay is done at handlers.itemhandlers.ItemSkillsTemplate;
			if (handler.useItem(player, item, _ctrlPressed) && (reuseDelay > 0))
			{
				player.addTimeStampItem(item, reuseDelay);
				sendSharedGroupUpdate(player, sharedReuseGroup, reuseDelay, reuseDelay);
			}
		}
	}
	
	private void reuseData(PlayerInstance player, ItemInstance item, long remainingTime)
	{
		final int hours = (int) (remainingTime / 3600000);
		final int minutes = (int) (remainingTime % 3600000) / 60000;
		final int seconds = (int) ((remainingTime / 1000) % 60);
		final SystemMessage sm;
		if (hours > 0)
		{
			sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_HOUR_S_S3_MINUTE_S_AND_S4_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
			sm.addItemName(item);
			sm.addInt(hours);
			sm.addInt(minutes);
		}
		else if (minutes > 0)
		{
			sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_MINUTE_S_S3_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
			sm.addItemName(item);
			sm.addInt(minutes);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
			sm.addItemName(item);
		}
		sm.addInt(seconds);
		player.sendPacket(sm);
	}
	
	private void sendSharedGroupUpdate(PlayerInstance player, int group, long remaining, int reuse)
	{
		if (group > 0)
		{
			player.sendPacket(new ExUseSharedGroupItem(_itemId, group, remaining, reuse));
		}
	}
}
