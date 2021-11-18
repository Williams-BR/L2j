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
package org.l2jmobius.gameserver.enums;

public enum InstanceType
{
	WorldObject(null),
	ItemInstance(WorldObject),
	Creature(WorldObject),
	Npc(Creature),
	Playable(Creature),
	Summon(Playable),
	Decoy(Creature),
	PlayerInstance(Playable),
	NpcInstance(Npc),
	MerchantInstance(NpcInstance),
	WarehouseInstance(NpcInstance),
	StaticObjectInstance(Creature),
	DoorInstance(Creature),
	TerrainObjectInstance(Npc),
	EffectPointInstance(Npc),
	// Summons, Pets, Decoys and Traps
	ServitorInstance(Summon),
	PetInstance(Summon),
	BabyPetInstance(PetInstance),
	DecoyInstance(Decoy),
	TrapInstance(Npc),
	// Attackable
	Attackable(Npc),
	GuardInstance(Attackable),
	QuestGuardInstance(GuardInstance),
	MonsterInstance(Attackable),
	ChestInstance(MonsterInstance),
	ControllableMobInstance(MonsterInstance),
	FeedableBeastInstance(MonsterInstance),
	TamedBeastInstance(FeedableBeastInstance),
	FriendlyMobInstance(Attackable),
	RiftInvaderInstance(MonsterInstance),
	RaidBossInstance(MonsterInstance),
	GrandBossInstance(RaidBossInstance),
	// FlyMobs
	FlyNpcInstance(NpcInstance),
	FlyMonsterInstance(MonsterInstance),
	FlyRaidBossInstance(RaidBossInstance),
	FlyTerrainObjectInstance(Npc),
	// Sepulchers
	SepulcherNpcInstance(NpcInstance),
	SepulcherMonsterInstance(MonsterInstance),
	// Festival
	FestivalGiudeInstance(Npc),
	FestivalMonsterInstance(MonsterInstance),
	// Vehicles
	Vehicle(Creature),
	BoatInstance(Vehicle),
	AirShipInstance(Vehicle),
	ControllableAirShipInstance(AirShipInstance),
	// Siege
	DefenderInstance(Attackable),
	ArtefactInstance(NpcInstance),
	ControlTowerInstance(Npc),
	FlameTowerInstance(Npc),
	SiegeFlagInstance(Npc),
	// Fort Siege
	FortCommanderInstance(DefenderInstance),
	// Fort NPCs
	FortLogisticsInstance(MerchantInstance),
	FortManagerInstance(MerchantInstance),
	// Seven Signs
	SignsPriestInstance(Npc),
	DawnPriestInstance(SignsPriestInstance),
	DuskPriestInstance(SignsPriestInstance),
	DungeonGatekeeperInstance(Npc),
	// City NPCs
	AdventurerInstance(NpcInstance),
	AuctioneerInstance(Npc),
	ClanHallManagerInstance(MerchantInstance),
	FishermanInstance(MerchantInstance),
	ObservationInstance(Npc),
	OlympiadManagerInstance(Npc),
	PetManagerInstance(MerchantInstance),
	RaceManagerInstance(Npc),
	TeleporterInstance(Npc),
	TrainerInstance(NpcInstance),
	VillageMasterInstance(NpcInstance),
	// Doormen
	DoormanInstance(NpcInstance),
	CastleDoormanInstance(DoormanInstance),
	FortDoormanInstance(DoormanInstance),
	ClanHallDoormanInstance(DoormanInstance),
	// Custom
	ClassMasterInstance(NpcInstance),
	SchemeBufferInstance(Npc),
	EventMobInstance(Npc);
	
	private final InstanceType _parent;
	private final long _typeL;
	private final long _typeH;
	private final long _maskL;
	private final long _maskH;
	
	private InstanceType(InstanceType parent)
	{
		_parent = parent;
		
		final int high = ordinal() - (Long.SIZE - 1);
		if (high < 0)
		{
			_typeL = 1L << ordinal();
			_typeH = 0;
		}
		else
		{
			_typeL = 0;
			_typeH = 1L << high;
		}
		
		if ((_typeL < 0) || (_typeH < 0))
		{
			throw new Error("Too many instance types, failed to load " + name());
		}
		
		if (parent != null)
		{
			_maskL = _typeL | parent._maskL;
			_maskH = _typeH | parent._maskH;
		}
		else
		{
			_maskL = _typeL;
			_maskH = _typeH;
		}
	}
	
	public InstanceType getParent()
	{
		return _parent;
	}
	
	public boolean isType(InstanceType it)
	{
		return ((_maskL & it._typeL) > 0) || ((_maskH & it._typeH) > 0);
	}
	
	public boolean isTypes(InstanceType... it)
	{
		for (InstanceType i : it)
		{
			if (isType(i))
			{
				return true;
			}
		}
		return false;
	}
}
