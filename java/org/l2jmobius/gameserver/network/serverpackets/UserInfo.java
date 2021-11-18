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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.model.Elementals;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class UserInfo implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private int _relation;
	private int _airShipHelm;
	
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	
	public UserInfo(PlayerInstance player)
	{
		_player = player;
		
		final int _territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(player);
		_relation = _player.isClanLeader() ? 0x40 : 0;
		if (_player.getSiegeState() == 1)
		{
			if (_territoryId == 0)
			{
				_relation |= 0x180;
			}
			else
			{
				_relation |= 0x1000;
			}
		}
		if (_player.getSiegeState() == 2)
		{
			_relation |= 0x80;
		}
		// _isDisguised = TerritoryWarManager.getInstance().isDisguised(character.getObjectId());
		if (_player.isInAirShip() && _player.getAirShip().isCaptain(_player))
		{
			_airShipHelm = _player.getAirShip().getHelmItemId();
		}
		else
		{
			_airShipHelm = 0;
		}
		
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(player.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(player.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(player.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(player.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = player.isFlying() ? _runSpd : 0;
		_flyWalkSpd = player.isFlying() ? _walkSpd : 0;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.USER_INFO.writeId(packet);
		
		packet.writeD(_player.getX());
		packet.writeD(_player.getY());
		packet.writeD(_player.getZ());
		packet.writeD(_player.getVehicle() != null ? _player.getVehicle().getObjectId() : 0);
		
		packet.writeD(_player.getObjectId());
		packet.writeS(_player.getAppearance().getVisibleName());
		packet.writeD(_player.getRace().ordinal());
		packet.writeD(_player.getAppearance().isFemale() ? 1 : 0);
		
		packet.writeD(_player.getBaseClass());
		
		packet.writeD(_player.getLevel());
		packet.writeQ(_player.getExp());
		packet.writeF((float) (_player.getExp() - ExperienceData.getInstance().getExpForLevel(_player.getLevel())) / (ExperienceData.getInstance().getExpForLevel(_player.getLevel() + 1) - ExperienceData.getInstance().getExpForLevel(_player.getLevel()))); // High Five exp %
		packet.writeD(_player.getSTR());
		packet.writeD(_player.getDEX());
		packet.writeD(_player.getCON());
		packet.writeD(_player.getINT());
		packet.writeD(_player.getWIT());
		packet.writeD(_player.getMEN());
		packet.writeD(_player.getMaxHp());
		packet.writeD((int) Math.round(_player.getCurrentHp()));
		packet.writeD(_player.getMaxMp());
		packet.writeD((int) Math.round(_player.getCurrentMp()));
		packet.writeD((int) _player.getSp());
		packet.writeD(_player.getCurrentLoad());
		packet.writeD(_player.getMaxLoad());
		
		packet.writeD(_player.getActiveWeaponItem() != null ? 40 : 20); // 20 no weapon, 40 weapon equipped
		
		for (int slot : getPaperdollOrder())
		{
			packet.writeD(_player.getInventory().getPaperdollObjectId(slot));
		}
		
		for (int slot : getPaperdollOrder())
		{
			packet.writeD(_player.getInventory().getPaperdollItemDisplayId(slot));
		}
		
		for (int slot : getPaperdollOrder())
		{
			packet.writeD(_player.getInventory().getPaperdollAugmentationId(slot));
		}
		
		packet.writeD(_player.getInventory().getTalismanSlots());
		packet.writeD(_player.getInventory().canEquipCloak() ? 1 : 0);
		packet.writeD((int) _player.getPAtk(null));
		packet.writeD((int) _player.getPAtkSpd());
		packet.writeD((int) _player.getPDef(null));
		packet.writeD(_player.getEvasionRate(null));
		packet.writeD(_player.getAccuracy());
		packet.writeD(_player.getCriticalHit(null, null));
		packet.writeD((int) _player.getMAtk(null, null));
		
		packet.writeD(_player.getMAtkSpd());
		packet.writeD((int) _player.getPAtkSpd());
		
		packet.writeD((int) _player.getMDef(null, null));
		
		packet.writeD(_player.getPvpFlag());
		packet.writeD(_player.getKarma());
		
		packet.writeD(_runSpd);
		packet.writeD(_walkSpd);
		packet.writeD(_swimRunSpd);
		packet.writeD(_swimWalkSpd);
		packet.writeD(_flyRunSpd);
		packet.writeD(_flyWalkSpd);
		packet.writeD(_flyRunSpd);
		packet.writeD(_flyWalkSpd);
		packet.writeF(_moveMultiplier);
		packet.writeF(_player.getAttackSpeedMultiplier());
		
		packet.writeF(_player.getCollisionRadius());
		packet.writeF(_player.getCollisionHeight());
		
		packet.writeD(_player.getAppearance().getHairStyle());
		packet.writeD(_player.getAppearance().getHairColor());
		packet.writeD(_player.getAppearance().getFace());
		packet.writeD(_player.isGM() ? 1 : 0); // builder level
		
		String title = _player.getTitle();
		if (_player.isGM() && _player.isInvisible())
		{
			title = "[Invisible]";
		}
		packet.writeS(title);
		
		packet.writeD(_player.getClanId());
		packet.writeD(_player.getClanCrestId());
		packet.writeD(_player.getAllyId());
		packet.writeD(_player.getAllyCrestId()); // ally crest id
		// 0x40 leader rights
		// siege flags: attacker - 0x180 sword over name, defender - 0x80 shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
		packet.writeD(_relation);
		packet.writeC(_player.getMountType().ordinal()); // mount type
		packet.writeC(_player.getPrivateStoreType().getId());
		packet.writeC(_player.hasDwarvenCraft() ? 1 : 0);
		packet.writeD(_player.getPkKills());
		packet.writeD(_player.getPvpKills());
		
		packet.writeH(_player.getCubics().size());
		for (int cubicId : _player.getCubics().keySet())
		{
			packet.writeH(cubicId);
		}
		
		packet.writeC(_player.isInPartyMatchRoom() ? 1 : 0);
		
		packet.writeD(_player.isInvisible() ? _player.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : _player.getAbnormalVisualEffects());
		packet.writeC(_player.isInsideZone(ZoneId.WATER) ? 1 : _player.isFlyingMounted() ? 2 : 0);
		
		packet.writeD(_player.getClanPrivileges().getBitmask());
		
		packet.writeH(_player.getRecomLeft()); // c2 recommendations remaining
		packet.writeH(_player.getRecomHave()); // c2 recommendations received
		packet.writeD(_player.getMountNpcId() > 0 ? _player.getMountNpcId() + 1000000 : 0);
		packet.writeH(_player.getInventoryLimit());
		
		packet.writeD(_player.getClassId().getId());
		packet.writeD(0x00); // special effects? circles around player...
		packet.writeD(_player.getMaxCp());
		packet.writeD((int) _player.getCurrentCp());
		packet.writeC(_player.isMounted() || (_airShipHelm != 0) ? 0 : _player.getEnchantEffect());
		
		packet.writeC(_player.getTeam().getId());
		
		packet.writeD(_player.getClanCrestLargeId());
		packet.writeC(_player.isNoble() ? 1 : 0); // 0x01: symbol on char menu ctrl+I
		packet.writeC(_player.isHero() || (_player.isGM() && Config.GM_HERO_AURA) ? 1 : 0); // 0x01: Hero Aura
		
		packet.writeC(_player.isFishing() ? 1 : 0); // Fishing Mode
		packet.writeD(_player.getFishx()); // fishing x
		packet.writeD(_player.getFishy()); // fishing y
		packet.writeD(_player.getFishz()); // fishing z
		packet.writeD(_player.getAppearance().getNameColor());
		
		// new c5
		packet.writeC(_player.isRunning() ? 0x01 : 0x00); // changes the Speed display on Status Window
		
		packet.writeD(_player.getPledgeClass()); // changes the text above CP on Status Window
		packet.writeD(_player.getPledgeType());
		
		packet.writeD(_player.getAppearance().getTitleColor());
		
		packet.writeD(_player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId()) : 0);
		
		// T1 Starts
		packet.writeD(_player.getTransformationDisplayId());
		
		final byte attackAttribute = _player.getAttackElement();
		packet.writeH(attackAttribute);
		packet.writeH(_player.getAttackElementValue(attackAttribute));
		packet.writeH(_player.getDefenseElementValue(Elementals.FIRE));
		packet.writeH(_player.getDefenseElementValue(Elementals.WATER));
		packet.writeH(_player.getDefenseElementValue(Elementals.WIND));
		packet.writeH(_player.getDefenseElementValue(Elementals.EARTH));
		packet.writeH(_player.getDefenseElementValue(Elementals.HOLY));
		packet.writeH(_player.getDefenseElementValue(Elementals.DARK));
		
		packet.writeD(_player.getAgathionId());
		
		// T2 Starts
		packet.writeD(_player.getFame()); // Fame
		packet.writeD(_player.isMinimapAllowed() ? 1 : 0); // Minimap on Hellbound
		packet.writeD(_player.getVitalityPoints()); // Vitality Points
		packet.writeD(_player.getAbnormalVisualEffectSpecial());
		// packet.writeD(_territoryId); // CT2.3
		// packet.writeD((_isDisguised ? 0x01: 0x00)); // CT2.3
		// packet.writeD(_territoryId); // CT2.3
		return true;
	}
}
