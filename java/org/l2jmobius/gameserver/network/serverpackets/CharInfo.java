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
import org.l2jmobius.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jmobius.gameserver.model.actor.Decoy;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skills.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class CharInfo implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private final Clan _clan;
	private int _objId;
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private final int _mAtkSpd;
	private final int _pAtkSpd;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	
	private int _vehicleId = 0;
	private final boolean _gmSeeInvis;
	
	private static final int[] PAPERDOLL_ORDER = new int[]
	{
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_CLOAK,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2,
		Inventory.PAPERDOLL_RBRACELET,
		Inventory.PAPERDOLL_LBRACELET,
		Inventory.PAPERDOLL_DECO1,
		Inventory.PAPERDOLL_DECO2,
		Inventory.PAPERDOLL_DECO3,
		Inventory.PAPERDOLL_DECO4,
		Inventory.PAPERDOLL_DECO5,
		Inventory.PAPERDOLL_DECO6,
		Inventory.PAPERDOLL_BELT
	};
	
	public CharInfo(PlayerInstance player, boolean gmSeeInvis)
	{
		_player = player;
		_objId = player.getObjectId();
		_clan = player.getClan();
		if ((_player.getVehicle() != null) && (_player.getInVehiclePosition() != null))
		{
			_x = _player.getInVehiclePosition().getX();
			_y = _player.getInVehiclePosition().getY();
			_z = _player.getInVehiclePosition().getZ();
			_vehicleId = _player.getVehicle().getObjectId();
		}
		else
		{
			_x = _player.getX();
			_y = _player.getY();
			_z = _player.getZ();
		}
		_heading = _player.getHeading();
		_mAtkSpd = _player.getMAtkSpd();
		_pAtkSpd = (int) _player.getPAtkSpd();
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(player.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(player.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(player.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(player.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = player.isFlying() ? _runSpd : 0;
		_flyWalkSpd = player.isFlying() ? _walkSpd : 0;
		_gmSeeInvis = gmSeeInvis;
	}
	
	public CharInfo(Decoy decoy, boolean gmSeeInvis)
	{
		this(decoy.getActingPlayer(), gmSeeInvis); // init
		_objId = decoy.getObjectId();
		_x = decoy.getX();
		_y = decoy.getY();
		_z = decoy.getZ();
		_heading = decoy.getHeading();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.CHAR_INFO.writeId(packet);
		packet.writeD(_x);
		packet.writeD(_y);
		packet.writeD(_z);
		packet.writeD(_vehicleId);
		packet.writeD(_objId);
		packet.writeS(_player.getAppearance().getVisibleName());
		packet.writeD(_player.getRace().ordinal());
		packet.writeD(_player.getAppearance().isFemale() ? 1 : 0);
		packet.writeD(_player.getBaseClass());
		
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
		
		packet.writeD(_player.getPvpFlag());
		packet.writeD(_player.getKarma());
		
		packet.writeD(_mAtkSpd);
		packet.writeD(_pAtkSpd);
		
		packet.writeD(0x00); // ?
		
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
		
		packet.writeS(_gmSeeInvis ? "Invisible" : _player.getAppearance().getVisibleTitle());
		
		if (!_player.isCursedWeaponEquipped())
		{
			packet.writeD(_player.getClanId());
			packet.writeD(_player.getClanCrestId());
			packet.writeD(_player.getAllyId());
			packet.writeD(_player.getAllyCrestId());
		}
		else
		{
			packet.writeD(0x00);
			packet.writeD(0x00);
			packet.writeD(0x00);
			packet.writeD(0x00);
		}
		
		packet.writeC(_player.isSitting() ? 0 : 1); // standing = 1 sitting = 0
		packet.writeC(_player.isRunning() ? 1 : 0); // running = 1 walking = 0
		packet.writeC(_player.isInCombat() ? 1 : 0);
		
		packet.writeC(!_player.isInOlympiadMode() && _player.isAlikeDead() ? 1 : 0);
		
		packet.writeC(!_gmSeeInvis && _player.isInvisible() ? 1 : 0); // invisible = 1 visible =0
		
		packet.writeC(_player.getMountType().ordinal()); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
		packet.writeC(_player.getPrivateStoreType().getId());
		
		packet.writeH(_player.getCubics().size());
		for (int cubicId : _player.getCubics().keySet())
		{
			packet.writeH(cubicId);
		}
		
		packet.writeC(_player.isInPartyMatchRoom() ? 1 : 0);
		
		packet.writeD(_gmSeeInvis ? (_player.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask()) : _player.getAbnormalVisualEffects());
		
		packet.writeC(_player.isInsideZone(ZoneId.WATER) ? 1 : _player.isFlyingMounted() ? 2 : 0);
		
		packet.writeH(_player.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
		packet.writeD(_player.getMountNpcId() + 1000000);
		packet.writeD(_player.getClassId().getId());
		packet.writeD(0x00); // ?
		packet.writeC(_player.isMounted() ? 0 : _player.getEnchantEffect());
		
		packet.writeC(_player.getTeam().getId());
		
		packet.writeD(_player.getClanCrestLargeId());
		packet.writeC(_player.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
		packet.writeC(_player.isHero() || (_player.isGM() && Config.GM_HERO_AURA) ? 1 : 0); // Hero Aura
		
		packet.writeC(_player.isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
		packet.writeD(_player.getFishx());
		packet.writeD(_player.getFishy());
		packet.writeD(_player.getFishz());
		
		packet.writeD(_player.getAppearance().getNameColor());
		
		packet.writeD(_heading);
		
		packet.writeD(_player.getPledgeClass());
		packet.writeD(_player.getPledgeType());
		
		packet.writeD(_player.getAppearance().getTitleColor());
		
		packet.writeD(_player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId()) : 0);
		
		packet.writeD(_clan != null ? _clan.getReputationScore() : 0);
		
		// T1
		packet.writeD(_player.getTransformationDisplayId());
		packet.writeD(_player.getAgathionId());
		
		// T2
		packet.writeD(0x01);
		
		// T2.3
		packet.writeD(_player.getAbnormalVisualEffectSpecial());
		return true;
	}
	
	@Override
	public int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
}
