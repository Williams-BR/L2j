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
package org.l2jmobius.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.ai.PlayerAI;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.cache.WarehouseCacheManager;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.data.sql.CharNameTable;
import org.l2jmobius.gameserver.data.sql.CharSummonTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.FishData;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.PlayerXpPercentLostData;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.data.xml.SendMessageLocalisationData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.CategoryType;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.HtmlActionScope;
import org.l2jmobius.gameserver.enums.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.enums.ItemLocation;
import org.l2jmobius.gameserver.enums.MountType;
import org.l2jmobius.gameserver.enums.PartyDistributionType;
import org.l2jmobius.gameserver.enums.PlayerAction;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.enums.Sex;
import org.l2jmobius.gameserver.enums.ShortcutType;
import org.l2jmobius.gameserver.enums.ShotType;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.enums.Team;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.instancemanager.AntiFeedManager;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.CoupleManager;
import org.l2jmobius.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jmobius.gameserver.instancemanager.DimensionalRiftManager;
import org.l2jmobius.gameserver.instancemanager.DuelManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import org.l2jmobius.gameserver.instancemanager.GrandBossManager;
import org.l2jmobius.gameserver.instancemanager.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.instancemanager.IdManager;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.instancemanager.ItemsOnGroundManager;
import org.l2jmobius.gameserver.instancemanager.PunishmentManager;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.instancemanager.RecipeManager;
import org.l2jmobius.gameserver.instancemanager.SiegeManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.AccessLevel;
import org.l2jmobius.gameserver.model.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.model.BlockList;
import org.l2jmobius.gameserver.model.ContactList;
import org.l2jmobius.gameserver.model.Duel;
import org.l2jmobius.gameserver.model.EnchantSkillLearn;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Macro;
import org.l2jmobius.gameserver.model.MacroList;
import org.l2jmobius.gameserver.model.ManufactureItem;
import org.l2jmobius.gameserver.model.Nevit;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.Party.MessageType;
import org.l2jmobius.gameserver.model.PetLevelData;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.PremiumItem;
import org.l2jmobius.gameserver.model.Radar;
import org.l2jmobius.gameserver.model.RecipeList;
import org.l2jmobius.gameserver.model.Request;
import org.l2jmobius.gameserver.model.ShortCuts;
import org.l2jmobius.gameserver.model.Shortcut;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.TeleportBookmark;
import org.l2jmobius.gameserver.model.TerritoryWard;
import org.l2jmobius.gameserver.model.TimeStamp;
import org.l2jmobius.gameserver.model.TradeList;
import org.l2jmobius.gameserver.model.UIKeysSettings;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Decoy;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.Vehicle;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.actor.status.PlayerStatus;
import org.l2jmobius.gameserver.model.actor.tasks.player.DismountTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.FameTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.InventoryEnableTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.LookingForFishTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.PetFeedTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.RecoBonusTaskEnd;
import org.l2jmobius.gameserver.model.actor.tasks.player.RecoGiveTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.RentPetTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.ResetChargesTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.ResetSoulsTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.SitDownTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.StandUpTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.TeleportWatchdogTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.VitalityTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.WarnUserTakeBreakTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.WaterTask;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.model.actor.transform.Transform;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.clan.ClanPrivilege;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.creature.playable.OnPlayableExpChanged;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerFameChanged;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerHennaRemove;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerItemEquip;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerKarmaChanged;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerPKChanged;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerProfessionCancel;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerPvPChanged;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerTransform;
import org.l2jmobius.gameserver.model.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.fishing.Fish;
import org.l2jmobius.gameserver.model.fishing.Fishing;
import org.l2jmobius.gameserver.model.holders.AdditionalSkillHolder;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.holders.MovieHolder;
import org.l2jmobius.gameserver.model.holders.SellBuffHolder;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.holders.SkillUseHolder;
import org.l2jmobius.gameserver.model.holders.SubClassHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.IEventListener;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.model.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerRefund;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.model.items.Armor;
import org.l2jmobius.gameserver.model.items.EtcItem;
import org.l2jmobius.gameserver.model.items.Henna;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.Weapon;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.ActionType;
import org.l2jmobius.gameserver.model.items.type.ArmorType;
import org.l2jmobius.gameserver.model.items.type.EtcItemType;
import org.l2jmobius.gameserver.model.items.type.WeaponType;
import org.l2jmobius.gameserver.model.multisell.PreparedListContainer;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.model.olympiad.OlympiadGameTask;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.partymatching.PartyMatchRoom;
import org.l2jmobius.gameserver.model.partymatching.PartyMatchRoomList;
import org.l2jmobius.gameserver.model.partymatching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestTimer;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.model.skills.AbnormalType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.skills.CommonSkill;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.model.zone.type.WaterZone;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AbstractHtmlPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.CharInfo;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import org.l2jmobius.gameserver.network.serverpackets.ExBrExtraUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExFishingEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExFishingStart;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExGetOnAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMode;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import org.l2jmobius.gameserver.network.serverpackets.ExSetCompassZoneCode;
import org.l2jmobius.gameserver.network.serverpackets.ExStartScenePlayer;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.ExUseSharedGroupItem;
import org.l2jmobius.gameserver.network.serverpackets.ExVoteSystemInfo;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FriendStatusPacket;
import org.l2jmobius.gameserver.network.serverpackets.GetOnVehicle;
import org.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.NicknameChanged;
import org.l2jmobius.gameserver.network.serverpackets.ObservationMode;
import org.l2jmobius.gameserver.network.serverpackets.ObservationReturn;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetInventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreListSell;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListSell;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopSellList;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.Ride;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.ShortCutInit;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;
import org.l2jmobius.gameserver.network.serverpackets.Snoop;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.TargetUnselected;
import org.l2jmobius.gameserver.network.serverpackets.TradeDone;
import org.l2jmobius.gameserver.network.serverpackets.TradeOtherDone;
import org.l2jmobius.gameserver.network.serverpackets.TradeStart;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;
import org.l2jmobius.gameserver.taskmanager.ItemsAutoDestroyTaskManager;
import org.l2jmobius.gameserver.taskmanager.PlayerAutoSaveTaskManager;
import org.l2jmobius.gameserver.taskmanager.PvpFlagTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.EnumIntBitmask;
import org.l2jmobius.gameserver.util.FloodProtectors;
import org.l2jmobius.gameserver.util.Util;

/**
 * This class represents all player characters in the world.<br>
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).
 */
public class PlayerInstance extends Playable
{
	// Character Skill SQL String Definitions:
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String ADD_NEW_SKILLS = "REPLACE INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE charId=? AND class_index=?";
	
	// Character Skill Save SQL String Definitions:
	private static final String ADD_SKILL_SAVE = "REPLACE INTO character_skills_save (charId,skill_id,skill_level,remaining_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,remaining_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE charId=? AND class_index=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";
	
	// Character Item Reuse Time String Definition:
	private static final String ADD_ITEM_REUSE_SAVE = "INSERT INTO character_item_reuse_save (charId,itemId,itemObjId,reuseDelay,systime) VALUES (?,?,?,?,?)";
	private static final String RESTORE_ITEM_REUSE_SAVE = "SELECT charId,itemId,itemObjId,reuseDelay,systime FROM character_item_reuse_save WHERE charId=?";
	private static final String DELETE_ITEM_REUSE_SAVE = "DELETE FROM character_item_reuse_save WHERE charId=?";
	
	// Character Character SQL String Definitions:
	private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,createDate) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,newbie=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,bookmarkslot=?,vitality_points=?,language=?,faction=? WHERE charId=?";
	private static final String RESTORE_CHARACTER = "SELECT * FROM characters WHERE charId=?";
	
	// Character Teleport Bookmark:
	private static final String INSERT_TP_BOOKMARK = "INSERT INTO character_tpbookmark (charId,Id,x,y,z,icon,tag,name) values (?,?,?,?,?,?,?,?)";
	private static final String UPDATE_TP_BOOKMARK = "UPDATE character_tpbookmark SET icon=?,tag=?,name=? where charId=? AND Id=?";
	private static final String RESTORE_TP_BOOKMARK = "SELECT Id,x,y,z,icon,tag,name FROM character_tpbookmark WHERE charId=?";
	private static final String DELETE_TP_BOOKMARK = "DELETE FROM character_tpbookmark WHERE charId=? AND Id=?";
	
	// Character Subclass SQL String Definitions:
	private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC";
	private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
	private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?";
	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";
	
	// Character Henna SQL String Definitions:
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE charId=? AND class_index=?";
	
	// Character Shortcut SQL String Definitions:
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";
	
	// Character Recipe List Save
	private static final String DELETE_CHAR_RECIPE_SHOP = "DELETE FROM character_recipeshoplist WHERE charId=?";
	private static final String INSERT_CHAR_RECIPE_SHOP = "REPLACE INTO character_recipeshoplist (`charId`, `recipeId`, `price`, `index`) VALUES (?, ?, ?, ?)";
	private static final String RESTORE_CHAR_RECIPE_SHOP = "SELECT * FROM character_recipeshoplist WHERE charId=? ORDER BY `index`";
	
	private static final String COND_OVERRIDE_KEY = "cond_override";
	
	public static final String NEWBIE_KEY = "NEWBIE";
	
	public static final int ID_NONE = -1;
	
	public static final int REQUEST_TIMEOUT = 15;
	
	//@formatter:off
	private static final int[][] NEVIT_HOURGLASS_BONUS =
	{
		{25, 50, 50, 50, 50, 50, 50, 50, 50, 50},
		{16, 33, 50, 50, 50, 50, 50, 50, 50, 50},
		{12, 25, 37, 50, 50, 50, 50, 50, 50, 50},
		{10, 20, 30, 40, 50, 50, 50, 50, 50, 50},
		{8, 16, 25, 33, 41, 50, 50, 50, 50, 50},
		{7, 14, 21, 28, 35, 42, 50, 50, 50, 50},
		{6, 12, 18, 25, 31, 37, 43, 50, 50, 50},
		{5, 11, 16, 22, 27, 33, 38, 44, 50, 50},
		{5, 10, 15, 20, 25, 30, 35, 40, 45, 50},
	};
	//@formatter:on
	
	private static final int NEVIT_HOURGLASS_ACTIVE = 0;
	private static final int NEVIT_HOURGLASS_PAUSED = 1;
	private static final int NEVIT_HOURGLASS_MAINTAIN = 10;
	
	private final Collection<IEventListener> _eventListeners = ConcurrentHashMap.newKeySet();
	
	private GameClient _client;
	private String _ip = "N/A";
	
	private final String _accountName;
	private long _deleteTimer;
	private Calendar _createDate = Calendar.getInstance();
	
	private String _lang = null;
	private String _htmlPrefix = "";
	
	private volatile boolean _isOnline = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;
	
	private boolean _subclassLock = false;
	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;
	
	/** data for mounted pets */
	private int _controlItemId;
	private PetLevelData _leveldata;
	private int _curFeed;
	protected Future<?> _mountFeedTask;
	private ScheduledFuture<?> _dismountTask;
	private boolean _petItems = false;
	
	/** The list of sub-classes this character has. */
	private final Map<Integer, SubClassHolder> _subClasses = new ConcurrentHashMap<>();
	
	private final PlayerAppearance _appearance;
	
	/** The Experience of the PlayerInstance before the last Death Penalty */
	private long _expBeforeDeath;
	
	/** The number of player killed during a PvP (the player killed was PvP Flagged) */
	private int _pvpKills;
	
	/** The PK counter of the PlayerInstance (= Number of non PvP Flagged player killed) */
	private int _pkKills;
	
	/** The PvP Flag state of the PlayerInstance (0=White, 1=Purple) */
	private byte _pvpFlag;
	
	/** The Fame of this PlayerInstance */
	private int _fame;
	private ScheduledFuture<?> _fameTask;
	
	/** Vitality recovery task */
	private ScheduledFuture<?> _vitalityTask;
	
	private ScheduledFuture<?> _teleportWatchdog;
	
	/** The Siege state of the PlayerInstance */
	private byte _siegeState = 0;
	
	/** The id of castle/fort which the PlayerInstance is registered for siege */
	private int _siegeSide = 0;
	
	private int _curWeightPenalty = 0;
	
	private int _lastCompassZone; // the last compass zone update send to the client
	
	private boolean _isIn7sDungeon = false;
	
	private final ContactList _contactList = new ContactList(this);
	
	private int _bookmarkSlot = 0; // The Teleport Bookmark Slot
	
	private final Map<Integer, TeleportBookmark> _tpbookmarks = new ConcurrentHashMap<>();
	
	private boolean _canFeed;
	private boolean _isInSiege;
	private boolean _isInHideoutSiege = false;
	
	/** Olympiad */
	private boolean _inOlympiadMode = false;
	private boolean _olympiadStart = false;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;
	/** Olympiad buff count. */
	private int _olyBuffsCount = 0;
	
	/** Duel */
	private boolean _isInDuel = false;
	private boolean _startingDuel = false;
	private int _duelState = Duel.DUELSTATE_NODUEL;
	private int _duelId = 0;
	private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
	
	/** Boat and AirShip */
	private Vehicle _vehicle = null;
	private Location _inVehiclePosition;
	
	public ScheduledFuture<?> _taskForFish;
	private MountType _mountType = MountType.NONE;
	private int _mountNpcId;
	private int _mountLevel;
	/** Store object used to summon the strider you are mounting **/
	private int _mountObjectID = 0;
	
	public int _telemode = 0;
	
	private boolean _inCrystallize;
	private boolean _isCrafting;
	
	private long _offlineShopStart = 0;
	
	private Transform _transformation;
	private final Map<Integer, Skill> _transformSkills = new ConcurrentHashMap<>();
	
	/** The table containing all RecipeList of the PlayerInstance */
	private final Map<Integer, RecipeList> _dwarvenRecipeBook = new ConcurrentHashMap<>();
	private final Map<Integer, RecipeList> _commonRecipeBook = new ConcurrentHashMap<>();
	
	/** Premium Items */
	private final Map<Integer, PremiumItem> _premiumItems = new ConcurrentHashMap<>();
	
	/** True if the PlayerInstance is sitting */
	private boolean _waitTypeSitting;
	
	/** Location before entering Observer Mode */
	private final Location _lastLoc = new Location(0, 0, 0);
	private boolean _observerMode = false;
	
	/** Stored from last ValidatePosition **/
	private final Location _lastServerPosition = new Location(0, 0, 0);
	
	private final AtomicBoolean _blinkActive = new AtomicBoolean();
	
	/** The number of recommendation obtained by the PlayerInstance */
	private int _recomHave; // how much I was recommended by others
	/** The number of recommendation that the PlayerInstance can give */
	private int _recomLeft; // how many recommendations I can give to others
	/** Recommendation Bonus task **/
	private ScheduledFuture<?> _nevitHourglassTask;
	/** Recommendation task **/
	private ScheduledFuture<?> _recoGiveTask;
	/** Recommendation Two Hours bonus **/
	protected boolean _recoTwoHoursGiven = false;
	
	private final PlayerInventory _inventory = new PlayerInventory(this);
	private final PlayerFreight _freight = new PlayerFreight(this);
	private PlayerWarehouse _warehouse;
	private PlayerRefund _refund;
	
	private PrivateStoreType _privateStoreType = PrivateStoreType.NONE;
	
	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private Map<Integer, ManufactureItem> _manufactureItems;
	private String _storeName = "";
	private TradeList _sellList;
	private TradeList _buyList;
	
	// Multisell
	private PreparedListContainer _currentMultiSell = null;
	
	/** Bitmask used to keep track of one-time/newbie quest rewards */
	private int _newbie;
	
	private boolean _noble = false;
	private boolean _hero = false;
	
	/** Premium System */
	private boolean _premiumStatus = false;
	
	/** Faction System */
	private boolean _isGood = false;
	private boolean _isEvil = false;
	
	/** The FolkInstance corresponding to the last Folk which one the player talked. */
	private Npc _lastFolkNpc = null;
	
	/** Last NPC Id talked on a quest */
	private int _questNpcObject = 0;
	
	/** The table containing all Quests began by the PlayerInstance */
	private final Map<String, QuestState> _quests = new ConcurrentHashMap<>();
	
	/** The list containing all shortCuts of this player. */
	private final ShortCuts _shortCuts = new ShortCuts(this);
	
	/** The list containing all macros of this player. */
	private final MacroList _macros = new MacroList(this);
	
	private final Set<PlayerInstance> _snoopListener = ConcurrentHashMap.newKeySet(1);
	private final Set<PlayerInstance> _snoopedPlayer = ConcurrentHashMap.newKeySet(1);
	
	/** Hennas */
	private final Henna[] _henna = new Henna[3];
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;
	
	/** The Summon of the PlayerInstance */
	private Summon _summon = null;
	/** The Decoy of the PlayerInstance */
	private Decoy _decoy = null;
	/** The Trap of the PlayerInstance */
	private TrapInstance _trap = null;
	/** The Agathion of the PlayerInstance */
	private int _agathionId = 0;
	// apparently, a PlayerInstance CAN have both a summon AND a tamed beast at the same time!!
	// after Freya players can control more than one tamed beast
	private Collection<TamedBeastInstance> _tamedBeast = null;
	
	private boolean _minimapAllowed = false;
	
	// client radar
	// TODO: This needs to be better integrated and saved/loaded
	private final Radar _radar;
	
	private ScheduledFuture<?> _taskWarnUserTakeBreak;
	
	// Party matching
	// private int _partymatching = 0;
	private int _partyroom = 0;
	// private int _partywait = 0;
	
	// Clan related attributes
	/** The Clan Identifier of the PlayerInstance */
	private int _clanId;
	
	/** The Clan object of the PlayerInstance */
	private Clan _clan;
	
	/** Apprentice and Sponsor IDs */
	private int _apprentice = 0;
	private int _sponsor = 0;
	
	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;
	
	private int _powerGrade = 0;
	private EnumIntBitmask<ClanPrivilege> _clanPrivileges = new EnumIntBitmask<>(ClanPrivilege.class, false);
	
	/** PlayerInstance's pledge class (knight, Baron, etc.) */
	private int _pledgeClass = 0;
	private int _pledgeType = 0;
	
	/** Level at which the player joined the clan as an academy member */
	private int _lvlJoinedAcademy = 0;
	
	private int _wantsPeace = 0;
	
	// Death Penalty Buff Level
	private int _deathPenaltyBuffLevel = 0;
	
	// charges
	private final AtomicInteger _charges = new AtomicInteger();
	private ScheduledFuture<?> _chargeTask = null;
	
	// Absorbed Souls
	private int _souls = 0;
	private ScheduledFuture<?> _soulTask = null;
	
	// WorldPosition used by TARGET_SIGNET_GROUND
	private Location _currentSkillWorldPosition;
	
	private AccessLevel _accessLevel;
	
	private boolean _messageRefusal = false; // message refusal mode
	
	private boolean _silenceMode = false; // silence mode
	private List<Integer> _silenceModeExcluded; // silence mode
	private boolean _dietMode = false; // ignore weight penalty
	private boolean _tradeRefusal = false; // Trade refusal
	private boolean _exchangeRefusal = false; // Exchange refusal
	
	private Party _party;
	PartyDistributionType _partyDistributionType;
	
	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	private PlayerInstance _activeRequester;
	private long _requestExpireTime = 0;
	private final Request _request = new Request(this);
	private ItemInstance _arrowItem;
	private ItemInstance _boltItem;
	
	// Used for protection after teleport
	private long _spawnProtectEndTime = 0;
	private long _teleportProtectEndTime = 0;
	
	private ItemInstance _lure = null;
	
	// protects a char from aggro mobs when getting up from fake death
	private long _recentFakeDeathEndTime = 0;
	private boolean _isFakeDeath;
	
	/** The fists Weapon of the PlayerInstance (used when no weapon is equipped) */
	private Weapon _fistsWeaponItem;
	
	private final Map<Integer, String> _chars = new LinkedHashMap<>();
	
	// private byte _updateKnownCounter = 0;
	
	private int _expertiseArmorPenalty = 0;
	private int _expertiseWeaponPenalty = 0;
	private int _expertisePenaltyBonus = 0;
	
	private boolean _isEnchanting = false;
	private int _activeEnchantItemId = ID_NONE;
	private int _activeEnchantSupportItemId = ID_NONE;
	private int _activeEnchantAttrItemId = ID_NONE;
	private long _activeEnchantTimestamp = 0;
	
	protected boolean _inventoryDisable = false;
	/** Player's cubics. */
	private final Map<Integer, CubicInstance> _cubics = new ConcurrentSkipListMap<>(); // TODO(Zoey76): This should be sorted in insert order.
	/** Active shots. */
	protected Set<Integer> _activeSoulShots = ConcurrentHashMap.newKeySet(1);
	
	/** Event parameters */
	private boolean _isRegisteredOnEvent = false;
	private boolean _isOnSoloEvent = false;
	private boolean _isOnEvent = false;
	
	private byte _handysBlockCheckerEventArena = -1;
	
	/** new loto ticket **/
	private final int[] _loto = new int[5];
	/** new race ticket **/
	private final int[] _race = new int[2];
	
	private final BlockList _blockList = new BlockList(this);
	
	private Fishing _fishCombat;
	private boolean _fishing = false;
	private int _fishx = 0;
	private int _fishy = 0;
	private int _fishz = 0;
	
	private ScheduledFuture<?> _taskRentPet;
	private ScheduledFuture<?> _taskWater;
	
	/** Last Html Npcs, 0 = last html was not bound to an npc */
	private final int[] _htmlActionOriginObjectIds = new int[HtmlActionScope.values().length];
	/**
	 * Origin of the last incoming html action request.<br>
	 * This can be used for htmls continuing the conversation with an npc.
	 */
	private int _lastHtmlActionOriginObjId;
	
	/** Bypass validations */
	@SuppressWarnings("unchecked")
	private final LinkedList<String>[] _htmlActionCaches = new LinkedList[HtmlActionScope.values().length];
	
	private Forum _forumMail;
	private Forum _forumMemo;
	
	/** Current skill in use. Note that Creature has _lastSkillCast, but this has the button presses */
	private SkillUseHolder _currentSkill;
	private SkillUseHolder _currentPetSkill;
	
	/** Skills queued because a skill is already in progress */
	private SkillUseHolder _queuedSkill;
	
	private int _cursedWeaponEquippedId = 0;
	private boolean _combatFlagEquippedId = false;
	
	private boolean _canRevive = true;
	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private boolean _revivePet = false;
	
	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;
	
	private double _originalCp = .0;
	private double _originalHp = .0;
	private double _originalMp = .0;
	
	/** Char Coords from Client */
	private int _clientX;
	private int _clientY;
	private int _clientZ;
	private int _clientHeading;
	
	// during fall validations will be disabled for 1000 ms.
	private static final int FALLING_VALIDATION_DELAY = 1000;
	private volatile long _fallingTimestamp = 0;
	private volatile int _fallingDamage = 0;
	private Future<?> _fallingDamageTask = null;
	
	private int _multiSocialTarget = 0;
	private int _multiSociaAction = 0;
	
	private MovieHolder _movieHolder = null;
	
	private String _adminConfirmCmd = null;
	
	private volatile long _lastItemAuctionInfoRequest = 0;
	
	private long _pvpFlagLasts;
	
	private long _notMoveUntil = 0;
	
	/** Map containing all custom skills of this player. */
	private Map<Integer, Skill> _customSkills = null;
	
	private volatile int _actionMask;
	
	private Map<Stat, Double> _servitorShare;
	
	/**
	 * Creates a player.
	 * @param objectId the object ID
	 * @param template the player template
	 * @param accountName the account name
	 * @param app the player appearance
	 */
	private PlayerInstance(int objectId, PlayerTemplate template, String accountName, PlayerAppearance app)
	{
		super(objectId, template);
		setInstanceType(InstanceType.PlayerInstance);
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
		
		for (int i = 0; i < _htmlActionCaches.length; ++i)
		{
			_htmlActionCaches[i] = new LinkedList<>();
		}
		
		_accountName = accountName;
		app.setOwner(this);
		_appearance = app;
		
		// Create an AI
		getAI();
		
		// Create a Radar object
		_radar = new Radar(this);
		startVitalityTask();
	}
	
	/**
	 * Creates a player.
	 * @param template the player template
	 * @param accountName the account name
	 * @param app the player appearance
	 */
	private PlayerInstance(PlayerTemplate template, String accountName, PlayerAppearance app)
	{
		this(IdManager.getInstance().getNextId(), template, accountName, app);
	}
	
	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}
	
	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}
	
	public void startPvPFlag()
	{
		updatePvPFlag(1);
		PvpFlagTaskManager.getInstance().add(this);
	}
	
	public void stopPvpRegTask()
	{
		PvpFlagTaskManager.getInstance().remove(this);
	}
	
	public void stopPvPFlag()
	{
		stopPvpRegTask();
		updatePvPFlag(0);
	}
	
	// Character UI
	private UIKeysSettings _uiKeySettings;
	
	// L2JMOD Wedding
	private boolean _married = false;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;
	
	// Item Mall
	private static final String GAME_POINTS_VAR = "PRIME_POINTS"; // Keep compatibility with later clients.
	
	// Save responder name for log it
	private String _lastPetitionGmName = null;
	
	private boolean _hasCharmOfCourage = false;
	
	private final List<QuestTimer> _questTimers = new ArrayList<>();
	
	// Selling buffs system
	private boolean _isSellingBuffs = false;
	private List<SellBuffHolder> _sellingBuffs = null;
	
	public boolean isSellingBuffs()
	{
		return _isSellingBuffs;
	}
	
	public void setSellingBuffs(boolean value)
	{
		_isSellingBuffs = value;
	}
	
	public List<SellBuffHolder> getSellingBuffs()
	{
		if (_sellingBuffs == null)
		{
			_sellingBuffs = new ArrayList<>();
		}
		return _sellingBuffs;
	}
	
	/**
	 * Create a new PlayerInstance and add it in the characters table of the database.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Create a new PlayerInstance with an account name</li>
	 * <li>Set the name, the Hair Style, the Hair Color and the Face type of the PlayerInstance</li>
	 * <li>Add the player in the characters table of the database</li>
	 * </ul>
	 * @param template The PlayerTemplate to apply to the PlayerInstance
	 * @param accountName The name of the PlayerInstance
	 * @param name The name of the PlayerInstance
	 * @param app the player's appearance
	 * @return The PlayerInstance added to the database or null
	 */
	public static PlayerInstance create(PlayerTemplate template, String accountName, String name, PlayerAppearance app)
	{
		// Create a new PlayerInstance with an account name
		final PlayerInstance player = new PlayerInstance(template, accountName, app);
		// Set the name of the PlayerInstance
		player.setName(name);
		// Set Character's create time
		player.setCreateDate(Calendar.getInstance());
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());
		// Kept for backwards compatibility.
		player.setNewbie(1);
		// Give 20 recommendations
		player.setRecomLeft(20);
		// Add the player in the characters table of the database
		return player.createDb() ? player : null;
	}
	
	public String getAccountName()
	{
		return _client == null ? _accountName : _client.getAccountName();
	}
	
	public String getAccountNamePlayer()
	{
		return _accountName;
	}
	
	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}
	
	public int getRelation(PlayerInstance target)
	{
		int result = 0;
		if (_clan != null)
		{
			result |= RelationChanged.RELATION_CLAN_MEMBER;
			if (getClan() == target.getClan())
			{
				result |= RelationChanged.RELATION_CLAN_MATE;
			}
			if (getAllyId() != 0)
			{
				result |= RelationChanged.RELATION_ALLY_MEMBER;
			}
		}
		if (isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}
		if ((getParty() != null) && (getParty() == target.getParty()))
		{
			result |= RelationChanged.RELATION_HAS_PARTY;
			for (int i = 0; i < _party.getMembers().size(); i++)
			{
				if (_party.getMembers().get(i) != this)
				{
					continue;
				}
				switch (i)
				{
					case 0:
					{
						result |= RelationChanged.RELATION_PARTYLEADER; // 0x10
						break;
					}
					case 1:
					{
						result |= RelationChanged.RELATION_PARTY4; // 0x8
						break;
					}
					case 2:
					{
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x7
						break;
					}
					case 3:
					{
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2; // 0x6
						break;
					}
					case 4:
					{
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY1; // 0x5
						break;
					}
					case 5:
					{
						result |= RelationChanged.RELATION_PARTY3; // 0x4
						break;
					}
					case 6:
					{
						result |= RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x3
						break;
					}
					case 7:
					{
						result |= RelationChanged.RELATION_PARTY2; // 0x2
						break;
					}
					case 8:
					{
						result |= RelationChanged.RELATION_PARTY1; // 0x1
						break;
					}
				}
			}
		}
		if (_siegeState != 0)
		{
			if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(this) != 0)
			{
				result |= RelationChanged.RELATION_TERRITORY_WAR;
			}
			else
			{
				result |= RelationChanged.RELATION_INSIEGE;
				if (getSiegeState() != target.getSiegeState())
				{
					result |= RelationChanged.RELATION_ENEMY;
				}
				else
				{
					result |= RelationChanged.RELATION_ALLY;
				}
				if (_siegeState == 1)
				{
					result |= RelationChanged.RELATION_ATTACKER;
				}
			}
		}
		if ((getClan() != null) && (target.getClan() != null))
		{
			if ((target.getPledgeType() != Clan.SUBUNIT_ACADEMY) && (getPledgeType() != Clan.SUBUNIT_ACADEMY) && target.getClan().isAtWarWith(getClan().getId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if (getClan().isAtWarWith(target.getClan().getId()))
				{
					result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
			}
		}
		if (_handysBlockCheckerEventArena != -1)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(getBlockCheckerArena());
			if (holder.getPlayerTeam(this) == 0)
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			result |= RelationChanged.RELATION_ATTACKER;
		}
		return result;
	}
	
	/**
	 * Retrieve a PlayerInstance from the characters table of the database and add it in _allObjects of the L2world (call restore method).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Retrieve the PlayerInstance from the characters table of the database</li>
	 * <li>Add the PlayerInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the PlayerInstance and make it invisible</li>
	 * <li>Update the overloaded status of the PlayerInstance</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @return The PlayerInstance loaded from the database
	 */
	public static PlayerInstance load(int objectId)
	{
		return restore(objectId);
	}
	
	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}
	
	@Override
	public PlayerStat getStat()
	{
		return (PlayerStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PlayerStat(this));
	}
	
	@Override
	public PlayerStatus getStatus()
	{
		return (PlayerStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayerStatus(this));
	}
	
	public PlayerAppearance getAppearance()
	{
		return _appearance;
	}
	
	/**
	 * @return the base PlayerTemplate link to the PlayerInstance.
	 */
	public PlayerTemplate getBaseTemplate()
	{
		return PlayerTemplateData.getInstance().getTemplate(_baseClass);
	}
	
	/**
	 * @return the PlayerTemplate link to the PlayerInstance.
	 */
	@Override
	public PlayerTemplate getTemplate()
	{
		return (PlayerTemplate) super.getTemplate();
	}
	
	/**
	 * @param newclass
	 */
	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(PlayerTemplateData.getInstance().getTemplate(newclass));
	}
	
	@Override
	protected CreatureAI initAI()
	{
		return new PlayerAI(this);
	}
	
	/** Return the Level of the PlayerInstance. */
	@Override
	public int getLevel()
	{
		return getStat().getLevel();
	}
	
	@Override
	public double getLevelMod()
	{
		if (isTransformed())
		{
			final double levelMod = _transformation.getLevelMod(this);
			if (levelMod > -1)
			{
				return levelMod;
			}
		}
		return super.getLevelMod();
	}
	
	/**
	 * @return the _newbie rewards state of the PlayerInstance.
	 */
	public int getNewbie()
	{
		return _newbie;
	}
	
	/**
	 * Set the _newbie rewards state of the PlayerInstance.
	 * @param newbieRewards The Identifier of the _newbie state
	 */
	public void setNewbie(int newbieRewards)
	{
		_newbie = newbieRewards;
	}
	
	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}
	
	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.getId();
	}
	
	public boolean isInStoreMode()
	{
		return _privateStoreType != PrivateStoreType.NONE;
	}
	
	public boolean isCrafting()
	{
		return _isCrafting;
	}
	
	public void setCrafting(boolean isCrafting)
	{
		_isCrafting = isCrafting;
	}
	
	/**
	 * @return a table containing all Common RecipeList of the PlayerInstance.
	 */
	public Collection<RecipeList> getCommonRecipeBook()
	{
		return _commonRecipeBook.values();
	}
	
	/**
	 * @return a table containing all Dwarf RecipeList of the PlayerInstance.
	 */
	public Collection<RecipeList> getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values();
	}
	
	/**
	 * Add a new RecipList to the table _commonrecipebook containing all RecipeList of the PlayerInstance
	 * @param recipe The RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerCommonRecipeList(RecipeList recipe, boolean saveToDb)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId(), false);
		}
	}
	
	/**
	 * Add a new RecipList to the table _recipebook containing all RecipeList of the PlayerInstance
	 * @param recipe The RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerDwarvenRecipeList(RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId(), true);
		}
	}
	
	/**
	 * @param recipeId The Identifier of the RecipeList to check in the player's recipe books
	 * @return {@code true}if player has the recipe on Common or Dwarven Recipe book else returns {@code false}
	 */
	public boolean hasRecipeList(int recipeId)
	{
		return _dwarvenRecipeBook.containsKey(recipeId) || _commonRecipeBook.containsKey(recipeId);
	}
	
	/**
	 * Tries to remove a RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all RecipeList of the PlayerInstance
	 * @param recipeId The Identifier of the RecipeList to remove from the _recipebook
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId, true);
		}
		else if (_commonRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId, false);
		}
		else
		{
			LOGGER.warning("Attempted to remove unknown RecipeList: " + recipeId);
		}
		
		for (Shortcut sc : _shortCuts.getAllShortCuts())
		{
			if ((sc != null) && (sc.getId() == recipeId) && (sc.getType() == ShortcutType.RECIPE))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
	}
	
	private void insertNewRecipeData(int recipeId, boolean isDwarf)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)"))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, recipeId);
			ps.setInt(3, isDwarf ? _classIndex : 0);
			ps.setInt(4, isDwarf ? 1 : 0);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "SQL exception while inserting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
	}
	
	private void deleteRecipeData(int recipeId, boolean isDwarf)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?"))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, recipeId);
			ps.setInt(3, isDwarf ? _classIndex : 0);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "SQL exception while deleting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
	}
	
	/**
	 * @return the Id for the last talked quest NPC.
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}
	
	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}
	
	/**
	 * @param quest The name of the quest
	 * @return the QuestState object corresponding to the quest name.
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}
	
	/**
	 * Add a QuestState to the table _quest containing all quests began by the PlayerInstance.
	 * @param qs The QuestState to add to _quest
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}
	
	/**
	 * Verify if the player has the quest state.
	 * @param quest the quest state to check
	 * @return {@code true} if the player has the quest state, {@code false} otherwise
	 */
	public boolean hasQuestState(String quest)
	{
		return _quests.containsKey(quest);
	}
	
	/**
	 * Remove a QuestState from the table _quest containing all quests began by the PlayerInstance.
	 * @param quest The name of the quest
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}
	
	/**
	 * Gets all the active quests.
	 * @return a list of active quests
	 */
	public List<Quest> getAllActiveQuests()
	{
		final List<Quest> quests = new LinkedList<>();
		for (QuestState qs : _quests.values())
		{
			if ((qs == null) || (qs.getQuest() == null) || (!qs.isStarted() && !Config.DEVELOPER))
			{
				continue;
			}
			
			// Ignore other scripts.
			final int questId = qs.getQuest().getId();
			if ((questId > 19999) || (questId < 1))
			{
				continue;
			}
			quests.add(qs.getQuest());
		}
		return quests;
	}
	
	public void processQuestEvent(String questName, String event)
	{
		final Quest quest = QuestManager.getInstance().getQuest(questName);
		if ((quest == null) || (event == null) || event.isEmpty())
		{
			return;
		}
		
		if (_questNpcObject > 0)
		{
			final WorldObject object = World.getInstance().findObject(getLastQuestNpcObject());
			if (object.isNpc() && isInsideRadius2D(object, Npc.INTERACTION_DISTANCE))
			{
				final Npc npc = (Npc) object;
				quest.notifyEvent(event, npc, this);
			}
		}
	}
	
	/** List of all QuestState instance that needs to be notified of this PlayerInstance's or its pet's death */
	private final Collection<QuestState> _notifyQuestOfDeathList = ConcurrentHashMap.newKeySet();
	
	/**
	 * Add QuestState instance that is to be notified of PlayerInstance's death.
	 * @param qs The QuestState that subscribe to this event
	 */
	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
		{
			return;
		}
		
		if (!_notifyQuestOfDeathList.contains(qs))
		{
			_notifyQuestOfDeathList.add(qs);
		}
	}
	
	/**
	 * Remove QuestState instance that is to be notified of PlayerInstance's death.
	 * @param qs The QuestState that subscribe to this event
	 */
	public void removeNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
		{
			return;
		}
		_notifyQuestOfDeathList.remove(qs);
	}
	
	/**
	 * @return a list of QuestStates which registered for notify of death of this PlayerInstance.
	 */
	public Collection<QuestState> getNotifyQuestOfDeath()
	{
		return _notifyQuestOfDeathList;
	}
	
	public boolean isNotifyQuestOfDeathEmpty()
	{
		return _notifyQuestOfDeathList.isEmpty();
	}
	
	/**
	 * @return a collection containing all ShortCut of the PlayerInstance.
	 */
	public Collection<Shortcut> getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}
	
	/**
	 * @param slot The slot in which the shortCuts is equipped
	 * @param page The page of shortCuts containing the slot
	 * @return the ShortCut of the PlayerInstance corresponding to the position (page-slot).
	 */
	public Shortcut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}
	
	/**
	 * Add a L2shortCut to the PlayerInstance _shortCuts
	 * @param shortcut
	 */
	public void registerShortCut(Shortcut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}
	
	/**
	 * Updates the shortcut bars with the new skill.
	 * @param skillId the skill Id to search and update.
	 * @param skillLevel the skill level to update.
	 */
	public void updateShortCuts(int skillId, int skillLevel)
	{
		_shortCuts.updateShortCuts(skillId, skillLevel);
	}
	
	/**
	 * Delete the ShortCut corresponding to the position (page-slot) from the PlayerInstance _shortCuts.
	 * @param slot
	 * @param page
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}
	
	/**
	 * @param macro the macro to add to this PlayerInstance.
	 */
	public void registerMacro(Macro macro)
	{
		_macros.registerMacro(macro);
	}
	
	/**
	 * @param id the macro Id to delete.
	 */
	public void deleteMacro(int id)
	{
		_macros.deleteMacro(id);
	}
	
	/**
	 * @return all Macro of the PlayerInstance.
	 */
	public MacroList getMacros()
	{
		return _macros;
	}
	
	/**
	 * Set the siege state of the PlayerInstance.
	 * @param siegeState 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	
	/**
	 * Get the siege state of the PlayerInstance.
	 * @return 1 = attacker, 2 = defender, 0 = not involved
	 */
	@Override
	public byte getSiegeState()
	{
		return _siegeState;
	}
	
	/**
	 * Set the siege Side of the PlayerInstance.
	 * @param value
	 */
	public void setSiegeSide(int value)
	{
		_siegeSide = value;
	}
	
	public boolean isRegisteredOnThisSiegeField(int value)
	{
		return (_siegeSide == value) || ((_siegeSide >= 81) && (_siegeSide <= 89));
	}
	
	@Override
	public int getSiegeSide()
	{
		return _siegeSide;
	}
	
	public boolean isSiegeFriend(WorldObject target)
	{
		// If i'm natural or not in siege zone, not friends.
		if ((_siegeState == 0) || !isInsideZone(ZoneId.SIEGE))
		{
			return false;
		}
		
		// If target isn't a player, is self, isn't on same siege or not on same state, not friends.
		final PlayerInstance targetPlayer = target.getActingPlayer();
		if ((targetPlayer == null) || (targetPlayer == this) || (targetPlayer.getSiegeSide() != _siegeSide) || (_siegeState != targetPlayer.getSiegeState()))
		{
			return false;
		}
		
		// Attackers are considered friends only if castle has no owner.
		if (_siegeState == 1)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(_siegeSide);
			if (castle == null)
			{
				return false;
			}
			if (castle.getOwner() == null)
			{
				return true;
			}
			
			return false;
		}
		
		// Both are defenders, friends.
		return true;
	}
	
	/**
	 * Set the PvP Flag of the PlayerInstance.
	 * @param pvpFlag
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
	}
	
	@Override
	public byte getPvpFlag()
	{
		return _pvpFlag;
	}
	
	@Override
	public void updatePvPFlag(int value)
	{
		if (_pvpFlag == value)
		{
			return;
		}
		setPvpFlag(value);
		
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		
		// If this player has a pet update the pets pvp flag as well
		if (hasSummon())
		{
			sendPacket(new RelationChanged(_summon, getRelation(this), false));
		}
		
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, target ->
		{
			target.sendPacket(new RelationChanged(this, getRelation(target), isAutoAttackable(target)));
			if (hasSummon())
			{
				target.sendPacket(new RelationChanged(_summon, getRelation(target), isAutoAttackable(target)));
			}
		});
	}
	
	@Override
	public void revalidateZone(boolean force)
	{
		// Cannot validate if not in a world region (happens during teleport)
		if (getWorldRegion() == null)
		{
			return;
		}
		
		// This function is called too often from movement code.
		if (!force && (calculateDistance3D(_lastZoneValidateLocation) < 100))
		{
			return;
		}
		_lastZoneValidateLocation.setXYZ(this);
		
		ZoneManager.getInstance().getRegion(this).revalidateZones(this);
		
		if (Config.ALLOW_WATER)
		{
			checkWaterState();
		}
		
		if (isInsideZone(ZoneId.ALTERED))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.ALTEREDZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.ALTEREDZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.ALTEREDZONE));
		}
		else if (isInsideZone(ZoneId.SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2));
		}
		else if (isInsideZone(ZoneId.PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE));
		}
		else if (_isIn7sDungeon)
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE));
		}
		else if (isInsideZone(ZoneId.PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE));
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
			{
				return;
			}
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				updatePvPStatus();
			}
			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE));
		}
	}
	
	/**
	 * @return True if the PlayerInstance can Craft Dwarven Recipes.
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(CommonSkill.CREATE_DWARVEN.getId()) >= 1;
	}
	
	public int getDwarvenCraft()
	{
		return getSkillLevel(CommonSkill.CREATE_DWARVEN.getId());
	}
	
	/**
	 * @return True if the PlayerInstance can Craft Dwarven Recipes.
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(CommonSkill.CREATE_COMMON.getId()) >= 1;
	}
	
	public int getCommonCraft()
	{
		return getSkillLevel(CommonSkill.CREATE_COMMON.getId());
	}
	
	/**
	 * @return the PK counter of the PlayerInstance.
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Set the PK counter of the PlayerInstance.
	 * @param pkKills
	 */
	public void setPkKills(int pkKills)
	{
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPKChanged(this, _pkKills, pkKills), this);
		_pkKills = pkKills;
	}
	
	/**
	 * @return the _deleteTimer of the PlayerInstance.
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Set the _deleteTimer of the PlayerInstance.
	 * @param deleteTimer
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * @return the number of recommendation obtained by the PlayerInstance.
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}
	
	/**
	 * Increment the number of recommendation obtained by the PlayerInstance (Max : 255).
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
		{
			_recomHave++;
		}
	}
	
	/**
	 * Set the number of recommendation obtained by the PlayerInstance (Max : 255).
	 * @param value
	 */
	public void setRecomHave(int value)
	{
		_recomHave = Math.min(Math.max(value, 0), 255);
	}
	
	/**
	 * Set the number of recommendation obtained by the PlayerInstance (Max : 255).
	 * @param value
	 */
	public void setRecomLeft(int value)
	{
		_recomLeft = Math.min(Math.max(value, 0), 255);
	}
	
	/**
	 * @return the number of recommendation that the PlayerInstance can give.
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}
	
	/**
	 * Increment the number of recommendation that the PlayerInstance can give.
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
		{
			_recomLeft--;
		}
	}
	
	public void giveRecom(PlayerInstance target)
	{
		target.incRecomHave();
		decRecomLeft();
	}
	
	/**
	 * Set the exp of the PlayerInstance before a death
	 * @param exp
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}
	
	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}
	
	/**
	 * Set the Karma of the PlayerInstance and send a Server->Client packet StatusUpdate (broadcast).
	 * @param value
	 */
	@Override
	public void setKarma(int value)
	{
		// Notify to scripts.
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerKarmaChanged(this, getKarma(), value), this);
		
		int karma = value;
		if (karma < 0)
		{
			karma = 0;
		}
		if ((getKarma() == 0) && (karma > 0))
		{
			World.getInstance().forEachVisibleObject(this, GuardInstance.class, object ->
			{
				if (object.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					object.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			});
		}
		else if ((getKarma() > 0) && (karma == 0))
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the PlayerInstance and all PlayerInstance to inform (broadcast)
			setKarmaFlag();
		}
		
		super.setKarma(karma);
		broadcastKarma();
	}
	
	public int getExpertiseArmorPenalty()
	{
		return _expertiseArmorPenalty;
	}
	
	public int getExpertiseWeaponPenalty()
	{
		return _expertiseWeaponPenalty;
	}
	
	public int getExpertisePenaltyBonus()
	{
		return _expertisePenaltyBonus;
	}
	
	public void setExpertisePenaltyBonus(int bonus)
	{
		_expertisePenaltyBonus = bonus;
	}
	
	public int getWeightPenalty()
	{
		return _dietMode ? 0 : _curWeightPenalty;
	}
	
	/**
	 * Update the overloaded status of the PlayerInstance.
	 */
	public void refreshOverloaded()
	{
		final int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			final long weightproc = ((getCurrentLoad() - getBonusWeightPenalty()) * 1000) / getMaxLoad();
			int newWeightPenalty;
			if ((weightproc < 500) || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if ((newWeightPenalty > 0) && !_dietMode)
				{
					addSkill(SkillData.getInstance().getSkill(4270, newWeightPenalty));
					setOverloaded(getCurrentLoad() > maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), false, true);
					setOverloaded(false);
				}
				broadcastUserInfo();
				sendPacket(new EtcStatusUpdate(this));
			}
		}
	}
	
	public void refreshExpertisePenalty()
	{
		if (!Config.EXPERTISE_PENALTY)
		{
			return;
		}
		
		final int expertiseLevel = getExpertiseLevel();
		int armorPenalty = 0;
		int weaponPenalty = 0;
		int crystaltype;
		for (ItemInstance item : _inventory.getItems())
		{
			if ((item != null) && item.isEquipped() && (item.getItemType() != EtcItemType.ARROW) && (item.getItemType() != EtcItemType.BOLT))
			{
				crystaltype = item.getItem().getCrystalType().getLevel();
				if (crystaltype > expertiseLevel)
				{
					if (item.isWeapon() && (crystaltype > weaponPenalty))
					{
						weaponPenalty = crystaltype;
					}
					else if (crystaltype > armorPenalty)
					{
						armorPenalty = crystaltype;
					}
				}
			}
		}
		
		boolean changed = false;
		
		// calc weapon penalty
		weaponPenalty = weaponPenalty - expertiseLevel - _expertisePenaltyBonus;
		weaponPenalty = Math.min(Math.max(weaponPenalty, 0), 4);
		if ((_expertiseWeaponPenalty != weaponPenalty) || (getSkillLevel(CommonSkill.WEAPON_GRADE_PENALTY.getId()) != weaponPenalty))
		{
			_expertiseWeaponPenalty = weaponPenalty;
			if (_expertiseWeaponPenalty > 0)
			{
				addSkill(SkillData.getInstance().getSkill(CommonSkill.WEAPON_GRADE_PENALTY.getId(), _expertiseWeaponPenalty));
			}
			else
			{
				removeSkill(getKnownSkill(CommonSkill.WEAPON_GRADE_PENALTY.getId()), false, true);
			}
			changed = true;
		}
		
		// calc armor penalty
		armorPenalty = armorPenalty - expertiseLevel - _expertisePenaltyBonus;
		armorPenalty = Math.min(Math.max(armorPenalty, 0), 4);
		if ((_expertiseArmorPenalty != armorPenalty) || (getSkillLevel(CommonSkill.ARMOR_GRADE_PENALTY.getId()) != armorPenalty))
		{
			_expertiseArmorPenalty = armorPenalty;
			if (_expertiseArmorPenalty > 0)
			{
				addSkill(SkillData.getInstance().getSkill(CommonSkill.ARMOR_GRADE_PENALTY.getId(), _expertiseArmorPenalty));
			}
			else
			{
				removeSkill(getKnownSkill(CommonSkill.ARMOR_GRADE_PENALTY.getId()), false, true);
			}
			changed = true;
		}
		
		if (changed)
		{
			sendPacket(new EtcStatusUpdate(this));
		}
	}
	
	public void useEquippableItem(ItemInstance item, boolean abortAttack)
	{
		// Check if the item is null.
		if (item == null)
		{
			return;
		}
		
		// Check if the item is owned by this player.
		if (item.getOwnerId() != getObjectId())
		{
			return;
		}
		
		// Check if the item is in the inventory.
		final ItemLocation itemLocation = item.getItemLocation();
		if ((itemLocation != ItemLocation.INVENTORY) && (itemLocation != ItemLocation.PAPERDOLL))
		{
			return;
		}
		
		// Equip or unEquip
		Collection<ItemInstance> items = null;
		final boolean isEquiped = item.isEquipped();
		final int oldInvLimit = getInventoryLimit();
		SystemMessage sm = null;
		if (isEquiped)
		{
			if (item.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(item.getEnchantLevel());
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
			}
			sm.addItemName(item);
			sendPacket(sm);
			
			final int slot = _inventory.getSlotFromItem(item);
			// we can't unequip talisman by body slot
			if (slot == Item.SLOT_DECO)
			{
				items = _inventory.unEquipItemInSlotAndRecord(item.getLocationSlot());
			}
			else
			{
				items = _inventory.unEquipItemInBodySlotAndRecord(slot);
			}
		}
		else
		{
			items = _inventory.equipItemAndRecord(item);
			if (item.isEquipped())
			{
				if (item.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPPED_S1_S2);
					sm.addInt(item.getEnchantLevel());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_EQUIPPED_YOUR_S1);
				}
				sm.addItemName(item);
				sendPacket(sm);
				
				// Consume mana - will start a task if required; returns if item is not a shadow item
				item.decreaseMana(false);
				
				if ((item.getItem().getBodyPart() & Item.SLOT_MULTI_ALLWEAPON) != 0)
				{
					rechargeShots(true, true);
				}
				
				// Notify to scripts
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemEquip(this, item), item.getItem());
				
			}
			else
			{
				sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
			}
		}
		
		refreshExpertisePenalty();
		broadcastUserInfo();
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(items);
		sendPacket(iu);
		
		if (abortAttack)
		{
			abortAttack();
		}
		
		if (getInventoryLimit() != oldInvLimit)
		{
			sendPacket(new ExStorageMaxCount(this));
		}
	}
	
	/**
	 * @return the the PvP Kills of the PlayerInstance (Number of player killed during a PvP).
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Set the the PvP Kills of the PlayerInstance (Number of player killed during a PvP).
	 * @param pvpKills
	 */
	public void setPvpKills(int pvpKills)
	{
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPChanged(this, _pvpKills, pvpKills), this);
		_pvpKills = pvpKills;
	}
	
	/**
	 * @return the Fame of this PlayerInstance
	 */
	public int getFame()
	{
		return _fame;
	}
	
	/**
	 * Set the Fame of this PlayerInstane
	 * @param fame
	 */
	public void setFame(int fame)
	{
		int newFame = fame;
		if (fame > Config.MAX_PERSONAL_FAME_POINTS)
		{
			newFame = Config.MAX_PERSONAL_FAME_POINTS;
		}
		else if (fame < 0)
		{
			newFame = 0;
		}
		
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerFameChanged(this, _fame, newFame), this);
		
		_fame = newFame;
	}
	
	/**
	 * @return the ClassId object of the PlayerInstance contained in PlayerTemplate.
	 */
	public ClassId getClassId()
	{
		return getTemplate().getClassId();
	}
	
	/**
	 * Set the template of the PlayerInstance.
	 * @param id The Identifier of the PlayerTemplate to set to the PlayerInstance
	 */
	public void setClassId(int id)
	{
		if (_subclassLock)
		{
			return;
		}
		_subclassLock = true;
		
		try
		{
			if ((_lvlJoinedAcademy != 0) && (_clan != null) && CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, id))
			{
				if (_lvlJoinedAcademy <= 16)
				{
					_clan.addReputationScore(Config.JOIN_ACADEMY_MAX_REP_SCORE, true);
				}
				else if (_lvlJoinedAcademy >= 39)
				{
					_clan.addReputationScore(Config.JOIN_ACADEMY_MIN_REP_SCORE, true);
				}
				else
				{
					_clan.addReputationScore(Config.JOIN_ACADEMY_MAX_REP_SCORE - ((_lvlJoinedAcademy - 16) * 20), true);
				}
				setLvlJoinedAcademy(0);
				// oust pledge member from the academy, cuz he has finished his 2nd class transfer
				final SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_HAS_BEEN_EXPELLED);
				msg.addPcName(this);
				_clan.broadcastToOnlineMembers(msg);
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
				_clan.removeClanMember(getObjectId(), 0);
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_WILL_NOW_GRADUATE_FROM_THE_CLAN_ACADEMY_AND_LEAVE_YOUR_CURRENT_CLAN_AS_A_GRADUATE_OF_THE_ACADEMY_YOU_CAN_IMMEDIATELY_JOIN_A_CLAN_AS_A_REGULAR_MEMBER_WITHOUT_BEING_SUBJECT_TO_ANY_PENALTIES);
				
				// receive graduation gift
				_inventory.addItem("Gift", 8181, 1, this, null); // give academy circlet
			}
			if (isSubClassActive())
			{
				getSubClasses().get(_classIndex).setClassId(id);
			}
			setTarget(this);
			broadcastPacket(new MagicSkillUse(this, 5103, 1, 1000, 0));
			setClassTemplate(id);
			if (getClassId().level() == 3)
			{
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_YOUR_THIRD_CLASS_TRANSFER_QUEST);
			}
			else
			{
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_A_CLASS_TRANSFER);
			}
			
			// Remove class permitted hennas.
			for (int slot = 1; slot < 4; slot++)
			{
				final Henna henna = getHenna(slot);
				if ((henna != null) && !henna.isAllowedClass(getClassId()))
				{
					removeHenna(slot);
				}
			}
			
			// Update class icon in party and clan
			if (isInParty())
			{
				_party.broadcastPacket(new PartySmallWindowUpdate(this));
			}
			
			if (_clan != null)
			{
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
			}
			
			// Add AutoGet skills and normal skills and/or learnByFS depending on configurations.
			rewardSkills();
			
			if (!canOverrideCond(PlayerCondOverride.SKILL_CONDITIONS) && Config.DECREASE_SKILL_LEVEL)
			{
				checkPlayerSkills();
			}
		}
		finally
		{
			_subclassLock = false;
		}
	}
	
	public boolean isChangingClass()
	{
		return _subclassLock;
	}
	
	/**
	 * Used for AltGameSkillLearn to set a custom skill learning class Id.
	 */
	private ClassId _learningClass = getClassId();
	
	/**
	 * @return the custom skill learning class Id.
	 */
	public ClassId getLearningClass()
	{
		return _learningClass;
	}
	
	/**
	 * @param learningClass the custom skill learning class Id to set.
	 */
	public void setLearningClass(ClassId learningClass)
	{
		_learningClass = learningClass;
	}
	
	/**
	 * @return the Experience of the PlayerInstance.
	 */
	public long getExp()
	{
		return getStat().getExp();
	}
	
	public void setActiveEnchantAttrItemId(int objectId)
	{
		_activeEnchantAttrItemId = objectId;
	}
	
	public int getActiveEnchantAttrItemId()
	{
		return _activeEnchantAttrItemId;
	}
	
	public void setActiveEnchantItemId(int objectId)
	{
		// If we don't have a Enchant Item, we are not enchanting.
		if (objectId == ID_NONE)
		{
			setActiveEnchantSupportItemId(ID_NONE);
			setActiveEnchantTimestamp(0);
			setEnchanting(false);
		}
		_activeEnchantItemId = objectId;
	}
	
	public int getActiveEnchantItemId()
	{
		return _activeEnchantItemId;
	}
	
	public void setActiveEnchantSupportItemId(int objectId)
	{
		_activeEnchantSupportItemId = objectId;
	}
	
	public int getActiveEnchantSupportItemId()
	{
		return _activeEnchantSupportItemId;
	}
	
	public long getActiveEnchantTimestamp()
	{
		return _activeEnchantTimestamp;
	}
	
	public void setActiveEnchantTimestamp(long value)
	{
		_activeEnchantTimestamp = value;
	}
	
	public void setEnchanting(boolean value)
	{
		_isEnchanting = value;
	}
	
	public boolean isEnchanting()
	{
		return _isEnchanting;
	}
	
	/**
	 * Set the fists weapon of the PlayerInstance (used when no weapon is equiped).
	 * @param weaponItem The fists Weapon to set to the PlayerInstance
	 */
	public void setFistsWeaponItem(Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}
	
	/**
	 * @return the fists weapon of the PlayerInstance (used when no weapon is equipped).
	 */
	public Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}
	
	/**
	 * @param classId
	 * @return the fists weapon of the PlayerInstance Class (used when no weapon is equipped).
	 */
	public Weapon findFistsWeaponItem(int classId)
	{
		Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			// human fighter fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(246);
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			// human mage fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(251);
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			// elven fighter fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(244);
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			// elven mage fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(249);
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			// dark elven fighter fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(245);
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			// dark elven mage fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(250);
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			// orc fighter fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(248);
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			// orc mage fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(252);
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			// dwarven fists
			weaponItem = (Weapon) ItemTable.getInstance().getTemplate(247);
		}
		return weaponItem;
	}
	
	/**
	 * This method reward all AutoGet skills and Normal skills if Auto-Learn configuration is true.
	 */
	public void rewardSkills()
	{
		// Give all normal skills if activated Auto-Learn is activated, included AutoGet skills.
		if (Config.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true, Config.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
		}
		else
		{
			giveAvailableAutoGetSkills();
		}
		
		if (Config.DECREASE_SKILL_LEVEL && !canOverrideCond(PlayerCondOverride.SKILL_CONDITIONS))
		{
			checkPlayerSkills();
		}
		
		checkItemRestriction();
		sendSkillList();
	}
	
	/**
	 * Re-give all skills which aren't saved to database, like Noble, Hero, Clan Skills.
	 */
	public void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load
		
		// Add noble skills if noble
		if (_noble)
		{
			setNoble(true);
		}
		
		// Add Hero skills if hero
		if (_hero)
		{
			setHero(true);
		}
		
		// Add clan skills
		if (_clan != null)
		{
			_clan.addSkillEffects(this);
			
			if ((_clan.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel()) && isClanLeader())
			{
				SiegeManager.getInstance().addSiegeSkills(this);
			}
			if (_clan.getCastleId() > 0)
			{
				CastleManager.getInstance().getCastleByOwner(getClan()).giveResidentialSkills(this);
			}
			if (_clan.getFortId() > 0)
			{
				FortManager.getInstance().getFortByOwner(getClan()).giveResidentialSkills(this);
			}
		}
		
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
		// Add Death Penalty Buff Level
		restoreDeathPenaltyBuffLevel();
	}
	
	/**
	 * Give all available skills to the player.
	 * @param includeByFs if {@code true} forgotten scroll skills present in the skill tree will be added
	 * @param includeAutoGet if {@code true} auto-get skills present in the skill tree will be added
	 * @param includeRequiredItems if {@code true} skills that have required items will be added
	 * @return the amount of new skills earned
	 */
	public int giveAvailableSkills(boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems)
	{
		int skillCounter = 0;
		// Get available skills.
		final Collection<Skill> skills = SkillTreeData.getInstance().getAllAvailableSkills(this, getClassId(), includeByFs, includeAutoGet, includeRequiredItems);
		final List<Skill> skillsForStore = new ArrayList<>();
		for (Skill sk : skills)
		{
			if (getKnownSkill(sk.getId()) == sk)
			{
				continue;
			}
			
			if (getSkillLevel(sk.getId()) == 0)
			{
				skillCounter++;
			}
			
			// Fix when learning toggle skills.
			if (sk.isToggle() && isAffectedBySkill(sk.getId()))
			{
				stopSkillEffects(SkillFinishType.REMOVED, sk.getId());
			}
			
			addSkill(sk, false);
			skillsForStore.add(sk);
		}
		storeSkills(skillsForStore, -1);
		if (Config.AUTO_LEARN_SKILLS && (skillCounter > 0))
		{
			sendMessage("You have learned " + skillCounter + " new skills.");
		}
		return skillCounter;
	}
	
	/**
	 * Give all available auto-get skills to the player.
	 */
	public void giveAvailableAutoGetSkills()
	{
		// Get available skills.
		final List<SkillLearn> autoGetSkills = SkillTreeData.getInstance().getAvailableAutoGetSkills(this);
		final SkillData st = SkillData.getInstance();
		Skill skill;
		for (SkillLearn s : autoGetSkills)
		{
			skill = st.getSkill(s.getSkillId(), s.getSkillLevel());
			if (skill != null)
			{
				addSkill(skill, true);
			}
			else
			{
				LOGGER.warning("Skipping null auto-get skill for player: " + toString());
			}
		}
	}
	
	/**
	 * Set the Experience value of the PlayerInstance.
	 * @param exp
	 */
	public void setExp(long exp)
	{
		getStat().setExp(Math.max(0, exp));
	}
	
	/**
	 * @return the Race object of the PlayerInstance.
	 */
	@Override
	public Race getRace()
	{
		if (!isSubClassActive())
		{
			return getTemplate().getRace();
		}
		return PlayerTemplateData.getInstance().getTemplate(_baseClass).getRace();
	}
	
	public Radar getRadar()
	{
		return _radar;
	}
	
	/* Return true if Hellbound minimap allowed */
	public boolean isMinimapAllowed()
	{
		return _minimapAllowed;
	}
	
	/* Enable or disable minimap on Hellbound */
	public void setMinimapAllowed(boolean value)
	{
		_minimapAllowed = value;
	}
	
	/**
	 * @return the SP amount of the PlayerInstance.
	 */
	public long getSp()
	{
		return getStat().getSp();
	}
	
	/**
	 * Set the SP amount of the PlayerInstance.
	 * @param sp
	 */
	public void setSp(long sp)
	{
		super.getStat().setSp(Math.max(0, sp));
	}
	
	/**
	 * @param castleId
	 * @return true if this PlayerInstance is a clan leader in ownership of the passed castle
	 */
	public boolean isCastleLord(int castleId)
	{
		// player has clan and is the clan leader, check the castle info
		if ((_clan != null) && (_clan.getLeader().getPlayerInstance() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			final Castle castle = CastleManager.getInstance().getCastleByOwner(_clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return the Clan Identifier of the PlayerInstance.
	 */
	@Override
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * @return the Clan Crest Identifier of the PlayerInstance or 0.
	 */
	public int getClanCrestId()
	{
		return _clan != null ? _clan.getCrestId() : 0;
	}
	
	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if ((_clan != null) && ((_clan.getCastleId() != 0) || (_clan.getHideoutId() != 0)))
		{
			return _clan.getCrestLargeId();
		}
		return 0;
	}
	
	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}
	
	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}
	
	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}
	
	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}
	
	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = Chronos.currentTimeMillis();
	}
	
	/**
	 * Return the PcInventory Inventory of the PlayerInstance contained in _inventory.
	 */
	@Override
	public PlayerInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Delete a ShortCut of the PlayerInstance _shortCuts.
	 * @param objectId
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}
	
	/**
	 * @return True if the PlayerInstance is sitting.
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}
	
	/**
	 * Set _waitTypeSitting to given value
	 * @param value
	 */
	public void setSitting(boolean value)
	{
		_waitTypeSitting = value;
	}
	
	/**
	 * Sit down the PlayerInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)
	 */
	public void sitDown()
	{
		sitDown(true);
	}
	
	public void sitDown(boolean checkCast)
	{
		if (checkCast && isCastingNow())
		{
			sendMessage("Cannot sit while casting");
			return;
		}
		
		if (_waitTypeSitting || isAttackDisabled() || isOutOfControl() || isImmobilized())
		{
			return;
		}
		
		breakAttack();
		setSitting(true);
		getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
		// Schedule a sit down task to wait for the animation to finish
		ThreadPool.schedule(new SitDownTask(this), 2500);
		setParalyzed(true);
	}
	
	/**
	 * Stand up the PlayerInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)
	 */
	public void standUp()
	{
		if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (getEffectList().isAffected(EffectFlag.RELAXING))
			{
				stopEffects(EffectType.RELAXING);
			}
			
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			ThreadPool.schedule(new StandUpTask(this), 2500);
		}
	}
	
	/**
	 * @return the PcWarehouse object of the PlayerInstance.
	 */
	public PlayerWarehouse getWarehouse()
	{
		if (_warehouse == null)
		{
			_warehouse = new PlayerWarehouse(this);
			_warehouse.restore();
		}
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().addCacheTask(this);
		}
		return _warehouse;
	}
	
	/**
	 * Free memory used by Warehouse
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
		{
			_warehouse.deleteMe();
		}
		_warehouse = null;
	}
	
	/**
	 * @return the PcFreight object of the PlayerInstance.
	 */
	public PlayerFreight getFreight()
	{
		return _freight;
	}
	
	/**
	 * @return true if refund list is not empty
	 */
	public boolean hasRefund()
	{
		return (_refund != null) && (_refund.getSize() > 0) && Config.ALLOW_REFUND;
	}
	
	/**
	 * @return refund object or create new if not exist
	 */
	public PlayerRefund getRefund()
	{
		if (_refund == null)
		{
			_refund = new PlayerRefund(this);
		}
		return _refund;
	}
	
	/**
	 * Clear refund
	 */
	public void clearRefund()
	{
		if (_refund != null)
		{
			_refund.deleteMe();
		}
		_refund = null;
	}
	
	/**
	 * @return the Adena amount of the PlayerInstance.
	 */
	public long getAdena()
	{
		return _inventory.getAdena();
	}
	
	/**
	 * @return the Ancient Adena amount of the PlayerInstance.
	 */
	public long getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}
	
	/**
	 * Add adena to Inventory of the PlayerInstance and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, long count, WorldObject reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_ADENA);
			sm.addLong(count);
			sendPacket(sm);
		}
		
		if (count <= 0)
		{
			return;
		}
		
		_inventory.addAdena(process, count, this, reference);
		// Send update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_inventory.getAdenaInstance());
			sendPacket(iu);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
	}
	
	/**
	 * Reduce adena in Inventory of the PlayerInstance and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : long Quantity of adena to be reduced
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean reduceAdena(String process, long count, WorldObject reference, boolean sendMessage)
	{
		if (count > _inventory.getAdena())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			}
			return false;
		}
		
		if (count > 0)
		{
			final ItemInstance adenaItem = _inventory.getAdenaInstance();
			if (!_inventory.reduceAdena(process, count, this, reference))
			{
				return false;
			}
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_ADENA_DISAPPEARED);
				sm.addLong(count);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Add ancient adena to Inventory of the PlayerInstance and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, long count, WorldObject reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
			sm.addItemName(Inventory.ANCIENT_ADENA_ID);
			sm.addLong(count);
			sendPacket(sm);
		}
		
		if (count <= 0)
		{
			return;
		}
		
		_inventory.addAncientAdena(process, count, this, reference);
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_inventory.getAncientAdenaInstance());
			sendPacket(iu);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
	}
	
	/**
	 * Reduce ancient adena in Inventory of the PlayerInstance and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : long Quantity of ancient adena to be reduced
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean reduceAncientAdena(String process, long count, WorldObject reference, boolean sendMessage)
	{
		if (count > _inventory.getAncientAdena())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			}
			return false;
		}
		
		if (count > 0)
		{
			final ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			if (!_inventory.reduceAncientAdena(process, count, this, reference))
			{
				return false;
			}
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(ancientAdenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				final SystemMessage sm;
				if (count > 1)
				{
					sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
					sm.addItemName(Inventory.ANCIENT_ADENA_ID);
					sm.addLong(count);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
					sm.addItemName(Inventory.ANCIENT_ADENA_ID);
				}
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, ItemInstance item, WorldObject reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				final SystemMessage sm;
				if (item.getCount() > 1)
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S2_S1);
					sm.addItemName(item);
					sm.addLong(item.getCount());
				}
				else if (item.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_A_S1_S2);
					sm.addInt(item.getEnchantLevel());
					sm.addItemName(item);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
					sm.addItemName(item);
				}
				sendPacket(sm);
			}
			
			// Add the item to inventory
			final ItemInstance newitem = _inventory.addItem(process, item, this, reference);
			
			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				sendPacket(playerIU);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			// Update current load as well
			final StatusUpdate su = new StatusUpdate(this);
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);
			
			// If over capacity, drop the item
			if (!canOverrideCond(PlayerCondOverride.ITEM_CONDITIONS) && !_inventory.validateCapacity(0, item.isQuestItem()) && newitem.isDropable() && (!newitem.isStackable() || (newitem.getLastChange() != ItemInstance.MODIFIED)))
			{
				dropItem("InvDrop", newitem, null, true, true);
			}
			else if (CursedWeaponsManager.getInstance().isCursed(newitem.getId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
			
			// Combat Flag
			else if (FortSiegeManager.getInstance().isCombat(item.getId()))
			{
				if (FortSiegeManager.getInstance().activateCombatFlag(this, item))
				{
					final Fort fort = FortManager.getInstance().getFort(this);
					fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_HAS_ACQUIRED_THE_FLAG), getName());
				}
			}
			// Territory Ward
			else if ((item.getId() >= 13560) && (item.getId() <= 13568))
			{
				final TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(item.getId() - 13479);
				if (ward != null)
				{
					ward.activate(this, item);
				}
			}
		}
	}
	
	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : long Quantity of items to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return
	 */
	public ItemInstance addItem(String process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		if (count > 0)
		{
			final Item item = ItemTable.getInstance().getTemplate(itemId);
			if (item == null)
			{
				LOGGER.log(Level.SEVERE, "Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && item.hasExImmediateEffect()) || !item.hasExImmediateEffect()))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("Sweeper") || process.equalsIgnoreCase("Quest"))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addLong(count);
						sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S2_S1);
						sm.addItemName(itemId);
						sm.addLong(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("Sweeper") || process.equalsIgnoreCase("Quest"))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			
			// Auto-use herbs.
			if (item.hasExImmediateEffect() && item.isEtcItem())
			{
				for (SkillHolder skillHolder : item.getSkills())
				{
					doSimultaneousCast(skillHolder.getSkill());
				}
				broadcastInfo();
			}
			else
			{
				// Add the item to inventory
				final ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);
				
				// If over capacity, drop the item
				if (!canOverrideCond(PlayerCondOverride.ITEM_CONDITIONS) && !_inventory.validateCapacity(0, item.isQuestItem()) && createdItem.isDropable() && (!createdItem.isStackable() || (createdItem.getLastChange() != ItemInstance.MODIFIED)))
				{
					dropItem("InvDrop", createdItem, null, true);
				}
				else if (CursedWeaponsManager.getInstance().isCursed(createdItem.getId()))
				{
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				}
				// Territory Ward
				else if ((createdItem.getId() >= 13560) && (createdItem.getId() <= 13568))
				{
					final TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(createdItem.getId() - 13479);
					if (ward != null)
					{
						ward.activate(this, createdItem);
					}
				}
				return createdItem;
			}
		}
		return null;
	}
	
	/**
	 * @param process the process name
	 * @param item the item holder
	 * @param reference the reference object
	 * @param sendMessage if {@code true} a system message will be sent
	 */
	public void addItem(String process, ItemHolder item, WorldObject reference, boolean sendMessage)
	{
		addItem(process, item.getId(), item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItem(String process, ItemInstance item, WorldObject reference, boolean sendMessage)
	{
		return destroyItem(process, item, item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be destroyed
	 * @param count
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItem(String process, ItemInstance item, long count, WorldObject reference, boolean sendMessage)
	{
		final ItemInstance destoyedItem = _inventory.destroyItem(process, item, count, this, reference);
		if (destoyedItem == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(destoyedItem);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm;
			if (count > 1)
			{
				sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
				sm.addItemName(destoyedItem);
				sm.addLong(count);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
				sm.addItemName(destoyedItem);
			}
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	@Override
	public boolean destroyItem(String process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		final ItemInstance item = _inventory.getItemByObjectId(objectId);
		if (item != null)
		{
			return destroyItem(process, item, count, reference, sendMessage);
		}
		if (sendMessage)
		{
			sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
		}
		return false;
	}
	
	/**
	 * Destroys shots from inventory without logging and only occasional saving to database. Sends a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		final ItemInstance item = _inventory.getItemByObjectId(objectId);
		if ((item != null) && (item.getCount() >= count))
		{
			return destroyItem(null, item, count, reference, sendMessage);
		}
		if (sendMessage)
		{
			sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
		}
		return false;
	}
	
	/**
	 * Destroy item from inventory by using its <b>itemId</b> and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		if (itemId == Inventory.ADENA_ID)
		{
			return reduceAdena(process, count, reference, sendMessage);
		}
		
		final ItemInstance item = _inventory.getItemByItemId(itemId);
		if ((item == null) || (item.getCount() < count) || (_inventory.destroyItemByItemId(process, itemId, count, this, reference) == null))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm;
			if (count > 1)
			{
				sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
				sm.addItemName(itemId);
				sm.addLong(count);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
				sm.addItemName(itemId);
			}
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Identifier of the item to be transfered
	 * @param count : long Quantity of items to be transfered
	 * @param target
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public ItemInstance transferItem(String process, int objectId, long count, Inventory target, WorldObject reference)
	{
		final ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
		{
			return null;
		}
		final ItemInstance newItem = _inventory.transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(this);
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);
		
		// Send target update packet
		if (target instanceof PlayerInventory)
		{
			final PlayerInstance targetPlayer = ((PlayerInventory) target).getOwner();
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate playerIU = new InventoryUpdate();
				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}
				targetPlayer.sendPacket(playerIU);
			}
			else
			{
				targetPlayer.sendPacket(new ItemList(targetPlayer, false));
			}
			
			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer);
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			final PetInventoryUpdate petIU = new PetInventoryUpdate();
			if (newItem.getCount() > count)
			{
				petIU.addModifiedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			((PetInventory) target).getOwner().sendPacket(petIU);
		}
		return newItem;
	}
	
	/**
	 * Use instead of calling {@link #addItem(String, ItemInstance, WorldObject, boolean)} and {@link #destroyItemByItemId(String, int, long, WorldObject, boolean)}<br>
	 * This method validates slots and weight limit, for stackable and non-stackable items.
	 * @param process a generic string representing the process that is exchanging this items
	 * @param reference the (probably NPC) reference, could be null
	 * @param coinId the item Id of the item given on the exchange
	 * @param cost the amount of items given on the exchange
	 * @param rewardId the item received on the exchange
	 * @param count the amount of items received on the exchange
	 * @param sendMessage if {@code true} it will send messages to the acting player
	 * @return {@code true} if the player successfully exchanged the items, {@code false} otherwise
	 */
	public boolean exchangeItemsById(String process, WorldObject reference, int coinId, long cost, int rewardId, long count, boolean sendMessage)
	{
		if (!_inventory.validateCapacityByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			}
			return false;
		}
		
		if (!_inventory.validateWeightByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			}
			return false;
		}
		
		if (destroyItemByItemId(process, coinId, cost, reference, sendMessage))
		{
			addItem(process, rewardId, count, reference, sendMessage);
			return true;
		}
		return false;
	}
	
	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process String Identifier of process triggering this action
	 * @param item ItemInstance to be dropped
	 * @param reference WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @param protectItem whether or not dropped item must be protected temporary against other players
	 * @return boolean informing if the action was successful
	 */
	public boolean dropItem(String process, ItemInstance item, WorldObject reference, boolean sendMessage, boolean protectItem)
	{
		final ItemInstance droppedItem = _inventory.dropItem(process, item, this, reference);
		if (droppedItem == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			return false;
		}
		
		droppedItem.dropMe(this, (getX() + Rnd.get(50)) - 25, (getY() + Rnd.get(50)) - 25, getZ() + 20);
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(droppedItem.getId()) && ((droppedItem.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !droppedItem.isEquipable()))
		{
			ItemsAutoDestroyTaskManager.getInstance().addItem(droppedItem);
		}
		
		// protection against auto destroy dropped item
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			droppedItem.setProtected(droppedItem.isEquipable() && (!droppedItem.isEquipable() || !Config.DESTROY_EQUIPABLE_PLAYER_ITEM));
		}
		else
		{
			droppedItem.setProtected(true);
		}
		
		// retail drop protection
		if (protectItem)
		{
			droppedItem.getDropProtection().protect(this);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(droppedItem);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
			sm.addItemName(droppedItem);
			sendPacket(sm);
		}
		
		return true;
	}
	
	public boolean dropItem(String process, ItemInstance item, WorldObject reference, boolean sendMessage)
	{
		return dropItem(process, item, reference, sendMessage, false);
	}
	
	/**
	 * Drop item from inventory by using its <b>objectID</b> and send a Server->Client InventoryUpdate packet to the PlayerInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : long Quantity of items to be dropped
	 * @param x : int coordinate for drop X
	 * @param y : int coordinate for drop Y
	 * @param z : int coordinate for drop Z
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @param protectItem
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public ItemInstance dropItem(String process, int objectId, long count, int x, int y, int z, WorldObject reference, boolean sendMessage, boolean protectItem)
	{
		final ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		final ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			return null;
		}
		
		item.dropMe(this, x, y, z);
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getId()) && ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable()))
		{
			ItemsAutoDestroyTaskManager.getInstance().addItem(item);
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			item.setProtected(!(!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM)));
		}
		else
		{
			item.setProtected(true);
		}
		
		// retail drop protection
		if (protectItem)
		{
			item.getDropProtection().protect(this);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		
		return item;
	}
	
	public ItemInstance checkItemManipulation(int objectId, long count, String action)
	{
		// TODO: if we remove objects that are not visible from the World, we'll have to remove this check
		if (World.getInstance().findObject(objectId) == null)
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item not available in World");
			return null;
		}
		
		final ItemInstance item = _inventory.getItemByObjectId(objectId);
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}
		
		if ((count < 0) || ((count > 1) && !item.isStackable()))
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}
		
		if (count > item.getCount())
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if ((hasSummon() && (_summon.getControlObjectId() == objectId)) || (_mountObjectID == objectId))
		{
			return null;
		}
		
		if (_activeEnchantItemId == objectId)
		{
			return null;
		}
		
		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && (isCastingNow() || isCastingSimultaneouslyNow()))
		{
			return null;
		}
		
		return item;
	}
	
	public boolean isSpawnProtected()
	{
		return (_spawnProtectEndTime != 0) && (_spawnProtectEndTime > Chronos.currentTimeMillis());
	}
	
	public boolean isTeleportProtected()
	{
		return (_teleportProtectEndTime != 0) && (_teleportProtectEndTime > Chronos.currentTimeMillis());
	}
	
	public void setSpawnProtection(boolean protect)
	{
		_spawnProtectEndTime = protect ? Chronos.currentTimeMillis() + (Config.PLAYER_SPAWN_PROTECTION * 1000) : 0;
	}
	
	public void setTeleportProtection(boolean protect)
	{
		_teleportProtectEndTime = protect ? Chronos.currentTimeMillis() + (Config.PLAYER_TELEPORT_PROTECTION * 1000) : 0;
	}
	
	/**
	 * Set protection from aggro mobs when getting up from fake death, according settings.
	 * @param protect
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeTaskManager.getInstance().getGameTicks() + (Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeTaskManager.TICKS_PER_SECOND) : 0;
	}
	
	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	public boolean isFakeDeath()
	{
		return _isFakeDeath;
	}
	
	public void setFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}
	
	@Override
	public boolean isAlikeDead()
	{
		return super.isAlikeDead() || _isFakeDeath;
	}
	
	/**
	 * @return the client owner of this char.
	 */
	public GameClient getClient()
	{
		return _client;
	}
	
	public void setClient(GameClient client)
	{
		_client = client;
		if ((_client != null) && (_client.getConnectionAddress() != null))
		{
			_ip = _client.getConnectionAddress().getHostAddress();
		}
	}
	
	public String getIPAddress()
	{
		return _ip;
	}
	
	public Location getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}
	
	public void setCurrentSkillWorldPosition(Location worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}
	
	@Override
	public void enableSkill(Skill skill)
	{
		super.enableSkill(skill);
		removeTimeStamp(skill);
	}
	
	@Override
	public boolean checkDoCastConditions(Skill skill)
	{
		if (!super.checkDoCastConditions(skill) || _observerMode)
		{
			return false;
		}
		
		if (_inOlympiadMode && skill.isBlockedInOlympiad())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_SKILL_IN_A_GRAND_OLYMPIAD_MATCH);
			return false;
		}
		
		// Check if the spell using charges or not in AirShip
		if ((_charges.get() < skill.getChargeConsume()) || (isInAirShip() && !skill.hasEffectType(EffectType.REFUEL_AIRSHIP)))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(skill);
			sendPacket(sm);
			return false;
		}
		return true;
	}
	
	/**
	 * Returns true if cp update should be done, false if not
	 * @return boolean
	 */
	private boolean needCpUpdate()
	{
		final double currentCp = getCurrentCp();
		if ((currentCp <= 1.0) || (getMaxCp() < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentCp <= _cpUpdateDecCheck) || (currentCp >= _cpUpdateIncCheck))
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns true if mp update should be done, false if not
	 * @return boolean
	 */
	private boolean needMpUpdate()
	{
		final double currentMp = getCurrentMp();
		if ((currentMp <= 1.0) || (getMaxMp() < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentMp <= _mpUpdateDecCheck) || (currentMp >= _mpUpdateIncCheck))
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the PlayerInstance and only current HP, MP and Level to all other PlayerInstance of the Party. <b><u>Actions</u>:</b>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this PlayerInstance</li>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other PlayerInstance of the Party</li> <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND current HP and MP to all PlayerInstance of the _statusListener</b></font>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		// TODO We mustn't send these informations to other players
		// Send the Server->Client packet StatusUpdate with current HP and MP to all PlayerInstance that must be informed of HP/MP updates of this PlayerInstance
		// super.broadcastStatusUpdate();
		
		// Send the Server->Client packet StatusUpdate with current HP, MP and CP to this PlayerInstance
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		sendPacket(su);
		
		final boolean needCpUpdate = needCpUpdate();
		final boolean needHpUpdate = needHpUpdate();
		final Party party = getParty();
		
		// Check if a party is in progress and party window update is usefull
		if ((party != null) && (needCpUpdate || needHpUpdate || needMpUpdate()))
		{
			party.broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
		}
		
		if (_inOlympiadMode && _olympiadStart && (needCpUpdate || needHpUpdate))
		{
			final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(getOlympiadGameId());
			if ((game != null) && game.isBattleStarted())
			{
				game.getZone().broadcastStatusUpdate(this);
			}
		}
		
		// In duel MP updated only with CP or HP
		if (_isInDuel && (needCpUpdate || needHpUpdate))
		{
			DuelManager.getInstance().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this PlayerInstance and CharInfo to all PlayerInstance in its _KnownPlayers. <b><u>Concept</u>:</b> Others PlayerInstance in the detection area of the PlayerInstance are identified in <b>_knownPlayers</b>. In order to inform other players of this
	 * PlayerInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet <b><u> Actions</u>:</b>
	 * <li>Send a Server->Client packet UserInfo to this PlayerInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all PlayerInstance in _KnownPlayers of the PlayerInstance (Public data only)</li> <font color=#FF0000><b><u>Caution</u>: DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP,
	 * STR, DEX...</b></font>
	 */
	public void broadcastUserInfo()
	{
		// Send user info to the current player
		sendPacket(new UserInfo(this));
		broadcastPacket(new ExBrExtraUserInfo(this));
		
		// Broadcast char info to known players
		broadcastCharInfo();
	}
	
	public void broadcastCharInfo()
	{
		final CharInfo charInfo = new CharInfo(this, false);
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			if (isVisibleFor(player))
			{
				if (isInvisible() && player.canOverrideCond(PlayerCondOverride.SEE_ALL_PLAYERS))
				{
					player.sendPacket(new CharInfo(this, true));
				}
				else
				{
					player.sendPacket(charInfo);
				}
				
				// Update relation.
				final int relation = getRelation(player);
				final boolean isAutoAttackable = isAutoAttackable(player);
				final RelationCache oldrelation = getKnownRelations().get(player.getObjectId());
				if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
				{
					final RelationChanged rc = new RelationChanged();
					rc.addRelation(this, relation, isAutoAttackable);
					if (hasSummon())
					{
						player.sendPacket(new RelationChanged(_summon, getRelation(player), isAutoAttackable(player)));
					}
					player.sendPacket(rc);
					getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
				}
			}
		});
	}
	
	public void broadcastTitleInfo()
	{
		// Send a Server->Client packet UserInfo to this PlayerInstance
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		
		// Send a Server->Client packet TitleUpdate to all PlayerInstance in _KnownPlayers of the PlayerInstance
		broadcastPacket(new NicknameChanged(this));
	}
	
	@Override
	public void broadcastPacket(IClientOutgoingPacket mov)
	{
		final boolean isCharInfo = mov instanceof CharInfo;
		if (!isCharInfo)
		{
			sendPacket(mov);
		}
		
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			player.sendPacket(mov);
			
			if (isCharInfo)
			{
				final int relation = getRelation(player);
				final boolean isAutoAttackable = isAutoAttackable(player);
				final RelationCache cache = getKnownRelations().get(player.getObjectId());
				if ((cache == null) || (cache.getRelation() != relation) || (cache.isAutoAttackable() != isAutoAttackable))
				{
					player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
					if (hasSummon())
					{
						player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
					}
					getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
				}
			}
		});
	}
	
	@Override
	public void broadcastPacket(IClientOutgoingPacket mov, int radius)
	{
		final boolean isCharInfo = mov instanceof CharInfo;
		if (!isCharInfo)
		{
			sendPacket(mov);
		}
		
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			if (!isVisibleFor(player) || (calculateDistance3D(player) >= radius))
			{
				return;
			}
			
			player.sendPacket(mov);
			
			if (isCharInfo)
			{
				final int relation = getRelation(player);
				final boolean isAutoAttackable = isAutoAttackable(player);
				final RelationCache cache = getKnownRelations().get(player.getObjectId());
				if ((cache == null) || (cache.getRelation() != relation) || (cache.isAutoAttackable() != isAutoAttackable))
				{
					player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
					if (hasSummon())
					{
						player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
					}
					getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
				}
			}
		});
	}
	
	/**
	 * @return the Alliance Identifier of the PlayerInstance.
	 */
	@Override
	public int getAllyId()
	{
		return _clan == null ? 0 : _clan.getAllyId();
	}
	
	public int getAllyCrestId()
	{
		return getAllyId() == 0 ? 0 : _clan.getAllyCrestId();
	}
	
	/**
	 * Send a Server->Client packet StatusUpdate to the PlayerInstance.
	 */
	@Override
	public void sendPacket(IClientOutgoingPacket... packets)
	{
		if (_client != null)
		{
			for (IClientOutgoingPacket packet : packets)
			{
				_client.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Send SystemMessage packet.
	 * @param id SystemMessageId
	 */
	@Override
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(new SystemMessage(id));
	}
	
	/**
	 * Manage Interact Task with another PlayerInstance. <b><u>Actions</u>:</b>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the PlayerInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the PlayerInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the PlayerInstance</li><br>
	 * @param target The Creature targeted
	 */
	public void doInteract(Creature target)
	{
		if (target == null)
		{
			return;
		}
		
		if (target.isPlayer())
		{
			final PlayerInstance temp = (PlayerInstance) target;
			sendPacket(ActionFailed.STATIC_PACKET);
			
			if ((temp.getPrivateStoreType() == PrivateStoreType.SELL) || (temp.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL))
			{
				sendPacket(new PrivateStoreListSell(this, temp));
			}
			else if (temp.getPrivateStoreType() == PrivateStoreType.BUY)
			{
				sendPacket(new PrivateStoreListBuy(this, temp));
			}
			else if (temp.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)
			{
				sendPacket(new RecipeShopSellList(this, temp));
			}
		}
		else // _interactTarget=null should never happen but one never knows ^^;
		{
			target.onAction(this);
		}
	}
	
	/**
	 * Manages AutoLoot Task.<br>
	 * <ul>
	 * <li>Send a system message to the player.</li>
	 * <li>Add the item to the player's inventory.</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this player with NewItem (use a new slot) or ModifiedItem (increase amount).</li>
	 * <li>Send a Server->Client packet StatusUpdate to this player with current weight.</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: If a party is in progress, distribute the items between the party members!</b></font>
	 * @param target the NPC dropping the item
	 * @param itemId the item ID
	 * @param itemCount the item count
	 */
	public void doAutoLoot(Attackable target, int itemId, long itemCount)
	{
		if (isInParty() && !ItemTable.getInstance().getTemplate(itemId).hasExImmediateEffect())
		{
			_party.distributeItem(this, itemId, itemCount, false, target);
		}
		else if (itemId == Inventory.ADENA_ID)
		{
			addAdena("Loot", itemCount, target, true);
		}
		else
		{
			addItem("Loot", itemId, itemCount, target, true);
		}
	}
	
	/**
	 * Method overload for {@link PlayerInstance#doAutoLoot(Attackable, int, long)}
	 * @param target the NPC dropping the item
	 * @param item the item holder
	 */
	public void doAutoLoot(Attackable target, ItemHolder item)
	{
		doAutoLoot(target, item.getId(), item.getCount());
	}
	
	/**
	 * Manage Pickup Task. <b><u>Actions</u>:</b>
	 * <li>Send a Server->Client packet StopMove to this PlayerInstance</li>
	 * <li>Remove the ItemInstance from the world and send server->client GetItem packets</li>
	 * <li>Send a System Message to the PlayerInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the PlayerInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this PlayerInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this PlayerInstance with current weight</li> <font color=#FF0000><b><u>Caution</u>: If a Party is in progress, distribute Items between party members</b></font>
	 * @param object The ItemInstance to pick up
	 */
	@Override
	public void doPickupItem(WorldObject object)
	{
		if (isAlikeDead() || _isFakeDeath)
		{
			return;
		}
		
		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		// Check if the WorldObject to pick up is a ItemInstance
		if (!object.isItem())
		{
			// dont try to pickup anything that is not an item :)
			LOGGER.warning(this + " trying to pickup wrong target." + getTarget());
			return;
		}
		
		final ItemInstance target = (ItemInstance) object;
		sendPacket(new StopMove(this));
		SystemMessage smsg = null;
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isSpawned())
			{
				// Send a Server->Client packet ActionFailed to this PlayerInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if (((isInParty() && (_party.getDistributionType() == PartyDistributionType.FINDERS_KEEPERS)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
				return;
			}
			
			if (isInvisible() && !canOverrideCond(PlayerCondOverride.ITEM_CONDITIONS))
			{
				return;
			}
			
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getObjectId()) && !isInLooterParty(target.getOwnerId()))
			{
				if (target.getId() == Inventory.ADENA_ID)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
					smsg.addLong(target.getCount());
				}
				else if (target.getCount() > 1)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addLong(target.getCount());
				}
				else
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
					smsg.addItemName(target);
				}
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(smsg);
				return;
			}
			
			// You can pickup only 1 combat flag
			if (FortSiegeManager.getInstance().isCombat(target.getId()) && !FortSiegeManager.getInstance().checkIfCanPickup(this))
			{
				return;
			}
			
			if ((target.getItemLootShedule() != null) && ((target.getOwnerId() == getObjectId()) || isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			// Remove the ItemInstance from the world and send server->client GetItem packets
			target.pickupMe(this);
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// Auto use herbs - pick up
		if (target.getItem().hasExImmediateEffect())
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler != null)
			{
				handler.useItem(this, target, false);
			}
			else
			{
				LOGGER.warning("No item handler registered for item ID: " + target.getId() + ".");
			}
			ItemTable.getInstance().destroyItem("Consume", target, this, null);
		}
		// Cursed Weapons are not distributed
		else if (CursedWeaponsManager.getInstance().isCursed(target.getId()))
		{
			addItem("Pickup", target, null, true);
		}
		else if (FortSiegeManager.getInstance().isCombat(target.getId()))
		{
			addItem("Pickup", target, null, true);
		}
		else
		{
			// if item is instance of ArmorType or WeaponType broadcast an "Attention" system message
			if ((target.getItemType() instanceof ArmorType) || (target.getItemType() instanceof WeaponType))
			{
				if (target.getEnchantLevel() > 0)
				{
					smsg = new SystemMessage(SystemMessageId.ATTENTION_C1_HAS_PICKED_UP_S2_S3);
					smsg.addPcName(this);
					smsg.addInt(target.getEnchantLevel());
				}
				else
				{
					smsg = new SystemMessage(SystemMessageId.ATTENTION_C1_HAS_PICKED_UP_S2);
					smsg.addPcName(this);
				}
				smsg.addItemName(target.getId());
				broadcastPacket(smsg, 1400);
			}
			
			// Check if a Party is in progress
			if (isInParty())
			{
				_party.distributeItem(this, target);
			}
			else if ((target.getId() == Inventory.ADENA_ID) && (_inventory.getAdenaInstance() != null))
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			else
			{
				addItem("Pickup", target, null, true);
				// Auto-Equip arrows/bolts if player has a bow/crossbow and player picks up arrows/bolts.
				final ItemInstance weapon = _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (weapon != null)
				{
					final EtcItem etcItem = target.getEtcItem();
					if (etcItem != null)
					{
						final EtcItemType itemType = etcItem.getItemType();
						if ((weapon.getItemType() == WeaponType.BOW) && (itemType == EtcItemType.ARROW))
						{
							checkAndEquipArrows();
						}
						else if ((weapon.getItemType() == WeaponType.CROSSBOW) && (itemType == EtcItemType.BOLT))
						{
							checkAndEquipBolts();
						}
					}
				}
			}
		}
	}
	
	@Override
	public void doAttack(Creature target)
	{
		super.doAttack(target);
		setRecentFakeDeath(false);
		if (target.isFakePlayer())
		{
			updatePvPStatus();
		}
	}
	
	@Override
	public void doCast(Skill skill)
	{
		if ((_currentSkill != null) && !checkUseMagicConditions(skill, _currentSkill.isCtrlPressed(), _currentSkill.isShiftPressed()))
		{
			setCastingNow(false);
			setCastingSimultaneouslyNow(false);
			return;
		}
		super.doCast(skill);
		setRecentFakeDeath(false);
	}
	
	public boolean canOpenPrivateStore()
	{
		if ((Config.SHOP_MIN_RANGE_FROM_NPC > 0) || (Config.SHOP_MIN_RANGE_FROM_PLAYER > 0))
		{
			for (Creature creature : World.getInstance().getVisibleObjects(this, Creature.class))
			{
				if ((creature.getMinShopDistance() > 0) && Util.checkIfInRange(creature.getMinShopDistance(), this, creature, true))
				{
					sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE));
					return false;
				}
			}
		}
		return !isAlikeDead() && !_inOlympiadMode && !isMounted() && !isInsideZone(ZoneId.NO_STORE) && !isCastingNow();
	}
	
	@Override
	public int getMinShopDistance()
	{
		return _waitTypeSitting ? Config.SHOP_MIN_RANGE_FROM_PLAYER : 0;
	}
	
	public void tryOpenPrivateBuyStore()
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if ((_privateStoreType == PrivateStoreType.BUY) || (_privateStoreType == PrivateStoreType.BUY_MANAGE))
			{
				setPrivateStoreType(PrivateStoreType.NONE);
			}
			if (_privateStoreType == PrivateStoreType.NONE)
			{
				if (_waitTypeSitting)
				{
					standUp();
				}
				setPrivateStoreType(PrivateStoreType.BUY_MANAGE);
				sendPacket(new PrivateStoreManageListBuy(this));
			}
		}
		else
		{
			if (isInsideZone(ZoneId.NO_STORE))
			{
				sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
			}
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public void tryOpenPrivateSellStore(boolean isPackageSale)
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if ((_privateStoreType == PrivateStoreType.SELL) || (_privateStoreType == PrivateStoreType.SELL_MANAGE) || (_privateStoreType == PrivateStoreType.PACKAGE_SELL))
			{
				setPrivateStoreType(PrivateStoreType.NONE);
			}
			
			if (_privateStoreType == PrivateStoreType.NONE)
			{
				if (_waitTypeSitting)
				{
					standUp();
				}
				setPrivateStoreType(PrivateStoreType.SELL_MANAGE);
				sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
			}
		}
		else
		{
			if ((_privateStoreType != PrivateStoreType.NONE) && !isAlikeDead())
			{
				setPrivateStoreType(PrivateStoreType.NONE);
				if (_waitTypeSitting)
				{
					standUp();
				}
				return;
			}
			if (isInsideZone(ZoneId.NO_STORE))
			{
				sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
			}
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public PreparedListContainer getMultiSell()
	{
		return _currentMultiSell;
	}
	
	public void setMultiSell(PreparedListContainer list)
	{
		_currentMultiSell = list;
	}
	
	@Override
	public boolean isTransformed()
	{
		return (_transformation != null) && !_transformation.isStance();
	}
	
	public boolean isInStance()
	{
		return (_transformation != null) && _transformation.isStance();
	}
	
	public void transform(Transform transformation)
	{
		if (!Config.ALLOW_MOUNTS_DURING_SIEGE && transformation.isRiding() && isInsideZone(ZoneId.SIEGE))
		{
			return;
		}
		
		if (_transformation != null)
		{
			// You already polymorphed and cannot polymorph again.
			sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN));
			return;
		}
		
		setQueuedSkill(null, false, false);
		if (isMounted())
		{
			// Get off the strider or something else if character is mounted
			dismount();
		}
		
		_transformation = transformation;
		getEffectList().stopAllToggles();
		transformation.onTransform(this);
		sendSkillList();
		sendPacket(new SkillCoolTime(this));
		broadcastUserInfo();
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerTransform(this, transformation.getId()), this);
	}
	
	@Override
	public void untransform()
	{
		if (_transformation == null)
		{
			return;
		}
		
		setQueuedSkill(null, false, false);
		_transformation.onUntransform(this);
		_transformation = null;
		getEffectList().stopSkillEffects(SkillFinishType.NORMAL, AbnormalType.TRANSFORM);
		sendSkillList();
		sendPacket(new SkillCoolTime(this));
		broadcastUserInfo();
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerTransform(this, 0), this);
	}
	
	@Override
	public Transform getTransformation()
	{
		return _transformation;
	}
	
	/**
	 * This returns the transformation Id of the current transformation. For example, if a player is transformed as a Buffalo, and then picks up the Zariche, the transform Id returned will be that of the Zariche, and NOT the Buffalo.
	 * @return Transformation Id
	 */
	public int getTransformationId()
	{
		return isTransformed() ? _transformation.getId() : 0;
	}
	
	public int getTransformationDisplayId()
	{
		return isTransformed() ? _transformation.getDisplayId() : 0;
	}
	
	/**
	 * Set a target. <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove the PlayerInstance from the _statusListener of the old target if it was a Creature</li>
	 * <li>Add the PlayerInstance to the _statusListener of the new target if it's a Creature</li>
	 * <li>Target the new WorldObject (add the target to the PlayerInstance _target, _knownObject and PlayerInstance to _KnownObject of the WorldObject)</li>
	 * </ul>
	 * @param worldObject The WorldObject to target
	 */
	@Override
	public void setTarget(WorldObject worldObject)
	{
		WorldObject newTarget = worldObject;
		if (newTarget != null)
		{
			final boolean isInParty = newTarget.isPlayer() && isInParty() && _party.containsPlayer(newTarget.getActingPlayer());
			
			// Prevents /target exploiting
			if (!isInParty && (Math.abs(newTarget.getZ() - getZ()) > 1000))
			{
				newTarget = null;
			}
			
			// Check if the new target is visible
			if ((newTarget != null) && !isInParty && !newTarget.isSpawned())
			{
				newTarget = null;
			}
			
			// vehicles cant be targeted
			if (!isGM() && (newTarget instanceof Vehicle))
			{
				newTarget = null;
			}
		}
		
		// Get the current target
		final WorldObject oldTarget = getTarget();
		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget)) // no target change?
			{
				// Validate location of the target.
				if ((newTarget != null) && (newTarget.getObjectId() != getObjectId()))
				{
					sendPacket(new ValidateLocation(newTarget));
				}
				return;
			}
			
			// Remove the target from the status listener.
			oldTarget.removeStatusListener(this);
		}
		
		if ((newTarget != null) && newTarget.isCreature())
		{
			final Creature target = (Creature) newTarget;
			
			// Validate location of the new target.
			if (newTarget.getObjectId() != getObjectId())
			{
				sendPacket(new ValidateLocation(target));
			}
			
			// Show the client his new target.
			sendPacket(new MyTargetSelected(this, target));
			
			// Register target to listen for hp changes.
			target.addStatusListener(this);
			
			// Send max/current hp.
			final StatusUpdate su = new StatusUpdate(target);
			su.addAttribute(StatusUpdate.MAX_HP, target.getMaxHp());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			sendPacket(su);
			
			// To others the new target, and not yourself!
			Broadcast.toKnownPlayers(this, new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ()));
		}
		
		// Target was removed?
		if ((newTarget == null) && (getTarget() != null))
		{
			broadcastPacket(new TargetUnselected(this));
		}
		
		// Target the new WorldObject (add the target to the PlayerInstance _target, _knownObject and PlayerInstance to _KnownObject of the WorldObject)
		super.setTarget(newTarget);
	}
	
	/**
	 * Return the active weapon instance (always equipped in the right hand).
	 */
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}
	
	/**
	 * Return the active weapon item (always equipped in the right hand).
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		final ItemInstance weapon = getActiveWeaponInstance();
		return weapon == null ? _fistsWeaponItem : (Weapon) weapon.getItem();
	}
	
	public ItemInstance getChestArmorInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}
	
	public ItemInstance getLegsArmorInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
	}
	
	public Armor getActiveChestArmorItem()
	{
		final ItemInstance armor = getChestArmorInstance();
		return armor == null ? null : (Armor) armor.getItem();
	}
	
	public Armor getActiveLegsArmorItem()
	{
		final ItemInstance legs = getLegsArmorInstance();
		return legs == null ? null : (Armor) legs.getItem();
	}
	
	public boolean isWearingHeavyArmor()
	{
		final ItemInstance legs = getLegsArmorInstance();
		final ItemInstance armor = getChestArmorInstance();
		if ((armor != null) && (legs != null) && (legs.getItemType() == ArmorType.HEAVY) && (armor.getItemType() == ArmorType.HEAVY))
		{
			return true;
		}
		if ((armor != null) && (getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == Item.SLOT_FULL_ARMOR) && (armor.getItemType() == ArmorType.HEAVY))
		{
			return true;
		}
		return false;
	}
	
	public boolean isWearingLightArmor()
	{
		final ItemInstance legs = getLegsArmorInstance();
		final ItemInstance armor = getChestArmorInstance();
		if ((armor != null) && (legs != null) && (legs.getItemType() == ArmorType.LIGHT) && (armor.getItemType() == ArmorType.LIGHT))
		{
			return true;
		}
		if ((armor != null) && (_inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == Item.SLOT_FULL_ARMOR) && (armor.getItemType() == ArmorType.LIGHT))
		{
			return true;
		}
		return false;
	}
	
	public boolean isWearingMagicArmor()
	{
		final ItemInstance legs = getLegsArmorInstance();
		final ItemInstance armor = getChestArmorInstance();
		if ((armor != null) && (legs != null) && (legs.getItemType() == ArmorType.MAGIC) && (armor.getItemType() == ArmorType.MAGIC))
		{
			return true;
		}
		if ((armor != null) && (_inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == Item.SLOT_FULL_ARMOR) && (armor.getItemType() == ArmorType.MAGIC))
		{
			return true;
		}
		return false;
	}
	
	public boolean isMarried()
	{
		return _married;
	}
	
	public void setMarried(boolean value)
	{
		_married = value;
	}
	
	public boolean isEngageRequest()
	{
		return _engagerequest;
	}
	
	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}
	
	public void setMarryRequest(boolean value)
	{
		_marryrequest = value;
	}
	
	public boolean isMarryRequest()
	{
		return _marryrequest;
	}
	
	public void setMarryAccepted(boolean value)
	{
		_marryaccepted = value;
	}
	
	public boolean isMarryAccepted()
	{
		return _marryaccepted;
	}
	
	public int getEngageId()
	{
		return _engageid;
	}
	
	public int getPartnerId()
	{
		return _partnerId;
	}
	
	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}
	
	public int getCoupleId()
	{
		return _coupleId;
	}
	
	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}
	
	public void engageAnswer(int answer)
	{
		if (!_engagerequest || (_engageid == 0))
		{
			return;
		}
		
		final PlayerInstance ptarget = World.getInstance().getPlayer(_engageid);
		setEngageRequest(false, 0);
		if (ptarget != null)
		{
			if (answer == 1)
			{
				CoupleManager.getInstance().createCouple(ptarget, PlayerInstance.this);
				ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
			}
			else
			{
				ptarget.sendMessage("Request to Engage has been >DENIED<!");
			}
		}
	}
	
	/**
	 * Return the secondary weapon instance (always equipped in the left hand).
	 */
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	/**
	 * Return the secondary Item item (always equipped in the left hand).<br>
	 * Arrows, Shield..
	 */
	@Override
	public Item getSecondaryWeaponItem()
	{
		final ItemInstance item = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return item != null ? item.getItem() : null;
	}
	
	/**
	 * Kill the Creature, Apply Death Penalty, Manage gain/loss Karma and Item Drop. <b><u>Actions</u>:</b>
	 * <li>Reduce the Experience of the PlayerInstance in function of the calculated Death Penalty</li>
	 * <li>If necessary, unsummon the Pet of the killed PlayerInstance</li>
	 * <li>Manage Karma gain for attacker and Karam loss for the killed PlayerInstance</li>
	 * <li>If the killed PlayerInstance has Karma, manage Drop Item</li>
	 * <li>Kill the PlayerInstance</li><br>
	 * @param killer
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the PlayerInstance
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (isMounted())
		{
			stopFeed();
		}
		
		// synchronized (this)
		// {
		if (_isFakeDeath)
		{
			stopFakeDeath(true);
		}
		// }
		if (killer != null)
		{
			final PlayerInstance pk = killer.getActingPlayer();
			final boolean fpcKill = killer.isFakePlayer();
			if ((pk != null) || fpcKill)
			{
				if (pk != null)
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPKill(pk, this), this);
					
					// pvp/pk item rewards
					if (!(Config.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
						!(Config.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
					{
						// pvp
						if (Config.REWARD_PVP_ITEM && (_pvpFlag != 0))
						{
							pk.addItem("PvP Item Reward", Config.REWARD_PVP_ITEM_ID, Config.REWARD_PVP_ITEM_AMOUNT, this, Config.REWARD_PVP_ITEM_MESSAGE);
						}
						// pk
						if (Config.REWARD_PK_ITEM && (_pvpFlag == 0))
						{
							pk.addItem("PK Item Reward", Config.REWARD_PK_ITEM_ID, Config.REWARD_PK_ITEM_AMOUNT, this, Config.REWARD_PK_ITEM_MESSAGE);
						}
					}
				}
				
				// announce pvp/pk
				if (Config.ANNOUNCE_PK_PVP && (((pk != null) && !pk.isGM()) || fpcKill))
				{
					String msg = "";
					if (_pvpFlag == 0)
					{
						msg = Config.ANNOUNCE_PK_MSG.replace("$killer", killer.getName()).replace("$target", getName());
						if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1);
							sm.addString(msg);
							Broadcast.toAllOnlinePlayers(sm);
						}
						else
						{
							Broadcast.toAllOnlinePlayers(msg, false);
						}
					}
					else if (_pvpFlag != 0)
					{
						msg = Config.ANNOUNCE_PVP_MSG.replace("$killer", killer.getName()).replace("$target", getName());
						if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1);
							sm.addString(msg);
							Broadcast.toAllOnlinePlayers(sm);
						}
						else
						{
							Broadcast.toAllOnlinePlayers(msg, false);
						}
					}
				}
				
				if (fpcKill && Config.FAKE_PLAYER_KILL_KARMA && (_pvpFlag == 0) && (getKarma() <= 0))
				{
					killer.setKarma(killer.getKarma() + 150);
					killer.broadcastInfo();
				}
			}
			
			broadcastStatusUpdate();
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			// Issues drop of Cursed Weapon.
			if (isCursedWeaponEquipped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			}
			else if (_combatFlagEquippedId)
			{
				// TODO: Fort siege during TW??
				if (TerritoryWarManager.getInstance().isTWInProgress())
				{
					TerritoryWarManager.getInstance().dropCombatFlag(this, true, false);
				}
				else
				{
					final Fort fort = FortManager.getInstance().getFort(this);
					if (fort != null)
					{
						FortSiegeManager.getInstance().dropCombatFlag(this, fort.getResidenceId());
					}
					else
					{
						final int slot = _inventory.getSlotFromItem(_inventory.getItemByItemId(9819));
						_inventory.unEquipItemInBodySlot(slot);
						destroyItem("CombatFlag", _inventory.getItemByItemId(9819), null, true);
					}
				}
			}
			else
			{
				final boolean insidePvpZone = isInsideZone(ZoneId.PVP);
				final boolean insideSiegeZone = isInsideZone(ZoneId.SIEGE);
				if ((pk == null) || !pk.isCursedWeaponEquipped())
				{
					onDieDropItem(killer); // Check if any item should be dropped
					if (!insidePvpZone && !insideSiegeZone)
					{
						if ((pk != null) && (pk.getClan() != null) && (getClan() != null) && !isAcademyMember() && !(pk.isAcademyMember()))
						{
							if ((_clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getId())) || (isInSiege() && pk.isInSiege()))
							{
								if (AntiFeedManager.getInstance().check(killer, this))
								{
									// when your reputation score is 0 or below, the other clan cannot acquire any reputation points
									if (_clan.getReputationScore() > 0)
									{
										pk.getClan().addReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
									}
									// when the opposing sides reputation score is 0 or below, your clans reputation score does not decrease
									if (pk.getClan().getReputationScore() > 0)
									{
										_clan.takeReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
									}
								}
							}
						}
					}
					// If player is Lucky shouldn't get penalized.
					if (!isLucky() && (insideSiegeZone || !insidePvpZone) && !_nevitSystem.isAdventBlessingActive())
					{
						calculateDeathExpPenalty(killer, isAtWarWith(pk));
					}
				}
			}
		}
		
		// Unsummon Cubics
		if (!_cubics.isEmpty())
		{
			for (CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			_cubics.clear();
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		if (isInParty() && _party.isInDimensionalRift())
		{
			_party.getDimensionalRift().getDeadMemberList().add(this);
		}
		
		if (_agathionId != 0)
		{
			setAgathionId(0);
		}
		
		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);
		
		stopRentPet();
		stopWaterTask();
		
		AntiFeedManager.getInstance().setLastDeathTime(getObjectId());
		
		// FIXME: Karma reduction tempfix.
		if (getKarma() > 0) // && (killer instanceof GuardInstance))
		{
			setKarma(getKarma() < 200 ? 0 : (int) (getKarma() - (getKarma() / 4)));
		}
		
		return true;
	}
	
	private void onDieDropItem(Creature killer)
	{
		if (isOnEvent() || (killer == null))
		{
			return;
		}
		
		final PlayerInstance pk = killer.getActingPlayer();
		if ((getKarma() <= 0) && (pk != null) && (pk.getClan() != null) && (getClan() != null) && (pk.getClan().isAtWarWith(_clanId)
		// || _clan.isAtWarWith(((PlayerInstance)killer).getClanId())
		))
		{
			return;
		}
		
		if ((!isInsideZone(ZoneId.PVP) || (pk == null)) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			final boolean isKillerNpc = killer instanceof Npc;
			final int pkLimit = Config.KARMA_PK_LIMIT;
			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;
			if ((getKarma() > 0) && (_pkKills >= pkLimit))
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && (getLevel() > 4) && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}
			
			if ((dropPercent > 0) && (Rnd.get(100) < dropPercent))
			{
				int dropCount = 0;
				int itemDropPercent = 0;
				for (ItemInstance itemDrop : _inventory.getItems())
				{
					// Don't drop
					if (itemDrop.isShadowItem() || // Dont drop Shadow Items
						itemDrop.isTimeLimitedItem() || // Dont drop Time Limited Items
						!itemDrop.isDropable() || (itemDrop.getId() == Inventory.ADENA_ID) || // Adena
						(itemDrop.getItem().getType2() == Item.TYPE2_QUEST) || // Quest Items
						(hasSummon() && (_summon.getControlObjectId() == itemDrop.getId())) || // Control Item of active pet
						(Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getId()) >= 0) || // Item listed in the non droppable item list
						(Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getId()) >= 0 // Item listed in the non droppable pet item list
						))
					{
						continue;
					}
					
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						_inventory.unEquipItemInSlot(itemDrop.getLocationSlot());
					}
					else
					{
						itemDropPercent = dropItem; // Item in inventory
					}
					
					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);
						if (isKarmaDrop)
						{
							LOGGER.warning(getName() + " has karma and dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
						}
						else
						{
							LOGGER.warning(getName() + " dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
						}
						
						if (++dropCount >= dropLimit)
						{
							break;
						}
					}
				}
			}
		}
	}
	
	public void onKillUpdatePvPKarma(Creature target)
	{
		if ((target == null) || !target.isPlayable())
		{
			return;
		}
		
		final PlayerInstance targetPlayer = target.getActingPlayer();
		if ((targetPlayer == null) || (targetPlayer == this))
		{
			return;
		}
		
		if (isCursedWeaponEquipped() && target.isPlayer())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			return;
		}
		
		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel())
		{
			return;
		}
		
		// If in Arena, do nothing
		if (isInsideZone(ZoneId.PVP) || targetPlayer.isInsideZone(ZoneId.PVP))
		{
			if ((getSiegeState() > 0) && (targetPlayer.getSiegeState() > 0) && (getSiegeState() != targetPlayer.getSiegeState()))
			{
				final Clan targetClan = targetPlayer.getClan();
				if ((_clan != null) && (targetClan != null))
				{
					_clan.addSiegeKill();
					targetClan.addSiegeDeath();
				}
			}
			return;
		}
		
		// Check if it's pvp
		if ((checkIfPvP(target) && (targetPlayer.getPvpFlag() != 0)) || (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP)))
		{
			increasePvpKills(target);
		}
		else
		{
			// Target player doesn't have pvp flag set
			// check about wars
			if ((targetPlayer.getClan() != null) && (getClan() != null) && getClan().isAtWarWith(targetPlayer.getClanId()) && targetPlayer.getClan().isAtWarWith(getClanId()) && (targetPlayer.getPledgeType() != Clan.SUBUNIT_ACADEMY) && (getPledgeType() != Clan.SUBUNIT_ACADEMY))
			{
				// 'Both way war' -> 'PvP Kill'
				increasePvpKills(target);
				return;
			}
			
			// 'No war' or 'One way war' -> 'Normal PK'
			if (targetPlayer.getKarma() > 0) // Target player has karma
			{
				if (Config.KARMA_AWARD_PK_KILL)
				{
					increasePvpKills(target);
				}
			}
			else if (targetPlayer.getPvpFlag() == 0) // Target player doesn't have karma
			{
				if (Config.FACTION_SYSTEM_ENABLED)
				{
					if ((_isGood && targetPlayer.isGood()) || (_isEvil && targetPlayer.isEvil()))
					{
						increasePkKillsAndKarma(target);
					}
				}
				else
				{
					increasePkKillsAndKarma(target);
				}
				checkItemRestriction(); // Unequip adventurer items
			}
		}
	}
	
	/**
	 * Increase the pvp kills count and send the info to the player
	 * @param target
	 */
	public void increasePvpKills(Creature target)
	{
		if (target.isPlayer() && AntiFeedManager.getInstance().check(this, target))
		{
			setPvpKills(_pvpKills + 1);
			updatePvpTitleAndColor(true);
			
			// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
			sendPacket(new UserInfo(this));
			sendPacket(new ExBrExtraUserInfo(this));
		}
	}
	
	/**
	 * Increase pk count, karma and send the info to the player
	 * @param target
	 */
	public void increasePkKillsAndKarma(Creature target)
	{
		// Only playables can increase karma/pk
		if ((target == null) || !target.isPlayable())
		{
			return;
		}
		
		// Calculate new karma. (calculate karma before incrase pk count!)
		setKarma(getKarma() + Formulas.calculateKarmaGain(_pkKills, target.isSummon()));
		
		// PK Points are increased only if you kill a player.
		if (target.isPlayer())
		{
			setPkKills(_pkKills + 1);
		}
		
		// Update player's UI.
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
	}
	
	public void updatePvpTitleAndColor(boolean broadcastInfo)
	{
		if (Config.PVP_COLOR_SYSTEM_ENABLED && !Config.FACTION_SYSTEM_ENABLED) // Faction system uses title colors.
		{
			if ((_pvpKills >= (Config.PVP_AMOUNT1)) && (_pvpKills < (Config.PVP_AMOUNT2)))
			{
				setTitle("\u00AE " + Config.TITLE_FOR_PVP_AMOUNT1 + " \u00AE");
				_appearance.setTitleColor(Config.NAME_COLOR_FOR_PVP_AMOUNT1);
			}
			else if ((_pvpKills >= (Config.PVP_AMOUNT2)) && (_pvpKills < (Config.PVP_AMOUNT3)))
			{
				setTitle("\u00AE " + Config.TITLE_FOR_PVP_AMOUNT2 + " \u00AE");
				_appearance.setTitleColor(Config.NAME_COLOR_FOR_PVP_AMOUNT2);
			}
			else if ((_pvpKills >= (Config.PVP_AMOUNT3)) && (_pvpKills < (Config.PVP_AMOUNT4)))
			{
				setTitle("\u00AE " + Config.TITLE_FOR_PVP_AMOUNT3 + " \u00AE");
				_appearance.setTitleColor(Config.NAME_COLOR_FOR_PVP_AMOUNT3);
			}
			else if ((_pvpKills >= (Config.PVP_AMOUNT4)) && (_pvpKills < (Config.PVP_AMOUNT5)))
			{
				setTitle("\u00AE " + Config.TITLE_FOR_PVP_AMOUNT4 + " \u00AE");
				_appearance.setTitleColor(Config.NAME_COLOR_FOR_PVP_AMOUNT4);
			}
			else if (_pvpKills >= (Config.PVP_AMOUNT5))
			{
				setTitle("\u00AE " + Config.TITLE_FOR_PVP_AMOUNT5 + " \u00AE");
				_appearance.setTitleColor(Config.NAME_COLOR_FOR_PVP_AMOUNT5);
			}
			
			if (broadcastInfo)
			{
				broadcastTitleInfo();
			}
		}
	}
	
	public void updatePvPStatus()
	{
		if (isInsideZone(ZoneId.PVP))
		{
			return;
		}
		setPvpFlagLasts(Chronos.currentTimeMillis() + Config.PVP_NORMAL_TIME);
		if (_pvpFlag == 0)
		{
			startPvPFlag();
		}
	}
	
	public void updatePvPStatus(Creature target)
	{
		final PlayerInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
		{
			return;
		}
		
		if (this == targetPlayer)
		{
			return;
		}
		
		if (Config.FACTION_SYSTEM_ENABLED && target.isPlayer() && ((isGood() && targetPlayer.isEvil()) || (isEvil() && targetPlayer.isGood())))
		{
			return;
		}
		
		if (_isInDuel && (targetPlayer.getDuelId() == getDuelId()))
		{
			return;
		}
		if ((!isInsideZone(ZoneId.PVP) || !targetPlayer.isInsideZone(ZoneId.PVP)) && (targetPlayer.getKarma() == 0))
		{
			if (checkIfPvP(targetPlayer))
			{
				setPvpFlagLasts(Chronos.currentTimeMillis() + Config.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(Chronos.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			}
			if (_pvpFlag == 0)
			{
				startPvPFlag();
			}
		}
	}
	
	/**
	 * @return {@code true} if player has Lucky effect and is level 9 or less
	 */
	public boolean isLucky()
	{
		return (getLevel() <= 9) && isAffectedBySkill(CommonSkill.LUCKY.getId());
	}
	
	/**
	 * Restore the specified % of experience this PlayerInstance has lost and sends a Server->Client StatusUpdate packet.
	 * @param restorePercent
	 */
	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((_expBeforeDeath - getExp()) * restorePercent) / 100));
			setExpBeforeDeath(0);
		}
	}
	
	/**
	 * Reduce the Experience (and level if necessary) of the PlayerInstance in function of the calculated Death Penalty.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <li>Calculate the Experience loss</li>
	 * <li>Set the value of _expBeforeDeath</li>
	 * <li>Set the new Experience value of the PlayerInstance and Decrease its level if necessary</li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience</li><br>
	 * @param killer
	 * @param atWar
	 */
	public void calculateDeathExpPenalty(Creature killer, boolean atWar)
	{
		final int lvl = getLevel();
		double percentLost = PlayerXpPercentLostData.getInstance().getXpPercent(getLevel());
		if (killer != null)
		{
			if (killer.isRaid())
			{
				percentLost *= calcStat(Stat.REDUCE_EXP_LOST_BY_RAID, 1);
			}
			else if (killer.isMonster())
			{
				percentLost *= calcStat(Stat.REDUCE_EXP_LOST_BY_MOB, 1);
			}
			else if (killer.isPlayable())
			{
				percentLost *= calcStat(Stat.REDUCE_EXP_LOST_BY_PVP, 1);
			}
		}
		
		if (getKarma() > 0)
		{
			percentLost *= Config.RATE_KARMA_EXP_LOST;
		}
		
		// Calculate the Experience loss
		long lostExp = 0;
		if (!isOnEvent())
		{
			if (lvl < ExperienceData.getInstance().getMaxLevel())
			{
				lostExp = Math.round(((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost) / 100);
			}
			else
			{
				lostExp = Math.round(((getStat().getExpForLevel(ExperienceData.getInstance().getMaxLevel()) - getStat().getExpForLevel(ExperienceData.getInstance().getMaxLevel() - 1)) * percentLost) / 100);
			}
		}
		
		if (isFestivalParticipant() || atWar)
		{
			lostExp /= 4.0;
		}
		
		setExpBeforeDeath(getExp());
		
		if (_nevitSystem.isAdventBlessingActive())
		{
			lostExp = 0;
		}
		
		getStat().removeExp(lostExp);
	}
	
	public boolean isPartyWaiting()
	{
		return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
	}
	
	public void setPartyRoom(int id)
	{
		_partyroom = id;
	}
	
	public int getPartyRoom()
	{
		return _partyroom;
	}
	
	public boolean isInPartyMatchRoom()
	{
		return _partyroom > 0;
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task. <b><u>Actions</u>:</b>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopFeed();
		storePetFood(_mountNpcId);
		stopRentPet();
		stopPvpRegTask();
		stopSoulTask();
		stopChargeTask();
		stopFameTask();
		stopVitalityTask();
		stopNevitHourglassTask();
		stopRecoGiveTask();
	}
	
	@Override
	public Summon getSummon()
	{
		return _summon;
	}
	
	/**
	 * @return the Decoy of the PlayerInstance or null.
	 */
	public Decoy getDecoy()
	{
		return _decoy;
	}
	
	/**
	 * @return the Trap of the PlayerInstance or null.
	 */
	public TrapInstance getTrap()
	{
		return _trap;
	}
	
	/**
	 * Set the Summon of the PlayerInstance.
	 * @param summon
	 */
	public void setPet(Summon summon)
	{
		_summon = summon;
	}
	
	/**
	 * Set the Decoy of the PlayerInstance.
	 * @param decoy
	 */
	public void setDecoy(Decoy decoy)
	{
		_decoy = decoy;
	}
	
	/**
	 * Set the Trap of this PlayerInstance
	 * @param trap
	 */
	public void setTrap(TrapInstance trap)
	{
		_trap = trap;
	}
	
	/**
	 * @return the Summon of the PlayerInstance or null.
	 */
	public Collection<TamedBeastInstance> getTrainedBeasts()
	{
		return _tamedBeast;
	}
	
	/**
	 * Set the Summon of the PlayerInstance.
	 * @param tamedBeast
	 */
	public void addTrainedBeast(TamedBeastInstance tamedBeast)
	{
		if (_tamedBeast == null)
		{
			_tamedBeast = ConcurrentHashMap.newKeySet();
		}
		_tamedBeast.add(tamedBeast);
	}
	
	/**
	 * @return the PlayerInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public Request getRequest()
	{
		return _request;
	}
	
	/**
	 * Set the PlayerInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 * @param requester
	 */
	public void setActiveRequester(PlayerInstance requester)
	{
		_activeRequester = requester;
	}
	
	/**
	 * @return the PlayerInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public PlayerInstance getActiveRequester()
	{
		final PlayerInstance requester = _activeRequester;
		if ((requester != null) && requester.isRequestExpired() && (_activeTradeList == null))
		{
			_activeRequester = null;
		}
		return _activeRequester;
	}
	
	/**
	 * @return True if a transaction is in progress.
	 */
	public boolean isProcessingRequest()
	{
		return (getActiveRequester() != null) || (_requestExpireTime > GameTimeTaskManager.getInstance().getGameTicks());
	}
	
	/**
	 * @return True if a transaction is in progress.
	 */
	public boolean isProcessingTransaction()
	{
		return (getActiveRequester() != null) || (_activeTradeList != null) || (_requestExpireTime > GameTimeTaskManager.getInstance().getGameTicks());
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 * @param partner
	 */
	public void onTransactionRequest(PlayerInstance partner)
	{
		_requestExpireTime = GameTimeTaskManager.getInstance().getGameTicks() + (REQUEST_TIMEOUT * GameTimeTaskManager.TICKS_PER_SECOND);
		partner.setActiveRequester(this);
	}
	
	/**
	 * Return true if last request is expired.
	 * @return
	 */
	public boolean isRequestExpired()
	{
		return _requestExpireTime <= GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 * @param warehouse
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}
	
	/**
	 * @return active Warehouse.
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}
	
	/**
	 * Select the TradeList to be used in next activity.
	 * @param tradeList
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}
	
	/**
	 * @return active TradeList.
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}
	
	public void onTradeStart(PlayerInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		
		final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_BEGIN_TRADING_WITH_C1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}
	
	public void onTradeConfirm(PlayerInstance partner)
	{
		final SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_CONFIRMED_THE_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(TradeOtherDone.STATIC_PACKET);
	}
	
	public void onTradeCancel(PlayerInstance partner)
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		_activeTradeList.lock();
		_activeTradeList = null;
		sendPacket(new TradeDone(0));
		final SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_CANCELLED_THE_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}
	
	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
		if (successfull)
		{
			sendPacket(SystemMessageId.YOUR_TRADE_WAS_SUCCESSFUL);
		}
	}
	
	public void startTrade(PlayerInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}
	
	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		final PlayerInstance partner = _activeTradeList.getPartner();
		if (partner != null)
		{
			partner.onTradeCancel(this);
		}
		onTradeCancel(this);
	}
	
	public boolean hasManufactureShop()
	{
		return (_manufactureItems != null) && !_manufactureItems.isEmpty();
	}
	
	/**
	 * Get the manufacture items map of this player.
	 * @return the the manufacture items map
	 */
	public Map<Integer, ManufactureItem> getManufactureItems()
	{
		if (_manufactureItems == null)
		{
			synchronized (this)
			{
				_manufactureItems = Collections.synchronizedMap(new LinkedHashMap<>());
			}
		}
		return _manufactureItems;
	}
	
	/**
	 * Get the store name, if any.
	 * @return the store name
	 */
	public String getStoreName()
	{
		return _storeName;
	}
	
	/**
	 * Set the store name.
	 * @param name the store name to set
	 */
	public void setStoreName(String name)
	{
		_storeName = name == null ? "" : name;
	}
	
	/**
	 * @return the _buyList object of the PlayerInstance.
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
		{
			_sellList = new TradeList(this);
		}
		return _sellList;
	}
	
	/**
	 * @return the _buyList object of the PlayerInstance.
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
		{
			_buyList = new TradeList(this);
		}
		return _buyList;
	}
	
	/**
	 * Set the Private Store type of the PlayerInstance. <b><u>Values</u>:</b>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li>
	 * <li>3 : STORE_PRIVATE_BUY</li>
	 * <li>4 : buymanage</li>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><br>
	 * @param privateStoreType
	 */
	public void setPrivateStoreType(PrivateStoreType privateStoreType)
	{
		_privateStoreType = privateStoreType;
		if (Config.OFFLINE_DISCONNECT_FINISHED && (privateStoreType == PrivateStoreType.NONE) && ((_client == null) || _client.isDetached()))
		{
			Disconnection.of(this).storeMe().deleteMe();
			World.OFFLINE_TRADE_COUNT--;
		}
	}
	
	/**
	 * <b><u>Values</u>:</b>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li>
	 * <li>3 : STORE_PRIVATE_BUY</li>
	 * <li>4 : buymanage</li>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li>
	 * @return the Private Store type of the PlayerInstance.
	 */
	public PrivateStoreType getPrivateStoreType()
	{
		return _privateStoreType;
	}
	
	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the PlayerInstance.
	 * @param clan
	 */
	public void setClan(Clan clan)
	{
		_clan = clan;
		if (clan == null)
		{
			setTitle("");
			_clanId = 0;
			_clanPrivileges = new EnumIntBitmask<>(ClanPrivilege.class, false);
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			_activeWarehouse = null;
			return;
		}
		
		if (!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}
		
		_clanId = clan.getId();
	}
	
	/**
	 * @return the _clan object of the PlayerInstance.
	 */
	@Override
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * @return True if the PlayerInstance is the leader of its clan.
	 */
	public boolean isClanLeader()
	{
		return (_clan != null) && (getObjectId() == _clan.getLeaderId());
	}
	
	/**
	 * Reduce the number of arrows/bolts owned by the PlayerInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).
	 */
	@Override
	protected void reduceArrowCount(boolean bolts)
	{
		final ItemInstance arrows = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (arrows == null)
		{
			_inventory.unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			if (bolts)
			{
				_boltItem = null;
			}
			else
			{
				_arrowItem = null;
			}
			sendPacket(new ItemList(this, false));
			return;
		}
		
		// Adjust item quantity
		if (arrows.getCount() > 1)
		{
			synchronized (arrows)
			{
				arrows.changeCountWithoutTrace(-1, this, null);
				arrows.setLastChange(ItemInstance.MODIFIED);
				_inventory.refreshWeight();
			}
		}
		else
		{
			// Destroy entire item and save to database
			_inventory.destroyItem("Consume", arrows, this, null);
			_inventory.unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			if (bolts)
			{
				_boltItem = null;
			}
			else
			{
				_arrowItem = null;
			}
			
			sendPacket(new ItemList(this, false));
			return;
		}
		
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(arrows);
			sendPacket(iu);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
	}
	
	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the PlayerInstance then return True.
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equipped in left hand
		if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the ItemInstance of the arrows needed for this bow
			_arrowItem = _inventory.findArrowForBow(getActiveWeaponItem());
			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				_inventory.setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
				
				// Send a Server->Client packet ItemList to this PlayerInstance to update left hand equipement
				sendPacket(new ItemList(this, false));
			}
		}
		else
		{
			// Get the ItemInstance of arrows equipped in left hand
			_arrowItem = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}
		return _arrowItem != null;
	}
	
	/**
	 * Equip bolts needed in left hand and send a Server->Client packet ItemList to the PlayerInstance then return True.
	 */
	@Override
	protected boolean checkAndEquipBolts()
	{
		// Check if nothing is equipped in left hand
		if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the ItemInstance of the arrows needed for this bow
			_boltItem = _inventory.findBoltForCrossBow(getActiveWeaponItem());
			if (_boltItem != null)
			{
				// Equip arrows needed in left hand
				_inventory.setPaperdollItem(Inventory.PAPERDOLL_LHAND, _boltItem);
				
				// Send a Server->Client packet ItemList to this PlayerInstance to update left hand equipement
				sendPacket(new ItemList(this, false));
			}
		}
		else
		{
			// Get the ItemInstance of arrows equipped in left hand
			_boltItem = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}
		return _boltItem != null;
	}
	
	/**
	 * Disarm the player's weapon.
	 * @return {@code true} if the player was disarmed or doesn't have a weapon to disarm, {@code false} otherwise.
	 */
	public boolean disarmWeapons()
	{
		// If there is no weapon to disarm then return true.
		final ItemInstance wpn = _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			return true;
		}
		
		// Don't allow disarming a cursed weapon
		if (isCursedWeaponEquipped())
		{
			return false;
		}
		
		// Don't allow disarming a Combat Flag or Territory Ward.
		if (_combatFlagEquippedId)
		{
			return false;
		}
		
		// Don't allow disarming if the weapon is force equip.
		if (wpn.getWeaponItem().isForceEquip())
		{
			return false;
		}
		
		final List<ItemInstance> unequipped = _inventory.unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
		final InventoryUpdate iu = new InventoryUpdate();
		for (ItemInstance itm : unequipped)
		{
			iu.addModifiedItem(itm);
		}
		
		sendPacket(iu);
		abortAttack();
		broadcastUserInfo();
		
		// This can be 0 if the user pressed the right mousebutton twice very fast.
		if (!unequipped.isEmpty())
		{
			final SystemMessage sm;
			final ItemInstance unequippedItem = unequipped.get(0);
			if (unequippedItem.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(unequippedItem.getEnchantLevel());
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
			}
			sm.addItemName(unequippedItem);
			sendPacket(sm);
		}
		return true;
	}
	
	/**
	 * Disarm the player's shield.
	 * @return {@code true}.
	 */
	public boolean disarmShield()
	{
		final ItemInstance sld = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			final List<ItemInstance> unequipped = _inventory.unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance itm : unequipped)
			{
				iu.addModifiedItem(itm);
			}
			sendPacket(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (!unequipped.isEmpty())
			{
				SystemMessage sm = null;
				final ItemInstance unequippedItem = unequipped.get(0);
				if (unequippedItem.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
					sm.addInt(unequippedItem.getEnchantLevel());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
				}
				sm.addItemName(unequippedItem);
				sendPacket(sm);
			}
		}
		return true;
	}
	
	public boolean mount(Summon pet)
	{
		if (!Config.ALLOW_MOUNTS_DURING_SIEGE && isInsideZone(ZoneId.SIEGE))
		{
			return false;
		}
		
		if (!disarmWeapons() || !disarmShield() || isTransformed())
		{
			return false;
		}
		
		getEffectList().stopAllToggles();
		setMount(pet.getId(), pet.getLevel());
		setMountObjectID(pet.getControlObjectId());
		startFeed(pet.getId());
		broadcastPacket(new Ride(this));
		
		// Notify self and others about speed change
		broadcastUserInfo();
		
		pet.unSummon(this);
		return true;
	}
	
	public boolean mount(int npcId, int controlItemObjId, boolean useFood)
	{
		if (!disarmWeapons() || !disarmShield() || isTransformed())
		{
			return false;
		}
		
		getEffectList().stopAllToggles();
		setMount(npcId, getLevel());
		setMountObjectID(controlItemObjId);
		broadcastPacket(new Ride(this));
		
		// Notify self and others about speed change
		broadcastUserInfo();
		if (useFood)
		{
			startFeed(npcId);
		}
		return true;
	}
	
	public boolean mountPlayer(Summon pet)
	{
		if ((pet != null) && pet.isMountable() && !isMounted() && !isBetrayed())
		{
			if (isDead())
			{
				// A strider cannot be ridden when dead
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_STRIDER_CANNOT_BE_RIDDEN_WHEN_DEAD);
				return false;
			}
			else if (pet.isDead())
			{
				// A dead strider cannot be ridden.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_DEAD_STRIDER_CANNOT_BE_RIDDEN);
				return false;
			}
			else if (pet.isInCombat() || pet.isRooted())
			{
				// A strider in battle cannot be ridden
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_STRIDER_IN_BATTLE_CANNOT_BE_RIDDEN);
				return false;
			}
			else if (isInCombat())
			{
				// A strider cannot be ridden while in battle
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_STRIDER_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE);
				return false;
			}
			else if (_waitTypeSitting)
			{
				// A strider can be ridden only when standing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_STRIDER_CAN_BE_RIDDEN_ONLY_WHEN_STANDING);
				return false;
			}
			else if (_fishing)
			{
				// You can't mount, dismount, break and drop items while fishing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_2);
				return false;
			}
			else if (isTransformed() || isCursedWeaponEquipped())
			{
				// no message needed, player while transformed doesn't have mount action
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (_inventory.getItemByItemId(9819) != null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				// FIXME: Wrong Message
				sendMessage("You cannot mount a steed while holding a flag.");
				return false;
			}
			else if (pet.isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_HUNGRY_STRIDER_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
				return false;
			}
			else if (!Util.checkIfInRange(200, this, pet, true))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_ARE_TOO_FAR_AWAY_FROM_YOUR_MOUNT_TO_RIDE);
				return false;
			}
			else if (!pet.isDead() && !isMounted())
			{
				mount(pet);
			}
		}
		else if (isRentedPet())
		{
			stopRentPet();
		}
		else if (isMounted())
		{
			if ((_mountType == MountType.WYVERN) && isInsideZone(ZoneId.NO_LANDING))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_IN_THIS_LOCATION);
				return false;
			}
			else if (isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_HUNGRY_STRIDER_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
				return false;
			}
			else
			{
				dismount();
			}
		}
		return true;
	}
	
	public boolean dismount()
	{
		if (ZoneManager.getInstance().getZone(getX(), getY(), getZ() - 300, WaterZone.class) == null)
		{
			if (!isInWater() && (getZ() > 10000))
			{
				sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_IN_THIS_LOCATION);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			if ((GeoEngine.getInstance().getHeight(getX(), getY(), getZ()) + 300) < getZ())
			{
				sendPacket(SystemMessageId.YOU_CANNOT_DISMOUNT_FROM_THIS_ELEVATION);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		else
		{
			ThreadPool.schedule(() ->
			{
				if (isInWater())
				{
					broadcastUserInfo();
				}
			}, 1500);
		}
		
		final boolean wasFlying = isFlying();
		sendPacket(new SetupGauge(getObjectId(), 3, 0, 0));
		final int petId = _mountNpcId;
		setMount(0, 0);
		stopFeed();
		if (wasFlying)
		{
			removeSkill(CommonSkill.WYVERN_BREATH.getSkill());
		}
		broadcastPacket(new Ride(this));
		setMountObjectID(0);
		storePetFood(petId);
		// Notify self and others about speed change
		broadcastUserInfo();
		return true;
	}
	
	public void setUptime(long time)
	{
		_uptime = time;
	}
	
	public long getUptime()
	{
		return Chronos.currentTimeMillis() - _uptime;
	}
	
	/**
	 * Return True if the PlayerInstance is invulnerable.
	 */
	@Override
	public boolean isInvul()
	{
		return super.isInvul() || isTeleportProtected();
	}
	
	/**
	 * Return True if the PlayerInstance has a Party in progress.
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}
	
	/**
	 * Set the _party object of the PlayerInstance (without joining it).
	 * @param party
	 */
	public void setParty(Party party)
	{
		_party = party;
	}
	
	/**
	 * Set the _party object of the PlayerInstance AND join it.
	 * @param party
	 */
	public void joinParty(Party party)
	{
		if (party != null)
		{
			// First set the party otherwise this wouldn't be considered
			// as in a party into the Creature.updateEffectIcons() call.
			_party = party;
			party.addPartyMember(this);
		}
	}
	
	/**
	 * Manage the Leave Party task of the PlayerInstance.
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this, MessageType.DISCONNECTED);
			_party = null;
		}
	}
	
	/**
	 * Return the _party object of the PlayerInstance.
	 */
	@Override
	public Party getParty()
	{
		return _party;
	}
	
	public void setPartyDistributionType(PartyDistributionType pdt)
	{
		_partyDistributionType = pdt;
	}
	
	public PartyDistributionType getPartyDistributionType()
	{
		return _partyDistributionType;
	}
	
	/**
	 * Return True if the PlayerInstance is a GM.
	 */
	@Override
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}
	
	/**
	 * Set the _accessLevel of the PlayerInstance.
	 * @param level
	 */
	public void setAccessLevel(int level)
	{
		_accessLevel = AdminData.getInstance().getAccessLevel(level);
		_appearance.setNameColor(_accessLevel.getNameColor());
		_appearance.setTitleColor(_accessLevel.getTitleColor());
		broadcastUserInfo();
		
		CharNameTable.getInstance().addName(this);
		
		if (!AdminData.getInstance().hasAccessLevel(level))
		{
			LOGGER.warning("Tried to set unregistered access level " + level + " for " + toString() + ". Setting access level without privileges!");
		}
		else if (level > 0)
		{
			LOGGER.warning(_accessLevel.getName() + " access level set for character " + getName() + "! Just a warning to be careful ;)");
		}
	}
	
	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}
	
	/**
	 * @return the _accessLevel of the PlayerInstance.
	 */
	@Override
	public AccessLevel getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS)
		{
			return AdminData.getInstance().getMasterAccessLevel();
		}
		if (_accessLevel == null)
		{
			setAccessLevel(0);
		}
		return _accessLevel;
	}
	
	/**
	 * Update Stats of the PlayerInstance client side by sending Server->Client packet UserInfo/StatusUpdate to this PlayerInstance and CharInfo/StatusUpdate to all PlayerInstance in its _KnownPlayers (broadcast).
	 * @param broadcastType
	 */
	public void updateAndBroadcastStatus(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		// Send a Server->Client packet UserInfo to this PlayerInstance and CharInfo to all PlayerInstance in its _KnownPlayers (broadcast)
		if (broadcastType == 1)
		{
			sendPacket(new UserInfo(this));
			sendPacket(new ExBrExtraUserInfo(this));
		}
		if (broadcastType == 2)
		{
			broadcastUserInfo();
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the PlayerInstance and all PlayerInstance to inform (broadcast).
	 */
	public void setKarmaFlag()
	{
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (hasSummon())
			{
				player.sendPacket(new RelationChanged(_summon, getRelation(player), isAutoAttackable(player)));
			}
		});
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the PlayerInstance and all PlayerInstance to inform (broadcast).
	 */
	public void broadcastKarma()
	{
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.KARMA, getKarma());
		sendPacket(su);
		
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (hasSummon())
			{
				player.sendPacket(new RelationChanged(_summon, getRelation(player), isAutoAttackable(player)));
			}
		});
	}
	
	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).
	 * @param isOnline
	 * @param updateInDb
	 */
	public void setOnlineStatus(boolean isOnline, boolean updateInDb)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		if (updateInDb)
		{
			updateOnlineStatus();
		}
	}
	
	public void setIn7sDungeon(boolean isIn7sDungeon)
	{
		_isIn7sDungeon = isIn7sDungeon;
	}
	
	/**
	 * Update the characters table of the database with online status and lastAccess of this PlayerInstance (called when login and logout).
	 */
	public void updateOnlineStatus()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?"))
		{
			ps.setInt(1, isOnlineInt());
			ps.setLong(2, Chronos.currentTimeMillis());
			ps.setInt(3, getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed updating character online status.", e);
		}
	}
	
	/**
	 * Create a new player in the characters table of the database.
	 * @return
	 */
	private boolean createDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_CHARACTER))
		{
			ps.setString(1, _accountName);
			ps.setInt(2, getObjectId());
			ps.setString(3, getName());
			ps.setInt(4, getLevel());
			ps.setInt(5, getMaxHp());
			ps.setDouble(6, getCurrentHp());
			ps.setInt(7, getMaxCp());
			ps.setDouble(8, getCurrentCp());
			ps.setInt(9, getMaxMp());
			ps.setDouble(10, getCurrentMp());
			ps.setInt(11, _appearance.getFace());
			ps.setInt(12, _appearance.getHairStyle());
			ps.setInt(13, _appearance.getHairColor());
			ps.setInt(14, _appearance.isFemale() ? 1 : 0);
			ps.setLong(15, getExp());
			ps.setLong(16, getSp());
			ps.setInt(17, getKarma());
			ps.setInt(18, _fame);
			ps.setInt(19, _pvpKills);
			ps.setInt(20, _pkKills);
			ps.setInt(21, _clanId);
			ps.setInt(22, getRace().ordinal());
			ps.setInt(23, getClassId().getId());
			ps.setLong(24, _deleteTimer);
			ps.setInt(25, hasDwarvenCraft() ? 1 : 0);
			ps.setString(26, getTitle());
			ps.setInt(27, _appearance.getTitleColor());
			ps.setInt(28, getAccessLevel().getLevel());
			ps.setInt(29, isOnlineInt());
			ps.setInt(30, _isIn7sDungeon ? 1 : 0);
			ps.setInt(31, _clanPrivileges.getBitmask());
			ps.setInt(32, _wantsPeace);
			ps.setInt(33, _baseClass);
			ps.setInt(34, _newbie);
			ps.setInt(35, _noble ? 1 : 0);
			ps.setLong(36, 0);
			ps.setTimestamp(37, new Timestamp(_createDate.getTimeInMillis()));
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not insert char data: " + e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	/**
	 * Retrieve a PlayerInstance from the characters table of the database and add it in _allObjects of the L2world. <b><u>Actions</u>:</b>
	 * <li>Retrieve the PlayerInstance from the characters table of the database</li>
	 * <li>Add the PlayerInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the PlayerInstance and make it invisible</li>
	 * <li>Update the overloaded status of the PlayerInstance</li><br>
	 * @param objectId Identifier of the object to initialized
	 * @return The PlayerInstance loaded from the database
	 */
	private static PlayerInstance restore(int objectId)
	{
		PlayerInstance player = null;
		double currentCp = 0;
		double currentHp = 0;
		double currentMp = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHARACTER))
		{
			// Retrieve the PlayerInstance from the characters table of the database
			ps.setInt(1, objectId);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					final int activeClassId = rset.getInt("classid");
					final boolean female = rset.getInt("sex") != Sex.MALE.ordinal();
					final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(activeClassId);
					final PlayerAppearance app = new PlayerAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
					player = new PlayerInstance(objectId, template, rset.getString("account_name"), app);
					player.setName(rset.getString("char_name"));
					player._lastAccess = rset.getLong("lastAccess");
					player.getStat().setExp(rset.getLong("exp"));
					player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
					player.getStat().setLevel(rset.getByte("level"));
					player.getStat().setSp(rset.getLong("sp"));
					
					player.setWantsPeace(rset.getInt("wantspeace"));
					
					player.setHeading(rset.getInt("heading"));
					
					player.setKarma(rset.getInt("karma"));
					player.setFame(rset.getInt("fame"));
					player.setPvpKills(rset.getInt("pvpkills"));
					player.setPkKills(rset.getInt("pkkills"));
					player.setOnlineTime(rset.getLong("onlinetime"));
					player.setNewbie(rset.getInt("newbie"));
					player.setNoble(rset.getInt("nobless") == 1);
					
					final int factionId = rset.getInt("faction");
					if (factionId == 1)
					{
						player.setGood();
					}
					if (factionId == 2)
					{
						player.setEvil();
					}
					
					player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
					if (player.getClanJoinExpiryTime() < Chronos.currentTimeMillis())
					{
						player.setClanJoinExpiryTime(0);
					}
					player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
					if (player.getClanCreateExpiryTime() < Chronos.currentTimeMillis())
					{
						player.setClanCreateExpiryTime(0);
					}
					
					player.setPowerGrade(rset.getInt("power_grade"));
					player.setPledgeType(rset.getInt("subpledge"));
					// player.setApprentice(rs.getInt("apprentice"));
					player.setDeleteTimer(rset.getLong("deletetime"));
					player.setTitle(rset.getString("title"));
					player.setAccessLevel(rset.getInt("accesslevel"));
					final int titleColor = rset.getInt("title_color");
					if (titleColor != PlayerAppearance.DEFAULT_TITLE_COLOR)
					{
						player.getAppearance().setTitleColor(titleColor);
					}
					player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
					player.setUptime(Chronos.currentTimeMillis());
					
					currentHp = rset.getDouble("curHp");
					currentCp = rset.getDouble("curCp");
					currentMp = rset.getDouble("curMp");
					player._classIndex = 0;
					try
					{
						player.setBaseClass(rset.getInt("base_class"));
					}
					catch (Exception e)
					{
						// TODO: Should this be logged?
						player.setBaseClass(activeClassId);
					}
					
					// Restore Subclass Data (cannot be done earlier in function)
					if (restoreSubClassData(player) && (activeClassId != player.getBaseClass()))
					{
						for (SubClassHolder subClass : player.getSubClasses().values())
						{
							if (subClass.getClassId() == activeClassId)
							{
								player._classIndex = subClass.getClassIndex();
							}
						}
					}
					if ((player.getClassIndex() == 0) && (activeClassId != player.getBaseClass()))
					{
						// Subclass in use but doesn't exist in DB -
						// a possible restart-while-modifysubclass cheat has been attempted.
						// Switching to use base class
						player.setClassId(player.getBaseClass());
						LOGGER.warning("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
					}
					else
					{
						player._activeClass = activeClassId;
					}
					
					player.setApprentice(rset.getInt("apprentice"));
					player.setSponsor(rset.getInt("sponsor"));
					player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
					player.setIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
					CursedWeaponsManager.getInstance().checkPlayer(player);
					
					player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
					
					player.setVitalityPoints(rset.getInt("vitality_points"), true);
					
					// Set the x,y,z position of the PlayerInstance and make it invisible
					final int x = rset.getInt("x");
					final int y = rset.getInt("y");
					final int z = rset.getInt("z");
					player.setXYZInvisible(x, y, z);
					player.setLastServerPosition(x, y, z);
					
					// Set Teleport Bookmark Slot
					player.setBookmarkSlot(rset.getInt("BookmarkSlot"));
					
					// character creation Time
					player.getCreateDate().setTimeInMillis(rset.getTimestamp("createDate").getTime());
					
					// Language
					player.setLang(rset.getString("language"));
					
					// Set Hero status if it applies
					player.setHero(Hero.getInstance().isHero(objectId));
					
					final int clanId = rset.getInt("clanid");
					if (clanId > 0)
					{
						player.setClan(ClanTable.getInstance().getClan(clanId));
					}
					
					if (player.getClan() != null)
					{
						if (player.getClan().getLeaderId() != player.getObjectId())
						{
							if (player.getPowerGrade() == 0)
							{
								player.setPowerGrade(5);
							}
							player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
						}
						else
						{
							player.getClanPrivileges().setAll();
							player.setPowerGrade(1);
						}
						player.setPledgeClass(ClanMember.calculatePledgeClass(player));
					}
					else
					{
						if (player.isNoble())
						{
							player.setPledgeClass(5);
						}
						
						if (player.isHero())
						{
							player.setPledgeClass(8);
						}
						
						player.getClanPrivileges().clear();
					}
					
					// Retrieve the name and ID of the other characters assigned to this account.
					try (PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?"))
					{
						stmt.setString(1, player._accountName);
						stmt.setInt(2, objectId);
						try (ResultSet chars = stmt.executeQuery())
						{
							while (chars.next())
							{
								player._chars.put(chars.getInt("charId"), chars.getString("char_name"));
							}
						}
					}
				}
			}
			
			if (player == null)
			{
				return null;
			}
			
			// Retrieve from the database all items of this PlayerInstance and add them to _inventory
			player.getInventory().restore();
			player.getFreight().restore();
			if (!Config.WAREHOUSE_CACHE)
			{
				player.getWarehouse();
			}
			
			// Retrieve from the database all secondary data of this PlayerInstance
			// Note that Clan, Noblesse and Hero skills are given separately and not here.
			// Retrieve from the database all skills of this PlayerInstance and add them to _skills
			player.restoreCharData();
			
			// Reward auto-get skills and all available skills if auto-learn skills is true.
			player.rewardSkills();
			
			player.restoreItemReuse();
			
			// Restore current Cp, HP and MP values
			player.setCurrentCp(currentCp);
			player.setCurrentHp(currentHp);
			player.setCurrentMp(currentMp);
			
			player.setOriginalCpHpMp(currentCp, currentHp, currentMp);
			if (currentHp < 0.5)
			{
				player.setDead(true);
				player.stopHpMpRegeneration();
			}
			
			// Restore pet if exists in the world
			player.setPet(World.getInstance().getPet(player.getObjectId()));
			if (player.hasSummon())
			{
				player.getSummon().setOwner(player);
			}
			
			// Update the overloaded status of the PlayerInstance
			player.refreshOverloaded();
			// Update the expertise status of the PlayerInstance
			player.refreshExpertisePenalty();
			
			player.restoreFriendList();
			
			if (Config.STORE_UI_SETTINGS)
			{
				player.restoreUISettings();
			}
			
			if (player.isGM())
			{
				player.setOverrideCond(player.getVariables().getLong(COND_OVERRIDE_KEY, PlayerCondOverride.getAllExceptionsMask()));
			}
			
			player.setOnlineStatus(true, false);
			
			PlayerAutoSaveTaskManager.getInstance().add(player);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed loading character.", e);
		}
		return player;
	}
	
	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			
			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}
		return _forumMail;
	}
	
	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}
	
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			
			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}
		return _forumMemo;
	}
	
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}
	
	/**
	 * Restores sub-class data for the PlayerInstance, used to check the current class index for the character.
	 * @param player
	 * @return
	 */
	private static boolean restoreSubClassData(PlayerInstance player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_SUBCLASSES))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final SubClassHolder subClass = new SubClassHolder();
					subClass.setClassId(rs.getInt("class_id"));
					subClass.setLevel(rs.getByte("level"));
					subClass.setExp(rs.getLong("exp"));
					subClass.setSp(rs.getLong("sp"));
					subClass.setClassIndex(rs.getInt("class_index"));
					
					// Enforce the correct indexing of _subClasses against their class indexes.
					player.getSubClasses().put(subClass.getClassIndex(), subClass);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore classes for " + player.getName() + ": " + e.getMessage(), e);
		}
		return true;
	}
	
	/**
	 * Restores:
	 * <ul>
	 * <li>Skills</li>
	 * <li>Macros</li>
	 * <li>Short-cuts</li>
	 * <li>Henna</li>
	 * <li>Teleport Bookmark</li>
	 * <li>Recipe Book</li>
	 * <li>Recipe Shop List (If configuration enabled)</li>
	 * <li>Premium Item List</li>
	 * <li>Pet Inventory Items</li>
	 * </ul>
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this PlayerInstance and add them to _skills.
		restoreSkills();
		
		// Retrieve from the database all macroses of this PlayerInstance and add them to _macros.
		_macros.restoreMe();
		
		// Retrieve from the database all shortCuts of this PlayerInstance and add them to _shortCuts.
		_shortCuts.restoreMe();
		
		// Retrieve from the database all henna of this PlayerInstance and add them to _henna.
		restoreHenna();
		
		// Retrieve from the database all teleport bookmark of this PlayerInstance and add them to _tpbookmark.
		restoreTeleportBookmark();
		
		// Retrieve from the database the recipe book of this PlayerInstance.
		restoreRecipeBook(true);
		
		// Restore Recipe Shop list.
		if (Config.STORE_RECIPE_SHOPLIST)
		{
			restoreRecipeShopList();
		}
		
		// Load Premium Item List.
		loadPremiumItemList();
		
		// Restore items in pet inventory.
		restorePetInventoryItems();
	}
	
	/**
	 * Restore recipe book data for this PlayerInstance.
	 * @param loadCommon
	 */
	private void restoreRecipeBook(boolean loadCommon)
	{
		final String sql = loadCommon ? "SELECT id, type, classIndex FROM character_recipebook WHERE charId=?" : "SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, getObjectId());
			if (!loadCommon)
			{
				ps.setInt(2, _classIndex);
			}
			
			try (ResultSet rs = ps.executeQuery())
			{
				_dwarvenRecipeBook.clear();
				
				RecipeList recipe;
				final RecipeData rd = RecipeData.getInstance();
				while (rs.next())
				{
					recipe = rd.getRecipeList(rs.getInt("id"));
					if (loadCommon)
					{
						if (rs.getInt(2) == 1)
						{
							if (rs.getInt(3) == _classIndex)
							{
								registerDwarvenRecipeList(recipe, false);
							}
						}
						else
						{
							registerCommonRecipeList(recipe, false);
						}
					}
					else
					{
						registerDwarvenRecipeList(recipe, false);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore recipe book data:" + e.getMessage(), e);
		}
	}
	
	public Map<Integer, PremiumItem> getPremiumItemList()
	{
		return _premiumItems;
	}
	
	private void loadPremiumItemList()
	{
		final String sql = "SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=?";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_premiumItems.put(rs.getInt("itemNum"), new PremiumItem(rs.getInt("itemId"), rs.getLong("itemCount"), rs.getString("itemSender")));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore premium items: " + e.getMessage(), e);
		}
	}
	
	public void updatePremiumItem(int itemNum, long newcount)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE character_premium_items SET itemCount=? WHERE charId=? AND itemNum=? "))
		{
			ps.setLong(1, newcount);
			ps.setInt(2, getObjectId());
			ps.setInt(3, itemNum);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not update premium items: " + e.getMessage(), e);
		}
	}
	
	public void deletePremiumItem(int itemNum)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_premium_items WHERE charId=? AND itemNum=? "))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, itemNum);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not delete premium item: " + e);
		}
	}
	
	/**
	 * Update PlayerInstance stats in the characters table of the database.
	 * @param storeActiveEffects
	 */
	public synchronized void store(boolean storeActiveEffects)
	{
		storeCharBase();
		storeCharSub();
		storeEffect(storeActiveEffects);
		storeItemReuseDelay();
		if (Config.STORE_RECIPE_SHOPLIST)
		{
			storeRecipeShopList();
		}
		if (Config.STORE_UI_SETTINGS)
		{
			storeUISettings();
		}
		SevenSigns.getInstance().saveSevenSignsData(getObjectId());
		
		final PlayerVariables vars = getScript(PlayerVariables.class);
		if (vars != null)
		{
			vars.storeMe();
		}
		
		final AccountVariables aVars = getScript(AccountVariables.class);
		if (aVars != null)
		{
			aVars.storeMe();
		}
		
		getInventory().updateDatabase();
		getWarehouse().updateDatabase();
		getFreight().updateDatabase();
	}
	
	@Override
	public void storeMe()
	{
		store(true);
	}
	
	private void storeCharBase()
	{
		// Get the exp, level, and sp of base class to store in base table
		final long exp = getStat().getBaseExp();
		final int level = getStat().getBaseLevel();
		final long sp = getStat().getBaseSp();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER))
		{
			ps.setInt(1, level);
			ps.setInt(2, getMaxHp());
			ps.setDouble(3, getCurrentHp());
			ps.setInt(4, getMaxCp());
			ps.setDouble(5, getCurrentCp());
			ps.setInt(6, getMaxMp());
			ps.setDouble(7, getCurrentMp());
			ps.setInt(8, _appearance.getFace());
			ps.setInt(9, _appearance.getHairStyle());
			ps.setInt(10, _appearance.getHairColor());
			ps.setInt(11, _appearance.isFemale() ? 1 : 0);
			ps.setInt(12, getHeading());
			ps.setInt(13, _observerMode ? _lastLoc.getX() : getX());
			ps.setInt(14, _observerMode ? _lastLoc.getY() : getY());
			ps.setInt(15, _observerMode ? _lastLoc.getZ() : getZ());
			ps.setLong(16, exp);
			ps.setLong(17, _expBeforeDeath);
			ps.setLong(18, sp);
			ps.setInt(19, getKarma());
			ps.setInt(20, _fame);
			ps.setInt(21, _pvpKills);
			ps.setInt(22, _pkKills);
			ps.setInt(23, _clanId);
			ps.setInt(24, getRace().ordinal());
			ps.setInt(25, getClassId().getId());
			ps.setLong(26, _deleteTimer);
			ps.setString(27, getTitle());
			ps.setInt(28, _appearance.getTitleColor());
			ps.setInt(29, getAccessLevel().getLevel());
			ps.setInt(30, isOnlineInt());
			ps.setInt(31, _isIn7sDungeon ? 1 : 0);
			ps.setInt(32, _clanPrivileges.getBitmask());
			ps.setInt(33, _wantsPeace);
			ps.setInt(34, _baseClass);
			long totalOnlineTime = _onlineTime;
			if (_onlineBeginTime > 0)
			{
				totalOnlineTime += (Chronos.currentTimeMillis() - _onlineBeginTime) / 1000;
			}
			ps.setLong(35, _offlineShopStart > 0 ? _onlineTime : totalOnlineTime);
			ps.setInt(36, _newbie);
			ps.setInt(37, _noble ? 1 : 0);
			ps.setInt(38, _powerGrade);
			ps.setInt(39, _pledgeType);
			ps.setInt(40, _lvlJoinedAcademy);
			ps.setLong(41, _apprentice);
			ps.setLong(42, _sponsor);
			ps.setLong(43, _clanJoinExpiryTime);
			ps.setLong(44, _clanCreateExpiryTime);
			ps.setString(45, getName());
			ps.setLong(46, _deathPenaltyBuffLevel);
			ps.setInt(47, _bookmarkSlot);
			ps.setInt(48, getVitalityPoints());
			ps.setString(49, _lang);
			int factionId = 0;
			if (_isGood)
			{
				factionId = 1;
			}
			if (_isEvil)
			{
				factionId = 2;
			}
			ps.setInt(50, factionId);
			ps.setInt(51, getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char base data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	private void storeCharSub()
	{
		if (getTotalSubClasses() <= 0)
		{
			return;
		}
		
		// TODO(Zoey76): Refactor this to use batch.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_CHAR_SUBCLASS))
		{
			for (SubClassHolder subClass : getSubClasses().values())
			{
				ps.setLong(1, subClass.getExp());
				ps.setLong(2, subClass.getSp());
				ps.setInt(3, subClass.getLevel());
				ps.setInt(4, subClass.getClassId());
				ps.setInt(5, getObjectId());
				ps.setInt(6, subClass.getClassIndex());
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store sub class data for " + getName() + ": " + e.getMessage(), e);
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Delete all current stored effects for char to avoid dupe
			try (PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				delete.setInt(1, getObjectId());
				delete.setInt(2, _classIndex);
				delete.execute();
			}
			
			int buffIndex = 0;
			final List<Integer> storedSkills = new ArrayList<>();
			final long currentTime = Chronos.currentTimeMillis();
			
			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			try (PreparedStatement statement = con.prepareStatement(ADD_SKILL_SAVE))
			{
				if (storeEffects)
				{
					for (BuffInfo info : getEffectList().getEffects())
					{
						if (info == null)
						{
							continue;
						}
						
						final Skill skill = info.getSkill();
						// Do not save heals.
						if (skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
						{
							continue;
						}
						
						if (skill.isToggle())
						{
							continue;
						}
						
						// Dances and songs are not kept in retail.
						if (skill.isDance() && !Config.ALT_STORE_DANCES)
						{
							continue;
						}
						
						if (storedSkills.contains(skill.getReuseHashCode()))
						{
							continue;
						}
						
						storedSkills.add(skill.getReuseHashCode());
						
						statement.setInt(1, getObjectId());
						statement.setInt(2, skill.getId());
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, info.getTime());
						
						final TimeStamp t = getSkillReuseTimeStamp(skill.getReuseHashCode());
						statement.setLong(5, (t != null) && (currentTime < t.getStamp()) ? t.getReuse() : 0);
						statement.setLong(6, (t != null) && (currentTime < t.getStamp()) ? t.getStamp() : 0);
						statement.setInt(7, 0); // Store type 0, active buffs/debuffs.
						statement.setInt(8, _classIndex);
						statement.setInt(9, ++buffIndex);
						statement.addBatch();
					}
				}
				
				// Skills under reuse.
				for (Entry<Integer, TimeStamp> ts : getSkillReuseTimeStamps().entrySet())
				{
					final int hash = ts.getKey();
					if (storedSkills.contains(hash))
					{
						continue;
					}
					
					final TimeStamp t = ts.getValue();
					if ((t != null) && (currentTime < t.getStamp()))
					{
						storedSkills.add(hash);
						
						statement.setInt(1, getObjectId());
						statement.setInt(2, t.getSkillId());
						statement.setInt(3, t.getSkillLevel());
						statement.setInt(4, -1);
						statement.setLong(5, t.getReuse());
						statement.setLong(6, t.getStamp());
						statement.setInt(7, 1); // Restore type 1, skill reuse.
						statement.setInt(8, _classIndex);
						statement.setInt(9, ++buffIndex);
						statement.addBatch();
					}
				}
				
				statement.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char effect data: ", e);
		}
	}
	
	private void storeItemReuseDelay()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);
			PreparedStatement ps2 = con.prepareStatement(ADD_ITEM_REUSE_SAVE))
		{
			ps1.setInt(1, getObjectId());
			ps1.execute();
			
			final long currentTime = Chronos.currentTimeMillis();
			for (TimeStamp ts : getItemReuseTimeStamps().values())
			{
				if ((ts != null) && (currentTime < ts.getStamp()))
				{
					ps2.setInt(1, getObjectId());
					ps2.setInt(2, ts.getItemId());
					ps2.setInt(3, ts.getItemObjectId());
					ps2.setLong(4, ts.getReuse());
					ps2.setLong(5, ts.getStamp());
					ps2.addBatch();
				}
			}
			ps2.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char item reuse data: ", e);
		}
	}
	
	/**
	 * @return True if the PlayerInstance is on line.
	 */
	public boolean isOnline()
	{
		return _isOnline;
	}
	
	public int isOnlineInt()
	{
		if (_isOnline && (_client != null))
		{
			return _client.isDetached() ? 2 : 1;
		}
		return 0;
	}
	
	/**
	 * Verifies if the player is in offline mode.<br>
	 * The offline mode may happen for different reasons:<br>
	 * Abnormally: Player gets abruptly disconnected from server.<br>
	 * Normally: The player gets into offline shop mode, only available by enabling the offline shop mod.
	 * @return {@code true} if the player is in offline mode, {@code false} otherwise
	 */
	public boolean isInOfflineMode()
	{
		return (_client == null) || _client.isDetached();
	}
	
	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}
	
	@Override
	public Skill addSkill(Skill newSkill)
	{
		addCustomSkill(newSkill);
		return super.addSkill(newSkill);
	}
	
	/**
	 * Add a skill to the PlayerInstance _skills and its Func objects to the calculator set of the PlayerInstance and save update in the character_skills table of the database. <b><u>Concept</u>:</b> All skills own by a PlayerInstance are identified in <b>_skills</b> <b><u> Actions</u>:</b>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of Creature calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the Creature</li><br>
	 * @param newSkill The Skill to add to the Creature
	 * @param store
	 * @return The Skill replaced or null if just added a new Skill
	 */
	public Skill addSkill(Skill newSkill, boolean store)
	{
		// Add a skill to the PlayerInstance _skills and its Func objects to the calculator set of the PlayerInstance
		final Skill oldSkill = addSkill(newSkill);
		// Add or update a PlayerInstance skill in the character_skills table of the database
		if (store)
		{
			storeSkill(newSkill, oldSkill, -1);
		}
		return oldSkill;
	}
	
	@Override
	public Skill removeSkill(Skill skill, boolean store)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, true);
	}
	
	public Skill removeSkill(Skill skill, boolean store, boolean cancelEffect)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, cancelEffect);
	}
	
	/**
	 * Remove a skill from the Creature and its Func objects from calculator set of the Creature and save update in the character_skills table of the database. <b><u>Concept</u>:</b> All skills own by a Creature are identified in <b>_skills</b> <b><u> Actions</u>:</b>
	 * <li>Remove the skill from the Creature _skills</li>
	 * <li>Remove all its Func objects from the Creature calculator set</li> <b><u> Overridden in</u>:</b>
	 * <li>PlayerInstance : Save update in the character_skills table of the database</li><br>
	 * @param skill The Skill to remove from the Creature
	 * @return The Skill removed
	 */
	public Skill removeSkill(Skill skill)
	{
		removeCustomSkill(skill);
		// Remove a skill from the Creature and its Func objects from calculator set of the Creature
		final Skill oldSkill = super.removeSkill(skill, true);
		if (oldSkill != null)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_SKILL_FROM_CHAR))
			{
				// Remove or update a PlayerInstance skill from the character_skills table of the database
				ps.setInt(1, oldSkill.getId());
				ps.setInt(2, getObjectId());
				ps.setInt(3, _classIndex);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Error could not delete skill: " + e.getMessage(), e);
			}
		}
		
		if ((getTransformationId() > 0) || isCursedWeaponEquipped())
		{
			return oldSkill;
		}
		
		if (skill != null)
		{
			for (Shortcut sc : _shortCuts.getAllShortCuts())
			{
				if ((sc != null) && (sc.getId() == skill.getId()) && (sc.getType() == ShortcutType.SKILL) && ((skill.getId() < 3080) || (skill.getId() > 3259)))
				{
					deleteShortCut(sc.getSlot(), sc.getPage());
				}
			}
		}
		return oldSkill;
	}
	
	/**
	 * Add or update a PlayerInstance skill in the character_skills table of the database.<br>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 * @param newSkill
	 * @param oldSkill
	 * @param newClassIndex
	 */
	private void storeSkill(Skill newSkill, Skill oldSkill, int newClassIndex)
	{
		final int classIndex = (newClassIndex > -1) ? newClassIndex : _classIndex;
		try (Connection con = DatabaseFactory.getConnection())
		{
			if ((oldSkill != null) && (newSkill != null))
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL))
				{
					ps.setInt(1, newSkill.getLevel());
					ps.setInt(2, oldSkill.getId());
					ps.setInt(3, getObjectId());
					ps.setInt(4, classIndex);
					ps.execute();
				}
			}
			else if (newSkill != null)
			{
				try (PreparedStatement ps = con.prepareStatement(ADD_NEW_SKILLS))
				{
					ps.setInt(1, getObjectId());
					ps.setInt(2, newSkill.getId());
					ps.setInt(3, newSkill.getLevel());
					ps.setInt(4, classIndex);
					ps.execute();
				}
			}
			// else
			// {
			// LOGGER.warning("Could not store new skill, it's null!");
			// }
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error could not store char skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Adds or updates player's skills in the database.
	 * @param newSkills the list of skills to store
	 * @param newClassIndex if newClassIndex > -1, the skills will be stored for that class index, not the current one
	 */
	private void storeSkills(List<Skill> newSkills, int newClassIndex)
	{
		if (newSkills.isEmpty())
		{
			return;
		}
		
		final int classIndex = (newClassIndex > -1) ? newClassIndex : _classIndex;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(ADD_NEW_SKILLS))
		{
			for (Skill addSkill : newSkills)
			{
				ps.setInt(1, getObjectId());
				ps.setInt(2, addSkill.getId());
				ps.setInt(3, addSkill.getLevel());
				ps.setInt(4, classIndex);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error could not store char skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all skills of this PlayerInstance and add them to _skills.
	 */
	private void restoreSkills()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR))
		{
			// Retrieve all skills of this PlayerInstance from the database
			ps.setInt(1, getObjectId());
			ps.setInt(2, _classIndex);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int id = rs.getInt("skill_id");
					final int level = rs.getInt("skill_level");
					
					// Create a Skill object for each record
					final Skill skill = SkillData.getInstance().getSkill(id, level);
					if (skill == null)
					{
						LOGGER.warning("Skipped null skill Id: " + id + " Level: " + level + " while restoring player skills for playerObjId: " + getObjectId());
						continue;
					}
					
					// Add the Skill object to the Creature _skills and its Func objects to the calculator set of the Creature
					addSkill(skill);
					
					if (Config.SKILL_CHECK_ENABLE && (!canOverrideCond(PlayerCondOverride.SKILL_CONDITIONS) || Config.SKILL_CHECK_GM) && !SkillTreeData.getInstance().isSkillAllowed(this, skill))
					{
						Util.handleIllegalPlayerAction(this, "Player " + getName() + " has invalid skill " + skill.getName() + " (" + skill.getId() + "/" + skill.getLevel() + "), class:" + ClassListData.getInstance().getClass(getClassId()).getClassName(), IllegalActionPunishmentType.BROADCAST);
						if (Config.SKILL_CHECK_REMOVE)
						{
							removeSkill(skill);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore character " + this + " skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all skill effects of this PlayerInstance and add them to the player.
	 */
	@Override
	public void restoreEffects()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_SKILL_SAVE))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, _classIndex);
			try (ResultSet rs = ps.executeQuery())
			{
				final long currentTime = Chronos.currentTimeMillis();
				while (rs.next())
				{
					final int remainingTime = rs.getInt("remaining_time");
					final long reuseDelay = rs.getLong("reuse_delay");
					final long systime = rs.getLong("systime");
					final int restoreType = rs.getInt("restore_type");
					final Skill skill = SkillData.getInstance().getSkill(rs.getInt("skill_id"), rs.getInt("skill_level"));
					if (skill == null)
					{
						continue;
					}
					
					final long time = systime - currentTime;
					if (time > 10)
					{
						disableSkill(skill, time);
						addTimeStamp(skill, reuseDelay, systime);
					}
					
					// Restore Type 1 The remaning skills lost effect upon logout but were still under a high reuse delay.
					if (restoreType > 0)
					{
						continue;
					}
					
					// Restore Type 0 These skill were still in effect on the character upon logout.
					// Some of which were self casted and might still have had a long reuse delay which also is restored.
					skill.applyEffects(this, this, false, remainingTime);
				}
			}
			// Remove previously restored skills
			try (PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				delete.setInt(1, getObjectId());
				delete.setInt(2, _classIndex);
				delete.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all Item Reuse Time of this PlayerInstance and add them to the player.
	 */
	private void restoreItemReuse()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_ITEM_REUSE_SAVE);
			PreparedStatement delete = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);)
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				int itemId;
				long reuseDelay;
				long systime;
				boolean isInInventory;
				long remainingTime;
				final long currentTime = Chronos.currentTimeMillis();
				while (rs.next())
				{
					itemId = rs.getInt("itemId");
					reuseDelay = rs.getLong("reuseDelay");
					systime = rs.getLong("systime");
					isInInventory = true;
					
					// Using item Id
					ItemInstance item = _inventory.getItemByItemId(itemId);
					if (item == null)
					{
						item = getWarehouse().getItemByItemId(itemId);
						isInInventory = false;
					}
					
					if ((item != null) && (item.getId() == itemId) && (item.getReuseDelay() > 0))
					{
						remainingTime = systime - currentTime;
						if (remainingTime > 10)
						{
							addTimeStampItem(item, reuseDelay, systime);
							if (isInInventory && item.isEtcItem())
							{
								final int group = item.getSharedReuseGroup();
								if (group > 0)
								{
									sendPacket(new ExUseSharedGroupItem(itemId, group, (int) remainingTime, (int) reuseDelay));
								}
							}
						}
					}
				}
			}
			
			// Delete item reuse.
			delete.setInt(1, getObjectId());
			delete.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " Item Reuse data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all Henna of this PlayerInstance, add them to _henna and calculate stats of the PlayerInstance.
	 */
	private void restoreHenna()
	{
		for (int i = 0; i < 3; i++)
		{
			_henna[i] = null;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_HENNAS))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, _classIndex);
			try (ResultSet rs = ps.executeQuery())
			{
				int slot;
				int symbolId;
				while (rs.next())
				{
					slot = rs.getInt("slot");
					if ((slot < 1) || (slot > 3))
					{
						continue;
					}
					
					symbolId = rs.getInt("symbol_id");
					if (symbolId == 0)
					{
						continue;
					}
					_henna[slot - 1] = HennaData.getInstance().getHenna(symbolId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed restoing character " + this + " hennas.", e);
		}
		
		// Calculate henna modifiers of this player.
		recalcHennaStats();
	}
	
	/**
	 * @return the number of Henna empty slot of the PlayerInstance.
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 0;
		if (getClassId().level() == 1)
		{
			totalSlots = 2;
		}
		else if (getClassId().level() > 1)
		{
			totalSlots = 3;
		}
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
			{
				totalSlots--;
			}
		}
		
		if (totalSlots <= 0)
		{
			return 0;
		}
		
		return totalSlots;
	}
	
	/**
	 * Remove a Henna of the PlayerInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this PlayerInstance.
	 * @param slot
	 * @return
	 */
	public boolean removeHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return false;
		}
		
		final Henna henna = _henna[slot - 1];
		if (henna == null)
		{
			return false;
		}
		
		_henna[slot - 1] = null;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot);
			statement.setInt(3, _classIndex);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed removing character henna.", e);
		}
		
		// Calculate Henna modifiers of this PlayerInstance
		recalcHennaStats();
		
		// Send Server->Client HennaInfo packet to this PlayerInstance
		sendPacket(new HennaInfo(this));
		
		// Send Server->Client UserInfo packet to this PlayerInstance
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		// Add the recovered dyes to the player's inventory and notify them.
		_inventory.addItem("Henna", henna.getDyeItemId(), henna.getCancelCount(), this, null);
		reduceAdena("Henna", henna.getCancelFee(), this, false);
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
		sm.addItemName(henna.getDyeItemId());
		sm.addLong(henna.getCancelCount());
		sendPacket(sm);
		sendPacket(SystemMessageId.THE_SYMBOL_HAS_BEEN_DELETED);
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(this, henna), this);
		return true;
	}
	
	/**
	 * Add a Henna to the PlayerInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this PlayerInstance.
	 * @param henna the henna to add to the player.
	 * @return {@code true} if the henna is added to the player, {@code false} otherwise.
	 */
	public boolean addHenna(Henna henna)
	{
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;
				
				// Calculate Henna modifiers of this PlayerInstance
				recalcHennaStats();
				
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement ps = con.prepareStatement(ADD_CHAR_HENNA))
				{
					ps.setInt(1, getObjectId());
					ps.setInt(2, henna.getDyeId());
					ps.setInt(3, i + 1);
					ps.setInt(4, _classIndex);
					ps.execute();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, "Failed saving character henna.", e);
				}
				
				// Send Server->Client HennaInfo packet to this PlayerInstance
				sendPacket(new HennaInfo(this));
				
				// Send Server->Client UserInfo packet to this PlayerInstance
				sendPacket(new UserInfo(this));
				sendPacket(new ExBrExtraUserInfo(this));
				
				// Notify to scripts
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(this, henna), this);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Calculate Henna modifiers of this PlayerInstance.
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;
		for (Henna h : _henna)
		{
			if (h == null)
			{
				continue;
			}
			
			_hennaINT += ((_hennaINT + h.getStatINT()) > 5) ? 5 - _hennaINT : h.getStatINT();
			_hennaSTR += ((_hennaSTR + h.getStatSTR()) > 5) ? 5 - _hennaSTR : h.getStatSTR();
			_hennaMEN += ((_hennaMEN + h.getStatMEN()) > 5) ? 5 - _hennaMEN : h.getStatMEN();
			_hennaCON += ((_hennaCON + h.getStatCON()) > 5) ? 5 - _hennaCON : h.getStatCON();
			_hennaWIT += ((_hennaWIT + h.getStatWIT()) > 5) ? 5 - _hennaWIT : h.getStatWIT();
			_hennaDEX += ((_hennaDEX + h.getStatDEX()) > 5) ? 5 - _hennaDEX : h.getStatDEX();
		}
	}
	
	/**
	 * @param slot the character inventory henna slot.
	 * @return the Henna of this PlayerInstance corresponding to the selected slot.
	 */
	public Henna getHenna(int slot)
	{
		return (slot < 1) || (slot > 3) ? null : _henna[slot - 1];
	}
	
	/**
	 * @return {@code true} if player has at least 1 henna symbol, {@code false} otherwise.
	 */
	public boolean hasHennas()
	{
		for (Henna henna : _henna)
		{
			if (henna != null)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return the henna holder for this player.
	 */
	public Henna[] getHennaList()
	{
		return _henna;
	}
	
	/**
	 * @return the INT Henna modifier of this PlayerInstance.
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}
	
	/**
	 * @return the STR Henna modifier of this PlayerInstance.
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}
	
	/**
	 * @return the CON Henna modifier of this PlayerInstance.
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}
	
	/**
	 * @return the MEN Henna modifier of this PlayerInstance.
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}
	
	/**
	 * @return the WIT Henna modifier of this PlayerInstance.
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}
	
	/**
	 * @return the DEX Henna modifier of this PlayerInstance.
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}
	
	public void autoSave()
	{
		storeMe();
		storeRecommendations(false);
		
		if (Config.UPDATE_ITEMS_ON_CHAR_STORE)
		{
			getInventory().updateDatabase();
			getWarehouse().updateDatabase();
			getFreight().updateDatabase();
		}
	}
	
	public boolean canLogout()
	{
		if (_subclassLock)
		{
			LOGGER.warning("Player " + getName() + " tried to restart/logout during class change.");
			return false;
		}
		
		if ((_activeEnchantItemId != ID_NONE) || (_activeEnchantAttrItemId != ID_NONE))
		{
			return false;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this) && !(isGM() && Config.GM_RESTART_FIGHTING))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_EXIT_THE_GAME_WHILE_IN_COMBAT);
			return false;
		}
		
		if (isRegisteredOnEvent())
		{
			sendMessage("A superior power doesn't allow you to leave.");
			return false;
		}
		
		// Prevent player from logging out if they are a festival participant
		// and it is in progress, otherwise notify party members that the player
		// is not longer a participant.
		if (isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				sendMessage("You cannot log out while you are a participant in a Festival.");
				return false;
			}
			
			if (isInParty())
			{
				_party.broadcastPacket(new SystemMessage(getName() + " has been removed from the upcoming Festival."));
			}
		}
		
		return true;
	}
	
	/**
	 * Return True if the PlayerInstance is autoAttackable.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Check if the attacker isn't the PlayerInstance Pet</li>
	 * <li>Check if the attacker is MonsterInstance</li>
	 * <li>If the attacker is a PlayerInstance, check if it is not in the same party</li>
	 * <li>Check if the PlayerInstance has Karma</li>
	 * <li>If the attacker is a PlayerInstance, check if it is not in the same siege clan (Attacker, Defender)</li>
	 * </ul>
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker == null)
		{
			return false;
		}
		
		// Check if the attacker isn't the PlayerInstance Pet
		if ((attacker == this) || (attacker == _summon))
		{
			return false;
		}
		
		// Friendly mobs doesnt attack players
		if (attacker instanceof FriendlyMobInstance)
		{
			return false;
		}
		
		// Check if the attacker is a MonsterInstance
		if (attacker.isMonster())
		{
			return true;
		}
		
		// is AutoAttackable if both players are in the same duel and the duel is still going on
		if (attacker.isPlayable() && (_duelState == Duel.DUELSTATE_DUELLING) && (getDuelId() == attacker.getActingPlayer().getDuelId()))
		{
			return true;
		}
		
		// Check if the attacker is not in the same party. NOTE: Party checks goes before oly checks in order to prevent patry member autoattack at oly.
		if (isInParty() && _party.getMembers().contains(attacker))
		{
			return false;
		}
		
		// Check if the attacker is in olympia and olympia start
		if (attacker.isPlayer() && attacker.getActingPlayer().isInOlympiadMode())
		{
			return _inOlympiadMode && _olympiadStart && (((PlayerInstance) attacker).getOlympiadGameId() == getOlympiadGameId());
		}
		
		// Check if the attacker is in an event
		if (isOnEvent())
		{
			return isOnSoloEvent() || (getTeam() != attacker.getTeam());
		}
		
		// Check if the attacker is a Playable
		if (attacker.isPlayable())
		{
			if (isInsideZone(ZoneId.PEACE))
			{
				return false;
			}
			
			// Get PlayerInstance
			final PlayerInstance attackerPlayer = attacker.getActingPlayer();
			if (_clan != null)
			{
				final Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the PlayerInstance aren't in the Defender clan
					if (siege.checkIsDefender(attackerPlayer.getClan()) && siege.checkIsDefender(getClan()))
					{
						return false;
					}
					
					// Check if a siege is in progress and if attacker and the PlayerInstance aren't in the Attacker clan
					if (siege.checkIsAttacker(attackerPlayer.getClan()) && siege.checkIsAttacker(getClan()))
					{
						return false;
					}
				}
				
				// Check if clan is at war
				if ((getClan() != null) && (attackerPlayer.getClan() != null) && getClan().isAtWarWith(attackerPlayer.getClanId()) && attackerPlayer.getClan().isAtWarWith(getClanId()) && (getWantsPeace() == 0) && (attackerPlayer.getWantsPeace() == 0) && !isAcademyMember())
				{
					return true;
				}
			}
			
			// Check if the PlayerInstance is in an arena, but NOT siege zone. NOTE: This check comes before clan/ally checks, but after party checks.
			// This is done because in arenas, clan/ally members can autoattack if they arent in party.
			if ((isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP)) && !(isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE)))
			{
				return true;
			}
			
			// Check if the attacker is not in the same clan
			if ((_clan != null) && _clan.isMember(attacker.getObjectId()))
			{
				return false;
			}
			
			// Check if the attacker is not in the same ally
			if (attacker.isPlayer() && (getAllyId() != 0) && (getAllyId() == attackerPlayer.getAllyId()))
			{
				return false;
			}
			
			// Now check again if the PlayerInstance is in pvp zone, but this time at siege PvP zone, applying clan/ally checks
			if ((isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP)) && (isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE)))
			{
				return true;
			}
			
			if (Config.FACTION_SYSTEM_ENABLED && ((isGood() && attackerPlayer.isEvil()) || (isEvil() && attackerPlayer.isGood())))
			{
				return true;
			}
		}
		
		if ((attacker instanceof DefenderInstance) && (_clan != null))
		{
			final Siege siege = SiegeManager.getInstance().getSiege(this);
			return (siege != null) && siege.checkIsAttacker(_clan);
		}
		
		if (attacker instanceof GuardInstance)
		{
			if (Config.FACTION_SYSTEM_ENABLED && Config.FACTION_GUARDS_ENABLED && ((_isGood && ((Npc) attacker).getTemplate().isClan(Config.FACTION_EVIL_TEAM_NAME)) || (_isEvil && ((Npc) attacker).getTemplate().isClan(Config.FACTION_GOOD_TEAM_NAME))))
			{
				return true;
			}
			return (getKarma() > 0); // Guards attack only PK players.
		}
		
		// Check if the PlayerInstance has Karma
		if ((getKarma() > 0) || (_pvpFlag > 0))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check if the active Skill can be casted.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Check if the skill isn't toggle and is offensive</li>
	 * <li>Check if the target is in the skill cast range</li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled</li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li>
	 * <li>Check if the caster isn't sitting</li>
	 * <li>Check if all skills are enabled and this skill is enabled</li>
	 * <li>Check if the caster own the weapon needed</li>
	 * <li>Check if the skill is active</li>
	 * <li>Check if all casting conditions are completed</li>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li>
	 * </ul>
	 * @param skill The Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 */
	@Override
	public boolean useMagic(Skill skill, boolean forceUse, boolean dontMove)
	{
		// Check if the skill is active
		if (skill.isPassive())
		{
			// just ignore the passive skill request. why does the client send it anyway ??
			// Send a Server->Client packet ActionFailed to the PlayerInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// ************************************* Check Casting in Progress *******************************************
		
		// If a skill is currently being used, queue this one if this is not the same
		if (isCastingNow())
		{
			// Check if new skill different from current skill in progress
			if ((_currentSkill != null) && (skill.getId() == _currentSkill.getSkillId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (isSkillDisabled(skill))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Create a new SkillUseHolder object and queue it in the player _queuedSkill
			setQueuedSkill(skill, forceUse, dontMove);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		setCastingNow(true);
		// Create a new SkillUseHolder object and set the player _currentSkill
		// This is used mainly to save & queue the button presses, since Creature has
		// _lastSkillCast which could otherwise replace it
		setCurrentSkill(skill, forceUse, dontMove);
		if (_queuedSkill != null)
		{
			setQueuedSkill(null, false, false);
		}
		
		if (!checkUseMagicConditions(skill, forceUse, dontMove))
		{
			setCastingNow(false);
			return false;
		}
		
		// Check if the target is correct and Notify the AI with AI_INTENTION_CAST and target
		WorldObject target = null;
		switch (skill.getTargetType())
		{
			case AURA: // AURA, SELF should be cast even if no target has been found
			case FRONT_AURA:
			case BEHIND_AURA:
			case GROUND:
			case SELF:
			case AURA_CORPSE_MOB:
			case COMMAND_CHANNEL:
			case AURA_FRIENDLY:
			{
				target = this;
				break;
			}
			default:
			{
				// Get the first target of the list
				target = skill.getFirstOfTargetList(this);
				break;
			}
		}
		
		// Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
		return true;
	}
	
	private boolean checkUseMagicConditions(Skill skill, boolean forceUse, boolean dontMove)
	{
		// ************************************* Check Player State *******************************************
		
		// Abnormal effects(ex : Stun, Sleep...) are checked in Creature useMagic()
		if (isOutOfControl() || isParalyzed() || isStunned() || isSleeping())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the player is dead
		if (isDead())
		{
			// Send a Server->Client packet ActionFailed to the PlayerInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (_fishing && !skill.hasEffectType(EffectType.FISHING, EffectType.FISHING_START))
		{
			// Only fishing skills are available
			sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_MAY_BE_USED_AT_THIS_TIME);
			return false;
		}
		
		if (_observerMode)
		{
			sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster is sitting
		if (_waitTypeSitting)
		{
			// Send a System Message to the caster
			sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
			
			// Send a Server->Client packet ActionFailed to the PlayerInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the skill type is toggle.
		if (skill.isToggle() && isAffectedBySkill(skill.getId()))
		{
			stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the player uses "Fake Death" skill
		// Note: do not check this before TOGGLE reset
		if (_isFakeDeath)
		{
			// Send a Server->Client packet ActionFailed to the PlayerInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// ************************************* Check Target *******************************************
		// Create and set a WorldObject containing the target of the skill
		WorldObject target = null;
		final TargetType sklTargetType = skill.getTargetType();
		if ((sklTargetType == TargetType.GROUND) && (_currentSkillWorldPosition == null))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		switch (sklTargetType)
		{
			// Target the player if skill type is AURA, PARTY, CLAN or SELF
			case AURA:
			case FRONT_AURA:
			case BEHIND_AURA:
			case PARTY:
			case CLAN:
			case PARTY_CLAN:
			case GROUND:
			case SELF:
			case AREA_SUMMON:
			case AURA_CORPSE_MOB:
			case COMMAND_CHANNEL:
			case AURA_FRIENDLY:
			{
				target = this;
				break;
			}
			case PET:
			case SERVITOR:
			case SUMMON:
			{
				target = _summon;
				break;
			}
			default:
			{
				target = getTarget();
				break;
			}
		}
		
		// Check the validity of the target
		if (target == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// skills can be used on Walls and Doors only during siege
		if (target.isDoor())
		{
			final DoorInstance door = (DoorInstance) target;
			if ((door.getCastle() != null) && (door.getCastle().getResidenceId() > 0))
			{
				if (!door.getCastle().getSiege().isInProgress())
				{
					sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
			}
			else if ((door.getFort() != null) && (door.getFort().getResidenceId() > 0) && (!door.getFort().getSiege().isInProgress() || !door.isShowHp()))
			{
				sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
		}
		
		// Are the target and the player in the same duel?
		if (_isInDuel && target.isPlayable())
		{
			// Get PlayerInstance
			final PlayerInstance cha = target.getActingPlayer();
			if (cha.getDuelId() != getDuelId())
			{
				sendMessage("You cannot do this while duelling.");
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// ************************************* Check skill availability *******************************************
		
		// Check if this skill is enabled (ex : reuse time)
		if (isSkillDisabled(skill))
		{
			final SystemMessage sm;
			if (hasSkillReuse(skill.getReuseHashCode()))
			{
				final int remainingTime = (int) (getSkillRemainingReuseTime(skill.getReuseHashCode()) / 1000);
				final int hours = remainingTime / 3600;
				final int minutes = (remainingTime % 3600) / 60;
				final int seconds = remainingTime % 60;
				if (hours > 0)
				{
					sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_HOUR_S_S3_MINUTE_S_AND_S4_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
					sm.addSkillName(skill);
					sm.addInt(hours);
					sm.addInt(minutes);
				}
				else if (minutes > 0)
				{
					sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_MINUTE_S_S3_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
					sm.addSkillName(skill);
					sm.addInt(minutes);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
					sm.addSkillName(skill);
				}
				
				sm.addInt(seconds);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_IS_NOT_AVAILABLE_AT_THIS_TIME_BEING_PREPARED_FOR_REUSE);
				sm.addSkillName(skill);
			}
			
			sendPacket(sm);
			return false;
		}
		
		// ************************************* Check casting conditions *******************************************
		
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
		{
			// Send a Server->Client packet ActionFailed to the PlayerInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// ************************************* Check Skill Type *******************************************
		
		// Check if this is bad magic skill
		if (skill.isBad())
		{
			if (isInsidePeaceZone(this, target) && !getAccessLevel().allowPeaceAttack())
			{
				// If Creature or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (_inOlympiadMode && !_olympiadStart)
			{
				// if PlayerInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (isSiegeFriend(target))
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
				return false;
			}
			
			if (!target.canBeAttacked() && !getAccessLevel().allowPeaceAttack() && !target.isDoor())
			{
				// If target is not attackable, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Check for Event Mob's
			if ((target instanceof EventMonsterInstance) && ((EventMonsterInstance) target).eventSkillAttackBlocked())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse)
			{
				switch (sklTargetType)
				{
					case AURA:
					case FRONT_AURA:
					case BEHIND_AURA:
					case AURA_CORPSE_MOB:
					case CLAN:
					case PARTY:
					case SELF:
					case GROUND:
					case AREA_SUMMON:
					case UNLOCKABLE:
					case AURA_FRIENDLY:
					{
						break;
					}
					default: // Send a Server->Client packet ActionFailed to the PlayerInstance
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
			}
			
			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the PlayerInstance and the target
				if (sklTargetType == TargetType.GROUND)
				{
					if (!isInsideRadius2D(_currentSkillWorldPosition.getX(), _currentSkillWorldPosition.getY(), _currentSkillWorldPosition.getZ(), skill.getCastRange() + getTemplate().getCollisionRadius()))
					{
						// Send a System Message to the caster
						sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
						
						// Send a Server->Client packet ActionFailed to the PlayerInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
				else if ((skill.getCastRange() > 0) && !isInsideRadius2D(target, skill.getCastRange() + getTemplate().getCollisionRadius()))
				{
					// Send a System Message to the caster
					sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
					
					// Send a Server->Client packet ActionFailed to the PlayerInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// Check if the skill is a good magic, target is a monster and if force attack is set, if not then we don't want to cast.
		if ((skill.getEffectPoint() > 0) && target.isMonster() && !forceUse)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
			case PARTY:
			case CLAN: // For such skills, checkPvpSkill() is called from Skill.getTargetList()
			case PARTY_CLAN: // For such skills, checkPvpSkill() is called from Skill.getTargetList()
			case AURA:
			case FRONT_AURA:
			case BEHIND_AURA:
			case AREA_SUMMON:
			case GROUND:
			case SELF:
			{
				break;
			}
			default:
			{
				// Verify that player can attack a player or summon
				if (target.isPlayable() && !getAccessLevel().allowPeaceAttack() && !checkPvpSkill(target, skill))
				{
					// Send a System Message to the player
					sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
					
					// Send a Server->Client packet ActionFailed to the player
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// GeoData Los Check here
		if (skill.getCastRange() > 0)
		{
			if (sklTargetType == TargetType.GROUND)
			{
				if (!GeoEngine.getInstance().canSeeLocation(this, _currentSkillWorldPosition))
				{
					sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (!GeoEngine.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		if ((skill.getFlyType() == FlyType.CHARGE) && !GeoEngine.getInstance().canMoveToTarget(getX(), getY(), getZ(), target.getX(), target.getY(), target.getZ(), getInstanceId()))
		{
			sendPacket(SystemMessageId.THE_TARGET_IS_LOCATED_WHERE_YOU_CANNOT_CHARGE);
			return false;
		}
		
		// finally, after passing all conditions
		return true;
	}
	
	public boolean isInLooterParty(int looterId)
	{
		final PlayerInstance looter = World.getInstance().getPlayer(looterId);
		
		// if PlayerInstance is in a CommandChannel
		if (isInParty() && _party.isInCommandChannel() && (looter != null))
		{
			return _party.getCommandChannel().getMembers().contains(looter);
		}
		return isInParty() && (looter != null) && _party.getMembers().contains(looter);
	}
	
	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target WorldObject instance containing the target
	 * @param skill Skill instance with the skill being casted
	 * @return {@code false} if the skill is a pvpSkill and target is not a valid pvp target, {@code true} otherwise.
	 */
	public boolean checkPvpSkill(WorldObject target, Skill skill)
	{
		if ((skill == null) || (target == null))
		{
			return false;
		}
		
		if (!target.isPlayable())
		{
			return true;
		}
		
		if (skill.isDebuff() || skill.hasEffectType(EffectType.STEAL_ABNORMAL) || skill.isBad())
		{
			final PlayerInstance targetPlayer = target.getActingPlayer();
			if ((targetPlayer == null) || (this == target))
			{
				return false;
			}
			
			// Duel
			if (isInDuel() && targetPlayer.isInDuel() && (getDuelId() == targetPlayer.getDuelId()))
			{
				return true;
			}
			
			// Olympiad
			if (isInOlympiadMode() && targetPlayer.isInOlympiadMode() && (getOlympiadGameId() == targetPlayer.getOlympiadGameId()))
			{
				return true;
			}
			
			final boolean isCtrlPressed = (_currentSkill != null) && _currentSkill.isCtrlPressed();
			
			// Peace Zone
			if (target.isInsideZone(ZoneId.PEACE))
			{
				return false;
			}
			
			// PvP Skills
			if (skill.isPvPOnly() && ((targetPlayer.getPvpFlag() == 0) && (targetPlayer.getKarma() == 0)) && !isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE))
			{
				return false;
			}
			
			// Siege
			if (isSiegeFriend(targetPlayer))
			{
				sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
				return false;
			}
			
			// Party
			if ((isInParty() && targetPlayer.isInParty()) //
				&& ((getParty().getLeader() == targetPlayer.getParty().getLeader()) || ((_party.getCommandChannel() != null) && _party.getCommandChannel().containsPlayer(targetPlayer))))
			{
				return (skill.getEffectRange() > 0) && isCtrlPressed && (getTarget() == target) && skill.isDamage();
			}
			
			// You can debuff anyone except party members while in an arena...
			if (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP))
			{
				return true;
			}
			
			final Clan tClan = targetPlayer.getClan();
			if ((_clan != null) && (tClan != null))
			{
				if (_clan.isAtWarWith(tClan.getId()) && tClan.isAtWarWith(_clan.getId()))
				{
					// Always return true at war
					return true;
				}
				else if ((getClanId() == targetPlayer.getClanId()) || ((getAllyId() > 0) && (getAllyId() == targetPlayer.getAllyId())))
				{
					// Check if skill can do dmg
					return (skill.getEffectRange() > 0) && isCtrlPressed && (getTarget() == target) && skill.isDamage();
				}
			}
			
			// On retail, it is impossible to debuff a "peaceful" player.
			if ((targetPlayer.getPvpFlag() == 0) && (targetPlayer.getKarma() == 0))
			{
				// Check if skill can do dmg
				return (skill.getEffectRange() > 0) && isCtrlPressed && (getTarget() == target) && skill.isDamage();
			}
			
			return (targetPlayer.getPvpFlag() > 0) || (targetPlayer.getKarma() > 0);
		}
		
		return true;
	}
	
	/**
	 * @return True if the PlayerInstance is a Mage.
	 */
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}
	
	public boolean isMounted()
	{
		return _mountType != MountType.NONE;
	}
	
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZoneId.NO_LANDING))
		{
			return true;
		}
		else
		// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
		// he cannot land.
		// castle owner is the leader of the clan that owns the castle where the pc is
		if (isInsideZone(ZoneId.SIEGE) && ((getClan() == null) || (CastleManager.getInstance().getCastle(this) != CastleManager.getInstance().getCastleByOwner(getClan())) || (this != getClan().getLeader().getPlayerInstance())))
		{
			return true;
		}
		return false;
	}
	
	// returns false if the change of mount type fails.
	public void setMount(int npcId, int npcLevel)
	{
		final MountType type = MountType.findByNpcId(npcId);
		switch (type)
		{
			case NONE: // None
			{
				if (isFlying())
				{
					removeSkill(CommonSkill.WYVERN_BREATH.getSkill().getId(), false);
					setFlying(false);
					sendSkillList();
				}
				break;
			}
			case WYVERN: // Wyvern
			{
				setFlying(true);
				addSkill(CommonSkill.WYVERN_BREATH.getSkill(), false);
				sendSkillList();
				break;
			}
		}
		
		_mountType = type;
		_mountNpcId = npcId;
		_mountLevel = npcLevel;
	}
	
	/**
	 * @return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern, 3: Wolf).
	 */
	public MountType getMountType()
	{
		return _mountType;
	}
	
	@Override
	public void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus(2);
	}
	
	@Override
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus(2);
	}
	
	public void stopAllEffectsNotStayOnSubclassChange()
	{
		getEffectList().stopAllEffectsNotStayOnSubclassChange();
		updateAndBroadcastStatus(2);
	}
	
	public void stopCubics()
	{
		if (_cubics.isEmpty())
		{
			return;
		}
		for (CubicInstance cubic : _cubics.values())
		{
			cubic.stopAction();
			cubic.cancelDisappear();
		}
		_cubics.clear();
		broadcastUserInfo();
	}
	
	public void stopCubicsByOthers()
	{
		if (_cubics.isEmpty())
		{
			return;
		}
		boolean broadcast = false;
		for (CubicInstance cubic : _cubics.values())
		{
			if (cubic.givenByOther())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
				_cubics.remove(cubic.getId());
				broadcast = true;
			}
		}
		if (broadcast)
		{
			broadcastUserInfo();
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this PlayerInstance and CharInfo to all PlayerInstance in its _KnownPlayers.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Others PlayerInstance in the detection area of the PlayerInstance are identified in <b>_knownPlayers</b>.<br>
	 * In order to inform other players of this PlayerInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Send a Server->Client packet UserInfo to this PlayerInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all PlayerInstance in _KnownPlayers of the PlayerInstance (Public data only)</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</b></font>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}
	
	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.
	 * @param value
	 */
	public void setInventoryBlockingStatus(boolean value)
	{
		_inventoryDisable = value;
		if (value)
		{
			ThreadPool.schedule(new InventoryEnableTask(this), 1500);
		}
	}
	
	/**
	 * @return True if the Inventory is disabled.
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}
	
	/**
	 * Add a cubic to this player.
	 * @param cubicId the cubic ID
	 * @param level
	 * @param cubicPower
	 * @param cubicDelay
	 * @param cubicSkillChance
	 * @param cubicMaxCount
	 * @param cubicDuration
	 * @param givenByOther
	 * @return the old cubic for this cubic ID if any, otherwise {@code null}
	 */
	public CubicInstance addCubic(int cubicId, int level, double cubicPower, int cubicDelay, int cubicSkillChance, int cubicMaxCount, int cubicDuration, boolean givenByOther)
	{
		return _cubics.put(cubicId, new CubicInstance(this, cubicId, level, (int) cubicPower, cubicDelay, cubicSkillChance, cubicMaxCount, cubicDuration, givenByOther));
	}
	
	/**
	 * Get the player's cubics.
	 * @return the cubics
	 */
	public Map<Integer, CubicInstance> getCubics()
	{
		return _cubics;
	}
	
	/**
	 * Get the player cubic by cubic ID, if any.
	 * @param cubicId the cubic ID
	 * @return the cubic with the given cubic ID, {@code null} otherwise
	 */
	public CubicInstance getCubicById(int cubicId)
	{
		return _cubics.get(cubicId);
	}
	
	/**
	 * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).
	 */
	public int getEnchantEffect()
	{
		final ItemInstance wpn = getActiveWeaponInstance();
		return wpn == null ? 0 : Math.min(127, wpn.getEnchantLevel());
	}
	
	/**
	 * Set the _lastFolkNpc of the PlayerInstance corresponding to the last Folk wich one the player talked.
	 * @param folkNpc
	 */
	public void setLastFolkNPC(Npc folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}
	
	/**
	 * @return the _lastFolkNpc of the PlayerInstance corresponding to the last Folk wich one the player talked.
	 */
	public Npc getLastFolkNPC()
	{
		return _lastFolkNpc;
	}
	
	/**
	 * @return True if PlayerInstance is a participant in the Festival of Darkness.
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}
	
	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.add(itemId);
	}
	
	public boolean removeAutoSoulShot(int itemId)
	{
		return _activeSoulShots.remove(itemId);
	}
	
	public Set<Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}
	
	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		ItemInstance item;
		IItemHandler handler;
		if ((_activeSoulShots == null) || _activeSoulShots.isEmpty())
		{
			return;
		}
		
		for (int itemId : _activeSoulShots)
		{
			item = _inventory.getItemByItemId(itemId);
			if (item != null)
			{
				if (magic && (item.getItem().getDefaultAction() == ActionType.SPIRITSHOT))
				{
					handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
					{
						handler.useItem(this, item, false);
					}
				}
				
				if (physical && (item.getItem().getDefaultAction() == ActionType.SOULSHOT))
				{
					handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
					{
						handler.useItem(this, item, false);
					}
				}
			}
			else
			{
				removeAutoSoulShot(itemId);
			}
		}
	}
	
	/**
	 * Cancel autoshot for all shots matching crystaltype {@link Item#getCrystalType()}.
	 * @param crystalType int type to disable
	 */
	public void disableAutoShotByCrystalType(int crystalType)
	{
		for (int itemId : _activeSoulShots)
		{
			if (ItemTable.getInstance().getTemplate(itemId).getCrystalType().getLevel() == crystalType)
			{
				disableAutoShot(itemId);
			}
		}
	}
	
	/**
	 * Cancel autoshot use for shot itemId
	 * @param itemId int id to disable
	 * @return true if canceled.
	 */
	public boolean disableAutoShot(int itemId)
	{
		if (_activeSoulShots.contains(itemId))
		{
			removeAutoSoulShot(itemId);
			sendPacket(new ExAutoSoulShot(itemId, 0));
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
			sm.addItemName(itemId);
			sendPacket(sm);
			return true;
		}
		return false;
	}
	
	/**
	 * Cancel all autoshots for player
	 */
	public void disableAutoShotsAll()
	{
		for (int itemId : _activeSoulShots)
		{
			sendPacket(new ExAutoSoulShot(itemId, 0));
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
			sm.addItemName(itemId);
			sendPacket(sm);
		}
		_activeSoulShots.clear();
	}
	
	public EnumIntBitmask<ClanPrivilege> getClanPrivileges()
	{
		return _clanPrivileges;
	}
	
	public void setClanPrivileges(EnumIntBitmask<ClanPrivilege> clanPrivileges)
	{
		_clanPrivileges = clanPrivileges.clone();
	}
	
	public boolean hasClanPrivilege(ClanPrivilege privilege)
	{
		return _clanPrivileges.has(privilege);
	}
	
	// baron etc
	public void setPledgeClass(int classId)
	{
		_pledgeClass = classId;
		checkItemRestriction();
	}
	
	public int getPledgeClass()
	{
		return _pledgeClass;
	}
	
	public void setPledgeType(int typeId)
	{
		_pledgeType = typeId;
	}
	
	@Override
	public int getPledgeType()
	{
		return _pledgeType;
	}
	
	public int getApprentice()
	{
		return _apprentice;
	}
	
	public void setApprentice(int apprenticeId)
	{
		_apprentice = apprenticeId;
	}
	
	public int getSponsor()
	{
		return _sponsor;
	}
	
	public void setSponsor(int sponsorId)
	{
		_sponsor = sponsorId;
	}
	
	@Override
	public void sendMessage(String message)
	{
		sendPacket(new SystemMessage(SendMessageLocalisationData.getLocalisation(this, message)));
	}
	
	public void enterObserverMode(Location loc)
	{
		setLastLocation();
		
		// Remove Hide.
		getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.HIDE);
		_observerMode = true;
		setTarget(null);
		setParalyzed(true);
		startParalyze();
		setInvul(true);
		setInvisible(true);
		sendPacket(new ObservationMode(loc));
		teleToLocation(loc, false);
		broadcastUserInfo();
	}
	
	public void setLastLocation()
	{
		_lastLoc.setXYZ(getX(), getY(), getZ());
	}
	
	public void unsetLastLocation()
	{
		_lastLoc.setXYZ(0, 0, 0);
	}
	
	public void enterOlympiadObserverMode(Location loc, int id, int instanceId)
	{
		if (hasSummon())
		{
			_summon.unSummon(this);
		}
		
		// Remove Hide.
		getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.HIDE);
		if (!_cubics.isEmpty())
		{
			for (CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			_cubics.clear();
		}
		
		if (_party != null)
		{
			_party.removePartyMember(this, MessageType.EXPELLED);
		}
		
		_olympiadGameId = id;
		if (_waitTypeSitting)
		{
			standUp();
		}
		if (!_observerMode)
		{
			setLastLocation();
		}
		
		_observerMode = true;
		setTarget(null);
		setInvul(true);
		setInvisible(true);
		teleToLocation(loc, instanceId, 0);
		sendPacket(new ExOlympiadMode(3));
		broadcastUserInfo();
	}
	
	public void leaveObserverMode()
	{
		setTarget(null);
		
		teleToLocation(_lastLoc, false);
		unsetLastLocation();
		sendPacket(new ObservationReturn(getLocation()));
		setParalyzed(false);
		if (!isGM())
		{
			setInvisible(false);
			setInvul(false);
		}
		if (hasAI())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		
		setFalling(); // prevent receive falling damage
		_observerMode = false;
		broadcastUserInfo();
	}
	
	public void leaveOlympiadObserverMode()
	{
		if (_olympiadGameId == -1)
		{
			return;
		}
		_olympiadGameId = -1;
		_observerMode = false;
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		setInstanceId(0);
		teleToLocation(_lastLoc, true);
		if (!isGM())
		{
			setInvisible(false);
			setInvul(false);
		}
		if (hasAI())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		unsetLastLocation();
		broadcastUserInfo();
	}
	
	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}
	
	public int getOlympiadSide()
	{
		return _olympiadSide;
	}
	
	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}
	
	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}
	
	/**
	 * Gets the player's olympiad buff count.
	 * @return the olympiad's buff count
	 */
	public int getOlympiadBuffCount()
	{
		return _olyBuffsCount;
	}
	
	/**
	 * Sets the player's olympiad buff count.
	 * @param buffs the olympiad's buff count
	 */
	public void setOlympiadBuffCount(int buffs)
	{
		_olyBuffsCount = buffs;
	}
	
	public Location getLastLocation()
	{
		return _lastLoc;
	}
	
	public boolean inObserverMode()
	{
		return _observerMode;
	}
	
	public int getTeleMode()
	{
		return _telemode;
	}
	
	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}
	
	public void setLoto(int i, int value)
	{
		_loto[i] = value;
	}
	
	public int getLoto(int i)
	{
		return _loto[i];
	}
	
	public void setRace(int i, int value)
	{
		_race[i] = value;
	}
	
	public int getRace(int i)
	{
		return _race[i];
	}
	
	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}
	
	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}
	
	public boolean getDietMode()
	{
		return _dietMode;
	}
	
	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}
	
	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}
	
	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}
	
	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}
	
	public BlockList getBlockList()
	{
		return _blockList;
	}
	
	public void setHero(boolean hero)
	{
		if (hero && (_baseClass == _activeClass))
		{
			for (Skill skill : SkillTreeData.getInstance().getHeroSkillTree().values())
			{
				addSkill(skill, false); // Don't persist hero skills into database
			}
		}
		else
		{
			for (Skill skill : SkillTreeData.getInstance().getHeroSkillTree().values())
			{
				removeSkill(skill, false, true); // Just remove skills from non-hero players
			}
		}
		_hero = hero;
		sendSkillList();
	}
	
	public void setInOlympiadMode(boolean value)
	{
		_inOlympiadMode = value;
	}
	
	public void setOlympiadStart(boolean value)
	{
		_olympiadStart = value;
	}
	
	public boolean isOlympiadStart()
	{
		return _olympiadStart;
	}
	
	public boolean isHero()
	{
		return _hero;
	}
	
	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}
	
	@Override
	public boolean isInDuel()
	{
		return _isInDuel;
	}
	
	public void setStartingDuel()
	{
		_startingDuel = true;
	}
	
	@Override
	public int getDuelId()
	{
		return _duelId;
	}
	
	public void setDuelState(int mode)
	{
		_duelState = mode;
	}
	
	public int getDuelState()
	{
		return _duelState;
	}
	
	/**
	 * Sets up the duel state using a non 0 duelId.
	 * @param duelId 0=not in a duel
	 */
	public void setInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
		_startingDuel = false;
	}
	
	/**
	 * This returns a SystemMessage stating why the player is not available for duelling.
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		final SystemMessage sm = new SystemMessage(_noDuelReason);
		sm.addPcName(this);
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}
	
	/**
	 * Checks if this player might join / start a duel.<br>
	 * To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || isJailed())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
			return false;
		}
		if (isDead() || isAlikeDead() || ((getCurrentHp() < (getMaxHp() / 2)) || (getCurrentMp() < (getMaxMp() / 2))))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_S_HP_OR_MP_IS_BELOW_50;
			return false;
		}
		if (_isInDuel || _startingDuel)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL;
			return false;
		}
		if (_inOlympiadMode)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
			return false;
		}
		if (isCursedWeaponEquipped())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_IN_A_CHAOTIC_STATE;
			return false;
		}
		if (_privateStoreType != PrivateStoreType.NONE)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
			return false;
		}
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_RIDING_A_BOAT_STEED_OR_STRIDER;
			return false;
		}
		if (_fishing)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_FISHING;
			return false;
		}
		if (isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.SIEGE))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_MAKE_A_CHALLENGE_TO_A_DUEL_BECAUSE_C1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA_PEACEFUL_ZONE_SEVEN_SIGNS_ZONE_NEAR_WATER_RESTART_PROHIBITED_AREA;
			return false;
		}
		return true;
	}
	
	public boolean isNoble()
	{
		return _noble;
	}
	
	public void setNoble(boolean value)
	{
		final Collection<Skill> nobleSkillTree = SkillTreeData.getInstance().getNobleSkillTree().values();
		if (value)
		{
			for (Skill skill : nobleSkillTree)
			{
				addSkill(skill, false);
			}
		}
		else
		{
			for (Skill skill : nobleSkillTree)
			{
				removeSkill(skill, false, true);
			}
		}
		
		_noble = value;
		sendSkillList();
	}
	
	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}
	
	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}
	
	@Override
	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}
	
	@Override
	public void setTeam(Team team)
	{
		super.setTeam(team);
		broadcastUserInfo();
		if (hasSummon())
		{
			_summon.broadcastStatusUpdate();
		}
	}
	
	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}
	
	public int getWantsPeace()
	{
		return _wantsPeace;
	}
	
	public boolean isFishing()
	{
		return _fishing;
	}
	
	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}
	
	public void sendSkillList()
	{
		boolean isDisabled = false;
		final SkillList sl = new SkillList();
		for (Skill s : getAllSkills())
		{
			if ((s == null) || ((_transformation != null) && !s.isPassive()) || (hasTransformSkill(s.getId()) && s.isPassive()))
			{
				continue;
			}
			if (_clan != null)
			{
				isDisabled = s.isClanSkill() && (_clan.getReputationScore() < 0);
			}
			
			boolean isEnchantable = SkillData.getInstance().isEnchantable(s.getId());
			if (isEnchantable)
			{
				final EnchantSkillLearn esl = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(s.getId());
				if ((esl == null) || (s.getLevel() < esl.getBaseLevel()))
				{
					isEnchantable = false;
				}
			}
			
			sl.addSkill(s.getDisplayId(), s.getDisplayLevel(), s.isPassive(), isDisabled, isEnchantable);
		}
		
		if (_transformation != null)
		{
			final Map<Integer, Integer> ts = new TreeMap<>();
			for (SkillHolder holder : _transformation.getTemplate(this).getSkills())
			{
				ts.putIfAbsent(holder.getSkillId(), holder.getSkillLevel());
				if (ts.get(holder.getSkillId()) < holder.getSkillLevel())
				{
					ts.put(holder.getSkillId(), holder.getSkillLevel());
				}
			}
			
			for (AdditionalSkillHolder holder : _transformation.getTemplate(this).getAdditionalSkills())
			{
				if (getLevel() >= holder.getMinLevel())
				{
					ts.putIfAbsent(holder.getSkillId(), holder.getSkillLevel());
					if (ts.get(holder.getSkillId()) < holder.getSkillLevel())
					{
						ts.put(holder.getSkillId(), holder.getSkillLevel());
					}
				}
			}
			
			// Add collection skills.
			if (_transformation.isFlying())
			{
				for (SkillLearn skill : SkillTreeData.getInstance().getCollectSkillTree().values())
				{
					if ((getKnownSkill(skill.getSkillId()) != null) && (!ts.containsKey(skill.getSkillId()) || (ts.get(skill.getSkillId()) < skill.getSkillLevel())))
					{
						ts.put(skill.getSkillId(), skill.getSkillLevel());
					}
				}
			}
			
			for (Entry<Integer, Integer> transformSkill : ts.entrySet())
			{
				final Skill sk = SkillData.getInstance().getSkill(transformSkill.getKey(), transformSkill.getValue());
				addTransformSkill(sk);
				sl.addSkill(transformSkill.getKey(), transformSkill.getValue(), false, false, false);
			}
		}
		
		sendPacket(sl);
	}
	
	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>) for this character.<br>
	 * 2. This method no longer changes the active _classIndex of the player. This is only done by the calling of setActiveClass() method as that should be the only way to do so.
	 * @param classId
	 * @param classIndex
	 * @return boolean subclassAdded
	 */
	public boolean addSubClass(int classId, int classIndex)
	{
		if (_subclassLock)
		{
			return false;
		}
		_subclassLock = true;
		
		try
		{
			if ((getTotalSubClasses() == Config.MAX_SUBCLASS) || (classIndex == 0))
			{
				return false;
			}
			
			if (getSubClasses().containsKey(classIndex))
			{
				return false;
			}
			
			// Note: Never change _classIndex in any method other than setActiveClass().
			
			final SubClassHolder newClass = new SubClassHolder();
			newClass.setClassId(classId);
			newClass.setClassIndex(classIndex);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(ADD_CHAR_SUBCLASS))
			{
				// Store the basic info about this new sub-class.
				ps.setInt(1, getObjectId());
				ps.setInt(2, newClass.getClassId());
				ps.setLong(3, newClass.getExp());
				ps.setLong(4, newClass.getSp());
				ps.setInt(5, newClass.getLevel());
				ps.setInt(6, newClass.getClassIndex()); // <-- Added
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "WARNING: Could not add character sub class for " + getName() + ": " + e.getMessage(), e);
				return false;
			}
			
			// Commit after database INSERT incase exception is thrown.
			getSubClasses().put(newClass.getClassIndex(), newClass);
			
			final ClassId subTemplate = ClassId.getClassId(classId);
			final Map<Integer, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(subTemplate);
			final Map<Integer, Skill> prevSkillList = new HashMap<>();
			for (SkillLearn skillInfo : skillTree.values())
			{
				if (skillInfo.getGetLevel() <= 40)
				{
					final Skill prevSkill = prevSkillList.get(skillInfo.getSkillId());
					final Skill newSkill = SkillData.getInstance().getSkill(skillInfo.getSkillId(), skillInfo.getSkillLevel());
					if ((prevSkill != null) && (prevSkill.getLevel() > newSkill.getLevel()))
					{
						continue;
					}
					
					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
				}
			}
			return true;
		}
		finally
		{
			_subclassLock = false;
		}
	}
	
	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<br>
	 * 2. Send over the newClassId to addSubClass() to create a new instance on this classIndex.<br>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.
	 * @param classIndex the class index to delete
	 * @param newClassId the new class Id
	 * @return {@code true} if the sub-class was modified, {@code false} otherwise
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		if (_subclassLock)
		{
			return false;
		}
		_subclassLock = true;
		
		try
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement deleteHennas = con.prepareStatement(DELETE_CHAR_HENNAS);
				PreparedStatement deleteShortcuts = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
				PreparedStatement deleteSkillReuse = con.prepareStatement(DELETE_SKILL_SAVE);
				PreparedStatement deleteSkills = con.prepareStatement(DELETE_CHAR_SKILLS);
				PreparedStatement deleteSubclass = con.prepareStatement(DELETE_CHAR_SUBCLASS))
			{
				// Remove all henna info stored for this sub-class.
				deleteHennas.setInt(1, getObjectId());
				deleteHennas.setInt(2, classIndex);
				deleteHennas.execute();
				
				// Remove all shortcuts info stored for this sub-class.
				deleteShortcuts.setInt(1, getObjectId());
				deleteShortcuts.setInt(2, classIndex);
				deleteShortcuts.execute();
				
				// Remove all effects info stored for this sub-class.
				deleteSkillReuse.setInt(1, getObjectId());
				deleteSkillReuse.setInt(2, classIndex);
				deleteSkillReuse.execute();
				
				// Remove all skill info stored for this sub-class.
				deleteSkills.setInt(1, getObjectId());
				deleteSkills.setInt(2, classIndex);
				deleteSkills.execute();
				
				// Remove all basic info stored about this sub-class.
				deleteSubclass.setInt(1, getObjectId());
				deleteSubclass.setInt(2, classIndex);
				deleteSubclass.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e.getMessage(), e);
				
				// This must be done in order to maintain data consistency.
				getSubClasses().remove(classIndex);
				return false;
			}
			
			// Notify to scripts before class is removed.
			if (!getSubClasses().isEmpty()) // also null check
			{
				final int classId = getSubClasses().get(classIndex).getClassId();
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionCancel(this, classId), this);
			}
			
			getSubClasses().remove(classIndex);
		}
		finally
		{
			_subclassLock = false;
		}
		
		return addSubClass(newClassId, classIndex);
	}
	
	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}
	
	public Map<Integer, SubClassHolder> getSubClasses()
	{
		return _subClasses;
	}
	
	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}
	
	public int getBaseClass()
	{
		return _baseClass;
	}
	
	public int getActiveClass()
	{
		return _activeClass;
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	private void setClassTemplate(int classId)
	{
		_activeClass = classId;
		
		final PlayerTemplate pcTemplate = PlayerTemplateData.getInstance().getTemplate(classId);
		if (pcTemplate == null)
		{
			LOGGER.severe("Missing template for classId: " + classId);
			throw new Error();
		}
		// Set the template of the PlayerInstance
		setTemplate(pcTemplate);
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionChange(this, pcTemplate, isSubClassActive()), this);
	}
	
	/**
	 * Changes the character's class based on the given class index.<br>
	 * An index of zero specifies the character's original (base) class, while indexes 1-3 specifies the character's sub-classes respectively.<br>
	 * <font color="00FF00"/>WARNING: Use only on subclase change</font>
	 * @param classIndex
	 */
	public void setActiveClass(int classIndex)
	{
		if (_subclassLock)
		{
			return;
		}
		_subclassLock = true;
		
		try
		{
			// Cannot switch or change subclasses while transformed
			if (_transformation != null)
			{
				return;
			}
			
			// Remove active item skills before saving char to database
			// because next time when choosing this class, weared items can
			// be different
			for (ItemInstance item : _inventory.getAugmentedItems())
			{
				if ((item != null) && item.isEquipped())
				{
					item.getAugmentation().removeBonus(this);
				}
			}
			
			// abort any kind of cast.
			abortCast();
			
			if (isChannelized())
			{
				getSkillChannelized().abortChannelization();
			}
			
			// 1. Call store() before modifying _classIndex to avoid skill effects rollover.
			// 2. Register the correct _classId against applied 'classIndex'.
			store(Config.SUBCLASS_STORE_SKILL_COOLTIME);
			
			resetTimeStamps();
			
			// clear charges
			_charges.set(0);
			stopChargeTask();
			
			if (hasServitor())
			{
				_summon.unSummon(this);
			}
			
			if (classIndex == 0)
			{
				setClassTemplate(_baseClass);
			}
			else
			{
				try
				{
					setClassTemplate(getSubClasses().get(classIndex).getClassId());
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e.getMessage(), e);
					return;
				}
			}
			_classIndex = classIndex;
			setLearningClass(getClassId());
			
			if (isInParty())
			{
				_party.recalculatePartyLevel();
			}
			
			// Update the character's change in class status.
			// 1. Remove any active cubics from the player.
			// 2. Renovate the characters table in the database with the new class info, storing also buff/effect data.
			// 3. Remove all existing skills.
			// 4. Restore all the learned skills for the current class from the database.
			// 5. Restore effect/buff data for the new class.
			// 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones.
			// 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes.
			// 8. Restore shortcut data related to this class.
			// 9. Resend a class change animation effect to broadcast to all nearby players.
			for (Skill oldSkill : getAllSkills())
			{
				removeSkill(oldSkill, false, true);
			}
			
			stopAllEffectsExceptThoseThatLastThroughDeath();
			stopAllEffectsNotStayOnSubclassChange();
			stopCubics();
			
			restoreRecipeBook(false);
			
			// Restore any Death Penalty Buff
			restoreDeathPenaltyBuffLevel();
			
			restoreSkills();
			rewardSkills();
			regiveTemporarySkills();
			getInventory().applyItemSkills();
			
			// Prevents some issues when changing between subclases that shares skills
			resetDisabledSkills();
			
			restoreEffects();
			
			sendPacket(new EtcStatusUpdate(this));
			
			// if player has quest 422: Repent Your Sins, remove it
			final QuestState st = getQuestState("Q00422_RepentYourSins");
			if (st != null)
			{
				st.exitQuest(true);
			}
			
			for (int i = 0; i < 3; i++)
			{
				_henna[i] = null;
			}
			
			restoreHenna();
			sendPacket(new HennaInfo(this));
			if (getCurrentHp() > getMaxHp())
			{
				setCurrentHp(getMaxHp());
			}
			if (getCurrentMp() > getMaxMp())
			{
				setCurrentMp(getMaxMp());
			}
			if (getCurrentCp() > getMaxCp())
			{
				setCurrentCp(getMaxCp());
			}
			
			refreshOverloaded();
			refreshExpertisePenalty();
			broadcastUserInfo();
			
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			_shortCuts.restoreMe();
			sendPacket(new ShortCutInit(this));
			broadcastPacket(new SocialAction(getObjectId(), SocialAction.LEVEL_UP));
			sendPacket(new SkillCoolTime(this));
			sendPacket(new ExStorageMaxCount(this));
			if (Config.ALTERNATE_CLASS_MASTER && Config.CLASS_MASTER_SETTINGS.isAllowed(getClassId().level() + 1) && (((getClassId().level() == 1) && (getLevel() >= 40)) || ((getClassId().level() == 2) && (getLevel() >= 76))))
			{
				ClassMasterInstance.showQuestionMark(this);
			}
		}
		finally
		{
			_subclassLock = false;
		}
	}
	
	public boolean isLocked()
	{
		return _subclassLock;
	}
	
	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			return;
		}
		_taskWarnUserTakeBreak.cancel(true);
		_taskWarnUserTakeBreak = null;
	}
	
	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			_taskWarnUserTakeBreak = ThreadPool.scheduleAtFixedRate(new WarnUserTakeBreakTask(this), 7200000, 7200000);
		}
	}
	
	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (checkLandingState() && (_mountType == MountType.WYVERN))
			{
				teleToLocation(TeleportWhereType.TOWN);
			}
			
			if (dismount()) // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				_taskRentPet = null;
			}
		}
	}
	
	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
		{
			_taskRentPet = ThreadPool.scheduleAtFixedRate(new RentPetTask(this), seconds * 1000, seconds * 1000);
		}
	}
	
	public boolean isRentedPet()
	{
		return _taskRentPet != null;
	}
	
	public void stopWaterTask()
	{
		if (_taskWater == null)
		{
			return;
		}
		_taskWater.cancel(false);
		_taskWater = null;
		sendPacket(new SetupGauge(getObjectId(), 2, 0));
	}
	
	public void startWaterTask()
	{
		if (isDead() || (_taskWater != null))
		{
			return;
		}
		final int timeinwater = (int) calcStat(Stat.BREATH, 60000, this, null);
		sendPacket(new SetupGauge(getObjectId(), 2, timeinwater));
		_taskWater = ThreadPool.scheduleAtFixedRate(new WaterTask(this), timeinwater, 1000);
	}
	
	public boolean isInWater()
	{
		return _taskWater != null;
	}
	
	public void checkWaterState()
	{
		if (isInsideZone(ZoneId.WATER))
		{
			startWaterTask();
		}
		else
		{
			stopWaterTask();
		}
	}
	
	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();
		
		if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(getObjectId()) != SevenSigns.getInstance().getCabalHighestScore()))
			{
				teleToLocation(TeleportWhereType.TOWN);
				setIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(getObjectId()) == SevenSigns.CABAL_NULL))
			{
				teleToLocation(TeleportWhereType.TOWN);
				setIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}
		
		if (isGM() && !Config.GM_STARTUP_BUILDER_HIDE)
		{
			// Bleah, see L2J custom below.
			if (isInvul())
			{
				sendMessage("Entering world in Invulnerable mode.");
			}
			if (isInvisible())
			{
				sendMessage("Entering world in Invisible mode.");
			}
			if (_silenceMode)
			{
				sendMessage("Entering world in Silence mode.");
			}
		}
		
		// Buff and status icons
		if (Config.STORE_SKILL_COOLTIME)
		{
			restoreEffects();
		}
		
		// TODO : Need to fix that hack!
		if (!isDead())
		{
			setCurrentCp(_originalCp);
			setCurrentHp(_originalHp);
			setCurrentMp(_originalMp);
		}
		
		revalidateZone(true);
		
		notifyFriends();
		if (!canOverrideCond(PlayerCondOverride.SKILL_CONDITIONS) && Config.DECREASE_SKILL_LEVEL)
		{
			checkPlayerSkills();
		}
		
		try
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(this))
			{
				zone.onPlayerLoginInside(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
		
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogin(this), this);
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	@Override
	public void doRevive()
	{
		super.doRevive();
		
		updateEffectIcons();
		sendPacket(new EtcStatusUpdate(this));
		_revivePet = false;
		_reviveRequested = 0;
		_revivePower = 0;
		
		// Teleport summon to player.
		if (isInsideZone(ZoneId.PEACE) && (_summon != null) && !_summon.isInsideZone(ZoneId.SIEGE))
		{
			_summon.teleToLocation(getLocation(), true);
		}
		
		if (isMounted())
		{
			startFeed(_mountNpcId);
		}
		
		if (isInParty() && _party.isInDimensionalRift() && !DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
		{
			_party.getDimensionalRift().memberRessurected(this);
		}
		
		if (getInstanceId() > 0)
		{
			final Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
			if (instance != null)
			{
				instance.cancelEjectDeadPlayer(this);
			}
		}
	}
	
	@Override
	public void setName(String value)
	{
		super.setName(value);
		if (Config.CACHE_CHAR_NAMES)
		{
			CharNameTable.getInstance().addName(this);
		}
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		doRevive();
		restoreExp(revivePower);
	}
	
	public void reviveRequest(PlayerInstance reviver, boolean pet, int power)
	{
		if (isResurrectionBlocked())
		{
			return;
		}
		
		if (_reviveRequested == 1)
		{
			if (_revivePet == pet)
			{
				reviver.sendPacket(SystemMessageId.RESURRECTION_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
			}
			else if (pet)
			{
				reviver.sendPacket(SystemMessageId.A_PET_CANNOT_BE_RESURRECTED_WHILE_IT_S_OWNER_IS_IN_THE_PROCESS_OF_RESURRECTING); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
			}
			else
			{
				reviver.sendPacket(SystemMessageId.WHILE_A_PET_IS_BEING_RESURRECTED_IT_CANNOT_HELP_IN_RESURRECTING_ITS_MASTER); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
			}
			return;
		}
		if ((pet && hasPet() && _summon.isDead()) || (!pet && isDead()))
		{
			_reviveRequested = 1;
			int restoreExp = 0;
			_revivePower = Formulas.calculateSkillResurrectRestorePercent(power, reviver);
			restoreExp = (int) Math.round(((_expBeforeDeath - getExp()) * _revivePower) / 100);
			_revivePet = pet;
			if (_hasCharmOfCourage)
			{
				final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.YOUR_CHARM_OF_COURAGE_IS_TRYING_TO_RESURRECT_YOU_WOULD_YOU_LIKE_TO_RESURRECT_NOW.getId());
				dlg.addTime(60000);
				sendPacket(dlg);
				return;
			}
			final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.C1_IS_MAKING_AN_ATTEMPT_TO_RESURRECT_YOU_IF_YOU_CHOOSE_THIS_PATH_S2_EXPERIENCE_POINTS_WILL_BE_RETURNED_TO_YOU_DO_YOU_WANT_TO_BE_RESURRECTED.getId());
			dlg.getSystemMessage().addPcName(reviver);
			dlg.getSystemMessage().addString(Integer.toString(restoreExp));
			sendPacket(dlg);
		}
	}
	
	public void reviveAnswer(int answer)
	{
		if ((_reviveRequested != 1) || (!isDead() && !_revivePet) || (_revivePet && hasPet() && !_summon.isDead()))
		{
			return;
		}
		
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
				{
					doRevive(_revivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if (hasPet())
			{
				if (_revivePower != 0)
				{
					_summon.doRevive(_revivePower);
				}
				else
				{
					_summon.doRevive();
				}
			}
		}
		_revivePet = false;
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	public boolean isReviveRequested()
	{
		return _reviveRequested == 1;
	}
	
	public boolean isRevivingPet()
	{
		return _revivePet;
	}
	
	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	public void onActionRequest()
	{
		if (isSpawnProtected())
		{
			setSpawnProtection(false);
			if (!isInsideZone(ZoneId.PEACE))
			{
				sendPacket(SystemMessageId.YOU_ARE_NO_LONGER_PROTECTED_FROM_AGGRESSIVE_MONSTERS);
			}
			if (Config.RESTORE_SERVITOR_ON_RECONNECT && !hasSummon() && CharSummonTable.getInstance().getServitors().containsKey(getObjectId()))
			{
				CharSummonTable.getInstance().restoreServitor(this);
			}
			if (Config.RESTORE_PET_ON_RECONNECT && !hasSummon() && CharSummonTable.getInstance().getPets().containsKey(getObjectId()))
			{
				CharSummonTable.getInstance().restorePet(this);
			}
		}
		if (isTeleportProtected())
		{
			setTeleportProtection(false);
			if (!isInsideZone(ZoneId.PEACE))
			{
				sendMessage("Teleport spawn protection ended.");
			}
		}
	}
	
	/**
	 * Expertise of the PlayerInstance (None=0, D=1, C=2, B=3, A=4, S=5, S80=6, S84=7)
	 * @return int Expertise skill level.
	 */
	public int getExpertiseLevel()
	{
		return getSkillLevel(239);
	}
	
	@Override
	public void teleToLocation(ILocational loc, boolean allowRandomOffset)
	{
		if ((_vehicle != null) && !_vehicle.isTeleporting())
		{
			setVehicle(null);
		}
		
		if (isFlyingMounted() && (loc.getZ() < -1005))
		{
			super.teleToLocation(loc.getX(), loc.getY(), -1005, loc.getHeading(), loc.getInstanceId());
		}
		
		super.teleToLocation(loc, allowRandomOffset);
	}
	
	@Override
	public synchronized void onTeleported()
	{
		super.onTeleported();
		
		if (isInAirShip())
		{
			getAirShip().sendInfo(this);
		}
		else // Update last player position upon teleport.
		{
			setLastServerPosition(getX(), getY(), getZ());
		}
		
		// Force a revalidation
		revalidateZone(true);
		
		checkItemRestriction();
		
		if ((Config.PLAYER_TELEPORT_PROTECTION > 0) && !_inOlympiadMode)
		{
			setTeleportProtection(true);
		}
		
		// Trained beast is lost after teleport
		if (_tamedBeast != null)
		{
			for (TamedBeastInstance tamedBeast : _tamedBeast)
			{
				tamedBeast.deleteMe();
			}
			_tamedBeast.clear();
		}
		
		// Modify the position of the pet if necessary
		if (_summon != null)
		{
			_summon.setFollowStatus(false);
			_summon.teleToLocation(getLocation(), false);
			((SummonAI) _summon.getAI()).setStartFollowController(true);
			_summon.setFollowStatus(true);
			_summon.updateAndBroadcastStatus(0);
		}
		
		// show movie if available
		if (_movieHolder != null)
		{
			sendPacket(new ExStartScenePlayer(_movieHolder.getMovie()));
		}
	}
	
	@Override
	public void setTeleporting(boolean teleport)
	{
		setTeleporting(teleport, true);
	}
	
	public void setTeleporting(boolean teleport, boolean useWatchDog)
	{
		super.setTeleporting(teleport);
		if (!useWatchDog)
		{
			return;
		}
		if (teleport)
		{
			if ((_teleportWatchdog == null) && (Config.TELEPORT_WATCHDOG_TIMEOUT > 0))
			{
				synchronized (this)
				{
					_teleportWatchdog = ThreadPool.schedule(new TeleportWatchdogTask(this), Config.TELEPORT_WATCHDOG_TIMEOUT * 1000);
				}
			}
		}
		else if (_teleportWatchdog != null)
		{
			_teleportWatchdog.cancel(false);
			_teleportWatchdog = null;
		}
	}
	
	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}
	
	public Location getLastServerPosition()
	{
		return _lastServerPosition;
	}
	
	public void setBlinkActive(boolean value)
	{
		_blinkActive.set(value);
	}
	
	public boolean isBlinkActive()
	{
		return _blinkActive.get();
	}
	
	@Override
	public void addExpAndSp(double addToExp, double addToSp)
	{
		getStat().addExpAndSp(addToExp, addToSp, false);
	}
	
	public void addExpAndSp(double addToExp, double addToSp, boolean useVitality)
	{
		getStat().addExpAndSp(addToExp, addToSp, useVitality);
	}
	
	public void removeExpAndSp(long removeExp, long removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp, true);
	}
	
	public void removeExpAndSp(long removeExp, long removeSp, boolean sendMessage)
	{
		getStat().removeExpAndSp(removeExp, removeSp, sendMessage);
	}
	
	@Override
	public void reduceCurrentHp(double value, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (skill != null)
		{
			getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.getDmgDirectlyToHP());
		}
		else
		{
			getStatus().reduceHp(value, attacker, awake, isDOT, false, false);
		}
		
		// notify the tamed beast of attacks
		if (_tamedBeast != null)
		{
			for (TamedBeastInstance tamedBeast : _tamedBeast)
			{
				tamedBeast.onOwnerGotAttacked(attacker);
			}
		}
	}
	
	public void broadcastSnoop(ChatType type, String name, String text)
	{
		if (_snoopListener.isEmpty())
		{
			return;
		}
		final Snoop sn = new Snoop(getObjectId(), getName(), type, name, text);
		for (PlayerInstance pci : _snoopListener)
		{
			if (pci != null)
			{
				pci.sendPacket(sn);
			}
		}
	}
	
	public void addSnooper(PlayerInstance pci)
	{
		_snoopListener.add(pci);
	}
	
	public void removeSnooper(PlayerInstance pci)
	{
		_snoopListener.remove(pci);
	}
	
	public void addSnooped(PlayerInstance pci)
	{
		_snoopedPlayer.add(pci);
	}
	
	public void removeSnooped(PlayerInstance pci)
	{
		_snoopedPlayer.remove(pci);
	}
	
	public void addHtmlAction(HtmlActionScope scope, String action)
	{
		_htmlActionCaches[scope.ordinal()].add(action);
	}
	
	public void clearHtmlActions(HtmlActionScope scope)
	{
		_htmlActionCaches[scope.ordinal()].clear();
	}
	
	public void setHtmlActionOriginObjectId(HtmlActionScope scope, int npcObjId)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_htmlActionOriginObjectIds[scope.ordinal()] = npcObjId;
	}
	
	public int getLastHtmlActionOriginId()
	{
		return _lastHtmlActionOriginObjId;
	}
	
	private boolean validateHtmlAction(Iterable<String> actionIter, String action)
	{
		for (String cachedAction : actionIter)
		{
			if (cachedAction.charAt(cachedAction.length() - 1) == AbstractHtmlPacket.VAR_PARAM_START_CHAR)
			{
				if (action.startsWith(cachedAction.substring(0, cachedAction.length() - 1).trim()))
				{
					return true;
				}
			}
			else if (cachedAction.equals(action))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if the HTML action was sent in a HTML packet.<br>
	 * If the HTML action was not sent for whatever reason, -1 is returned.<br>
	 * Otherwise, the NPC object ID or 0 is returned.<br>
	 * 0 means the HTML action was not bound to an NPC<br>
	 * and no range checks need to be made.
	 * @param action the HTML action to check
	 * @return NPC object ID, 0 or -1
	 */
	public int validateHtmlAction(String action)
	{
		for (int i = 0; i < _htmlActionCaches.length; ++i)
		{
			if (validateHtmlAction(_htmlActionCaches[i], action))
			{
				_lastHtmlActionOriginObjId = _htmlActionOriginObjectIds[i];
				return _lastHtmlActionOriginObjId;
			}
		}
		return -1;
	}
	
	/**
	 * Performs following tests:
	 * <ul>
	 * <li>Inventory contains item</li>
	 * <li>Item owner id == owner id</li>
	 * <li>It isn't pet control item while mounting pet or pet summoned</li>
	 * <li>It isn't active enchant item</li>
	 * <li>It isn't cursed weapon/item</li>
	 * <li>It isn't wear item</li>
	 * </ul>
	 * @param objectId item object id
	 * @param action just for login purpose
	 * @return
	 */
	public boolean validateItemManipulation(int objectId, String action)
	{
		final ItemInstance item = _inventory.getItemByObjectId(objectId);
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if ((hasSummon() && (_summon.getControlObjectId() == objectId)) || (_mountObjectID == objectId))
		{
			return false;
		}
		
		if (_activeEnchantItemId == objectId)
		{
			return false;
		}
		
		if (CursedWeaponsManager.getInstance().isCursed(item.getId()))
		{
			// can not trade a cursed weapon
			return false;
		}
		
		return true;
	}
	
	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return (_vehicle != null) && _vehicle.isBoat();
	}
	
	/**
	 * @return
	 */
	public BoatInstance getBoat()
	{
		return (BoatInstance) _vehicle;
	}
	
	/**
	 * @return Returns the inAirShip.
	 */
	public boolean isInAirShip()
	{
		return (_vehicle != null) && _vehicle.isAirShip();
	}
	
	/**
	 * @return
	 */
	public AirShipInstance getAirShip()
	{
		return (AirShipInstance) _vehicle;
	}
	
	public Vehicle getVehicle()
	{
		return _vehicle;
	}
	
	public void setVehicle(Vehicle v)
	{
		if ((v == null) && (_vehicle != null))
		{
			_vehicle.removePassenger(this);
		}
		
		_vehicle = v;
	}
	
	public boolean isInVehicle()
	{
		return _vehicle != null;
	}
	
	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}
	
	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}
	
	/**
	 * @return
	 */
	public Location getInVehiclePosition()
	{
		return _inVehiclePosition;
	}
	
	public void setInVehiclePosition(Location pt)
	{
		_inVehiclePosition = pt;
	}
	
	/**
	 * Manage the delete task of a PlayerInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the PlayerInstance is in observer mode, set its position to its position before entering in observer mode</li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * <li>Cancel Crafting, Attack or Cast</li>
	 * <li>Remove the PlayerInstance from the world</li>
	 * <li>Stop Party and Unsummon Pet</li>
	 * <li>Update database with items in its inventory and remove them from the world</li>
	 * <li>Remove all WorldObject from _knownObjects and _knownPlayer of the Creature then cancel Attak or Cast and notify AI</li>
	 * <li>Close the connection with the client</li>
	 * </ul>
	 * <br>
	 * Remember this method is not to be used to half-ass disconnect players! This method is dedicated only to erase the player from the world.<br>
	 * If you intend to disconnect a player please use {@link Disconnection}
	 */
	@Override
	public boolean deleteMe()
	{
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogout(this), this);
		
		try
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(this))
			{
				zone.onPlayerLogoutInside(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			if (!_isOnline)
			{
				LOGGER.log(Level.SEVERE, "deleteMe() called on offline character " + this, new RuntimeException());
			}
			setOnlineStatus(false, true);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (Config.ENABLE_BLOCK_CHECKER_EVENT && (_handysBlockCheckerEventArena != -1))
			{
				HandysBlockCheckerManager.getInstance().onDisconnect(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			_isOnline = false;
			abortAttack();
			abortCast();
			stopMove(null);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// remove combat flag
		try
		{
			if (_inventory.getItemByItemId(9819) != null)
			{
				final Fort fort = FortManager.getInstance().getFort(this);
				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(this, fort.getResidenceId());
				}
				else
				{
					final int slot = _inventory.getSlotFromItem(_inventory.getItemByItemId(9819));
					_inventory.unEquipItemInBodySlot(slot);
					destroyItem("CombatFlag", _inventory.getItemByItemId(9819), null, true);
				}
			}
			else if (_combatFlagEquippedId)
			{
				TerritoryWarManager.getInstance().dropCombatFlag(this, false, false);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			PartyMatchWaitingList.getInstance().removePlayer(this);
			if (_partyroom != 0)
			{
				final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
				if (room != null)
				{
					room.deleteMember(this);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (isFlying())
			{
				removeSkill(SkillData.getInstance().getSkill(4289, 1));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Make sure player variables are stored.
		getVariables().storeMe();
		
		// Make sure account variables are stored.
		getAccountVariables().storeMe();
		
		// Recommendations must be saved before task (timer) is canceled
		try
		{
			storeRecommendations(false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			setTeleporting(false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Stop crafting, if in progress
		try
		{
			RecipeManager.getInstance().requestMakeItemAbort(this);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Cancel Attak or Cast
		try
		{
			setTarget(null);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		// Stop all toggles.
		getEffectList().stopAllToggles();
		
		// Remove from world regions zones
		ZoneManager.getInstance().getRegion(this).removeFromZones(this);
		
		// If a Party is in progress, leave it (and festival party)
		if (isInParty())
		{
			try
			{
				leaveParty();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		stopCubics();
		
		// Remove the PlayerInstance from the world
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (OlympiadManager.getInstance().isRegistered(this) || (getOlympiadGameId() != -1))
		{
			OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
		}
		
		// If the PlayerInstance has Pet, unsummon it
		if (hasSummon())
		{
			try
			{
				_summon.setRestoreSummon(true);
				
				_summon.unSummon(this);
				// Dead pet wasn't unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline)
				if (hasSummon())
				{
					_summon.broadcastNpcInfo(0);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			} // returns pet to control item
		}
		
		if (_clan != null)
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				final ClanMember clanMember = _clan.getClanMember(getObjectId());
				if (clanMember != null)
				{
					clanMember.setPlayerInstance(null);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			setActiveRequester(null);
			cancelActiveTrade();
		}
		
		// If the PlayerInstance is a GM, remove it from the GM List
		if (isGM())
		{
			try
			{
				AdminData.getInstance().deleteGm(this);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		try
		{
			// Check if the PlayerInstance is in observer mode to set its position to its position
			// before entering in observer mode
			if (_observerMode)
			{
				setLocationInvisible(_lastLoc);
			}
			
			if (_vehicle != null)
			{
				_vehicle.oustPlayer(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// remove player from instance and set spawn location if any
		try
		{
			final int instanceId = getInstanceId();
			if ((instanceId != 0) && !Config.RESTORE_PLAYER_INSTANCE)
			{
				final Instance inst = InstanceManager.getInstance().getInstance(instanceId);
				if (inst != null)
				{
					inst.removePlayer(getObjectId());
					final Location loc = inst.getExitLoc();
					if (loc != null)
					{
						final int x = loc.getX() + Rnd.get(-30, 30);
						final int y = loc.getY() + Rnd.get(-30, 30);
						setXYZInvisible(x, y, loc.getZ());
						if (hasSummon()) // dead pet
						{
							_summon.teleToLocation(loc, true);
							_summon.setInstanceId(0);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Update database with items in its inventory and remove them from the world
		try
		{
			_inventory.deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Update database with items in its warehouse and remove them from the world
		try
		{
			clearWarehouse();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().remCacheTask(this);
		}
		
		try
		{
			_freight.deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			clearRefund();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (isCursedWeaponEquipped())
		{
			try
			{
				CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).setPlayer(null);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		if (_clanId > 0)
		{
			_clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			// ClanTable.getInstance().getClan(getClanId()).broadcastToOnlineMembers(new PledgeShowMemberListAdd(this));
		}
		
		for (PlayerInstance player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (PlayerInstance player : _snoopListener)
		{
			player.removeSnooped(this);
		}
		
		try
		{
			notifyFriends();
			_blockList.playerLogout();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on deleteMe() notifyFriends: " + e.getMessage(), e);
		}
		
		PlayerAutoSaveTaskManager.getInstance().remove(this);
		
		return super.deleteMe();
	}
	
	private Fish _fish;
	
	// startFishing() was stripped of any pre-fishing related checks, namely the fishing zone check.
	// Also worthy of note is the fact the code to find the hook landing position was also striped.
	// The stripped code was moved into fishing.java.
	// In my opinion it makes more sense for it to be there since all other skill related checks were also there.
	// Last but not least, moving the zone check there, fixed a bug where baits would always be consumed no matter if fishing actualy took place.
	// startFishing() now takes up 3 arguments, wich are acurately described as being the hook landing coordinates.
	public void startFishing(int x, int y, int z)
	{
		stopMove(null);
		setImmobilized(true);
		_fishing = true;
		_fishx = x;
		_fishy = y;
		_fishz = z;
		// broadcastUserInfo();
		// Starts fishing
		final int lvl = getRandomFishLvl();
		final int grade = getRandomFishGrade();
		final int group = getRandomFishGroup(grade);
		final List<Fish> fish = FishData.getInstance().getFish(lvl, group, grade);
		if ((fish == null) || fish.isEmpty())
		{
			sendMessage("Error - Fish are not defined");
			endFishing(false);
			return;
		}
		// Use a copy constructor else the fish data may be over-written below
		_fish = fish.get(Rnd.get(fish.size())).clone();
		fish.clear();
		sendPacket(SystemMessageId.YOU_CAST_YOUR_LINE_AND_START_TO_FISH);
		if (!GameTimeTaskManager.getInstance().isNight() && _lure.isNightLure())
		{
			_fish.setFishGroup(-1);
		}
		// sendMessage("Hook x,y: " + _x + "," + _y + " - Water Z, Player Z:" + _z + ", " + getZ()); //debug line, uncoment to show coordinates used in fishing.
		broadcastPacket(new ExFishingStart(this, _fish.getFishGroup(), x, y, z, _lure.isNightLure()));
		sendPacket(new PlaySound(1, "SF_P_01", 0, 0, 0, 0, 0));
		startLookingForFishTask();
	}
	
	public void stopLookingForFishTask()
	{
		if (_taskForFish == null)
		{
			return;
		}
		_taskForFish.cancel(false);
		_taskForFish = null;
	}
	
	public void startLookingForFishTask()
	{
		if (!isDead() && (_taskForFish == null))
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;
			if (_lure != null)
			{
				final int lureid = _lure.getId();
				isNoob = _fish.getFishGrade() == 0;
				isUpperGrade = _fish.getFishGrade() == 2;
				if ((lureid == 6519) || (lureid == 6522) || (lureid == 6525) || (lureid == 8505) || (lureid == 8508) || (lureid == 8511))
				{
					checkDelay = _fish.getGutsCheckTime() * 133;
				}
				else if ((lureid == 6520) || (lureid == 6523) || (lureid == 6526) || ((lureid >= 8505) && (lureid <= 8513)) || ((lureid >= 7610) && (lureid <= 7613)) || ((lureid >= 7807) && (lureid <= 7809)) || ((lureid >= 8484) && (lureid <= 8486)))
				{
					checkDelay = _fish.getGutsCheckTime() * 100;
				}
				else if ((lureid == 6521) || (lureid == 6524) || (lureid == 6527) || (lureid == 8507) || (lureid == 8510) || (lureid == 8513))
				{
					checkDelay = _fish.getGutsCheckTime() * 66;
				}
			}
			_taskForFish = ThreadPool.scheduleAtFixedRate(new LookingForFishTask(this, _fish.getStartCombatTime(), _fish.getFishGuts(), _fish.getFishGroup(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}
	
	private int getRandomFishGrade()
	{
		switch (_lure.getId())
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
			case 8486: // prize-winning for beginners
			{
				return 0;
			}
			case 8485: // prize-winning luminous
			case 8506: // green luminous
			case 8509: // purple luminous
			case 8512: // yellow luminous
			{
				return 2;
			}
			default:
			{
				return 1;
			}
		}
	}
	
	private int getRandomFishGroup(int group)
	{
		final int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
			case 0: // fish for novices
			{
				switch (_lure.getId())
				{
					case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
					{
						if (check <= 54)
						{
							type = 5;
						}
						else if (check <= 77)
						{
							type = 4;
						}
						else
						{
							type = 6;
						}
						break;
					}
					case 7808: // purple lure, preferred by fat fish (type 4)
					{
						if (check <= 54)
						{
							type = 4;
						}
						else if (check <= 77)
						{
							type = 6;
						}
						else
						{
							type = 5;
						}
						break;
					}
					case 7809: // yellow lure, preferred by ugly fish (type 6)
					{
						if (check <= 54)
						{
							type = 6;
						}
						else if (check <= 77)
						{
							type = 5;
						}
						else
						{
							type = 4;
						}
						break;
					}
					case 8486: // prize-winning fishing lure for beginners
					{
						if (check <= 33)
						{
							type = 4;
						}
						else if (check <= 66)
						{
							type = 5;
						}
						else
						{
							type = 6;
						}
						break;
					}
				}
				break;
			}
			case 1: // normal fish
			{
				switch (_lure.getId())
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
					{
						type = 3;
						break;
					}
					case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
					{
						if (check <= 54)
						{
							type = 1;
						}
						else if (check <= 74)
						{
							type = 0;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					}
					case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
					{
						if (check <= 54)
						{
							type = 0;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					}
					case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
					{
						if (check <= 55)
						{
							type = 2;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 0;
						}
						else
						{
							type = 3;
						}
						break;
					}
					case 8484: // prize-winning fishing lure
					{
						if (check <= 33)
						{
							type = 0;
						}
						else if (check <= 66)
						{
							type = 1;
						}
						else
						{
							type = 2;
						}
						break;
					}
				}
				break;
			}
			case 2: // upper grade fish, luminous lure
			{
				switch (_lure.getId())
				{
					case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
					{
						if (check <= 54)
						{
							type = 8;
						}
						else if (check <= 77)
						{
							type = 7;
						}
						else
						{
							type = 9;
						}
						break;
					}
					case 8509: // purple lure, preferred by fat fish (type 7)
					{
						if (check <= 54)
						{
							type = 7;
						}
						else if (check <= 77)
						{
							type = 9;
						}
						else
						{
							type = 8;
						}
						break;
					}
					case 8512: // yellow lure, preferred by ugly fish (type 9)
					{
						if (check <= 54)
						{
							type = 9;
						}
						else if (check <= 77)
						{
							type = 8;
						}
						else
						{
							type = 7;
						}
						break;
					}
					case 8485: // prize-winning fishing lure
					{
						if (check <= 33)
						{
							type = 7;
						}
						else if (check <= 66)
						{
							type = 8;
						}
						else
						{
							type = 9;
						}
						break;
					}
				}
			}
		}
		return type;
	}
	
	private int getRandomFishLvl()
	{
		int skillLevel = getSkillLevel(1315);
		final BuffInfo info = getEffectList().getBuffInfoBySkillId(2274);
		if (info != null)
		{
			skillLevel = (int) info.getSkill().getPower();
		}
		if (skillLevel <= 0)
		{
			return 1;
		}
		int randomlvl;
		final int check = Rnd.get(100);
		if (check <= 50)
		{
			randomlvl = skillLevel;
		}
		else if (check <= 85)
		{
			randomlvl = skillLevel - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skillLevel + 1;
			if (randomlvl > 27)
			{
				randomlvl = 27;
			}
		}
		return randomlvl;
	}
	
	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new Fishing(this, _fish, isNoob, isUpperGrade, _lure.getId());
	}
	
	public void endFishing(boolean win)
	{
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		// broadcastUserInfo();
		if (_fishCombat == null)
		{
			sendPacket(SystemMessageId.THE_BAIT_HAS_BEEN_LOST_BECAUSE_THE_FISH_GOT_AWAY);
		}
		_fishCombat = null;
		_lure = null;
		// Ends fishing
		broadcastPacket(new ExFishingEnd(win, this));
		sendPacket(SystemMessageId.YOU_REEL_YOUR_LINE_IN_AND_STOP_FISHING);
		setImmobilized(false);
		stopLookingForFishTask();
	}
	
	public Fishing getFishCombat()
	{
		return _fishCombat;
	}
	
	public int getFishx()
	{
		return _fishx;
	}
	
	public int getFishy()
	{
		return _fishy;
	}
	
	public int getFishz()
	{
		return _fishz;
	}
	
	public void setLure(ItemInstance lure)
	{
		_lure = lure;
	}
	
	public ItemInstance getLure()
	{
		return _lure;
	}
	
	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
		{
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		}
		else
		{
			ivlim = getRace() == Race.DWARF ? Config.INVENTORY_MAXIMUM_DWARF : Config.INVENTORY_MAXIMUM_NO_DWARF;
		}
		return ivlim += (int) getStat().calcStat(Stat.INV_LIM, 0, null, null);
	}
	
	public int getWareHouseLimit()
	{
		return (getRace() == Race.DWARF ? Config.WAREHOUSE_SLOTS_DWARF : Config.WAREHOUSE_SLOTS_NO_DWARF) + (int) getStat().calcStat(Stat.WH_LIM, 0, null, null);
	}
	
	public int getPrivateSellStoreLimit()
	{
		return (getRace() == Race.DWARF ? Config.MAX_PVTSTORESELL_SLOTS_DWARF : Config.MAX_PVTSTORESELL_SLOTS_OTHER) + (int) getStat().calcStat(Stat.P_SELL_LIM, 0, null, null);
	}
	
	public int getPrivateBuyStoreLimit()
	{
		return (getRace() == Race.DWARF ? Config.MAX_PVTSTOREBUY_SLOTS_DWARF : Config.MAX_PVTSTOREBUY_SLOTS_OTHER) + (int) getStat().calcStat(Stat.P_BUY_LIM, 0, null, null);
	}
	
	public int getDwarfRecipeLimit()
	{
		return Config.DWARF_RECIPE_LIMIT + (int) getStat().calcStat(Stat.REC_D_LIM, 0, null, null);
	}
	
	public int getCommonRecipeLimit()
	{
		return Config.COMMON_RECIPE_LIMIT + (int) getStat().calcStat(Stat.REC_C_LIM, 0, null, null);
	}
	
	/**
	 * @return Returns the mountNpcId.
	 */
	public int getMountNpcId()
	{
		return _mountNpcId;
	}
	
	/**
	 * @return Returns the mountLevel.
	 */
	public int getMountLevel()
	{
		return _mountLevel;
	}
	
	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}
	
	public int getMountObjectID()
	{
		return _mountObjectID;
	}
	
	/**
	 * @return the current skill in use or return null.
	 */
	public SkillUseHolder getCurrentSkill()
	{
		return _currentSkill;
	}
	
	/**
	 * Create a new SkillUseHolder object and set the player _currentSkill.
	 * @param currentSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setCurrentSkill(Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentSkill = null;
			return;
		}
		_currentSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * @return the current pet skill in use or return null.
	 */
	public SkillUseHolder getCurrentPetSkill()
	{
		return _currentPetSkill;
	}
	
	/**
	 * Create a new SkillUseHolder object and set the player _currentPetSkill.
	 * @param currentSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setCurrentPetSkill(Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentPetSkill = null;
			return;
		}
		_currentPetSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
	}
	
	public SkillUseHolder getQueuedSkill()
	{
		return _queuedSkill;
	}
	
	/**
	 * Create a new SkillUseHolder object and queue it in the player _queuedSkill.
	 * @param queuedSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setQueuedSkill(Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			_queuedSkill = null;
			return;
		}
		_queuedSkill = new SkillUseHolder(queuedSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * @return {@code true} if player is jailed, {@code false} otherwise.
	 */
	public boolean isJailed()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.JAIL) //
			|| PunishmentManager.getInstance().hasPunishment(getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.JAIL) //
			|| PunishmentManager.getInstance().hasPunishment(getIPAddress(), PunishmentAffect.IP, PunishmentType.JAIL) //
			|| ((_client != null) && (_client.getHardwareInfo() != null) && PunishmentManager.getInstance().hasPunishment(_client.getHardwareInfo().getMacAddress(), PunishmentAffect.HWID, PunishmentType.JAIL));
	}
	
	/**
	 * @return {@code true} if player is chat banned, {@code false} otherwise.
	 */
	public boolean isChatBanned()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN) //
			|| PunishmentManager.getInstance().hasPunishment(getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.CHAT_BAN) //
			|| PunishmentManager.getInstance().hasPunishment(getIPAddress(), PunishmentAffect.IP, PunishmentType.CHAT_BAN) //
			|| ((_client != null) && (_client.getHardwareInfo() != null) && PunishmentManager.getInstance().hasPunishment(_client.getHardwareInfo().getMacAddress(), PunishmentAffect.HWID, PunishmentType.CHAT_BAN));
	}
	
	public void startFameTask(long delay, int fameFixRate)
	{
		if ((getLevel() < 40) || (getClassId().level() < 2))
		{
			return;
		}
		if (_fameTask == null)
		{
			_fameTask = ThreadPool.scheduleAtFixedRate(new FameTask(this, fameFixRate), delay, delay);
		}
	}
	
	public void stopFameTask()
	{
		if (_fameTask == null)
		{
			return;
		}
		_fameTask.cancel(false);
		_fameTask = null;
	}
	
	public void startVitalityTask()
	{
		if (Config.ENABLE_VITALITY && (_vitalityTask == null))
		{
			_vitalityTask = ThreadPool.scheduleAtFixedRate(new VitalityTask(this), 1000, 60000);
		}
	}
	
	public void stopVitalityTask()
	{
		if (_vitalityTask != null)
		{
			_vitalityTask.cancel(false);
			_vitalityTask = null;
		}
	}
	
	public int getPowerGrade()
	{
		return _powerGrade;
	}
	
	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}
	
	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}
	
	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}
	
	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}
	
	public boolean isCombatFlagEquipped()
	{
		return _combatFlagEquippedId;
	}
	
	public void setCombatFlagEquipped(boolean value)
	{
		_combatFlagEquippedId = value;
	}
	
	/**
	 * Returns the Number of Souls this PlayerInstance got.
	 * @return
	 */
	public int getChargedSouls()
	{
		return _souls;
	}
	
	/**
	 * Increase Souls
	 * @param count
	 */
	public void increaseSouls(int count)
	{
		_souls += count;
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SOUL_COUNT_HAS_INCREASED_BY_S1_IT_IS_NOW_AT_S2);
		sm.addInt(count);
		sm.addInt(_souls);
		sendPacket(sm);
		restartSoulTask();
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Decreases existing Souls.
	 * @param count
	 * @return
	 */
	public boolean decreaseSouls(int count)
	{
		_souls -= count;
		if (_souls < 0)
		{
			_souls = 0;
		}
		
		if (_souls == 0)
		{
			stopSoulTask();
		}
		else
		{
			restartSoulTask();
		}
		
		sendPacket(new EtcStatusUpdate(this));
		return true;
	}
	
	/**
	 * Clear out all Souls from this PlayerInstance
	 */
	public void clearSouls()
	{
		_souls = 0;
		stopSoulTask();
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Starts/Restarts the SoulTask to Clear Souls after 10 Mins.
	 */
	private void restartSoulTask()
	{
		if (_soulTask != null)
		{
			_soulTask.cancel(false);
			_soulTask = null;
		}
		_soulTask = ThreadPool.schedule(new ResetSoulsTask(this), 600000);
	}
	
	/**
	 * Stops the Clearing Task.
	 */
	public void stopSoulTask()
	{
		if (_soulTask == null)
		{
			return;
		}
		_soulTask.cancel(false);
		_soulTask = null;
	}
	
	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}
	
	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}
	
	public void calculateDeathPenaltyBuffLevel(Creature killer)
	{
		if (killer == null)
		{
			LOGGER.warning(this + " called calculateDeathPenaltyBuffLevel with killer null!");
			return;
		}
		
		if (isResurrectSpecialAffected() || isLucky() || isOnEvent() || isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE) || canOverrideCond(PlayerCondOverride.DEATH_PENALTY))
		{
			return;
		}
		
		double percent = 1.0;
		if (killer.isRaid())
		{
			percent *= calcStat(Stat.REDUCE_DEATH_PENALTY_BY_RAID, 1);
		}
		else if (killer.isMonster())
		{
			percent *= calcStat(Stat.REDUCE_DEATH_PENALTY_BY_MOB, 1);
		}
		else if (killer.isPlayable())
		{
			percent *= calcStat(Stat.REDUCE_DEATH_PENALTY_BY_PVP, 1);
		}
		
		if ((Rnd.get(1, 100) <= ((Config.DEATH_PENALTY_CHANCE) * percent)) && (!killer.isPlayable() || (getKarma() > 0)))
		{
			increaseDeathPenaltyBuffLevel();
		}
	}
	
	public void increaseDeathPenaltyBuffLevel()
	{
		if (_deathPenaltyBuffLevel >= 15)
		{
			return;
		}
		
		if (_deathPenaltyBuffLevel != 0)
		{
			final Skill skill = SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel());
			if (skill != null)
			{
				removeSkill(skill, true);
			}
		}
		_deathPenaltyBuffLevel++;
		addSkill(SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel()), false);
		sendPacket(new EtcStatusUpdate(this));
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_DEATH_PENALTY_IS_NOW_LEVEL_S1);
		sm.addInt(_deathPenaltyBuffLevel);
		sendPacket(sm);
	}
	
	public void reduceDeathPenaltyBuffLevel()
	{
		if (_deathPenaltyBuffLevel <= 0)
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel());
		if (skill != null)
		{
			removeSkill(skill, true);
		}
		
		_deathPenaltyBuffLevel--;
		
		if (_deathPenaltyBuffLevel > 0)
		{
			addSkill(SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel()), false);
			sendPacket(new EtcStatusUpdate(this));
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_DEATH_PENALTY_IS_NOW_LEVEL_S1);
			sm.addInt(_deathPenaltyBuffLevel);
			sendPacket(sm);
		}
		else
		{
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(SystemMessageId.YOUR_DEATH_PENALTY_HAS_BEEN_LIFTED);
		}
	}
	
	public void restoreDeathPenaltyBuffLevel()
	{
		if (_deathPenaltyBuffLevel > 0)
		{
			addSkill(SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel()), false);
		}
	}
	
	@Override
	public PlayerInstance getActingPlayer()
	{
		return this;
	}
	
	@Override
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		// Check if hit is missed
		if (miss)
		{
			if (target.isPlayer())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EVADED_C2_S_ATTACK);
				sm.addPcName(target.getActingPlayer());
				sm.addString(getName());
				target.sendPacket(sm);
			}
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_ATTACK_WENT_ASTRAY);
			sm.addPcName(this);
			sendPacket(sm);
			return;
		}
		
		// Check if hit is critical
		if (pcrit)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_LANDED_A_CRITICAL_HIT);
			sm.addPcName(this);
			sendPacket(sm);
		}
		if (mcrit)
		{
			sendPacket(SystemMessageId.MAGIC_CRITICAL_HIT);
		}
		
		if (isInOlympiadMode() && target.isPlayer() && target.getActingPlayer().isInOlympiadMode() && (target.getActingPlayer().getOlympiadGameId() == getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(this, damage);
		}
		
		SystemMessage sm = null;
		if (target.isInvul() && !target.isNpc())
		{
			sm = new SystemMessage(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
		}
		else if (target.isDoor() || (target instanceof ControlTowerInstance))
		{
			sm = new SystemMessage(SystemMessageId.YOU_HIT_FOR_S1_DAMAGE);
			sm.addInt(damage);
		}
		else if (this != target)
		{
			sm = new SystemMessage(SystemMessageId.C1_HAS_DONE_S3_POINTS_OF_DAMAGE_TO_C2);
			sm.addPcName(this);
			
			// Localisation related.
			String targetName = target.getName();
			if (Config.MULTILANG_ENABLE && target.isNpc())
			{
				final String[] localisation = NpcNameLocalisationData.getInstance().getLocalisation(_lang, target.getId());
				if (localisation != null)
				{
					targetName = localisation[0];
				}
			}
			
			sm.addString(targetName);
			sm.addInt(damage);
		}
		
		if (sm != null)
		{
			sendPacket(sm);
		}
	}
	
	/**
	 * @param npcId
	 */
	public void setAgathionId(int npcId)
	{
		_agathionId = npcId;
	}
	
	/**
	 * @return
	 */
	public int getAgathionId()
	{
		return _agathionId;
	}
	
	public int getVitalityPoints()
	{
		return getStat().getVitalityPoints();
	}
	
	/**
	 * @return Vitality Level
	 */
	public int getVitalityLevel()
	{
		return getStat().getVitalityLevel();
	}
	
	public void setVitalityPoints(int points, boolean quiet)
	{
		getStat().setVitalityPoints(points, quiet);
	}
	
	public void updateVitalityPoints(float points, boolean useRates, boolean quiet)
	{
		getStat().updateVitalityPoints(points, useRates, quiet);
	}
	
	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			final ItemInstance equippedItem = _inventory.getPaperdollItem(i);
			if ((equippedItem != null) && !equippedItem.getItem().checkCondition(this, this, false))
			{
				_inventory.unEquipItemInSlot(i);
				
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(equippedItem);
				sendPacket(iu);
				
				SystemMessage sm = null;
				if (equippedItem.getItem().getBodyPart() == Item.SLOT_BACK)
				{
					sendPacket(SystemMessageId.YOUR_CLOAK_HAS_BEEN_UNEQUIPPED_BECAUSE_YOUR_ARMOR_SET_IS_NO_LONGER_COMPLETE);
					return;
				}
				
				if (equippedItem.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
					sm.addInt(equippedItem.getEnchantLevel());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
				}
				sm.addItemName(equippedItem);
				sendPacket(sm);
			}
		}
	}
	
	public void addTransformSkill(Skill sk)
	{
		_transformSkills.put(sk.getId(), sk);
		if (sk.isPassive())
		{
			addSkill(sk, false);
		}
	}
	
	public Skill getTransformSkill(int id)
	{
		return _transformSkills.get(id);
	}
	
	public boolean hasTransformSkill(int id)
	{
		return _transformSkills.containsKey(id);
	}
	
	public void removeAllTransformSkills()
	{
		_transformSkills.clear();
	}
	
	protected void startFeed(int npcId)
	{
		_canFeed = npcId > 0;
		if (!isMounted())
		{
			return;
		}
		if (hasSummon())
		{
			setCurrentFeed(((PetInstance) _summon).getCurrentFed());
			_controlItemId = _summon.getControlObjectId();
			sendPacket(new SetupGauge(getObjectId(), 3, (_curFeed * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
			if (!isDead())
			{
				_mountFeedTask = ThreadPool.scheduleAtFixedRate(new PetFeedTask(this), 10000, 10000);
			}
		}
		else if (_canFeed)
		{
			setCurrentFeed(getMaxFeed());
			sendPacket(new SetupGauge(getObjectId(), 3, (_curFeed * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
			if (!isDead())
			{
				_mountFeedTask = ThreadPool.scheduleAtFixedRate(new PetFeedTask(this), 10000, 10000);
			}
		}
	}
	
	public void stopFeed()
	{
		if (_mountFeedTask == null)
		{
			return;
		}
		_mountFeedTask.cancel(false);
		_mountFeedTask = null;
	}
	
	private final PetLevelData getPetLevelData(int npcId)
	{
		if (_leveldata == null)
		{
			_leveldata = PetDataTable.getInstance().getPetData(npcId).getPetLevelData(getMountLevel());
		}
		return _leveldata;
	}
	
	public int getCurrentFeed()
	{
		return _curFeed;
	}
	
	public int getFeedConsume()
	{
		// if pet is attacking
		if (isAttackingNow())
		{
			return getPetLevelData(_mountNpcId).getPetFeedBattle();
		}
		return getPetLevelData(_mountNpcId).getPetFeedNormal();
	}
	
	public void setCurrentFeed(int num)
	{
		final boolean lastHungryState = isHungry();
		_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
		sendPacket(new SetupGauge(getObjectId(), 3, (_curFeed * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
		// broadcast move speed change when strider becomes hungry / full
		if (lastHungryState != isHungry())
		{
			broadcastUserInfo();
		}
	}
	
	private int getMaxFeed()
	{
		return getPetLevelData(_mountNpcId).getPetMaxFeed();
	}
	
	public boolean isHungry()
	{
		return _canFeed && (getCurrentFeed() < ((PetDataTable.getInstance().getPetData(getMountNpcId()).getHungryLimit() / 100f) * getPetLevelData(getMountNpcId()).getPetMaxFeed()));
	}
	
	public void enteredNoLanding(int delay)
	{
		_dismountTask = ThreadPool.schedule(new DismountTask(this), delay * 1000);
	}
	
	public void exitedNoLanding()
	{
		if (_dismountTask == null)
		{
			return;
		}
		_dismountTask.cancel(true);
		_dismountTask = null;
	}
	
	public void storePetFood(int petId)
	{
		if ((_controlItemId == 0) || (petId == 0))
		{
			return;
		}
		final String req = "UPDATE pets SET fed=? WHERE item_obj_id = ?";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(req))
		{
			ps.setInt(1, _curFeed);
			ps.setInt(2, _controlItemId);
			ps.executeUpdate();
			_controlItemId = 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", e);
		}
	}
	
	public void setInSiege(boolean value)
	{
		_isInSiege = value;
	}
	
	public boolean isInSiege()
	{
		return _isInSiege;
	}
	
	/**
	 * @param isInHideoutSiege sets the value of {@link #_isInHideoutSiege}.
	 */
	public void setInHideoutSiege(boolean isInHideoutSiege)
	{
		_isInHideoutSiege = isInHideoutSiege;
	}
	
	/**
	 * @return the value of {@link #_isInHideoutSiege}, {@code true} if the player is participing on a Hideout Siege, otherwise {@code false}.
	 */
	public boolean isInHideoutSiege()
	{
		return _isInHideoutSiege;
	}
	
	public FloodProtectors getFloodProtectors()
	{
		return _client.getFloodProtectors();
	}
	
	public boolean isFlyingMounted()
	{
		return isTransformed() && _transformation.isFlying();
	}
	
	/**
	 * Returns the Number of Charges this PlayerInstance got.
	 * @return
	 */
	public int getCharges()
	{
		return _charges.get();
	}
	
	public void increaseCharges(int count, int max)
	{
		if (_charges.get() >= max)
		{
			sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
			return;
		}
		
		// Charge clear task should be reset every time a charge is increased.
		restartChargeTask();
		
		if (_charges.addAndGet(count) >= max)
		{
			_charges.set(max);
			sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_FORCE_HAS_INCREASED_TO_LEVEL_S1);
			sm.addInt(_charges.get());
			sendPacket(sm);
		}
		
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public boolean decreaseCharges(int count)
	{
		if (_charges.get() < count)
		{
			return false;
		}
		
		// Charge clear task should be reset every time a charge is decreased and stopped when charges become 0.
		if (_charges.addAndGet(-count) == 0)
		{
			stopChargeTask();
		}
		else
		{
			restartChargeTask();
		}
		
		sendPacket(new EtcStatusUpdate(this));
		return true;
	}
	
	public void clearCharges()
	{
		_charges.set(0);
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Starts/Restarts the ChargeTask to Clear Charges after 10 Mins.
	 */
	private void restartChargeTask()
	{
		if (_chargeTask != null)
		{
			synchronized (this)
			{
				_chargeTask.cancel(false);
			}
		}
		_chargeTask = ThreadPool.schedule(new ResetChargesTask(this), 600000);
	}
	
	/**
	 * Stops the Charges Clearing Task.
	 */
	public void stopChargeTask()
	{
		if (_chargeTask == null)
		{
			return;
		}
		_chargeTask.cancel(false);
		_chargeTask = null;
	}
	
	public void teleportBookmarkModify(int id, int icon, String tag, String name)
	{
		final TeleportBookmark bookmark = _tpbookmarks.get(id);
		if (bookmark != null)
		{
			bookmark.setIcon(icon);
			bookmark.setTag(tag);
			bookmark.setName(name);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_TP_BOOKMARK))
			{
				ps.setInt(1, icon);
				ps.setString(2, tag);
				ps.setString(3, name);
				ps.setInt(4, getObjectId());
				ps.setInt(5, id);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not update character teleport bookmark data: " + e.getMessage(), e);
			}
		}
		
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public void teleportBookmarkDelete(int id)
	{
		if (_tpbookmarks.remove(id) == null)
		{
			return;
		}
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_TP_BOOKMARK))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, id);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not delete character teleport bookmark data: " + e.getMessage(), e);
		}
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public void teleportBookmarkGo(int id)
	{
		if (!teleportBookmarkCondition(0))
		{
			return;
		}
		if (_inventory.getInventoryItemCount(13016, 0) == 0)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
			return;
		}
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
		sm.addItemName(13016);
		sendPacket(sm);
		
		final TeleportBookmark bookmark = _tpbookmarks.get(id);
		if (bookmark != null)
		{
			destroyItem("Consume", _inventory.getItemByItemId(13016).getObjectId(), 1, null, false);
			teleToLocation(bookmark, false);
		}
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public boolean teleportBookmarkCondition(int type)
	{
		if (isInCombat())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_BATTLE);
			return false;
		}
		else if (_isInSiege || (_siegeState != 0))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_A_LARGE_SCALE_BATTLE_SUCH_AS_A_CASTLE_SIEGE_FORTRESS_SIEGE_OR_HIDEOUT_SIEGE);
			return false;
		}
		if (_isInDuel || _startingDuel)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_DUEL);
			return false;
		}
		else if (isFlying())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_FLYING);
			return false;
		}
		else if (_inOlympiadMode)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_IN_AN_OLYMPIAD_MATCH);
			return false;
		}
		else if (isParalyzed())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_IN_A_PETRIFIED_OR_PARALYZED_STATE);
			return false;
		}
		else if (isDead())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_DEAD);
			return false;
		}
		else if ((type == 1) && (_isIn7sDungeon || (isInParty() && _party.isInDimensionalRift())))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			return false;
		}
		else if (isInWater())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_UNDERWATER);
			return false;
		}
		else if ((type == 1) && (isInsideZone(ZoneId.SIEGE) || isInsideZone(ZoneId.CLAN_HALL) || isInsideZone(ZoneId.JAIL) || isInsideZone(ZoneId.CASTLE) || isInsideZone(ZoneId.NO_SUMMON_FRIEND) || isInsideZone(ZoneId.FORT)))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			return false;
		}
		else if (isInsideZone(ZoneId.NO_BOOKMARK) || isInBoat() || isInAirShip())
		{
			if (type == 0)
			{
				sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA);
			}
			else if (type == 1)
			{
				sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			}
			return false;
		}
		else
		{
			return true;
		}
	}
	
	public void teleportBookmarkAdd(int x, int y, int z, int icon, String tag, String name)
	{
		if (!teleportBookmarkCondition(1))
		{
			return;
		}
		
		if (_tpbookmarks.size() >= _bookmarkSlot)
		{
			sendPacket(SystemMessageId.YOU_HAVE_NO_SPACE_TO_SAVE_THE_TELEPORT_LOCATION);
			return;
		}
		
		if (Config.BOOKMARK_CONSUME_ITEM_ID > 0)
		{
			if (_inventory.getInventoryItemCount(Config.BOOKMARK_CONSUME_ITEM_ID, -1) == 0)
			{
				if (Config.BOOKMARK_CONSUME_ITEM_ID == 20033)
				{
					sendPacket(SystemMessageId.YOU_CANNOT_BOOKMARK_THIS_LOCATION_BECAUSE_YOU_DO_NOT_HAVE_A_MY_TELEPORT_FLAG);
				}
				else
				{
					sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				}
				return;
			}
			
			destroyItem("Consume", _inventory.getItemByItemId(Config.BOOKMARK_CONSUME_ITEM_ID).getObjectId(), 1, null, true);
		}
		
		int id;
		for (id = 1; id <= _bookmarkSlot; ++id)
		{
			if (!_tpbookmarks.containsKey(id))
			{
				break;
			}
		}
		_tpbookmarks.put(id, new TeleportBookmark(id, x, y, z, icon, tag, name));
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_TP_BOOKMARK))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, id);
			ps.setInt(3, x);
			ps.setInt(4, y);
			ps.setInt(5, z);
			ps.setInt(6, icon);
			ps.setString(7, tag);
			ps.setString(8, name);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not insert character teleport bookmark data: " + e.getMessage(), e);
		}
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public void restoreTeleportBookmark()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_TP_BOOKMARK))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_tpbookmarks.put(rs.getInt("Id"), new TeleportBookmark(rs.getInt("Id"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("icon"), rs.getString("tag"), rs.getString("name")));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed restoing character teleport bookmark.", e);
		}
	}
	
	@Override
	public void sendInfo(PlayerInstance player)
	{
		if (isInBoat())
		{
			setXYZ(getBoat().getLocation());
			
			player.sendPacket(new CharInfo(this, isInvisible() && player.canOverrideCond(PlayerCondOverride.SEE_ALL_PLAYERS)));
			player.sendPacket(new ExBrExtraUserInfo(this));
			player.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), _inVehiclePosition));
		}
		else if (isInAirShip())
		{
			setXYZ(getAirShip().getLocation());
			
			player.sendPacket(new CharInfo(this, isInvisible() && player.canOverrideCond(PlayerCondOverride.SEE_ALL_PLAYERS)));
			player.sendPacket(new ExBrExtraUserInfo(this));
			player.sendPacket(new ExGetOnAirShip(this, getAirShip()));
		}
		else
		{
			player.sendPacket(new CharInfo(this, isInvisible() && player.canOverrideCond(PlayerCondOverride.SEE_ALL_PLAYERS)));
			player.sendPacket(new ExBrExtraUserInfo(this));
		}
		
		final int relation = getRelation(player);
		final boolean isAutoAttackable = isAutoAttackable(player);
		final RelationCache cache = getKnownRelations().get(player.getObjectId());
		if ((cache == null) || (cache.getRelation() != relation) || (cache.isAutoAttackable() != isAutoAttackable))
		{
			player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
			if (hasSummon())
			{
				player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
			}
			getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
		}
		
		switch (_privateStoreType)
		{
			case SELL:
			{
				player.sendPacket(new PrivateStoreMsgSell(this));
				break;
			}
			case PACKAGE_SELL:
			{
				player.sendPacket(new ExPrivateStoreSetWholeMsg(this));
				break;
			}
			case BUY:
			{
				player.sendPacket(new PrivateStoreMsgBuy(this));
				break;
			}
			case MANUFACTURE:
			{
				player.sendPacket(new RecipeShopMsg(this));
				break;
			}
		}
		if (isTransformed())
		{
			// Required double send for fix Mounted H5+
			sendPacket(new CharInfo(player, false));
		}
	}
	
	public void playMovie(MovieHolder holder)
	{
		if (_movieHolder != null)
		{
			return;
		}
		abortAttack();
		// abortCast(); Confirmed in retail, playing a movie does not abort cast.
		stopMove(null);
		setMovieHolder(holder);
		if (!isTeleporting())
		{
			sendPacket(new ExStartScenePlayer(holder.getMovie()));
		}
	}
	
	public void stopMovie()
	{
		setMovieHolder(null);
	}
	
	public boolean isAllowedToEnchantSkills()
	{
		if (_subclassLock)
		{
			return false;
		}
		if (isTransformed() || isInStance())
		{
			return false;
		}
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this))
		{
			return false;
		}
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			return false;
		}
		if (isInBoat() || isInAirShip())
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Set the _createDate of the PlayerInstance.
	 * @param createDate
	 */
	public void setCreateDate(Calendar createDate)
	{
		_createDate = createDate;
	}
	
	/**
	 * @return the _createDate of the PlayerInstance.
	 */
	public Calendar getCreateDate()
	{
		return _createDate;
	}
	
	/**
	 * @return number of days to char birthday.
	 */
	public int checkBirthDay()
	{
		final Calendar now = Calendar.getInstance();
		
		// "Characters with a February 29 creation date will receive a gift on February 28."
		if ((_createDate.get(Calendar.DAY_OF_MONTH) == 29) && (_createDate.get(Calendar.MONTH) == 1))
		{
			_createDate.add(Calendar.HOUR_OF_DAY, -24);
		}
		
		if ((now.get(Calendar.MONTH) == _createDate.get(Calendar.MONTH)) && (now.get(Calendar.DAY_OF_MONTH) == _createDate.get(Calendar.DAY_OF_MONTH)) && (now.get(Calendar.YEAR) != _createDate.get(Calendar.YEAR)))
		{
			return 0;
		}
		
		int i;
		for (i = 1; i < 6; i++)
		{
			now.add(Calendar.HOUR_OF_DAY, 24);
			if ((now.get(Calendar.MONTH) == _createDate.get(Calendar.MONTH)) && (now.get(Calendar.DAY_OF_MONTH) == _createDate.get(Calendar.DAY_OF_MONTH)) && (now.get(Calendar.YEAR) != _createDate.get(Calendar.YEAR)))
			{
				return i;
			}
		}
		return -1;
	}
	
	/** Friend list. */
	private final Collection<Integer> _friendList = ConcurrentHashMap.newKeySet();
	
	public Collection<Integer> getFriendList()
	{
		return _friendList;
	}
	
	public void restoreFriendList()
	{
		_friendList.clear();
		
		final String sqlQuery = "SELECT friendId FROM character_friends WHERE charId=? AND relation=0";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sqlQuery))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int friendId = rs.getInt("friendId");
					if (friendId == getObjectId())
					{
						continue;
					}
					_friendList.add(friendId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + getName() + "'s FriendList: " + e.getMessage(), e);
		}
	}
	
	private void notifyFriends()
	{
		final FriendStatusPacket pkt = new FriendStatusPacket(getObjectId());
		for (int id : _friendList)
		{
			final PlayerInstance friend = World.getInstance().getPlayer(id);
			if (friend != null)
			{
				friend.sendPacket(pkt);
			}
		}
	}
	
	/**
	 * Verify if this player is in silence mode.
	 * @return the {@code true} if this player is in silence mode, {@code false} otherwise
	 */
	public boolean isSilenceMode()
	{
		return _silenceMode;
	}
	
	/**
	 * While at silenceMode, checks if this player blocks PMs for this user
	 * @param playerObjId the player object Id
	 * @return {@code true} if the given Id is not excluded and this player is in silence mode, {@code false} otherwise
	 */
	public boolean isSilenceMode(int playerObjId)
	{
		if (Config.SILENCE_MODE_EXCLUDE && _silenceMode && (_silenceModeExcluded != null))
		{
			return !_silenceModeExcluded.contains(playerObjId);
		}
		return _silenceMode;
	}
	
	/**
	 * Set the silence mode.
	 * @param mode the value
	 */
	public void setSilenceMode(boolean mode)
	{
		_silenceMode = mode;
		if (_silenceModeExcluded != null)
		{
			_silenceModeExcluded.clear(); // Clear the excluded list on each setSilenceMode
		}
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Add a player to the "excluded silence mode" list.
	 * @param playerObjId the player's object Id
	 */
	public void addSilenceModeExcluded(int playerObjId)
	{
		if (_silenceModeExcluded == null)
		{
			_silenceModeExcluded = new ArrayList<>(1);
		}
		_silenceModeExcluded.add(playerObjId);
	}
	
	private void storeRecipeShopList()
	{
		if (hasManufactureShop())
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				try (PreparedStatement st = con.prepareStatement(DELETE_CHAR_RECIPE_SHOP))
				{
					st.setInt(1, getObjectId());
					st.execute();
				}
				
				try (PreparedStatement st = con.prepareStatement(INSERT_CHAR_RECIPE_SHOP))
				{
					final AtomicInteger slot = new AtomicInteger(1);
					con.setAutoCommit(false);
					for (ManufactureItem item : _manufactureItems.values())
					{
						st.setInt(1, getObjectId());
						st.setInt(2, item.getRecipeId());
						st.setLong(3, item.getCost());
						st.setInt(4, slot.getAndIncrement());
						st.addBatch();
					}
					st.executeBatch();
					con.commit();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not store recipe shop for playerId " + getObjectId() + ": ", e);
			}
		}
	}
	
	private void restoreRecipeShopList()
	{
		if (_manufactureItems != null)
		{
			_manufactureItems.clear();
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_RECIPE_SHOP))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					getManufactureItems().put(rs.getInt("recipeId"), new ManufactureItem(rs.getInt("recipeId"), rs.getLong("price")));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore recipe shop list data for playerId: " + getObjectId(), e);
		}
	}
	
	public float getCollisionRadius()
	{
		if (isMounted() && (_mountNpcId > 0))
		{
			return NpcData.getInstance().getTemplate(getMountNpcId()).getFCollisionRadius();
		}
		if (isTransformed())
		{
			return _transformation.getCollisionRadius(this);
		}
		return _appearance.isFemale() ? getBaseTemplate().getFCollisionRadiusFemale() : getBaseTemplate().getFCollisionRadius();
	}
	
	public float getCollisionHeight()
	{
		if (isMounted() && (_mountNpcId > 0))
		{
			return NpcData.getInstance().getTemplate(getMountNpcId()).getFCollisionHeight();
		}
		if (isTransformed())
		{
			return _transformation.getCollisionHeight(this);
		}
		return _appearance.isFemale() ? getBaseTemplate().getFCollisionHeightFemale() : getBaseTemplate().getFCollisionHeight();
	}
	
	public int getClientX()
	{
		return _clientX;
	}
	
	public int getClientY()
	{
		return _clientY;
	}
	
	public int getClientZ()
	{
		return _clientZ;
	}
	
	public int getClientHeading()
	{
		return _clientHeading;
	}
	
	public void setClientX(int value)
	{
		_clientX = value;
	}
	
	public void setClientY(int value)
	{
		_clientY = value;
	}
	
	public void setClientZ(int value)
	{
		_clientZ = value;
	}
	
	public void setClientHeading(int value)
	{
		_clientHeading = value;
	}
	
	/**
	 * @param z
	 * @return true if character falling now on the start of fall return false for correct coord sync!
	 */
	public boolean isFalling(int z)
	{
		if (isDead() || isFlying() || isFlyingMounted() || isInsideZone(ZoneId.WATER))
		{
			return false;
		}
		
		if ((_fallingTimestamp != 0) && (Chronos.currentTimeMillis() < _fallingTimestamp))
		{
			return true;
		}
		
		final int deltaZ = getZ() - z;
		if (deltaZ <= getBaseTemplate().getSafeFallHeight())
		{
			_fallingTimestamp = 0;
			return false;
		}
		
		// If there is no geodata loaded for the place we are, client Z correction might cause falling damage.
		if (!GeoEngine.getInstance().hasGeo(getX(), getY()))
		{
			_fallingTimestamp = 0;
			return false;
		}
		
		if (_fallingDamage == 0)
		{
			_fallingDamage = (int) Formulas.calcFallDam(this, deltaZ);
		}
		if (_fallingDamageTask != null)
		{
			_fallingDamageTask.cancel(true);
		}
		_fallingDamageTask = ThreadPool.schedule(() ->
		{
			if ((_fallingDamage > 0) && !isInvul())
			{
				reduceCurrentHp(Math.min(_fallingDamage, getCurrentHp() - 1), null, false, true, null);
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_RECEIVED_S1_FALLING_DAMAGE);
				sm.addInt(_fallingDamage);
				sendPacket(sm);
			}
			_fallingDamage = 0;
			_fallingDamageTask = null;
		}, 1500);
		
		// Prevent falling under ground.
		sendPacket(new ValidateLocation(this));
		setFalling();
		
		return false;
	}
	
	/**
	 * Set falling timestamp
	 */
	public void setFalling()
	{
		_fallingTimestamp = Chronos.currentTimeMillis() + FALLING_VALIDATION_DELAY;
	}
	
	public MovieHolder getMovieHolder()
	{
		return _movieHolder;
	}
	
	public void setMovieHolder(MovieHolder movie)
	{
		_movieHolder = movie;
	}
	
	/**
	 * Update last item auction request timestamp to current
	 */
	public void updateLastItemAuctionRequest()
	{
		_lastItemAuctionInfoRequest = Chronos.currentTimeMillis();
	}
	
	/**
	 * @return true if receiving item auction requests<br>
	 *         (last request was in 2 seconds before)
	 */
	public boolean isItemAuctionPolling()
	{
		return (Chronos.currentTimeMillis() - _lastItemAuctionInfoRequest) < 2000;
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || (_movieHolder != null);
	}
	
	private void restoreUISettings()
	{
		_uiKeySettings = new UIKeysSettings(getObjectId());
	}
	
	private void storeUISettings()
	{
		if (_uiKeySettings == null)
		{
			return;
		}
		
		if (!_uiKeySettings.isSaved())
		{
			_uiKeySettings.saveInDB();
		}
	}
	
	public UIKeysSettings getUISettings()
	{
		return _uiKeySettings;
	}
	
	public String getHtmlPrefix()
	{
		return Config.MULTILANG_ENABLE ? _htmlPrefix : "";
	}
	
	public String getLang()
	{
		return _lang;
	}
	
	public boolean setLang(String lang)
	{
		boolean result = false;
		if (Config.MULTILANG_ENABLE)
		{
			if (Config.MULTILANG_ALLOWED.contains(lang))
			{
				_lang = lang;
				result = true;
			}
			else
			{
				_lang = Config.MULTILANG_DEFAULT;
			}
			
			_htmlPrefix = _lang.equals("en") ? "" : "data/lang/" + _lang + "/";
		}
		else
		{
			_lang = null;
			_htmlPrefix = "";
		}
		
		return result;
	}
	
	public long getOfflineStartTime()
	{
		return _offlineShopStart;
	}
	
	public void setOfflineStartTime(long time)
	{
		_offlineShopStart = time;
	}
	
	/**
	 * Remove player from BossZones (used on char logout/exit)
	 */
	public void removeFromBossZone()
	{
		try
		{
			for (BossZone zone : GrandBossManager.getInstance().getZones().values())
			{
				zone.removePlayer(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on removeFromBossZone(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Check all player skills for skill level. If player level is lower than skill learn level - 9, skill level is decreased to next possible level.
	 */
	public void checkPlayerSkills()
	{
		for (Entry<Integer, Skill> e : getSkills().entrySet())
		{
			final SkillLearn learn = SkillTreeData.getInstance().getClassSkill(e.getKey(), e.getValue().getLevel() % 100, getClassId());
			if (learn != null)
			{
				final int levelDiff = e.getKey() == CommonSkill.EXPERTISE.getId() ? 0 : 9;
				if (getLevel() < (learn.getGetLevel() - levelDiff))
				{
					deacreaseSkillLevel(e.getValue(), levelDiff);
				}
			}
		}
	}
	
	private void deacreaseSkillLevel(Skill skill, int levelDiff)
	{
		int nextLevel = -1;
		final Map<Integer, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(getClassId());
		for (SkillLearn sl : skillTree.values())
		{
			if ((sl.getSkillId() == skill.getId()) && (nextLevel < sl.getSkillLevel()) && (getLevel() >= (sl.getGetLevel() - levelDiff)))
			{
				nextLevel = sl.getSkillLevel(); // next possible skill level
			}
		}
		
		if (nextLevel == -1)
		{
			LOGGER.info("Removing skill " + skill + " from player " + toString());
			removeSkill(skill, true); // there is no lower skill
		}
		else
		{
			LOGGER.info("Decreasing skill " + skill + " to " + nextLevel + " for player " + toString());
			addSkill(SkillData.getInstance().getSkill(skill.getId(), nextLevel), true); // replace with lower one
		}
	}
	
	public boolean canMakeSocialAction()
	{
		return (_privateStoreType == PrivateStoreType.NONE) && (getActiveRequester() == null) && !isAlikeDead() && !isAllSkillsDisabled() && !isCastingNow() && !isCastingSimultaneouslyNow() && (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE);
	}
	
	public void setMultiSocialAction(int id, int targetId)
	{
		_multiSociaAction = id;
		_multiSocialTarget = targetId;
	}
	
	public int getMultiSociaAction()
	{
		return _multiSociaAction;
	}
	
	public int getMultiSocialTarget()
	{
		return _multiSocialTarget;
	}
	
	public Collection<TeleportBookmark> getTeleportBookmarks()
	{
		return _tpbookmarks.values();
	}
	
	public int getBookmarkSlot()
	{
		return _bookmarkSlot;
	}
	
	public void setBookmarkSlot(int slot)
	{
		_bookmarkSlot = slot;
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public int getQuestInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_QUEST_ITEMS;
	}
	
	public boolean canAttackCreature(Creature creature)
	{
		if (creature == null)
		{
			return false;
		}
		if (creature.isAttackable())
		{
			return true;
		}
		if (creature.isPlayable())
		{
			if (creature.isInsideZone(ZoneId.PVP) && !creature.isInsideZone(ZoneId.SIEGE))
			{
				return true;
			}
			
			PlayerInstance target;
			if (creature.isSummon())
			{
				target = ((Summon) creature).getOwner();
			}
			else
			{
				target = (PlayerInstance) creature;
			}
			
			if (isInDuel() && target.isInDuel() && (target.getDuelId() == getDuelId()))
			{
				return true;
			}
			else if (isInParty() && target.isInParty())
			{
				if (getParty() == target.getParty())
				{
					return false;
				}
				if (((getParty().getCommandChannel() != null) || (target.getParty().getCommandChannel() != null)) && (getParty().getCommandChannel() == target.getParty().getCommandChannel()))
				{
					return false;
				}
			}
			else if ((getClan() != null) && (target.getClan() != null))
			{
				if (getClanId() == target.getClanId())
				{
					return false;
				}
				if (((getAllyId() > 0) || (target.getAllyId() > 0)) && (getAllyId() == target.getAllyId()))
				{
					return false;
				}
				if (getClan().isAtWarWith(target.getClan().getId()) && target.getClan().isAtWarWith(getClan().getId()))
				{
					return true;
				}
			}
			else if ((getClan() == null) || (target.getClan() == null))
			{
				if ((target.getPvpFlag() == 0) && (target.getKarma() == 0))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Test if player inventory is under 90% capacity
	 * @param includeQuestInv check also quest inventory
	 * @return
	 */
	public boolean isInventoryUnder90(boolean includeQuestInv)
	{
		return (includeQuestInv ? _inventory.getSize() : _inventory.getNonQuestSize()) <= (getInventoryLimit() * 0.9);
	}
	
	/**
	 * Test if player inventory is under 80% capacity
	 * @param includeQuestInv check also quest inventory
	 * @return
	 */
	public boolean isInventoryUnder80(boolean includeQuestInv)
	{
		return (includeQuestInv ? _inventory.getSize() : _inventory.getNonQuestSize()) <= (getInventoryLimit() * 0.8);
	}
	
	public boolean havePetInvItems()
	{
		return _petItems;
	}
	
	public void setPetInvItems(boolean haveit)
	{
		_petItems = haveit;
	}
	
	/**
	 * Restore Pet's inventory items from database.
	 */
	private void restorePetInventoryItems()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;"))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				setPetInvItems(rs.next() && (rs.getInt("object_id") > 0));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not check Items in Pet Inventory for playerId: " + getObjectId(), e);
		}
	}
	
	public String getAdminConfirmCmd()
	{
		return _adminConfirmCmd;
	}
	
	public void setAdminConfirmCmd(String adminConfirmCmd)
	{
		_adminConfirmCmd = adminConfirmCmd;
	}
	
	/**
	 * Load PlayerInstance Recommendations data.
	 * @return
	 */
	private long loadRecommendations()
	{
		long timeLeft = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT rec_have,rec_left,time_left FROM character_reco_bonus WHERE charId=? LIMIT 1"))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					setRecomHave(rs.getInt("rec_have"));
					setRecomLeft(rs.getInt("rec_left"));
					timeLeft = rs.getLong("time_left");
				}
				else
				{
					timeLeft = 3600000;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore Recommendations for player: " + getObjectId(), e);
		}
		return timeLeft;
	}
	
	/**
	 * Update PlayerInstance Recommendations data.
	 * @param cancelRecoTask
	 */
	public void storeRecommendations(boolean cancelRecoTask)
	{
		long recoTaskEnd = 0;
		if ((_nevitHourglassTask != null) && !_nevitHourglassTask.isDone())
		{
			recoTaskEnd = _nevitHourglassTask.getDelay(TimeUnit.MILLISECONDS);
			if (cancelRecoTask)
			{
				_nevitHourglassTask.cancel(false);
				_nevitHourglassTask = null;
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO character_reco_bonus (charId,rec_have,rec_left,time_left) VALUES (?,?,?,?)"))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, _recomHave);
			ps.setInt(3, _recomLeft);
			ps.setLong(4, getStat().hasPausedNevitHourglass() ? loadRecommendations() : recoTaskEnd);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not update Recommendations for player: " + getObjectId(), e);
		}
	}
	
	/**
	 * Store recommendation values without tapping into Nevit Hourglass task.
	 */
	public void storeRecommendationValues()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO character_reco_bonus (charId,rec_have,rec_left) VALUES (?,?,?)"))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, _recomHave);
			ps.setInt(3, _recomLeft);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not update Recommendations for player: " + getObjectId(), e);
		}
	}
	
	public void checkRecoBonusTask()
	{
		// Create bonus task
		startNevitHourglassTask();
		
		// Create task to give new recommendations
		_recoGiveTask = ThreadPool.scheduleAtFixedRate(new RecoGiveTask(this), 7200000, 3600000);
		
		// Store new data
		storeRecommendations(false);
	}
	
	public void startNevitHourglassTask()
	{
		if (_nevitHourglassTask == null)
		{
			// Load data
			final long taskTime = loadRecommendations();
			if (taskTime > 0)
			{
				// If player have some timeleft, start bonus task
				_nevitHourglassTask = ThreadPool.schedule(new RecoBonusTaskEnd(this), taskTime);
			}
			sendPacket(new ExVoteSystemInfo(this));
		}
	}
	
	public void stopNevitHourglassTask()
	{
		if (_nevitHourglassTask != null)
		{
			_nevitHourglassTask.cancel(false);
			_nevitHourglassTask = null;
		}
	}
	
	public void stopRecoGiveTask()
	{
		if (_recoGiveTask != null)
		{
			_recoGiveTask.cancel(false);
			_recoGiveTask = null;
		}
	}
	
	public boolean isRecoTwoHoursGiven()
	{
		return _recoTwoHoursGiven;
	}
	
	public void setRecoTwoHoursGiven(boolean value)
	{
		_recoTwoHoursGiven = value;
	}
	
	public int getNevitHourglassTime()
	{
		if (_nevitHourglassTask != null)
		{
			return (int) Math.max(0, _nevitHourglassTask.getDelay(TimeUnit.SECONDS));
		}
		return 0;
	}
	
	public boolean isNevitHourglassActive()
	{
		return (getNevitHourglassTime() > 0) || getStat().hasPausedNevitHourglass();
	}
	
	public int getNevitHourglassStatus()
	{
		if (getStat().hasPausedNevitHourglass())
		{
			return NEVIT_HOURGLASS_PAUSED;
		}
		else if (getNevitHourglassTime() > 0)
		{
			return NEVIT_HOURGLASS_ACTIVE;
		}
		else
		{
			return NEVIT_HOURGLASS_MAINTAIN;
		}
	}
	
	public int getNevitHourglassBonus()
	{
		if (_isOnline && (_recomHave != 0) && isNevitHourglassActive())
		{
			final int lvl = getLevel() / 10;
			final int exp = (Math.min(100, _recomHave) - 1) / 10;
			return NEVIT_HOURGLASS_BONUS[lvl][exp];
		}
		return 0;
	}
	
	public double getNevitHourglassMultiplier()
	{
		double multiplier = 1.0;
		if (_isOnline && (_recomHave != 0) && isNevitHourglassActive())
		{
			final double bonus = getNevitHourglassBonus();
			if (bonus > 0)
			{
				multiplier += (bonus / 100);
			}
		}
		return multiplier;
	}
	
	public void setPremiumStatus(boolean premiumStatus)
	{
		_premiumStatus = premiumStatus;
	}
	
	public boolean hasPremiumStatus()
	{
		return Config.PREMIUM_SYSTEM_ENABLED && _premiumStatus;
	}
	
	public void setLastPetitionGmName(String gmName)
	{
		_lastPetitionGmName = gmName;
	}
	
	public String getLastPetitionGmName()
	{
		return _lastPetitionGmName;
	}
	
	public ContactList getContactList()
	{
		return _contactList;
	}
	
	public long getNotMoveUntil()
	{
		return _notMoveUntil;
	}
	
	public void updateNotMoveUntil()
	{
		_notMoveUntil = Chronos.currentTimeMillis() + Config.PLAYER_MOVEMENT_BLOCK_TIME;
	}
	
	@Override
	public boolean isPlayer()
	{
		return true;
	}
	
	@Override
	public boolean isChargedShot(ShotType type)
	{
		final ItemInstance weapon = getActiveWeaponInstance();
		return (weapon != null) && weapon.isChargedShot(type);
	}
	
	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		final ItemInstance weapon = getActiveWeaponInstance();
		if (weapon != null)
		{
			weapon.setChargedShot(type, charged);
		}
	}
	
	/**
	 * @param skillId the display skill Id
	 * @return the custom skill
	 */
	public Skill getCustomSkill(int skillId)
	{
		return (_customSkills != null) ? _customSkills.get(skillId) : null;
	}
	
	/**
	 * Add a skill level to the custom skills map.
	 * @param skill the skill to add
	 */
	private final void addCustomSkill(Skill skill)
	{
		if ((skill == null) || (skill.getDisplayId() == skill.getId()))
		{
			return;
		}
		if (_customSkills == null)
		{
			_customSkills = new ConcurrentHashMap<>();
		}
		_customSkills.put(skill.getDisplayId(), skill);
	}
	
	/**
	 * Remove a skill level from the custom skill map.
	 * @param skill the skill to remove
	 */
	private final void removeCustomSkill(Skill skill)
	{
		if ((skill != null) && (_customSkills != null) && (skill.getDisplayId() != skill.getId()))
		{
			_customSkills.remove(skill.getDisplayId());
		}
	}
	
	/**
	 * @return {@code true} if current player can revive and shows 'To Village' button upon death, {@code false} otherwise.
	 */
	@Override
	public boolean canRevive()
	{
		return _canRevive;
	}
	
	/**
	 * This method can prevent from displaying 'To Village' button upon death.
	 * @param value
	 */
	@Override
	public void setCanRevive(boolean value)
	{
		_canRevive = value;
	}
	
	public boolean isRegisteredOnEvent()
	{
		return _isRegisteredOnEvent || _isOnEvent;
	}
	
	public void setRegisteredOnEvent(boolean value)
	{
		_isRegisteredOnEvent = value;
	}
	
	@Override
	public boolean isOnEvent()
	{
		return _isOnEvent;
	}
	
	public void setOnEvent(boolean value)
	{
		_isOnEvent = value;
	}
	
	public boolean isOnSoloEvent()
	{
		return _isOnSoloEvent;
	}
	
	public void setOnSoloEvent(boolean value)
	{
		_isOnSoloEvent = value;
	}
	
	public void setBlockCheckerArena(byte arena)
	{
		_handysBlockCheckerEventArena = arena;
	}
	
	public int getBlockCheckerArena()
	{
		return _handysBlockCheckerEventArena;
	}
	
	public void setOriginalCpHpMp(double cp, double hp, double mp)
	{
		_originalCp = cp;
		_originalHp = hp;
		_originalMp = mp;
	}
	
	@Override
	public void addOverrideCond(PlayerCondOverride... excs)
	{
		super.addOverrideCond(excs);
		getVariables().set(COND_OVERRIDE_KEY, Long.toString(_exceptions));
	}
	
	@Override
	public void removeOverridedCond(PlayerCondOverride... excs)
	{
		super.removeOverridedCond(excs);
		getVariables().set(COND_OVERRIDE_KEY, Long.toString(_exceptions));
	}
	
	/**
	 * @return {@code true} if {@link PlayerVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return getScript(PlayerVariables.class) != null;
	}
	
	/**
	 * @return {@link PlayerVariables} instance containing parameters regarding player.
	 */
	public PlayerVariables getVariables()
	{
		final PlayerVariables vars = getScript(PlayerVariables.class);
		return vars != null ? vars : addScript(new PlayerVariables(getObjectId()));
	}
	
	/**
	 * @return {@code true} if {@link AccountVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasAccountVariables()
	{
		return getScript(AccountVariables.class) != null;
	}
	
	/**
	 * @return {@link AccountVariables} instance containing parameters regarding player.
	 */
	public AccountVariables getAccountVariables()
	{
		final AccountVariables vars = getScript(AccountVariables.class);
		return vars != null ? vars : addScript(new AccountVariables(getAccountName()));
	}
	
	/**
	 * Adds a event listener.
	 * @param listener
	 */
	public void addEventListener(IEventListener listener)
	{
		_eventListeners.add(listener);
	}
	
	/**
	 * Removes event listener
	 * @param listener
	 */
	public void removeEventListener(IEventListener listener)
	{
		_eventListeners.remove(listener);
	}
	
	public void removeEventListener(Class<? extends IEventListener> clazz)
	{
		_eventListeners.removeIf(e -> e.getClass() == clazz);
	}
	
	public Collection<IEventListener> getEventListeners()
	{
		return _eventListeners;
	}
	
	@Override
	public int getId()
	{
		return getClassId().getId();
	}
	
	public boolean isPartyBanned()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN);
	}
	
	/**
	 * @param act
	 * @return {@code true} if action was added successfully, {@code false} otherwise.
	 */
	public boolean addAction(PlayerAction act)
	{
		if (!hasAction(act))
		{
			_actionMask |= act.getMask();
			return true;
		}
		return false;
	}
	
	/**
	 * @param act
	 * @return {@code true} if action was removed successfully, {@code false} otherwise.
	 */
	public boolean removeAction(PlayerAction act)
	{
		if (hasAction(act))
		{
			_actionMask &= ~act.getMask();
			return true;
		}
		return false;
	}
	
	/**
	 * @param act
	 * @return {@code true} if action is present, {@code false} otherwise.
	 */
	public boolean hasAction(PlayerAction act)
	{
		return (_actionMask & act.getMask()) == act.getMask();
	}
	
	// High Five: Nevit's Bonus System
	private final Nevit _nevitSystem = new Nevit(this);
	
	public Nevit getNevitSystem()
	{
		return _nevitSystem;
	}
	
	/**
	 * Set true/false if character got Charm of Courage
	 * @param value true/false
	 */
	public void setCharmOfCourage(boolean value)
	{
		_hasCharmOfCourage = value;
	}
	
	/**
	 * @return {@code true} if effect is present, {@code false} otherwise.
	 */
	public boolean hasCharmOfCourage()
	{
		return _hasCharmOfCourage;
	}
	
	public boolean isGood()
	{
		return _isGood;
	}
	
	public boolean isEvil()
	{
		return _isEvil;
	}
	
	public void setGood()
	{
		_isGood = true;
		_isEvil = false;
	}
	
	public void setEvil()
	{
		_isGood = false;
		_isEvil = true;
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player got war with the target, {@code false} otherwise.
	 */
	public boolean isAtWarWith(Creature target)
	{
		if (target == null)
		{
			return false;
		}
		if ((_clan != null) && !isAcademyMember() && (target.getClan() != null) && !target.isAcademyMember())
		{
			return _clan.isAtWarWith(target.getClan());
		}
		return false;
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player in same party with the target, {@code false} otherwise.
	 */
	public boolean isInPartyWith(Creature target)
	{
		return isInParty() && target.isInParty() && (getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player in same command channel with the target, {@code false} otherwise.
	 */
	public boolean isInCommandChannelWith(Creature target)
	{
		return isInParty() && target.isInParty() && getParty().isInCommandChannel() && target.getParty().isInCommandChannel() && (getParty().getCommandChannel().getLeaderObjectId() == target.getParty().getCommandChannel().getLeaderObjectId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player in same clan with the target, {@code false} otherwise.
	 */
	public boolean isInClanWith(Creature target)
	{
		return (getClanId() != 0) && (target.getClanId() != 0) && (getClanId() == target.getClanId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player in same ally with the target, {@code false} otherwise.
	 */
	public boolean isInAllyWith(Creature target)
	{
		return (getAllyId() != 0) && (target.getAllyId() != 0) && (getAllyId() == target.getAllyId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player at duel with the target, {@code false} otherwise.
	 */
	public boolean isInDuelWith(Creature target)
	{
		return isInDuel() && target.isInDuel() && (getDuelId() == target.getDuelId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player is on same siege side with the target, {@code false} otherwise.
	 */
	public boolean isOnSameSiegeSideWith(Creature target)
	{
		return (getSiegeState() > 0) && isInsideZone(ZoneId.SIEGE) && (getSiegeState() == target.getSiegeState()) && (getSiegeSide() == target.getSiegeSide());
	}
	
	/**
	 * @return the game shop points of the player.
	 */
	public long getGamePoints()
	{
		return getAccountVariables().getInt(GAME_POINTS_VAR, 0);
	}
	
	/**
	 * Sets game shop points for current player.
	 * @param points
	 */
	public void setGamePoints(long points)
	{
		// Immediate store upon change
		final AccountVariables vars = getAccountVariables();
		vars.set(GAME_POINTS_VAR, Math.max(points, 0));
		vars.storeMe();
	}
	
	private TerminateReturn onExperienceReceived()
	{
		if (isDead())
		{
			return new TerminateReturn(false, false, false);
		}
		return new TerminateReturn(true, true, true);
	}
	
	public void disableExpGain()
	{
		addListener(new FunctionEventListener(this, EventType.ON_PLAYABLE_EXP_CHANGED, (OnPlayableExpChanged event) -> onExperienceReceived(), this));
	}
	
	public void enableExpGain()
	{
		removeListenerIf(EventType.ON_PLAYABLE_EXP_CHANGED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Precautionary method to end all tasks upon disconnection.
	 * @TODO: Rework stopAllTimers() method.
	 */
	public void stopAllTasks()
	{
		if ((_mountFeedTask != null) && !_mountFeedTask.isDone() && !_mountFeedTask.isCancelled())
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
		}
		if ((_dismountTask != null) && !_dismountTask.isDone() && !_dismountTask.isCancelled())
		{
			_dismountTask.cancel(false);
			_dismountTask = null;
		}
		if ((_fameTask != null) && !_fameTask.isDone() && !_fameTask.isCancelled())
		{
			_fameTask.cancel(false);
			_fameTask = null;
		}
		if ((_vitalityTask != null) && !_vitalityTask.isDone() && !_vitalityTask.isCancelled())
		{
			_vitalityTask.cancel(false);
			_vitalityTask = null;
		}
		if ((_teleportWatchdog != null) && !_teleportWatchdog.isDone() && !_teleportWatchdog.isCancelled())
		{
			_teleportWatchdog.cancel(false);
			_teleportWatchdog = null;
		}
		if ((_taskForFish != null) && !_taskForFish.isDone() && !_taskForFish.isCancelled())
		{
			_taskForFish.cancel(false);
			_taskForFish = null;
		}
		if ((_chargeTask != null) && !_chargeTask.isDone() && !_chargeTask.isCancelled())
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
		if ((_soulTask != null) && !_soulTask.isDone() && !_soulTask.isCancelled())
		{
			_soulTask.cancel(false);
			_soulTask = null;
		}
		if ((_taskRentPet != null) && !_taskRentPet.isDone() && !_taskRentPet.isCancelled())
		{
			_taskRentPet.cancel(false);
			_taskRentPet = null;
		}
		if ((_taskWater != null) && !_taskWater.isDone() && !_taskWater.isCancelled())
		{
			_taskWater.cancel(false);
			_taskWater = null;
		}
		if ((_fallingDamageTask != null) && !_fallingDamageTask.isDone() && !_fallingDamageTask.isCancelled())
		{
			_fallingDamageTask.cancel(false);
			_fallingDamageTask = null;
		}
		if ((_taskWarnUserTakeBreak != null) && !_taskWarnUserTakeBreak.isDone() && !_taskWarnUserTakeBreak.isCancelled())
		{
			_taskWarnUserTakeBreak.cancel(false);
			_taskWarnUserTakeBreak = null;
		}
		
		synchronized (_questTimers)
		{
			for (QuestTimer timer : _questTimers)
			{
				timer.cancelTask();
			}
			_questTimers.clear();
		}
	}
	
	public void addQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.add(questTimer);
		}
	}
	
	public void removeQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.remove(questTimer);
		}
	}
	
	public void setServitorShare(Map<Stat, Double> map)
	{
		_servitorShare = map;
	}
	
	public double getServitorShareBonus(Stat stat)
	{
		final Map<Stat, Double> stats = _servitorShare;
		if (stats == null)
		{
			return 1.0d;
		}
		
		final Double val = stats.get(stat);
		if (val == null)
		{
			return 1.0d;
		}
		
		return val;
	}
}