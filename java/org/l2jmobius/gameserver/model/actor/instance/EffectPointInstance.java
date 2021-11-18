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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class EffectPointInstance extends Npc
{
	private final PlayerInstance _owner;
	
	/**
	 * Creates an effect point.
	 * @param template the effect point NPC template
	 * @param owner the owner
	 */
	public EffectPointInstance(NpcTemplate template, Creature owner)
	{
		super(template);
		setInstanceType(InstanceType.EffectPointInstance);
		setInvul(false);
		_owner = owner == null ? null : owner.getActingPlayer();
		if (owner != null)
		{
			setInstanceId(owner.getInstanceId());
		}
	}
	
	@Override
	public PlayerInstance getActingPlayer()
	{
		return _owner;
	}
	
	/**
	 * this is called when a player interacts with this NPC
	 * @param player
	 */
	@Override
	public void onAction(PlayerInstance player, boolean interact)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onActionShift(PlayerInstance player)
	{
		if (player == null)
		{
			return;
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}