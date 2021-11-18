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
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.attackable.OnAttackableAttack;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.attackable.OnAttackableKill;
import org.l2jmobius.gameserver.model.skills.Skill;

/**
 * This class extends Guard class for quests, that require tracking of onAttack and onKill events from monsters' attacks.
 * @author GKR
 */
public class QuestGuardInstance extends GuardInstance
{
	private boolean _isAutoAttackable = true;
	private boolean _isPassive = false;
	
	/**
	 * Creates a quest guard.
	 * @param template the quest guard NPC template
	 */
	public QuestGuardInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.QuestGuardInstance);
	}
	
	@Override
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		super.addDamage(attacker, damage, skill);
		
		if (attacker.isAttackable())
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(null, this, damage, skill, false), this);
		}
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the NpcInstance (the corpse disappeared after 7 seconds)
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (killer.isAttackable())
		{
			// Delayed notification
			EventDispatcher.getInstance().notifyEventAsyncDelayed(new OnAttackableKill(null, this, false), this, _onKillDelay);
		}
		return true;
	}
	
	@Override
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		if (!_isPassive && !attacker.isPlayer())
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	public void setPassive(boolean value)
	{
		_isPassive = value;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return _isAutoAttackable && !attacker.isPlayer();
	}
	
	@Override
	public void setAutoAttackable(boolean value)
	{
		_isAutoAttackable = value;
	}
	
	public boolean isPassive()
	{
		return _isPassive;
	}
}
