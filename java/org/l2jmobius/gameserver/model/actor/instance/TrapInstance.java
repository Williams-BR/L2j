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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.enums.TrapAction;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.tasks.npc.trap.TrapTask;
import org.l2jmobius.gameserver.model.actor.tasks.npc.trap.TrapTriggerTask;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.impl.creature.trap.OnTrapAction;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo.TrapInfo;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanager.DecayTaskManager;

/**
 * Trap instance.
 * @author Zoey76
 */
public class TrapInstance extends Npc
{
	private static final int TICK = 1000; // 1s
	private boolean _hasLifeTime;
	private boolean _isInArena = false;
	private boolean _isTriggered;
	private final int _lifeTime;
	private PlayerInstance _owner;
	private final List<Integer> _playersWhoDetectedMe = new ArrayList<>();
	private final SkillHolder _skill;
	private int _remainingTime;
	// Tasks
	private ScheduledFuture<?> _trapTask = null;
	
	/**
	 * Creates a trap.
	 * @param template the trap NPC template
	 * @param instanceId the instance ID
	 * @param lifeTime the life time
	 */
	public TrapInstance(NpcTemplate template, int instanceId, int lifeTime)
	{
		super(template);
		setInstanceType(InstanceType.TrapInstance);
		setInstanceId(instanceId);
		setName(template.getName());
		setInvul(false);
		
		_owner = null;
		_isTriggered = false;
		_skill = getTemplate().getParameters().getObject("trap_skill", SkillHolder.class);
		_hasLifeTime = lifeTime >= 0;
		_lifeTime = lifeTime != 0 ? lifeTime : 30000;
		_remainingTime = _lifeTime;
		if (_skill != null)
		{
			_trapTask = ThreadPool.scheduleAtFixedRate(new TrapTask(this), TICK, TICK);
		}
	}
	
	/**
	 * Creates a trap.
	 * @param template the trap NPC template
	 * @param owner the owner
	 * @param lifeTime the life time
	 */
	public TrapInstance(NpcTemplate template, PlayerInstance owner, int lifeTime)
	{
		this(template, owner.getInstanceId(), lifeTime);
		_owner = owner;
	}
	
	@Override
	public void broadcastPacket(IClientOutgoingPacket mov)
	{
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			if (_isTriggered || canBeSeen(player))
			{
				player.sendPacket(mov);
			}
		});
	}
	
	@Override
	public void broadcastPacket(IClientOutgoingPacket mov, int radiusInKnownlist)
	{
		World.getInstance().forEachVisibleObjectInRange(this, PlayerInstance.class, radiusInKnownlist, player ->
		{
			if (_isTriggered || canBeSeen(player))
			{
				player.sendPacket(mov);
			}
		});
	}
	
	/**
	 * Verify if the character can see the trap.
	 * @param creature The creature to verify
	 * @return {@code true} if the character can see the trap, {@code false} otherwise
	 */
	public boolean canBeSeen(Creature creature)
	{
		if ((creature != null) && _playersWhoDetectedMe.contains(creature.getObjectId()))
		{
			return true;
		}
		
		if ((_owner == null) || (creature == null))
		{
			return false;
		}
		if (creature == _owner)
		{
			return true;
		}
		
		if (creature.isPlayer())
		{
			// observers can't see trap
			if (((PlayerInstance) creature).inObserverMode())
			{
				return false;
			}
			
			// olympiad competitors can't see trap
			if (_owner.isInOlympiadMode() && ((PlayerInstance) creature).isInOlympiadMode() && (((PlayerInstance) creature).getOlympiadSide() != _owner.getOlympiadSide()))
			{
				return false;
			}
		}
		
		if (_isInArena)
		{
			return true;
		}
		
		if (_owner.isInParty() && creature.isInParty() && (_owner.getParty().getLeaderObjectId() == creature.getParty().getLeaderObjectId()))
		{
			return true;
		}
		return false;
	}
	
	public boolean checkTarget(Creature target)
	{
		// Range seems to be reduced from Freya(300) to H5(150)
		if (!target.isInsideRadius2D(this, 150))
		{
			return false;
		}
		
		if (!Skill.checkForAreaOffensiveSkills(this, target, _skill.getSkill(), _isInArena))
		{
			return false;
		}
		
		// observers
		if (target.isPlayer() && target.getActingPlayer().inObserverMode())
		{
			return false;
		}
		
		// olympiad own team and their summons not attacked
		if ((_owner != null) && _owner.isInOlympiadMode())
		{
			final PlayerInstance player = target.getActingPlayer();
			if ((player != null) && player.isInOlympiadMode() && (player.getOlympiadSide() == _owner.getOlympiadSide()))
			{
				return false;
			}
		}
		
		if (_isInArena)
		{
			return true;
		}
		
		// trap owned by players not attack non-flagged players
		if (_owner != null)
		{
			if (target.isAttackable())
			{
				return true;
			}
			
			final PlayerInstance player = target.getActingPlayer();
			if ((player == null) || ((player.getPvpFlag() == 0) && (player.getKarma() == 0)))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean deleteMe()
	{
		if (_owner != null)
		{
			_owner.setTrap(null);
			_owner = null;
		}
		return super.deleteMe();
	}
	
	@Override
	public PlayerInstance getActingPlayer()
	{
		return _owner;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public int getKarma()
	{
		return _owner != null ? _owner.getKarma() : 0;
	}
	
	/**
	 * Get the owner of this trap.
	 * @return the owner
	 */
	public PlayerInstance getOwner()
	{
		return _owner;
	}
	
	public byte getPvpFlag()
	{
		return _owner != null ? _owner.getPvpFlag() : 0;
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
	
	public Skill getSkill()
	{
		return _skill.getSkill();
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return !canBeSeen(attacker);
	}
	
	@Override
	public boolean isTrap()
	{
		return true;
	}
	
	/**
	 * Checks is triggered
	 * @return True if trap is triggered.
	 */
	public boolean isTriggered()
	{
		return _isTriggered;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_isInArena = isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE);
		_playersWhoDetectedMe.clear();
	}
	
	@Override
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss || (_owner == null))
		{
			return;
		}
		
		if (_owner.isInOlympiadMode() && target.isPlayer() && ((PlayerInstance) target).isInOlympiadMode() && (((PlayerInstance) target).getOlympiadGameId() == _owner.getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(getOwner(), damage);
		}
		
		if (target.isInvul() && !target.isNpc())
		{
			_owner.sendPacket(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_DONE_S3_POINTS_OF_DAMAGE_TO_C2);
			sm.addString(getName());
			sm.addString(target.getName());
			sm.addInt(damage);
			_owner.sendPacket(sm);
		}
	}
	
	@Override
	public void sendInfo(PlayerInstance player)
	{
		if (_isTriggered || canBeSeen(player))
		{
			player.sendPacket(new TrapInfo(this, player));
		}
	}
	
	public void setDetected(Creature detector)
	{
		if (_isInArena)
		{
			if (detector.isPlayable())
			{
				sendInfo(detector.getActingPlayer());
			}
			return;
		}
		
		if ((_owner != null) && (_owner.getPvpFlag() == 0) && (_owner.getKarma() == 0))
		{
			return;
		}
		
		_playersWhoDetectedMe.add(detector.getObjectId());
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, detector, TrapAction.TRAP_DETECTED), this);
		if (detector.isPlayable())
		{
			sendInfo(detector.getActingPlayer());
		}
	}
	
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancel(this);
	}
	
	/**
	 * Trigger the trap.
	 * @param target the target
	 */
	public void triggerTrap(Creature target)
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		
		_isTriggered = true;
		broadcastPacket(new TrapInfo(this, null));
		setTarget(target);
		
		EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, target, TrapAction.TRAP_TRIGGERED), this);
		ThreadPool.schedule(new TrapTriggerTask(this), 500);
	}
	
	public void unSummon()
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		
		if (_owner != null)
		{
			_owner.setTrap(null);
			_owner = null;
		}
		
		if (isSpawned() && !isDead())
		{
			ZoneManager.getInstance().getRegion(this).removeFromZones(this);
			deleteMe();
		}
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	public boolean hasLifeTime()
	{
		return _hasLifeTime;
	}
	
	public void setHasLifeTime(boolean value)
	{
		_hasLifeTime = value;
	}
	
	public int getRemainingTime()
	{
		return _remainingTime;
	}
	
	public void setRemainingTime(int time)
	{
		_remainingTime = time;
	}
	
	public int getLifeTime()
	{
		return _lifeTime;
	}
}
