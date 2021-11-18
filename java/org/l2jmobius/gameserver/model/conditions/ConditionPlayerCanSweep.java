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
package org.l2jmobius.gameserver.model.conditions;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Checks Sweeper conditions:
 * <ul>
 * <li>Minimum checks, player not null, skill not null.</li>
 * <li>Checks if the target isn't null, is dead and spoiled.</li>
 * <li>Checks if the sweeper player is the target spoiler, or is in the spoiler party.</li>
 * <li>Checks if the corpse is too old.</li>
 * <li>Checks inventory limit and weight max load won't be exceed after sweep.</li>
 * </ul>
 * If two or more conditions aren't meet at the same time, one message per condition will be shown.
 * @author Zoey76
 */
public class ConditionPlayerCanSweep extends Condition
{
	private final boolean _value;
	
	public ConditionPlayerCanSweep(boolean value)
	{
		_value = value;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, Item item)
	{
		boolean canSweep = false;
		if (effector.getActingPlayer() != null)
		{
			final PlayerInstance sweeper = effector.getActingPlayer();
			if (skill != null)
			{
				final WorldObject[] targets = skill.getTargetList(sweeper);
				if (targets != null)
				{
					Attackable target;
					for (WorldObject objTarget : targets)
					{
						if ((objTarget != null) && objTarget.isAttackable())
						{
							target = (Attackable) objTarget;
							if (target.isDead())
							{
								if (target.isSpoiled())
								{
									canSweep = target.checkSpoilOwner(sweeper, true);
									canSweep &= !target.isOldCorpse(sweeper, Config.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY, true);
									canSweep &= sweeper.getInventory().checkInventorySlotsAndWeight(target.getSpoilLootItems(), true, true);
								}
								else
								{
									sweeper.sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);
								}
							}
						}
					}
				}
			}
		}
		return _value == canSweep;
	}
}
