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

import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.instancemanager.MapRegionManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.GuardInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;

/**
 * Escape effect implementation.
 * @author Adry_85
 */
public class Escape extends AbstractEffect
{
	private final TeleportWhereType _escapeType;
	
	public Escape(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_escapeType = params.getEnum("escapeType", TeleportWhereType.class, null);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (_escapeType == null)
		{
			return;
		}
		
		if (info.getEffected() instanceof GuardInstance)
		{
			info.getEffected().teleToLocation(((Npc) info.getEffected()).getSpawn());
			info.getEffected().setHeading(((Npc) info.getEffected()).getSpawn().getHeading());
		}
		else
		{
			info.getEffected().teleToLocation(MapRegionManager.getInstance().getTeleToLocation(info.getEffected(), _escapeType), true);
			info.getEffected().getActingPlayer().setIn7sDungeon(false);
			info.getEffected().setInstanceId(0);
		}
	}
}