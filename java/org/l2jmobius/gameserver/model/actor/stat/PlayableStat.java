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
package org.l2jmobius.gameserver.model.actor.stat;

import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.impl.creature.playable.OnPlayableExpChanged;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.type.SwampZone;

public class PlayableStat extends CreatureStat
{
	protected static final Logger LOGGER = Logger.getLogger(PlayableStat.class.getName());
	
	public PlayableStat(Playable activeChar)
	{
		super(activeChar);
	}
	
	public boolean addExp(long amount)
	{
		final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnPlayableExpChanged(getActiveChar(), getExp(), getExp() + amount), getActiveChar(), TerminateReturn.class);
		if ((term != null) && term.terminate())
		{
			return false;
		}
		
		if (((getExp() + amount) < 0) || ((amount > 0) && (getExp() == (getExpForLevel(getMaxLevel()) - 1))))
		{
			return true;
		}
		
		long value = amount;
		if ((getExp() + value) >= getExpForLevel(getMaxLevel()))
		{
			value = getExpForLevel(getMaxLevel()) - 1 - getExp();
		}
		
		setExp(getExp() + value);
		byte minimumLevel = 1;
		if (getActiveChar().isPet())
		{
			// get minimum level from NpcTemplate
			minimumLevel = (byte) PetDataTable.getInstance().getPetMinLevel(((PetInstance) getActiveChar()).getTemplate().getId());
		}
		
		byte level = minimumLevel; // minimum level
		for (byte tmp = level; tmp <= getMaxLevel(); tmp++)
		{
			if (getExp() >= getExpForLevel(tmp))
			{
				continue;
			}
			level = --tmp;
			break;
		}
		
		if ((level != getLevel()) && (level >= minimumLevel))
		{
			addLevel((byte) (level - getLevel()));
		}
		
		return true;
	}
	
	public boolean removeExp(long amount)
	{
		long value = amount;
		if ((getExp() - value) < 0)
		{
			value = getExp() - 1;
		}
		
		setExp(getExp() - value);
		byte minimumLevel = 1;
		if (getActiveChar().isPet())
		{
			// get minimum level from NpcTemplate
			minimumLevel = (byte) PetDataTable.getInstance().getPetMinLevel(((PetInstance) getActiveChar()).getTemplate().getId());
		}
		byte level = minimumLevel;
		for (byte tmp = level; tmp <= getMaxLevel(); tmp++)
		{
			if (getExp() >= getExpForLevel(tmp))
			{
				continue;
			}
			level = --tmp;
			break;
		}
		if ((level != getLevel()) && (level >= minimumLevel))
		{
			addLevel((byte) (level - getLevel()));
		}
		return true;
	}
	
	public boolean removeExpAndSp(long removeExp, long removeSp)
	{
		boolean expRemoved = false;
		boolean spRemoved = false;
		if (removeExp > 0)
		{
			expRemoved = removeExp(removeExp);
		}
		if (removeSp > 0)
		{
			spRemoved = removeSp(removeSp);
		}
		return expRemoved || spRemoved;
	}
	
	public boolean addLevel(byte amount)
	{
		byte value = amount;
		if ((getLevel() + value) > (getMaxLevel() - 1))
		{
			if (getLevel() < (getMaxLevel() - 1))
			{
				value = (byte) (getMaxLevel() - 1 - getLevel());
			}
			else
			{
				return false;
			}
		}
		
		final boolean levelIncreased = (getLevel() + value) > getLevel();
		value += getLevel();
		setLevel(value);
		
		// Sync up exp with current level
		if ((getExp() >= getExpForLevel(getLevel() + 1)) || (getExpForLevel(getLevel()) > getExp()))
		{
			setExp(getExpForLevel(getLevel()));
		}
		
		if (!levelIncreased && getActiveChar().isPlayer() && !getActiveChar().isGM() && Config.DECREASE_SKILL_LEVEL)
		{
			((PlayerInstance) getActiveChar()).checkPlayerSkills();
		}
		
		if (!levelIncreased)
		{
			return false;
		}
		
		getActiveChar().getStatus().setCurrentHp(getActiveChar().getStat().getMaxHp());
		getActiveChar().getStatus().setCurrentMp(getActiveChar().getStat().getMaxMp());
		
		return true;
	}
	
	public boolean addSp(long amount)
	{
		if (amount < 0)
		{
			LOGGER.warning("wrong usage");
			return false;
		}
		
		final long currentSp = getSp();
		if (currentSp >= Config.MAX_SP)
		{
			return false;
		}
		
		long value = amount;
		if (currentSp > (Config.MAX_SP - value))
		{
			value = Config.MAX_SP - currentSp;
		}
		
		setSp(currentSp + value);
		return true;
	}
	
	public boolean removeSp(long amount)
	{
		final long currentSp = getSp();
		if (currentSp < amount)
		{
			setSp(getSp() - currentSp);
			return true;
		}
		setSp(getSp() - amount);
		return true;
	}
	
	public long getExpForLevel(int level)
	{
		return ExperienceData.getInstance().getExpForLevel(level);
	}
	
	@Override
	public double getRunSpeed()
	{
		if (getActiveChar().isInsideZone(ZoneId.SWAMP))
		{
			final SwampZone zone = ZoneManager.getInstance().getZone(getActiveChar(), SwampZone.class);
			if (zone != null)
			{
				return super.getRunSpeed() * zone.getMoveBonus();
			}
		}
		return super.getRunSpeed();
	}
	
	@Override
	public double getWalkSpeed()
	{
		if (getActiveChar().isInsideZone(ZoneId.SWAMP))
		{
			final SwampZone zone = ZoneManager.getInstance().getZone(getActiveChar(), SwampZone.class);
			if (zone != null)
			{
				return super.getWalkSpeed() * zone.getMoveBonus();
			}
		}
		return super.getWalkSpeed();
	}
	
	@Override
	public Playable getActiveChar()
	{
		return (Playable) super.getActiveChar();
	}
	
	public int getMaxLevel()
	{
		return ExperienceData.getInstance().getMaxLevel();
	}
	
	@Override
	public int getPhysicalAttackAngle()
	{
		final Weapon weapon = getActiveChar().getActiveWeaponItem();
		return weapon != null ? weapon.getBaseAttackAngle() : super.getPhysicalAttackAngle();
	}
}
