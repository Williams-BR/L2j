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
package handlers.effecthandlers;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.TrapInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skills.BuffInfo;

/**
 * Summon Trap effect implementation.
 * @author Zoey76
 */
public class SummonTrap extends AbstractEffect
{
	private final int _despawnTime;
	private final int _npcId;
	
	public SummonTrap(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_despawnTime = params.getInt("despawnTime", 0);
		_npcId = params.getInt("npcId", 0);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if ((info.getEffected() == null) || !info.getEffected().isPlayer() || info.getEffected().isAlikeDead() || info.getEffected().getActingPlayer().inObserverMode())
		{
			return;
		}
		
		if (_npcId <= 0)
		{
			LOGGER.warning(SummonTrap.class.getSimpleName() + ": Invalid NPC ID:" + _npcId + " in skill ID: " + info.getSkill().getId());
			return;
		}
		
		final PlayerInstance player = info.getEffected().getActingPlayer();
		if (player.inObserverMode() || player.isMounted())
		{
			return;
		}
		
		// Unsummon previous trap
		if (player.getTrap() != null)
		{
			player.getTrap().unSummon();
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_npcId);
		if (npcTemplate == null)
		{
			LOGGER.warning(SummonTrap.class.getSimpleName() + ": Spawn of the non-existing Trap ID: " + _npcId + " in skill ID:" + info.getSkill().getId());
			return;
		}
		
		final TrapInstance trap = new TrapInstance(npcTemplate, player, _despawnTime);
		trap.setCurrentHp(trap.getMaxHp());
		trap.setCurrentMp(trap.getMaxMp());
		trap.setInvul(true);
		trap.setHeading(player.getHeading());
		trap.spawnMe(player.getX(), player.getY(), player.getZ());
		player.setTrap(trap);
	}
}
