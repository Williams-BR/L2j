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

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.creature.playable.OnPlayableExpChanged;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExSpawnEmitter;

/**
 * Soul Eating effect implementation.
 * @author UnAfraid
 */
public class SoulEating extends AbstractEffect
{
	private final int _expNeeded;
	
	public SoulEating(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		_expNeeded = params.getInt("expNeeded");
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		if (info.getEffected().isPlayer())
		{
			info.getEffected().removeListenerIf(EventType.ON_PLAYABLE_EXP_CHANGED, listener -> listener.getOwner() == this);
		}
	}
	
	public void onExperienceReceived(Playable playable, long exp)
	{
		// TODO: Verify logic.
		if (playable.isPlayer() && (exp >= _expNeeded))
		{
			final PlayerInstance player = playable.getActingPlayer();
			final int maxSouls = (int) player.calcStat(Stat.MAX_SOULS, 0, null, null);
			if (player.getChargedSouls() >= maxSouls)
			{
				playable.sendPacket(SystemMessageId.SOUL_CANNOT_BE_ABSORBED_ANYMORE);
				return;
			}
			
			player.increaseSouls(1);
			
			if ((player.getTarget() != null) && player.getTarget().isNpc())
			{
				final Npc npc = (Npc) playable.getTarget();
				player.broadcastPacket(new ExSpawnEmitter(player, npc), 500);
			}
		}
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (info.getEffected().isPlayer())
		{
			info.getEffected().addListener(new ConsumerEventListener(info.getEffected(), EventType.ON_PLAYABLE_EXP_CHANGED, (OnPlayableExpChanged event) -> onExperienceReceived(event.getActiveChar(), (event.getNewExp() - event.getOldExp())), this));
		}
	}
}
