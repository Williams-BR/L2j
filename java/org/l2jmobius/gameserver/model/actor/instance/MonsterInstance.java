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

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.util.MinionList;

/**
 * This class manages all Monsters. MonsterInstance:
 * <ul>
 * <li>MinionInstance</li>
 * <li>RaidBossInstance</li>
 * <li>GrandBossInstance</li>
 * </ul>
 */
public class MonsterInstance extends Attackable
{
	protected boolean _enableMinions = true;
	
	private MonsterInstance _master = null;
	private MinionList _minionList = null;
	
	/**
	 * Creates a monster.
	 * @param template the monster NPC template
	 */
	public MonsterInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.MonsterInstance);
		setAutoAttackable(true);
	}
	
	/**
	 * Return True if the attacker is not another MonsterInstance.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (isFakePlayer())
		{
			return isInCombat() || attacker.isMonster() || (getScriptValue() > 0);
		}
		
		// Check if the MonsterInstance target is aggressive
		if (Config.GUARD_ATTACK_AGGRO_MOB && isAggressive() && (attacker instanceof GuardInstance))
		{
			return true;
		}
		
		if (attacker.isMonster())
		{
			return attacker.isFakePlayer();
		}
		
		// Anything considers monsters friendly except Players, Attackables (Guards, Friendly NPC), Traps and EffectPoints.
		if (!attacker.isPlayable() && !attacker.isAttackable() && !(attacker instanceof TrapInstance) && !(attacker instanceof EffectPointInstance))
		{
			return false;
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Return True if the MonsterInstance is Aggressive (aggroRange > 0).
	 */
	@Override
	public boolean isAggressive()
	{
		return getTemplate().isAggressive() && !isAffected(EffectFlag.PASSIVE);
	}
	
	@Override
	public void onSpawn()
	{
		if (!isTeleporting() && (_master != null))
		{
			setRandomWalking(false);
			setIsRaidMinion(_master.isRaid());
			_master.getMinionList().onMinionSpawn(this);
		}
		
		// dynamic script-based minions spawned here, after all preparations.
		super.onSpawn();
	}
	
	@Override
	public synchronized void onTeleported()
	{
		super.onTeleported();
		
		if (hasMinions())
		{
			getMinionList().onMasterTeleported();
		}
	}
	
	@Override
	public boolean deleteMe()
	{
		if (hasMinions())
		{
			getMinionList().onMasterDie(true);
		}
		
		if (_master != null)
		{
			_master.getMinionList().onMinionDie(this, 0);
		}
		
		return super.deleteMe();
	}
	
	@Override
	public MonsterInstance getLeader()
	{
		return _master;
	}
	
	public void setLeader(MonsterInstance leader)
	{
		_master = leader;
	}
	
	public void enableMinions(boolean value)
	{
		_enableMinions = value;
	}
	
	public boolean hasMinions()
	{
		return _minionList != null;
	}
	
	public MinionList getMinionList()
	{
		if (_minionList == null)
		{
			synchronized (this)
			{
				if (_minionList == null)
				{
					_minionList = new MinionList(this);
				}
			}
		}
		return _minionList;
	}
	
	@Override
	public boolean isMonster()
	{
		return true;
	}
	
	/**
	 * @return true if this MonsterInstance (or its master) is registered in WalkingManager
	 */
	@Override
	public boolean isWalker()
	{
		return ((_master == null) ? super.isWalker() : _master.isWalker());
	}
	
	/**
	 * @return {@code true} if this MonsterInstance is not raid minion, master state otherwise.
	 */
	@Override
	public boolean giveRaidCurse()
	{
		return (isRaidMinion() && (_master != null)) ? _master.giveRaidCurse() : super.giveRaidCurse();
	}
	
	@Override
	public void doCast(Skill skill, Creature target, WorldObject[] targets)
	{
		// Might need some exceptions here, but it will prevent the monster buffing player bug.
		if (!skill.isBad() && (getTarget() != null) && getTarget().isPlayer())
		{
			setCastingNow(false);
			setCastingSimultaneouslyNow(false);
			return;
		}
		super.doCast(skill, target, targets);
	}
}
