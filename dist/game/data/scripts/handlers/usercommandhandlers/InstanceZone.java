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
package handlers.usercommandhandlers;

import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Instance Zone user command.
 * @author nille02
 */
public class InstanceZone implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		114
	};
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
	
	@Override
	public boolean useUserCommand(int id, PlayerInstance player)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		if ((world != null) && (world.getTemplateId() >= 0))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.INSTANT_ZONE_CURRENTLY_IN_USE_S1);
			sm.addInstanceName(world.getTemplateId());
			player.sendPacket(sm);
		}
		
		final Map<Integer, Long> instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(player.getObjectId());
		boolean firstMessage = true;
		if (instanceTimes != null)
		{
			for (Entry<Integer, Long> entry : instanceTimes.entrySet())
			{
				final long remainingTime = (entry.getValue() - Chronos.currentTimeMillis()) / 1000;
				if (remainingTime > 60)
				{
					if (firstMessage)
					{
						firstMessage = false;
						player.sendPacket(SystemMessageId.INSTANCE_ZONE_TIME_LIMIT);
					}
					final int hours = (int) (remainingTime / 3600);
					final int minutes = (int) ((remainingTime % 3600) / 60);
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_FOR_RE_USE_AFTER_S2_HOUR_S_S3_MINUTE_S);
					sm.addInstanceName(entry.getKey());
					sm.addInt(hours);
					sm.addInt(minutes);
					player.sendPacket(sm);
				}
				else
				{
					InstanceManager.getInstance().deleteInstanceTime(player.getObjectId(), entry.getKey());
				}
			}
		}
		if (firstMessage)
		{
			player.sendPacket(SystemMessageId.THERE_IS_NO_INSTANCE_ZONE_UNDER_A_TIME_LIMIT);
		}
		return true;
	}
}
