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
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CtrlEvent;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.instancemanager.DuelManager;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.tasks.cubics.CubicAction;
import org.l2jmobius.gameserver.model.actor.tasks.cubics.CubicDisappear;
import org.l2jmobius.gameserver.model.actor.tasks.cubics.CubicHeal;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.interfaces.IIdentifiable;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class CubicInstance implements IIdentifiable
{
	private static final Logger LOGGER = Logger.getLogger(CubicInstance.class.getName());
	
	// Type of Cubics
	public static final int STORM_CUBIC = 1;
	public static final int VAMPIRIC_CUBIC = 2;
	public static final int LIFE_CUBIC = 3;
	public static final int VIPER_CUBIC = 4;
	public static final int POLTERGEIST_CUBIC = 5;
	public static final int BINDING_CUBIC = 6;
	public static final int AQUA_CUBIC = 7;
	public static final int SPARK_CUBIC = 8;
	public static final int ATTRACT_CUBIC = 9;
	public static final int SMART_CUBIC_EVATEMPLAR = 10;
	public static final int SMART_CUBIC_SHILLIENTEMPLAR = 11;
	public static final int SMART_CUBIC_ARCANALORD = 12;
	public static final int SMART_CUBIC_ELEMENTALMASTER = 13;
	public static final int SMART_CUBIC_SPECTRALMASTER = 14;
	
	// Max range of cubic skills
	// TODO: Check/fix the max range
	public static final int MAX_MAGIC_RANGE = 900;
	
	// Cubic skills
	public static final int SKILL_CUBIC_HEAL = 4051;
	public static final int SKILL_CUBIC_CURE = 5579;
	
	private final PlayerInstance _owner;
	private Creature _target;
	
	private final int _cubicId;
	private final int _cubicPower;
	private final int _cubicDelay;
	private final int _cubicSkillChance;
	private final int _cubicMaxCount;
	private final boolean _givenByOther;
	
	private final List<Skill> _skills = new ArrayList<>();
	
	private Future<?> _disappearTask;
	private Future<?> _actionTask;
	
	public CubicInstance(PlayerInstance owner, int cubicId, int level, int cubicPower, int cubicDelay, int cubicSkillChance, int cubicMaxCount, int cubicDuration, boolean givenByOther)
	{
		_owner = owner;
		_cubicId = cubicId;
		_cubicPower = cubicPower;
		_cubicDelay = cubicDelay * 1000;
		_cubicSkillChance = cubicSkillChance;
		_cubicMaxCount = cubicMaxCount;
		_givenByOther = givenByOther;
		
		switch (_cubicId)
		{
			case STORM_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(4049, level));
				break;
			}
			case VAMPIRIC_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(4050, level));
				break;
			}
			case LIFE_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(4051, level));
				doAction();
				break;
			}
			case VIPER_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(4052, level));
				break;
			}
			case POLTERGEIST_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(4053, level));
				_skills.add(SkillData.getInstance().getSkill(4054, level));
				_skills.add(SkillData.getInstance().getSkill(4055, level));
				break;
			}
			case BINDING_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(4164, level));
				break;
			}
			case AQUA_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(4165, level));
				break;
			}
			case SPARK_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(4166, level));
				break;
			}
			case ATTRACT_CUBIC:
			{
				_skills.add(SkillData.getInstance().getSkill(5115, level));
				_skills.add(SkillData.getInstance().getSkill(5116, level));
				break;
			}
			case SMART_CUBIC_ARCANALORD:
			{
				_skills.add(SkillData.getInstance().getSkill(4051, 7));
				_skills.add(SkillData.getInstance().getSkill(4165, 9));
				break;
			}
			case SMART_CUBIC_ELEMENTALMASTER:
			{
				_skills.add(SkillData.getInstance().getSkill(4049, 8));
				_skills.add(SkillData.getInstance().getSkill(4166, 9));
				break;
			}
			case SMART_CUBIC_SPECTRALMASTER:
			{
				_skills.add(SkillData.getInstance().getSkill(4049, 8));
				_skills.add(SkillData.getInstance().getSkill(4052, 6));
				break;
			}
			case SMART_CUBIC_EVATEMPLAR:
			{
				_skills.add(SkillData.getInstance().getSkill(4053, 8));
				_skills.add(SkillData.getInstance().getSkill(4165, 9));
				break;
			}
			case SMART_CUBIC_SHILLIENTEMPLAR:
			{
				_skills.add(SkillData.getInstance().getSkill(4049, 8));
				_skills.add(SkillData.getInstance().getSkill(5115, 4));
				break;
			}
		}
		_disappearTask = ThreadPool.schedule(new CubicDisappear(this), cubicDuration * 1000); // disappear
	}
	
	public void doAction()
	{
		if (_actionTask == null)
		{
			synchronized (this)
			{
				if (_actionTask == null)
				{
					switch (_cubicId)
					{
						case AQUA_CUBIC:
						case BINDING_CUBIC:
						case SPARK_CUBIC:
						case STORM_CUBIC:
						case POLTERGEIST_CUBIC:
						case VAMPIRIC_CUBIC:
						case VIPER_CUBIC:
						case ATTRACT_CUBIC:
						case SMART_CUBIC_ARCANALORD:
						case SMART_CUBIC_ELEMENTALMASTER:
						case SMART_CUBIC_SPECTRALMASTER:
						case SMART_CUBIC_EVATEMPLAR:
						case SMART_CUBIC_SHILLIENTEMPLAR:
						{
							_actionTask = ThreadPool.scheduleAtFixedRate(new CubicAction(this, _cubicSkillChance), 0, _cubicDelay);
							break;
						}
						case LIFE_CUBIC:
						{
							_actionTask = ThreadPool.scheduleAtFixedRate(new CubicHeal(this), 0, _cubicDelay);
							break;
						}
					}
				}
			}
		}
	}
	
	@Override
	public int getId()
	{
		return _cubicId;
	}
	
	public PlayerInstance getOwner()
	{
		return _owner;
	}
	
	public int getCubicPower()
	{
		return _cubicPower;
	}
	
	public Creature getTarget()
	{
		return _target;
	}
	
	public void setTarget(Creature target)
	{
		_target = target;
	}
	
	public List<Skill> getSkills()
	{
		return _skills;
	}
	
	public int getCubicMaxCount()
	{
		return _cubicMaxCount;
	}
	
	public void stopAction()
	{
		_target = null;
		if (_actionTask != null)
		{
			_actionTask.cancel(true);
			_actionTask = null;
		}
	}
	
	public void cancelDisappear()
	{
		if (_disappearTask != null)
		{
			_disappearTask.cancel(true);
			_disappearTask = null;
		}
	}
	
	/** this sets the enemy target for a cubic */
	public void getCubicTarget()
	{
		try
		{
			_target = null;
			final WorldObject ownerTarget = _owner.getTarget();
			if (ownerTarget == null)
			{
				return;
			}
			// Custom event targeting
			if (_owner.isOnEvent())
			{
				if (ownerTarget.getActingPlayer() != null)
				{
					final PlayerInstance target = ownerTarget.getActingPlayer();
					if (((_owner.getTeam() != target.getTeam()) || _owner.isOnSoloEvent()) && !(target.isDead()))
					{
						_target = (Creature) ownerTarget;
					}
				}
				return;
			}
			// Duel targeting
			if (_owner.isInDuel())
			{
				final PlayerInstance playerA = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerA();
				final PlayerInstance playerB = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerB();
				if (DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
				{
					final Party partyA = playerA.getParty();
					final Party partyB = playerB.getParty();
					Party partyEnemy = null;
					if (partyA != null)
					{
						if (partyA.getMembers().contains(_owner))
						{
							if (partyB != null)
							{
								partyEnemy = partyB;
							}
							else
							{
								_target = playerB;
							}
						}
						else
						{
							partyEnemy = partyA;
						}
					}
					else
					{
						if (playerA == _owner)
						{
							if (partyB != null)
							{
								partyEnemy = partyB;
							}
							else
							{
								_target = playerB;
							}
						}
						else
						{
							_target = playerA;
						}
					}
					if (((_target == playerA) || (_target == playerB)) && (_target == ownerTarget))
					{
						return;
					}
					if (partyEnemy != null)
					{
						if (partyEnemy.getMembers().contains(ownerTarget))
						{
							_target = (Creature) ownerTarget;
						}
						return;
					}
				}
				if ((playerA != _owner) && (ownerTarget == playerA))
				{
					_target = playerA;
					return;
				}
				if ((playerB != _owner) && (ownerTarget == playerB))
				{
					_target = playerB;
					return;
				}
				_target = null;
				return;
			}
			// Olympiad targeting
			if (_owner.isInOlympiadMode())
			{
				if (_owner.isOlympiadStart() && ownerTarget.isPlayable())
				{
					final PlayerInstance targetPlayer = ownerTarget.getActingPlayer();
					if ((targetPlayer != null) && (targetPlayer.getOlympiadGameId() == _owner.getOlympiadGameId()) && (targetPlayer.getOlympiadSide() != _owner.getOlympiadSide()))
					{
						_target = (Creature) ownerTarget;
					}
				}
				return;
			}
			// test owners target if it is valid then use it
			if (ownerTarget.isCreature() && (ownerTarget != _owner.getSummon()) && (ownerTarget != _owner))
			{
				// target mob which has aggro on you or your summon
				if (ownerTarget.isAttackable())
				{
					final Attackable attackable = (Attackable) ownerTarget;
					if (attackable.isInAggroList(_owner) && !attackable.isDead())
					{
						_target = (Creature) ownerTarget;
						return;
					}
					if (_owner.hasSummon() && attackable.isInAggroList(_owner.getSummon()) && !attackable.isDead())
					{
						_target = (Creature) ownerTarget;
						return;
					}
				}
				
				// get target in pvp or in siege
				PlayerInstance enemy = null;
				if (((_owner.getPvpFlag() > 0) && !_owner.isInsideZone(ZoneId.PEACE)) || _owner.isInsideZone(ZoneId.PVP))
				{
					if (!((Creature) ownerTarget).isDead())
					{
						enemy = ownerTarget.getActingPlayer();
					}
					
					if (enemy != null)
					{
						boolean targetIt = true;
						if (_owner.getParty() != null)
						{
							if (_owner.getParty().getMembers().contains(enemy))
							{
								targetIt = false;
							}
							else if ((_owner.getParty().getCommandChannel() != null) && _owner.getParty().getCommandChannel().getMembers().contains(enemy))
							{
								targetIt = false;
							}
						}
						if ((_owner.getClan() != null) && !_owner.isInsideZone(ZoneId.PVP))
						{
							if (_owner.getClan().isMember(enemy.getObjectId()))
							{
								targetIt = false;
							}
							if ((_owner.getAllyId() > 0) && (enemy.getAllyId() > 0) && (_owner.getAllyId() == enemy.getAllyId()))
							{
								targetIt = false;
							}
						}
						if ((enemy.getPvpFlag() == 0) && !enemy.isInsideZone(ZoneId.PVP))
						{
							targetIt = false;
						}
						if (enemy.isInsideZone(ZoneId.PEACE))
						{
							targetIt = false;
						}
						if ((_owner.getSiegeState() > 0) && (_owner.getSiegeState() == enemy.getSiegeState()))
						{
							targetIt = false;
						}
						if (!enemy.isSpawned())
						{
							targetIt = false;
						}
						
						if (targetIt)
						{
							_target = enemy;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
	}
	
	public void useCubicContinuous(Skill skill, WorldObject[] targets)
	{
		for (Creature target : (Creature[]) targets)
		{
			if ((target == null) || target.isDead())
			{
				continue;
			}
			
			if (skill.isBad())
			{
				final byte shld = Formulas.calcShldUse(_owner, target, skill);
				final boolean acted = Formulas.calcCubicSkillSuccess(this, target, skill, shld);
				if (!acted)
				{
					_owner.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
					continue;
				}
			}
			
			// Apply effects
			skill.applyEffects(_owner, target, false, false, true, 0);
			
			// If this is a bad skill notify the duel manager, so it can be removed after the duel (player & target must be in the same duel).
			if (target.isPlayer() && target.getActingPlayer().isInDuel() && skill.isBad() && (_owner.getDuelId() == target.getActingPlayer().getDuelId()))
			{
				DuelManager.getInstance().onBuff(target.getActingPlayer(), skill);
			}
		}
	}
	
	/**
	 * @param skill
	 * @param targets
	 */
	public void useCubicMdam(Skill skill, WorldObject[] targets)
	{
		for (Creature target : (Creature[]) targets)
		{
			if (target == null)
			{
				continue;
			}
			
			if (target.isAlikeDead())
			{
				if (target.isPlayer() && Config.FAKE_DEATH_DAMAGE_STAND)
				{
					target.stopFakeDeath(true);
				}
				else
				{
					continue;
				}
			}
			
			final boolean mcrit = Formulas.calcMCrit(_owner.getMCriticalHit(target, skill));
			final byte shld = Formulas.calcShldUse(_owner, target, skill);
			int damage = (int) Formulas.calcMagicDam(this, target, skill, mcrit, shld);
			if (damage > 0)
			{
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}
				
				// Shield Deflect Magic: If target is reflecting the skill then no damage is done.
				if (target.getStat().calcStat(Stat.VENGEANCE_SKILL_MAGIC_DAMAGE, 0, target, skill) > Rnd.get(100))
				{
					damage = 0;
				}
				else
				{
					_owner.sendDamageMessage(target, damage, mcrit, false, false);
					target.reduceCurrentHp(damage, _owner, skill);
				}
			}
		}
	}
	
	public void useCubicDrain(Skill skill, WorldObject[] targets)
	{
		for (Creature target : (Creature[]) targets)
		{
			if (target.isAlikeDead())
			{
				continue;
			}
			
			final boolean mcrit = Formulas.calcMCrit(_owner.getMCriticalHit(target, skill));
			final byte shld = Formulas.calcShldUse(_owner, target, skill);
			final int damage = (int) Formulas.calcMagicDam(this, target, skill, mcrit, shld);
			
			// TODO: Unhardcode fixed value
			final double hpAdd = (0.4 * damage);
			final PlayerInstance owner = _owner;
			final double hp = ((owner.getCurrentHp() + hpAdd) > owner.getMaxHp() ? owner.getMaxHp() : (owner.getCurrentHp() + hpAdd));
			owner.setCurrentHp(hp);
			
			// Check to see if we should damage the target
			if ((damage > 0) && !target.isDead())
			{
				target.reduceCurrentHp(damage, _owner, skill);
				
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}
				owner.sendDamageMessage(target, damage, mcrit, false, false);
			}
		}
	}
	
	public void useCubicDisabler(Skill skill, WorldObject[] targets)
	{
		for (Creature target : (Creature[]) targets)
		{
			if ((target == null) || target.isDead())
			{
				continue;
			}
			
			final byte shld = Formulas.calcShldUse(_owner, target, skill);
			if (skill.hasEffectType(EffectType.STUN, EffectType.PARALYZE, EffectType.ROOT) && Formulas.calcCubicSkillSuccess(this, target, skill, shld))
			{
				// Apply effects
				skill.applyEffects(_owner, target, false, false, true, 0);
				
				// If this is a bad skill notify the duel manager, so it can be removed after the duel (player & target must be in the same duel).
				if (target.isPlayer() && target.getActingPlayer().isInDuel() && skill.isBad() && (_owner.getDuelId() == target.getActingPlayer().getDuelId()))
				{
					DuelManager.getInstance().onBuff(target.getActingPlayer(), skill);
				}
			}
			
			if (skill.hasEffectType(EffectType.AGGRESSION) && Formulas.calcCubicSkillSuccess(this, target, skill, shld))
			{
				if (target.isAttackable())
				{
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _owner, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
				}
				
				// Apply effects
				skill.applyEffects(_owner, target, false, false, true, 0);
			}
		}
	}
	
	/**
	 * @param owner
	 * @param target
	 * @return true if the target is inside of the owner's max Cubic range
	 */
	public static boolean isInCubicRange(Creature owner, Creature target)
	{
		if ((owner == null) || (target == null))
		{
			return false;
		}
		
		int x;
		int y;
		int z;
		// temporary range check until real behavior of cubics is known/coded
		final int range = MAX_MAGIC_RANGE;
		x = (owner.getX() - target.getX());
		y = (owner.getY() - target.getY());
		z = (owner.getZ() - target.getZ());
		return (((x * x) + (y * y) + (z * z)) <= (range * range));
	}
	
	/** this sets the friendly target for a cubic */
	public void cubicTargetForHeal()
	{
		Creature target = null;
		double percentleft = 100.0;
		Party party = _owner.getParty();
		
		// if owner is in a duel but not in a party duel, then it is the same as he does not have a party
		if (_owner.isInDuel() && !DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
		{
			party = null;
		}
		
		if ((party != null) && !_owner.isInOlympiadMode())
		{
			// Get all visible objects in a spheric area near the Creature
			// Get a list of Party Members
			for (Creature partyMember : party.getMembers())
			{
				// if party member not dead, check if he is in cast range of heal cubic and member is in cubic casting range, check if he need heal and if he have the lowest HP
				if (!partyMember.isDead() && isInCubicRange(_owner, partyMember) && (partyMember.getCurrentHp() < partyMember.getMaxHp()) && (percentleft > (partyMember.getCurrentHp() / partyMember.getMaxHp())))
				{
					percentleft = (partyMember.getCurrentHp() / partyMember.getMaxHp());
					target = partyMember;
				}
				if (partyMember.getSummon() != null)
				{
					if (partyMember.getSummon().isDead())
					{
						continue;
					}
					
					// If party member's pet not dead, check if it is in cast range of heal cubic.
					if (!isInCubicRange(_owner, partyMember.getSummon()))
					{
						continue;
					}
					
					// member's pet is in cubic casting range, check if he need heal and if he have the lowest HP
					if ((partyMember.getSummon().getCurrentHp() < partyMember.getSummon().getMaxHp()) && (percentleft > (partyMember.getSummon().getCurrentHp() / partyMember.getSummon().getMaxHp())))
					{
						percentleft = (partyMember.getSummon().getCurrentHp() / partyMember.getSummon().getMaxHp());
						target = partyMember.getSummon();
					}
				}
			}
		}
		else
		{
			if (_owner.getCurrentHp() < _owner.getMaxHp())
			{
				percentleft = (_owner.getCurrentHp() / _owner.getMaxHp());
				target = _owner;
			}
			if (_owner.hasSummon() && !_owner.getSummon().isDead() && (_owner.getSummon().getCurrentHp() < _owner.getSummon().getMaxHp()) && (percentleft > (_owner.getSummon().getCurrentHp() / _owner.getSummon().getMaxHp())) && isInCubicRange(_owner, _owner.getSummon()))
			{
				target = _owner.getSummon();
			}
		}
		
		_target = target;
	}
	
	public boolean givenByOther()
	{
		return _givenByOther;
	}
}
