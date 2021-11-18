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
package org.l2jmobius.gameserver.taskmanager;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.instancemanager.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Mobius
 */
public class MessageDeletionTaskManager implements Runnable
{
	private static final Map<Integer, Long> PENDING_MESSAGES = new ConcurrentHashMap<>();
	private static boolean _working = false;
	
	protected MessageDeletionTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 10000, 10000);
	}
	
	@Override
	public void run()
	{
		if (_working)
		{
			return;
		}
		_working = true;
		
		Integer msgId;
		Message msg;
		final long time = Chronos.currentTimeMillis();
		for (Entry<Integer, Long> entry : PENDING_MESSAGES.entrySet())
		{
			if (time > entry.getValue().longValue())
			{
				msgId = entry.getKey();
				msg = MailManager.getInstance().getMessage(msgId.intValue());
				if (msg == null)
				{
					PENDING_MESSAGES.remove(msgId);
					return;
				}
				
				if (msg.hasAttachments())
				{
					final PlayerInstance sender = World.getInstance().getPlayer(msg.getSenderId());
					if (sender != null)
					{
						msg.getAttachments().returnToWh(sender.getWarehouse());
						sender.sendPacket(SystemMessageId.THE_MAIL_WAS_RETURNED_DUE_TO_THE_EXCEEDED_WAITING_TIME);
					}
					else
					{
						msg.getAttachments().returnToWh(null);
					}
					msg.getAttachments().deleteMe();
					msg.removeAttachments();
					
					final PlayerInstance receiver = World.getInstance().getPlayer(msg.getReceiverId());
					if (receiver != null)
					{
						receiver.sendPacket(new SystemMessage(SystemMessageId.THE_MAIL_WAS_RETURNED_DUE_TO_THE_EXCEEDED_WAITING_TIME));
					}
				}
				
				MailManager.getInstance().deleteMessageInDb(msgId.intValue());
				PENDING_MESSAGES.remove(msgId);
			}
		}
		
		_working = false;
	}
	
	public void add(int msgId, long deletionTime)
	{
		PENDING_MESSAGES.put(msgId, deletionTime);
	}
	
	public static MessageDeletionTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MessageDeletionTaskManager INSTANCE = new MessageDeletionTaskManager();
	}
}
