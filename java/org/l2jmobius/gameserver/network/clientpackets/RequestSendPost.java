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

import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.ADENA_ID;
import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.MAX_ADENA;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.data.sql.CharNameTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.instancemanager.MailManager;
import org.l2jmobius.gameserver.model.AccessLevel;
import org.l2jmobius.gameserver.model.BlockList;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExNoticePostSent;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Migi, DS
 */
public class RequestSendPost implements IClientIncomingPacket
{
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private static final int MAX_RECV_LENGTH = 16;
	private static final int MAX_SUBJ_LENGTH = 128;
	private static final int MAX_TEXT_LENGTH = 512;
	private static final int MAX_ATTACHMENTS = 8;
	private static final int INBOX_SIZE = 240;
	private static final int OUTBOX_SIZE = 240;
	
	private static final int MESSAGE_FEE = 100;
	private static final int MESSAGE_FEE_PER_SLOT = 1000; // 100 adena message fee + 1000 per each item slot
	
	private String _receiver;
	private boolean _isCod;
	private String _subject;
	private String _text;
	private AttachmentItem[] _items = null;
	private long _reqAdena;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_receiver = packet.readS();
		_isCod = packet.readD() != 0;
		_subject = packet.readS();
		_text = packet.readS();
		
		final int attachCount = packet.readD();
		if ((attachCount < 0) || (attachCount > Config.MAX_ITEM_IN_PACKET) || (((attachCount * BATCH_LENGTH) + 8) != packet.getReadableBytes()))
		{
			return false;
		}
		
		if (attachCount > 0)
		{
			_items = new AttachmentItem[attachCount];
			for (int i = 0; i < attachCount; i++)
			{
				final int objectId = packet.readD();
				final long count = packet.readQ();
				if ((objectId < 1) || (count < 0))
				{
					_items = null;
					return false;
				}
				_items[i] = new AttachmentItem(objectId, count);
			}
		}
		
		_reqAdena = packet.readQ();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		if (!Config.ALLOW_MAIL)
		{
			return;
		}
		
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!Config.ALLOW_ATTACHMENTS)
		{
			_items = null;
			_isCod = false;
			_reqAdena = 0;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			return;
		}
		
		if (!player.isInsideZone(ZoneId.PEACE) && (_items != null))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FORWARD_IN_A_NON_PEACE_ZONE_LOCATION);
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FORWARD_DURING_AN_EXCHANGE);
			return;
		}
		
		if (player.isEnchanting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FORWARD_DURING_AN_ITEM_ENHANCEMENT_OR_ATTRIBUTE_ENHANCEMENT);
			return;
		}
		
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FORWARD_BECAUSE_THE_PRIVATE_SHOP_OR_WORKSHOP_IS_IN_PROGRESS);
			return;
		}
		
		if (_receiver.length() > MAX_RECV_LENGTH)
		{
			player.sendPacket(SystemMessageId.THE_ALLOWED_LENGTH_FOR_RECIPIENT_EXCEEDED);
			return;
		}
		
		if (_subject.length() > MAX_SUBJ_LENGTH)
		{
			player.sendPacket(SystemMessageId.THE_ALLOWED_LENGTH_FOR_A_TITLE_EXCEEDED);
			return;
		}
		
		if (_text.length() > MAX_TEXT_LENGTH)
		{
			// not found message for this
			player.sendPacket(SystemMessageId.THE_ALLOWED_LENGTH_FOR_A_TITLE_EXCEEDED);
			return;
		}
		
		if ((_items != null) && (_items.length > MAX_ATTACHMENTS))
		{
			player.sendPacket(SystemMessageId.ITEM_SELECTION_IS_POSSIBLE_UP_TO_8);
			return;
		}
		
		if ((_reqAdena < 0) || (_reqAdena > MAX_ADENA))
		{
			return;
		}
		
		if (_isCod)
		{
			if (_reqAdena == 0)
			{
				player.sendPacket(SystemMessageId.WHEN_NOT_ENTERING_THE_AMOUNT_FOR_THE_PAYMENT_REQUEST_YOU_CANNOT_SEND_ANY_MAIL);
				return;
			}
			if ((_items == null) || (_items.length == 0))
			{
				player.sendPacket(SystemMessageId.IT_S_A_PAYMENT_REQUEST_TRANSACTION_PLEASE_ATTACH_THE_ITEM);
				return;
			}
		}
		
		final int receiverId = CharNameTable.getInstance().getIdByName(_receiver);
		if (receiverId <= 0)
		{
			player.sendPacket(SystemMessageId.WHEN_THE_RECIPIENT_DOESN_T_EXIST_OR_THE_CHARACTER_HAS_BEEN_DELETED_SENDING_MAIL_IS_NOT_POSSIBLE);
			return;
		}
		
		if (receiverId == player.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SEND_A_MAIL_TO_YOURSELF);
			return;
		}
		
		final int level = CharNameTable.getInstance().getAccessLevelById(receiverId);
		final AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(level);
		if (accessLevel.isGm() && !player.getAccessLevel().isGm())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_MESSAGE_TO_C1_DID_NOT_REACH_ITS_RECIPIENT_YOU_CANNOT_SEND_MAIL_TO_THE_GM_STAFF);
			sm.addString(_receiver);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isJailed() && ((Config.JAIL_DISABLE_TRANSACTION && (_items != null)) || Config.JAIL_DISABLE_CHAT))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FORWARD_IN_A_NON_PEACE_ZONE_LOCATION);
			return;
		}
		
		if (BlockList.isInBlockList(receiverId, player.getObjectId()))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BLOCKED_YOU_YOU_CANNOT_SEND_MAIL_TO_C1);
			sm.addString(_receiver);
			player.sendPacket(sm);
			return;
		}
		
		if (MailManager.getInstance().getOutboxSize(player.getObjectId()) >= OUTBOX_SIZE)
		{
			player.sendPacket(SystemMessageId.THE_MAIL_LIMIT_240_HAS_BEEN_EXCEEDED_AND_THIS_CANNOT_BE_FORWARDED);
			return;
		}
		
		if (MailManager.getInstance().getInboxSize(receiverId) >= INBOX_SIZE)
		{
			player.sendPacket(SystemMessageId.THE_MAIL_LIMIT_240_HAS_BEEN_EXCEEDED_AND_THIS_CANNOT_BE_FORWARDED);
			return;
		}
		
		if (!client.getFloodProtectors().getSendMail().tryPerformAction("sendmail"))
		{
			player.sendPacket(SystemMessageId.THE_PREVIOUS_MAIL_WAS_FORWARDED_LESS_THAN_1_MINUTE_AGO_AND_THIS_CANNOT_BE_FORWARDED);
			return;
		}
		
		final Message msg = new Message(player.getObjectId(), receiverId, _isCod, _subject, _text, _reqAdena);
		if (removeItems(player, msg))
		{
			MailManager.getInstance().sendMessage(msg);
			player.sendPacket(ExNoticePostSent.valueOf(true));
			player.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_SENT);
		}
	}
	
	private final boolean removeItems(PlayerInstance player, Message msg)
	{
		long currentAdena = player.getAdena();
		long fee = MESSAGE_FEE;
		if (_items != null)
		{
			for (AttachmentItem i : _items)
			{
				// Check validity of requested item
				final ItemInstance item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "attach");
				if ((item == null) || !item.isTradeable() || item.isEquipped())
				{
					player.sendPacket(SystemMessageId.THE_ITEM_THAT_YOU_RE_TRYING_TO_SEND_CANNOT_BE_FORWARDED_BECAUSE_IT_ISN_T_PROPER);
					return false;
				}
				
				fee += MESSAGE_FEE_PER_SLOT;
				if (item.getId() == ADENA_ID)
				{
					currentAdena -= i.getCount();
				}
			}
		}
		
		// Check if enough adena and charge the fee
		if ((currentAdena < fee) || !player.reduceAdena("MailFee", fee, null, false))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FORWARD_BECAUSE_YOU_DON_T_HAVE_ENOUGH_ADENA);
			return false;
		}
		
		if (_items == null)
		{
			return true;
		}
		
		final Mail attachments = msg.createAttachments();
		
		// message already has attachments ? oO
		if (attachments == null)
		{
			return false;
		}
		
		final StringBuilder recv = new StringBuilder(32);
		recv.append(msg.getReceiverName() + "[" + msg.getReceiverId() + "]");
		final String receiver = recv.toString();
		
		// Proceed to the transfer
		final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (AttachmentItem i : _items)
		{
			// Check validity of requested item
			final ItemInstance oldItem = player.checkItemManipulation(i.getObjectId(), i.getCount(), "attach");
			if ((oldItem == null) || !oldItem.isTradeable() || oldItem.isEquipped())
			{
				LOGGER.warning("Error adding attachment for char " + player.getName() + " (olditem == null)");
				return false;
			}
			
			final ItemInstance newItem = player.getInventory().transferItem("SendMail", i.getObjectId(), i.getCount(), attachments, player, receiver);
			if (newItem == null)
			{
				LOGGER.warning("Error adding attachment for char " + player.getName() + " (newitem == null)");
				continue;
			}
			newItem.setItemLocation(newItem.getItemLocation(), msg.getId());
			if (playerIU != null)
			{
				if ((oldItem.getCount() > 0) && (oldItem != newItem))
				{
					playerIU.addModifiedItem(oldItem);
				}
				else
				{
					playerIU.addRemovedItem(oldItem);
				}
			}
		}
		
		// Send updated item list to the player
		if (playerIU != null)
		{
			player.sendPacket(playerIU);
		}
		else
		{
			player.sendPacket(new ItemList(player, false));
		}
		
		// Update current load status on player
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		return true;
	}
	
	private static class AttachmentItem
	{
		private final int _objectId;
		private final long _count;
		
		public AttachmentItem(int id, long num)
		{
			_objectId = id;
			_count = num;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public long getCount()
		{
			return _count;
		}
	}
}
