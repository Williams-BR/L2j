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
package org.l2jmobius.gameserver.model.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.EffectList;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectTaskInfo;
import org.l2jmobius.gameserver.model.effects.EffectTickTask;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;

/**
 * Buff Info.<br>
 * Complex DTO that holds all the information for a given buff (or debuff or dance/song) set of effects issued by an skill.
 * @author Zoey76
 */
public class BuffInfo
{
	// Data
	/** Data. */
	private final Creature _effector;
	private final Creature _effected;
	private final Skill _skill;
	/** The effects. */
	private final List<AbstractEffect> _effects = new ArrayList<>(1);
	// Tasks
	/** Effect tasks for ticks. */
	private final Map<AbstractEffect, EffectTaskInfo> _tasks = new ConcurrentHashMap<>();
	// Time and ticks
	/** Abnormal time. */
	private int _abnormalTime;
	/** The game ticks at the start of this effect. */
	private final int _periodStartTicks;
	// Misc
	/** If {@code true} then this effect has been cancelled. */
	private volatile SkillFinishType _finishType = SkillFinishType.NORMAL;
	/** If {@code true} then this effect is in use (or has been stop because an Herb took place). */
	private boolean _isInUse = true;
	
	/**
	 * Buff Info constructor.
	 * @param effector
	 * @param effected
	 * @param skill
	 */
	public BuffInfo(Creature effector, Creature effected, Skill skill)
	{
		_effector = effector;
		_effected = effected;
		_skill = skill;
		_abnormalTime = Formulas.calcEffectAbnormalTime(effector, effected, skill);
		_periodStartTicks = GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Gets the effects on this buff info.
	 * @return the effects
	 */
	public List<AbstractEffect> getEffects()
	{
		return _effects;
	}
	
	/**
	 * Adds an effect to this buff info.
	 * @param effect the effect to add
	 */
	public void addEffect(AbstractEffect effect)
	{
		_effects.add(effect);
	}
	
	/**
	 * Adds an effect task to this buff info.
	 * @param effect the effect that owns the task
	 * @param effectTaskInfo the task info
	 */
	private void addTask(AbstractEffect effect, EffectTaskInfo effectTaskInfo)
	{
		_tasks.put(effect, effectTaskInfo);
	}
	
	/**
	 * Gets the task for the given effect.
	 * @param effect the effect
	 * @return the task
	 */
	private EffectTaskInfo getEffectTask(AbstractEffect effect)
	{
		return _tasks.get(effect);
	}
	
	/**
	 * Gets the skill that created this buff info.
	 * @return the skill
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Gets the calculated abnormal time.
	 * @return the abnormal time
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	/**
	 * Sets the abnormal time.
	 * @param abnormalTime the abnormal time to set
	 */
	public void setAbnormalTime(int abnormalTime)
	{
		_abnormalTime = abnormalTime;
	}
	
	/**
	 * Gets the period start ticks.
	 * @return the period start
	 */
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	/**
	 * Get the remaining time in seconds for this buff info.
	 * @return the elapsed time
	 */
	public int getTime()
	{
		return _abnormalTime - ((GameTimeTaskManager.getInstance().getGameTicks() - _periodStartTicks) / GameTimeTaskManager.TICKS_PER_SECOND);
	}
	
	/**
	 * Verify if this buff info has been cancelled.
	 * @return {@code true} if this buff info has been cancelled, {@code false} otherwise
	 */
	public boolean isRemoved()
	{
		return _finishType == SkillFinishType.REMOVED;
	}
	
	/**
	 * Set the buff info to removed.
	 * @param type the SkillFinishType to set
	 */
	public void setFinishType(SkillFinishType type)
	{
		_finishType = type;
	}
	
	/**
	 * Verify if this buff info is in use.
	 * @return {@code true} if this buff info is in use, {@code false} otherwise
	 */
	public boolean isInUse()
	{
		return _isInUse;
	}
	
	/**
	 * Set the buff info to in use.
	 * @param value the value to set
	 */
	public void setInUse(boolean value)
	{
		_isInUse = value;
	}
	
	/**
	 * Gets the character that launched the buff.
	 * @return the effector
	 */
	public Creature getEffector()
	{
		return _effector;
	}
	
	/**
	 * Gets the target of the skill.
	 * @return the effected
	 */
	public Creature getEffected()
	{
		return _effected;
	}
	
	/**
	 * Stops all the effects for this buff info.<br>
	 * Removes effects stats.<br>
	 * <b>It will not remove the buff info from the effect list</b>.<br>
	 * Instead call {@link EffectList#stopSkillEffects(SkillFinishType, Skill)}
	 * @param type determines the system message that will be sent.
	 * @param broadcast if {@code true} broadcast abnormal visual effects
	 */
	public void stopAllEffects(SkillFinishType type, boolean broadcast)
	{
		setFinishType(type);
		
		// Remove this buff info from BuffFinishTask.
		_effected.removeBuffInfoTime(this);
		finishEffects(broadcast);
	}
	
	public void initializeEffects()
	{
		if ((_effected == null) || (_skill == null))
		{
			return;
		}
		
		// When effects are initialized, the successfully landed.
		if (_effected.isPlayer() && !_skill.isPassive())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S_EFFECT_CAN_BE_FELT);
			sm.addSkillName(_skill);
			_effected.sendPacket(sm);
		}
		
		// Creates a task that will stop all the effects.
		if (_abnormalTime > 0)
		{
			_effected.addBuffInfoTime(this);
		}
		
		boolean update = false;
		for (AbstractEffect effect : _effects)
		{
			if (effect.isInstant() || (_effected.isDead() && !_skill.isPassive()))
			{
				continue;
			}
			
			// Call on start.
			effect.onStart(this);
			
			// Do not add continuous effect if target just died from the initial effect, otherwise they'll be ticked forever.
			if (_effected.isDead())
			{
				continue;
			}
			
			// If it's a continuous effect, if has ticks schedule a task with period, otherwise schedule a simple task to end it.
			if (effect.getTicks() > 0)
			{
				// The task for the effect ticks.
				final EffectTickTask effectTask = new EffectTickTask(this, effect);
				addTask(effect, new EffectTaskInfo(effectTask, ThreadPool.scheduleAtFixedRate(effectTask, effect.getTicks() * Config.EFFECT_TICK_RATIO, effect.getTicks() * Config.EFFECT_TICK_RATIO)));
			}
			
			// Add stats.
			_effected.addStatFuncs(effect.getStatFuncs(_effector, _effected, _skill));
			
			update = true;
		}
		
		if (update)
		{
			// Add abnormal visual effects.
			addAbnormalVisualEffects();
		}
	}
	
	/**
	 * Called on each tick.<br>
	 * Verify if the effect should end and the effect task should be cancelled.
	 * @param effect the effect that is ticking
	 */
	public void onTick(AbstractEffect effect)
	{
		boolean continueForever = false;
		// If the effect is in use, allow it to affect the effected.
		if (_isInUse)
		{
			// Callback for on action time event.
			continueForever = effect.onActionTime(this);
		}
		
		if (!continueForever && _skill.isToggle())
		{
			final EffectTaskInfo task = getEffectTask(effect);
			if (task != null)
			{
				final ScheduledFuture<?> schedule = task.getScheduledFuture();
				if ((schedule != null) && !schedule.isCancelled() && !schedule.isDone())
				{
					schedule.cancel(true); // Don't allow to finish current run.
				}
				_effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, _skill); // Remove the buff from the effect list.
			}
		}
	}
	
	public void finishEffects(boolean broadcast)
	{
		// Cancels the ticking task.
		for (EffectTaskInfo effectTask : _tasks.values())
		{
			final ScheduledFuture<?> schedule = effectTask.getScheduledFuture();
			if ((schedule != null) && !schedule.isCancelled() && !schedule.isDone())
			{
				schedule.cancel(true); // Don't allow to finish current run.
			}
		}
		// Remove stats
		removeStats();
		// Notify on exit.
		for (AbstractEffect effect : _effects)
		{
			// Instant effects shouldn't call onExit(..).
			if ((effect != null) && !effect.isInstant())
			{
				effect.onExit(this);
			}
		}
		// Remove abnormal visual effects.
		removeAbnormalVisualEffects(broadcast);
		// Set the proper system message.
		if (!(_effected.isSummon() && !((Summon) _effected).getOwner().hasSummon()))
		{
			SystemMessageId smId = null;
			if (_finishType == SkillFinishType.SILENT)
			{
				// smId is null.
			}
			else if (_skill.isToggle())
			{
				smId = SystemMessageId.S1_HAS_BEEN_ABORTED;
			}
			else if (_finishType == SkillFinishType.REMOVED)
			{
				smId = SystemMessageId.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED;
			}
			else if (!_skill.isPassive())
			{
				smId = SystemMessageId.S1_HAS_WORN_OFF;
			}
			
			if (smId != null)
			{
				final SystemMessage sm = new SystemMessage(smId);
				sm.addSkillName(_skill);
				_effected.sendPacket(sm);
			}
		}
		// Remove short buff.
		if (this == _effected.getEffectList().getShortBuff())
		{
			_effected.getEffectList().shortBuffStatusUpdate(null);
		}
	}
	
	/**
	 * Applies all the abnormal visual effects to the effected.<br>
	 * Prevents multiple updates.
	 */
	private void addAbnormalVisualEffects()
	{
		if (_skill.hasAbnormalVisualEffects())
		{
			_effected.startAbnormalVisualEffect(false, _skill.getAbnormalVisualEffects());
		}
		
		if (_effected.isPlayer() && _skill.hasAbnormalVisualEffectsEvent())
		{
			_effected.startAbnormalVisualEffect(false, _skill.getAbnormalVisualEffectsEvent());
		}
		
		if (_skill.hasAbnormalVisualEffectsSpecial())
		{
			_effected.startAbnormalVisualEffect(false, _skill.getAbnormalVisualEffectsSpecial());
		}
		
		// Update abnormal visual effects.
		_effected.updateAbnormalEffect();
	}
	
	/**
	 * Removes all the abnormal visual effects from the effected.<br>
	 * Prevents multiple updates.
	 * @param broadcast if {@code true} broadcast abnormal visual effects
	 */
	private void removeAbnormalVisualEffects(boolean broadcast)
	{
		if ((_effected == null) || (_skill == null))
		{
			return;
		}
		
		if (_skill.hasAbnormalVisualEffects())
		{
			_effected.stopAbnormalVisualEffect(false, _skill.getAbnormalVisualEffects());
		}
		
		if (_effected.isPlayer() && _skill.hasAbnormalVisualEffectsEvent())
		{
			_effected.stopAbnormalVisualEffect(false, _skill.getAbnormalVisualEffectsEvent());
		}
		
		if (_skill.hasAbnormalVisualEffectsSpecial())
		{
			_effected.stopAbnormalVisualEffect(false, _skill.getAbnormalVisualEffectsSpecial());
		}
		
		if (broadcast)
		{
			_effected.updateAbnormalEffect();
		}
	}
	
	/**
	 * Adds the buff stats.
	 */
	public void addStats()
	{
		_effects.forEach(effect -> _effected.addStatFuncs(effect.getStatFuncs(_effector, _effected, _skill)));
	}
	
	/**
	 * Removes the buff stats.
	 */
	public void removeStats()
	{
		_effects.forEach(_effected::removeStatsOwner);
		_effected.removeStatsOwner(_skill);
	}
	
	@Override
	public String toString()
	{
		return "BuffInfo [effector=" + _effector + ", effected=" + _effected + ", skill=" + _skill + ", effects=" + _effects + ", tasks=" + _tasks + ", abnormalTime=" + _abnormalTime + ", periodStartTicks=" + _periodStartTicks + ", isRemoved=" + isRemoved() + ", isInUse=" + _isInUse + "]";
	}
}