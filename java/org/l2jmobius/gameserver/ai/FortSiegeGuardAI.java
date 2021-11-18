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
package org.l2jmobius.gameserver.ai;

import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.Collection;
import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.DefenderInstance;
import org.l2jmobius.gameserver.model.actor.instance.FortCommanderInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Util;

/**
 * This class manages AI of Attackable.
 */
public class FortSiegeGuardAI extends CreatureAI implements Runnable
{
	private static final int MAX_ATTACK_TIMEOUT = 300; // int ticks, i.e. 30 seconds
	
	/** The Attackable AI task executed every 1s (call onEvtThink method) */
	private Future<?> _aiTask;
	
	/** For attack AI, analysis of mob and its targets */
	private final SelfAnalysis _selfAnalysis = new SelfAnalysis();
	
	/** The delay after which the attacked is stopped */
	private int _attackTimeout;
	
	/** The Attackable aggro counter */
	private int _globalAggro;
	
	/** The flag used to indicate that a thinking action is in progress */
	private boolean _thinking; // to prevent recursive thinking
	
	private final int _attackRange;
	
	/**
	 * Constructor of AttackableAI.
	 * @param creature the creature
	 */
	public FortSiegeGuardAI(DefenderInstance creature)
	{
		super(creature);
		_selfAnalysis.init();
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
		_attackRange = _actor.getPhysicalAttackRange();
	}
	
	@Override
	public void run()
	{
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	/**
	 * <b><u>Actor is a GuardInstance</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The PlayerInstance target has karma (=PK)</li>
	 * <li>The MonsterInstance target is aggressive</li>
	 * </ul>
	 * <br>
	 * <b><u>Actor is a SiegeGuardInstance</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>A siege is in progress</li>
	 * <li>The PlayerInstance target isn't a Defender</li>
	 * </ul>
	 * <br>
	 * <b><u>Actor is a FriendlyMobInstance</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another NpcInstance</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The PlayerInstance target has karma (=PK)</li>
	 * </ul>
	 * <br>
	 * <b><u>Actor is a MonsterInstance</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another NpcInstance</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The actor is Aggressive</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return True if the target is autoattackable (depends on the actor type).
	 */
	private boolean autoAttackCondition(Creature target)
	{
		if (target == null)
		{
			return false;
		}
		
		// Check if the target isn't another guard, folk or a door
		if ((target instanceof DefenderInstance) || target.isNpc() || target.isDoor() || target.isAlikeDead() || (target instanceof FortCommanderInstance) || target.isPlayable())
		{
			PlayerInstance player = null;
			if (target.isPlayer())
			{
				player = (PlayerInstance) target;
			}
			else if (target.isSummon())
			{
				player = ((Summon) target).getOwner();
			}
			if ((player == null) || ((player.getClan() != null) && (player.getClan().getFortId() == ((Npc) _actor).getFort().getResidenceId())))
			{
				return false;
			}
		}
		if (target.isInvul() && ((target.isPlayer() && target.isGM()) || (target.isSummon() && ((Summon) target).getOwner().isGM())))
		{
			return false;
		}
		
		// Get the owner if the target is a summon
		Creature currentTarget = target;
		if (currentTarget.isSummon())
		{
			final PlayerInstance owner = ((Summon) currentTarget).getOwner();
			if (_actor.isInsideRadius3D(owner, 1000))
			{
				currentTarget = owner;
			}
		}
		
		return (!currentTarget.isPlayable() || !((Playable) currentTarget).isSilentMovingAffected() || _actor.isInsideRadius2D(currentTarget, 250)) && _actor.isAutoAttackable(currentTarget) && GeoEngine.getInstance().canSeeTarget(_actor, currentTarget);
	}
	
	/**
	 * Set the Intention of this CreatureAI and create an AI Task executed every 1s (call onEvtThink method) for this Attackable.<br>
	 * <font color=#FF0000><b><u>Caution</u>: If actor _knowPlayer isn't EMPTY, AI_INTENTION_IDLE will be change in AI_INTENTION_ACTIVE</b></font>
	 * @param newIntention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	synchronized void changeIntention(CtrlIntention newIntention, Object arg0, Object arg1)
	{
		CtrlIntention intention = newIntention;
		if (intention == AI_INTENTION_IDLE /* || intention == AI_INTENTION_ACTIVE */) // active becomes idle if only a summon is present
		{
			// Check if actor is not dead
			if (!_actor.isAlikeDead())
			{
				final Attackable npc = (Attackable) _actor;
				
				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (!World.getInstance().getVisibleObjects(npc, PlayerInstance.class).isEmpty())
				{
					intention = AI_INTENTION_ACTIVE;
				}
				else
				{
					intention = AI_INTENTION_IDLE;
				}
			}
			
			if (intention == AI_INTENTION_IDLE)
			{
				// Set the Intention of this AttackableAI to AI_INTENTION_IDLE
				super.changeIntention(AI_INTENTION_IDLE, null, null);
				
				// Stop AI task and detach AI from NPC
				if (_aiTask != null)
				{
					_aiTask.cancel(true);
					_aiTask = null;
				}
				
				// Cancel the AI
				_actor.detachAI();
				
				return;
			}
		}
		
		// Set the Intention of this AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);
		
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		if (_aiTask == null)
		{
			_aiTask = ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
		}
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Event.
	 * @param target The Creature to attack
	 */
	@Override
	protected void onIntentionAttack(Creature target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
		// if (_actor.getTarget() != null)
		super.onIntentionAttack(target);
	}
	
	/**
	 * Manage AI standard thinks of a Attackable (called by onEvtThink).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable Creature in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor can't attack, order to it to return to its home location</li>
	 * </ul>
	 */
	private void thinkActive()
	{
		final Attackable npc = (Attackable) _actor;
		
		// Update every 1s the _globalAggro counter to come close to 0
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		
		// Add all autoAttackable Creature in Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			World.getInstance().forEachVisibleObjectInRange(npc, Creature.class, _attackRange, target ->
			{
				if (autoAttackCondition(target) && (npc.getHating(target) == 0)) // check aggression
				{
					npc.addDamageHate(target, 0, 1);
				}
			});
			
			// Chose a target from its aggroList
			final Creature hated = _actor.isConfused() ? getAttackTarget() : npc.getMostHated();
			
			// Order to the Attackable to attack the target
			if (hated != null)
			{
				// Get the hate level of the Attackable against this Creature target contained in _aggroList
				final int aggro = npc.getHating(hated);
				if ((aggro + _globalAggro) > 0)
				{
					// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance
					if (!_actor.isRunning())
					{
						_actor.setRunning();
					}
					
					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(AI_INTENTION_ATTACK, hated, null);
				}
				
				return;
			}
		}
		// Order to the SiegeGuardInstance to return to its home location because there's no target to attack
		if (_actor.getWalkSpeed() >= 0)
		{
			(_actor instanceof DefenderInstance ? (DefenderInstance) _actor : (FortCommanderInstance) _actor).returnHome();
		}
	}
	
	/**
	 * Manage AI attack thinks of a Attackable (called by onEvtThink).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Call all WorldObject of its Faction inside the Faction Range</li>
	 * <li>Chose a target and order to attack it with magic skill or physical attack</li>
	 * </ul>
	 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
	 */
	private void thinkAttack()
	{
		if ((_attackTimeout < GameTimeTaskManager.getInstance().getGameTicks()) && _actor.isRunning())
		{
			_actor.setWalking();
			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		}
		
		final Creature attackTarget = getAttackTarget();
		// Check if target is dead or if timeout is expired to stop this attack
		if ((attackTarget == null) || attackTarget.isAlikeDead() || (_attackTimeout < GameTimeTaskManager.getInstance().getGameTicks()))
		{
			// Stop hating this target after the attack timeout or if target is dead
			if (attackTarget != null)
			{
				((Attackable) _actor).stopHating(attackTarget);
			}
			
			// Cancel target and timeout
			_attackTimeout = Integer.MAX_VALUE;
			setAttackTarget(null);
			
			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE, null, null);
			_actor.setWalking();
			return;
		}
		
		factionNotifyAndSupport();
		attackPrepare();
	}
	
	private final void factionNotifyAndSupport()
	{
		final Creature target = getAttackTarget();
		// Call all WorldObject of its Faction inside the Faction Range
		if ((((Npc) _actor).getTemplate().getClans() == null) || (target == null) || target.isInvul())
		{
			return;
		}
		
		// Go through all Creature that belong to its faction
		// for (Creature creature : _actor.getKnownList().getKnownCharactersInRadius(((NpcInstance) _actor).getFactionRange()+_actor.getTemplate().collisionRadius))
		for (Creature creature : World.getInstance().getVisibleObjectsInRange(_actor, Creature.class, 1000))
		{
			if (!(creature instanceof Npc))
			{
				if (_selfAnalysis.hasHealOrResurrect && creature.isPlayer() && ((Npc) _actor).getFort().getSiege().checkIsDefender(((PlayerInstance) creature).getClan())//
					&& !_actor.isAttackDisabled() && (creature.getCurrentHp() < (creature.getMaxHp() * 0.6)) && (_actor.getCurrentHp() > (_actor.getMaxHp() / 2)) && (_actor.getCurrentMp() > (_actor.getMaxMp() / 2)) && creature.isInCombat())
				{
					for (Skill sk : _selfAnalysis.healSkills)
					{
						if ((_actor.getCurrentMp() < sk.getMpConsume()) || _actor.isSkillDisabled(sk) || !Util.checkIfInRange(sk.getCastRange(), _actor, creature, true))
						{
							continue;
						}
						final int chance = 5;
						if (chance >= Rnd.get(100))
						{
							continue;
						}
						if (!GeoEngine.getInstance().canSeeTarget(_actor, creature))
						{
							break;
						}
						final WorldObject oldTarget = _actor.getTarget();
						_actor.setTarget(creature);
						clientStopMoving(null);
						_actor.doCast(sk);
						_actor.setTarget(oldTarget);
						return;
					}
				}
				continue;
			}
			
			final Npc npc = (Npc) creature;
			if (!npc.isInMyClan((Npc) _actor))
			{
				continue;
			}
			
			if (npc.getAI() != null) // TODO: possibly check not needed
			{
				if (!npc.isDead() && (Math.abs(target.getZ() - npc.getZ()) < 600) && ((npc.getAI()._intention == AI_INTENTION_IDLE) || (npc.getAI()._intention == AI_INTENTION_ACTIVE)) && target.isInsideRadius3D(npc, 1500) && GeoEngine.getInstance().canSeeTarget(npc, target))
				{
					// Notify the WorldObject AI with EVT_AGGRESSION
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1);
					return;
				}
				// heal friends
				if (_selfAnalysis.hasHealOrResurrect && !_actor.isAttackDisabled() && (npc.getCurrentHp() < (npc.getMaxHp() * 0.6)) && (_actor.getCurrentHp() > (_actor.getMaxHp() / 2)) && (_actor.getCurrentMp() > (_actor.getMaxMp() / 2)) && npc.isInCombat())
				{
					for (Skill sk : _selfAnalysis.healSkills)
					{
						if ((_actor.getCurrentMp() < sk.getMpConsume()) || _actor.isSkillDisabled(sk) || !Util.checkIfInRange(sk.getCastRange(), _actor, npc, true))
						{
							continue;
						}
						
						final int chance = 4;
						if (chance >= Rnd.get(100))
						{
							continue;
						}
						if (!GeoEngine.getInstance().canSeeTarget(_actor, npc))
						{
							break;
						}
						
						final WorldObject oldTarget = _actor.getTarget();
						_actor.setTarget(npc);
						clientStopMoving(null);
						_actor.doCast(sk);
						_actor.setTarget(oldTarget);
						return;
					}
				}
			}
		}
	}
	
	private void attackPrepare()
	{
		// Get all information needed to choose between physical or magical attack
		Collection<Skill> skills = null;
		double dist2 = 0;
		int range = 0;
		Creature attackTarget = getAttackTarget();
		try
		{
			_actor.setTarget(attackTarget);
			skills = _actor.getAllSkills();
			dist2 = _actor.calculateDistanceSq2D(attackTarget);
			range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
			if (attackTarget.isMoving())
			{
				range += 50;
			}
		}
		catch (NullPointerException e)
		{
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}
		
		// never attack defenders
		final DefenderInstance sGuard = _actor instanceof FortCommanderInstance ? (FortCommanderInstance) _actor : (DefenderInstance) _actor;
		if (attackTarget.isPlayer() && sGuard.getFort().getSiege().checkIsDefender(((PlayerInstance) attackTarget).getClan()))
		{
			// Cancel the target
			sGuard.stopHating(attackTarget);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}
		
		// Check if the actor isn't muted and if it is far from target
		if (!_actor.isMuted() && (dist2 > (range * range)))
		{
			// check for long ranged skills and heal/buff skills
			for (Skill sk : skills)
			{
				final int castRange = sk.getCastRange();
				if ((dist2 <= (castRange * castRange)) && (castRange > 70) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)) && !sk.isPassive())
				{
					final WorldObject oldTarget = _actor.getTarget();
					if ((sk.isContinuous() && !sk.isDebuff()) || sk.hasEffectType(EffectType.HEAL))
					{
						if (sk.hasEffectType(EffectType.HEAL) && (_actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)))
						{
							break;
						}
						
						boolean useSkillSelf = true;
						if (sk.isContinuous() && !sk.isDebuff() && _actor.isAffectedBySkill(sk.getId()))
						{
							useSkillSelf = false;
						}
						
						if (useSkillSelf)
						{
							_actor.setTarget(_actor);
						}
					}
					
					clientStopMoving(null);
					_actor.doCast(sk);
					_actor.setTarget(oldTarget);
					return;
				}
			}
			
			// Check if the SiegeGuardInstance is attacking, knows the target and can't run
			if (!(_actor.isAttackingNow()) && (_actor.getRunSpeed() == 0) && (_actor.isInSurroundingRegion(attackTarget)))
			{
				// Cancel the target
				_actor.setTarget(null);
				setIntention(AI_INTENTION_IDLE, null, null);
			}
			else
			{
				final double dx = _actor.getX() - attackTarget.getX();
				final double dy = _actor.getY() - attackTarget.getY();
				final double dz = _actor.getZ() - attackTarget.getZ();
				final double homeX = attackTarget.getX() - sGuard.getSpawn().getX();
				final double homeY = attackTarget.getY() - sGuard.getSpawn().getY();
				
				// Check if the SiegeGuardInstance isn't too far from it's home location
				if ((((dx * dx) + (dy * dy)) > 10000) && (((homeX * homeX) + (homeY * homeY)) > 3240000) // 1800 * 1800
					&& (_actor.isInSurroundingRegion(attackTarget)))
				{
					// Cancel the target
					_actor.setTarget(null);
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				// Temporary hack for preventing guards jumping off towers,
				// before replacing this with effective geodata checks and AI modification
				else if ((dz * dz) < (170 * 170))
				{
					if (_selfAnalysis.isMage)
					{
						range = _selfAnalysis.maxCastRange - 50;
					}
					if (_actor.getWalkSpeed() <= 0)
					{
						return;
					}
					moveToPawn(attackTarget, attackTarget.isMoving() ? range - 70 : range);
				}
			}
		}
		else
		{
			if (_actor.isMuted() && (dist2 > (range * range)))
			{
				// Temporary hack for preventing guards jumping off towers,
				// before replacing this with effective geodata checks and AI modification
				final double dz = _actor.getZ() - attackTarget.getZ();
				if ((dz * dz) < (170 * 170)) // normally 130 if guard z coordinates correct
				{
					if (_selfAnalysis.isMage)
					{
						range = _selfAnalysis.maxCastRange - 50;
					}
					if (_actor.getWalkSpeed() <= 0)
					{
						return;
					}
					moveToPawn(attackTarget, attackTarget.isMoving() ? range - 70 : range);
				}
				return;
			}
			if (dist2 <= (range * range))
			{
				final Creature hated = _actor.isConfused() ? attackTarget : ((Attackable) _actor).getMostHated();
				if (hated == null)
				{
					setIntention(AI_INTENTION_ACTIVE, null, null);
					return;
				}
				if (hated != attackTarget)
				{
					attackTarget = hated;
				}
				
				_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
				
				// check for close combat skills && heal/buff skills
				if (!_actor.isMuted() && (Rnd.get(100) <= 5))
				{
					for (Skill sk : skills)
					{
						final int castRange = sk.getCastRange();
						if (((castRange * castRange) >= dist2) && !sk.isPassive() && (_actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)) && !_actor.isSkillDisabled(sk))
						{
							final WorldObject oldTarget = _actor.getTarget();
							if ((sk.isContinuous() && !sk.isDebuff()) || sk.hasEffectType(EffectType.HEAL))
							{
								if (sk.hasEffectType(EffectType.HEAL) && (_actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)))
								{
									break;
								}
								
								boolean useSkillSelf = true;
								if (sk.isContinuous() && !sk.isDebuff() && _actor.isAffectedBySkill(sk.getId()))
								{
									useSkillSelf = false;
								}
								
								if (useSkillSelf)
								{
									_actor.setTarget(_actor);
								}
							}
							
							clientStopMoving(null);
							_actor.doCast(sk);
							_actor.setTarget(oldTarget);
							return;
						}
					}
				}
				// Finally, do the physical attack itself
				_actor.doAttack(attackTarget);
			}
		}
	}
	
	/**
	 * Manage AI thinking actions of a Attackable.
	 */
	@Override
	public void onEvtThink()
	{
		// if(getIntention() != AI_INTENTION_IDLE && (!_actor.isSpawned() || !_actor.hasAI() || !_actor.isKnownPlayers()))
		// setIntention(AI_INTENTION_IDLE);
		
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a Attackable
			if (getIntention() == AI_INTENTION_ACTIVE)
			{
				thinkActive();
			}
			else if (getIntention() == AI_INTENTION_ATTACK)
			{
				thinkAttack();
			}
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}
	
	/**
	 * Launch actions corresponding to the Event Attacked.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
	 * <li>Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance</li>
	 * <li>Set the Intention to AI_INTENTION_ATTACK</li>
	 * </ul>
	 * @param attacker The Creature that attacks the actor
	 */
	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor
		((Attackable) _actor).addDamageHate(attacker, 0, 1);
		
		// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance
		if (!_actor.isRunning())
		{
			_actor.setRunning();
		}
		
		// Set the Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK)
		{
			setIntention(AI_INTENTION_ATTACK, attacker, null);
		}
		
		super.onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event Aggression.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li>
	 * <li>Set the actor Intention to AI_INTENTION_ATTACK (if actor is GuardInstance check if it isn't too far from its home location)</li>
	 * </ul>
	 * @param target The Creature that attacks
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		if (_actor == null)
		{
			return;
		}
		
		final Attackable me = (Attackable) _actor;
		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already present
			me.addDamageHate(target, 0, aggro);
			
			// Get the hate of the actor against the target
			if (me.getHating(target) <= 0)
			{
				if (me.getMostHated() == null)
				{
					_globalAggro = -25;
					me.clearAggroList();
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				return;
			}
			
			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != AI_INTENTION_ATTACK)
			{
				// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance
				if (!_actor.isRunning())
				{
					_actor.setRunning();
				}
				
				final DefenderInstance sGuard = _actor instanceof FortCommanderInstance ? (FortCommanderInstance) _actor : (DefenderInstance) _actor;
				final double homeX = target.getX() - sGuard.getSpawn().getX();
				final double homeY = target.getY() - sGuard.getSpawn().getY();
				
				// Check if the SiegeGuardInstance is not too far from its home location
				if (((homeX * homeX) + (homeY * homeY)) < 3240000)
				{
					setIntention(AI_INTENTION_ATTACK, target, null);
				}
			}
		}
		else
		{
			// currently only for setting lower general aggro
			if (aggro >= 0)
			{
				return;
			}
			
			final Creature mostHated = me.getMostHated();
			if (mostHated == null)
			{
				_globalAggro = -25;
				return;
			}
			
			for (Creature aggroed : me.getAggroList().keySet())
			{
				me.addDamageHate(aggroed, 0, aggro);
			}
			
			if (me.getHating(mostHated) <= 0)
			{
				_globalAggro = -25;
				me.clearAggroList();
				setIntention(AI_INTENTION_IDLE, null, null);
			}
		}
	}
	
	@Override
	public void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		_actor.detachAI();
		super.stopAITask();
	}
}