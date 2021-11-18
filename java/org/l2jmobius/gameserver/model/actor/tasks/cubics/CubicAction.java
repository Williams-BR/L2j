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
package org.l2jmobius.gameserver.model.actor.tasks.cubics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.CubicInstance;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * Cubic action task.
 * @author Zoey76
 */
public class CubicAction implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(CubicAction.class.getName());
	private final CubicInstance _cubic;
	private final AtomicInteger _currentCount = new AtomicInteger();
	private final int _chance;
	
	public CubicAction(CubicInstance cubic, int chance)
	{
		_cubic = cubic;
		_chance = chance;
	}
	
	@Override
	public void run()
	{
		if (_cubic == null)
		{
			return;
		}
		
		try
		{
			if (_cubic.getOwner().isDead() || !_cubic.getOwner().isOnline())
			{
				_cubic.stopAction();
				_cubic.getOwner().getCubics().remove(_cubic.getId());
				_cubic.getOwner().broadcastUserInfo();
				_cubic.cancelDisappear();
				return;
			}
			
			if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_cubic.getOwner()))
			{
				if (_cubic.getOwner().hasSummon())
				{
					if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_cubic.getOwner().getSummon()))
					{
						_cubic.stopAction();
						return;
					}
				}
				else
				{
					_cubic.stopAction();
					return;
				}
			}
			
			// The cubic has already reached its limit and it will stay idle until its duration ends.
			if ((_cubic.getCubicMaxCount() > -1) && (_currentCount.get() >= _cubic.getCubicMaxCount()))
			{
				_cubic.stopAction();
				return;
			}
			
			// Smart Cubic debuff cancel is 100%
			boolean useCubicCure = false;
			if ((_cubic.getId() >= CubicInstance.SMART_CUBIC_EVATEMPLAR) && (_cubic.getId() <= CubicInstance.SMART_CUBIC_SPECTRALMASTER))
			{
				for (BuffInfo info : _cubic.getOwner().getEffectList().getDebuffs())
				{
					if (info.getSkill().canBeDispeled())
					{
						useCubicCure = true;
						info.getEffected().getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill());
					}
				}
			}
			
			if (useCubicCure)
			{
				// Smart Cubic debuff cancel is needed, no other skill is used in this activation period
				_cubic.getOwner().broadcastPacket(new MagicSkillUse(_cubic.getOwner(), _cubic.getOwner(), CubicInstance.SKILL_CUBIC_CURE, 1, 0, 0));
				
				// The cubic has done an action, increase the current count
				_currentCount.incrementAndGet();
			}
			else if (Rnd.get(1, 100) < _chance)
			{
				final Skill skill = _cubic.getSkills().get(Rnd.get(_cubic.getSkills().size()));
				if (skill == null)
				{
					return;
				}
				
				if (skill.getId() == CubicInstance.SKILL_CUBIC_HEAL)
				{
					// friendly skill, so we look a target in owner's party
					_cubic.cubicTargetForHeal();
				}
				else
				{
					// offensive skill, we look for an enemy target
					_cubic.getCubicTarget();
					if (!CubicInstance.isInCubicRange(_cubic.getOwner(), _cubic.getTarget()))
					{
						_cubic.setTarget(null);
					}
				}
				final Creature target = _cubic.getTarget();
				if ((target != null) && !target.isDead())
				{
					_cubic.getOwner().broadcastPacket(new MagicSkillUse(_cubic.getOwner(), target, skill.getId(), skill.getLevel(), 0, 0));
					
					final Creature[] targets =
					{
						target
					};
					
					if (skill.isContinuous())
					{
						_cubic.useCubicContinuous(skill, targets);
					}
					else
					{
						skill.activateSkill(_cubic, targets);
					}
					
					if (skill.hasEffectType(EffectType.MAGICAL_ATTACK))
					{
						_cubic.useCubicMdam(skill, targets);
					}
					else if (skill.hasEffectType(EffectType.HP_DRAIN))
					{
						_cubic.useCubicDrain(skill, targets);
					}
					else if (skill.hasEffectType(EffectType.STUN, EffectType.ROOT, EffectType.PARALYZE))
					{
						_cubic.useCubicDisabler(skill, targets);
					}
					else if (skill.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT))
					{
						_cubic.useCubicContinuous(skill, targets);
					}
					else if (skill.hasEffectType(EffectType.AGGRESSION))
					{
						_cubic.useCubicDisabler(skill, targets);
					}
					
					// The cubic has done an action, increase the current count
					_currentCount.incrementAndGet();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
	}
}