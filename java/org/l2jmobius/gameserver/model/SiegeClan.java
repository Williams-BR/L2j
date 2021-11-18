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
package org.l2jmobius.gameserver.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.actor.Npc;

public class SiegeClan
{
	private int _clanId = 0;
	private final Set<Npc> _flag = ConcurrentHashMap.newKeySet();
	private SiegeClanType _type;
	
	public enum SiegeClanType
	{
		OWNER,
		DEFENDER,
		ATTACKER,
		DEFENDER_PENDING
	}
	
	public SiegeClan(int clanId, SiegeClanType type)
	{
		_clanId = clanId;
		_type = type;
	}
	
	public int getNumFlags()
	{
		return _flag.size();
	}
	
	public void addFlag(Npc flag)
	{
		_flag.add(flag);
	}
	
	public boolean removeFlag(Npc flag)
	{
		final boolean ret = _flag.remove(flag);
		flag.deleteMe();
		return ret;
	}
	
	public void removeFlags()
	{
		_flag.forEach(Npc::decayMe);
		_flag.clear();
	}
	
	public int getClanId()
	{
		return _clanId;
	}
	
	public Set<Npc> getFlag()
	{
		return _flag;
	}
	
	public SiegeClanType getType()
	{
		return _type;
	}
	
	public void setType(SiegeClanType setType)
	{
		_type = setType;
	}
}
