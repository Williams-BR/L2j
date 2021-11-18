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

import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.enchant.EnchantResultType;
import org.l2jmobius.gameserver.model.items.enchant.EnchantScroll;
import org.l2jmobius.gameserver.model.items.enchant.EnchantSupportItem;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.skills.CommonSkill;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

public class RequestEnchantItem implements IClientIncomingPacket
{
	protected static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.items");
	
	private int _objectId;
	private int _supportId;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_objectId = packet.readD();
		_supportId = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if ((player == null) || (_objectId == 0))
		{
			return;
		}
		
		if (!player.isOnline() || client.isDetached())
		{
			player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
			return;
		}
		
		if (player.isProcessingTransaction() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ENCHANT_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
			return;
		}
		
		final ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		ItemInstance scroll = player.getInventory().getItemByObjectId(player.getActiveEnchantItemId());
		ItemInstance support = player.getInventory().getItemByObjectId(player.getActiveEnchantSupportItemId());
		if ((item == null) || (scroll == null))
		{
			player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
			return;
		}
		
		// template for scroll
		final EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(scroll);
		
		// scroll not found in list
		if (scrollTemplate == null)
		{
			return;
		}
		
		// template for support item, if exist
		EnchantSupportItem supportTemplate = null;
		if (support != null)
		{
			if (support.getObjectId() != _supportId)
			{
				player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
				return;
			}
			supportTemplate = EnchantItemData.getInstance().getSupportItem(support);
		}
		
		// first validation check - also over enchant check
		if (!scrollTemplate.isValid(item, supportTemplate) || (Config.DISABLE_OVER_ENCHANTING && (item.getEnchantLevel() == scrollTemplate.getMaxEnchantLevel())))
		{
			player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
			player.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		// fast auto-enchant cheat check
		if ((player.getActiveEnchantTimestamp() == 0) || ((Chronos.currentTimeMillis() - player.getActiveEnchantTimestamp()) < 2000))
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " use autoenchant program ", Config.DEFAULT_PUNISH);
			player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
			player.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		// attempting to destroy scroll
		scroll = player.getInventory().destroyItem("Enchant", scroll.getObjectId(), 1, player, item);
		if (scroll == null)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to enchant with a scroll he doesn't have", Config.DEFAULT_PUNISH);
			player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
			player.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		// attempting to destroy support if exist
		if (support != null)
		{
			support = player.getInventory().destroyItem("Enchant", support.getObjectId(), 1, player, item);
			if (support == null)
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to enchant with a support item he doesn't have", Config.DEFAULT_PUNISH);
				player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
				player.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		synchronized (item)
		{
			// last validation check
			if ((item.getOwnerId() != player.getObjectId()) || !item.isEnchantable())
			{
				player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
				player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
				player.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}
			
			final EnchantResultType resultType = scrollTemplate.calculateSuccess(player, item, supportTemplate);
			switch (resultType)
			{
				case ERROR:
				{
					player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
					player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
					player.sendPacket(new EnchantResult(2, 0, 0));
					break;
				}
				case SUCCESS:
				{
					Skill enchant4Skill = null;
					final Item it = item.getItem();
					// Increase enchant level only if scroll's base template has chance, some armors can success over +20 but they shouldn't have increased.
					if (scrollTemplate.getChance(player, item) > 0)
					{
						item.setEnchantLevel(item.getEnchantLevel() + 1);
						item.updateDatabase();
					}
					player.sendPacket(new EnchantResult(0, 0, 0));
					if (Config.LOG_ITEM_ENCHANTS)
					{
						if (item.getEnchantLevel() > 0)
						{
							if (support == null)
							{
								LOGGER_ENCHANT.info("Success, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
							}
							else
							{
								LOGGER_ENCHANT.info("Success, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
							}
						}
						else if (support == null)
						{
							LOGGER_ENCHANT.info("Success, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
						}
						else
						{
							LOGGER_ENCHANT.info("Success, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
						}
					}
					
					// announce the success
					final int minEnchantAnnounce = item.isArmor() ? 6 : 7;
					final int maxEnchantAnnounce = item.isArmor() ? 0 : 15;
					if ((item.getEnchantLevel() == minEnchantAnnounce) || (item.getEnchantLevel() == maxEnchantAnnounce))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_SUCCESSFULLY_ENCHANTED_A_S2_S3);
						sm.addString(player.getName());
						sm.addInt(item.getEnchantLevel());
						sm.addItemName(item);
						player.broadcastPacket(sm);
						
						final Skill skill = CommonSkill.FIREWORK.getSkill();
						if (skill != null)
						{
							player.broadcastPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
						}
					}
					
					if ((item.isArmor()) && (item.getEnchantLevel() == 4) && item.isEquipped())
					{
						enchant4Skill = it.getEnchant4Skill();
						if (enchant4Skill != null)
						{
							// add skills bestowed from +4 armor
							player.addSkill(enchant4Skill, false);
							player.sendSkillList();
						}
					}
					break;
				}
				case FAILURE:
				{
					if (scrollTemplate.isSafe())
					{
						// safe enchant - remain old value
						player.sendPacket(SystemMessageId.ENCHANT_FAILED_THE_ENCHANT_LEVEL_FOR_THE_CORRESPONDING_ITEM_WILL_BE_EXACTLY_RETAINED);
						player.sendPacket(new EnchantResult(5, 0, 0));
						if (Config.LOG_ITEM_ENCHANTS)
						{
							if (item.getEnchantLevel() > 0)
							{
								if (support == null)
								{
									LOGGER_ENCHANT.info("Safe Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
								}
								else
								{
									LOGGER_ENCHANT.info("Safe Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
								}
							}
							else if (support == null)
							{
								LOGGER_ENCHANT.info("Safe Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
							}
							else
							{
								LOGGER_ENCHANT.info("Safe Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
							}
						}
					}
					else
					{
						// unequip item on enchant failure to avoid item skills stack
						if (item.isEquipped())
						{
							if (item.getEnchantLevel() > 0)
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
								sm.addInt(item.getEnchantLevel());
								sm.addItemName(item);
								player.sendPacket(sm);
							}
							else
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
								sm.addItemName(item);
								player.sendPacket(sm);
							}
							
							for (ItemInstance itm : player.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot()))
							{
								iu.addModifiedItem(itm);
							}
							player.sendPacket(iu);
							player.broadcastUserInfo();
						}
						
						if (scrollTemplate.isBlessed())
						{
							// blessed enchant - clear enchant value
							player.sendPacket(SystemMessageId.THE_BLESSED_ENCHANT_FAILED_THE_ENCHANT_VALUE_OF_THE_ITEM_BECAME_0);
							
							item.setEnchantLevel(0);
							item.updateDatabase();
							player.sendPacket(new EnchantResult(3, 0, 0));
							if (Config.LOG_ITEM_ENCHANTS)
							{
								if (item.getEnchantLevel() > 0)
								{
									if (support == null)
									{
										LOGGER_ENCHANT.info("Blessed Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
									}
									else
									{
										LOGGER_ENCHANT.info("Blessed Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
									}
								}
								else if (support == null)
								{
									LOGGER_ENCHANT.info("Blessed Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
								}
								else
								{
									LOGGER_ENCHANT.info("Blessed Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
								}
							}
						}
						else
						{
							// enchant failed, destroy item
							if (player.getInventory().destroyItem("Enchant", item, player, null) == null)
							{
								// unable to destroy item, cheater ?
								Util.handleIllegalPlayerAction(player, "Unable to delete item on enchant failure from player " + player.getName() + ", possible cheater !", Config.DEFAULT_PUNISH);
								player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
								player.sendPacket(new EnchantResult(2, 0, 0));
								if (Config.LOG_ITEM_ENCHANTS)
								{
									if (item.getEnchantLevel() > 0)
									{
										if (support == null)
										{
											LOGGER_ENCHANT.info("Unable to destroy, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
										}
										else
										{
											LOGGER_ENCHANT.info("Unable to destroy, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
										}
									}
									else if (support == null)
									{
										LOGGER_ENCHANT.info("Unable to destroy, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
									}
									else
									{
										LOGGER_ENCHANT.info("Unable to destroy, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
									}
								}
								return;
							}
							
							World.getInstance().removeObject(item);
							
							final int crystalId = item.getItem().getCrystalItemId();
							if ((crystalId != 0) && item.getItem().isCrystallizable())
							{
								int count = item.getCrystalCount() - ((item.getItem().getCrystalCount() + 1) / 2);
								count = count < 1 ? 1 : count;
								player.getInventory().addItem("Enchant", crystalId, count, player, item);
								
								final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
								sm.addItemName(crystalId);
								sm.addLong(count);
								player.sendPacket(sm);
								player.sendPacket(new EnchantResult(1, crystalId, count));
							}
							else
							{
								player.sendPacket(new EnchantResult(4, 0, 0));
							}
							
							if (Config.LOG_ITEM_ENCHANTS)
							{
								if (item.getEnchantLevel() > 0)
								{
									if (support == null)
									{
										LOGGER_ENCHANT.info("Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
									}
									else
									{
										LOGGER_ENCHANT.info("Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", +" + item.getEnchantLevel() + " " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
									}
								}
								else if (support == null)
								{
									LOGGER_ENCHANT.info("Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "]");
								}
								else
								{
									LOGGER_ENCHANT.info("Fail, Character:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", " + item.getName() + "(" + item.getCount() + ") [" + item.getObjectId() + "], " + scroll.getName() + "(" + scroll.getCount() + ") [" + scroll.getObjectId() + "], " + support.getName() + "(" + support.getCount() + ") [" + support.getObjectId() + "]");
								}
							}
						}
					}
					break;
				}
			}
			
			final StatusUpdate su = new StatusUpdate(player);
			su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
			player.sendPacket(su);
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				if (scroll.getCount() == 0)
				{
					iu.addRemovedItem(scroll);
				}
				else
				{
					iu.addModifiedItem(scroll);
				}
				
				if (item.getCount() == 0)
				{
					iu.addRemovedItem(item);
				}
				else
				{
					iu.addModifiedItem(item);
				}
				
				if (support != null)
				{
					if (support.getCount() == 0)
					{
						iu.addRemovedItem(support);
					}
					else
					{
						iu.addModifiedItem(support);
					}
				}
				
				player.sendPacket(iu);
			}
			else
			{
				player.sendPacket(new ItemList(player, true));
			}
			
			player.broadcastUserInfo();
			player.setActiveEnchantItemId(PlayerInstance.ID_NONE);
		}
	}
}
