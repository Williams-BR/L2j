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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.AISkillScope;
import org.l2jmobius.gameserver.enums.AIType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.instancemanager.DimensionalRiftManager;
import org.l2jmobius.gameserver.instancemanager.ItemsOnGroundManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.FestivalMonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.FriendlyMobInstance;
import org.l2jmobius.gameserver.model.actor.instance.GrandBossInstance;
import org.l2jmobius.gameserver.model.actor.instance.GuardInstance;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.RaidBossInstance;
import org.l2jmobius.gameserver.model.actor.instance.RiftInvaderInstance;
import org.l2jmobius.gameserver.model.actor.instance.StaticObjectInstance;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.attackable.OnAttackableFactionCall;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.attackable.OnAttackableHate;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.skills.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.taskmanager.AttackableThinkTaskManager;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Util;

/**
 * This class manages AI of Attackable.
 * @author Zoey76
 */
public class AttackableAI extends CreatureAI
{
	/**
	 * Fear task.
	 * @author Zoey76
	 */
	public static class FearTask implements Runnable
	{
		private final AttackableAI _ai;
		private final Creature _effector;
		private boolean _start;
		
		public FearTask(AttackableAI ai, Creature effector, boolean start)
		{
			_ai = ai;
			_effector = effector;
			_start = start;
		}
		
		@Override
		public void run()
		{
			if (_effector != null)
			{
				final int fearTimeLeft = _ai.getFearTime() - FEAR_TICKS;
				_ai.setFearTime(fearTimeLeft);
				_ai.onEvtAfraid(_effector, _start);
				_start = false;
			}
		}
	}
	
	protected static final int FEAR_TICKS = 5;
	private static final int RANDOM_WALK_RATE = 30; // confirmed
	private static final int MAX_ATTACK_TIMEOUT = 1200; // int ticks, i.e. 2min
	/** The delay after which the attacked is stopped. */
	private int _attackTimeout;
	/** The Attackable aggro counter. */
	private int _globalAggro;
	/** The flag used to indicate that a thinking action is in progress, to prevent recursive thinking. */
	private boolean _thinking;
	private int _chaosTime = 0;
	// Fear parameters
	private int _fearTime;
	private Future<?> _fearTask = null;
	
	/**
	 * Constructor of AttackableAI.
	 * @param creature the creature
	 */
	public AttackableAI(Attackable creature)
	{
		super(creature);
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
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
	 * <li>The target isn't a Folk, a Door or another Npc</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The PlayerInstance target has karma (=PK)</li>
	 * </ul>
	 * <br>
	 * <b><u>Actor is a MonsterInstance</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another Npc</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The actor is Aggressive</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return True if the target is autoattackable (depends on the actor type).
	 */
	private boolean autoAttackCondition(Creature target)
	{
		if ((target == null) || (getActiveChar() == null))
		{
			return false;
		}
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
		{
			// However EffectInvincible requires to check GMs specially
			if (target.isPlayer() && target.isGM())
			{
				return false;
			}
			
			if (target.isSummon() && ((Summon) target).getOwner().isGM())
			{
				return false;
			}
		}
		
		// Check if the target isn't a Folk or a Door
		if (target.isDoor())
		{
			return false;
		}
		
		// Check if the target isn't dead, is in the Aggro range and is at the same height
		final Attackable me = getActiveChar();
		if (target.isAlikeDead() || (target.isPlayable() && !me.isInsideRadius3D(target, me.getAggroRange())))
		{
			return false;
		}
		
		// Check if the target is a Playable and if the AI isn't a Raid Boss, can see Silent Moving players and the target isn't in silent move mode
		if (target.isPlayable() && !(me.isRaid()) && !(me.canSeeThroughSilentMove()) && ((Playable) target).isSilentMovingAffected())
		{
			return false;
		}
		
		// Gets the player if there is any.
		final PlayerInstance player = target.getActingPlayer();
		if (player != null)
		{
			// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
			if (player.isGM() && !player.getAccessLevel().canTakeAggro())
			{
				return false;
			}
			
			// check if the target is within the grace period for JUST getting up from fake death
			if (player.isRecentFakeDeath())
			{
				return false;
			}
			
			if (Config.FACTION_SYSTEM_ENABLED && Config.FACTION_GUARDS_ENABLED && ((player.isGood() && ((Npc) _actor).getTemplate().isClan(Config.FACTION_EVIL_TEAM_NAME)) || (player.isEvil() && ((Npc) _actor).getTemplate().isClan(Config.FACTION_GOOD_TEAM_NAME))))
			{
				return true;
			}
			
			if (player.isInParty() && player.getParty().isInDimensionalRift())
			{
				final byte riftType = player.getParty().getDimensionalRift().getType();
				final byte riftRoom = player.getParty().getDimensionalRift().getCurrentRoom();
				if ((me instanceof RiftInvaderInstance) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ()))
				{
					return false;
				}
			}
		}
		
		// Check if the actor is a GuardInstance
		if (me instanceof GuardInstance)
		{
			// Check if the PlayerInstance target has karma (=PK)
			if ((player != null) && (player.getKarma() > 0))
			{
				return GeoEngine.getInstance().canSeeTarget(me, player); // Los Check
			}
			// Check if the MonsterInstance target is aggressive
			if (target.isMonster() && Config.GUARD_ATTACK_AGGRO_MOB)
			{
				return (((MonsterInstance) target).isAggressive() && GeoEngine.getInstance().canSeeTarget(me, target));
			}
			
			return false;
		}
		else if (me instanceof FriendlyMobInstance)
		{
			// Check if the target isn't another Npc
			if (target instanceof Npc)
			{
				return false;
			}
			
			// Check if the PlayerInstance target has karma (=PK)
			if (target.isPlayer() && (((PlayerInstance) target).getKarma() > 0))
			{
				return GeoEngine.getInstance().canSeeTarget(me, target); // Los Check
			}
			return false;
		}
		else
		{
			if (target.isAttackable())
			{
				if (!target.isAutoAttackable(me))
				{
					return false;
				}
				
				if (me.isChaos() && me.isInsideRadius2D(target, me.getAggroRange()))
				{
					if (((Attackable) target).isInMyClan(me))
					{
						return false;
					}
					// Los Check
					return GeoEngine.getInstance().canSeeTarget(me, target);
				}
			}
			
			if (target.isAttackable() || (target instanceof Npc))
			{
				return false;
			}
			
			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if (!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE))
			{
				return false;
			}
			
			if (me.isChampion() && Config.CHAMPION_PASSIVE)
			{
				return false;
			}
			
			// Check if the actor is Aggressive
			return (me.isAggressive() && GeoEngine.getInstance().canSeeTarget(me, target));
		}
	}
	
	public void startAITask()
	{
		AttackableThinkTaskManager.getInstance().add(getActiveChar());
	}
	
	@Override
	public void stopAITask()
	{
		AttackableThinkTaskManager.getInstance().remove(getActiveChar());
		super.stopAITask();
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
		if ((intention == AI_INTENTION_IDLE) || (intention == AI_INTENTION_ACTIVE))
		{
			// Check if actor is not dead
			final Attackable npc = getActiveChar();
			if (!npc.isAlikeDead())
			{
				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (!World.getInstance().getVisibleObjects(npc, PlayerInstance.class).isEmpty())
				{
					intention = AI_INTENTION_ACTIVE;
				}
				else if ((npc.getSpawn() != null) && !npc.isInsideRadius3D(npc.getSpawn().getLocation(), Config.MAX_DRIFT_RANGE + Config.MAX_DRIFT_RANGE))
				{
					intention = AI_INTENTION_ACTIVE;
				}
			}
			
			if (intention == AI_INTENTION_IDLE)
			{
				// Set the Intention of this AttackableAI to AI_INTENTION_IDLE
				super.changeIntention(AI_INTENTION_IDLE, null, null);
				
				// Stop AI task and detach AI from NPC
				stopAITask();
				
				// Cancel the AI
				_actor.detachAI();
				return;
			}
		}
		
		// Set the Intention of this AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);
		
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		startAITask();
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
		super.onIntentionAttack(target);
	}
	
	@Override
	protected void onEvtAfraid(Creature effector, boolean start)
	{
		if ((_fearTime > 0) && (_fearTask == null))
		{
			_fearTask = ThreadPool.scheduleAtFixedRate(new FearTask(this, effector, start), 0, FEAR_TICKS * 1000); // seconds
			_actor.startAbnormalVisualEffect(true, AbnormalVisualEffect.TURN_FLEE);
		}
		else
		{
			super.onEvtAfraid(effector, start);
			
			if ((_actor.isDead() || (_fearTime <= 0)) && (_fearTask != null))
			{
				_fearTask.cancel(true);
				_fearTask = null;
				_actor.stopAbnormalVisualEffect(true, AbnormalVisualEffect.TURN_FLEE);
				setIntention(AI_INTENTION_IDLE);
			}
		}
	}
	
	protected void thinkCast()
	{
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		clientStopMoving(null);
		setIntention(AI_INTENTION_ACTIVE);
		_actor.doCast(_skill);
	}
	
	/**
	 * Manage AI standard thinks of a Attackable (called by onEvtThink). <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable Creature in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor is a GuardInstance that can't attack, order to it to return to its home location</li>
	 * <li>If the actor is a MonsterInstance that can't attack, order to it to random walk (1/100)</li>
	 * </ul>
	 */
	protected void thinkActive()
	{
		final Attackable npc = getActiveChar();
		
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
			World.getInstance().forEachVisibleObject(npc, Creature.class, target ->
			{
				if ((target instanceof StaticObjectInstance))
				{
					return;
				}
				
				if (npc.isFakePlayer() && npc.isAggressive())
				{
					final List<ItemInstance> droppedItems = npc.getFakePlayerDrops();
					if (droppedItems.isEmpty())
					{
						Creature nearestTarget = null;
						double closestDistance = Double.MAX_VALUE;
						for (Creature t : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, npc.getAggroRange()))
						{
							if ((t == _actor) || (t == null) || t.isDead())
							{
								continue;
							}
							if ((Config.FAKE_PLAYER_AGGRO_FPC && t.isFakePlayer()) //
								|| (Config.FAKE_PLAYER_AGGRO_MONSTERS && t.isMonster() && !t.isFakePlayer()) //
								|| (Config.FAKE_PLAYER_AGGRO_PLAYERS && t.isPlayer()))
							{
								final int hating = npc.getHating(t);
								final double distance = npc.calculateDistance2D(t);
								if ((hating == 0) && (closestDistance > distance))
								{
									nearestTarget = t;
									closestDistance = distance;
								}
							}
						}
						if (nearestTarget != null)
						{
							npc.addDamageHate(nearestTarget, 0, 1);
						}
					}
					else if (!npc.isInCombat()) // must pickup items
					{
						final int itemIndex = npc.getFakePlayerDrops().size() - 1; // last item dropped - can also use 0 for first item dropped
						final ItemInstance droppedItem = npc.getFakePlayerDrops().get(itemIndex);
						if ((droppedItem != null) && droppedItem.isSpawned())
						{
							if (npc.calculateDistance2D(droppedItem) > 50)
							{
								moveTo(droppedItem);
							}
							else
							{
								npc.getFakePlayerDrops().remove(itemIndex);
								droppedItem.pickupMe(npc);
								if (Config.SAVE_DROPPED_ITEM)
								{
									ItemsOnGroundManager.getInstance().removeObject(droppedItem);
								}
								if (droppedItem.getItem().hasExImmediateEffect())
								{
									for (SkillHolder skillHolder : droppedItem.getItem().getSkills())
									{
										npc.doSimultaneousCast(skillHolder.getSkill());
									}
									npc.broadcastInfo(); // ? check if this is necessary
								}
							}
						}
						else
						{
							npc.getFakePlayerDrops().remove(itemIndex);
						}
						npc.setRunning();
					}
					return;
				}
				
				/*
				 * Check to see if this is a festival mob spawn. If it is, then check to see if the aggro trigger is a festival participant...if so, move to attack it.
				 */
				if ((npc instanceof FestivalMonsterInstance) && target.isPlayer())
				{
					final PlayerInstance targetPlayer = (PlayerInstance) target;
					if (!(targetPlayer.isFestivalParticipant()))
					{
						return;
					}
				}
				
				// For each Creature check if the target is autoattackable
				if (autoAttackCondition(target)) // check aggression
				{
					if (target.isFakePlayer())
					{
						if (!npc.isFakePlayer() || (npc.isFakePlayer() && Config.FAKE_PLAYER_AGGRO_FPC))
						{
							final int hating = npc.getHating(target);
							if (hating == 0)
							{
								npc.addDamageHate(target, 0, 0);
							}
						}
						return;
					}
					if (target.isPlayable())
					{
						final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnAttackableHate(getActiveChar(), target.getActingPlayer(), target.isSummon()), getActiveChar(), TerminateReturn.class);
						if ((term != null) && term.terminate())
						{
							return;
						}
					}
					
					if (npc.getHating(target) == 0)
					{
						npc.addDamageHate(target, 0, 0);
					}
				}
			});
			
			// Chose a target from its aggroList
			final Creature hated = npc.isConfused() ? getAttackTarget() : npc.getMostHated();
			
			// Order to the Attackable to attack the target
			if ((hated != null) && !npc.isCoreAIDisabled())
			{
				// Get the hate level of the Attackable against this Creature target contained in _aggroList
				final int aggro = npc.getHating(hated);
				if ((aggro + _globalAggro) > 0)
				{
					// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance
					if (!npc.isRunning())
					{
						npc.setRunning();
					}
					
					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(AI_INTENTION_ATTACK, hated);
				}
				
				return;
			}
		}
		
		// Chance to forget attackers after some time
		if ((npc.getCurrentHp() == npc.getMaxHp()) && (npc.getCurrentMp() == npc.getMaxMp()) && !npc.getAttackByList().isEmpty() && (Rnd.get(500) == 0))
		{
			npc.clearAggroList();
			npc.getAttackByList().clear();
		}
		
		// Check if the mob should not return to spawn point
		if (!npc.canReturnToSpawnPoint())
		{
			return;
		}
		
		// Check if the actor is a GuardInstance
		if ((npc instanceof GuardInstance) && !npc.isWalker())
		{
			if (Config.ENABLE_GUARD_RETURN && (npc.getSpawn() != null) && (Util.calculateDistance(npc, npc.getSpawn(), false, false) > 50) && /* !npc.isInsideZone(ZoneId.SIEGE) && */!npc.isCastingNow())
			{
				// Custom guard teleport to spawn.
				npc.clearAggroList();
				npc.doCast(SkillData.getInstance().getSkill(1050, 1));
			}
			else
			{
				// Order to the GuardInstance to return to its home location because there's no target to attack
				npc.returnHome();
			}
		}
		
		// If this is a festival monster, then it remains in the same location.
		if (npc instanceof FestivalMonsterInstance)
		{
			return;
		}
		
		// Minions following leader
		final Creature leader = npc.getLeader();
		if ((leader != null) && !leader.isAlikeDead())
		{
			final int offset;
			final int minRadius = 30;
			if (npc.isRaidMinion())
			{
				offset = 500; // for Raids - need correction
			}
			else
			{
				offset = 200; // for normal minions - need correction :)
			}
			
			if (leader.isRunning())
			{
				npc.setRunning();
			}
			else
			{
				npc.setWalking();
			}
			
			if (npc.calculateDistanceSq2D(leader) > (offset * offset))
			{
				int x1 = Rnd.get(minRadius * 2, offset * 2); // x
				int y1 = Rnd.get(x1, offset * 2); // distance
				int z1;
				y1 = (int) Math.sqrt((y1 * y1) - (x1 * x1)); // y
				x1 = x1 > (offset + minRadius) ? (leader.getX() + x1) - offset : (leader.getX() - x1) + minRadius;
				y1 = y1 > (offset + minRadius) ? (leader.getY() + y1) - offset : (leader.getY() - y1) + minRadius;
				z1 = leader.getZ();
				// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
				moveTo(x1, y1, z1);
				return;
			}
			if (Rnd.get(RANDOM_WALK_RATE) == 0)
			{
				for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
				{
					if (cast(sk))
					{
						return;
					}
				}
			}
		}
		// Order to the MonsterInstance to random walk (1/100)
		else if ((npc.getSpawn() != null) && (Rnd.get(RANDOM_WALK_RATE) == 0) && npc.isRandomWalkingEnabled())
		{
			
			for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
			{
				if (cast(sk))
				{
					return;
				}
			}
			
			int x1 = npc.getSpawn().getX();
			int y1 = npc.getSpawn().getY();
			int z1 = npc.getSpawn().getZ();
			final int range = Config.MAX_DRIFT_RANGE;
			if (!npc.isInsideRadius2D(x1, y1, 0, range))
			{
				npc.setReturningToSpawnPoint(true);
			}
			else
			{
				final int deltaX = Rnd.get(range * 2); // x
				int deltaY = Rnd.get(deltaX, range * 2); // distance
				deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
				x1 = (deltaX + x1) - range;
				y1 = (deltaY + y1) - range;
				z1 = npc.getZ();
			}
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
			final Location moveLoc = _actor.isFlying() ? new Location(x1, y1, z1) : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceId());
			moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
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
	 */
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if ((npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		if (Config.AGGRO_DISTANCE_CHECK_ENABLED && npc.isMonster() && !npc.isWalker() && !(npc instanceof GrandBossInstance))
		{
			final Spawn spawn = npc.getSpawn();
			if ((spawn != null) && (npc.calculateDistance3D(spawn.getLocation()) > (npc.isRaid() ? Config.AGGRO_DISTANCE_CHECK_RAID_RANGE : Config.AGGRO_DISTANCE_CHECK_RANGE)))
			{
				if ((Config.AGGRO_DISTANCE_CHECK_RAIDS || !npc.isRaid()) && (Config.AGGRO_DISTANCE_CHECK_INSTANCES || (npc.getInstanceId() == 0)))
				{
					if (Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
					{
						npc.setCurrentHp(npc.getMaxHp());
						npc.setCurrentMp(npc.getMaxMp());
					}
					npc.abortAttack();
					npc.clearAggroList();
					npc.getAttackByList().clear();
					if (npc.hasAI())
					{
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, spawn.getLocation());
					}
					else
					{
						npc.teleToLocation(spawn.getLocation(), true);
					}
					
					// Minions should return as well.
					if (((MonsterInstance) _actor).hasMinions())
					{
						for (MonsterInstance minion : ((MonsterInstance) _actor).getMinionList().getSpawnedMinions())
						{
							if (Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
							{
								minion.setCurrentHp(minion.getMaxHp());
								minion.setCurrentMp(minion.getMaxMp());
							}
							minion.abortAttack();
							minion.clearAggroList();
							minion.getAttackByList().clear();
							if (minion.hasAI())
							{
								minion.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, spawn.getLocation());
							}
							else
							{
								minion.teleToLocation(spawn.getLocation(), true);
							}
						}
					}
					return;
				}
			}
		}
		
		if (npc.isCoreAIDisabled())
		{
			return;
		}
		
		final Creature mostHate = npc.getMostHated();
		if (mostHate == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		
		setAttackTarget(mostHate);
		npc.setTarget(mostHate);
		
		// Immobilize condition
		if (npc.isMovementDisabled())
		{
			movementDisable();
			return;
		}
		
		// Check if target is dead or if timeout is expired to stop this attack
		final Creature originalAttackTarget = getAttackTarget();
		if ((originalAttackTarget == null) || originalAttackTarget.isAlikeDead())
		{
			// Stop hating this target after the attack timeout or if target is dead
			npc.stopHating(originalAttackTarget);
			return;
		}
		
		if (_attackTimeout < GameTimeTaskManager.getInstance().getGameTicks())
		{
			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
			
			if (!_actor.isFakePlayer())
			{
				npc.setWalking();
			}
			
			// Monster teleport to spawn
			if (npc.isMonster() && (npc.getSpawn() != null) && (npc.getInstanceId() == 0) && (npc.isInCombat() || World.getInstance().getVisibleObjects(npc, PlayerInstance.class).isEmpty()))
			{
				npc.teleToLocation(npc.getSpawn(), false);
			}
			return;
		}
		
		// Actor should be able to see target.
		if (!GeoEngine.getInstance().canSeeTarget(_actor, originalAttackTarget))
		{
			moveTo(originalAttackTarget);
			return;
		}
		
		final int collision = npc.getTemplate().getCollisionRadius();
		
		// Handle all WorldObject of its Faction inside the Faction Range
		
		final Set<Integer> clans = getActiveChar().getTemplate().getClans();
		if ((clans != null) && !clans.isEmpty())
		{
			final int factionRange = npc.getTemplate().getClanHelpRange() + collision;
			// Go through all WorldObject that belong to its faction
			try
			{
				final Creature finalTarget = originalAttackTarget;
				// Call friendly npcs for help only if this NPC was attacked by the target creature.
				boolean targetExistsInAttackByList = false;
				for (Creature reference : npc.getAttackByList())
				{
					if (reference == finalTarget)
					{
						targetExistsInAttackByList = true;
						break;
					}
				}
				if (targetExistsInAttackByList)
				{
					World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, factionRange, called ->
					{
						// Don't call dead npcs, npcs without ai or npcs which are too far away.
						if (called.isDead() || !called.hasAI() || (Math.abs(finalTarget.getZ() - called.getZ()) > 600))
						{
							return;
						}
						// Don't call npcs who are already doing some action (e.g. attacking, casting).
						if ((called.getAI()._intention != AI_INTENTION_IDLE) && (called.getAI()._intention != AI_INTENTION_ACTIVE))
						{
							return;
						}
						// Don't call npcs who aren't in the same clan.
						if (!getActiveChar().getTemplate().isClan(called.getTemplate().getClans()))
						{
							return;
						}
						
						if (finalTarget.isPlayable())
						{
							// Dimensional Rift check.
							if (finalTarget.isInParty() && finalTarget.getParty().isInDimensionalRift())
							{
								final byte riftType = finalTarget.getParty().getDimensionalRift().getType();
								final byte riftRoom = finalTarget.getParty().getDimensionalRift().getCurrentRoom();
								if ((npc instanceof RiftInvaderInstance) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
								{
									return;
								}
							}
							
							// By default, when a faction member calls for help, attack the caller's attacker.
							// Notify the AI with EVT_AGGRESSION
							called.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, finalTarget, 1);
							EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(called, getActiveChar(), finalTarget.getActingPlayer(), finalTarget.isSummon()), called);
						}
						else if (called.getAI()._intention != AI_INTENTION_ATTACK)
						{
							called.addDamageHate(finalTarget, 0, npc.getHating(finalTarget));
							called.getAI().setIntention(AI_INTENTION_ATTACK, finalTarget);
						}
					});
				}
			}
			catch (NullPointerException e)
			{
				// LOGGER.warning(getClass().getSimpleName() + ": There has been a problem trying to think the attack!", e);
			}
		}
		
		// Initialize data
		final List<Skill> aiSuicideSkills = npc.getTemplate().getAISkills(AISkillScope.SUICIDE);
		if (!aiSuicideSkills.isEmpty() && ((int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30))
		{
			final Skill skill = aiSuicideSkills.get(Rnd.get(aiSuicideSkills.size()));
			if (Util.checkIfInRange(skill.getAffectRange(), getActiveChar(), mostHate, false) && npc.hasSkillChance() && cast(skill))
			{
				return;
			}
		}
		
		// ------------------------------------------------------
		// In case many mobs are trying to hit from same place, move a bit, circling around the target
		// Note from Gnacik:
		// On l2js because of that sometimes mobs don't attack player only running
		// around player without any sense, so decrease chance for now
		final int combinedCollision = collision + mostHate.getTemplate().getCollisionRadius();
		if (!npc.isMovementDisabled() && (Rnd.get(100) <= 3))
		{
			for (Attackable nearby : World.getInstance().getVisibleObjects(npc, Attackable.class))
			{
				if (npc.isInsideRadius2D(nearby, collision) && (nearby != mostHate))
				{
					int newX = combinedCollision + Rnd.get(40);
					newX = Rnd.nextBoolean() ? mostHate.getX() + newX : mostHate.getX() - newX;
					int newY = combinedCollision + Rnd.get(40);
					newY = Rnd.nextBoolean() ? mostHate.getY() + newY : mostHate.getY() - newY;
					if (!npc.isInsideRadius2D(newX, newY, 0, collision))
					{
						final int newZ = npc.getZ() + 30;
						
						// Mobius: Verify destination. Prevents wall collision issues and fixes monsters not avoiding obstacles.
						moveTo(GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getInstanceId()));
					}
					return;
				}
			}
		}
		
		// Calculate Archer movement.
		if ((!npc.isMovementDisabled()) && (npc.getAiType() == AIType.ARCHER) && (Rnd.get(100) < 15))
		{
			final double distance2 = npc.calculateDistanceSq2D(mostHate);
			if (Math.sqrt(distance2) <= (60 + combinedCollision))
			{
				int posX = npc.getX();
				int posY = npc.getY();
				final int posZ = npc.getZ() + 30;
				if (originalAttackTarget.getX() < posX)
				{
					posX += 300;
				}
				else
				{
					posX -= 300;
				}
				
				if (originalAttackTarget.getY() < posY)
				{
					posY += 300;
				}
				else
				{
					posY -= 300;
				}
				
				if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceId()))
				{
					setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, posZ, 0));
				}
				return;
			}
		}
		
		// BOSS/Raid Minion Target Reconsider
		if (npc.isRaid() || npc.isRaidMinion())
		{
			_chaosTime++;
			if (npc instanceof RaidBossInstance)
			{
				if (!((MonsterInstance) npc).hasMinions())
				{
					if ((_chaosTime > Config.RAID_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 100) / npc.getMaxHp()))))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
				else
				{
					if ((_chaosTime > Config.RAID_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()))))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
			}
			else if (npc instanceof GrandBossInstance)
			{
				if (_chaosTime > Config.GRAND_CHAOS_TIME)
				{
					final double chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
					if (((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate)))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
			}
			else
			{
				if ((_chaosTime > Config.MINION_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()))))
				{
					aggroReconsider();
					_chaosTime = 0;
					return;
				}
			}
		}
		
		final List<Skill> generalSkills = npc.getTemplate().getAISkills(AISkillScope.GENERAL);
		if (!generalSkills.isEmpty())
		{
			// Heal Condition
			final List<Skill> aiHealSkills = npc.getTemplate().getAISkills(AISkillScope.HEAL);
			if (!aiHealSkills.isEmpty())
			{
				if (npc.isMinion())
				{
					final Creature leader = npc.getLeader();
					if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
					{
						for (Skill healSkill : aiHealSkills)
						{
							if (healSkill.getTargetType() == TargetType.SELF)
							{
								continue;
							}
							
							if (!checkSkillCastConditions(npc, healSkill))
							{
								continue;
							}
							
							if (!Util.checkIfInRange((healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(healSkill) && !npc.isMovementDisabled())
							{
								moveToPawn(leader, healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
								return;
							}
							
							if (GeoEngine.getInstance().canSeeTarget(npc, leader))
							{
								clientStopMoving(null);
								final WorldObject target = npc.getTarget();
								npc.setTarget(leader);
								npc.doCast(healSkill);
								npc.setTarget(target);
								// LOGGER.debug(this + " used heal skill " + healSkill + " on leader " + leader);
								return;
							}
						}
					}
				}
				
				double percentage = (npc.getCurrentHp() / npc.getMaxHp()) * 100;
				if (Rnd.get(100) < ((100 - percentage) / 3))
				{
					for (Skill sk : aiHealSkills)
					{
						if (!checkSkillCastConditions(npc, sk))
						{
							continue;
						}
						
						clientStopMoving(null);
						final WorldObject target = npc.getTarget();
						npc.setTarget(npc);
						npc.doCast(sk);
						npc.setTarget(target);
						// LOGGER.debug(this + " used heal skill " + sk + " on itself");
						return;
					}
				}
				
				for (Skill sk : aiHealSkills)
				{
					if (!checkSkillCastConditions(npc, sk))
					{
						continue;
					}
					
					if (sk.getTargetType() == TargetType.ONE)
					{
						for (Attackable obj : World.getInstance().getVisibleObjectsInRange(npc, Attackable.class, sk.getCastRange() + collision))
						{
							if (!obj.isDead())
							{
								continue;
							}
							
							if (!obj.isInMyClan(npc))
							{
								continue;
							}
							
							percentage = (obj.getCurrentHp() / obj.getMaxHp()) * 100;
							if ((Rnd.get(100) < ((100 - percentage) / 10)) && GeoEngine.getInstance().canSeeTarget(npc, obj))
							{
								clientStopMoving(null);
								final WorldObject target = npc.getTarget();
								npc.setTarget(obj);
								npc.doCast(sk);
								npc.setTarget(target);
								// LOGGER.debug(this + " used heal skill " + sk + " on " + obj);
								return;
							}
						}
					}
					
					if (isParty(sk))
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
			
			// Res Skill Condition
			final List<Skill> aiResSkills = npc.getTemplate().getAISkills(AISkillScope.RES);
			if (!aiResSkills.isEmpty())
			{
				if (npc.isMinion())
				{
					final Creature leader = npc.getLeader();
					if ((leader != null) && leader.isDead())
					{
						for (Skill sk : aiResSkills)
						{
							if (sk.getTargetType() == TargetType.SELF)
							{
								continue;
							}
							
							if (!checkSkillCastConditions(npc, sk))
							{
								continue;
							}
							
							if (!Util.checkIfInRange((sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(sk) && !npc.isMovementDisabled())
							{
								moveToPawn(leader, sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
								return;
							}
							
							if (GeoEngine.getInstance().canSeeTarget(npc, leader))
							{
								clientStopMoving(null);
								final WorldObject target = npc.getTarget();
								npc.setTarget(leader);
								npc.doCast(sk);
								npc.setTarget(target);
								// LOGGER.debug(this + " used resurrection skill " + sk + " on leader " + leader);
								return;
							}
						}
					}
				}
				
				for (Skill sk : aiResSkills)
				{
					if (!checkSkillCastConditions(npc, sk))
					{
						continue;
					}
					if (sk.getTargetType() == TargetType.ONE)
					{
						for (Attackable obj : World.getInstance().getVisibleObjectsInRange(npc, Attackable.class, sk.getCastRange() + collision))
						{
							if (!obj.isDead())
							{
								continue;
							}
							if (!npc.isInMyClan(obj))
							{
								continue;
							}
							
							if ((Rnd.get(100) < 10) && GeoEngine.getInstance().canSeeTarget(npc, obj))
							{
								clientStopMoving(null);
								final WorldObject target = npc.getTarget();
								npc.setTarget(obj);
								npc.doCast(sk);
								npc.setTarget(target);
								// LOGGER.debug(this + " used heal skill " + sk + " on clan member " + obj);
								return;
							}
						}
					}
					
					if (isParty(sk))
					{
						clientStopMoving(null);
						final WorldObject target = npc.getTarget();
						npc.setTarget(npc);
						npc.doCast(sk);
						npc.setTarget(target);
						// LOGGER.debug(this + " used heal skill " + sk + " on party");
						return;
					}
				}
			}
		}
		
		// Long/Short Range skill usage.
		if (!npc.getShortRangeSkills().isEmpty() && npc.hasSkillChance())
		{
			final Skill shortRangeSkill = npc.getShortRangeSkills().get(Rnd.get(npc.getShortRangeSkills().size()));
			if (checkSkillCastConditions(npc, shortRangeSkill))
			{
				clientStopMoving(null);
				npc.doCast(shortRangeSkill);
				// LOGGER.debug(this + " used short range skill " + shortRangeSkill + " on " + npc.getTarget());
				return;
			}
		}
		
		if (!npc.getLongRangeSkills().isEmpty() && npc.hasSkillChance())
		{
			final Skill longRangeSkill = npc.getLongRangeSkills().get(Rnd.get(npc.getLongRangeSkills().size()));
			if (checkSkillCastConditions(npc, longRangeSkill))
			{
				clientStopMoving(null);
				npc.doCast(longRangeSkill);
				// LOGGER.debug(this + " used long range skill " + longRangeSkill + " on " + npc.getTarget());
				return;
			}
		}
		
		final double dist = npc.calculateDistance2D(mostHate);
		final int dist2 = (int) dist - collision;
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		if (npc.getAiType() == AIType.ARCHER)
		{
			range = 850 + combinedCollision; // Base bow range for NPCs.
		}
		if (mostHate.isMoving())
		{
			range += 50;
			if (npc.isMoving())
			{
				range += 50;
			}
		}
		
		// Starts melee attack
		if ((dist2 > range) || !GeoEngine.getInstance().canSeeTarget(npc, mostHate))
		{
			if (npc.isMovementDisabled())
			{
				targetReconsider();
			}
			else
			{
				final Creature target = getAttackTarget();
				if (target != null)
				{
					if (target.isMoving())
					{
						range -= 100;
					}
					moveToPawn(target, Math.max(range, 5));
				}
			}
			return;
		}
		
		// Attacks target
		_actor.doAttack(getAttackTarget());
	}
	
	private boolean cast(Skill sk)
	{
		if (sk == null)
		{
			return false;
		}
		
		final Attackable caster = getActiveChar();
		if (!checkSkillCastConditions(caster, sk))
		{
			return false;
		}
		
		if ((getAttackTarget() == null) && (caster.getMostHated() != null))
		{
			setAttackTarget(caster.getMostHated());
		}
		
		final Creature attackTarget = getAttackTarget();
		if (attackTarget == null)
		{
			return false;
		}
		
		final double dist = caster.calculateDistance2D(attackTarget);
		double dist2 = dist - attackTarget.getTemplate().getCollisionRadius();
		final double srange = sk.getCastRange() + caster.getTemplate().getCollisionRadius();
		if (attackTarget.isMoving())
		{
			dist2 -= 30;
		}
		
		if (sk.isContinuous())
		{
			if (!sk.isDebuff())
			{
				if (!caster.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					_actor.setTarget(attackTarget);
					return true;
				}
				// If actor already have buff, start looking at others same faction mob to cast
				if (sk.getTargetType() == TargetType.SELF)
				{
					return false;
				}
				if (sk.getTargetType() == TargetType.ONE)
				{
					final Creature target = effectTargetReconsider(sk, true);
					if (target != null)
					{
						clientStopMoving(null);
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
				if (canParty(sk))
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else
			{
				if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
				{
					if (!attackTarget.isAffectedBySkill(sk.getId()))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == TargetType.ONE)
				{
					final Creature target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
		{
			if (sk.getTargetType() == TargetType.ONE)
			{
				if ((attackTarget.getEffectList().getFirstEffect(EffectType.BUFF) != null) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.setTarget(target);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if (((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				else if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.HEAL))
		{
			if (caster.isMinion() && (sk.getTargetType() != TargetType.SELF))
			{
				final Creature leader = caster.getLeader();
				if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
				{
					if (!Util.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
					{
						moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
					}
					if (GeoEngine.getInstance().canSeeTarget(caster, leader))
					{
						clientStopMoving(null);
						caster.setTarget(leader);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			
			double percentage = (caster.getCurrentHp() / caster.getMaxHp()) * 100;
			if (Rnd.get(100) < ((100 - percentage) / 3))
			{
				clientStopMoving(null);
				caster.setTarget(caster);
				caster.doCast(sk);
				caster.setTarget(attackTarget);
				return true;
			}
			
			if (sk.getTargetType() == TargetType.ONE)
			{
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (obj.isDead())
					{
						continue;
					}
					
					if (!caster.isInMyClan(obj))
					{
						continue;
					}
					
					percentage = (obj.getCurrentHp() / obj.getMaxHp()) * 100;
					if ((Rnd.get(100) < ((100 - percentage) / 10)) && GeoEngine.getInstance().canSeeTarget(caster, obj))
					{
						clientStopMoving(null);
						caster.setTarget(obj);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			if (isParty(sk))
			{
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getAffectRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (obj.isInMyClan(caster) && (obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
					{
						clientStopMoving(null);
						caster.setTarget(caster);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK, EffectType.MAGICAL_ATTACK, EffectType.DEATH_LINK, EffectType.HP_DRAIN))
		{
			if (!canAura(sk))
			{
				if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				final Creature target = skillTargetReconsider(sk);
				if (target != null)
				{
					clientStopMoving(null);
					caster.setTarget(target);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else
			{
				clientStopMoving(null);
				caster.doCast(sk);
				return true;
			}
		}
		
		if (sk.hasEffectType(EffectType.SLEEP))
		{
			if (sk.getTargetType() == TargetType.ONE)
			{
				final double range = caster.getPhysicalAttackRange() + caster.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
				if (!attackTarget.isDead() && (dist2 <= srange) && ((dist2 > range) || attackTarget.isMoving()) && !attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.STUN, EffectType.ROOT, EffectType.PARALYZE, EffectType.MUTE, EffectType.FEAR))
		{
			if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && (dist2 <= srange))
			{
				if (!attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (sk.getTargetType() == TargetType.ONE)
			{
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT))
		{
			if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
			{
				if (!attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (sk.getTargetType() == TargetType.ONE)
			{
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.RESURRECTION))
		{
			if (!isParty(sk))
			{
				if (caster.isMinion() && (sk.getTargetType() != TargetType.SELF))
				{
					final Creature leader = caster.getLeader();
					if (leader != null)
					{
						if (leader.isDead() && !Util.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
						{
							moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
						}
						if (GeoEngine.getInstance().canSeeTarget(caster, leader))
						{
							clientStopMoving(null);
							caster.setTarget(leader);
							caster.doCast(sk);
							caster.setTarget(attackTarget);
							return true;
						}
					}
				}
				
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (!obj.isDead())
					{
						continue;
					}
					
					if (!caster.isInMyClan(obj))
					{
						continue;
					}
					
					if ((Rnd.get(100) < 10) && GeoEngine.getInstance().canSeeTarget(caster, obj))
					{
						clientStopMoving(null);
						caster.setTarget(obj);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			else if (isParty(sk))
			{
				for (Npc obj : World.getInstance().getVisibleObjectsInRange(caster, Npc.class, sk.getAffectRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (caster.isInMyClan(obj) && (obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
					{
						clientStopMoving(null);
						caster.setTarget(caster);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
		}
		
		if (!canAura(sk))
		{
			if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
			{
				clientStopMoving(null);
				caster.doCast(sk);
				return true;
			}
			
			final Creature target = skillTargetReconsider(sk);
			if (target != null)
			{
				clientStopMoving(null);
				caster.setTarget(target);
				caster.doCast(sk);
				caster.setTarget(attackTarget);
				return true;
			}
		}
		else
		{
			clientStopMoving(null);
			caster.doCast(sk);
			return true;
		}
		return false;
	}
	
	private void movementDisable()
	{
		final Creature target = getAttackTarget();
		if (target == null)
		{
			return;
		}
		
		final Attackable npc = getActiveChar();
		if (npc.getTarget() == null)
		{
			npc.setTarget(target);
		}
		
		final double dist = npc.calculateDistance2D(target);
		
		// TODO(Zoey76): Review this "magic changes".
		final int random = Rnd.get(100);
		if (!target.isImmobilized() && (random < 15) && tryCast(npc, target, AISkillScope.IMMOBILIZE, dist))
		{
			return;
		}
		
		if ((random < 20) && tryCast(npc, target, AISkillScope.COT, dist))
		{
			return;
		}
		
		if ((random < 30) && tryCast(npc, target, AISkillScope.DEBUFF, dist))
		{
			return;
		}
		
		if ((random < 40) && tryCast(npc, target, AISkillScope.NEGATIVE, dist))
		{
			return;
		}
		
		if ((npc.isMovementDisabled() || (npc.getAiType() == AIType.MAGE) || (npc.getAiType() == AIType.HEALER)) && tryCast(npc, target, AISkillScope.ATTACK, dist))
		{
			return;
		}
		
		if (tryCast(npc, target, AISkillScope.UNIVERSAL, dist))
		{
			return;
		}
		
		// If cannot cast, try to attack.
		final int range = npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
		if ((dist <= range) && GeoEngine.getInstance().canSeeTarget(npc, target))
		{
			_actor.doAttack(target);
			return;
		}
		
		// If cannot cast nor attack, find a new target.
		targetReconsider();
	}
	
	private boolean tryCast(Attackable npc, Creature target, AISkillScope aiSkillScope, double dist)
	{
		for (Skill sk : npc.getTemplate().getAISkills(aiSkillScope))
		{
			if (!checkSkillCastConditions(npc, sk) || (((sk.getCastRange() + target.getTemplate().getCollisionRadius()) <= dist) && !canAura(sk)))
			{
				continue;
			}
			
			if (!GeoEngine.getInstance().canSeeTarget(npc, target))
			{
				continue;
			}
			
			clientStopMoving(null);
			npc.doCast(sk);
			return true;
		}
		return false;
	}
	
	/**
	 * @param caster the caster
	 * @param skill the skill to check.
	 * @return {@code true} if the skill is available for casting {@code false} otherwise.
	 */
	private static boolean checkSkillCastConditions(Attackable caster, Skill skill)
	{
		if (caster.isCastingNow() && !skill.isSimultaneousCast())
		{
			return false;
		}
		
		// Not enough MP.
		if (skill.getMpConsume() >= caster.getCurrentMp())
		{
			return false;
		}
		
		// Character is in "skill disabled" mode.
		if (caster.isSkillDisabled(skill))
		{
			return false;
		}
		
		// If is a static skill and magic skill and character is muted or is a physical skill muted and character is physically muted.
		if (!skill.isStatic() && ((skill.isMagic() && caster.isMuted()) || caster.isPhysicalMuted()))
		{
			return false;
		}
		
		return true;
	}
	
	private Creature effectTargetReconsider(Skill sk, boolean positive)
	{
		if (sk == null)
		{
			return null;
		}
		
		final Attackable actor = getActiveChar();
		if (!sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
		{
			if (!positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				for (Creature obj : actor.getAttackByList())
				{
					if ((obj == null) || obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj) || (obj == getAttackTarget()))
					{
						continue;
					}
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(obj);
						dist2 = dist - actor.getTemplate().getCollisionRadius();
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
						if (obj.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					if ((dist2 <= range) && !getAttackTarget().isAffectedBySkill(sk.getId()))
					{
						return obj;
					}
				}
				
				// ----------------------------------------------------------------------
				// If there is nearby Target with aggro, start going on random target that is attackable
				for (Creature obj : World.getInstance().getVisibleObjectsInRange(actor, Creature.class, range))
				{
					if (obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj))
					{
						continue;
					}
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(obj);
						dist2 = dist;
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
						if (obj.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					
					if ((obj.isPlayer() || obj.isSummon()) && (dist2 <= range) && !getAttackTarget().isAffectedBySkill(sk.getId()))
					{
						return obj;
					}
				}
			}
			else if (positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				for (Attackable targets : World.getInstance().getVisibleObjectsInRange(actor, Attackable.class, range))
				{
					if (targets.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, targets))
					{
						continue;
					}
					
					if (targets.isInMyClan(actor))
					{
						continue;
					}
					
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(targets);
						dist2 = dist - actor.getTemplate().getCollisionRadius();
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + targets.getTemplate().getCollisionRadius();
						if (targets.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					if ((dist2 <= range) && !targets.isAffectedBySkill(sk.getId()))
					{
						return targets;
					}
				}
			}
		}
		else
		{
			double dist = 0;
			double dist2 = 0;
			int range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
			for (Creature obj : World.getInstance().getVisibleObjectsInRange(actor, Creature.class, range))
			{
				if (obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj))
				{
					continue;
				}
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 -= 70;
					}
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if ((obj.isPlayer() || obj.isSummon()) && (dist2 <= range) && (getAttackTarget().getEffectList().getFirstEffect(EffectType.BUFF) != null))
				{
					return obj;
				}
			}
		}
		return null;
	}
	
	private Creature skillTargetReconsider(Skill sk)
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		final Attackable actor = getActiveChar();
		if (actor.getHateList() != null)
		{
			for (Creature obj : actor.getHateList())
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead())
				{
					continue;
				}
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
					// if(obj.isMoving())
					// dist2 = dist2 - 40;
				}
				catch (NullPointerException e)
				{
					continue;
				}
				if (dist2 <= range)
				{
					return obj;
				}
			}
		}
		
		if (!(actor instanceof GuardInstance))
		{
			for (WorldObject target : World.getInstance().getVisibleObjects(actor, WorldObject.class))
			{
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(target);
					dist2 = dist;
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
					// if(obj.isMoving())
					// dist2 = dist2 - 40;
				}
				catch (NullPointerException e)
				{
					continue;
				}
				final Creature obj = target.isCreature() ? (Creature) target : null;
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || (dist2 > range))
				{
					continue;
				}
				if (obj.isPlayer())
				{
					return obj;
				}
				if (obj.isAttackable() && actor.isChaos())
				{
					if (!((Attackable) obj).isInMyClan(actor))
					{
						return obj;
					}
					continue;
				}
				if (obj.isSummon())
				{
					return obj;
				}
			}
		}
		return null;
	}
	
	private void targetReconsider()
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		final Attackable actor = getActiveChar();
		final Creature mostHate = actor.getMostHated();
		if (actor.getHateList() != null)
		{
			for (Creature obj : actor.getHateList())
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor))
				{
					continue;
				}
				
				try
				{
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = actor.getPhysicalAttackRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 -= 70;
					}
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if (dist2 <= range)
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
					return;
				}
			}
		}
		if (!(actor instanceof GuardInstance))
		{
			World.getInstance().forEachVisibleObject(actor, Creature.class, obj ->
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor) || (obj == getAttackTarget()))
				{
					return;
				}
				if (obj.isPlayer())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj.isAttackable())
				{
					if (actor.isChaos())
					{
						if (((Attackable) obj).isInMyClan(actor))
						{
							return;
						}
						actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj.isSummon())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			});
		}
	}
	
	private void aggroReconsider()
	{
		final Attackable actor = getActiveChar();
		final Creature mostHate = actor.getMostHated();
		if (actor.getHateList() != null)
		{
			final int rand = Rnd.get(actor.getHateList().size());
			int count = 0;
			for (Creature obj : actor.getHateList())
			{
				if (count < rand)
				{
					count++;
					continue;
				}
				
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj == getAttackTarget()) || (obj == actor))
				{
					continue;
				}
				
				try
				{
					actor.setTarget(getAttackTarget());
				}
				catch (NullPointerException e)
				{
					continue;
				}
				actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
				actor.setTarget(obj);
				setAttackTarget(obj);
				return;
			}
		}
		
		if (!(actor instanceof GuardInstance))
		{
			World.getInstance().forEachVisibleObject(actor, Creature.class, obj ->
			{
				if (!GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor))
				{
					return;
				}
				if (obj.isPlayer())
				{
					actor.addDamageHate(obj, 0, (mostHate != null) && !mostHate.isDead() ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj.isAttackable())
				{
					if (actor.isChaos())
					{
						if (((Attackable) obj).isInMyClan(actor))
						{
							return;
						}
						actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj.isSummon())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			});
		}
	}
	
	/**
	 * Manage AI thinking actions of a Attackable.
	 */
	@Override
	public void onEvtThink()
	{
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (_thinking || getActiveChar().isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a Attackable
			switch (getIntention())
			{
				case AI_INTENTION_ACTIVE:
				{
					thinkActive();
					break;
				}
				case AI_INTENTION_ATTACK:
				{
					thinkAttack();
					break;
				}
				case AI_INTENTION_CAST:
				{
					thinkCast();
					break;
				}
			}
		}
		catch (Exception e)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": " + this.getActor().getName() + " - onEvtThink() failed!");
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
		final Attackable me = getActiveChar();
		
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor
		me.addDamageHate(attacker, 0, 1);
		
		// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance
		if (!me.isRunning())
		{
			me.setRunning();
		}
		
		// Set the Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK)
		{
			setIntention(AI_INTENTION_ATTACK, attacker);
		}
		else if (me.getMostHated() != getAttackTarget())
		{
			setIntention(AI_INTENTION_ATTACK, attacker);
		}
		
		if (me.isMonster())
		{
			MonsterInstance master = (MonsterInstance) me;
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
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
	 * @param target the Creature that attacks
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		final Attackable me = getActiveChar();
		if (me.isDead() || (target == null))
		{
			return;
		}
		
		// Add the target to the actor _aggroList or update hate if already present
		me.addDamageHate(target, 0, aggro);
		
		// Set the actor AI Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK)
		{
			// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance
			if (!me.isRunning())
			{
				me.setRunning();
			}
			
			setIntention(AI_INTENTION_ATTACK, target);
		}
		
		if (me.isMonster())
		{
			MonsterInstance master = (MonsterInstance) me;
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, target);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, target);
			}
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	public Attackable getActiveChar()
	{
		return (Attackable) _actor;
	}
	
	public int getFearTime()
	{
		return _fearTime;
	}
	
	public void setFearTime(int fearTime)
	{
		_fearTime = fearTime;
	}
}
