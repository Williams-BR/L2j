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
package org.l2jmobius.gameserver.model.actor.tasks.creature;

import org.l2jmobius.gameserver.model.actor.Creature;

/**
 * Task launching the function onHitTimer().<br>
 * <b><u>Actions</u>:</b>
 * <ul>
 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a PlayerInstance)</li>
 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are PlayerInstance</li>
 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
 * </ul>
 * @author xban1x
 */
public class HitTask implements Runnable
{
	private final Creature _creature;
	private final Creature _hitTarget;
	private final int _damage;
	private final boolean _crit;
	private final boolean _miss;
	private final byte _shld;
	private final boolean _soulshot;
	
	public HitTask(Creature creature, Creature target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld)
	{
		_creature = creature;
		_hitTarget = target;
		_damage = damage;
		_crit = crit;
		_shld = shld;
		_miss = miss;
		_soulshot = soulshot;
	}
	
	@Override
	public void run()
	{
		if (_creature != null)
		{
			_creature.onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld);
		}
	}
}
