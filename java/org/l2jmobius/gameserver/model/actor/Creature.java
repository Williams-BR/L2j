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
package org.l2jmobius.gameserver.model.actor;

import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.commons.util.EmptyQueue;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.CreatureAI.IntentionCommand;
import org.l2jmobius.gameserver.ai.CtrlEvent;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.enums.CategoryType;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.enums.ShotType;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.enums.Team;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.instancemanager.IdManager;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.instancemanager.MapRegionManager;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.AccessLevel;
import org.l2jmobius.gameserver.model.EffectList;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.TimeStamp;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.QuestGuardInstance;
import org.l2jmobius.gameserver.model.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.model.actor.status.CreatureStatus;
import org.l2jmobius.gameserver.model.actor.tasks.creature.HitTask;
import org.l2jmobius.gameserver.model.actor.tasks.creature.MagicUseTask;
import org.l2jmobius.gameserver.model.actor.tasks.creature.NotifyAITask;
import org.l2jmobius.gameserver.model.actor.tasks.creature.QueuedMagicUseTask;
import org.l2jmobius.gameserver.model.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.actor.transform.Transform;
import org.l2jmobius.gameserver.model.actor.transform.TransformTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureAttackAvoid;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureDamageDealt;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureKill;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureSee;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureTeleported;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.holders.InvulSkillHolder;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.holders.SkillUseHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.IDeletable;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.interfaces.ISkillsHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.WeaponType;
import org.l2jmobius.gameserver.model.options.OptionsSkillHolder;
import org.l2jmobius.gameserver.model.options.OptionsSkillType;
import org.l2jmobius.gameserver.model.skills.AbnormalType;
import org.l2jmobius.gameserver.model.skills.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skills.BuffFinishTask;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.skills.CommonSkill;
import org.l2jmobius.gameserver.model.skills.EffectScope;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.SkillChannelized;
import org.l2jmobius.gameserver.model.skills.SkillChannelizer;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.stats.BaseStat;
import org.l2jmobius.gameserver.model.stats.Calculator;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneRegion;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.Attack;
import org.l2jmobius.gameserver.network.serverpackets.ChangeMoveType;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.ExRotation;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillCanceled;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.MoveToLocation;
import org.l2jmobius.gameserver.network.serverpackets.Revive;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TeleportToLocation;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanager.CreatureSeeTaskManager;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Util;

/**
 * Mother class of all character objects of the world (PC, NPC...)<br>
 * Creature:<br>
 * <ul>
 * <li>DoorInstance</li>
 * <li>Playable</li>
 * <li>Npc</li>
 * <li>StaticObjectInstance</li>
 * <li>Trap</li>
 * <li>Vehicle</li>
 * </ul>
 * <b>Concept of CreatureTemplate:</b><br>
 * Each Creature owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
 * All of those properties are stored in a different template for each type of Creature.<br>
 * Each template is loaded once in the server cache memory (reduce memory use).<br>
 * When a new instance of Creature is spawned, server just create a link between the instance and the template.<br>
 * This link is stored in {@link #_template}
 * @version $Revision: 1.53.2.45.2.34 $ $Date: 2005/04/11 10:06:08 $
 */
public abstract class Creature extends WorldObject implements ISkillsHolder, IDeletable
{
	public static final Logger LOGGER = Logger.getLogger(Creature.class.getName());
	
	private Set<Creature> _attackByList;
	private volatile boolean _isCastingNow = false;
	private volatile boolean _isCastingSimultaneouslyNow = false;
	private Skill _lastSkillCast;
	private Skill _lastSimultaneousSkillCast;
	
	private boolean _isDead = false;
	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false; // the char is carrying too much
	private boolean _isParalyzed = false;
	private boolean _isPendingRevive = false;
	private boolean _isRunning = false;
	protected boolean _showSummonAnimation = false;
	protected boolean _isTeleporting = false;
	private boolean _isInvul = false;
	private boolean _isMortal = true; // Char will die when HP decreased to 0
	private boolean _isFlying = false;
	
	private CreatureStat _stat;
	private CreatureStatus _status;
	private CreatureTemplate _template; // The link on the CreatureTemplate object containing generic and static properties of this Creature type (ex : Max HP, Speed...)
	private String _title;
	
	public static final double MAX_HP_BAR_PX = 352.0;
	
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	
	private int _karma = 0;
	
	/** Table of Calculators containing all used calculator */
	private Calculator[] _calculators;
	/** Map containing all skills of this character. */
	private final Map<Integer, Skill> _skills = new ConcurrentHashMap<>();
	/** Map containing the skill reuse time stamps. */
	private final Map<Integer, TimeStamp> _reuseTimeStampsSkills = new ConcurrentHashMap<>();
	/** Map containing the item reuse time stamps. */
	private final Map<Integer, TimeStamp> _reuseTimeStampsItems = new ConcurrentHashMap<>();
	/** Map containing all the disabled skills. */
	private final Map<Integer, Long> _disabledSkills = new ConcurrentHashMap<>();
	private boolean _allSkillsDisabled;
	
	private final byte[] _zones = new byte[ZoneId.getZoneCount()];
	protected final Location _lastZoneValidateLocation = new Location(getX(), getY(), getZ());
	
	private final StampedLock _attackLock = new StampedLock();
	
	private Team _team = Team.NONE;
	
	protected long _exceptions = 0;
	
	private boolean _lethalable = true;
	
	private Map<Integer, OptionsSkillHolder> _triggerSkills;
	
	private Map<Integer, InvulSkillHolder> _invulAgainst;
	/** Creatures effect list. */
	private final EffectList _effectList = new EffectList(this);
	/** The creature that summons this character. */
	private Creature _summoner = null;
	
	private SkillChannelizer _channelizer = null;
	
	private SkillChannelized _channelized = null;
	
	private BuffFinishTask _buffFinishTask = null;
	
	/** Map 32 bits, containing all abnormal visual effects in progress. */
	private int _abnormalVisualEffects;
	/** Map 32 bits, containing all special abnormal visual effects in progress. */
	private int _abnormalVisualEffectsSpecial;
	/** Map 32 bits, containing all event abnormal visual effects in progress. */
	private int _abnormalVisualEffectsEvent;
	
	/** Movement data of this Creature */
	protected MoveData _move;
	private boolean _cursorKeyMovement = false;
	
	/** This creature's target. */
	private WorldObject _target;
	/** Represents the time where the attack should end, in nanoseconds. */
	private volatile long _attackEndTime;
	private int _disableBowAttackEndTime;
	
	private int _castInterruptTime;
	
	/** Table of calculators containing all standard NPC calculator (ex : ACCURACY_COMBAT, EVASION_RATE) */
	private static final Calculator[] NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
	
	private CreatureAI _ai = null;
	
	/** Future Skill Cast */
	protected Future<?> _skillCast;
	protected Future<?> _skillCast2;
	
	private final Map<Integer, RelationCache> _knownRelations = new ConcurrentHashMap<>();
	
	private Set<Creature> _seenCreatures = null;
	private int _seenCreatureRange = Config.ALT_PARTY_RANGE;
	
	/** A list containing the dropped items of this fake player. */
	private final List<ItemInstance> _fakePlayerDrops = new CopyOnWriteArrayList<>();
	
	private OnCreatureAttack _onCreatureAttack = null;
	private OnCreatureAttacked _onCreatureAttacked = null;
	private OnCreatureDamageDealt _onCreatureDamageDealt = null;
	private OnCreatureDamageReceived _onCreatureDamageReceived = null;
	private OnCreatureAttackAvoid _onCreatureAttackAvoid = null;
	private OnCreatureSkillUse _onCreatureSkillUse = null;
	
	/**
	 * Creates a creature.
	 * @param template the creature template
	 */
	public Creature(CreatureTemplate template)
	{
		this(IdManager.getInstance().getNextId(), template);
	}
	
	/**
	 * Constructor of Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Each Creature owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
	 * All of those properties are stored in a different template for each type of Creature. Each template is loaded once in the server cache memory (reduce memory use).<br>
	 * When a new instance of Creature is spawned, server just create a link between the instance and the template This link is stored in <b>_template</b><br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Set the _template of the Creature</li>
	 * <li>Set _overloaded to false (the character can take more items)</li>
	 * <li>If Creature is a NPCInstance, copy skills from template to object</li>
	 * <li>If Creature is a NPCInstance, link _calculators to NPC_STD_CALCULATOR</li>
	 * <li>If Creature is NOT a NPCInstance, create an empty _skills slot</li>
	 * <li>If Creature is a PlayerInstance or Summon, copy basic Calculator set to object</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @param template The CreatureTemplate to apply to the object
	 */
	public Creature(int objectId, CreatureTemplate template)
	{
		super(objectId);
		if (template == null)
		{
			throw new NullPointerException("Template is null!");
		}
		
		setInstanceType(InstanceType.Creature);
		initCharStat();
		initCharStatus();
		
		// Set its template to the new Creature
		_template = template;
		if (isDoor())
		{
			_calculators = Formulas.getStdDoorCalculators();
		}
		else if (isNpc())
		{
			// Copy the Standard Calculators of the NPCInstance in _calculators
			_calculators = NPC_STD_CALCULATOR;
			
			// Copy the skills of the NPCInstance from its template to the Creature Instance
			// The skills list can be affected by spell effects so it's necessary to make a copy
			// to avoid that a spell affecting a NpcInstance, affects others NPCInstance of the same type too.
			for (Skill skill : template.getSkills().values())
			{
				addSkill(skill);
			}
		}
		else
		{
			// If Creature is a PlayerInstance or a Summon, create the basic calculator set
			_calculators = new Calculator[Stat.NUM_STATS];
			if (isSummon())
			{
				// Copy the skills of the Summon from its template to the Creature Instance
				// The skills list can be affected by spell effects so it's necessary to make a copy
				// to avoid that a spell affecting a Summon, affects others Summon of the same type too.
				for (Skill skill : template.getSkills().values())
				{
					addSkill(skill);
				}
			}
			
			Formulas.addFuncsToNewCharacter(this);
		}
		
		setInvul(true);
	}
	
	public EffectList getEffectList()
	{
		return _effectList;
	}
	
	/**
	 * @return character inventory, default null, overridden in Playable types and in NPcInstance
	 */
	public Inventory getInventory()
	{
		return null;
	}
	
	public boolean destroyItemByItemId(String process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		// Default: NPCs consume virtual items for their skills
		// TODO: should be logged if even happens.. should be false
		return true;
	}
	
	public boolean destroyItem(String process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		// Default: NPCs consume virtual items for their skills
		// TODO: should be logged if even happens.. should be false
		return true;
	}
	
	/**
	 * Check if the character is in the given zone Id.
	 * @param zone the zone Id to check
	 * @return {code true} if the character is in that zone
	 */
	@Override
	public boolean isInsideZone(ZoneId zone)
	{
		final Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
		switch (zone)
		{
			case PVP:
			{
				if ((instance != null) && instance.isPvP())
				{
					return true;
				}
				return (_zones[ZoneId.PVP.ordinal()] > 0) && (_zones[ZoneId.PEACE.ordinal()] == 0);
			}
			case PEACE:
			{
				if ((instance != null) && instance.isPvP())
				{
					return false;
				}
			}
		}
		return _zones[zone.ordinal()] > 0;
	}
	
	/**
	 * @param zone
	 * @param state
	 */
	public void setInsideZone(ZoneId zone, boolean state)
	{
		synchronized (_zones)
		{
			if (state)
			{
				_zones[zone.ordinal()]++;
			}
			else if (_zones[zone.ordinal()] > 0)
			{
				_zones[zone.ordinal()]--;
			}
		}
	}
	
	/**
	 * This will return true if the player is transformed,<br>
	 * but if the player is not transformed it will return false.
	 * @return transformation status
	 */
	public boolean isTransformed()
	{
		return false;
	}
	
	public Transform getTransformation()
	{
		return null;
	}
	
	/**
	 * This will untransform a player if they are an instance of PlayerInstance and if they are transformed.
	 */
	public void untransform()
	{
		// Just a place holder
	}
	
	/**
	 * This will return true if the player is GM,<br>
	 * but if the player is not GM it will return false.
	 * @return GM status
	 */
	public boolean isGM()
	{
		return false;
	}
	
	/**
	 * Overridden in PlayerInstance.
	 * @return the access level.
	 */
	public AccessLevel getAccessLevel()
	{
		return null;
	}
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateIncCheck = _stat.getMaxHp();
		_hpUpdateInterval = _hpUpdateIncCheck / MAX_HP_BAR_PX;
		_hpUpdateDecCheck = _hpUpdateIncCheck - _hpUpdateInterval;
	}
	
	/**
	 * Remove the Creature from the world when the decay task is launched.<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T REMOVE the object from _allObjects of World </b></font><br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packets to players</b></font>
	 */
	public void onDecay()
	{
		decayMe();
		final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
		if (region != null)
		{
			region.removeFromZones(this);
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		revalidateZone(true);
	}
	
	public synchronized void onTeleported()
	{
		if (!_isTeleporting)
		{
			return;
		}
		spawnMe(getX(), getY(), getZ());
		setTeleporting(false);
		EventDispatcher.getInstance().notifyEventAsync(new OnCreatureTeleported(this), this);
	}
	
	/**
	 * Add Creature instance that is attacking to the attacker list.
	 * @param creature The Creature that attacks this one
	 */
	public void addAttackerToAttackByList(Creature creature)
	{
		// DS: moved to Attackable
	}
	
	/**
	 * Send a packet to the Creature AND to all PlayerInstance in the _KnownPlayers of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * PlayerInstance in the detection area of the Creature are identified in <b>_knownPlayers</b>.<br>
	 * In order to inform other players of state modification on the Creature, server just need to go through _knownPlayers to send Server->Client Packet
	 * @param mov
	 */
	public void broadcastPacket(IClientOutgoingPacket mov)
	{
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(mov);
			}
		});
	}
	
	/**
	 * Send a packet to the Creature AND to all PlayerInstance in the radius (max knownlist radius) from the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * PlayerInstance in the detection area of the Creature are identified in <b>_knownPlayers</b>.<br>
	 * In order to inform other players of state modification on the Creature, server just need to go through _knownPlayers to send Server->Client Packet
	 * @param mov
	 * @param radiusInKnownlist
	 */
	public void broadcastPacket(IClientOutgoingPacket mov, int radiusInKnownlist)
	{
		World.getInstance().forEachVisibleObjectInRange(this, PlayerInstance.class, radiusInKnownlist, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(mov);
			}
		});
	}
	
	public void broadcastMoveToLocation()
	{
		if (isPlayable())
		{
			broadcastPacket(new MoveToLocation(this));
		}
		else
		{
			final WorldRegion region = getWorldRegion();
			if ((region != null) && region.areNeighborsActive())
			{
				broadcastPacket(new MoveToLocation(this));
			}
		}
	}
	
	public void broadcastSocialAction(int id)
	{
		if (isPlayable())
		{
			broadcastPacket(new SocialAction(getObjectId(), id));
		}
		else
		{
			final WorldRegion region = getWorldRegion();
			if ((region != null) && region.areNeighborsActive())
			{
				broadcastPacket(new SocialAction(getObjectId(), id));
			}
		}
	}
	
	/**
	 * @return true if hp update should be done, false if not
	 */
	protected boolean needHpUpdate()
	{
		final double currentHp = _status.getCurrentHp();
		final double maxHp = _stat.getMaxHp();
		if ((currentHp <= 1.0) || (maxHp < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentHp < _hpUpdateDecCheck) || (Math.abs(currentHp - _hpUpdateDecCheck) <= 1e-6) || (currentHp > _hpUpdateIncCheck) || (Math.abs(currentHp - _hpUpdateIncCheck) <= 1e-6))
		{
			if (Math.abs(currentHp - maxHp) <= 1e-6)
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send the Server->Client packet StatusUpdate with current HP and MP to all other PlayerInstance to inform.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Create the Server->Client packet StatusUpdate with current HP and MP</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all Creature called _statusListener that must be informed of HP/MP updates of this Creature</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND CP information</b></font>
	 */
	public void broadcastStatusUpdate()
	{
		if (_status.getStatusListener().isEmpty() || !needHpUpdate())
		{
			return;
		}
		
		// Create the Server->Client packet StatusUpdate with current HP
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.MAX_HP, _stat.getMaxHp());
		su.addAttribute(StatusUpdate.CUR_HP, (int) _status.getCurrentHp());
		
		// Go through the StatusListener
		// Send the Server->Client packet StatusUpdate with current HP and MP
		for (Creature temp : _status.getStatusListener())
		{
			if (temp != null)
			{
				temp.sendPacket(su);
			}
		}
	}
	
	/**
	 * @param text
	 */
	public void sendMessage(String text)
	{
		// default implementation
	}
	
	/**
	 * Teleport a Creature and its pet if necessary.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the movement of the Creature</li>
	 * <li>Set the x,y,z position of the WorldObject and if necessary modify its _worldRegion</li>
	 * <li>Send a Server->Client packet TeleportToLocationt to the Creature AND to all PlayerInstance in its _KnownPlayers</li>
	 * <li>Modify the position of the pet if necessary</li>
	 * </ul>
	 * @param xValue
	 * @param yValue
	 * @param zValue
	 * @param headingValue
	 * @param instanceId
	 * @param randomOffset
	 */
	public void teleToLocation(int xValue, int yValue, int zValue, int headingValue, int instanceId, int randomOffset)
	{
		int x = xValue;
		int y = yValue;
		int z = zValue;
		int heading = headingValue;
		
		// Prepare creature for teleport.
		if (_isPendingRevive)
		{
			doRevive();
		}
		
		// Abort any client actions, casting and remove target.
		stopMove(null);
		abortAttack();
		abortCast();
		setTarget(null);
		
		setTeleporting(true);
		
		getAI().setIntention(AI_INTENTION_ACTIVE);
		
		// Remove the object from its old location.
		decayMe();
		
		// Adjust position a bit.
		if (Config.OFFSET_ON_TELEPORT_ENABLED && (randomOffset > 0))
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		z += 5;
		
		// Send teleport packet where needed.
		broadcastPacket(new TeleportToLocation(this, x, y, z, heading));
		
		// Change instance id.
		setInstanceId(instanceId);
		
		// Set the x,y,z position of the WorldObject and if necessary modify its _worldRegion.
		setXYZ(x, y, z);
		// Also adjust heading.
		if (heading != 0)
		{
			setHeading(heading);
		}
		
		// Allow recall of the detached characters.
		if (!isPlayer() || ((getActingPlayer().getClient() != null) && getActingPlayer().getClient().isDetached()))
		{
			onTeleported();
		}
		
		revalidateZone(true);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int instanceId, boolean randomOffset)
	{
		teleToLocation(x, y, z, heading, instanceId, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int instanceId)
	{
		teleToLocation(x, y, z, heading, instanceId, 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset)
	{
		teleToLocation(x, y, z, heading, -1, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading)
	{
		teleToLocation(x, y, z, heading, -1, 0);
	}
	
	public void teleToLocation(int x, int y, int z, boolean randomOffset)
	{
		teleToLocation(x, y, z, 0, -1, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, 0, -1, 0);
	}
	
	public void teleToLocation(ILocational loc, int randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), randomOffset);
	}
	
	public void teleToLocation(ILocational loc, int instanceId, int randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), instanceId, randomOffset);
	}
	
	public void teleToLocation(ILocational loc, boolean randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(ILocational loc)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), 0);
	}
	
	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(MapRegionManager.getInstance().getTeleToLocation(this, teleportWhere), true);
	}
	
	private boolean canUseRangeWeapon()
	{
		if (isTransformed())
		{
			return true;
		}
		
		// Check for arrows and MP
		if (isPlayer())
		{
			final Weapon weaponItem = getActiveWeaponItem();
			if ((weaponItem == null) || !weaponItem.isRange())
			{
				return false;
			}
			
			// Equip arrows needed in left hand and send a Server->Client packet ItemList to the PlayerInstance then return True
			if (!checkAndEquipArrows())
			{
				// Cancel the action because the PlayerInstance have no arrow
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(weaponItem.isBow() ? SystemMessageId.YOU_HAVE_RUN_OUT_OF_ARROWS : SystemMessageId.NOT_ENOUGH_BOLTS);
				return false;
			}
			
			// Verify if the bow can be use
			if (_disableBowAttackEndTime <= GameTimeTaskManager.getInstance().getGameTicks())
			{
				// Verify if PlayerInstance owns enough MP
				int mpConsume = weaponItem.getMpConsume();
				if ((weaponItem.getReducedMpConsume() > 0) && (Rnd.get(100) < weaponItem.getReducedMpConsumeChance()))
				{
					mpConsume = weaponItem.getReducedMpConsume();
				}
				mpConsume = (int) calcStat(Stat.BOW_MP_CONSUME_RATE, mpConsume, null, null);
				if (_status.getCurrentMp() < mpConsume)
				{
					// If PlayerInstance doesn't have enough MP, stop the attack
					ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 1000);
					sendPacket(SystemMessageId.NOT_ENOUGH_MP);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				// If PlayerInstance have enough MP, the bow consumes it
				if (mpConsume > 0)
				{
					_status.reduceMp(mpConsume);
				}
				
				// Set the period of bow no re-use
				_disableBowAttackEndTime = (5 * GameTimeTaskManager.TICKS_PER_SECOND) + GameTimeTaskManager.getInstance().getGameTicks();
			}
			else
			{
				// Cancel the action because the bow can't be re-use at this moment
				ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 1000);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		else if (isNpc())
		{
			if (_disableBowAttackEndTime > GameTimeTaskManager.getInstance().getGameTicks())
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Launch a physical attack against a target (Simple, Bow, Pole or Dual).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Get the active weapon (always equipped in the right hand)</li>
	 * <li>If weapon is a bow, check for arrows, MP and bow re-use delay (if necessary, equip the PlayerInstance with arrows in left hand)</li>
	 * <li>If weapon is a bow, consume MP and set the new period of bow non re-use</li>
	 * <li>Get the Attack Speed of the Creature (delay (in milliseconds) before next attack)</li>
	 * <li>Select the type of attack to start (Simple, Bow, Pole or Dual) and verify if SoulShot are charged then start calculation</li>
	 * <li>If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack to the Creature AND to all PlayerInstance in the _KnownPlayers of the Creature</li>
	 * <li>Notify AI with EVT_READY_TO_ACT</li>
	 * </ul>
	 * @param target The Creature targeted
	 */
	public void doAttack(Creature target)
	{
		final long stamp = _attackLock.tryWriteLock();
		if (stamp == 0)
		{
			return;
		}
		try
		{
			if ((target == null) || isAttackDisabled() || !target.isTargetable())
			{
				return;
			}
			
			// Notify to scripts
			if (_onCreatureAttack == null)
			{
				_onCreatureAttack = new OnCreatureAttack();
			}
			_onCreatureAttack.setAttacker(this);
			_onCreatureAttack.setTarget(target);
			
			final TerminateReturn attackReturn = EventDispatcher.getInstance().notifyEvent(_onCreatureAttack, this, TerminateReturn.class);
			if ((attackReturn != null) && attackReturn.terminate())
			{
				getAI().setIntention(AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Notify to scripts
			if (_onCreatureAttacked == null)
			{
				_onCreatureAttacked = new OnCreatureAttacked();
			}
			_onCreatureAttacked.setAttacker(this);
			_onCreatureAttacked.setTarget(target);
			final TerminateReturn attackedReturn = EventDispatcher.getInstance().notifyEvent(_onCreatureAttacked, target, TerminateReturn.class);
			if ((attackedReturn != null) && attackedReturn.terminate())
			{
				getAI().setIntention(AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!isAlikeDead())
			{
				if ((isNpc() && target.isAlikeDead()) || !isInSurroundingRegion(target))
				{
					getAI().setIntention(AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if (isPlayer())
				{
					if (target.isDead())
					{
						getAI().setIntention(AI_INTENTION_ACTIVE);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					
					final PlayerInstance actor = getActingPlayer();
					if (actor.isTransformed() && !actor.getTransformation().canAttack())
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
			}
			
			// Check if attacker's weapon can attack
			if (getActiveWeaponItem() != null)
			{
				final Weapon wpn = getActiveWeaponItem();
				if (!wpn.isAttackWeapon() && !isGM())
				{
					if (wpn.getItemType() == WeaponType.FISHINGROD)
					{
						sendPacket(SystemMessageId.YOU_LOOK_ODDLY_AT_THE_FISHING_POLE_IN_DISBELIEF_AND_REALIZE_THAT_YOU_CAN_T_ATTACK_ANYTHING_WITH_THIS);
					}
					else
					{
						sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_PERFORM_ANY_ATTACKS);
					}
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			if (getActingPlayer() != null)
			{
				if (getActingPlayer().inObserverMode())
				{
					sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if (getActingPlayer().isSiegeFriend(target))
				{
					if (TerritoryWarManager.getInstance().isTWInProgress())
					{
						sendPacket(SystemMessageId.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
					}
					else
					{
						sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
					}
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Checking if target has moved to peace zone
				else if (target.isInsidePeaceZone(getActingPlayer()))
				{
					getAI().setIntention(AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (isInsidePeaceZone(this, target))
			{
				getAI().setIntention(AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			stopEffectsOnAction();
			
			// GeoData Los Check here (or dz > 1000)
			if (!GeoEngine.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Mobius: Do not move when attack is launched.
			if (isMoving())
			{
				stopMove(getLocation());
			}
			
			final Weapon weaponItem = getActiveWeaponItem();
			final int timeAtk = calculateTimeBetweenAttacks();
			final int timeToHit = timeAtk / 2;
			final Attack attack = new Attack(this, target, isChargedShot(ShotType.SOULSHOTS), (weaponItem != null) ? weaponItem.getCrystalTypePlus().getLevel() : 0);
			setHeading(Util.calculateHeadingFrom(this, target));
			final int reuse = calculateReuseTime(weaponItem);
			boolean hitted = false;
			switch (getAttackType())
			{
				case BOW:
				{
					if (!canUseRangeWeapon())
					{
						return;
					}
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeToHit + (reuse / 2), TimeUnit.MILLISECONDS);
					hitted = doAttackHitByBow(attack, target, timeAtk, reuse);
					break;
				}
				case CROSSBOW:
				{
					if (!canUseRangeWeapon())
					{
						return;
					}
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeToHit + (reuse / 2), TimeUnit.MILLISECONDS);
					hitted = doAttackHitByCrossBow(attack, target, timeAtk, reuse);
					break;
				}
				case POLE:
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitByPole(attack, target, timeToHit);
					break;
				}
				case FIST:
				{
					if (!isPlayer())
					{
						_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
						hitted = doAttackHitSimple(attack, target, timeToHit);
						break;
					}
					// Fallthrough.
				}
				case DUAL:
				case DUALFIST:
				case DUALDAGGER:
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitByDual(attack, target, timeToHit);
					break;
				}
				default:
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitSimple(attack, target, timeToHit);
					break;
				}
			}
			
			if (isFakePlayer() && (target.isPlayable() || target.isFakePlayer()))
			{
				final Npc npc = ((Npc) this);
				if (!npc.isScriptValue(1))
				{
					npc.setScriptValue(1); // in combat
					broadcastInfo(); // update flag status
					QuestManager.getInstance().getQuest("PvpFlaggingStopTask").notifyEvent("FLAG_CHECK", npc, null);
				}
			}
			
			// Flag the attacker if it's a PlayerInstance outside a PvP area
			final PlayerInstance player = getActingPlayer();
			if ((player != null) && !player.isInsideZone(ZoneId.PVP) && (player != target)) // Prevent players from flagging in PvP Zones.
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
				if (player.getSummon() != target)
				{
					player.updatePvPStatus(target);
				}
			}
			
			// Check if hit isn't missed
			if (!hitted)
			{
				abortAttack(); // Abort the attack of the Creature and send Server->Client ActionFailed packet
			}
			else
			{
				// If we didn't miss the hit, discharge the shoulshots, if any
				setChargedShot(ShotType.SOULSHOTS, false);
				if (player != null)
				{
					if (player.isCursedWeaponEquipped())
					{
						// If hit by a cursed weapon, CP is reduced to 0
						if (!target.isInvul())
						{
							target.setCurrentCp(0);
						}
					}
					// If a cursed weapon is hit by a Hero, CP is reduced to 0
					else if (player.isHero() && target.isPlayer() && target.getActingPlayer().isCursedWeaponEquipped())
					{
						target.setCurrentCp(0);
					}
				}
			}
			
			// If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
			// to the Creature AND to all PlayerInstance in the _KnownPlayers of the Creature
			if (attack.hasHits())
			{
				broadcastPacket(attack);
			}
			
			// Notify AI with EVT_READY_TO_ACT
			ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), timeAtk + reuse);
		}
		finally
		{
			_attackLock.unlockWrite(stamp);
		}
	}
	
	/**
	 * Launch a Bow attack.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Calculate if hit is missed or not</li>
	 * <li>Consume arrows</li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient</li>
	 * <li>If hit isn't missed, calculate if hit is critical</li>
	 * <li>If hit isn't missed, calculate physical damages</li>
	 * <li>If the Creature is a PlayerInstance, Send a Server->Client packet SetupGauge</li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Calculate and set the disable delay of the bow in function of the Attack Speed</li>
	 * <li>Add this hit to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The Creature targeted
	 * @param sAtk The Attack Speed of the attacker
	 * @param reuse
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitByBow(Attack attack, Creature target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calculate if hit is missed or not
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Consume arrows
		reduceArrowCount(false);
		
		_move = null;
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(this, target);
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			
			// Bows Ranged Damage Formula (Damage gradually decreases when 60% or lower than full hit range, and increases when 60% or higher).
			// full hit range is 500 which is the base bow range, and the 60% of this is 800.
			damage1 *= (calculateDistance3D(target) / 4000) + 0.8;
		}
		
		// Check if the Creature is a PlayerInstance
		if (isPlayer())
		{
			sendPacket(new SetupGauge(getObjectId(), SetupGauge.RED, sAtk + reuse));
		}
		
		// Create a new hit task with Medium priority
		ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		
		// Calculate and set the disable delay of the bow in function of the Attack Speed
		_disableBowAttackEndTime = ((sAtk + reuse) / GameTimeTaskManager.MILLIS_IN_TICK) + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Add this hit to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	/**
	 * Launch a CrossBow attack.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Calculate if hit is missed or not</li>
	 * <li>Consume bolts</li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient</li>
	 * <li>If hit isn't missed, calculate if hit is critical</li>
	 * <li>If hit isn't missed, calculate physical damages</li>
	 * <li>If the Creature is a PlayerInstance, Send a Server->Client packet SetupGauge</li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Calculate and set the disable delay of the crossbow in function of the Attack Speed</li>
	 * <li>Add this hit to the Server-Client packet Attack</li><br>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The Creature targeted
	 * @param sAtk The Attack Speed of the attacker
	 * @param reuse
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitByCrossBow(Attack attack, Creature target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calculate if hit is missed or not
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Consume bolts
		reduceArrowCount(true);
		
		_move = null;
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(this, target);
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
		}
		
		// Check if the Creature is a PlayerInstance
		if (isPlayer())
		{
			// Send a system message
			sendPacket(SystemMessageId.YOUR_CROSSBOW_IS_PREPARING_TO_FIRE);
			
			// Send a Server->Client packet SetupGauge
			sendPacket(new SetupGauge(getObjectId(), SetupGauge.RED, sAtk + reuse));
		}
		
		// Create a new hit task with Medium priority
		ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		
		// Calculate and set the disable delay of the bow in function of the Attack Speed
		_disableBowAttackEndTime = ((sAtk + reuse) / GameTimeTaskManager.MILLIS_IN_TICK) + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Add this hit to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	/**
	 * Launch a Dual attack.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Calculate if hits are missed or not</li>
	 * <li>If hits aren't missed, calculate if shield defense is efficient</li>
	 * <li>If hits aren't missed, calculate if hit is critical</li>
	 * <li>If hits aren't missed, calculate physical damages</li>
	 * <li>Create 2 new hit tasks with Medium priority</li>
	 * <li>Add those hits to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The Creature targeted
	 * @param sAtk
	 * @return True if hit 1 or hit 2 isn't missed
	 */
	private boolean doAttackHitByDual(Attack attack, Creature target, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;
		
		// Calculate if hits are missed or not
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		final boolean miss2 = Formulas.calcHitMiss(this, target);
		
		// Check if hit 1 isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient against hit 1
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit 1 is critical
			crit1 = Formulas.calcCrit(this, target);
			
			// Calculate physical damages of hit 1
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			damage1 /= 2;
		}
		
		// Check if hit 2 isn't missed
		if (!miss2)
		{
			// Calculate if shield defense is efficient against hit 2
			shld2 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit 2 is critical
			crit2 = Formulas.calcCrit(this, target);
			
			// Calculate physical damages of hit 2
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, attack.hasSoulshot());
			damage2 /= 2;
		}
		
		// Create a new hit task with Medium priority for hit 1
		ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk / 2);
		
		// Create a new hit task with Medium priority for hit 2 with a higher delay
		ThreadPool.schedule(new HitTask(this, target, damage2, crit2, miss2, attack.hasSoulshot(), shld2), sAtk);
		
		// Add those hits to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		attack.addHit(target, damage2, miss2, crit2, shld2);
		
		// Return true if hit 1 or hit 2 isn't missed
		return (!miss1 || !miss2);
	}
	
	/**
	 * Launch a Pole attack.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Get all visible objects in a spherical area near the Creature to obtain possible targets</li>
	 * <li>If possible target is the Creature targeted, launch a simple attack against it</li>
	 * <li>If possible target isn't the Creature targeted but is attackable, launch a simple attack against it</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target
	 * @param sAtk
	 * @return True if one hit isn't missed
	 */
	private boolean doAttackHitByPole(Attack attack, Creature target, int sAtk)
	{
		// Perform the main target hit.
		boolean hitted = doAttackHitSimple(attack, target, 100, sAtk);
		
		// H5 Changes: without Polearm Mastery (skill 216) max simultaneous attacks is 3 (1 by default + 2 in skill 3599).
		int attackCountMax = (int) _stat.calcStat(Stat.ATTACK_COUNT_MAX, 1, null, null);
		if (attackCountMax > 1)
		{
			final double headingAngle = Util.convertHeadingToDegree(getHeading());
			final int maxRadius = _stat.getPhysicalAttackRange();
			final int physicalAttackAngle = _stat.getPhysicalAttackAngle();
			double attackpercent = 85;
			for (Creature obj : World.getInstance().getVisibleObjectsInRange(this, Creature.class, maxRadius))
			{
				// Skip main target.
				if (obj == target)
				{
					continue;
				}
				
				// Skip dead or fake dead target.
				if (obj.isAlikeDead())
				{
					continue;
				}
				
				// Check if target is auto attackable.
				if (!obj.isAutoAttackable(this))
				{
					continue;
				}
				
				// Check if target is within attack angle.
				if (Math.abs(calculateDirectionTo(obj) - headingAngle) > physicalAttackAngle)
				{
					continue;
				}
				
				if (obj.isPet() && isPlayer() && (((PetInstance) obj).getOwner() == getActingPlayer()))
				{
					continue;
				}
				
				if (isAttackable() && obj.isPlayer() && _target.isAttackable())
				{
					continue;
				}
				
				if (isAttackable() && obj.isAttackable() && !((Attackable) this).isChaos())
				{
					continue;
				}
				
				// Launch a simple attack against the additional target.
				hitted |= doAttackHitSimple(attack, obj, attackpercent, sAtk);
				attackpercent /= 1.15;
				if (--attackCountMax <= 0)
				{
					break;
				}
			}
		}
		
		// Return true if one hit isn't missed
		return hitted;
	}
	
	/**
	 * Launch a simple attack.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Calculate if hit is missed or not</li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient</li>
	 * <li>If hit isn't missed, calculate if hit is critical</li>
	 * <li>If hit isn't missed, calculate physical damages</li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Add this hit to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The Creature targeted
	 * @param sAtk
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitSimple(Attack attack, Creature target, int sAtk)
	{
		return doAttackHitSimple(attack, target, 100, sAtk);
	}
	
	private boolean doAttackHitSimple(Attack attack, Creature target, double attackpercent, int sAtk)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calculate if hit is missed or not
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(this, target);
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			if (attackpercent != 100)
			{
				damage1 = (int) ((damage1 * attackpercent) / 100);
			}
		}
		
		// Create a new hit task with Medium priority
		ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		
		// Add this hit to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	/**
	 * Manage the casting task (casting and interrupt time, re-use delay...) and display the casting bar and animation on client.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Verify the possibility of the the cast : skill is a spell, caster isn't muted...</li>
	 * <li>Get the list of all targets (ex : area effects) and define the Creature targeted (its stats will be used in calculation)</li>
	 * <li>Calculate the casting time (base + modifier of MAtkSpd), interrupt time and re-use delay</li>
	 * <li>Send a Server->Client packet MagicSkillUser (to display casting animation), a packet SetupGauge (to display casting bar) and a system message</li>
	 * <li>Disable all skills during the casting time (create a task EnableAllSkills)</li>
	 * <li>Disable the skill during the re-use delay (create a task EnableSkill)</li>
	 * <li>Create a task MagicUseTask (that will call method onMagicUseTimer) to launch the Magic Skill at the end of the casting time</li>
	 * </ul>
	 * @param skill The Skill to use
	 */
	public void doCast(Skill skill)
	{
		beginCast(skill, false);
	}
	
	public void doSimultaneousCast(Skill skill)
	{
		beginCast(skill, true);
	}
	
	public void doCast(Skill skill, Creature target, WorldObject[] targets)
	{
		if (!checkDoCastConditions(skill))
		{
			setCastingNow(false);
			return;
		}
		
		// Override casting type
		if (skill.isSimultaneousCast())
		{
			doSimultaneousCast(skill, target, targets);
			return;
		}
		
		stopEffectsOnAction();
		
		// Recharge AutoSoulShot
		// this method should not used with Playable
		beginCast(skill, false, target, targets);
	}
	
	public void doSimultaneousCast(Skill skill, Creature target, WorldObject[] targets)
	{
		if (!checkDoCastConditions(skill))
		{
			setCastingSimultaneouslyNow(false);
			return;
		}
		stopEffectsOnAction();
		
		beginCast(skill, true, target, targets);
	}
	
	private void beginCast(Skill skill, boolean isSimultaneous)
	{
		if (!checkDoCastConditions(skill))
		{
			if (isSimultaneous)
			{
				setCastingSimultaneouslyNow(false);
			}
			else
			{
				setCastingNow(false);
			}
			if (isPlayer())
			{
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}
		
		// Override casting type
		boolean simultaneously = isSimultaneous;
		if (skill.isSimultaneousCast() && !simultaneously)
		{
			simultaneously = true;
		}
		
		stopEffectsOnAction();
		
		// Set the target of the skill in function of Skill Type and Target Type
		Creature target = null;
		// Get all possible targets of the skill in a table in function of the skill target type
		final WorldObject[] targets = skill.getTargetList(this);
		boolean doit = false;
		
		// AURA skills should always be using caster as target
		switch (skill.getTargetType())
		{
			case AREA_SUMMON: // We need it to correct facing
			{
				target = getSummon();
				break;
			}
			case AURA:
			case AURA_CORPSE_MOB:
			case FRONT_AURA:
			case BEHIND_AURA:
			case GROUND:
			case AURA_FRIENDLY:
			{
				target = this;
				break;
			}
			case SELF:
			case PET:
			case SERVITOR:
			case SUMMON:
			case OWNER_PET:
			case PARTY:
			case CLAN:
			case PARTY_CLAN:
			case COMMAND_CHANNEL:
			{
				doit = true;
				// Fallthrough.
			}
			default:
			{
				if (targets.length == 0)
				{
					if (simultaneously)
					{
						setCastingSimultaneouslyNow(false);
					}
					else
					{
						setCastingNow(false);
					}
					// Send a Server->Client packet ActionFailed to the PlayerInstance
					if (isPlayer())
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						getAI().setIntention(AI_INTENTION_ACTIVE);
					}
					return;
				}
				
				if ((skill.isContinuous() && !skill.isDebuff()) || skill.hasEffectType(EffectType.CPHEAL, EffectType.HEAL))
				{
					doit = true;
				}
				
				if (doit)
				{
					target = (Creature) targets[0];
				}
				else
				{
					target = (Creature) _target;
				}
			}
		}
		
		beginCast(skill, simultaneously, target, targets);
	}
	
	private void beginCast(Skill skill, boolean simultaneously, Creature target, WorldObject[] targets)
	{
		if (target == null)
		{
			if (simultaneously)
			{
				setCastingSimultaneouslyNow(false);
			}
			else
			{
				setCastingNow(false);
			}
			if (isPlayer())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}
		
		if (_onCreatureSkillUse == null)
		{
			_onCreatureSkillUse = new OnCreatureSkillUse();
		}
		_onCreatureSkillUse.setCaster(this);
		_onCreatureSkillUse.setSkill(skill);
		_onCreatureSkillUse.setSimultaneously(simultaneously);
		_onCreatureSkillUse.setTarget(target);
		_onCreatureSkillUse.setTargets(targets);
		final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(_onCreatureSkillUse, this, TerminateReturn.class);
		if ((term != null) && term.terminate())
		{
			if (simultaneously)
			{
				setCastingSimultaneouslyNow(false);
			}
			else
			{
				setCastingNow(false);
			}
			if (isPlayer())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}
		
		// TODO: Unhardcode using event listeners!
		if (skill.hasEffectType(EffectType.RESURRECTION) && (isResurrectionBlocked() || target.isResurrectionBlocked()))
		{
			sendPacket(SystemMessageId.REJECT_RESURRECTION); // Reject resurrection
			target.sendPacket(SystemMessageId.REJECT_RESURRECTION); // Reject resurrection
			if (simultaneously)
			{
				setCastingSimultaneouslyNow(false);
			}
			else
			{
				setCastingNow(false);
			}
			
			if (isPlayer())
			{
				getAI().setIntention(AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
			}
			return;
		}
		
		// Get the Identifier of the skill
		final int magicId = skill.getId();
		
		// Get the Base Casting Time of the Skills.
		int skillTime = (skill.getHitTime() + skill.getCoolTime());
		if (!skill.isChanneling() || (skill.getChannelingSkillId() == 0))
		{
			// Calculate the Casting Time of the "Non-Static" Skills (with caster PAtk/MAtkSpd).
			if (!skill.isStatic())
			{
				skillTime = Formulas.calcAtkSpd(this, skill, skillTime);
			}
			// Calculate the Casting Time of Magic Skills (reduced in 40% if using SPS/BSPS)
			if (skill.isMagic() && (isChargedShot(ShotType.SPIRITSHOTS) || isChargedShot(ShotType.BLESSED_SPIRITSHOTS)))
			{
				skillTime = (int) (0.6 * skillTime);
			}
		}
		
		// Avoid broken Casting Animation.
		// Client can't handle less than 550ms Casting Animation in Magic Skills with more than 550ms base.
		if (skill.isMagic() && ((skill.getHitTime() + skill.getCoolTime()) > 550) && (skillTime < 550))
		{
			skillTime = 550;
		}
		// Client can't handle less than 500ms Casting Animation in Physical Skills with 500ms base or more.
		else if (!skill.isStatic() && ((skill.getHitTime() + skill.getCoolTime()) >= 500) && (skillTime < 500))
		{
			skillTime = 500;
		}
		
		// queue herbs and potions
		if (_isCastingSimultaneouslyNow && simultaneously)
		{
			ThreadPool.schedule(() -> beginCast(skill, simultaneously, target, targets), 100);
			return;
		}
		
		// Set the _castInterruptTime and casting status (PlayerInstance already has this true)
		if (simultaneously)
		{
			setCastingSimultaneouslyNow(true);
		}
		else
		{
			setCastingNow(true);
		}
		
		if (!simultaneously)
		{
			_castInterruptTime = -2 + GameTimeTaskManager.getInstance().getGameTicks() + (skillTime / GameTimeTaskManager.MILLIS_IN_TICK);
			setLastSkillCast(skill);
		}
		else
		{
			setLastSimultaneousSkillCast(skill);
		}
		
		// Calculate the Reuse Time of the Skill
		int reuseDelay;
		if (skill.isStaticReuse() || skill.isStatic())
		{
			reuseDelay = (skill.getReuseDelay());
		}
		else if (skill.isMagic())
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stat.MAGIC_REUSE_RATE, 1, null, null));
		}
		else if (skill.isPhysical())
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stat.P_REUSE, 1, null, null));
		}
		else
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stat.DANCE_REUSE, 1, null, null));
		}
		
		final boolean skillMastery = Formulas.calcSkillMastery(this, skill);
		
		// Skill reuse check
		if ((reuseDelay > 30000) && !skillMastery)
		{
			addTimeStamp(skill, reuseDelay);
		}
		
		// Check if this skill consume mp on start casting
		final int initmpcons = _stat.getMpInitialConsume(skill);
		if (initmpcons > 0)
		{
			_status.reduceMp(initmpcons);
			final StatusUpdate su = new StatusUpdate(this);
			su.addAttribute(StatusUpdate.CUR_MP, (int) _status.getCurrentMp());
			sendPacket(su);
		}
		
		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
		if (reuseDelay > 10)
		{
			if (skillMastery)
			{
				reuseDelay = 100;
				if (getActingPlayer() != null)
				{
					getActingPlayer().sendPacket(SystemMessageId.A_SKILL_IS_READY_TO_BE_USED_AGAIN);
				}
			}
			
			disableSkill(skill, reuseDelay);
		}
		
		// Make sure that char is facing selected target
		if (target != this)
		{
			setHeading(Util.calculateHeadingFrom(this, target));
			broadcastPacket(new ExRotation(getObjectId(), getHeading()));
		}
		
		if (isPlayable())
		{
			if ((skill.getItemConsumeId() > 0) && !destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsumeCount(), null, true))
			{
				getActingPlayer().sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				abortCast();
				return;
			}
			
			// reduce talisman mana on skill use
			if ((skill.getReferenceItemId() > 0) && (ItemTable.getInstance().getTemplate(skill.getReferenceItemId()).getBodyPart() == Item.SLOT_DECO))
			{
				for (ItemInstance item : getInventory().getAllItemsByItemId(skill.getReferenceItemId()))
				{
					if (item.isEquipped())
					{
						if (item.getMana() < item.useSkillDisTime())
						{
							abortCast();
							return;
						}
						item.decreaseMana(false, item.useSkillDisTime());
						break;
					}
				}
			}
		}
		
		// Broadcast fly effect if needed.
		if (skill.getFlyType() != null)
		{
			broadcastPacket(new FlyToLocation(this, target, skill.getFlyType()));
			setXYZ(target.getX(), target.getY(), target.getZ());
		}
		
		if (!skill.isToggle())
		{
			// Send a Server->Client packet MagicSkillUser with target, displayId, level, skillTime, reuseDelay
			// to the Creature AND to all PlayerInstance in the _KnownPlayers of the Creature
			broadcastPacket(new MagicSkillUse(this, target, skill.getDisplayId(), skill.getDisplayLevel(), skillTime, reuseDelay));
			broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getDisplayLevel(), targets));
		}
		
		// Send a system message to the player.
		if (isPlayer() && !skill.isAbnormalInstant())
		{
			SystemMessage sm = null;
			switch (magicId)
			{
				case 1312: // Fishing
				{
					// Done in PlayerInstance.startFishing()
					break;
				}
				case 2046: // Wolf Collar
				{
					sm = new SystemMessage(SystemMessageId.SUMMONING_YOUR_PET);
					break;
				}
				default:
				{
					sm = new SystemMessage(SystemMessageId.USE_S1);
					sm.addSkillName(skill);
					break;
				}
			}
			
			sendPacket(sm);
		}
		
		if (skill.hasEffects(EffectScope.START))
		{
			skill.applyEffectScope(EffectScope.START, new BuffInfo(this, target, skill), true, false);
		}
		
		final MagicUseTask mut = new MagicUseTask(this, targets, skill, skillTime, simultaneously);
		
		// launch the magic in skillTime milliseconds
		if (skillTime > 0)
		{
			// Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
			if (isPlayer() && !simultaneously)
			{
				sendPacket(new SetupGauge(getObjectId(), SetupGauge.BLUE, skillTime));
			}
			
			if (skill.isChanneling() && (skill.getChannelingSkillId() > 0))
			{
				getSkillChannelizer().startChanneling(skill);
			}
			
			if (simultaneously)
			{
				final Future<?> future = _skillCast2;
				if (future != null)
				{
					future.cancel(true);
					_skillCast2 = null;
				}
				
				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (skillTime)
				// For client animation reasons (party buffs especially) 400 ms before!
				_skillCast2 = ThreadPool.schedule(mut, skillTime - 400);
			}
			else
			{
				final Future<?> future = _skillCast;
				if (future != null)
				{
					future.cancel(true);
					_skillCast = null;
				}
				
				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (skillTime)
				// For client animation reasons (party buffs especially) 400 ms before!
				_skillCast = ThreadPool.schedule(mut, skillTime - 400);
			}
		}
		else
		{
			mut.setSkillTime(0);
			onMagicLaunchedTimer(mut);
		}
	}
	
	/**
	 * Check if casting of skill is possible
	 * @param skill
	 * @return True if casting is possible
	 */
	public boolean checkDoCastConditions(Skill skill)
	{
		if ((skill == null) || isSkillDisabled(skill) || ((skill.getFlyType() == FlyType.CHARGE) && isMovementDisabled()))
		{
			// Send a Server->Client packet ActionFailed to the PlayerInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough MP
		if (_status.getCurrentMp() < (_stat.getMpConsume(skill) + _stat.getMpInitialConsume(skill)))
		{
			// Send a System Message to the caster
			sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			
			// Send a Server->Client packet ActionFailed to the PlayerInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough HP
		if (_status.getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster
			sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			
			// Send a Server->Client packet ActionFailed to the PlayerInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Skill mute checks.
		if (!skill.isStatic())
		{
			// Check if the skill is a magic spell and if the Creature is not muted
			if (skill.isMagic())
			{
				if (isMuted())
				{
					// Send a Server->Client packet ActionFailed to the PlayerInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else
			{
				// Check if the skill is physical and if the Creature is not physical_muted
				if (isPhysicalMuted())
				{
					// Send a Server->Client packet ActionFailed to the PlayerInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// prevent casting signets to peace zone
		if (skill.isChanneling() && (skill.getChannelingSkillId() > 0) && (getInstanceId() == 0))
		{
			final ZoneRegion zoneRegion = ZoneManager.getInstance().getRegion(this);
			boolean canCast = true;
			if ((skill.getTargetType() == TargetType.GROUND) && isPlayer())
			{
				final Location wp = getActingPlayer().getCurrentSkillWorldPosition();
				if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
				{
					canCast = false;
				}
			}
			else if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
			{
				canCast = false;
			}
			if (!canCast)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
				sm.addSkillName(skill);
				sendPacket(sm);
				return false;
			}
		}
		
		// Check if the caster's weapon is limited to use only its own skills
		if (getActiveWeaponItem() != null)
		{
			final Weapon wep = getActiveWeaponItem();
			if (wep.useWeaponSkillsOnly() && !isGM() && wep.hasSkills())
			{
				boolean found = false;
				for (SkillHolder sh : wep.getSkills())
				{
					if (sh.getSkillId() == skill.getId())
					{
						found = true;
					}
				}
				
				if (!found)
				{
					if (getActingPlayer() != null)
					{
						sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_USE_ANY_OTHER_SKILL_EXCEPT_THE_WEAPON_S_SKILL);
					}
					return false;
				}
			}
		}
		
		// Check if the spell consumes an Item
		// TODO: combine check and consume
		if ((skill.getItemConsumeId() > 0) && (getInventory() != null))
		{
			// Get the ItemInstance consumed by the spell
			final ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Check if the caster owns enough consumed Item to cast
			if ((requiredItems == null) || (requiredItems.getCount() < skill.getItemConsumeCount()))
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (skill.hasEffectType(EffectType.SUMMON))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_A_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addInt(skill.getItemConsumeCount());
					sendPacket(sm);
				}
				else
				{
					// Send a System Message to the caster
					sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
				}
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Gets the item reuse time stamps map.
	 * @return the item reuse time stamps map
	 */
	public Map<Integer, TimeStamp> getItemReuseTimeStamps()
	{
		return _reuseTimeStampsItems;
	}
	
	/**
	 * Adds a item reuse time stamp.
	 * @param item the item
	 * @param reuse the reuse
	 */
	public void addTimeStampItem(ItemInstance item, long reuse)
	{
		addTimeStampItem(item, reuse, -1);
	}
	
	/**
	 * Adds a item reuse time stamp.<br>
	 * Used for restoring purposes.
	 * @param item the item
	 * @param reuse the reuse
	 * @param systime the system time
	 */
	public void addTimeStampItem(ItemInstance item, long reuse, long systime)
	{
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, systime));
	}
	
	/**
	 * Gets the item remaining reuse time for a given item object ID.
	 * @param itemObjId the item object ID
	 * @return if the item has a reuse time stamp, the remaining time, otherwise -1
	 */
	public long getItemRemainingReuseTime(int itemObjId)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsItems.get(itemObjId);
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Gets the item remaining reuse time for a given shared reuse item group.
	 * @param group the shared reuse item group
	 * @return if the shared reuse item group has a reuse time stamp, the remaining time, otherwise -1
	 */
	public long getReuseDelayOnGroup(int group)
	{
		if ((group > 0) && !_reuseTimeStampsItems.isEmpty())
		{
			final long currentTime = Chronos.currentTimeMillis();
			for (TimeStamp ts : _reuseTimeStampsItems.values())
			{
				if (ts.getSharedReuseGroup() == group)
				{
					final long stamp = ts.getStamp();
					if (currentTime < stamp)
					{
						return Math.max(stamp - currentTime, 0);
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * Gets the skill reuse time stamps map.
	 * @return the skill reuse time stamps map
	 */
	public Map<Integer, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}
	
	/**
	 * Adds the skill reuse time stamp.
	 * @param skill the skill
	 * @param reuse the delay
	 */
	public void addTimeStamp(Skill skill, long reuse)
	{
		addTimeStamp(skill, reuse, -1);
	}
	
	/**
	 * Adds the skill reuse time stamp.<br>
	 * Used for restoring purposes.
	 * @param skill the skill
	 * @param reuse the reuse
	 * @param systime the system time
	 */
	public void addTimeStamp(Skill skill, long reuse, long systime)
	{
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}
	
	/**
	 * Removes a skill reuse time stamp.
	 * @param skill the skill to remove
	 */
	public void removeTimeStamp(Skill skill)
	{
		_reuseTimeStampsSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Removes all skill reuse time stamps.
	 */
	public void resetTimeStamps()
	{
		_reuseTimeStampsSkills.clear();
	}
	
	/**
	 * Gets the skill remaining reuse time for a given skill hash code.
	 * @param hashCode the skill hash code
	 * @return if the skill has a reuse time stamp, the remaining time, otherwise -1
	 */
	public long getSkillRemainingReuseTime(int hashCode)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsSkills.get(hashCode);
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Verifies if the skill is under reuse time.
	 * @param hashCode the skill hash code
	 * @return {@code true} if the skill is under reuse time, {@code false} otherwise
	 */
	public boolean hasSkillReuse(int hashCode)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsSkills.get(hashCode);
		return (reuseStamp != null) && reuseStamp.hasNotPassed();
	}
	
	/**
	 * Gets the skill reuse time stamp.
	 * @param hashCode the skill hash code
	 * @return if the skill has a reuse time stamp, the skill reuse time stamp, otherwise {@code null}
	 */
	public TimeStamp getSkillReuseTimeStamp(int hashCode)
	{
		return _reuseTimeStampsSkills.get(hashCode);
	}
	
	/**
	 * Gets the disabled skills map.
	 * @return the disabled skills map
	 */
	public Map<Integer, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}
	
	/**
	 * Enables a skill.
	 * @param skill the skill to enable
	 */
	public void enableSkill(Skill skill)
	{
		if (skill == null)
		{
			return;
		}
		_disabledSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Disables a skill for a given time.<br>
	 * If delay is lesser or equal than zero, skill will be disabled "forever".
	 * @param skill the skill to disable
	 * @param delay delay in milliseconds
	 */
	public void disableSkill(Skill skill, long delay)
	{
		if (skill == null)
		{
			return;
		}
		_disabledSkills.put(skill.getReuseHashCode(), delay > 0 ? Chronos.currentTimeMillis() + delay : Long.MAX_VALUE);
	}
	
	/**
	 * Removes all the disabled skills.
	 */
	public void resetDisabledSkills()
	{
		_disabledSkills.clear();
	}
	
	/**
	 * Verifies if the skill is disabled.
	 * @param skill the skill
	 * @return {@code true} if the skill is disabled, {@code false} otherwise
	 */
	public boolean isSkillDisabled(Skill skill)
	{
		if (skill == null)
		{
			return false;
		}
		
		if (isAllSkillsDisabled())
		{
			return true;
		}
		
		final int hashCode = skill.getReuseHashCode();
		if (hasSkillReuse(hashCode))
		{
			return true;
		}
		
		if (_disabledSkills.isEmpty())
		{
			return false;
		}
		final Long stamp = _disabledSkills.get(hashCode);
		if (stamp == null)
		{
			return false;
		}
		if (stamp < Chronos.currentTimeMillis())
		{
			_disabledSkills.remove(hashCode);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Disables all skills.
	 */
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	/**
	 * Enables all skills, except those under reuse time or previously disabled.
	 */
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	/**
	 * Kill the Creature.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the Creature</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other PlayerInstance to inform</li>
	 * <li>Notify Creature AI</li>
	 * </ul>
	 * @param killer The Creature who killed it
	 * @return false if the player is already dead.
	 */
	public boolean doDie(Creature killer)
	{
		final TerminateReturn returnBack = EventDispatcher.getInstance().notifyEvent(new OnCreatureKill(killer, this), this, TerminateReturn.class);
		if ((returnBack != null) && returnBack.terminate())
		{
			return false;
		}
		
		// killing is only possible one time
		synchronized (this)
		{
			if (_isDead)
			{
				return false;
			}
			
			// now reset currentHp to zero
			setCurrentHp(0);
			setDead(true);
		}
		
		// Calculate rewards for main damage dealer.
		final Creature mainDamageDealer = isMonster() ? ((MonsterInstance) this).getMainDamageDealer() : null;
		calculateRewards(mainDamageDealer != null ? mainDamageDealer : killer);
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		_status.stopHpMpRegeneration();
		
		if (isAttackable())
		{
			final Spawn spawn = ((Npc) this).getSpawn();
			if ((spawn != null) && spawn.isRespawnEnabled())
			{
				stopAllEffects();
			}
			else
			{
				_effectList.stopAllEffectsWithoutExclusions(true, true);
			}
		}
		else
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other PlayerInstance to inform
		broadcastStatusUpdate();
		
		// Notify Creature AI
		if (hasAI())
		{
			getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		}
		
		ZoneManager.getInstance().getRegion(this).onDeath(this);
		
		getAttackByList().clear();
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		return true;
	}
	
	@Override
	public boolean decayMe()
	{
		if (hasAI())
		{
			getAI().stopAITask();
		}
		
		_onCreatureAttack = null;
		_onCreatureAttacked = null;
		_onCreatureDamageDealt = null;
		_onCreatureDamageReceived = null;
		_onCreatureAttackAvoid = null;
		_onCreatureSkillUse = null;
		
		return super.decayMe();
	}
	
	@Override
	public boolean deleteMe()
	{
		if (hasAI())
		{
			getAI().stopAITask();
		}
		
		// Remove all effects, do not broadcast changes.
		_effectList.stopAllEffectsWithoutExclusions(false, false);
		
		// Forget all seen creatures.
		if (_seenCreatures != null)
		{
			CreatureSeeTaskManager.getInstance().remove(this);
			_seenCreatures.clear();
		}
		
		// Cancel the BuffFinishTask related to this creature.
		cancelBuffFinishTask();
		
		// Set world region to null.
		setWorldRegion(null);
		
		return true;
	}
	
	public void detachAI()
	{
		if (isWalker())
		{
			return;
		}
		setAI(null);
	}
	
	protected void calculateRewards(Creature killer)
	{
	}
	
	/** Sets HP, MP and CP and revives the Creature. */
	public void doRevive()
	{
		if (!_isDead)
		{
			return;
		}
		if (!_isTeleporting)
		{
			setIsPendingRevive(false);
			setDead(false);
			
			if ((Config.RESPAWN_RESTORE_CP > 0) && (_status.getCurrentCp() < (_stat.getMaxCp() * Config.RESPAWN_RESTORE_CP)))
			{
				_status.setCurrentCp(_stat.getMaxCp() * Config.RESPAWN_RESTORE_CP);
			}
			if ((Config.RESPAWN_RESTORE_HP > 0) && (_status.getCurrentHp() < (_stat.getMaxHp() * Config.RESPAWN_RESTORE_HP)))
			{
				_status.setCurrentHp(_stat.getMaxHp() * Config.RESPAWN_RESTORE_HP);
			}
			if ((Config.RESPAWN_RESTORE_MP > 0) && (_status.getCurrentMp() < (_stat.getMaxMp() * Config.RESPAWN_RESTORE_MP)))
			{
				_status.setCurrentMp(_stat.getMaxMp() * Config.RESPAWN_RESTORE_MP);
			}
			
			// Start broadcast status
			broadcastPacket(new Revive(this));
			ZoneManager.getInstance().getRegion(this).onRevive(this);
		}
		else
		{
			setIsPendingRevive(true);
		}
	}
	
	/**
	 * Revives the Creature using skill.
	 * @param revivePower
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	/**
	 * Gets this creature's AI.
	 * @return the AI
	 */
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				ai = _ai;
				if (ai == null)
				{
					_ai = ai = initAI();
				}
			}
		}
		return ai;
	}
	
	/**
	 * Initialize this creature's AI.<br>
	 * OOP approach to be overridden in child classes.
	 * @return the new AI
	 */
	protected CreatureAI initAI()
	{
		return new CreatureAI(this);
	}
	
	public void setAI(CreatureAI newAI)
	{
		final CreatureAI oldAI = _ai;
		if ((oldAI != null) && (oldAI != newAI) && (oldAI instanceof AttackableAI))
		{
			oldAI.stopAITask();
		}
		_ai = newAI;
	}
	
	/**
	 * Verifies if this creature has an AI,
	 * @return {@code true} if this creature has an AI, {@code false} otherwise
	 */
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	/**
	 * @return True if the Creature is RaidBoss or his minion.
	 */
	public boolean isRaid()
	{
		return false;
	}
	
	/**
	 * @return True if the Creature is minion.
	 */
	public boolean isMinion()
	{
		return false;
	}
	
	/**
	 * @return True if the Creature is minion of RaidBoss.
	 */
	public boolean isRaidMinion()
	{
		return false;
	}
	
	/**
	 * @return a list of Creature that attacked.
	 */
	public Set<Creature> getAttackByList()
	{
		if (_attackByList == null)
		{
			synchronized (this)
			{
				if (_attackByList == null)
				{
					_attackByList = ConcurrentHashMap.newKeySet();
				}
			}
		}
		return _attackByList;
	}
	
	public Skill getLastSimultaneousSkillCast()
	{
		return _lastSimultaneousSkillCast;
	}
	
	public void setLastSimultaneousSkillCast(Skill skill)
	{
		_lastSimultaneousSkillCast = skill;
	}
	
	public Skill getLastSkillCast()
	{
		return _lastSkillCast;
	}
	
	public void setLastSkillCast(Skill skill)
	{
		_lastSkillCast = skill;
	}
	
	public boolean isAfraid()
	{
		return isAffected(EffectFlag.FEAR);
	}
	
	/**
	 * @return True if the Creature can't use its skills (ex : stun, sleep...).
	 */
	public boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isStunned() || isSleeping() || isParalyzed();
	}
	
	/**
	 * @return True if the Creature can't attack (attackEndTime, attackMute, fake death, stun, sleep, paralyze).
	 */
	public boolean isAttackDisabled()
	{
		return isAttackingNow() || isDisabled();
	}
	
	/**
	 * @return True if the Creature is disabled (attackMute, fake death, stun, sleep, paralyze).
	 */
	public boolean isDisabled()
	{
		return _AIdisabled || isAlikeDead() || isPhysicalAttackMuted() || isStunned() || isSleeping() || isParalyzed();
	}
	
	public Calculator[] getCalculators()
	{
		return _calculators;
	}
	
	public boolean isConfused()
	{
		return isAffected(EffectFlag.CONFUSED);
	}
	
	/**
	 * @return True if the Creature is dead or use fake death.
	 */
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	/**
	 * @return True if the Creature is dead.
	 */
	public boolean isDead()
	{
		return _isDead;
	}
	
	public void setDead(boolean value)
	{
		_isDead = value;
	}
	
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	public void setImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	public boolean isMuted()
	{
		return isAffected(EffectFlag.MUTED);
	}
	
	public boolean isPhysicalMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_MUTED);
	}
	
	public boolean isPhysicalAttackMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_ATTACK_MUTED);
	}
	
	/**
	 * @return True if the Creature can't move (stun, root, sleep, overload, paralyzed).
	 */
	public boolean isMovementDisabled()
	{
		// check for isTeleporting to prevent teleport cheating (if appear packet not received)
		return isStunned() || isRooted() || isSleeping() || _isOverloaded || isParalyzed() || _isImmobilized || isAlikeDead() || _isTeleporting;
	}
	
	/**
	 * @return True if the Creature can not be controlled by the player (confused, afraid).
	 */
	public boolean isOutOfControl()
	{
		return isConfused() || isAfraid();
	}
	
	public boolean isOverloaded()
	{
		return _isOverloaded;
	}
	
	/**
	 * Set the overloaded status of the Creature is overloaded (if True, the PlayerInstance can't take more item).
	 * @param value
	 */
	public void setOverloaded(boolean value)
	{
		_isOverloaded = value;
	}
	
	public boolean isParalyzed()
	{
		return _isParalyzed || isAffected(EffectFlag.PARALYZED);
	}
	
	public void setParalyzed(boolean value)
	{
		_isParalyzed = value;
	}
	
	public boolean isPendingRevive()
	{
		return _isDead && _isPendingRevive;
	}
	
	public void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}
	
	public boolean isDisarmed()
	{
		return isAffected(EffectFlag.DISARMED);
	}
	
	/**
	 * @return the summon
	 */
	public Summon getSummon()
	{
		return null;
	}
	
	/**
	 * @return {@code true} if the character has a summon, {@code false} otherwise
	 */
	public boolean hasSummon()
	{
		return getSummon() != null;
	}
	
	/**
	 * @return {@code true} if the character has a pet, {@code false} otherwise
	 */
	public boolean hasPet()
	{
		return hasSummon() && getSummon().isPet();
	}
	
	/**
	 * @return {@code true} if the character has a servitor, {@code false} otherwise
	 */
	public boolean hasServitor()
	{
		return hasSummon() && getSummon().isServitor();
	}
	
	public boolean isRooted()
	{
		return isAffected(EffectFlag.ROOTED);
	}
	
	/**
	 * @return True if the Creature is running.
	 */
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	private final void setRunning(boolean value)
	{
		if (_isRunning == value)
		{
			return;
		}
		
		_isRunning = value;
		if (_stat.getRunSpeed() != 0)
		{
			broadcastPacket(new ChangeMoveType(this));
		}
		if (isPlayer())
		{
			getActingPlayer().broadcastUserInfo();
		}
		else if (isSummon())
		{
			broadcastStatusUpdate();
		}
		else if (isNpc())
		{
			World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
			{
				if (!isVisibleFor(player))
				{
					return;
				}
				
				if (isFakePlayer())
				{
					player.sendPacket(new FakePlayerInfo((Npc) this));
				}
				else if (_stat.getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo((Npc) this, player));
				}
				else
				{
					player.sendPacket(new AbstractNpcInfo.NpcInfo((Npc) this, player));
				}
			});
		}
	}
	
	/** Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance. */
	public void setRunning()
	{
		setRunning(true);
	}
	
	public boolean isSleeping()
	{
		return isAffected(EffectFlag.SLEEP);
	}
	
	public boolean isStunned()
	{
		return isAffected(EffectFlag.STUNNED);
	}
	
	public boolean isBetrayed()
	{
		return isAffected(EffectFlag.BETRAYED);
	}
	
	public boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public void setTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	public void setInvul(boolean value)
	{
		_isInvul = value;
	}
	
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || isAffected(EffectFlag.INVUL);
	}
	
	public void setMortal(boolean value)
	{
		_isMortal = value;
	}
	
	public boolean isMortal()
	{
		return _isMortal;
	}
	
	public boolean isUndead()
	{
		return false;
	}
	
	public boolean isResurrectionBlocked()
	{
		return isAffected(EffectFlag.BLOCK_RESURRECTION);
	}
	
	public boolean isFlying()
	{
		return _isFlying;
	}
	
	public void setFlying(boolean mode)
	{
		_isFlying = mode;
	}
	
	public CreatureStat getStat()
	{
		return _stat;
	}
	
	/**
	 * Initializes the CharStat class of the WorldObject, is overwritten in classes that require a different CharStat Type.<br>
	 * Removes the need for instanceof checks.
	 */
	public void initCharStat()
	{
		_stat = new CreatureStat(this);
	}
	
	public void setStat(CreatureStat value)
	{
		_stat = value;
	}
	
	public CreatureStatus getStatus()
	{
		return _status;
	}
	
	/**
	 * Initializes the CharStatus class of the WorldObject, is overwritten in classes that require a different CharStatus Type.<br>
	 * Removes the need for instanceof checks.
	 */
	public void initCharStatus()
	{
		_status = new CreatureStatus(this);
	}
	
	public void setStatus(CreatureStatus value)
	{
		_status = value;
	}
	
	public CreatureTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Set the template of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Each Creature owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
	 * All of those properties are stored in a different template for each type of Creature.<br>
	 * Each template is loaded once in the server cache memory (reduce memory use).<br>
	 * When a new instance of Creature is spawned, server just create a link between the instance and the template This link is stored in <b>_template</b>.
	 * @param template
	 */
	protected final void setTemplate(CreatureTemplate template)
	{
		_template = template;
	}
	
	/**
	 * @return the Title of the Creature.
	 */
	public String getTitle()
	{
		return _title;
	}
	
	/**
	 * Set the Title of the Creature.
	 * @param value
	 */
	public void setTitle(String value)
	{
		if (value == null)
		{
			_title = "";
		}
		else
		{
			_title = isPlayer() && (value.length() > 21) ? value.substring(0, 20) : value;
		}
	}
	
	/**
	 * Set the Creature movement type to walk and send Server->Client packet ChangeMoveType to all others PlayerInstance.
	 */
	public void setWalking()
	{
		setRunning(false);
	}
	
	/**
	 * Gets the abnormal visual effects affecting this character.
	 * @return a map of 32 bits containing all abnormal visual effects in progress for this character
	 */
	public int getAbnormalVisualEffects()
	{
		return _abnormalVisualEffects;
	}
	
	/**
	 * Gets the special abnormal visual effects affecting this character.
	 * @return a map of 32 bits containing all special effect in progress for this character
	 */
	public int getAbnormalVisualEffectSpecial()
	{
		return _abnormalVisualEffectsSpecial;
	}
	
	/**
	 * Gets the event abnormal visual effects affecting this character.
	 * @return a map of 32 bits containing all event abnormal visual effects in progress for this character
	 */
	public int getAbnormalVisualEffectEvent()
	{
		return _abnormalVisualEffectsEvent;
	}
	
	/**
	 * Verify if this creature is affected by the given abnormal visual effect.
	 * @param ave the abnormal visual effect
	 * @return {@code true} if the creature is affected by the abnormal visual effect, {@code false} otherwise
	 */
	public boolean hasAbnormalVisualEffect(AbnormalVisualEffect ave)
	{
		if (ave.isEvent())
		{
			return (_abnormalVisualEffectsEvent & ave.getMask()) == ave.getMask();
		}
		
		if (ave.isSpecial())
		{
			return (_abnormalVisualEffectsSpecial & ave.getMask()) == ave.getMask();
		}
		
		return (_abnormalVisualEffects & ave.getMask()) == ave.getMask();
	}
	
	/**
	 * Adds the abnormal visual effect flags in the binary mask and send Server->Client UserInfo/CharInfo packet.
	 * @param update if {@code true} update packets will be sent
	 * @param aves the abnormal visual effects
	 */
	public void startAbnormalVisualEffect(boolean update, AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			if (ave.isEvent())
			{
				_abnormalVisualEffectsEvent |= ave.getMask();
			}
			else if (ave.isSpecial())
			{
				_abnormalVisualEffectsSpecial |= ave.getMask();
			}
			else
			{
				_abnormalVisualEffects |= ave.getMask();
			}
		}
		if (update)
		{
			updateAbnormalEffect();
		}
	}
	
	/**
	 * Removes the abnormal visual effect flags from the binary mask and send Server->Client UserInfo/CharInfo packet.
	 * @param update if {@code true} update packets will be sent
	 * @param aves the abnormal visual effects
	 */
	public void stopAbnormalVisualEffect(boolean update, AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			if (ave.isEvent())
			{
				_abnormalVisualEffectsEvent &= ~ave.getMask();
			}
			else if (ave.isSpecial())
			{
				_abnormalVisualEffectsSpecial &= ~ave.getMask();
			}
			else
			{
				_abnormalVisualEffects &= ~ave.getMask();
			}
		}
		if (update)
		{
			updateAbnormalEffect();
		}
	}
	
	/**
	 * Active the abnormal effect Fake Death flag, notify the Creature AI and send Server->Client UserInfo/CharInfo packet.
	 */
	public void startFakeDeath()
	{
		if (!isPlayer())
		{
			return;
		}
		
		getActingPlayer().setFakeDeath(true);
		// Aborts any attacks/casts if fake dead
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
		
		// Remove target from those that have the untargetable creature on target.
		if (Config.FAKE_DEATH_UNTARGET)
		{
			World.getInstance().forEachVisibleObject(this, Creature.class, c ->
			{
				if (c.getTarget() == this)
				{
					c.setTarget(null);
				}
			});
		}
	}
	
	/**
	 * Launch a Stun Abnormal Effect on the Creature.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Calculate the success rate of the Stun Abnormal Effect on this Creature</li>
	 * <li>If Stun succeed, active the abnormal effect Stun flag, notify the Creature AI and send Server->Client UserInfo/CharInfo packet</li>
	 * <li>If Stun NOT succeed, send a system message Failed to the PlayerInstance attacker</li>
	 * </ul>
	 */
	public void startStunning()
	{
		// Aborts any attacks/casts if stunned
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_STUNNED);
		if (!isSummon())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		updateAbnormalEffect();
	}
	
	public void startParalyze()
	{
		// Aborts any attacks/casts if paralyzed
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_PARALYZED);
	}
	
	/**
	 * Stop all active skills effects in progress on the Creature.
	 */
	public void stopAllEffects()
	{
		_effectList.stopAllEffects();
	}
	
	/**
	 * Stops all effects, except those that last through death.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effectList.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	/**
	 * Stop and remove the effects corresponding to the skill ID.
	 * @param type determines the system message that will be sent.
	 * @param skillId the skill Id
	 */
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		_effectList.stopSkillEffects(type, skillId);
	}
	
	public void stopEffects(EffectType type)
	{
		_effectList.stopEffects(type);
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set.<br>
	 * Called on any action except movement (attack, cast).
	 */
	public void stopEffectsOnAction()
	{
		_effectList.stopEffectsOnAction();
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnDamage" set.<br>
	 * Called on decreasing HP and mana burn.
	 * @param awake
	 */
	public void stopEffectsOnDamage(boolean awake)
	{
		_effectList.stopEffectsOnDamage(awake);
	}
	
	/**
	 * Stop a specified/all Fake Death abnormal Effect.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Delete a specified/all (if effect=null) Fake Death abnormal Effect from Creature and update client magic icon</li>
	 * <li>Set the abnormal effect flag _fake_death to False</li>
	 * <li>Notify the Creature AI</li>
	 * </ul>
	 * @param removeEffects
	 */
	public void stopFakeDeath(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectType.FAKE_DEATH);
		}
		
		// if this is a player instance, start the grace period for this character (grace from mobs only)!
		if (isPlayer())
		{
			getActingPlayer().setFakeDeath(false);
			getActingPlayer().setRecentFakeDeath(true);
		}
		
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
		// TODO: Temp hack: players see FD on ppl that are moving: Teleport to someone who uses FD - if he gets up he will fall down again for that client -
		// even tho he is actually standing... Probably bad info in CharInfo packet?
		broadcastPacket(new Revive(this));
	}
	
	/**
	 * Stop a specified/all Stun abnormal Effect.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Delete a specified/all (if effect=null) Stun abnormal Effect from Creature and update client magic icon</li>
	 * <li>Set the abnormal effect flag _stuned to False</li>
	 * <li>Notify the Creature AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li>
	 * </ul>
	 * @param removeEffects
	 */
	public void stopStunning(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectType.STUN);
		}
		
		if (!isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		updateAbnormalEffect();
	}
	
	/**
	 * Stop Effect: Transformation.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove Transformation Effect</li>
	 * <li>Notify the Creature AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li>
	 * </ul>
	 * @param removeEffects
	 */
	public void stopTransformation(boolean removeEffects)
	{
		if (removeEffects)
		{
			_effectList.stopSkillEffects(SkillFinishType.NORMAL, AbnormalType.TRANSFORM);
		}
		
		// if this is a player instance, then untransform, also set the transform_id column equal to 0 if not cursed.
		if (isPlayer() && (getActingPlayer().getTransformation() != null))
		{
			getActingPlayer().untransform();
		}
		
		if (!isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		updateAbnormalEffect();
	}
	
	public abstract void updateAbnormalEffect();
	
	/**
	 * Update active skills in progress (In Use and Not In Use because stacked) icons on client.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * All active skills effects in progress (In Use and Not In Use because stacked) are represented by an icon on the client.<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method ONLY UPDATE the client of the player and not clients of all players in the party.</b></font>
	 */
	public void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	/**
	 * Updates Effect Icons for this character(player/summon) and his party if any.
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// overridden
	}
	
	public boolean isAffectedBySkill(int skillId)
	{
		return _effectList.isAffectedBySkill(skillId);
	}
	
	/**
	 * This class group all movement data.<br>
	 * <br>
	 * <b><u>Data</u>:</b>
	 * <ul>
	 * <li>_moveTimestamp : Last time position update</li>
	 * <li>_xDestination, _yDestination, _zDestination : Position of the destination</li>
	 * <li>_xMoveFrom, _yMoveFrom, _zMoveFrom : Position of the origin</li>
	 * <li>_moveStartTime : Start time of the movement</li>
	 * <li>_ticksToMove : Number of ticks between the start and the destination</li>
	 * <li>_xSpeedTicks, _ySpeedTicks : Speed in unit/ticks</li>
	 * </ul>
	 */
	public static class MoveData
	{
		// when we retrieve x/y/z we use GameTimeControl.getGameTicks()
		// if we are moving, but move timestamp==gameticks, we don't need
		// to recalculate position
		public int _moveStartTime;
		public int _moveTimestamp; // last update
		public int _xDestination;
		public int _yDestination;
		public int _zDestination;
		public double _xAccurate; // otherwise there would be rounding errors
		public double _yAccurate;
		public double _zAccurate;
		public int _heading;
		
		public boolean disregardingGeodata;
		public int onGeodataPathIndex;
		public List<Location> geoPath;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
	}
	
	/**
	 * Add a Func to the Calculator set of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b> A Creature owns a table of Calculators called <b>_calculators</b>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematical function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * To reduce cache memory use, NPCInstances who don't have skills share the same Calculator set called <b>NPC_STD_CALCULATOR</b>.<br>
	 * That's why, if a NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR must be create in its _calculators before adding new Func object.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If _calculators is linked to NPC_STD_CALCULATOR, create a copy of NPC_STD_CALCULATOR in _calculators</li>
	 * <li>Add the Func object to _calculators</li>
	 * </ul>
	 * @param function The Func object to add to the Calculator corresponding to the state affected
	 */
	public void addStatFunc(AbstractFunction function)
	{
		if (function == null)
		{
			return;
		}
		
		synchronized (this)
		{
			// Check if Calculator set is linked to the standard Calculator set of NPC
			if (_calculators == NPC_STD_CALCULATOR)
			{
				// Create a copy of the standard NPC Calculator set
				_calculators = new Calculator[Stat.NUM_STATS];
				for (int i = 0; i < Stat.NUM_STATS; i++)
				{
					if (NPC_STD_CALCULATOR[i] != null)
					{
						_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
					}
				}
			}
			
			// Select the Calculator of the affected state in the Calculator set
			final int stat = function.getStat().ordinal();
			if (_calculators[stat] == null)
			{
				_calculators[stat] = new Calculator();
			}
			
			// Add the Func to the calculator corresponding to the state
			_calculators[stat].addFunc(function);
		}
	}
	
	/**
	 * Add a list of Funcs to the Calculator set of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * A Creature owns a table of Calculators called <b>_calculators</b>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method is ONLY for PlayerInstance</b></font><br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Equip an item from inventory</li>
	 * <li>Learn a new passive skill</li>
	 * <li>Use an active skill</li>
	 * </ul>
	 * @param functions The list of Func objects to add to the Calculator corresponding to the state affected
	 */
	public void addStatFuncs(List<AbstractFunction> functions)
	{
		final List<Stat> modifiedStats = new ArrayList<>();
		for (AbstractFunction f : functions)
		{
			modifiedStats.add(f.getStat());
			addStatFunc(f);
		}
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove a Func from the Calculator set of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * A Creature owns a table of Calculators called <b>_calculators</b>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * To reduce cache memory use, NPCInstances who don't have skills share the same Calculator set called <b>NPC_STD_CALCULATOR</b>.<br>
	 * That's why, if a NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR must be create in its _calculators before addind new Func object.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove the Func object from _calculators</li>
	 * <li>If Creature is a NPCInstance and _calculators is equal to NPC_STD_CALCULATOR, free cache memory and just create a link on NPC_STD_CALCULATOR in _calculators</li>
	 * </ul>
	 * @param function The Func object to remove from the Calculator corresponding to the state affected
	 */
	public void removeStatFunc(AbstractFunction function)
	{
		if (function == null)
		{
			return;
		}
		
		// Select the Calculator of the affected state in the Calculator set
		final int stat = function.getStat().ordinal();
		
		synchronized (this)
		{
			if (_calculators[stat] == null)
			{
				return;
			}
			
			// Remove the Func object from the Calculator
			_calculators[stat].removeFunc(function);
			
			if (_calculators[stat].size() == 0)
			{
				_calculators[stat] = null;
			}
			
			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (isNpc())
			{
				int i = 0;
				for (; i < Stat.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stat.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
		}
	}
	
	/**
	 * Remove a list of Funcs from the Calculator set of the PlayerInstance.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * A Creature owns a table of Calculators called <b>_calculators</b>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method is ONLY for PlayerInstance</b></font><br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Unequip an item from inventory</li>
	 * <li>Stop an active skill</li>
	 * </ul>
	 * @param functions The list of Func objects to add to the Calculator corresponding to the state affected
	 */
	public void removeStatFuncs(AbstractFunction[] functions)
	{
		final List<Stat> modifiedStats = new ArrayList<>();
		for (AbstractFunction f : functions)
		{
			modifiedStats.add(f.getStat());
			removeStatFunc(f);
		}
		
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove all Func objects with the selected owner from the Calculator set of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * A Creature owns a table of Calculators called <b>_calculators</b>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * To reduce cache memory use, NPCInstances who don't have skills share the same Calculator set called <b>NPC_STD_CALCULATOR</b>.<br>
	 * That's why, if a NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR must be create in its _calculators before addind new Func object.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove all Func objects of the selected owner from _calculators</li>
	 * <li>If Creature is a NPCInstance and _calculators is equal to NPC_STD_CALCULATOR, free cache memory and just create a link on NPC_STD_CALCULATOR in _calculators</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Unequip an item from inventory</li>
	 * <li>Stop an active skill</li>
	 * </ul>
	 * @param owner The Object(Skill, Item...) that has created the effect
	 */
	public void removeStatsOwner(Object owner)
	{
		List<Stat> modifiedStats = null;
		int i = 0;
		// Go through the Calculator set
		synchronized (this)
		{
			for (Calculator calc : _calculators)
			{
				if (calc != null)
				{
					// Delete all Func objects of the selected owner
					if (modifiedStats != null)
					{
						modifiedStats.addAll(calc.removeOwner(owner));
					}
					else
					{
						modifiedStats = calc.removeOwner(owner);
					}
					
					if (calc.size() == 0)
					{
						_calculators[i] = null;
					}
				}
				i++;
			}
			
			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (isNpc())
			{
				i = 0;
				for (; i < Stat.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stat.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
			
			broadcastModifiedStats(modifiedStats);
		}
	}
	
	protected void broadcastModifiedStats(List<Stat> stats)
	{
		if (!isSpawned())
		{
			return;
		}
		
		if ((stats == null) || stats.isEmpty())
		{
			return;
		}
		
		if (isSummon())
		{
			final Summon summon = (Summon) this;
			if (summon.getOwner() != null)
			{
				summon.updateAndBroadcastStatus(1);
			}
		}
		else
		{
			boolean broadcastFull = false;
			final StatusUpdate su = new StatusUpdate(this);
			for (Stat stat : stats)
			{
				if (stat == Stat.POWER_ATTACK_SPEED)
				{
					su.addAttribute(StatusUpdate.ATK_SPD, (int) _stat.getPAtkSpd());
				}
				else if (stat == Stat.MAGIC_ATTACK_SPEED)
				{
					su.addAttribute(StatusUpdate.CAST_SPD, _stat.getMAtkSpd());
				}
				else if (stat == Stat.MOVE_SPEED)
				{
					broadcastFull = true;
				}
			}
			
			if (isPlayer())
			{
				if (broadcastFull)
				{
					getActingPlayer().updateAndBroadcastStatus(2);
				}
				else
				{
					getActingPlayer().updateAndBroadcastStatus(1);
					if (su.hasAttributes())
					{
						broadcastPacket(su);
					}
				}
				if ((getSummon() != null) && isAffected(EffectFlag.SERVITOR_SHARE))
				{
					getSummon().broadcastStatusUpdate();
				}
			}
			else if (isNpc())
			{
				if (broadcastFull)
				{
					World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
					{
						if (!isVisibleFor(player))
						{
							return;
						}
						
						if (isFakePlayer())
						{
							player.sendPacket(new FakePlayerInfo((Npc) this));
						}
						else if (_stat.getRunSpeed() == 0)
						{
							player.sendPacket(new ServerObjectInfo((Npc) this, player));
						}
						else
						{
							player.sendPacket(new AbstractNpcInfo.NpcInfo((Npc) this, player));
						}
					});
				}
				else if (su.hasAttributes())
				{
					broadcastPacket(su);
				}
			}
			else if (su.hasAttributes())
			{
				broadcastPacket(su);
			}
		}
	}
	
	public int getXdestination()
	{
		final MoveData m = _move;
		if (m != null)
		{
			return m._xDestination;
		}
		return getX();
	}
	
	/**
	 * @return the Y destination of the Creature or the Y position if not in movement.
	 */
	public int getYdestination()
	{
		final MoveData m = _move;
		if (m != null)
		{
			return m._yDestination;
		}
		return getY();
	}
	
	/**
	 * @return the Z destination of the Creature or the Z position if not in movement.
	 */
	public int getZdestination()
	{
		final MoveData m = _move;
		if (m != null)
		{
			return m._zDestination;
		}
		return getZ();
	}
	
	/**
	 * @return True if the Creature is in combat.
	 */
	public boolean isInCombat()
	{
		return hasAI() && ((getAI().getAttackTarget() != null) || getAI().isAutoAttacking());
	}
	
	/**
	 * @return True if the Creature is moving.
	 */
	public boolean isMoving()
	{
		return _move != null;
	}
	
	/**
	 * @return True if the Creature is traveling a calculated path.
	 */
	public boolean isOnGeodataPath()
	{
		final MoveData m = _move;
		if (m == null)
		{
			return false;
		}
		if (m.onGeodataPathIndex == -1)
		{
			return false;
		}
		if (m.onGeodataPathIndex == (m.geoPath.size() - 1))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * @return True if the Creature is casting.
	 */
	public boolean isCastingNow()
	{
		return _isCastingNow;
	}
	
	public void setCastingNow(boolean value)
	{
		_isCastingNow = value;
	}
	
	public boolean isCastingSimultaneouslyNow()
	{
		return _isCastingSimultaneouslyNow;
	}
	
	public void setCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}
	
	/**
	 * @return True if the cast of the Creature can be aborted.
	 */
	public boolean canAbortCast()
	{
		return _castInterruptTime > GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	public int getCastInterruptTime()
	{
		return _castInterruptTime;
	}
	
	/**
	 * Verifies if the creature is attacking now.
	 * @return {@code true} if the creature is attacking now, {@code false} otherwise
	 */
	public boolean isAttackingNow()
	{
		return _attackEndTime > System.nanoTime();
	}
	
	/**
	 * Abort the attack of the Creature and send Server->Client ActionFailed packet.
	 */
	public void abortAttack()
	{
		if (isAttackingNow())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Abort the cast of the Creature and send Server->Client MagicSkillCanceld/ActionFailed packet.
	 */
	public void abortCast()
	{
		if (_isCastingNow || _isCastingSimultaneouslyNow)
		{
			Future<?> future = _skillCast;
			// cancels the skill hit scheduled task
			if (future != null)
			{
				future.cancel(true);
				_skillCast = null;
			}
			future = _skillCast2;
			if (future != null)
			{
				future.cancel(true);
				_skillCast2 = null;
			}
			
			// TODO: Handle removing spawned npc.
			if (isChanneling())
			{
				getSkillChannelizer().stopChanneling();
			}
			
			if (_allSkillsDisabled)
			{
				enableAllSkills(); // this remains for forced skill use, e.g. scroll of escape
			}
			setCastingNow(false);
			setCastingSimultaneouslyNow(false);
			// safeguard for cannot be interrupt any more
			_castInterruptTime = 0;
			if (isPlayer())
			{
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING); // setting back previous intention
			}
			broadcastPacket(new MagicSkillCanceled(getObjectId())); // broadcast packet to stop animations client-side
			sendPacket(ActionFailed.STATIC_PACKET); // send an "action failed" packet to the caster
		}
	}
	
	/**
	 * Update the position of the Creature during a movement and return True if the movement is finished.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <b>_move</b> of the Creature.<br>
	 * The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<br>
	 * When the movement is started (ex : by MovetoLocation), this method will be called each 0.1 sec to estimate and update the Creature position on the server.<br>
	 * Note, that the current server position can differe from the current client position even if each movement is straight foward.<br>
	 * That's why, client send regularly a Client->Server ValidatePosition packet to eventually correct the gap on the server.<br>
	 * But, it's always the server position that is used in range calculation. At the end of the estimated movement time,<br>
	 * the Creature position is automatically set to the destination position even if the movement is not finished.<br>
	 * <font color=#FF0000><b><u>Caution</u>: The current Z position is obtained FROM THE CLIENT by the Client->Server ValidatePosition Packet.<br>
	 * But x and y positions must be calculated to avoid that players try to modify their movement speed.</b></font>
	 * @return True if the movement is finished
	 */
	public boolean updatePosition()
	{
		// Get movement data
		final MoveData m = _move;
		if (m == null)
		{
			return true;
		}
		
		if (!isSpawned())
		{
			_move = null;
			return true;
		}
		
		// Check if this is the first update
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}
		
		final int gameTicks = GameTimeTaskManager.getInstance().getGameTicks();
		
		// Check if the position has already been calculated
		if (m._moveTimestamp == gameTicks)
		{
			return false;
		}
		
		final int xPrev = getX();
		final int yPrev = getY();
		final int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations
		double dx = m._xDestination - m._xAccurate;
		double dy = m._yDestination - m._yAccurate;
		double dz = m._zDestination - zPrev; // Z coordinate will follow client values
		if (isPlayer() && !_isFlying)
		{
			final double distance = Math.hypot(dx, dy);
			if (_cursorKeyMovement // In case of cursor movement, avoid moving through obstacles.
				|| (distance > 3000) // Stop movement when player has clicked far away and intersected with an obstacle.
				|| ((getWorldRegion() != null) && !getWorldRegion().getDoors().isEmpty())) // Check for nearby doors.
			{
				final double angle = Util.convertHeadingToDegree(getHeading());
				final double radian = Math.toRadians(angle);
				final double course = Math.toRadians(180);
				final double frontDistance = 10 * (_stat.getMoveSpeed() / 100);
				final int x1 = (int) (Math.cos(Math.PI + radian + course) * frontDistance);
				final int y1 = (int) (Math.sin(Math.PI + radian + course) * frontDistance);
				final int x = xPrev + x1;
				final int y = yPrev + y1;
				if (!GeoEngine.getInstance().canMoveToTarget(xPrev, yPrev, zPrev, x, y, zPrev, getInstanceId()))
				{
					_move.onGeodataPathIndex = -1;
					stopMove(getActingPlayer().getLastServerPosition());
					return false;
				}
			}
			// Prevent player moving on ledges.
			if ((dz > 180) && (distance < 300))
			{
				_move.onGeodataPathIndex = -1;
				stopMove(getActingPlayer().getLastServerPosition());
				return false;
			}
		}
		
		final boolean isFloating = _isFlying || isInsideZone(ZoneId.WATER);
		double delta = (dx * dx) + (dy * dy);
		if ((delta < 10000) && ((dz * dz) > 2500) // close enough, allows error between client and server geodata if it cannot be avoided
			&& !isFloating)
		{
			delta = Math.sqrt(delta);
		}
		else
		{
			delta = Math.sqrt(delta + (dz * dz));
		}
		
		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			final double distPassed = (_stat.getMoveSpeed() * (gameTicks - m._moveTimestamp)) / GameTimeTaskManager.TICKS_PER_SECOND;
			distFraction = distPassed / delta;
		}
		
		// if (Config.DEVELOPER) LOGGER.warning("Move Ticks:" + (gameTicks - m._moveTimestamp) + ", distPassed:" + distPassed + ", distFraction:" + distFraction);
		if (distFraction > 1)
		{
			// Set the position of the Creature to the destination
			super.setXYZ(m._xDestination, m._yDestination, m._zDestination);
		}
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;
			
			// Set the position of the Creature to estimated after parcial move
			super.setXYZ((int) (m._xAccurate), (int) (m._yAccurate), zPrev + (int) ((dz * distFraction) + 0.5));
		}
		revalidateZone(false);
		
		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;
		
		if (distFraction > 1)
		{
			ThreadPool.execute(() -> getAI().notifyEvent(CtrlEvent.EVT_ARRIVED));
			return true;
		}
		
		return false;
	}
	
	public void revalidateZone(boolean force)
	{
		// This function is called too often from movement code.
		if (!force && (calculateDistance3D(_lastZoneValidateLocation) < (isNpc() && !isInCombat() ? Config.MAX_DRIFT_RANGE : 100)))
		{
			return;
		}
		_lastZoneValidateLocation.setXYZ(this);
		
		final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
		if (region != null)
		{
			region.revalidateZones(this);
		}
		else // Precaution. Moved at invalid region?
		{
			World.getInstance().disposeOutOfBoundsObject(this);
		}
	}
	
	/**
	 * Stop movement of the Creature (Called by AI Accessor only).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Delete movement data of the Creature</li>
	 * <li>Set the current position (x,y,z), its current WorldRegion if necessary and its heading</li>
	 * <li>Remove the WorldObject object from _gmList of GmListTable</li>
	 * <li>Remove object from _knownObjects and _knownPlayer of all surrounding WorldRegion Creatures</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T send Server->Client packet StopMove/StopRotation</b></font>
	 * @param loc
	 */
	public void stopMove(Location loc)
	{
		// Delete movement data of the Creature
		_move = null;
		_cursorKeyMovement = false;
		
		// All data are contained in a Location object
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
		broadcastPacket(new StopMove(this));
	}
	
	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * @param showSummonAnimation The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	/**
	 * Target a WorldObject (add the target to the Creature _target, _knownObject and Creature to _KnownObject of the WorldObject).<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * The WorldObject (including Creature) targeted is identified in <b>_target</b> of the Creature.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Set the _target of Creature to WorldObject</li>
	 * <li>If necessary, add WorldObject to _knownObject of the Creature</li>
	 * <li>If necessary, add Creature to _KnownObject of the WorldObject</li>
	 * <li>If object==null, cancel Attak or Cast</li>
	 * </ul>
	 * @param object L2object to target
	 */
	public void setTarget(WorldObject object)
	{
		if ((object != null) && !object.isSpawned())
		{
			_target = null;
			return;
		}
		_target = object;
	}
	
	/**
	 * @return the identifier of the WorldObject targeted or -1.
	 */
	public int getTargetId()
	{
		if (_target != null)
		{
			return _target.getObjectId();
		}
		return 0;
	}
	
	/**
	 * @return the WorldObject targeted or null.
	 */
	public WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * Calculate movement data for a move to location action and add the Creature to movingObjects of GameTimeTaskManager (only called by AI Accessor).<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <b>_move</b> of the Creature.<br>
	 * The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<br>
	 * All Creature in movement are identified in <b>movingObjects</b> of GameTimeTaskManager that will call the updatePosition method of those Creature each 0.1s.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Get current position of the Creature</li>
	 * <li>Calculate distance (dx,dy) between current position and destination including offset</li>
	 * <li>Create and Init a MoveData object</li>
	 * <li>Set the Creature _move object to MoveData object</li>
	 * <li>Add the Creature to movingObjects of the GameTimeTaskManager</li>
	 * <li>Create a task to notify the AI that Creature arrives at a check point of the movement</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T send Server->Client packet MoveToPawn/CharMoveToLocation.</b></font><br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>AI : onIntentionMoveTo(Location), onIntentionPickUp(WorldObject), onIntentionInteract(WorldObject)</li>
	 * <li>FollowTask</li>
	 * </ul>
	 * @param xValue The X position of the destination
	 * @param yValue The Y position of the destination
	 * @param zValue The Y position of the destination
	 * @param offsetValue The size of the interaction area of the Creature targeted
	 */
	public void moveToLocation(int xValue, int yValue, int zValue, int offsetValue)
	{
		// Get the Move Speed of the Creature
		final double speed = _stat.getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			return;
		}
		
		int x = xValue;
		int y = yValue;
		int z = zValue;
		int offset = offsetValue;
		
		// Get current position of the Creature
		final int curX = getX();
		final int curY = getY();
		final int curZ = getZ();
		
		// Calculate distance (dx,dy) between current position and destination
		// TODO: improve Z axis move/follow support when dx,dy are small compared to dz
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.hypot(dx, dy);
		
		final boolean verticalMovementOnly = _isFlying && (distance == 0) && (dz != 0);
		if (verticalMovementOnly)
		{
			distance = Math.abs(dz);
		}
		
		// Make water move short and use no geodata checks for swimming chars distance in a click can easily be over 3000.
		final boolean isInWater = isInsideZone(ZoneId.WATER);
		if (isInWater && (distance > 700))
		{
			final double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.hypot(dx, dy);
		}
		
		// @formatter:off
		// Define movement angles needed
		// ^
		// |    X (x,y)
		// |   /
		// |  / distance
		// | /
		// |/ angle
		// X ---------->
		// (curx,cury)
		// @formatter:on
		
		double cos;
		double sin;
		
		// Check if a movement offset is defined or no distance to go through
		if ((offset > 0) || (distance < 1))
		{
			// approximation for moving closer when z coordinates are different
			// TODO: handle Z axis movement better
			offset -= Math.abs(dz);
			if (offset < 5)
			{
				offset = 5;
			}
			
			// If no distance to go through, the movement is canceled
			if ((distance < 1) || ((distance - offset) <= 0))
			{
				// Notify the AI that the Creature is arrived at destination
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				return;
			}
			
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
			distance -= (offset - 5); // due to rounding error, we have to move a bit closer to be in range
			
			// Calculate the new destination with offset included
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
		}
		
		// Create and Init a MoveData object
		final MoveData m = new MoveData();
		
		// GEODATA MOVEMENT CHECKS AND PATHFINDING
		m.onGeodataPathIndex = -1; // Initialize not on geodata path
		m.disregardingGeodata = false;
		if (!_isFlying && !isInWater && !isVehicle() && !_cursorKeyMovement)
		{
			final boolean isInVehicle = isPlayer() && (getActingPlayer().getVehicle() != null);
			if (isInVehicle)
			{
				m.disregardingGeodata = true;
			}
			
			// Movement checks.
			if (Config.PATHFINDING && !(this instanceof QuestGuardInstance))
			{
				final double originalDistance = distance;
				final int originalX = x;
				final int originalY = y;
				final int originalZ = z;
				final int gtx = (originalX - World.WORLD_X_MIN) >> 4;
				final int gty = (originalY - World.WORLD_Y_MIN) >> 4;
				if (isOnGeodataPath())
				{
					try
					{
						if ((gtx == _move.geoPathGtx) && (gty == _move.geoPathGty))
						{
							return;
						}
						_move.onGeodataPathIndex = -1; // Set not on geodata path.
					}
					catch (NullPointerException e)
					{
						// nothing
					}
				}
				
				if (!isInVehicle // Not in vehicle.
					&& !(isPlayer() && (distance > 3000)) // Should be able to click far away and move.
					&& !(isMonster() && (Math.abs(dz) > 100)) // Monsters can move on ledges.
					&& !(((curZ - z) > 300) && (distance < 300))) // Prohibit correcting destination if character wants to fall.
				{
					// location different if destination wasn't reached (or just z coord is different)
					final Location destiny = GeoEngine.getInstance().getValidLocation(curX, curY, curZ, x, y, z, getInstanceId());
					x = destiny.getX();
					y = destiny.getY();
					z = destiny.getZ();
					dx = x - curX;
					dy = y - curY;
					dz = z - curZ;
					distance = verticalMovementOnly ? Math.pow(dz, 2) : Math.hypot(dx, dy);
				}
				
				// Pathfinding checks.
				if (((originalDistance - distance) > 30) && !isAfraid() && !isInVehicle)
				{
					// Path calculation -- overrides previous movement check
					m.geoPath = GeoEngine.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, getInstanceId());
					if ((m.geoPath == null) || (m.geoPath.size() < 2)) // No path found
					{
						if (isPlayer() && !_isFlying && !isInWater)
						{
							return;
						}
						// if (!isPlayable() && !isMinion() && (Math.abs(z - curZ) > 140))
						// {
						// return;
						// }
						// if (isSummon() && !((Summon) this).getFollowStatus())
						// {
						// return;
						// }
						
						m.disregardingGeodata = true;
						
						x = originalX;
						y = originalY;
						z = originalZ;
						distance = originalDistance;
					}
					else
					{
						m.onGeodataPathIndex = 0; // on first segment
						m.geoPathGtx = gtx;
						m.geoPathGty = gty;
						m.geoPathAccurateTx = originalX;
						m.geoPathAccurateTy = originalY;
						x = m.geoPath.get(m.onGeodataPathIndex).getX();
						y = m.geoPath.get(m.onGeodataPathIndex).getY();
						z = m.geoPath.get(m.onGeodataPathIndex).getZ();
						dx = x - curX;
						dy = y - curY;
						dz = z - curZ;
						distance = verticalMovementOnly ? Math.pow(dz, 2) : Math.hypot(dx, dy);
						sin = dy / distance;
						cos = dx / distance;
					}
				}
			}
			
			// If no distance to go through, the movement is canceled
			if ((distance < 1) && (Config.PATHFINDING || isPlayable()))
			{
				if (isSummon())
				{
					// Do not break following owner.
					if (getAI().getFollowTarget() != getActingPlayer())
					{
						((Summon) this).setFollowStatus(false);
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					}
				}
				else
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				}
				return;
			}
		}
		
		// Apply Z distance for flying or swimming for correct timing calculations
		if ((_isFlying || isInWater) && !verticalMovementOnly)
		{
			distance = Math.hypot(distance, dz);
		}
		
		// Calculate the number of ticks between the current position and the destination
		// One tick added for rounding reasons
		final int ticksToMove = 1 + (int) ((GameTimeTaskManager.TICKS_PER_SECOND * distance) / speed);
		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z; // this is what was requested from client
		
		// Calculate and set the heading of the Creature
		m._heading = 0; // initial value for coordinate sync
		// Does not broke heading on vertical movements
		if (!verticalMovementOnly)
		{
			setHeading(Util.calculateHeadingFrom(cos, sin));
		}
		
		m._moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the Creature _move object to MoveData object
		_move = m;
		
		// Add the Creature to movingObjects of the GameTimeTaskManager
		// The GameTimeTaskManager manage objects movement
		GameTimeTaskManager.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that Creature arrives at a check point of the movement
		if ((ticksToMove * GameTimeTaskManager.MILLIS_IN_TICK) > 3000)
		{
			ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive to destination by GameTimeTaskManager
	}
	
	public boolean moveToNextRoutePoint()
	{
		if (!isOnGeodataPath())
		{
			// Cancel the move action
			_move = null;
			return false;
		}
		
		// Get the Move Speed of the Creature
		final double speed = _stat.getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			// Cancel the move action
			_move = null;
			return false;
		}
		
		final MoveData md = _move;
		if (md == null)
		{
			return false;
		}
		
		// Get current position of the Creature
		final int curX = getX();
		final int curY = getY();
		
		// Create and Init a MoveData object
		final MoveData m = new MoveData();
		
		// Update MoveData object
		m.onGeodataPathIndex = md.onGeodataPathIndex + 1; // next segment
		m.geoPath = md.geoPath;
		m.geoPathGtx = md.geoPathGtx;
		m.geoPathGty = md.geoPathGty;
		m.geoPathAccurateTx = md.geoPathAccurateTx;
		m.geoPathAccurateTy = md.geoPathAccurateTy;
		if (md.onGeodataPathIndex == (md.geoPath.size() - 2))
		{
			m._xDestination = md.geoPathAccurateTx;
			m._yDestination = md.geoPathAccurateTy;
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		else
		{
			m._xDestination = md.geoPath.get(m.onGeodataPathIndex).getX();
			m._yDestination = md.geoPath.get(m.onGeodataPathIndex).getY();
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		
		final double distance = Math.hypot(m._xDestination - curX, m._yDestination - curY);
		// Calculate and set the heading of the Creature
		if (distance != 0)
		{
			setHeading(Util.calculateHeadingFrom(curX, curY, m._xDestination, m._yDestination));
		}
		
		// Calculate the number of ticks between the current position and the destination
		// One tick added for rounding reasons
		final int ticksToMove = 1 + (int) ((GameTimeTaskManager.TICKS_PER_SECOND * distance) / speed);
		m._heading = 0; // initial value for coordinate sync
		m._moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the Creature _move object to MoveData object
		_move = m;
		
		// Add the Creature to movingObjects of the GameTimeTaskManager
		// The GameTimeTaskManager manage objects movement
		GameTimeTaskManager.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that Creature arrives at a check point of the movement
		if ((ticksToMove * GameTimeTaskManager.MILLIS_IN_TICK) > 3000)
		{
			ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}
		
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeTaskManager
		
		// Send a Server->Client packet CharMoveToLocation to the actor and all PlayerInstance in its _knownPlayers
		broadcastMoveToLocation();
		return true;
	}
	
	public boolean validateMovementHeading(int heading)
	{
		final MoveData m = _move;
		if (m == null)
		{
			return true;
		}
		
		boolean result = true;
		if (m._heading != heading)
		{
			result = (m._heading == 0); // initial value or false
			m._heading = heading;
		}
		
		return result;
	}
	
	/**
	 * Check if this object is inside the given 2D radius around the given point.
	 * @param loc Location of the target
	 * @param radius the radius around the target
	 * @return true if the Creature is inside the radius.
	 */
	public boolean isInsideRadius2D(ILocational loc, int radius)
	{
		return isInsideRadius2D(loc.getX(), loc.getY(), loc.getZ(), radius);
	}
	
	/**
	 * Check if this object is inside the given 2D radius around the given point.
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @param radius the radius around the target
	 * @return true if the Creature is inside the radius.
	 */
	public boolean isInsideRadius2D(int x, int y, int z, int radius)
	{
		return calculateDistanceSq2D(x, y, z) < (radius * radius);
	}
	
	/**
	 * Check if this object is inside the given 3D radius around the given point.
	 * @param loc Location of the target
	 * @param radius the radius around the target
	 * @return true if the Creature is inside the radius.
	 */
	public boolean isInsideRadius3D(ILocational loc, int radius)
	{
		return isInsideRadius3D(loc.getX(), loc.getY(), loc.getZ(), radius);
	}
	
	/**
	 * Check if this object is inside the given 3D radius around the given point.
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @param radius the radius around the target
	 * @return true if the Creature is inside the radius.
	 */
	public boolean isInsideRadius3D(int x, int y, int z, int radius)
	{
		return calculateDistanceSq3D(x, y, z) < (radius * radius);
	}
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>PlayerInstance</li>
	 * @return True if arrows are available.
	 */
	protected boolean checkAndEquipArrows()
	{
		return true;
	}
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>PlayerInstance</li>
	 * @return True if bolts are available.
	 */
	protected boolean checkAndEquipBolts()
	{
		return true;
	}
	
	/**
	 * Add Exp and Sp to the Creature.<br>
	 * <br>
	 * <b><u>Overridden in</u>:</b>
	 * <li>PlayerInstance</li>
	 * <li>PetInstance</li><br>
	 * @param addToExp
	 * @param addToSp
	 */
	public void addExpAndSp(double addToExp, double addToSp)
	{
		// Dummy method (overridden by players and pets)
	}
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>PlayerInstance</li>
	 * @return the active weapon instance (always equipped in the right hand).
	 */
	public abstract ItemInstance getActiveWeaponInstance();
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>PlayerInstance</li>
	 * @return the active weapon item (always equipped in the right hand).
	 */
	public abstract Weapon getActiveWeaponItem();
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>PlayerInstance</li>
	 * @return the secondary weapon instance (always equipped in the left hand).
	 */
	public abstract ItemInstance getSecondaryWeaponInstance();
	
	/**
	 * <b><u>Overridden in</u>:</b>
	 * <li>PlayerInstance</li>
	 * @return the secondary {@link Item} item (always equipped in the left hand).
	 */
	public abstract Item getSecondaryWeaponItem();
	
	/**
	 * Manage hit process (called by Hit Task).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a PlayerInstance)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are PlayerInstance</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
	 * </ul>
	 * @param target The Creature targeted
	 * @param damageValue Number of HP to reduce
	 * @param crit True if hit is critical
	 * @param miss True if hit is missed
	 * @param soulshot True if SoulShot are charged
	 * @param shld True if shield is efficient
	 */
	public void onHitTimer(Creature target, int damageValue, boolean crit, boolean miss, boolean soulshot, byte shld)
	{
		// If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL
		// and send a Server->Client packet ActionFailed (if attacker is a PlayerInstance)
		if ((target == null) || isAlikeDead())
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		// Check if fake players should aggro each other.
		if (isFakePlayer() && !Config.FAKE_PLAYER_AGGRO_FPC && target.isFakePlayer())
		{
			return;
		}
		
		if ((isNpc() && target.isAlikeDead()) || target.isDead() || (!isInSurroundingRegion(target) && !isDoor()))
		{
			// getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
			// Some times attack is processed but target die before the hit
			// So we need to recharge shot for next attack
			rechargeShots(true, false);
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (miss)
		{
			// Notify target AI
			if (target.hasAI())
			{
				target.getAI().notifyEvent(CtrlEvent.EVT_EVADED, this);
			}
			notifyAttackAvoid(target, false);
		}
		else
		{
			// If we didn't miss the hit, discharge the shoulshots, if any
			setChargedShot(ShotType.SOULSHOTS, false);
		}
		
		// Send message about damage/crit or miss
		int damage = damageValue;
		sendDamageMessage(target, damage, false, crit, miss);
		
		// Check Raidboss attack Creature will be petrified if attacking a raid that's more than 8 levels lower
		if (target.isRaid() && target.giveRaidCurse() && !Config.RAID_DISABLE_CURSE && (getLevel() > (target.getLevel() + 8)))
		{
			final Skill skill = CommonSkill.RAID_CURSE2.getSkill();
			if (skill != null)
			{
				abortAttack();
				abortCast();
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				skill.applyEffects(target, this);
			}
			else
			{
				LOGGER.warning("Skill 4515 at level 1 is missing in DP.");
			}
			damage = 0; // prevents messing up drop calculation
		}
		
		// If Creature target is a PlayerInstance, send a system message
		if (target.isPlayer())
		{
			final PlayerInstance enemy = target.getActingPlayer();
			enemy.getAI().clientStartAutoAttack();
		}
		
		if (!miss && (damage > 0))
		{
			final Weapon weapon = getActiveWeaponItem();
			final boolean isBow = ((weapon != null) && ((weapon.getItemType() == WeaponType.BOW) || (weapon.getItemType() == WeaponType.CROSSBOW)));
			int reflectedDamage = 0;
			if (!isBow && !target.isInvul()) // Do not reflect if weapon is of type bow or target is invunlerable
			{
				// quick fix for no drop from raid if boss attack high-level char with damage reflection
				if (!target.isRaid() || (getActingPlayer() == null) || (getActingPlayer().getLevel() <= (target.getLevel() + 8)))
				{
					// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
					final double reflectPercent = target.getStat().calcStat(Stat.REFLECT_DAMAGE_PERCENT, 0, null, null);
					if (reflectPercent > 0)
					{
						reflectedDamage = (int) ((reflectPercent / 100.) * damage);
						if (reflectedDamage > target.getMaxHp())
						{
							reflectedDamage = target.getMaxHp();
						}
					}
				}
			}
			
			// reduce targets HP
			target.reduceCurrentHp(damage, this, null);
			target.notifyDamageReceived(damage, this, null, crit, false);
			if (reflectedDamage > 0)
			{
				reduceCurrentHp(reflectedDamage, target, true, false, null);
				notifyDamageReceived(reflectedDamage, target, null, crit, false);
			}
			
			if (!isBow) // Do not absorb if weapon is of type bow
			{
				// Absorb HP from the damage inflicted
				double absorbPercent = _stat.calcStat(Stat.ABSORB_DAMAGE_PERCENT, 0, null, null);
				if (absorbPercent > 0)
				{
					final int maxCanAbsorb = (int) (_stat.getMaxRecoverableHp() - _status.getCurrentHp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb; // Can't absord more than max hp
					}
					
					if (absorbDamage > 0)
					{
						setCurrentHp(_status.getCurrentHp() + absorbDamage);
					}
				}
				
				// Absorb MP from the damage inflicted
				absorbPercent = _stat.calcStat(Stat.ABSORB_MANA_DAMAGE_PERCENT, 0, null, null);
				if (absorbPercent > 0)
				{
					final int maxCanAbsorb = (int) (_stat.getMaxRecoverableMp() - _status.getCurrentMp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb; // Can't absord more than max hp
					}
					
					if (absorbDamage > 0)
					{
						setCurrentMp(_status.getCurrentMp() + absorbDamage);
					}
				}
			}
			
			// Notify AI with EVT_ATTACKED
			if (target.hasAI())
			{
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
			}
			getAI().clientStartAutoAttack();
			if (isSummon())
			{
				final PlayerInstance owner = ((Summon) this).getOwner();
				if (owner != null)
				{
					owner.getAI().clientStartAutoAttack();
				}
			}
			
			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
			{
				target.breakAttack();
				target.breakCast();
			}
			
			if (_triggerSkills != null)
			{
				for (OptionsSkillHolder holder : _triggerSkills.values())
				{
					if (((!crit && (holder.getSkillType() == OptionsSkillType.ATTACK)) || ((holder.getSkillType() == OptionsSkillType.CRITICAL) && crit)) && (Rnd.get(100) < holder.getChance()))
					{
						makeTriggerCast(holder.getSkill(), target);
					}
				}
			}
			// Launch weapon onCritical Special ability effect if available
			if (crit && (weapon != null))
			{
				weapon.castOnCriticalSkill(this, target);
			}
		}
		// Recharge any active auto-soulshot tasks for current creature.
		rechargeShots(true, false);
	}
	
	/**
	 * Break an attack and send Server->Client ActionFailed packet and a System Message to the Creature.
	 */
	public void breakAttack()
	{
		if (isAttackingNow())
		{
			// Abort the attack of the Creature and send Server->Client ActionFailed packet
			abortAttack();
			if (isPlayer())
			{
				// Send a system message
				sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
			}
		}
	}
	
	/**
	 * Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature.
	 */
	public void breakCast()
	{
		// damage can only cancel magical & static skills
		if (_isCastingNow && canAbortCast() && (_lastSkillCast != null) && (_lastSkillCast.isMagic() || _lastSkillCast.isStatic()))
		{
			// Abort the cast of the Creature and send Server->Client MagicSkillCanceld/ActionFailed packet.
			abortCast();
			
			if (isPlayer())
			{
				// Send a system message
				sendPacket(SystemMessageId.YOUR_CASTING_HAS_BEEN_INTERRUPTED);
			}
		}
	}
	
	/**
	 * Reduce the arrow number of the Creature.<br>
	 * <br>
	 * <b><u>Overridden in</u>:</b>
	 * <li>PlayerInstance</li><br>
	 * @param bolts
	 */
	protected void reduceArrowCount(boolean bolts)
	{
		// default is to do nothing
	}
	
	/**
	 * Manage Forced attack (shift + select target).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If Creature or target is in a town area, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed</li>
	 * <li>If target is confused, send a Server->Client packet ActionFailed</li>
	 * <li>If Creature is a ArtefactInstance, send a Server->Client packet ActionFailed</li>
	 * <li>Send a Server->Client packet MyTargetSelected to start attack and Notify AI with AI_INTENTION_ATTACK</li>
	 * </ul>
	 * @param player The PlayerInstance to attack
	 */
	@Override
	public void onForcedAttack(PlayerInstance player)
	{
		if (isInsidePeaceZone(player))
		{
			// If Creature or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInOlympiadMode() && (player.getTarget() != null) && player.getTarget().isPlayable())
		{
			PlayerInstance target = null;
			final WorldObject object = player.getTarget();
			if ((object != null) && object.isPlayable())
			{
				target = object.getActingPlayer();
			}
			
			if ((target == null) || (target.isInOlympiadMode() && (!player.isOlympiadStart() || (player.getOlympiadGameId() != target.getOlympiadGameId()))))
			{
				// if PlayerInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		if ((player.getTarget() != null) && !player.getTarget().canBeAttacked() && !player.getAccessLevel().allowPeaceAttack())
		{
			// If target is not attackable, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isConfused())
		{
			// If target is confused, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// GeoData Los Check or dz > 1000
		if (!GeoEngine.getInstance().canSeeTarget(player, this))
		{
			player.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.getBlockCheckerArena() != -1)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// Notify AI with AI_INTENTION_ATTACK
		player.getAI().setIntention(AI_INTENTION_ATTACK, this);
	}
	
	/**
	 * @param attacker
	 * @return True if inside peace zone.
	 */
	public boolean isInsidePeaceZone(PlayerInstance attacker)
	{
		return isInsidePeaceZone(attacker, this);
	}
	
	public boolean isInsidePeaceZone(PlayerInstance attacker, WorldObject target)
	{
		return (!attacker.getAccessLevel().allowPeaceAttack() && isInsidePeaceZone((WorldObject) attacker, target));
	}
	
	public boolean isInsidePeaceZone(WorldObject attacker, WorldObject target)
	{
		if ((target == null) || !((target.isPlayable() || target.isFakePlayer()) && attacker.isPlayable()))
		{
			return false;
		}
		if (InstanceManager.getInstance().getInstance(getInstanceId()).isPvP())
		{
			return false;
		}
		
		if (TerritoryWarManager.PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE && TerritoryWarManager.getInstance().isTWInProgress() && target.isPlayer() && target.getActingPlayer().isCombatFlagEquipped())
		{
			return false;
		}
		
		if (Config.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			// allows red to be attacked and red to attack flagged players
			if ((target.getActingPlayer() != null) && (target.getActingPlayer().getKarma() > 0))
			{
				return false;
			}
			if ((attacker.getActingPlayer() != null) && (attacker.getActingPlayer().getKarma() > 0) && (target.getActingPlayer() != null) && (target.getActingPlayer().getPvpFlag() > 0))
			{
				return false;
			}
		}
		
		return (target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE));
	}
	
	/**
	 * @return true if this character is inside an active grid.
	 */
	public boolean isInActiveRegion()
	{
		final WorldRegion region = getWorldRegion();
		return ((region != null) && (region.isActive()));
	}
	
	/**
	 * @return True if the Creature has a Party in progress.
	 */
	public boolean isInParty()
	{
		return false;
	}
	
	/**
	 * @return the Party object of the Creature.
	 */
	public Party getParty()
	{
		return null;
	}
	
	/**
	 * @return the Attack Speed of the Creature (delay (in milliseconds) before next attack).
	 */
	public int calculateTimeBetweenAttacks()
	{
		return (int) (500000 / _stat.getPAtkSpd());
	}
	
	/**
	 * @param weapon
	 * @return the Reuse Time of Attack (used for bow delay)
	 */
	public int calculateReuseTime(Weapon weapon)
	{
		if (isTransformed())
		{
			switch (getAttackType())
			{
				case BOW:
				{
					return (int) ((1500 * 333 * _stat.getWeaponReuseModifier(null)) / _stat.getPAtkSpd());
				}
				case CROSSBOW:
				{
					return (int) ((1200 * 333 * _stat.getWeaponReuseModifier(null)) / _stat.getPAtkSpd());
				}
			}
		}
		
		if ((weapon == null) || (weapon.getReuseDelay() == 0))
		{
			return 0;
		}
		
		return (int) ((weapon.getReuseDelay() * 333) / _stat.getPAtkSpd());
	}
	
	/**
	 * Add a skill to the Creature _skills and its Func objects to the calculator set of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * All skills own by a Creature are identified in <b>_skills</b><br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of Creature calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the Creature</li>
	 * </ul>
	 * <br>
	 * <b><u>Overridden in</u>:</b>
	 * <ul>
	 * <li>PlayerInstance : Save update in the character_skills table of the database</li>
	 * </ul>
	 * @param newSkill The Skill to add to the Creature
	 * @return The Skill replaced or null if just added a new Skill
	 */
	@Override
	public Skill addSkill(Skill newSkill)
	{
		Skill oldSkill = null;
		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			// If an old skill has been replaced, remove all its Func objects
			if (oldSkill != null)
			{
				removeStatsOwner(oldSkill);
				
				if (oldSkill.isPassive())
				{
					stopSkillEffects(SkillFinishType.NORMAL, oldSkill.getId());
				}
			}
			// Add Func objects of newSkill to the calculator set of the Creature
			addStatFuncs(newSkill.getStatFuncs(null, this));
			if (newSkill.isPassive())
			{
				newSkill.applyEffects(this, this, false, true, false, 0);
			}
		}
		return oldSkill;
	}
	
	public Skill removeSkill(Skill skill, boolean cancelEffect)
	{
		return (skill != null) ? removeSkill(skill.getId(), cancelEffect) : null;
	}
	
	public Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}
	
	public Skill removeSkill(int skillId, boolean cancelEffect)
	{
		// Remove the skill from the Creature _skills
		final Skill oldSkill = _skills.remove(skillId);
		// Remove all its Func objects from the Creature calculator set
		if (oldSkill != null)
		{
			// Stop casting if this skill is used right now
			if ((_lastSkillCast != null) && _isCastingNow && (oldSkill.getId() == _lastSkillCast.getId()))
			{
				abortCast();
			}
			if ((_lastSimultaneousSkillCast != null) && _isCastingSimultaneouslyNow && (oldSkill.getId() == _lastSimultaneousSkillCast.getId()))
			{
				abortCast();
			}
			
			// Stop effects.
			if (cancelEffect || oldSkill.isToggle() || oldSkill.isPassive())
			{
				removeStatsOwner(oldSkill);
				stopSkillEffects(SkillFinishType.REMOVED, oldSkill.getId());
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * All skills own by a Creature are identified in <b>_skills</b> the Creature
	 * @return all skills own by the Creature in a table of Skill.
	 */
	public Collection<Skill> getAllSkills()
	{
		return _skills.values();
	}
	
	/**
	 * @return the map containing this character skills.
	 */
	@Override
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Return the level of a skill owned by the Creature.
	 * @param skillId The identifier of the Skill whose level must be returned
	 * @return The level of the Skill identified by skillId
	 */
	@Override
	public int getSkillLevel(int skillId)
	{
		final Skill skill = getKnownSkill(skillId);
		return (skill == null) ? 0 : skill.getLevel();
	}
	
	/**
	 * @param skillId The identifier of the Skill to check the knowledge
	 * @return the skill from the known skill.
	 */
	@Override
	public Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
	
	/**
	 * Return the number of buffs affecting this Creature.
	 * @return The number of Buffs affecting this Creature
	 */
	public int getBuffCount()
	{
		return _effectList.getBuffCount();
	}
	
	public int getDanceCount()
	{
		return _effectList.getDanceCount();
	}
	
	/**
	 * Manage the magic skill launching task (MP, HP, Item consumation...) and display the magic skill animation on client.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Send a Server->Client packet MagicSkillLaunched (to display magic skill animation) to all PlayerInstance of Creature _knownPlayers</li>
	 * <li>Consumme MP, HP and Item if necessary</li>
	 * <li>Send a Server->Client packet StatusUpdate with MP modification to the PlayerInstance</li>
	 * <li>Launch the magic skill in order to calculate its effects</li>
	 * <li>If the skill type is PDAM, notify the AI of the target with AI_INTENTION_ATTACK</li>
	 * <li>Notify the AI of the Creature with EVT_FINISH_CASTING</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: A magic skill casting MUST BE in progress</b></font>
	 * @param mut
	 */
	public void onMagicLaunchedTimer(MagicUseTask mut)
	{
		final Skill skill = mut.getSkill();
		final WorldObject[] targets = mut.getTargets();
		if ((skill == null) || (targets == null))
		{
			abortCast();
			return;
		}
		
		if (targets.length == 0)
		{
			switch (skill.getTargetType())
			{
				// only AURA-type skills can be cast without target
				case AURA:
				case FRONT_AURA:
				case BEHIND_AURA:
				case AURA_CORPSE_MOB:
				case AURA_FRIENDLY:
				{
					break;
				}
				default:
				{
					abortCast();
					return;
				}
			}
		}
		
		// Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
		int escapeRange = 0;
		if (skill.getEffectRange() > escapeRange)
		{
			escapeRange = skill.getEffectRange();
		}
		else if ((skill.getCastRange() < 0) && (skill.getAffectRange() > 80))
		{
			escapeRange = skill.getAffectRange();
		}
		
		if ((targets.length > 0) && (escapeRange > 0))
		{
			int skipRange = 0;
			int skipLOS = 0;
			int skipPeaceZone = 0;
			final List<WorldObject> targetList = new ArrayList<>();
			for (WorldObject target : targets)
			{
				if (target.isCreature())
				{
					if (!isInsideRadius3D(target.getX(), target.getY(), target.getZ(), escapeRange + _template.getCollisionRadius()))
					{
						skipRange++;
						continue;
					}
					
					// Healing party members should ignore LOS.
					if (((skill.getTargetType() != TargetType.PARTY) || !skill.hasEffectType(EffectType.HEAL)) //
						&& (mut.getSkillTime() > 550) && !GeoEngine.getInstance().canSeeTarget(this, target))
					{
						skipLOS++;
						continue;
					}
					
					if (skill.isBad())
					{
						if (isPlayer())
						{
							if (((Creature) target).isInsidePeaceZone(getActingPlayer()))
							{
								skipPeaceZone++;
								continue;
							}
						}
						else
						{
							if (((Creature) target).isInsidePeaceZone(this, target))
							{
								skipPeaceZone++;
								continue;
							}
						}
					}
					targetList.add(target);
				}
			}
			if (targetList.isEmpty())
			{
				if (isPlayer())
				{
					if (skipRange > 0)
					{
						sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
					}
					else if (skipLOS > 0)
					{
						sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
					}
					else if (skipPeaceZone > 0)
					{
						sendPacket(SystemMessageId.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
					}
				}
				abortCast();
				return;
			}
			mut.setTargets(targetList.toArray(new WorldObject[targetList.size()]));
		}
		
		// Ensure that a cast is in progress
		// Check if player is using fake death.
		// Static skills can be used while faking death.
		if ((mut.isSimultaneous() && !_isCastingSimultaneouslyNow) || (!mut.isSimultaneous() && !_isCastingNow) || (isAlikeDead() && !skill.isStatic()))
		{
			// now cancels both, simultaneous and normal
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		mut.setPhase(2);
		if (mut.getSkillTime() == 0)
		{
			onMagicHitTimer(mut);
		}
		else
		{
			_skillCast = ThreadPool.schedule(mut, 400);
		}
	}
	
	// Runs in the end of skill casting
	public void onMagicHitTimer(MagicUseTask mut)
	{
		final Skill skill = mut.getSkill();
		final WorldObject[] targets = mut.getTargets();
		if ((skill == null) || (targets == null))
		{
			abortCast();
			return;
		}
		
		try
		{
			// Go through targets table
			for (WorldObject tgt : targets)
			{
				if (tgt.isPlayable())
				{
					if (isPlayer() && tgt.isSummon())
					{
						((Summon) tgt).updateAndBroadcastStatus(1);
					}
				}
				else if (isPlayable() && tgt.isAttackable())
				{
					final Creature target = (Creature) tgt;
					if (skill.getEffectPoint() > 0)
					{
						((Attackable) target).reduceHate(this, skill.getEffectPoint());
					}
					else if (skill.getEffectPoint() < 0)
					{
						((Attackable) target).addDamageHate(this, 0, -skill.getEffectPoint());
					}
				}
			}
			
			rechargeShots(skill.useSoulShot(), skill.useSpiritShot());
			
			final StatusUpdate su = new StatusUpdate(this);
			boolean isSendStatus = false;
			
			// Consume MP of the Creature and Send the Server->Client packet StatusUpdate with current HP and MP to all other PlayerInstance to inform
			final double mpConsume = _stat.getMpConsume(skill);
			if (mpConsume > 0)
			{
				if (mpConsume > _status.getCurrentMp())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_MP);
					abortCast();
					return;
				}
				
				_status.reduceMp(mpConsume);
				su.addAttribute(StatusUpdate.CUR_MP, (int) _status.getCurrentMp());
				isSendStatus = true;
			}
			
			// Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other PlayerInstance to inform
			if (skill.getHpConsume() > 0)
			{
				final double consumeHp = skill.getHpConsume();
				if (consumeHp >= _status.getCurrentHp())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_HP);
					abortCast();
					return;
				}
				
				_status.reduceHp(consumeHp, this, true);
				su.addAttribute(StatusUpdate.CUR_HP, (int) _status.getCurrentHp());
				isSendStatus = true;
			}
			
			// Send a Server->Client packet StatusUpdate with MP modification to the PlayerInstance
			if (isSendStatus)
			{
				sendPacket(su);
			}
			
			if (isPlayer())
			{
				// Consume Charges
				if (skill.getChargeConsume() > 0)
				{
					getActingPlayer().decreaseCharges(skill.getChargeConsume());
				}
				
				// Consume Souls if necessary
				if ((skill.getMaxSoulConsumeCount() > 0) && !getActingPlayer().decreaseSouls(skill.getMaxSoulConsumeCount()))
				{
					abortCast();
					return;
				}
			}
			
			// Launch the magic skill in order to calculate its effects
			callSkill(mut.getSkill(), mut.getTargets());
		}
		catch (NullPointerException e)
		{
			LOGGER.log(Level.WARNING, "", e);
		}
		
		if (mut.getSkillTime() > 0)
		{
			mut.setCount(mut.getCount() + 1);
		}
		
		mut.setPhase(3);
		if (mut.getSkillTime() == 0)
		{
			onMagicFinalizer(mut);
		}
		else
		{
			if (mut.isSimultaneous())
			{
				_skillCast2 = ThreadPool.schedule(mut, 0);
			}
			else
			{
				_skillCast = ThreadPool.schedule(mut, 0);
			}
		}
	}
	
	// Runs after skillTime
	public void onMagicFinalizer(MagicUseTask mut)
	{
		if (mut.isSimultaneous())
		{
			_skillCast2 = null;
			setCastingSimultaneouslyNow(false);
			return;
		}
		
		// Cleanup
		_skillCast = null;
		_castInterruptTime = 0;
		
		// On each repeat recharge shots before cast.
		if (mut.getCount() > 0)
		{
			rechargeShots(mut.getSkill().useSoulShot(), mut.getSkill().useSpiritShot());
		}
		
		// Stop casting
		setCastingNow(false);
		setCastingSimultaneouslyNow(false);
		
		final Skill skill = mut.getSkill();
		final WorldObject target = mut.getTargets().length > 0 ? mut.getTargets()[0] : null;
		
		// Attack target after skill use
		if ((skill.nextActionIsAttack()) && (_target != this) && (target != null) && (_target == target) && _target.isCreature() && target.canBeAttacked())
		{
			final IntentionCommand nextIntention = getAI().getNextIntention();
			if ((nextIntention == null) || (nextIntention.getCtrlIntention() != CtrlIntention.AI_INTENTION_MOVE_TO))
			{
				if (isPlayer())
				{
					final PlayerInstance currPlayer = getActingPlayer();
					final SkillUseHolder currSkill = currPlayer.getCurrentSkill();
					if ((currSkill == null) || !currSkill.isShiftPressed())
					{
						getAI().setIntention(AI_INTENTION_ATTACK, target);
					}
				}
				else
				{
					getAI().setIntention(AI_INTENTION_ATTACK, target);
				}
			}
		}
		
		if (skill.isBad() && (skill.getTargetType() != TargetType.UNLOCKABLE))
		{
			getAI().clientStartAutoAttack();
		}
		
		// Notify the AI of the Creature with EVT_FINISH_CASTING
		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
		
		// Notify DP Scripts
		notifyQuestEventSkillFinished(skill, target);
		
		// If character is a player, then wipe their current cast state and check if a skill is queued.
		// If there is a queued skill, launch it and wipe the queue.
		if (isPlayer())
		{
			final PlayerInstance currPlayer = getActingPlayer();
			final SkillUseHolder queuedSkill = currPlayer.getQueuedSkill();
			currPlayer.setCurrentSkill(null, false, false);
			if (queuedSkill != null)
			{
				currPlayer.setQueuedSkill(null, false, false);
				
				// DON'T USE : Recursive call to useMagic() method
				// currPlayer.useMagic(queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed());
				ThreadPool.execute(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
			}
		}
		
		if (isChanneling())
		{
			getSkillChannelizer().stopChanneling();
		}
	}
	
	// Quest event ON_SPELL_FNISHED
	protected void notifyQuestEventSkillFinished(Skill skill, WorldObject target)
	{
	}
	
	/**
	 * Launch the magic skill and calculate its effects on each target contained in the targets table.
	 * @param skill The Skill to use
	 * @param targets The table of WorldObject targets
	 */
	public void callSkill(Skill skill, WorldObject[] targets)
	{
		try
		{
			final Weapon activeWeapon = getActiveWeaponItem();
			
			// Check if the toggle skill effects are already in progress on the Creature
			if (skill.isToggle() && isAffectedBySkill(skill.getId()))
			{
				return;
			}
			
			// Initial checks
			for (WorldObject obj : targets)
			{
				if ((obj == null) || !obj.isCreature())
				{
					continue;
				}
				
				final Creature target = (Creature) obj;
				// Check raid monster attack and check buffing characters who attack raid monsters.
				Creature targetsAttackTarget = null;
				Creature targetsCastTarget = null;
				if (target.hasAI())
				{
					targetsAttackTarget = target.getAI().getAttackTarget();
					targetsCastTarget = target.getAI().getCastTarget();
				}
				
				if (!Config.RAID_DISABLE_CURSE && ((target.isRaid() && target.giveRaidCurse() && (getLevel() > (target.getLevel() + 8))) || (!skill.isBad() && (targetsAttackTarget != null) && targetsAttackTarget.isRaid() && targetsAttackTarget.giveRaidCurse() && targetsAttackTarget.getAttackByList().contains(target) && (getLevel() > (targetsAttackTarget.getLevel() + 8))) || (!skill.isBad() && (targetsCastTarget != null) && targetsCastTarget.isRaid() && targetsCastTarget.giveRaidCurse() && targetsCastTarget.getAttackByList().contains(target) && (getLevel() > (targetsCastTarget.getLevel() + 8)))))
				{
					final CommonSkill curse = skill.isMagic() ? CommonSkill.RAID_CURSE : CommonSkill.RAID_CURSE2;
					final Skill curseSkill = curse.getSkill();
					if (curseSkill != null)
					{
						abortAttack();
						abortCast();
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						curseSkill.applyEffects(target, this);
					}
					else
					{
						LOGGER.warning("Skill ID " + curse.getId() + " level " + curse.getLevel() + " is missing in DP!");
					}
					return;
				}
				
				// Check if over-hit is possible
				if (skill.isOverhit() && target.isAttackable())
				{
					((Attackable) target).overhitEnabled(true);
				}
				
				// Static skills not trigger any chance skills
				if (!skill.isStatic())
				{
					// Launch weapon Special ability skill effect if available
					if ((activeWeapon != null) && !target.isDead())
					{
						activeWeapon.castOnMagicSkill(this, target, skill);
					}
					
					if (_triggerSkills != null)
					{
						for (OptionsSkillHolder holder : _triggerSkills.values())
						{
							if (((skill.isMagic() && (holder.getSkillType() == OptionsSkillType.MAGIC)) || (skill.isPhysical() && (holder.getSkillType() == OptionsSkillType.ATTACK))) && (Rnd.get(100) < holder.getChance()))
							{
								makeTriggerCast(holder.getSkill(), target);
							}
						}
					}
				}
			}
			
			// Launch the magic skill and calculate its effects
			skill.activateSkill(this, targets);
			
			final PlayerInstance player = getActingPlayer();
			if (player != null)
			{
				for (WorldObject target : targets)
				{
					// EVT_ATTACKED and PvPStatus
					if (target.isCreature())
					{
						if (skill.getEffectPoint() <= 0)
						{
							if ((target.isPlayable() || target.isTrap()) && skill.isBad())
							{
								// Casted on target_self but don't harm self
								if (!target.equals(this))
								{
									// Combat-mode check
									if (target.isPlayer())
									{
										target.getActingPlayer().getAI().clientStartAutoAttack();
									}
									else if (target.isSummon() && ((Creature) target).hasAI())
									{
										final PlayerInstance owner = ((Summon) target).getOwner();
										if (owner != null)
										{
											owner.getAI().clientStartAutoAttack();
										}
									}
									
									// attack of the own pet does not flag player
									// triggering trap not flag trap owner
									if ((player.getSummon() != target) && !isTrap() && !((skill.getEffectPoint() == 0) && (skill.getAffectRange() > 0)))
									{
										player.updatePvPStatus((Creature) target);
									}
								}
							}
							else if (target.isAttackable())
							{
								switch (skill.getId())
								{
									case 51: // Lure
									case 511: // Temptation
									{
										break;
									}
									default:
									{
										// add attacker into list
										((Creature) target).addAttackerToAttackByList(this);
									}
								}
							}
							// notify target AI about the attack
							if (((Creature) target).hasAI() && !skill.hasEffectType(EffectType.HATE) && (skill.getAbnormalType() != AbnormalType.TURN_PASSIVE))
							{
								((Creature) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
							}
						}
						else
						{
							if (target.isPlayer())
							{
								// Casting non offensive skill on player with pvp flag set or with karma
								if (!(target.equals(this) || target.equals(player)) && ((target.getActingPlayer().getPvpFlag() > 0) || (target.getActingPlayer().getKarma() > 0)))
								{
									player.updatePvPStatus();
								}
							}
							else if (target.isAttackable())
							{
								player.updatePvPStatus();
							}
						}
					}
					if (target.isFakePlayer())
					{
						player.updatePvPStatus();
					}
				}
				
				// Mobs in range 1000 see spell
				World.getInstance().forEachVisibleObjectInRange(player, Npc.class, 1000, npcMob ->
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npcMob, player, skill, targets, isSummon()), npcMob);
					
					// On Skill See logic
					if (npcMob.isAttackable())
					{
						final Attackable attackable = (Attackable) npcMob;
						int skillEffectPoint = skill.getEffectPoint();
						if (player.hasSummon() && (targets.length == 1) && CommonUtil.contains(targets, player.getSummon()))
						{
							skillEffectPoint = 0;
						}
						
						if ((skillEffectPoint > 0) && attackable.hasAI() && (attackable.getAI().getIntention() == AI_INTENTION_ATTACK))
						{
							final WorldObject npcTarget = attackable.getTarget();
							for (WorldObject skillTarget : targets)
							{
								if ((npcTarget == skillTarget) || (npcMob == skillTarget))
								{
									final Creature originalCaster = isSummon() ? this : player;
									attackable.addDamageHate(originalCaster, 0, (skillEffectPoint * 150) / (attackable.getLevel() + 7));
								}
							}
						}
					}
				});
			}
			// Notify AI
			if (skill.isBad() && !skill.hasEffectType(EffectType.HATE))
			{
				for (WorldObject target : targets)
				{
					if (target.isCreature())
					{
						final Creature creature = (Creature) target;
						if (creature.hasAI())
						{
							// Notify target AI about the attack
							creature.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
						}
					}
					
					if (isFakePlayer()) // fake player attacks player
					{
						if (target.isPlayable() || target.isFakePlayer())
						{
							final Npc npc = ((Npc) this);
							if (!npc.isScriptValue(1))
							{
								npc.setScriptValue(1); // in combat
								npc.broadcastInfo(); // update flag status
								QuestManager.getInstance().getQuest("PvpFlaggingStopTask").notifyEvent("FLAG_CHECK", npc, null);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": callSkill() failed.", e);
		}
	}
	
	/**
	 * @return the Level Modifier ((level + 89) / 100).
	 */
	public double getLevelMod()
	{
		return ((getLevel() + 89) / 100d);
	}
	
	public void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}
	
	/**
	 * Sets _isCastingNow to true and _castInterruptTime is calculated from end time (ticks)
	 * @param newSkillCastEndTick
	 */
	public void forceIsCasting(int newSkillCastEndTick)
	{
		setCastingNow(true);
		// for interrupt -400 ms
		_castInterruptTime = newSkillCastEndTick - 4;
	}
	
	private boolean _AIdisabled = false;
	
	public void updatePvPFlag(int value)
	{
		// Overridden in PlayerInstance
	}
	
	/**
	 * @return a multiplier based on weapon random damage
	 */
	public double getRandomDamageMultiplier()
	{
		final Weapon activeWeapon = getActiveWeaponItem();
		int random;
		if (activeWeapon != null)
		{
			random = activeWeapon.getRandomDamage();
		}
		else
		{
			random = 5 + (int) Math.sqrt(getLevel());
		}
		return (1 + ((double) Rnd.get(0 - random, random) / 100));
	}
	
	public long getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	public int getBowAttackEndTime()
	{
		return _disableBowAttackEndTime;
	}
	
	/**
	 * Not Implemented.
	 * @return
	 */
	public abstract int getLevel();
	
	public double calcStat(Stat stat, double init)
	{
		return _stat.calcStat(stat, init, null, null);
	}
	
	// Stat - NEED TO REMOVE ONCE CREATURESTATUS IS COMPLETE
	public double calcStat(Stat stat, double init, Creature target, Skill skill)
	{
		return _stat.calcStat(stat, init, target, skill);
	}
	
	public int getAccuracy()
	{
		return _stat.getAccuracy();
	}
	
	public float getAttackSpeedMultiplier()
	{
		return _stat.getAttackSpeedMultiplier();
	}
	
	public double getCriticalDmg(Creature target, double init)
	{
		return _stat.getCriticalDmg(target, init);
	}
	
	public int getCriticalHit(Creature target, Skill skill)
	{
		return _stat.getCriticalHit(target, skill);
	}
	
	public int getEvasionRate(Creature target)
	{
		return _stat.getEvasionRate(target);
	}
	
	public int getMagicalAttackRange(Skill skill)
	{
		return _stat.getMagicalAttackRange(skill);
	}
	
	public int getMaxCp()
	{
		return _stat.getMaxCp();
	}
	
	public int getMaxRecoverableCp()
	{
		return _stat.getMaxRecoverableCp();
	}
	
	public double getMAtk(Creature target, Skill skill)
	{
		return _stat.getMAtk(target, skill);
	}
	
	public int getMAtkSpd()
	{
		return _stat.getMAtkSpd();
	}
	
	public int getMaxMp()
	{
		return _stat.getMaxMp();
	}
	
	public int getMaxRecoverableMp()
	{
		return _stat.getMaxRecoverableMp();
	}
	
	public int getMaxHp()
	{
		return _stat.getMaxHp();
	}
	
	public int getMaxRecoverableHp()
	{
		return _stat.getMaxRecoverableHp();
	}
	
	public int getMCriticalHit(Creature target, Skill skill)
	{
		return _stat.getMCriticalHit(target, skill);
	}
	
	public double getMDef(Creature target, Skill skill)
	{
		return _stat.getMDef(target, skill);
	}
	
	public double getMReuseRate(Skill skill)
	{
		return _stat.getMReuseRate(skill);
	}
	
	public double getPAtk(Creature target)
	{
		return _stat.getPAtk(target);
	}
	
	public double getPAtkSpd()
	{
		return _stat.getPAtkSpd();
	}
	
	public double getPDef(Creature target)
	{
		return _stat.getPDef(target);
	}
	
	public int getPhysicalAttackRange()
	{
		return _stat.getPhysicalAttackRange();
	}
	
	public double getMovementSpeedMultiplier()
	{
		return _stat.getMovementSpeedMultiplier();
	}
	
	public double getRunSpeed()
	{
		return _stat.getRunSpeed();
	}
	
	public double getWalkSpeed()
	{
		return _stat.getWalkSpeed();
	}
	
	public double getSwimRunSpeed()
	{
		return _stat.getSwimRunSpeed();
	}
	
	public double getSwimWalkSpeed()
	{
		return _stat.getSwimWalkSpeed();
	}
	
	public double getMoveSpeed()
	{
		return _stat.getMoveSpeed();
	}
	
	public int getShldDef()
	{
		return _stat.getShldDef();
	}
	
	public int getSTR()
	{
		return _stat.getSTR();
	}
	
	public int getDEX()
	{
		return _stat.getDEX();
	}
	
	public int getCON()
	{
		return _stat.getCON();
	}
	
	public int getINT()
	{
		return _stat.getINT();
	}
	
	public int getWIT()
	{
		return _stat.getWIT();
	}
	
	public int getMEN()
	{
		return _stat.getMEN();
	}
	
	// Status - NEED TO REMOVE ONCE CREATURESTATUS IS COMPLETE
	public void addStatusListener(Creature object)
	{
		_status.addStatusListener(object);
	}
	
	public void reduceCurrentHp(double amount, Creature attacker, Skill skill)
	{
		reduceCurrentHp(amount, attacker, true, false, skill);
	}
	
	public void reduceCurrentHpByDOT(double amount, Creature attacker, Skill skill)
	{
		reduceCurrentHp(amount, attacker, !skill.isToggle(), true, skill);
	}
	
	public void reduceCurrentHp(double amount, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (Config.CHAMPION_ENABLE && isChampion() && (Config.CHAMPION_HP != 0))
		{
			_status.reduceHp(amount / Config.CHAMPION_HP, attacker, awake, isDOT, false);
		}
		else
		{
			_status.reduceHp(amount, attacker, awake, isDOT, false);
		}
	}
	
	public void reduceCurrentMp(double amount)
	{
		_status.reduceMp(amount);
	}
	
	@Override
	public void removeStatusListener(Creature object)
	{
		_status.removeStatusListener(object);
	}
	
	protected void stopHpMpRegeneration()
	{
		_status.stopHpMpRegeneration();
	}
	
	public double getCurrentCp()
	{
		return _status.getCurrentCp();
	}
	
	public void setCurrentCp(double newCp)
	{
		_status.setCurrentCp(newCp);
	}
	
	public double getCurrentHp()
	{
		return _status.getCurrentHp();
	}
	
	public void setCurrentHp(double newHp)
	{
		_status.setCurrentHp(newHp);
	}
	
	public void setCurrentHpMp(double newHp, double newMp)
	{
		_status.setCurrentHpMp(newHp, newMp);
	}
	
	public double getCurrentMp()
	{
		return _status.getCurrentMp();
	}
	
	public void setCurrentMp(double newMp)
	{
		_status.setCurrentMp(newMp);
	}
	
	/**
	 * @return the max weight that the Creature can load.
	 */
	public int getMaxLoad()
	{
		if (isPlayer() || isPet())
		{
			// Weight Limit = (CON Modifier*69000) * Skills
			// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
			final double baseLoad = Math.floor(BaseStat.CON.calcBonus(this) * 69000 * Config.ALT_WEIGHT_LIMIT);
			return (int) calcStat(Stat.WEIGHT_LIMIT, baseLoad, this, null);
		}
		return 0;
	}
	
	public int getBonusWeightPenalty()
	{
		if (isPlayer() || isPet())
		{
			return (int) calcStat(Stat.WEIGHT_PENALTY, 1, this, null);
		}
		return 0;
	}
	
	/**
	 * @return the current weight of the Creature.
	 */
	public int getCurrentLoad()
	{
		if (isPlayer() || isPet())
		{
			return getInventory().getTotalWeight();
		}
		return 0;
	}
	
	public boolean isChampion()
	{
		return false;
	}
	
	/**
	 * Send system message about damage.
	 * @param target
	 * @param damage
	 * @param mcrit
	 * @param pcrit
	 * @param miss
	 */
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss && target.isPlayer())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EVADED_C2_S_ATTACK);
			sm.addPcName(target.getActingPlayer());
			sm.addString(getName());
			target.sendPacket(sm);
		}
	}
	
	public byte getAttackElement()
	{
		return _stat.getAttackElement();
	}
	
	public int getAttackElementValue(byte attackAttribute)
	{
		return _stat.getAttackElementValue(attackAttribute);
	}
	
	public int getDefenseElementValue(byte defenseAttribute)
	{
		return _stat.getDefenseElementValue(defenseAttribute);
	}
	
	public void startPhysicalAttackMuted()
	{
		abortAttack();
	}
	
	public void disableCoreAI(boolean value)
	{
		_AIdisabled = value;
	}
	
	public boolean isCoreAIDisabled()
	{
		return _AIdisabled;
	}
	
	/**
	 * @return true
	 */
	public boolean giveRaidCurse()
	{
		return true;
	}
	
	/**
	 * Check if target is affected with special buff
	 * @see EffectList#isAffected(EffectFlag)
	 * @param flag int
	 * @return boolean
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return _effectList.isAffected(flag);
	}
	
	public Team getTeam()
	{
		return _team;
	}
	
	public void setTeam(Team team)
	{
		_team = team;
	}
	
	public void addOverrideCond(PlayerCondOverride... excs)
	{
		for (PlayerCondOverride exc : excs)
		{
			_exceptions |= exc.getMask();
		}
	}
	
	public void removeOverridedCond(PlayerCondOverride... excs)
	{
		for (PlayerCondOverride exc : excs)
		{
			_exceptions &= ~exc.getMask();
		}
	}
	
	public boolean canOverrideCond(PlayerCondOverride excs)
	{
		return (_exceptions & excs.getMask()) == excs.getMask();
	}
	
	public void setOverrideCond(long masks)
	{
		_exceptions = masks;
	}
	
	public void setLethalable(boolean value)
	{
		_lethalable = value;
	}
	
	public boolean isLethalable()
	{
		return _lethalable;
	}
	
	public Map<Integer, OptionsSkillHolder> getTriggerSkills()
	{
		if (_triggerSkills == null)
		{
			synchronized (this)
			{
				if (_triggerSkills == null)
				{
					_triggerSkills = new ConcurrentHashMap<>();
				}
			}
		}
		return _triggerSkills;
	}
	
	public void addTriggerSkill(OptionsSkillHolder holder)
	{
		getTriggerSkills().put(holder.getSkillId(), holder);
	}
	
	public void removeTriggerSkill(OptionsSkillHolder holder)
	{
		getTriggerSkills().remove(holder.getSkillId());
	}
	
	public void makeTriggerCast(Skill skill, Creature target, boolean ignoreTargetType)
	{
		try
		{
			if ((skill == null))
			{
				return;
			}
			
			if (skill.checkCondition(this, target, false))
			{
				if (isSkillDisabled(skill))
				{
					return;
				}
				
				if (skill.getReuseDelay() > 0)
				{
					disableSkill(skill, skill.getReuseDelay());
				}
				
				// @formatter:off
				final WorldObject[] targets = !ignoreTargetType ? skill.getTargetList(this, false, target) : new Creature[]{ target };
				// @formatter:on
				if (targets.length == 0)
				{
					return;
				}
				
				Creature skillTarget = target;
				for (WorldObject obj : targets)
				{
					if ((obj != null) && obj.isCreature())
					{
						skillTarget = (Creature) obj;
						break;
					}
				}
				
				if (Config.ALT_VALIDATE_TRIGGER_SKILLS && isPlayable() && (skillTarget != null) && skillTarget.isPlayable())
				{
					final PlayerInstance player = getActingPlayer();
					if (!player.checkPvpSkill(skillTarget, skill))
					{
						return;
					}
				}
				
				broadcastPacket(new MagicSkillUse(this, skillTarget, skill.getDisplayId(), skill.getLevel(), 0, 0));
				broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getLevel(), targets));
				
				// Launch the magic skill and calculate its effects
				skill.activateSkill(this, targets);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "", e);
		}
	}
	
	public void makeTriggerCast(Skill skill, Creature target)
	{
		makeTriggerCast(skill, target, false);
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return {@code true} if current player can revive and shows 'To Village' button upon death, {@code false} otherwise.
	 */
	public boolean canRevive()
	{
		return true;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @param value
	 */
	public void setCanRevive(boolean value)
	{
	}
	
	/**
	 * Dummy method overriden in {@link Attackable}
	 * @return {@code true} if there is a loot to sweep, {@code false} otherwise.
	 */
	public boolean isSweepActive()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return the clan id of current character.
	 */
	public int getClanId()
	{
		return 0;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return the clan of current character.
	 */
	public Clan getClan()
	{
		return null;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return {@code true} if player is in academy, {@code false} otherwise.
	 */
	public boolean isAcademyMember()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return the pledge type of current character.
	 */
	public int getPledgeType()
	{
		return 0;
	}
	
	/**
	 * Dummy method overriden in {@link PlayerInstance}
	 * @return the alliance id of current character.
	 */
	public int getAllyId()
	{
		return 0;
	}
	
	/**
	 * Notifies to listeners that current character received damage.
	 * @param damage
	 * @param attacker
	 * @param skill
	 * @param critical
	 * @param damageOverTime
	 */
	public void notifyDamageReceived(double damage, Creature attacker, Skill skill, boolean critical, boolean damageOverTime)
	{
		if (attacker != null)
		{
			if (_onCreatureDamageDealt == null)
			{
				_onCreatureDamageDealt = new OnCreatureDamageDealt();
			}
			_onCreatureDamageDealt.setAttacker(attacker);
			_onCreatureDamageDealt.setTarget(this);
			_onCreatureDamageDealt.setDamage(damage);
			_onCreatureDamageDealt.setSkill(skill);
			_onCreatureDamageDealt.setCritical(critical);
			_onCreatureDamageDealt.setDamageOverTime(damageOverTime);
			EventDispatcher.getInstance().notifyEvent(_onCreatureDamageDealt, attacker);
		}
		
		if (_onCreatureDamageReceived == null)
		{
			_onCreatureDamageReceived = new OnCreatureDamageReceived();
		}
		_onCreatureDamageReceived.setAttacker(attacker);
		_onCreatureDamageReceived.setTarget(this);
		_onCreatureDamageReceived.setDamage(damage);
		_onCreatureDamageReceived.setSkill(skill);
		_onCreatureDamageReceived.setCritical(critical);
		_onCreatureDamageReceived.setDamageOverTime(damageOverTime);
		EventDispatcher.getInstance().notifyEventAsync(_onCreatureDamageReceived, this);
	}
	
	/**
	 * Notifies to listeners that current character avoid attack.
	 * @param target
	 * @param isDot
	 */
	public void notifyAttackAvoid(Creature target, boolean isDot)
	{
		if (_onCreatureAttackAvoid == null)
		{
			_onCreatureAttackAvoid = new OnCreatureAttackAvoid();
		}
		_onCreatureAttackAvoid.setAttacker(this);
		_onCreatureAttackAvoid.setTarget(target);
		_onCreatureAttackAvoid.setDamageOverTime(isDot);
		EventDispatcher.getInstance().notifyEvent(_onCreatureAttackAvoid, target);
	}
	
	/**
	 * @return {@link WeaponType} of current character's weapon or basic weapon type.
	 */
	public WeaponType getAttackType()
	{
		if (isTransformed())
		{
			final TransformTemplate template = getTransformation().getTemplate(getActingPlayer());
			if (template != null)
			{
				return template.getBaseAttackType();
			}
		}
		final Weapon weapon = getActiveWeaponItem();
		if (weapon != null)
		{
			return weapon.getItemType();
		}
		return _template.getBaseAttackType();
	}
	
	public boolean isInCategory(CategoryType type)
	{
		return CategoryData.getInstance().isInCategory(type, getId());
	}
	
	/**
	 * @return the character that summoned this NPC.
	 */
	public Creature getSummoner()
	{
		return _summoner;
	}
	
	/**
	 * @param summoner the summoner of this NPC.
	 */
	public void setSummoner(Creature summoner)
	{
		_summoner = summoner;
	}
	
	@Override
	public boolean isCreature()
	{
		return true;
	}
	
	/**
	 * @return {@code true} if current character is casting channeling skill, {@code false} otherwise.
	 */
	public boolean isChanneling()
	{
		return (_channelizer != null) && _channelizer.isChanneling();
	}
	
	public SkillChannelizer getSkillChannelizer()
	{
		if (_channelizer == null)
		{
			_channelizer = new SkillChannelizer(this);
		}
		return _channelizer;
	}
	
	/**
	 * @return {@code true} if current character is affected by channeling skill, {@code false} otherwise.
	 */
	public boolean isChannelized()
	{
		return (_channelized != null) && !_channelized.isChannelized();
	}
	
	public SkillChannelized getSkillChannelized()
	{
		if (_channelized == null)
		{
			_channelized = new SkillChannelized();
		}
		return _channelized;
	}
	
	public void addInvulAgainst(SkillHolder holder)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(holder.getSkillId());
		if (invulHolder != null)
		{
			invulHolder.increaseInstances();
			return;
		}
		getInvulAgainstSkills().put(holder.getSkillId(), new InvulSkillHolder(holder));
	}
	
	public void removeInvulAgainst(SkillHolder holder)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(holder.getSkillId());
		if ((invulHolder != null) && (invulHolder.decreaseInstances() < 1))
		{
			getInvulAgainstSkills().remove(holder.getSkillId());
		}
	}
	
	public boolean isInvulAgainst(int skillId, int skillLevel)
	{
		if (_invulAgainst != null)
		{
			final SkillHolder holder = getInvulAgainstSkills().get(skillId);
			return ((holder != null) && ((holder.getSkillLevel() < 1) || (holder.getSkillLevel() == skillLevel)));
		}
		return false;
	}
	
	private Map<Integer, InvulSkillHolder> getInvulAgainstSkills()
	{
		if (_invulAgainst == null)
		{
			synchronized (this)
			{
				if (_invulAgainst == null)
				{
					_invulAgainst = new ConcurrentHashMap<>();
				}
			}
		}
		return _invulAgainst;
	}
	
	@Override
	public Queue<AbstractEventListener> getListeners(EventType type)
	{
		final Queue<AbstractEventListener> objectListenres = super.getListeners(type);
		final Queue<AbstractEventListener> templateListeners = _template.getListeners(type);
		final Queue<AbstractEventListener> globalListeners = isNpc() && !isMonster() ? Containers.Npcs().getListeners(type) : isMonster() ? Containers.Monsters().getListeners(type) : isPlayer() ? Containers.Players().getListeners(type) : EmptyQueue.emptyQueue();
		
		// Attempt to do not create collection
		if (objectListenres.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty())
		{
			return EmptyQueue.emptyQueue();
		}
		else if (!objectListenres.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty())
		{
			return objectListenres;
		}
		else if (!templateListeners.isEmpty() && objectListenres.isEmpty() && globalListeners.isEmpty())
		{
			return templateListeners;
		}
		else if (!globalListeners.isEmpty() && objectListenres.isEmpty() && templateListeners.isEmpty())
		{
			return globalListeners;
		}
		
		final Queue<AbstractEventListener> both = new LinkedBlockingDeque<>(objectListenres.size() + templateListeners.size() + globalListeners.size());
		both.addAll(objectListenres);
		both.addAll(templateListeners);
		both.addAll(globalListeners);
		return both;
	}
	
	public Race getRace()
	{
		return _template.getRace();
	}
	
	@Override
	public void setXYZ(int newX, int newY, int newZ)
	{
		final ZoneRegion oldZoneRegion = ZoneManager.getInstance().getRegion(this);
		final ZoneRegion newZoneRegion = ZoneManager.getInstance().getRegion(newX, newY);
		
		// Mobius: Prevent moving to nonexistent regions.
		if (newZoneRegion == null)
		{
			return;
		}
		
		if (oldZoneRegion != newZoneRegion)
		{
			oldZoneRegion.removeFromZones(this);
			newZoneRegion.revalidateZones(this);
		}
		
		super.setXYZ(newX, newY, newZ);
	}
	
	public boolean isInDuel()
	{
		return false;
	}
	
	public int getDuelId()
	{
		return 0;
	}
	
	public byte getSiegeState()
	{
		return 0;
	}
	
	public int getSiegeSide()
	{
		return 0;
	}
	
	public Map<Integer, RelationCache> getKnownRelations()
	{
		return _knownRelations;
	}
	
	protected void initSeenCreatures()
	{
		if (_seenCreatures == null)
		{
			synchronized (this)
			{
				if (_seenCreatures == null)
				{
					if (isNpc())
					{
						final NpcTemplate template = ((Npc) this).getTemplate();
						if ((template != null) && (template.getAggroRange() > 0))
						{
							_seenCreatureRange = template.getAggroRange();
						}
					}
					
					_seenCreatures = ConcurrentHashMap.newKeySet(1);
				}
			}
		}
		
		CreatureSeeTaskManager.getInstance().add(this);
	}
	
	public void updateSeenCreatures()
	{
		if ((_seenCreatures == null) || _isDead || !isSpawned())
		{
			return;
		}
		
		World.getInstance().forEachVisibleObjectInRange(this, Creature.class, _seenCreatureRange, creature ->
		{
			if (!creature.isInvisible())
			{
				final WorldRegion worldRegion = getWorldRegion();
				if ((worldRegion != null) && worldRegion.areNeighborsActive() && _seenCreatures.add(creature))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnCreatureSee(this, creature), this);
				}
			}
		});
	}
	
	public void removeSeenCreature(WorldObject worldObject)
	{
		if (_seenCreatures == null)
		{
			return;
		}
		
		_seenCreatures.remove(worldObject);
	}
	
	public int getKarma()
	{
		return _karma;
	}
	
	public void setKarma(int karma)
	{
		_karma = karma;
	}
	
	public int getMinShopDistance()
	{
		return 0;
	}
	
	public void setCursorKeyMovement(boolean value)
	{
		_cursorKeyMovement = value;
	}
	
	public List<ItemInstance> getFakePlayerDrops()
	{
		return _fakePlayerDrops;
	}
	
	public void addBuffInfoTime(BuffInfo info)
	{
		if (_buffFinishTask == null)
		{
			_buffFinishTask = new BuffFinishTask();
		}
		_buffFinishTask.addBuffInfo(info);
	}
	
	public void removeBuffInfoTime(BuffInfo info)
	{
		if (_buffFinishTask != null)
		{
			_buffFinishTask.removeBuffInfo(info);
		}
	}
	
	public void cancelBuffFinishTask()
	{
		if (_buffFinishTask != null)
		{
			final ScheduledFuture<?> task = _buffFinishTask.getTask();
			if ((task != null) && !task.isCancelled() && !task.isDone())
			{
				task.cancel(true);
			}
			_buffFinishTask = null;
		}
	}
}
