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
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.instancemanager.TownManager;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.TrapInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.skills.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.type.TownZone;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public abstract class AbstractNpcInfo implements IClientOutgoingPacket
{
	protected int _x;
	protected int _y;
	protected int _z;
	protected int _heading;
	protected int _displayId;
	protected boolean _isAttackable;
	protected boolean _isSummoned;
	protected int _mAtkSpd;
	protected int _pAtkSpd;
	protected final int _runSpd;
	protected final int _walkSpd;
	protected final int _swimRunSpd;
	protected final int _swimWalkSpd;
	protected final int _flyRunSpd;
	protected final int _flyWalkSpd;
	protected double _moveMultiplier;
	
	protected int _rhand;
	protected int _lhand;
	protected int _chest;
	protected int _enchantEffect;
	protected float _collisionHeight;
	protected float _collisionRadius;
	protected String _name = "";
	protected String _title = "";
	protected final boolean _gmSeeInvis;
	
	public AbstractNpcInfo(Creature creature, boolean gmSeeInvis)
	{
		_isSummoned = creature.isShowSummonAnimation();
		_x = creature.getX();
		_y = creature.getY();
		_z = creature.getZ();
		_heading = creature.getHeading();
		_mAtkSpd = creature.getMAtkSpd();
		_pAtkSpd = (int) creature.getPAtkSpd();
		_moveMultiplier = creature.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(creature.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(creature.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(creature.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(creature.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = creature.isFlying() ? _runSpd : 0;
		_flyWalkSpd = creature.isFlying() ? _walkSpd : 0;
		_gmSeeInvis = gmSeeInvis;
	}
	
	/**
	 * Packet for Npcs
	 */
	public static class NpcInfo extends AbstractNpcInfo
	{
		private final Npc _npc;
		private int _clanCrest = 0;
		private int _allyCrest = 0;
		private int _allyId = 0;
		private int _clanId = 0;
		private int _displayEffect = 0;
		private String[] _localisation;
		
		public void setLang(String lang)
		{
			_localisation = NpcNameLocalisationData.getInstance().getLocalisation(lang, _npc.getId());
			if (_localisation != null)
			{
				_name = _localisation[0];
				_title = _localisation[1];
			}
		}
		
		public NpcInfo(Npc cha, Creature attacker)
		{
			super(cha, attacker.canOverrideCond(PlayerCondOverride.SEE_ALL_PLAYERS));
			_npc = cha;
			_displayId = cha.getTemplate().getDisplayId(); // On every subclass
			_rhand = cha.getRightHandItem(); // On every subclass
			_lhand = cha.getLeftHandItem(); // On every subclass
			_enchantEffect = cha.getEnchantEffect();
			_collisionHeight = cha.getTemplate().getFCollisionHeight(); // On every subclass
			_collisionRadius = cha.getTemplate().getFCollisionRadius(); // On every subclass
			_isAttackable = cha.isAutoAttackable(attacker);
			
			// npc crest of owning clan/ally of castle
			if (cha.isNpc() && cha.isInsideZone(ZoneId.TOWN) && (Config.SHOW_CREST_WITHOUT_QUEST || cha.getCastle().getShowNpcCrest()) && (cha.getCastle().getOwnerId() != 0))
			{
				final TownZone town = TownManager.getTown(_x, _y, _z);
				if (town != null)
				{
					final int townId = town.getTownId();
					if ((townId != 33) && (townId != 22))
					{
						final Clan clan = ClanTable.getInstance().getClan(cha.getCastle().getOwnerId());
						_clanCrest = clan.getCrestId();
						_clanId = clan.getId();
						_allyCrest = clan.getAllyCrestId();
						_allyId = clan.getAllyId();
					}
				}
			}
			
			_displayEffect = cha.getDisplayEffect();
		}
		
		@Override
		public boolean write(PacketWriter packet)
		{
			OutgoingPackets.NPC_INFO.writeId(packet);
			packet.writeD(_npc.getObjectId());
			packet.writeD(_displayId + 1000000); // npctype id
			packet.writeD(_isAttackable ? 1 : 0);
			packet.writeD(_x);
			packet.writeD(_y);
			packet.writeD(_z);
			packet.writeD(_heading);
			packet.writeD(0x00);
			packet.writeD(_mAtkSpd);
			packet.writeD(_pAtkSpd);
			packet.writeD(_runSpd);
			packet.writeD(_walkSpd);
			packet.writeD(_swimRunSpd);
			packet.writeD(_swimWalkSpd);
			packet.writeD(_flyRunSpd);
			packet.writeD(_flyWalkSpd);
			packet.writeD(_flyRunSpd);
			packet.writeD(_flyWalkSpd);
			packet.writeF(_moveMultiplier);
			packet.writeF(_npc.getAttackSpeedMultiplier());
			packet.writeF(_collisionRadius);
			packet.writeF(_collisionHeight);
			packet.writeD(_rhand); // right hand weapon
			packet.writeD(_chest);
			packet.writeD(_lhand); // left hand weapon
			packet.writeC(1); // name above char 1=true ... ??
			packet.writeC(_npc.isRunning() ? 1 : 0);
			packet.writeC(_npc.isInCombat() ? 1 : 0);
			packet.writeC(_npc.isAlikeDead() ? 1 : 0);
			packet.writeC(_isSummoned ? 2 : 0); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
			packet.writeD(-1); // High Five NPCString ID
			if ((_localisation == null) && _npc.getTemplate().isUsingServerSideName())
			{
				_name = _npc.getName(); // On every subclass
			}
			packet.writeS(_name);
			packet.writeD(-1); // High Five NPCString ID
			if (_npc.isInvisible())
			{
				_title = "Invisible";
			}
			else if (_localisation == null)
			{
				if (_npc.getTemplate().isUsingServerSideTitle())
				{
					_title = _npc.getTemplate().getTitle(); // On every subclass
				}
				else
				{
					_title = _npc.getTitle(); // On every subclass
				}
			}
			// Custom level titles
			if (_npc.isMonster() && (Config.SHOW_NPC_LEVEL || Config.SHOW_NPC_AGGRESSION))
			{
				String t1 = "";
				if (Config.SHOW_NPC_LEVEL)
				{
					t1 += "Lv " + _npc.getLevel();
				}
				String t2 = "";
				if (Config.SHOW_NPC_AGGRESSION)
				{
					if (!t1.isEmpty())
					{
						t2 += " ";
					}
					final MonsterInstance monster = (MonsterInstance) _npc;
					if (monster.isAggressive())
					{
						t2 += "[A]"; // Aggressive.
					}
					if ((monster.getTemplate().getClans() != null) && (monster.getTemplate().getClanHelpRange() > 0))
					{
						t2 += "[G]"; // Group.
					}
				}
				t1 += t2;
				if ((_title != null) && !_title.isEmpty())
				{
					t1 += " " + _title;
				}
				_title = _npc.isChampion() ? Config.CHAMP_TITLE + " " + t1 : t1;
			}
			else if (Config.CHAMPION_ENABLE && _npc.isChampion())
			{
				_title = (Config.CHAMP_TITLE); // On every subclass
			}
			packet.writeS(_title);
			packet.writeD(0x00); // Title color 0=client default
			packet.writeD(0x00); // pvp flag
			packet.writeD(0x00); // karma
			
			packet.writeD(_npc.isInvisible() ? _npc.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : _npc.getAbnormalVisualEffects());
			packet.writeD(_clanId); // clan id
			packet.writeD(_clanCrest); // crest id
			packet.writeD(_allyId); // ally id
			packet.writeD(_allyCrest); // all crest
			
			packet.writeC(_npc.isInsideZone(ZoneId.WATER) ? 1 : _npc.isFlying() ? 2 : 0); // C2
			packet.writeC(_npc.getTeam().getId());
			
			packet.writeF(_collisionRadius);
			packet.writeF(_collisionHeight);
			packet.writeD(_enchantEffect); // C4
			packet.writeD(_npc.isFlying() ? 1 : 0); // C6
			packet.writeD(0x00);
			packet.writeD(_npc.getColorEffect()); // CT1.5 Pet form and skills, Color effect
			packet.writeC(_npc.isTargetable() ? 0x01 : 0x00);
			packet.writeC(_npc.isShowName() ? 0x01 : 0x00);
			packet.writeD(_npc.getAbnormalVisualEffectSpecial());
			packet.writeD(_displayEffect);
			
			return true;
		}
	}
	
	public static class TrapInfo extends AbstractNpcInfo
	{
		private final TrapInstance _trap;
		
		public TrapInfo(TrapInstance cha, Creature attacker)
		{
			super(cha, (attacker != null) && attacker.canOverrideCond(PlayerCondOverride.SEE_ALL_PLAYERS));
			
			_trap = cha;
			_displayId = cha.getTemplate().getDisplayId();
			_isAttackable = cha.isAutoAttackable(attacker);
			_rhand = 0;
			_lhand = 0;
			_collisionHeight = _trap.getTemplate().getFCollisionHeight();
			_collisionRadius = _trap.getTemplate().getFCollisionRadius();
			if (cha.getTemplate().isUsingServerSideName())
			{
				_name = cha.getName();
			}
			_title = cha.getOwner() != null ? cha.getOwner().getName() : "";
		}
		
		@Override
		public boolean write(PacketWriter packet)
		{
			OutgoingPackets.NPC_INFO.writeId(packet);
			packet.writeD(_trap.getObjectId());
			packet.writeD(_displayId + 1000000); // npctype id
			packet.writeD(_isAttackable ? 1 : 0);
			packet.writeD(_x);
			packet.writeD(_y);
			packet.writeD(_z);
			packet.writeD(_heading);
			packet.writeD(0x00);
			packet.writeD(_mAtkSpd);
			packet.writeD(_pAtkSpd);
			packet.writeD(_runSpd);
			packet.writeD(_walkSpd);
			packet.writeD(_swimRunSpd);
			packet.writeD(_swimWalkSpd);
			packet.writeD(_flyRunSpd);
			packet.writeD(_flyWalkSpd);
			packet.writeD(_flyRunSpd);
			packet.writeD(_flyWalkSpd);
			packet.writeF(_moveMultiplier);
			packet.writeF(_trap.getAttackSpeedMultiplier());
			packet.writeF(_collisionRadius);
			packet.writeF(_collisionHeight);
			packet.writeD(_rhand); // right hand weapon
			packet.writeD(_chest);
			packet.writeD(_lhand); // left hand weapon
			packet.writeC(1); // name above char 1=true ... ??
			packet.writeC(1);
			packet.writeC(_trap.isInCombat() ? 1 : 0);
			packet.writeC(_trap.isAlikeDead() ? 1 : 0);
			packet.writeC(_isSummoned ? 2 : 0); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
			packet.writeD(-1); // High Five NPCString ID
			packet.writeS(_name);
			packet.writeD(-1); // High Five NPCString ID
			packet.writeS(_title);
			packet.writeD(0x00); // title color 0 = client default
			
			packet.writeD(_trap.getPvpFlag());
			packet.writeD(_trap.getKarma());
			
			packet.writeD(_trap.isInvisible() ? _trap.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : _trap.getAbnormalVisualEffects());
			packet.writeD(0x00); // clan id
			packet.writeD(0x00); // crest id
			packet.writeD(0000); // C2
			packet.writeD(0000); // C2
			packet.writeC(0000); // C2
			
			packet.writeC(_trap.getTeam().getId());
			
			packet.writeF(_collisionRadius);
			packet.writeF(_collisionHeight);
			packet.writeD(0x00); // C4
			packet.writeD(0x00); // C6
			packet.writeD(0x00);
			packet.writeD(0); // CT1.5 Pet form and skills
			packet.writeC(0x01);
			packet.writeC(0x01);
			packet.writeD(0x00);
			
			return true;
		}
	}
	
	/**
	 * Packet for summons.
	 */
	public static class SummonInfo extends AbstractNpcInfo
	{
		private final Summon _summon;
		private final int _form;
		private final int _value;
		
		public SummonInfo(Summon cha, Creature attacker, int value)
		{
			super(cha, attacker.canOverrideCond(PlayerCondOverride.SEE_ALL_PLAYERS));
			_summon = cha;
			_value = value;
			_form = cha.getFormId();
			_isAttackable = cha.isAutoAttackable(attacker);
			_rhand = cha.getWeapon();
			_lhand = 0;
			_chest = cha.getArmor();
			_enchantEffect = cha.getTemplate().getWeaponEnchant();
			_name = cha.getName();
			_title = (cha.getOwner() != null) && cha.getOwner().isOnline() ? cha.getOwner().getName() : "";
			_displayId = cha.getTemplate().getDisplayId();
			_collisionHeight = cha.getTemplate().getFCollisionHeight();
			_collisionRadius = cha.getTemplate().getFCollisionRadius();
		}
		
		@Override
		public boolean write(PacketWriter packet)
		{
			OutgoingPackets.NPC_INFO.writeId(packet);
			packet.writeD(_summon.getObjectId());
			packet.writeD(_displayId + 1000000); // npctype id
			packet.writeD(_isAttackable ? 1 : 0);
			packet.writeD(_x);
			packet.writeD(_y);
			packet.writeD(_z);
			packet.writeD(_heading);
			packet.writeD(0x00);
			packet.writeD(_mAtkSpd);
			packet.writeD(_pAtkSpd);
			packet.writeD(_runSpd);
			packet.writeD(_walkSpd);
			packet.writeD(_swimRunSpd);
			packet.writeD(_swimWalkSpd);
			packet.writeD(_flyRunSpd);
			packet.writeD(_flyWalkSpd);
			packet.writeD(_flyRunSpd);
			packet.writeD(_flyWalkSpd);
			packet.writeF(_moveMultiplier);
			packet.writeF(_summon.getAttackSpeedMultiplier());
			packet.writeF(_collisionRadius);
			packet.writeF(_collisionHeight);
			packet.writeD(_rhand); // right hand weapon
			packet.writeD(_chest);
			packet.writeD(_lhand); // left hand weapon
			packet.writeC(0x01); // name above char 1=true ... ??
			packet.writeC(0x01); // always running 1=running 0=walking
			packet.writeC(_summon.isInCombat() ? 1 : 0);
			packet.writeC(_summon.isAlikeDead() ? 1 : 0);
			packet.writeC(_isSummoned ? 2 : _value); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
			packet.writeD(-1); // High Five NPCString ID
			packet.writeS(_name);
			packet.writeD(-1); // High Five NPCString ID
			packet.writeS(_title);
			packet.writeD(0x01); // Title color 0=client default
			
			packet.writeD(_summon.getPvpFlag());
			packet.writeD(_summon.getKarma());
			
			packet.writeD(_gmSeeInvis && _summon.isInvisible() ? _summon.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : _summon.getAbnormalVisualEffects());
			
			packet.writeD(0x00); // clan id
			packet.writeD(0x00); // crest id
			packet.writeD(0x00); // C2
			packet.writeD(0x00); // C2
			packet.writeC(_summon.isInsideZone(ZoneId.WATER) ? 1 : _summon.isFlying() ? 2 : 0); // C2
			
			packet.writeC(_summon.getTeam().getId());
			
			packet.writeF(_collisionRadius);
			packet.writeF(_collisionHeight);
			packet.writeD(_enchantEffect); // C4
			packet.writeD(0x00); // C6
			packet.writeD(0x00);
			packet.writeD(_form); // CT1.5 Pet form and skills
			packet.writeC(0x01);
			packet.writeC(0x01);
			packet.writeD(_summon.getAbnormalVisualEffectSpecial());
			
			return true;
		}
	}
}
