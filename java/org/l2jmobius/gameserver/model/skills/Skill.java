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
package org.l2jmobius.gameserver.model.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.ShotType;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.instancemanager.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.model.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.model.ExtractableProductItem;
import org.l2jmobius.gameserver.model.ExtractableSkill;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.BlockInstance;
import org.l2jmobius.gameserver.model.actor.instance.CubicInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.interfaces.IIdentifiable;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.stats.BaseStat;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.TraitType;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.model.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

public class Skill implements IIdentifiable
{
	private static final Logger LOGGER = Logger.getLogger(Skill.class.getName());
	
	private static final WorldObject[] EMPTY_TARGET_LIST = new WorldObject[0];
	
	/** Skill ID. */
	private final int _id;
	/** Skill level. */
	private final int _level;
	/** Custom skill ID displayed by the client. */
	private final int _displayId;
	/** Custom skill level displayed by the client. */
	private final int _displayLevel;
	/** Skill client's name. */
	private final String _name;
	/** Operative type: passive, active, toggle. */
	private final SkillOperateType _operateType;
	private final int _magic;
	private final TraitType _traitType;
	private final boolean _staticReuse;
	/** MP consumption. */
	private final int _mpConsume;
	/** Initial MP consumption. */
	private final int _mpInitialConsume;
	/** MP consumption per channeling. */
	private final int _mpPerChanneling;
	/** HP consumption. */
	private final int _hpConsume;
	/** Amount of items consumed by this skill from caster. */
	private final int _itemConsumeCount;
	/** Id of item consumed by this skill from caster. */
	private final int _itemConsumeId;
	/** Cast range: how far can be the target. */
	private final int _castRange;
	/** Effect range: how far the skill affect the target. */
	private final int _effectRange;
	/** Abnormal instant, used for herbs mostly. */
	private final boolean _isAbnormalInstant;
	/** Abnormal level, global effect level. */
	private final int _abnormalLevel;
	/** Abnormal type: global effect "group". */
	private final AbnormalType _abnormalType;
	/** Abnormal time: global effect duration time. */
	private final int _abnormalTime;
	/** Abnormal visual effect: the visual effect displayed ingame. */
	private AbnormalVisualEffect[] _abnormalVisualEffects = null;
	/** Abnormal visual effect special: the visual effect displayed ingame. */
	private AbnormalVisualEffect[] _abnormalVisualEffectsSpecial = null;
	/** Abnormal visual effect event: the visual effect displayed ingame. */
	private AbnormalVisualEffect[] _abnormalVisualEffectsEvent = null;
	/** If {@code true} this skill's effect should stay after death. */
	private final boolean _stayAfterDeath;
	/** If {@code true} this skill's effect should stay after class-subclass change. */
	private final boolean _stayOnSubclassChange;
	/** If {@code true} this skill's effect recovery HP/MP or CP from herb. */
	private final boolean _isRecoveryHerb;
	
	private int _refId;
	// all times in milliseconds
	private final int _hitTime;
	// private final int _skillInterruptTime;
	private final int _coolTime;
	private final int _reuseHashCode;
	private final int _reuseDelay;
	
	/** Target type of the skill : SELF, PARTY, CLAN, PET... */
	private final TargetType _targetType;
	private final int _feed;
	// base success chance
	private final double _power;
	private final double _pvpPower;
	private final double _pvePower;
	private final int _magicLevel;
	private final int _lvlBonusRate;
	private final int _activateRate;
	private final int _minChance;
	private final int _maxChance;
	private final int _blowChance;
	
	// Effecting area of the skill, in radius.
	// The radius center varies according to the _targetType:
	// "caster" if targetType = AURA/PARTY/CLAN or "target" if targetType = AREA
	private final int _affectRange;
	private final int[] _affectLimit = new int[2];
	
	private final boolean _nextActionIsAttack;
	
	private final boolean _removedOnAnyActionExceptMove;
	private final boolean _removedOnDamage;
	
	private final boolean _blockedInOlympiad;
	
	private final byte _element;
	private final int _elementPower;
	
	private final BaseStat _basicProperty;
	
	private final boolean _overhit;
	
	private final int _minPledgeClass;
	private final int _chargeConsume;
	private final int _soulMaxConsume;
	
	private final boolean _isHeroSkill; // If true the skill is a Hero Skill
	private final boolean _isGMSkill; // True if skill is GM skill
	private final boolean _isSevenSigns;
	
	private final int _baseCritRate; // percent of success for skill critical hit (especially for PhysicalAttack & Blow - they're not affected by rCrit values or buffs).
	private final boolean _directHpDmg; // If true then damage is being make directly
	private final boolean _isTriggeredSkill; // If true the skill will take activation buff slot instead of a normal buff slot
	private final int _effectPoint;
	// Condition lists
	private List<Condition> _preCondition;
	private List<Condition> _itemPreCondition;
	// Function lists
	private List<FuncTemplate> _funcTemplates;
	
	private final Map<EffectScope, List<AbstractEffect>> _effectLists = new EnumMap<>(EffectScope.class);
	
	// Flying support
	private final FlyType _flyType;
	private final int _flyRadius;
	private final float _flyCourse;
	
	private final boolean _isDebuff;
	
	private final String _attribute;
	
	private final boolean _ignoreShield;
	
	private final boolean _isSuicideAttack;
	private final boolean _canBeDispeled;
	
	private final boolean _isClanSkill;
	private final boolean _excludedFromCheck;
	private final boolean _simultaneousCast;
	
	private ExtractableSkill _extractableItems = null;
	
	private final String _icon;
	
	private volatile Byte[] _effectTypes;
	
	// Channeling data
	private final int _channelingSkillId;
	private final int _channelingTickInitialDelay;
	private final int _channelingTickInterval;
	
	private final boolean _isPvPOnly;
	
	public Skill(StatSet set)
	{
		_id = set.getInt("skill_id");
		_level = set.getInt("level");
		_displayId = set.getInt("displayId", _id);
		_displayLevel = set.getInt("displayLevel", _level);
		_name = set.getString("name", "");
		_operateType = set.getEnum("operateType", SkillOperateType.class);
		_magic = set.getInt("isMagic", 0);
		_traitType = set.getEnum("trait", TraitType.class, TraitType.NONE);
		_staticReuse = set.getBoolean("staticReuse", false);
		_mpConsume = set.getInt("mpConsume", 0);
		_mpInitialConsume = set.getInt("mpInitialConsume", 0);
		_mpPerChanneling = set.getInt("mpPerChanneling", _mpConsume);
		_hpConsume = set.getInt("hpConsume", 0);
		_itemConsumeCount = set.getInt("itemConsumeCount", 0);
		_itemConsumeId = set.getInt("itemConsumeId", 0);
		_castRange = set.getInt("castRange", -1);
		_effectRange = set.getInt("effectRange", -1);
		_abnormalLevel = set.getInt("abnormalLevel", 0);
		_abnormalType = set.getEnum("abnormalType", AbnormalType.class, AbnormalType.NONE);
		int abnormalTime = set.getInt("abnormalTime", 0);
		if (Config.ENABLE_MODIFY_SKILL_DURATION && Config.SKILL_DURATION_LIST.containsKey(_id))
		{
			if ((_level < 100) || (_level > 140))
			{
				abnormalTime = Config.SKILL_DURATION_LIST.get(_id);
			}
			else if ((_level >= 100) && (_level < 140))
			{
				abnormalTime += Config.SKILL_DURATION_LIST.get(_id);
			}
		}
		
		_abnormalTime = abnormalTime;
		_isAbnormalInstant = set.getBoolean("abnormalInstant", false);
		parseAbnormalVisualEffect(set.getString("abnormalVisualEffect", null));
		_attribute = set.getString("attribute", "");
		_stayAfterDeath = set.getBoolean("stayAfterDeath", false);
		_stayOnSubclassChange = set.getBoolean("stayOnSubclassChange", true);
		_hitTime = set.getInt("hitTime", 0);
		_coolTime = set.getInt("coolTime", 0);
		_isDebuff = set.getBoolean("isDebuff", false);
		_isRecoveryHerb = set.getBoolean("isRecoveryHerb", false);
		_feed = set.getInt("feed", 0);
		_reuseHashCode = SkillData.getSkillHashCode(_id, _level);
		if (Config.ENABLE_MODIFY_SKILL_REUSE && Config.SKILL_REUSE_LIST.containsKey(_id))
		{
			_reuseDelay = Config.SKILL_REUSE_LIST.get(_id);
		}
		else
		{
			_reuseDelay = set.getInt("reuseDelay", 0);
		}
		
		_affectRange = set.getInt("affectRange", 0);
		
		final String affectLimit = set.getString("affectLimit", null);
		if (affectLimit != null)
		{
			try
			{
				final String[] valuesSplit = affectLimit.split("-");
				_affectLimit[0] = Integer.parseInt(valuesSplit[0]);
				_affectLimit[1] = Integer.parseInt(valuesSplit[1]);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid affectLimit value: " + affectLimit + ", \"percent-percent\" required");
			}
		}
		
		_targetType = set.getEnum("targetType", TargetType.class, TargetType.SELF);
		_power = set.getFloat("power", 0.f);
		_pvpPower = set.getFloat("pvpPower", (float) _power);
		_pvePower = set.getFloat("pvePower", (float) _power);
		_magicLevel = set.getInt("magicLevel", 0);
		_lvlBonusRate = set.getInt("lvlBonusRate", 0);
		_activateRate = set.getInt("activateRate", -1);
		_minChance = set.getInt("minChance", Config.MIN_ABNORMAL_STATE_SUCCESS_RATE);
		_maxChance = set.getInt("maxChance", Config.MAX_ABNORMAL_STATE_SUCCESS_RATE);
		_ignoreShield = set.getBoolean("ignoreShld", false);
		_nextActionIsAttack = set.getBoolean("nextActionAttack", false);
		_removedOnAnyActionExceptMove = set.getBoolean("removedOnAnyActionExceptMove", false);
		_removedOnDamage = set.getBoolean("removedOnDamage", false);
		_blockedInOlympiad = set.getBoolean("blockedInOlympiad", false);
		_element = set.getByte("element", (byte) -1);
		_elementPower = set.getInt("elementPower", 0);
		_basicProperty = set.getEnum("basicProperty", BaseStat.class, BaseStat.NONE);
		_overhit = set.getBoolean("overHit", false);
		_isSuicideAttack = set.getBoolean("isSuicideAttack", false);
		_minPledgeClass = set.getInt("minPledgeClass", 0);
		_chargeConsume = set.getInt("chargeConsume", 0);
		_soulMaxConsume = set.getInt("soulMaxConsumeCount", 0);
		_blowChance = set.getInt("blowChance", 0);
		_isHeroSkill = SkillTreeData.getInstance().isHeroSkill(_id, _level);
		_isGMSkill = SkillTreeData.getInstance().isGMSkill(_id, _level);
		_isSevenSigns = (_id > 4360) && (_id < 4367);
		_isClanSkill = SkillTreeData.getInstance().isClanSkill(_id, _level);
		_baseCritRate = set.getInt("baseCritRate", 0);
		_directHpDmg = set.getBoolean("dmgDirectlyToHp", false);
		_isTriggeredSkill = set.getBoolean("isTriggeredSkill", false);
		_effectPoint = set.getInt("effectPoint", 0);
		_flyType = set.getEnum("flyType", FlyType.class, null);
		_flyRadius = set.getInt("flyRadius", 0);
		_flyCourse = set.getFloat("flyCourse", 0);
		_canBeDispeled = set.getBoolean("canBeDispeled", true);
		_excludedFromCheck = set.getBoolean("excludedFromCheck", false);
		_simultaneousCast = set.getBoolean("simultaneousCast", false);
		
		final String capsuled_items = set.getString("capsuled_items_skill", null);
		if (capsuled_items != null)
		{
			if (capsuled_items.isEmpty())
			{
				LOGGER.warning("Empty Extractable Item Skill data in Skill Id: " + _id);
			}
			
			_extractableItems = parseExtractableSkill(_id, _level, capsuled_items);
		}
		
		_icon = set.getString("icon", "icon.skill0000");
		_channelingSkillId = set.getInt("channelingSkillId", 0);
		_channelingTickInterval = set.getInt("channelingTickInterval", 2) * 1000;
		_channelingTickInitialDelay = set.getInt("channelingTickInitialDelay", _channelingTickInterval / 1000) * 1000;
		_isPvPOnly = set.getBoolean("isPvPOnly", false);
	}
	
	public TraitType getTraitType()
	{
		return _traitType;
	}
	
	public byte getElement()
	{
		return _element;
	}
	
	public int getElementPower()
	{
		return _elementPower;
	}
	
	/**
	 * Return the target type of the skill : SELF, PARTY, CLAN, PET...
	 * @return
	 */
	public TargetType getTargetType()
	{
		return _targetType;
	}
	
	public boolean isAOE()
	{
		switch (_targetType)
		{
			case AREA:
			case AURA:
			case BEHIND_AREA:
			case BEHIND_AURA:
			case FRONT_AREA:
			case FRONT_AURA:
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isDamage()
	{
		return hasEffectType(EffectType.MAGICAL_ATTACK, EffectType.HP_DRAIN, EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK);
	}
	
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	public boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}
	
	/**
	 * Return the power of the skill.
	 * @param creature
	 * @param target
	 * @param isPvP
	 * @param isPvE
	 * @return
	 */
	public double getPower(Creature creature, Creature target, boolean isPvP, boolean isPvE)
	{
		if (creature == null)
		{
			return getPower(isPvP, isPvE);
		}
		
		if (hasEffectType(EffectType.DEATH_LINK))
		{
			return getPower(isPvP, isPvE) * (-((creature.getCurrentHp() * 2) / creature.getMaxHp()) + 2);
		}
		
		if (hasEffectType(EffectType.PHYSICAL_ATTACK_HP_LINK))
		{
			return getPower(isPvP, isPvE) * (-((target.getCurrentHp() * 2) / target.getMaxHp()) + 2);
		}
		return getPower(isPvP, isPvE);
	}
	
	public double getPower()
	{
		return _power;
	}
	
	public double getPower(boolean isPvP, boolean isPvE)
	{
		return isPvE ? _pvePower : isPvP ? _pvpPower : _power;
	}
	
	/**
	 * Verify if this skill is abnormal instant.<br>
	 * Herb buff skills yield {@code true} for this check.
	 * @return {@code true} if the skill is abnormal instant, {@code false} otherwise
	 */
	public boolean isAbnormalInstant()
	{
		return _isAbnormalInstant;
	}
	
	/**
	 * Gets the skill abnormal type.
	 * @return the abnormal type
	 */
	public AbnormalType getAbnormalType()
	{
		return _abnormalType;
	}
	
	/**
	 * Gets the skill abnormal level.
	 * @return the skill abnormal level
	 */
	public int getAbnormalLevel()
	{
		return _abnormalLevel;
	}
	
	/**
	 * Gets the skill abnormal time.<br>
	 * Is the base to calculate the duration of the continuous effects of this skill.
	 * @return the abnormal time
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	/**
	 * Gets the skill abnormal visual effect.
	 * @return the abnormal visual effect
	 */
	public AbnormalVisualEffect[] getAbnormalVisualEffects()
	{
		return _abnormalVisualEffects;
	}
	
	/**
	 * Verify if the skill has abnormal visual effects.
	 * @return {@code true} if the skill has abnormal visual effects, {@code false} otherwise
	 */
	public boolean hasAbnormalVisualEffects()
	{
		return (_abnormalVisualEffects != null) && (_abnormalVisualEffects.length > 0);
	}
	
	/**
	 * Gets the skill special abnormal visual effect.
	 * @return the abnormal visual effect
	 */
	public AbnormalVisualEffect[] getAbnormalVisualEffectsSpecial()
	{
		return _abnormalVisualEffectsSpecial;
	}
	
	/**
	 * Verify if the skill has special abnormal visual effects.
	 * @return {@code true} if the skill has special abnormal visual effects, {@code false} otherwise
	 */
	public boolean hasAbnormalVisualEffectsSpecial()
	{
		return (_abnormalVisualEffectsSpecial != null) && (_abnormalVisualEffectsSpecial.length > 0);
	}
	
	/**
	 * Gets the skill event abnormal visual effect.
	 * @return the abnormal visual effect
	 */
	public AbnormalVisualEffect[] getAbnormalVisualEffectsEvent()
	{
		return _abnormalVisualEffectsEvent;
	}
	
	/**
	 * Verify if the skill has event abnormal visual effects.
	 * @return {@code true} if the skill has event abnormal visual effects, {@code false} otherwise
	 */
	public boolean hasAbnormalVisualEffectsEvent()
	{
		return (_abnormalVisualEffectsEvent != null) && (_abnormalVisualEffectsEvent.length > 0);
	}
	
	/**
	 * Gets the skill magic level.
	 * @return the skill magic level
	 */
	public int getMagicLevel()
	{
		return _magicLevel;
	}
	
	public int getLvlBonusRate()
	{
		return _lvlBonusRate;
	}
	
	public int getActivateRate()
	{
		return _activateRate;
	}
	
	/**
	 * Return custom minimum skill/effect chance.
	 * @return
	 */
	public int getMinChance()
	{
		return _minChance;
	}
	
	/**
	 * Return custom maximum skill/effect chance.
	 * @return
	 */
	public int getMaxChance()
	{
		return _maxChance;
	}
	
	/**
	 * Return true if skill effects should be removed on any action except movement
	 * @return
	 */
	public boolean isRemovedOnAnyActionExceptMove()
	{
		return _removedOnAnyActionExceptMove;
	}
	
	/**
	 * @return {@code true} if skill effects should be removed on damage
	 */
	public boolean isRemovedOnDamage()
	{
		return _removedOnDamage;
	}
	
	/**
	 * @return {@code true} if skill can not be used in olympiad.
	 */
	public boolean isBlockedInOlympiad()
	{
		return _blockedInOlympiad;
	}
	
	/**
	 * Return the additional effect Id.
	 * @return
	 */
	public int getChannelingSkillId()
	{
		return _channelingSkillId;
	}
	
	/**
	 * Return true if character should attack target after skill
	 * @return
	 */
	public boolean nextActionIsAttack()
	{
		return _nextActionIsAttack;
	}
	
	/**
	 * @return Returns the castRange.
	 */
	public int getCastRange()
	{
		return _castRange;
	}
	
	/**
	 * @return Returns the effectRange.
	 */
	public int getEffectRange()
	{
		return _effectRange;
	}
	
	/**
	 * @return Returns the hpConsume.
	 */
	public int getHpConsume()
	{
		return _hpConsume;
	}
	
	/**
	 * Gets the skill ID.
	 * @return the skill ID
	 */
	@Override
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Verify if this skill is a debuff.
	 * @return {@code true} if this skill is a debuff, {@code false} otherwise
	 */
	public boolean isDebuff()
	{
		return _isDebuff;
	}
	
	/**
	 * Verify if this skill is coming from Recovery Herb.
	 * @return {@code true} if this skill is a recover herb, {@code false} otherwise
	 */
	public boolean isRecoveryHerb()
	{
		return _isRecoveryHerb;
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public int getDisplayLevel()
	{
		return _displayLevel;
	}
	
	/**
	 * Return skill basicProperty base stat (STR, INT ...).
	 * @return
	 */
	public BaseStat getBasicProperty()
	{
		return _basicProperty;
	}
	
	/**
	 * @return Returns the how much items will be consumed.
	 */
	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}
	
	/**
	 * @return Returns the ID of item for consume.
	 */
	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	/**
	 * @return Returns the level.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return Returns true to set physical skills.
	 */
	public boolean isPhysical()
	{
		return _magic == 0;
	}
	
	/**
	 * @return Returns true to set magic skills.
	 */
	public boolean isMagic()
	{
		return _magic == 1;
	}
	
	/**
	 * @return Returns true to set static skills.
	 */
	public boolean isStatic()
	{
		return _magic == 2;
	}
	
	/**
	 * @return Returns true to set dance skills.
	 */
	public boolean isDance()
	{
		return _magic == 3;
	}
	
	/**
	 * @return Returns true to set static reuse.
	 */
	public boolean isStaticReuse()
	{
		return _staticReuse;
	}
	
	/**
	 * @return Returns the mpConsume.
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	/**
	 * @return Returns the mpInitialConsume.
	 */
	public int getMpInitialConsume()
	{
		return _mpInitialConsume;
	}
	
	/**
	 * @return Mana consumption per channeling tick.
	 */
	public int getMpPerChanneling()
	{
		return _mpPerChanneling;
	}
	
	/**
	 * @return the skill name
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the reuse delay
	 */
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	public int getReuseHashCode()
	{
		return _reuseHashCode;
	}
	
	public int getHitTime()
	{
		return _hitTime;
	}
	
	/**
	 * @return the cool time
	 */
	public int getCoolTime()
	{
		return _coolTime;
	}
	
	public int getAffectRange()
	{
		return _affectRange;
	}
	
	public int getAffectLimit()
	{
		return (_affectLimit[0] + Rnd.get(_affectLimit[1]));
	}
	
	public boolean isActive()
	{
		return (_operateType != null) && _operateType.isActive();
	}
	
	public boolean isPassive()
	{
		return (_operateType != null) && _operateType.isPassive();
	}
	
	public boolean isToggle()
	{
		return (_operateType != null) && _operateType.isToggle();
	}
	
	public boolean isContinuous()
	{
		return ((_operateType != null) && _operateType.isContinuous()) || isSelfContinuous();
	}
	
	public boolean isSelfContinuous()
	{
		return (_operateType != null) && _operateType.isSelfContinuous();
	}
	
	public boolean isChanneling()
	{
		return (_operateType != null) && _operateType.isChanneling();
	}
	
	public boolean isTriggeredSkill()
	{
		return _isTriggeredSkill;
	}
	
	/**
	 * Verify if the skill is a transformation skill.
	 * @return {@code true} if the skill is a transformation, {@code false} otherwise
	 */
	public boolean isTransformation()
	{
		return _abnormalType == AbnormalType.TRANSFORM;
	}
	
	public int getEffectPoint()
	{
		return _effectPoint;
	}
	
	public boolean useSoulShot()
	{
		return hasEffectType(EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK);
	}
	
	public boolean useSpiritShot()
	{
		return _magic == 1;
	}
	
	public boolean useFishShot()
	{
		return hasEffectType(EffectType.FISHING);
	}
	
	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}
	
	public boolean isHeroSkill()
	{
		return _isHeroSkill;
	}
	
	public boolean isGMSkill()
	{
		return _isGMSkill;
	}
	
	public boolean is7Signs()
	{
		return _isSevenSigns;
	}
	
	/**
	 * Verify if this is a healing potion skill.
	 * @return {@code true} if this is a healing potion skill, {@code false} otherwise
	 */
	public boolean isHealingPotionSkill()
	{
		return _abnormalType == AbnormalType.HP_RECOVER;
	}
	
	public int getChargeConsume()
	{
		return _chargeConsume;
	}
	
	public int getMaxSoulConsumeCount()
	{
		return _soulMaxConsume;
	}
	
	public int getBaseCritRate()
	{
		return _baseCritRate;
	}
	
	public boolean getDmgDirectlyToHP()
	{
		return _directHpDmg;
	}
	
	public FlyType getFlyType()
	{
		return _flyType;
	}
	
	public int getFlyRadius()
	{
		return _flyRadius;
	}
	
	public float getFlyCourse()
	{
		return _flyCourse;
	}
	
	public boolean isStayAfterDeath()
	{
		return _stayAfterDeath;
	}
	
	public boolean isStayOnSubclassChange()
	{
		return _stayOnSubclassChange;
	}
	
	public boolean isBad()
	{
		return (_effectPoint < 0) && (_targetType != TargetType.SELF);
	}
	
	public boolean checkCondition(Creature creature, WorldObject object, boolean itemOrWeapon)
	{
		if (creature.isFakePlayer() || (creature.canOverrideCond(PlayerCondOverride.SKILL_CONDITIONS) && !Config.GM_SKILL_RESTRICTION))
		{
			return true;
		}
		
		if (creature.isPlayer() && creature.getActingPlayer().isMounted() && isBad() && !MountEnabledSkillList.contains(_id))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(_id);
			creature.sendPacket(sm);
			return false;
		}
		
		final List<Condition> preCondition = itemOrWeapon ? _itemPreCondition : _preCondition;
		if ((preCondition == null) || preCondition.isEmpty())
		{
			return true;
		}
		
		final Creature target = object instanceof Creature ? (Creature) object : null;
		for (Condition cond : preCondition)
		{
			if (!cond.test(creature, target, this))
			{
				final String msg = cond.getMessage();
				final int msgId = cond.getMessageId();
				if (msgId != 0)
				{
					final SystemMessage sm = new SystemMessage(msgId);
					if (cond.isAddName())
					{
						sm.addSkillName(_id);
					}
					creature.sendPacket(sm);
				}
				else if (msg != null)
				{
					creature.sendMessage(msg);
				}
				return false;
			}
		}
		return true;
	}
	
	public WorldObject[] getTargetList(Creature creature, boolean onlyFirst)
	{
		// Init to null the target of the skill
		Creature target = null;
		
		// Get the L2Objcet targeted by the user of the skill at this moment
		final WorldObject objTarget = creature.getTarget();
		// If the WorldObject targeted is a Creature, it becomes the Creature target
		if ((objTarget != null) && objTarget.isCreature())
		{
			target = (Creature) objTarget;
		}
		return getTargetList(creature, onlyFirst, target);
	}
	
	/**
	 * Return all targets of the skill in a table in function a the skill type.<br>
	 * <br>
	 * <b><u>Values of skill type</u>:</b>
	 * <ul>
	 * <li>ONE : The skill can only be used on the PlayerInstance targeted, or on the caster if it's a PlayerInstance and no PlayerInstance targeted</li>
	 * <li>SELF</li>
	 * <li>HOLY, UNDEAD</li>
	 * <li>PET</li>
	 * <li>AURA, AURA_CLOSE</li>
	 * <li>AREA</li>
	 * <li>MULTIFACE</li>
	 * <li>PARTY, CLAN</li>
	 * <li>CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN</li>
	 * <li>UNLOCKABLE</li>
	 * <li>ITEM</li>
	 * <ul>
	 * @param creature The Creature who use the skill
	 * @param onlyFirst
	 * @param target
	 * @return
	 */
	public WorldObject[] getTargetList(Creature creature, boolean onlyFirst, Creature target)
	{
		final ITargetTypeHandler handler = TargetHandler.getInstance().getHandler(getTargetType());
		if (handler != null)
		{
			try
			{
				return handler.getTargetList(this, creature, onlyFirst, target);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in Skill.getTargetList(): " + e.getMessage(), e);
			}
		}
		creature.sendMessage("Target type of skill is not currently handled.");
		return EMPTY_TARGET_LIST;
	}
	
	public WorldObject[] getTargetList(Creature creature)
	{
		return getTargetList(creature, false);
	}
	
	public WorldObject getFirstOfTargetList(Creature creature)
	{
		final WorldObject[] targets = getTargetList(creature, true);
		if (targets.length == 0)
		{
			return null;
		}
		return targets[0];
	}
	
	/**
	 * Check if should be target added to the target list false if target is dead, target same as caster,<br>
	 * target inside peace zone, target in the same party with caster, caster can see target Additional checks if not in PvP zones (arena, siege):<br>
	 * target in not the same clan and alliance with caster, and usual skill PvP check. If TvT event is active - performing additional checks. Caution: distance is not checked.
	 * @param caster
	 * @param target
	 * @param skill
	 * @param sourceInArena
	 * @return
	 */
	public static boolean checkForAreaOffensiveSkills(Creature caster, Creature target, Skill skill, boolean sourceInArena)
	{
		if ((target == null) || target.isDead() || (target == caster))
		{
			return false;
		}
		
		final PlayerInstance player = caster.getActingPlayer();
		final PlayerInstance targetPlayer = target.getActingPlayer();
		if (player != null)
		{
			if (targetPlayer != null)
			{
				if ((targetPlayer == caster) || (targetPlayer == player))
				{
					return false;
				}
				
				if (targetPlayer.inObserverMode())
				{
					return false;
				}
				
				if (skill.isBad() && (player.getSiegeState() > 0) && player.isInsideZone(ZoneId.SIEGE) && (player.getSiegeState() == targetPlayer.getSiegeState()) && (player.getSiegeSide() == targetPlayer.getSiegeSide()))
				{
					return false;
				}
				
				if (skill.isBad() && target.isInsideZone(ZoneId.PEACE))
				{
					return false;
				}
				
				if (player.isInParty() && targetPlayer.isInParty())
				{
					// Same party
					if (player.getParty().getLeaderObjectId() == targetPlayer.getParty().getLeaderObjectId())
					{
						return false;
					}
					
					// Same command channel
					if (player.getParty().isInCommandChannel() && (player.getParty().getCommandChannel() == targetPlayer.getParty().getCommandChannel()))
					{
						return false;
					}
				}
				
				if (player.isOnEvent() && !player.isOnSoloEvent() && targetPlayer.isOnEvent() && (player.getTeam() == target.getTeam()))
				{
					return false;
				}
				
				if (!sourceInArena && !(targetPlayer.isInsideZone(ZoneId.PVP) && !targetPlayer.isInsideZone(ZoneId.SIEGE)))
				{
					if ((player.getAllyId() != 0) && (player.getAllyId() == targetPlayer.getAllyId()))
					{
						return false;
					}
					
					if ((player.getClanId() != 0) && (player.getClanId() == targetPlayer.getClanId()))
					{
						return false;
					}
					
					if (!player.checkPvpSkill(targetPlayer, skill))
					{
						return false;
					}
				}
			}
		}
		else
		{
			// target is mob
			if ((targetPlayer == null) && target.isAttackable() && caster.isAttackable())
			{
				return false;
			}
		}
		
		if (!GeoEngine.getInstance().canSeeTarget(caster, target))
		{
			return false;
		}
		return true;
	}
	
	public static boolean addSummon(Creature caster, PlayerInstance owner, int radius, boolean isDead)
	{
		if (!owner.hasSummon())
		{
			return false;
		}
		return addCharacter(caster, owner.getSummon(), radius, isDead);
	}
	
	public static boolean addCharacter(Creature caster, Creature target, int radius, boolean isDead)
	{
		if (isDead != target.isDead())
		{
			return false;
		}
		
		if ((radius > 0) && !Util.checkIfInRange(radius, caster, target, true))
		{
			return false;
		}
		return true;
	}
	
	public List<AbstractFunction> getStatFuncs(AbstractEffect effect, Creature creature)
	{
		if ((_funcTemplates == null) || (creature == null))
		{
			return Collections.<AbstractFunction> emptyList();
		}
		
		if (!creature.isPlayable() && !creature.isAttackable())
		{
			return Collections.<AbstractFunction> emptyList();
		}
		
		final List<AbstractFunction> funcs = new ArrayList<>(_funcTemplates.size());
		for (FuncTemplate t : _funcTemplates)
		{
			final AbstractFunction f = t.getFunc(creature, null, this, this); // skill is owner
			if (f != null)
			{
				funcs.add(f);
			}
		}
		return funcs;
	}
	
	/**
	 * Gets the skill effects.
	 * @param effectScope the effect scope
	 * @return the list of effects for the give scope
	 */
	public List<AbstractEffect> getEffects(EffectScope effectScope)
	{
		return _effectLists.get(effectScope);
	}
	
	/**
	 * Verify if this skill has effects for the given scope.
	 * @param effectScope the effect scope
	 * @return {@code true} if this skill has effects for the given scope, {@code false} otherwise
	 */
	public boolean hasEffects(EffectScope effectScope)
	{
		final List<AbstractEffect> effects = _effectLists.get(effectScope);
		return (effects != null) && !effects.isEmpty();
	}
	
	/**
	 * Applies the effects from this skill to the target for the given effect scope.
	 * @param effectScope the effect scope
	 * @param info the buff info
	 * @param applyInstantEffects if {@code true} instant effects will be applied to the effected
	 * @param addContinuousEffects if {@code true} continuous effects will be applied to the effected
	 */
	public void applyEffectScope(EffectScope effectScope, BuffInfo info, boolean applyInstantEffects, boolean addContinuousEffects)
	{
		if ((effectScope != null) && hasEffects(effectScope))
		{
			for (AbstractEffect effect : getEffects(effectScope))
			{
				if (effect != null)
				{
					if (effect.isInstant())
					{
						if (applyInstantEffects && effect.calcSuccess(info))
						{
							effect.onStart(info);
						}
					}
					else if (addContinuousEffects && effect.canStart(info))
					{
						info.addEffect(effect);
					}
				}
			}
		}
	}
	
	/**
	 * Method overload for {@link Skill#applyEffects(Creature, Creature, boolean, boolean, boolean, int)}.<br>
	 * Simplify the calls.
	 * @param effector the caster of the skill
	 * @param effected the target of the effect
	 */
	public void applyEffects(Creature effector, Creature effected)
	{
		applyEffects(effector, effected, false, false, true, 0);
	}
	
	/**
	 * Method overload for {@link Skill#applyEffects(Creature, Creature, boolean, boolean, boolean, int)}.<br>
	 * Simplify the calls, allowing abnormal time time customization.
	 * @param effector the caster of the skill
	 * @param effected the target of the effect
	 * @param instant if {@code true} instant effects will be applied to the effected
	 * @param abnormalTime custom abnormal time, if equal or lesser than zero will be ignored
	 */
	public void applyEffects(Creature effector, Creature effected, boolean instant, int abnormalTime)
	{
		applyEffects(effector, effected, false, false, instant, abnormalTime);
	}
	
	/**
	 * Applies the effects from this skill to the target.
	 * @param effector the caster of the skill
	 * @param effected the target of the effect
	 * @param self if {@code true} self-effects will be casted on the caster
	 * @param passive if {@code true} passive effects will be applied to the effector
	 * @param instant if {@code true} instant effects will be applied to the effected
	 * @param abnormalTime custom abnormal time, if equal or lesser than zero will be ignored
	 */
	public void applyEffects(Creature effector, Creature effected, boolean self, boolean passive, boolean instant, int abnormalTime)
	{
		// null targets cannot receive any effects.
		if (effected == null)
		{
			return;
		}
		
		// Check bad skills against target.
		if ((effector != effected) && isBad() && (effected.isInvul() || (effector.isGM() && !effector.getAccessLevel().canGiveDamage())))
		{
			return;
		}
		
		if (effected.isInvulAgainst(_id, _level))
		{
			return;
		}
		
		boolean addContinuousEffects = !passive && (_operateType.isToggle() || (_operateType.isContinuous() && Formulas.calcEffectSuccess(effector, effected, this)));
		if (!self && !passive)
		{
			final BuffInfo info = new BuffInfo(effector, effected, this);
			if (addContinuousEffects && (abnormalTime > 0))
			{
				info.setAbnormalTime(abnormalTime);
			}
			
			applyEffectScope(EffectScope.GENERAL, info, instant, addContinuousEffects);
			
			final EffectScope pvpOrPveEffectScope = effector.isPlayable() && effected.isAttackable() ? EffectScope.PVE : effector.isPlayable() && effected.isPlayable() ? EffectScope.PVP : null;
			applyEffectScope(pvpOrPveEffectScope, info, instant, addContinuousEffects);
			applyEffectScope(EffectScope.CHANNELING, info, instant, addContinuousEffects);
			if (addContinuousEffects)
			{
				effected.getEffectList().add(info);
			}
			
			// Support for buff sharing feature including healing herbs.
			if (effected.isPlayer() && effected.hasServitor() && (_abnormalType != AbnormalType.TRANSFORM) && ((addContinuousEffects && isContinuous() && !_isDebuff) || _isRecoveryHerb))
			{
				applyEffects(effector, effected.getSummon(), _isRecoveryHerb, 0);
			}
		}
		
		if (self)
		{
			addContinuousEffects = !passive && (_operateType.isToggle() || ((_operateType.isContinuous() || _operateType.isSelfContinuous()) && Formulas.calcEffectSuccess(effector, effector, this)));
			
			final BuffInfo info = new BuffInfo(effector, effector, this);
			if (addContinuousEffects && (abnormalTime > 0))
			{
				info.setAbnormalTime(abnormalTime);
			}
			
			applyEffectScope(EffectScope.SELF, info, instant, addContinuousEffects);
			
			// TODO : Need to be done better after skill rework
			if (addContinuousEffects && hasEffectType(EffectType.BUFF))
			{
				info.getEffector().getEffectList().add(info);
			}
			
			// Support for buff sharing feature.
			// Avoiding Servitor Share since it's implementation already "shares" the effect.
			if (addContinuousEffects && info.getEffected().isPlayer() && info.getEffected().hasServitor() && isContinuous() && !_isDebuff && (getId() != CommonSkill.SERVITOR_SHARE.getId()))
			{
				applyEffects(effector, info.getEffected().getSummon(), false, 0);
			}
		}
		
		if (passive)
		{
			final BuffInfo info = new BuffInfo(effector, effector, this);
			applyEffectScope(EffectScope.PASSIVE, info, false, true);
			effector.getEffectList().add(info);
		}
	}
	
	/**
	 * Activates a skill for the given creature and targets.
	 * @param caster the caster
	 * @param targets the targets
	 */
	public void activateSkill(Creature caster, WorldObject... targets)
	{
		activateSkill(caster, null, targets);
	}
	
	/**
	 * Activates a skill for the given cubic and targets.
	 * @param cubic the cubic
	 * @param targets the targets
	 */
	public void activateSkill(CubicInstance cubic, WorldObject... targets)
	{
		activateSkill(cubic.getOwner(), cubic, targets);
	}
	
	/**
	 * Activates the skill to the targets.
	 * @param caster the caster
	 * @param cubic the cubic, can be {@code null}
	 * @param targets the targets
	 */
	private void activateSkill(Creature caster, CubicInstance cubic, WorldObject... targets)
	{
		switch (_id)
		{
			// TODO: replace with AI
			case 5852:
			case 5853:
			{
				final BlockInstance block = targets[0] instanceof BlockInstance ? (BlockInstance) targets[0] : null;
				final PlayerInstance player = caster.isPlayer() ? (PlayerInstance) caster : null;
				if ((block == null) || (player == null))
				{
					return;
				}
				
				final int arena = player.getBlockCheckerArena();
				if (arena != -1)
				{
					final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(arena);
					if (holder == null)
					{
						return;
					}
					
					final int team = holder.getPlayerTeam(player);
					final int color = block.getColorEffect();
					if ((team == 0) && (color == 0x00))
					{
						block.changeColor(player, holder, team);
					}
					else if ((team == 1) && (color == 0x53))
					{
						block.changeColor(player, holder, team);
					}
				}
				break;
			}
			default:
			{
				for (WorldObject obj : targets)
				{
					final Creature target = (Creature) obj;
					if (Formulas.calcBuffDebuffReflection(target, this))
					{
						// if skill is reflected instant effects should be casted on target
						// and continuous effects on caster
						applyEffects(target, caster, false, 0);
						
						final BuffInfo info = new BuffInfo(caster, target, this);
						applyEffectScope(EffectScope.GENERAL, info, true, false);
						
						final EffectScope pvpOrPveEffectScope = caster.isPlayable() && target.isAttackable() ? EffectScope.PVE : caster.isPlayable() && target.isPlayable() ? EffectScope.PVP : null;
						applyEffectScope(pvpOrPveEffectScope, info, true, false);
						applyEffectScope(EffectScope.CHANNELING, info, true, false);
					}
					else
					{
						applyEffects(caster, target);
					}
				}
				break;
			}
		}
		
		// Self Effect
		if (hasEffects(EffectScope.SELF))
		{
			if (caster.isAffectedBySkill(_id))
			{
				caster.stopSkillEffects(SkillFinishType.REMOVED, _id);
			}
			applyEffects(caster, caster, true, false, true, 0);
		}
		
		if (cubic == null)
		{
			if (useSpiritShot())
			{
				caster.setChargedShot(caster.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
			}
			else if (useSoulShot())
			{
				caster.setChargedShot(ShotType.SOULSHOTS, false);
			}
		}
		
		if (_isSuicideAttack)
		{
			caster.doDie(caster);
		}
	}
	
	public void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new ArrayList<>(1);
		}
		_funcTemplates.add(f);
	}
	
	/**
	 * Adds an effect to the effect list for the give effect scope.
	 * @param effectScope the effect scope
	 * @param effect the effect to add
	 */
	public void addEffect(EffectScope effectScope, AbstractEffect effect)
	{
		List<AbstractEffect> effects = _effectLists.get(effectScope);
		if (effects == null)
		{
			effects = new ArrayList<>(1);
			_effectLists.put(effectScope, effects);
		}
		effects.add(effect);
	}
	
	public void attach(Condition c, boolean itemOrWeapon)
	{
		if (itemOrWeapon)
		{
			if (_itemPreCondition == null)
			{
				_itemPreCondition = new ArrayList<>();
			}
			_itemPreCondition.add(c);
		}
		else
		{
			if (_preCondition == null)
			{
				_preCondition = new ArrayList<>();
			}
			_preCondition.add(c);
		}
	}
	
	@Override
	public String toString()
	{
		return "Skill " + _name + "(" + _id + "," + _level + ")";
	}
	
	/**
	 * @return pet food
	 */
	public int getFeed()
	{
		return _feed;
	}
	
	/**
	 * used for tracking item id in case that item consume cannot be used
	 * @return reference item id
	 */
	public int getReferenceItemId()
	{
		return _refId;
	}
	
	public void setReferenceItemId(int value)
	{
		_refId = value;
	}
	
	public String getAttributeName()
	{
		return _attribute;
	}
	
	/**
	 * @return the _blowChance
	 */
	public int getBlowChance()
	{
		return _blowChance;
	}
	
	public boolean ignoreShield()
	{
		return _ignoreShield;
	}
	
	public boolean canBeDispeled()
	{
		return _canBeDispeled;
	}
	
	/**
	 * Verify if the skill can be stolen.
	 * @return {@code true} if skill can be stolen, {@code false} otherwise
	 */
	public boolean canBeStolen()
	{
		return !isPassive() && !isToggle() && !_isDebuff && !_isHeroSkill && !_isGMSkill && !(isStatic() && (getId() != CommonSkill.CARAVANS_SECRET_MEDICINE.getId())) && _canBeDispeled && (getId() != CommonSkill.SERVITOR_SHARE.getId());
	}
	
	public boolean isClanSkill()
	{
		return _isClanSkill;
	}
	
	public boolean isExcludedFromCheck()
	{
		return _excludedFromCheck;
	}
	
	public boolean isSimultaneousCast()
	{
		return _simultaneousCast;
	}
	
	/**
	 * Parse an extractable skill.
	 * @param skillId the skill Id
	 * @param skillLevel the skill level
	 * @param values the values to parse
	 * @return the parsed extractable skill
	 * @author Zoey76
	 */
	private ExtractableSkill parseExtractableSkill(int skillId, int skillLevel, String values)
	{
		final String[] prodLists = values.split(";");
		final List<ExtractableProductItem> products = new ArrayList<>();
		String[] prodData;
		for (String prodList : prodLists)
		{
			prodData = prodList.split(",");
			if (prodData.length < 3)
			{
				LOGGER.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLevel + " -> wrong seperator!");
			}
			List<ItemHolder> items = null;
			double chance = 0;
			final int length = prodData.length - 1;
			try
			{
				items = new ArrayList<>(length / 2);
				for (int j = 0; j < length; j += 2)
				{
					final int prodId = Integer.parseInt(prodData[j]);
					final int quantity = Integer.parseInt(prodData[j + 1]);
					if ((prodId <= 0) || (quantity <= 0))
					{
						LOGGER.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLevel + " wrong production Id: " + prodId + " or wrond quantity: " + quantity + "!");
					}
					items.add(new ItemHolder(prodId, quantity));
				}
				chance = Double.parseDouble(prodData[length]);
			}
			catch (Exception e)
			{
				LOGGER.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLevel + " -> incomplete/invalid production data or wrong seperator!");
			}
			products.add(new ExtractableProductItem(items, chance));
		}
		
		if (products.isEmpty())
		{
			LOGGER.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLevel + " -> There are no production items!");
		}
		return new ExtractableSkill(SkillData.getSkillHashCode(skillId, skillLevel), products);
	}
	
	/**
	 * Parses all the abnormal visual effects.
	 * @param abnormalVisualEffects the abnormal visual effects list
	 */
	private void parseAbnormalVisualEffect(String abnormalVisualEffects)
	{
		if (abnormalVisualEffects != null)
		{
			final String[] data = abnormalVisualEffects.split(";");
			List<AbnormalVisualEffect> avesEvent = null;
			List<AbnormalVisualEffect> avesSpecial = null;
			List<AbnormalVisualEffect> aves = null;
			for (String ave2 : data)
			{
				final AbnormalVisualEffect ave = AbnormalVisualEffect.valueOf(ave2);
				if (ave != null)
				{
					if (ave.isEvent())
					{
						if (avesEvent == null)
						{
							avesEvent = new ArrayList<>(1);
						}
						avesEvent.add(ave);
						continue;
					}
					
					if (ave.isSpecial())
					{
						if (avesSpecial == null)
						{
							avesSpecial = new ArrayList<>(1);
						}
						avesSpecial.add(ave);
						continue;
					}
					
					if (aves == null)
					{
						aves = new ArrayList<>(1);
					}
					aves.add(ave);
				}
			}
			
			if (avesEvent != null)
			{
				_abnormalVisualEffectsEvent = avesEvent.toArray(new AbnormalVisualEffect[avesEvent.size()]);
			}
			
			if (avesSpecial != null)
			{
				_abnormalVisualEffectsSpecial = avesSpecial.toArray(new AbnormalVisualEffect[avesSpecial.size()]);
			}
			
			if (aves != null)
			{
				_abnormalVisualEffects = aves.toArray(new AbnormalVisualEffect[aves.size()]);
			}
		}
	}
	
	public ExtractableSkill getExtractableSkill()
	{
		return _extractableItems;
	}
	
	/**
	 * @param effectType Effect type to check if its present on this skill effects.
	 * @param effectTypes Effect types to check if are present on this skill effects.
	 * @return {@code true} if at least one of specified {@link EffectType} types is present on this skill effects, {@code false} otherwise.
	 */
	public boolean hasEffectType(EffectType effectType, EffectType... effectTypes)
	{
		if (_effectTypes == null)
		{
			synchronized (this)
			{
				if (_effectTypes == null)
				{
					final Set<Byte> effectTypesSet = new HashSet<>();
					for (List<AbstractEffect> effectList : _effectLists.values())
					{
						if (effectList != null)
						{
							for (AbstractEffect effect : effectList)
							{
								if (effect == null)
								{
									continue;
								}
								effectTypesSet.add((byte) effect.getEffectType().ordinal());
							}
						}
					}
					
					final Byte[] effectTypesArray = effectTypesSet.toArray(new Byte[effectTypesSet.size()]);
					Arrays.sort(effectTypesArray);
					_effectTypes = effectTypesArray;
				}
			}
		}
		
		if (Arrays.binarySearch(_effectTypes, (byte) effectType.ordinal()) >= 0)
		{
			return true;
		}
		
		for (EffectType type : effectTypes)
		{
			if (Arrays.binarySearch(_effectTypes, (byte) type.ordinal()) >= 0)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return icon of the current skill.
	 */
	public String getIcon()
	{
		return _icon;
	}
	
	public int getChannelingTickInterval()
	{
		return _channelingTickInterval;
	}
	
	public int getChannelingTickInitialDelay()
	{
		return _channelingTickInitialDelay;
	}
	
	public boolean isPvPOnly()
	{
		return _isPvPOnly;
	}
}