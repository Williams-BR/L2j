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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.MountType;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.instancemanager.CHSiegeManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.FortSiege;
import org.l2jmobius.gameserver.model.siege.Siegable;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * A siege zone
 * @author durgus
 */
public class SiegeZone extends ZoneType
{
	private static final int DISMOUNT_DELAY = 5;
	
	public SiegeZone(int id)
	{
		super(id);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
	}
	
	public class Settings extends AbstractZoneSettings
	{
		private int _siegableId = -1;
		private Siegable _siege = null;
		private boolean _isActiveSiege = false;
		
		protected Settings()
		{
		}
		
		public int getSiegeableId()
		{
			return _siegableId;
		}
		
		protected void setSiegeableId(int id)
		{
			_siegableId = id;
		}
		
		public Siegable getSiege()
		{
			return _siege;
		}
		
		public void setSiege(Siegable s)
		{
			_siege = s;
		}
		
		public boolean isActiveSiege()
		{
			return _isActiveSiege;
		}
		
		public void setActiveSiege(boolean value)
		{
			_isActiveSiege = value;
		}
		
		@Override
		public void clear()
		{
			_siegableId = -1;
			_siege = null;
			_isActiveSiege = false;
		}
	}
	
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			getSettings().setSiegeableId(Integer.parseInt(value));
		}
		else if (name.equals("fortId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			getSettings().setSiegeableId(Integer.parseInt(value));
		}
		else if (name.equals("clanHallId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			getSettings().setSiegeableId(Integer.parseInt(value));
			final SiegableHall hall = CHSiegeManager.getInstance().getConquerableHalls().get(getSettings().getSiegeableId());
			if (hall != null)
			{
				hall.setSiegeZone(this);
			}
			else
			{
				LOGGER.warning("SiegeZone: Siegable clan hall with id " + value + " does not exist!");
			}
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (!getSettings().isActiveSiege())
		{
			return;
		}
		
		creature.setInsideZone(ZoneId.PVP, true);
		creature.setInsideZone(ZoneId.SIEGE, true);
		creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true); // FIXME: Custom ?
		
		if (!creature.isPlayer())
		{
			return;
		}
		
		final PlayerInstance plyer = creature.getActingPlayer();
		if (plyer.isRegisteredOnThisSiegeField(getSettings().getSiegeableId()))
		{
			plyer.setInSiege(true); // in siege
			if (getSettings().getSiege().giveFame() && (getSettings().getSiege().getFameFrequency() > 0))
			{
				plyer.startFameTask(getSettings().getSiege().getFameFrequency() * 1000, getSettings().getSiege().getFameAmount());
			}
		}
		
		creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
		if (!Config.ALLOW_WYVERN_DURING_SIEGE && (plyer.getMountType() == MountType.WYVERN))
		{
			plyer.sendPacket(SystemMessageId.THIS_AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_ATOP_OF_A_WYVERN_YOU_WILL_BE_DISMOUNTED_FROM_YOUR_WYVERN_IF_YOU_DO_NOT_LEAVE);
			plyer.enteredNoLanding(DISMOUNT_DELAY);
		}
		
		if (!Config.ALLOW_MOUNTS_DURING_SIEGE && plyer.isMounted())
		{
			plyer.dismount();
		}
		
		if (!Config.ALLOW_MOUNTS_DURING_SIEGE && plyer.isTransformed() && plyer.getTransformation().isRiding())
		{
			plyer.untransform();
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, false);
		creature.setInsideZone(ZoneId.SIEGE, false);
		creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false); // FIXME: Custom ?
		if (getSettings().isActiveSiege() && creature.isPlayer())
		{
			final PlayerInstance player = creature.getActingPlayer();
			creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
			if (player.getMountType() == MountType.WYVERN)
			{
				player.exitedNoLanding();
			}
			// Set pvp flag
			if (player.getPvpFlag() == 0)
			{
				player.startPvPFlag();
			}
		}
		if (!creature.isPlayer())
		{
			return;
		}
		
		final PlayerInstance player = creature.getActingPlayer();
		player.stopFameTask();
		player.setInSiege(false);
		
		if (!(getSettings().getSiege() instanceof FortSiege) || (player.getInventory().getItemByItemId(9819) == null))
		{
			return;
		}
		
		final Fort fort = FortManager.getInstance().getFortById(getSettings().getSiegeableId());
		if (fort != null)
		{
			FortSiegeManager.getInstance().dropCombatFlag(player, fort.getResidenceId());
		}
		else
		{
			final int slot = player.getInventory().getSlotFromItem(player.getInventory().getItemByItemId(9819));
			player.getInventory().unEquipItemInBodySlot(slot);
			player.destroyItem("CombatFlag", player.getInventory().getItemByItemId(9819), null, true);
		}
		
		final Summon summon = player.getSummon();
		if (summon != null)
		{
			summon.abortAttack();
			summon.abortCast();
			summon.stopAllEffects();
			summon.unSummon(player);
		}
	}
	
	@Override
	public void onDieInside(Creature creature)
	{
		if (!getSettings().isActiveSiege() || !creature.isPlayer() || !creature.getActingPlayer().isRegisteredOnThisSiegeField(getSettings().getSiegeableId()))
		{
			return;
		}
		
		int level = 1;
		final BuffInfo info = creature.getEffectList().getBuffInfoBySkillId(5660);
		if (info != null)
		{
			level = Math.min(level + info.getSkill().getLevel(), 5);
		}
		
		final Skill skill = SkillData.getInstance().getSkill(5660, level);
		if (skill != null)
		{
			skill.applyEffects(creature, creature);
		}
	}
	
	@Override
	public void onPlayerLogoutInside(PlayerInstance player)
	{
		if (player.getClanId() != getSettings().getSiegeableId())
		{
			player.teleToLocation(TeleportWhereType.TOWN);
		}
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (getSettings().isActiveSiege())
		{
			for (Creature creature : getCharactersInside())
			{
				if (creature != null)
				{
					onEnter(creature);
				}
			}
		}
		else
		{
			PlayerInstance player;
			for (Creature creature : getCharactersInside())
			{
				if (creature == null)
				{
					continue;
				}
				
				creature.setInsideZone(ZoneId.PVP, false);
				creature.setInsideZone(ZoneId.SIEGE, false);
				creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
				
				if (creature.isPlayer())
				{
					player = creature.getActingPlayer();
					creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
					player.stopFameTask();
					if (player.getMountType() == MountType.WYVERN)
					{
						player.exitedNoLanding();
					}
				}
			}
		}
	}
	
	/**
	 * Sends a message to all players in this zone
	 * @param message
	 */
	public void announceToPlayers(String message)
	{
		for (PlayerInstance player : getPlayersInside())
		{
			if (player != null)
			{
				player.sendMessage(message);
			}
		}
	}
	
	public int getSiegeObjectId()
	{
		return getSettings().getSiegeableId();
	}
	
	public boolean isActive()
	{
		return getSettings().isActiveSiege();
	}
	
	public void setActive(boolean value)
	{
		getSettings().setActiveSiege(value);
	}
	
	public void setSiegeInstance(Siegable siege)
	{
		getSettings().setSiege(siege);
	}
	
	/**
	 * Removes all foreigners from the zone
	 * @param owningClanId
	 */
	public void banishForeigners(int owningClanId)
	{
		for (PlayerInstance temp : getPlayersInside())
		{
			if (temp.getClanId() == owningClanId)
			{
				continue;
			}
			temp.teleToLocation(TeleportWhereType.TOWN);
		}
	}
}
