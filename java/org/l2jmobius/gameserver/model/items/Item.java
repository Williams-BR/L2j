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
package org.l2jmobius.gameserver.model.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.enums.ItemGrade;
import org.l2jmobius.gameserver.model.Elementals;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.events.ListenersContainer;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.interfaces.IIdentifiable;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.ActionType;
import org.l2jmobius.gameserver.model.items.type.CrystalType;
import org.l2jmobius.gameserver.model.items.type.EtcItemType;
import org.l2jmobius.gameserver.model.items.type.ItemType;
import org.l2jmobius.gameserver.model.items.type.MaterialType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.model.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<br>
 * Mother class of :
 * <ul>
 * <li>Armor</li>
 * <li>EtcItem</li>
 * <li>Weapon</li>
 * </ul>
 */
public abstract class Item extends ListenersContainer implements IIdentifiable
{
	protected static final Logger LOGGER = Logger.getLogger(Item.class.getName());
	
	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;
	
	public static final int TYPE2_WEAPON = 0;
	public static final int TYPE2_SHIELD_ARMOR = 1;
	public static final int TYPE2_ACCESSORY = 2;
	public static final int TYPE2_QUEST = 3;
	public static final int TYPE2_MONEY = 4;
	public static final int TYPE2_OTHER = 5;
	
	public static final int SLOT_NONE = 0x0000;
	public static final int SLOT_UNDERWEAR = 0x0001;
	public static final int SLOT_R_EAR = 0x0002;
	public static final int SLOT_L_EAR = 0x0004;
	public static final int SLOT_LR_EAR = 0x00006;
	public static final int SLOT_NECK = 0x0008;
	public static final int SLOT_R_FINGER = 0x0010;
	public static final int SLOT_L_FINGER = 0x0020;
	public static final int SLOT_LR_FINGER = 0x0030;
	public static final int SLOT_HEAD = 0x0040;
	public static final int SLOT_R_HAND = 0x0080;
	public static final int SLOT_L_HAND = 0x0100;
	public static final int SLOT_GLOVES = 0x0200;
	public static final int SLOT_CHEST = 0x0400;
	public static final int SLOT_LEGS = 0x0800;
	public static final int SLOT_FEET = 0x1000;
	public static final int SLOT_BACK = 0x2000;
	public static final int SLOT_LR_HAND = 0x4000;
	public static final int SLOT_FULL_ARMOR = 0x8000;
	public static final int SLOT_HAIR = 0x010000;
	public static final int SLOT_ALLDRESS = 0x020000;
	public static final int SLOT_HAIR2 = 0x040000;
	public static final int SLOT_HAIRALL = 0x080000;
	public static final int SLOT_R_BRACELET = 0x100000;
	public static final int SLOT_L_BRACELET = 0x200000;
	public static final int SLOT_DECO = 0x400000;
	public static final int SLOT_BELT = 0x10000000;
	public static final int SLOT_WOLF = -100;
	public static final int SLOT_HATCHLING = -101;
	public static final int SLOT_STRIDER = -102;
	public static final int SLOT_BABYPET = -103;
	public static final int SLOT_GREATWOLF = -104;
	
	public static final int SLOT_MULTI_ALLWEAPON = SLOT_LR_HAND | SLOT_R_HAND;
	
	private int _itemId;
	private int _displayId;
	private String _name;
	private String _icon;
	private int _weight;
	private boolean _stackable;
	private MaterialType _materialType;
	private CrystalType _crystalType;
	private int _equipReuseDelay;
	private int _duration;
	private long _time;
	private int _autoDestroyTime;
	private int _bodyPart;
	private int _referencePrice;
	private int _crystalCount;
	private boolean _sellable;
	private boolean _dropable;
	private boolean _destroyable;
	private boolean _tradeable;
	private boolean _depositable;
	private boolean _enchantable;
	private boolean _elementable;
	private boolean _questItem;
	private boolean _freightable;
	private boolean _allowSelfResurrection;
	private boolean _isOlyRestricted;
	private boolean _forNpc;
	private boolean _common;
	private boolean _heroItem;
	private boolean _pvpItem;
	private boolean _immediateEffect;
	private boolean _exImmediateEffect;
	private int _defaultEnchantLevel;
	private ActionType _defaultAction;
	
	protected int _type1; // needed for item list (inventory)
	protected int _type2; // different lists for armor, weapon, etc
	protected Elementals[] _elementals = null;
	protected List<FuncTemplate> _funcTemplates;
	protected List<Condition> _preConditions;
	private SkillHolder[] _skillHolder;
	private SkillHolder _unequipSkill = null;
	
	private int _useSkillDisTime;
	private int _reuseDelay;
	private int _sharedReuseGroup;
	
	/**
	 * Constructor of the Item that fill class variables.
	 * @param set : StatSet corresponding to a set of couples (key,value) for description of the item
	 */
	protected Item(StatSet set)
	{
		set(set);
	}
	
	public void set(StatSet set)
	{
		_itemId = set.getInt("item_id");
		_displayId = set.getInt("displayId", _itemId);
		_name = set.getString("name");
		_icon = set.getString("icon", null);
		_weight = set.getInt("weight", 0);
		_materialType = set.getEnum("material", MaterialType.class, MaterialType.STEEL);
		_equipReuseDelay = set.getInt("equip_reuse_delay", 0) * 1000;
		_duration = set.getInt("duration", -1);
		_time = set.getInt("time", -1);
		_autoDestroyTime = set.getInt("auto_destroy_time", -1) * 1000;
		_bodyPart = ItemTable.SLOTS.get(set.getString("bodypart", "none"));
		_referencePrice = set.getInt("price", 0);
		_crystalType = set.getEnum("crystal_type", CrystalType.class, CrystalType.NONE);
		_crystalCount = set.getInt("crystal_count", 0);
		_stackable = set.getBoolean("is_stackable", false);
		_sellable = set.getBoolean("is_sellable", true);
		_dropable = set.getBoolean("is_dropable", true);
		_destroyable = set.getBoolean("is_destroyable", true);
		_tradeable = set.getBoolean("is_tradable", true);
		_depositable = set.getBoolean("is_depositable", true);
		_elementable = set.getBoolean("element_enabled", false);
		_enchantable = set.getBoolean("enchant_enabled", false);
		_questItem = set.getBoolean("is_questitem", false);
		_freightable = set.getBoolean("is_freightable", false);
		_allowSelfResurrection = set.getBoolean("allow_self_resurrection", false);
		_isOlyRestricted = set.getBoolean("is_oly_restricted", false);
		_forNpc = set.getBoolean("for_npc", false);
		_immediateEffect = set.getBoolean("immediate_effect", false);
		_exImmediateEffect = set.getBoolean("ex_immediate_effect", false);
		_defaultAction = set.getEnum("default_action", ActionType.class, ActionType.NONE);
		_useSkillDisTime = set.getInt("useSkillDisTime", 0);
		_defaultEnchantLevel = set.getInt("enchanted", 0);
		_reuseDelay = set.getInt("reuse_delay", 0);
		_sharedReuseGroup = set.getInt("shared_reuse_group", 0);
		String skills = set.getString("item_skill", null);
		if (skills != null)
		{
			final String[] skillsSplit = skills.split(";");
			_skillHolder = new SkillHolder[skillsSplit.length];
			int used = 0;
			for (String element : skillsSplit)
			{
				try
				{
					final String[] skillSplit = element.split("-");
					final int id = Integer.parseInt(skillSplit[0]);
					final int level = Integer.parseInt(skillSplit[1]);
					if (id == 0)
					{
						LOGGER.info("Ignoring item_skill(" + element + ") for item " + this + ". Skill id is 0!");
						continue;
					}
					
					if (level == 0)
					{
						LOGGER.info("Ignoring item_skill(" + element + ") for item " + this + ". Skill level is 0!");
						continue;
					}
					
					_skillHolder[used] = new SkillHolder(id, level);
					++used;
				}
				catch (Exception e)
				{
					LOGGER.warning("Failed to parse item_skill(" + element + ") for item " + this + "! Format: SkillId0-SkillLevel0[;SkillIdN-SkillLevelN]");
				}
			}
			
			// this is only loading? just don't leave a null or use a collection?
			if (used != _skillHolder.length)
			{
				final SkillHolder[] skillHolder = new SkillHolder[used];
				System.arraycopy(_skillHolder, 0, skillHolder, 0, used);
				_skillHolder = skillHolder;
			}
		}
		
		skills = set.getString("unequip_skill", null);
		if (skills != null)
		{
			final String[] info = skills.split("-");
			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, don't add new skill
					LOGGER.info("Couldnt parse " + skills + " in weapon unequip skills! item " + this);
				}
				if ((id > 0) && (level > 0))
				{
					_unequipSkill = new SkillHolder(id, level);
				}
			}
		}
		
		_common = ((_itemId >= 11605) && (_itemId <= 12361));
		_heroItem = ((_itemId >= 6611) && (_itemId <= 6621)) || ((_itemId >= 9388) && (_itemId <= 9390)) || (_itemId == 6842);
		_pvpItem = ((_itemId >= 10667) && (_itemId <= 10835)) || ((_itemId >= 12852) && (_itemId <= 12977)) || ((_itemId >= 14363) && (_itemId <= 14525)) || (_itemId == 14528) || (_itemId == 14529) || (_itemId == 14558) || ((_itemId >= 15913) && (_itemId <= 16024)) || ((_itemId >= 16134) && (_itemId <= 16147)) || (_itemId == 16149) || (_itemId == 16151) || (_itemId == 16153) || (_itemId == 16155) || (_itemId == 16157) || (_itemId == 16159) || ((_itemId >= 16168) && (_itemId <= 16176)) || ((_itemId >= 16179) && (_itemId <= 16220)) || ((_itemId >= 21932) && (_itemId <= 21934)) || (_itemId == 21938) || ((_itemId >= 21952) && (_itemId <= 21954)) || (_itemId == 21958) || ((_itemId >= 21970) && (_itemId <= 21972));
	}
	
	/**
	 * Returns the itemType.
	 * @return Enum
	 */
	public abstract ItemType getItemType();
	
	/**
	 * Verifies if the item is an etc item.
	 * @return {@code true} if the item is an etc item, {@code false} otherwise.
	 */
	public boolean isEtcItem()
	{
		return false;
	}
	
	/**
	 * Verifies if the item is an armor.
	 * @return {@code true} if the item is an armor, {@code false} otherwise.
	 */
	public boolean isArmor()
	{
		return false;
	}
	
	/**
	 * Verifies if the item is a weapon.
	 * @return {@code true} if the item is a weapon, {@code false} otherwise.
	 */
	public boolean isWeapon()
	{
		return false;
	}
	
	/**
	 * Verifies if the item is a magic weapon.
	 * @return {@code true} if the weapon is magic, {@code false} otherwise.
	 */
	public boolean isMagicWeapon()
	{
		return false;
	}
	
	/**
	 * @return the _equipReuseDelay
	 */
	public int getEquipReuseDelay()
	{
		return _equipReuseDelay;
	}
	
	/**
	 * Returns the duration of the item
	 * @return int
	 */
	public int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Returns the time of the item
	 * @return long
	 */
	public long getTime()
	{
		return _time;
	}
	
	/**
	 * @return the auto destroy time of the item in seconds: 0 or less - default
	 */
	public int getAutoDestroyTime()
	{
		return _autoDestroyTime;
	}
	
	/**
	 * Returns the ID of the item
	 * @return int
	 */
	@Override
	public int getId()
	{
		return _itemId;
	}
	
	/**
	 * Returns the ID of the item
	 * @return int
	 */
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public abstract int getItemMask();
	
	/**
	 * Return the type of material of the item
	 * @return MaterialType
	 */
	public MaterialType getMaterialType()
	{
		return _materialType;
	}
	
	/**
	 * Returns the type 2 of the item
	 * @return int
	 */
	public int getType2()
	{
		return _type2;
	}
	
	/**
	 * Returns the weight of the item
	 * @return int
	 */
	public int getWeight()
	{
		return _weight;
	}
	
	/**
	 * Returns if the item is crystallizable
	 * @return boolean
	 */
	public boolean isCrystallizable()
	{
		return (_crystalType != CrystalType.NONE) && (_crystalCount > 0);
	}
	
	/**
	 * @return return General item grade (No S80, S84)
	 */
	public ItemGrade getItemGrade()
	{
		return ItemGrade.valueOf(_crystalType);
	}
	
	/**
	 * Return the type of crystal if item is crystallizable
	 * @return CrystalType
	 */
	public CrystalType getCrystalType()
	{
		return _crystalType;
	}
	
	/**
	 * Return the ID of crystal if item is crystallizable
	 * @return int
	 */
	public int getCrystalItemId()
	{
		return _crystalType.getCrystalId();
	}
	
	/**
	 * For grades S80 and S84 return S
	 * @return the grade of the item.
	 */
	public CrystalType getCrystalTypePlus()
	{
		switch (_crystalType)
		{
			case S80:
			case S84:
			{
				return CrystalType.S;
			}
			default:
			{
				return _crystalType;
			}
		}
	}
	
	/**
	 * @return the quantity of crystals for crystallization.
	 */
	public int getCrystalCount()
	{
		return _crystalCount;
	}
	
	/**
	 * @param enchantLevel
	 * @return the quantity of crystals for crystallization on specific enchant level
	 */
	public int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
				{
					return _crystalCount + (_crystalType.getCrystalEnchantBonusArmor() * ((3 * enchantLevel) - 6));
				}
				case TYPE2_WEAPON:
				{
					return _crystalCount + (_crystalType.getCrystalEnchantBonusWeapon() * ((2 * enchantLevel) - 3));
				}
				default:
				{
					return _crystalCount;
				}
			}
		}
		if (enchantLevel <= 0)
		{
			return _crystalCount;
		}
		switch (_type2)
		{
			case TYPE2_SHIELD_ARMOR:
			case TYPE2_ACCESSORY:
			{
				return _crystalCount + (_crystalType.getCrystalEnchantBonusArmor() * enchantLevel);
			}
			case TYPE2_WEAPON:
			{
				return _crystalCount + (_crystalType.getCrystalEnchantBonusWeapon() * enchantLevel);
			}
			default:
			{
				return _crystalCount;
			}
		}
	}
	
	/**
	 * @return the name of the item.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the base elemental of the item.
	 */
	public Elementals[] getElementals()
	{
		return _elementals;
	}
	
	public Elementals getElemental(byte attribute)
	{
		for (Elementals elm : _elementals)
		{
			if (elm.getElement() == attribute)
			{
				return elm;
			}
		}
		return null;
	}
	
	/**
	 * Sets the base elemental of the item.
	 * @param element the element to set.
	 */
	public void setElementals(Elementals element)
	{
		if (_elementals == null)
		{
			_elementals = new Elementals[1];
			_elementals[0] = element;
		}
		else
		{
			Elementals elm = getElemental(element.getElement());
			if (elm != null)
			{
				elm.setValue(element.getValue());
			}
			else
			{
				elm = element;
				final Elementals[] array = new Elementals[_elementals.length + 1];
				System.arraycopy(_elementals, 0, array, 0, _elementals.length);
				array[_elementals.length] = elm;
				_elementals = array;
			}
		}
	}
	
	/**
	 * @return the part of the body used with the item.
	 */
	public int getBodyPart()
	{
		return _bodyPart;
	}
	
	/**
	 * @return the type 1 of the item.
	 */
	public int getType1()
	{
		return _type1;
	}
	
	/**
	 * @return {@code true} if the item is stackable, {@code false} otherwise.
	 */
	public boolean isStackable()
	{
		return _stackable;
	}
	
	/**
	 * @return {@code true} if the item can be equipped, {@code false} otherwise.
	 */
	public boolean isEquipable()
	{
		return (_bodyPart != 0) && !(getItemType() instanceof EtcItemType);
	}
	
	/**
	 * @return the price of reference of the item.
	 */
	public int getReferencePrice()
	{
		return _referencePrice;
	}
	
	/**
	 * @return {@code true} if the item can be sold, {@code false} otherwise.
	 */
	public boolean isSellable()
	{
		return _sellable;
	}
	
	/**
	 * @return {@code true} if the item can be dropped, {@code false} otherwise.
	 */
	public boolean isDropable()
	{
		return _dropable;
	}
	
	/**
	 * @return {@code true} if the item can be destroyed, {@code false} otherwise.
	 */
	public boolean isDestroyable()
	{
		return _destroyable;
	}
	
	/**
	 * @return {@code true} if the item can be traded, {@code false} otherwise.
	 */
	public boolean isTradeable()
	{
		return _tradeable;
	}
	
	/**
	 * @return {@code true} if the item can be put into warehouse, {@code false} otherwise.
	 */
	public boolean isDepositable()
	{
		return _depositable;
	}
	
	/**
	 * This method also check the enchant blacklist.
	 * @return {@code true} if the item can be enchanted, {@code false} otherwise.
	 */
	public boolean isEnchantable()
	{
		return (Arrays.binarySearch(Config.ENCHANT_BLACKLIST, _itemId) < 0) && _enchantable;
	}
	
	/**
	 * @return {@code true} if the item can be elemented, {@code false} otherwise.
	 */
	public boolean isElementable()
	{
		return _elementable;
	}
	
	/**
	 * Returns if item is common
	 * @return boolean
	 */
	public boolean isCommon()
	{
		return _common;
	}
	
	/**
	 * Returns if item is hero-only
	 * @return
	 */
	public boolean isHeroItem()
	{
		return _heroItem;
	}
	
	/**
	 * Returns if item is pvp
	 * @return
	 */
	public boolean isPvpItem()
	{
		return _pvpItem;
	}
	
	public boolean isPotion()
	{
		return getItemType() == EtcItemType.POTION;
	}
	
	public boolean isElixir()
	{
		return getItemType() == EtcItemType.ELIXIR;
	}
	
	public boolean isScroll()
	{
		return getItemType() == EtcItemType.SCROLL;
	}
	
	/**
	 * Get the functions used by this item.
	 * @param item : ItemInstance pointing out the item
	 * @param creature : Creature pointing out the player
	 * @return the list of functions
	 */
	public List<AbstractFunction> getStatFuncs(ItemInstance item, Creature creature)
	{
		if ((_funcTemplates == null) || _funcTemplates.isEmpty())
		{
			return Collections.<AbstractFunction> emptyList();
		}
		
		final List<AbstractFunction> funcs = new ArrayList<>(_funcTemplates.size());
		for (FuncTemplate t : _funcTemplates)
		{
			final AbstractFunction f = t.getFunc(creature, creature, item, item);
			if (f != null)
			{
				funcs.add(f);
			}
		}
		return funcs;
	}
	
	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * @param f : FuncTemplate to add
	 */
	public void attach(FuncTemplate f)
	{
		switch (f.getStat())
		{
			case FIRE_RES:
			case FIRE_POWER:
			{
				setElementals(new Elementals(Elementals.FIRE, (int) f.getValue()));
				break;
			}
			case WATER_RES:
			case WATER_POWER:
			{
				setElementals(new Elementals(Elementals.WATER, (int) f.getValue()));
				break;
			}
			case WIND_RES:
			case WIND_POWER:
			{
				setElementals(new Elementals(Elementals.WIND, (int) f.getValue()));
				break;
			}
			case EARTH_RES:
			case EARTH_POWER:
			{
				setElementals(new Elementals(Elementals.EARTH, (int) f.getValue()));
				break;
			}
			case HOLY_RES:
			case HOLY_POWER:
			{
				setElementals(new Elementals(Elementals.HOLY, (int) f.getValue()));
				break;
			}
			case DARK_RES:
			case DARK_POWER:
			{
				setElementals(new Elementals(Elementals.DARK, (int) f.getValue()));
				break;
			}
		}
		
		if (_funcTemplates == null)
		{
			_funcTemplates = new ArrayList<>(1);
		}
		_funcTemplates.add(f);
	}
	
	public void attach(Condition c)
	{
		if (_preConditions == null)
		{
			_preConditions = new ArrayList<>(1);
		}
		if (!_preConditions.contains(c))
		{
			_preConditions.add(c);
		}
	}
	
	public boolean hasSkills()
	{
		return _skillHolder != null;
	}
	
	/**
	 * Method to retrieve skills linked to this item armor and weapon: passive skills etcitem: skills used on item use <-- ???
	 * @return Skills linked to this item as SkillHolder[]
	 */
	public SkillHolder[] getSkills()
	{
		return _skillHolder;
	}
	
	/**
	 * @return skill that activates, when player unequip this weapon or armor
	 */
	public Skill getUnequipSkill()
	{
		return _unequipSkill == null ? null : _unequipSkill.getSkill();
	}
	
	public boolean checkCondition(Creature creature, WorldObject object, boolean sendMessage)
	{
		if (creature.canOverrideCond(PlayerCondOverride.ITEM_CONDITIONS) && !Config.GM_ITEM_RESTRICTION)
		{
			return true;
		}
		
		// Don't allow hero equipment and restricted items during Olympiad
		if ((isOlyRestrictedItem() || _heroItem) && creature.isPlayer() && creature.getActingPlayer().isInOlympiadMode())
		{
			if (isEquipable())
			{
				creature.sendPacket(SystemMessageId.YOU_CANNOT_EQUIP_THAT_ITEM_IN_A_GRAND_OLYMPIAD_MATCH);
			}
			else
			{
				creature.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_ITEM_IN_A_GRAND_OLYMPIAD_MATCH);
			}
			return false;
		}
		
		if (!isConditionAttached())
		{
			return true;
		}
		
		final Creature target = object.isCreature() ? (Creature) object : null;
		for (Condition preCondition : _preConditions)
		{
			if (preCondition == null)
			{
				continue;
			}
			
			if (!preCondition.test(creature, target, null, null))
			{
				if (creature.isSummon())
				{
					creature.sendPacket(SystemMessageId.THIS_PET_CANNOT_USE_THIS_ITEM);
					return false;
				}
				
				if (sendMessage)
				{
					final String msg = preCondition.getMessage();
					final int msgId = preCondition.getMessageId();
					if (msg != null)
					{
						creature.sendMessage(msg);
					}
					else if (msgId != 0)
					{
						final SystemMessage sm = new SystemMessage(msgId);
						if (preCondition.isAddName())
						{
							sm.addItemName(_itemId);
						}
						creature.sendPacket(sm);
					}
				}
				return false;
			}
		}
		return true;
	}
	
	public boolean isConditionAttached()
	{
		return (_preConditions != null) && !_preConditions.isEmpty();
	}
	
	public boolean isQuestItem()
	{
		return _questItem;
	}
	
	public boolean isFreightable()
	{
		return _freightable;
	}
	
	public boolean isAllowSelfResurrection()
	{
		return _allowSelfResurrection;
	}
	
	public boolean isOlyRestrictedItem()
	{
		return _isOlyRestricted || Config.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId);
	}
	
	public boolean isForNpc()
	{
		return _forNpc;
	}
	
	/**
	 * Returns the name of the item followed by the item ID.
	 * @return the name and the ID of the item
	 */
	@Override
	public String toString()
	{
		return _name + "(" + _itemId + ")";
	}
	
	/**
	 * Verifies if the item has effects immediately.<br>
	 * <i>Used for herbs mostly.</i>
	 * @return {@code true} if the item applies effects immediately, {@code false} otherwise
	 */
	public boolean hasExImmediateEffect()
	{
		return _exImmediateEffect;
	}
	
	/**
	 * Verifies if the item has effects immediately.
	 * @return {@code true} if the item applies effects immediately, {@code false} otherwise
	 */
	public boolean hasImmediateEffect()
	{
		return _immediateEffect;
	}
	
	/**
	 * @return the _default_action
	 */
	public ActionType getDefaultAction()
	{
		return _defaultAction;
	}
	
	public int useSkillDisTime()
	{
		return _useSkillDisTime;
	}
	
	/**
	 * Gets the item reuse delay time in seconds.
	 * @return the reuse delay time
	 */
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	/**
	 * Gets the shared reuse group.<br>
	 * Items with the same reuse group will render reuse delay upon those items when used.
	 * @return the shared reuse group
	 */
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}
	
	/**
	 * Usable in HTML windows.
	 * @return the icon link in client files
	 */
	public String getIcon()
	{
		return _icon;
	}
	
	public int getDefaultEnchantLevel()
	{
		return _defaultEnchantLevel;
	}
	
	public boolean isPetItem()
	{
		return getItemType() == EtcItemType.PET_COLLAR;
	}
	
	public Skill getEnchant4Skill()
	{
		return null;
	}
}