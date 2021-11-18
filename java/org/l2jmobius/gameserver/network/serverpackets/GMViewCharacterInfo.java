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

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class GMViewCharacterInfo implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	
	public GMViewCharacterInfo(PlayerInstance player)
	{
		_player = player;
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
		OutgoingPackets.GM_VIEW_CHARACTER_INFO.writeId(packet);
		
		packet.writeD(_player.getX());
		packet.writeD(_player.getY());
		packet.writeD(_player.getZ());
		packet.writeD(_player.getHeading());
		packet.writeD(_player.getObjectId());
		packet.writeS(_player.getName());
		packet.writeD(_player.getRace().ordinal());
		packet.writeD(_player.getAppearance().isFemale() ? 1 : 0);
		packet.writeD(_player.getClassId().getId());
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
		packet.writeD((int) _player.getCurrentHp());
		packet.writeD(_player.getMaxMp());
		packet.writeD((int) _player.getCurrentMp());
		packet.writeD((int) _player.getSp());
		packet.writeD(_player.getCurrentLoad());
		packet.writeD(_player.getMaxLoad());
		packet.writeD(_player.getPkKills());
		
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
		
		packet.writeD(_player.getInventory().getTalismanSlots()); // CT2.3
		packet.writeD(_player.getInventory().canEquipCloak() ? 1 : 0); // CT2.3
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
		
		packet.writeD(_player.getPvpFlag()); // 0-non-pvp 1-pvp = violett name
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
		packet.writeF(_player.getAttackSpeedMultiplier()); // 2.9); //
		packet.writeF(_player.getCollisionRadius()); // scale
		packet.writeF(_player.getCollisionHeight()); // y offset ??!? fem dwarf 4033
		packet.writeD(_player.getAppearance().getHairStyle());
		packet.writeD(_player.getAppearance().getHairColor());
		packet.writeD(_player.getAppearance().getFace());
		packet.writeD(_player.isGM() ? 0x01 : 0x00); // builder level
		
		packet.writeS(_player.getTitle());
		packet.writeD(_player.getClanId()); // pledge id
		packet.writeD(_player.getClanCrestId()); // pledge crest id
		packet.writeD(_player.getAllyId()); // ally id
		packet.writeC(_player.getMountType().ordinal()); // mount type
		packet.writeC(_player.getPrivateStoreType().getId());
		packet.writeC(_player.hasDwarvenCraft() ? 1 : 0);
		packet.writeD(_player.getPkKills());
		packet.writeD(_player.getPvpKills());
		
		packet.writeH(_player.getRecomLeft());
		packet.writeH(_player.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
		packet.writeD(_player.getClassId().getId());
		packet.writeD(0x00); // special effects? circles around player...
		packet.writeD(_player.getMaxCp());
		packet.writeD((int) _player.getCurrentCp());
		
		packet.writeC(_player.isRunning() ? 0x01 : 0x00); // changes the Speed display on Status Window
		
		packet.writeC(321);
		
		packet.writeD(_player.getPledgeClass()); // changes the text above CP on Status Window
		
		packet.writeC(_player.isNoble() ? 0x01 : 0x00);
		packet.writeC(_player.isHero() ? 0x01 : 0x00);
		
		packet.writeD(_player.getAppearance().getNameColor());
		packet.writeD(_player.getAppearance().getTitleColor());
		
		final byte attackAttribute = _player.getAttackElement();
		packet.writeH(attackAttribute);
		packet.writeH(_player.getAttackElementValue(attackAttribute));
		for (byte i = 0; i < 6; i++)
		{
			packet.writeH(_player.getDefenseElementValue(i));
		}
		packet.writeD(_player.getFame());
		packet.writeD(_player.getVitalityPoints());
		return true;
	}
}
