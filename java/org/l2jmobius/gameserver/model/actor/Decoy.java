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
package org.l2jmobius.gameserver.model.actor;

import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CharInfo;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.taskmanager.DecayTaskManager;

public abstract class Decoy extends Creature
{
	private final PlayerInstance _owner;
	
	/**
	 * Creates an abstract decoy.
	 * @param template the decoy template
	 * @param owner the owner
	 */
	public Decoy(CreatureTemplate template, PlayerInstance owner)
	{
		super(template);
		setInstanceType(InstanceType.Decoy);
		_owner = owner;
		setXYZInvisible(owner.getX(), owner.getY(), owner.getZ());
		setInvul(false);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		sendPacket(new CharInfo(this, false));
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(new CharInfo(this, false));
			}
		});
	}
	
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancel(this);
	}
	
	@Override
	public void onDecay()
	{
		deleteMe(_owner);
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return _owner.isAutoAttackable(attacker);
	}
	
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	public void deleteMe(PlayerInstance owner)
	{
		decayMe();
		owner.setDecoy(null);
	}
	
	public synchronized void unSummon(PlayerInstance owner)
	{
		if (!isSpawned() || isDead())
		{
			return;
		}
		ZoneManager.getInstance().getRegion(this).removeFromZones(this);
		owner.setDecoy(null);
		decayMe();
	}
	
	public PlayerInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	public PlayerInstance getActingPlayer()
	{
		return _owner;
	}
	
	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	@Override
	public void sendInfo(PlayerInstance player)
	{
		player.sendPacket(new CharInfo(this, false));
	}
	
	@Override
	public void sendPacket(IClientOutgoingPacket... packets)
	{
		if (_owner != null)
		{
			_owner.sendPacket(packets);
		}
	}
	
	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (_owner != null)
		{
			_owner.sendPacket(id);
		}
	}
}
