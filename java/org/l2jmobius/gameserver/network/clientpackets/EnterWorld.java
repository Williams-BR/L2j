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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.AnnouncementsTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.instancemanager.CHSiegeManager;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.ClanHallManager;
import org.l2jmobius.gameserver.instancemanager.CoupleManager;
import org.l2jmobius.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jmobius.gameserver.instancemanager.DimensionalRiftManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.instancemanager.MailManager;
import org.l2jmobius.gameserver.instancemanager.PetitionManager;
import org.l2jmobius.gameserver.instancemanager.PunishmentManager;
import org.l2jmobius.gameserver.instancemanager.ServerRestartManager;
import org.l2jmobius.gameserver.instancemanager.SiegeManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.model.Couple;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.ClassMasterInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.holders.ClientHardwareInfoHolder;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.residences.AuctionableHall;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.FortSiege;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.model.skills.CommonSkill;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.Die;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExNoticePostArrived;
import org.l2jmobius.gameserver.network.serverpackets.ExNotifyPremiumItem;
import org.l2jmobius.gameserver.network.serverpackets.ExRotation;
import org.l2jmobius.gameserver.network.serverpackets.ExShowContactList;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.ExVoteSystemInfo;
import org.l2jmobius.gameserver.network.serverpackets.FriendList;
import org.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.PledgeStatusChanged;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;
import org.l2jmobius.gameserver.network.serverpackets.ShortCutInit;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.BuilderUtil;

/**
 * Enter World Packet Handler
 * <p>
 * <p>
 * 0000: 03
 * <p>
 * packet format rev87 bddddbdcccccccccccccccccccc
 * <p>
 */
public class EnterWorld implements IClientIncomingPacket
{
	private static final Map<String, ClientHardwareInfoHolder> TRACE_HWINFO = new ConcurrentHashMap<>();
	
	private final int[][] _tracert = new int[5][4];
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		packet.readB(32); // Unknown Byte Array
		packet.readD(); // Unknown Value
		packet.readD(); // Unknown Value
		packet.readD(); // Unknown Value
		packet.readD(); // Unknown Value
		packet.readB(32); // Unknown Byte Array
		packet.readD(); // Unknown Value
		for (int i = 0; i < 5; i++)
		{
			for (int o = 0; o < 4; o++)
			{
				_tracert[i][o] = packet.readC();
			}
		}
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			LOGGER.warning("EnterWorld failed! player returned 'null'.");
			Disconnection.of(client).defaultSequence(LeaveWorld.STATIC_PACKET);
			return;
		}
		
		client.setConnectionState(ConnectionState.IN_GAME);
		
		final String[] adress = new String[5];
		for (int i = 0; i < 5; i++)
		{
			adress[i] = _tracert[i][0] + "." + _tracert[i][1] + "." + _tracert[i][2] + "." + _tracert[i][3];
		}
		
		LoginServerThread.getInstance().sendClientTracert(player.getAccountName(), adress);
		client.setClientTracert(_tracert);
		
		// Restore to instanced area if enabled
		if (Config.RESTORE_PLAYER_INSTANCE)
		{
			player.setInstanceId(InstanceManager.getInstance().getPlayerInstance(player.getObjectId()));
		}
		else
		{
			final int instanceId = InstanceManager.getInstance().getPlayerInstance(player.getObjectId());
			if (instanceId > 0)
			{
				InstanceManager.getInstance().getInstance(instanceId).removePlayer(player.getObjectId());
			}
		}
		
		player.updatePvpTitleAndColor(false);
		
		// Apply special GM properties to the GM when entering
		if (player.isGM())
		{
			gmStartupProcess:
			{
				if (Config.GM_STARTUP_BUILDER_HIDE && AdminData.getInstance().hasAccess("admin_hide", player.getAccessLevel()))
				{
					BuilderUtil.setHiding(player, true);
					BuilderUtil.sendSysMessage(player, "hide is default for builder.");
					BuilderUtil.sendSysMessage(player, "FriendAddOff is default for builder.");
					BuilderUtil.sendSysMessage(player, "whisperoff is default for builder.");
					
					// It isn't recommend to use the below custom L2J GMStartup functions together with retail-like GMStartupBuilderHide, so breaking the process at that stage.
					break gmStartupProcess;
				}
				
				if (Config.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_invul", player.getAccessLevel()))
				{
					player.setInvul(true);
				}
				
				if (Config.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_invisible", player.getAccessLevel()))
				{
					player.setInvisible(true);
				}
				
				if (Config.GM_STARTUP_SILENCE && AdminData.getInstance().hasAccess("admin_silence", player.getAccessLevel()))
				{
					player.setSilenceMode(true);
				}
				
				if (Config.GM_STARTUP_DIET_MODE && AdminData.getInstance().hasAccess("admin_diet", player.getAccessLevel()))
				{
					player.setDietMode(true);
					player.refreshOverloaded();
				}
			}
			
			if (Config.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmliston", player.getAccessLevel()))
			{
				AdminData.getInstance().addGm(player, false);
			}
			else
			{
				AdminData.getInstance().addGm(player, true);
			}
			
			if (Config.GM_GIVE_SPECIAL_SKILLS)
			{
				SkillTreeData.getInstance().addSkills(player, false);
			}
			
			if (Config.GM_GIVE_SPECIAL_AURA_SKILLS)
			{
				SkillTreeData.getInstance().addSkills(player, true);
			}
		}
		
		// Set dead status if applies
		if (player.getCurrentHp() < 0.5)
		{
			player.setDead(true);
		}
		
		boolean showClanNotice = false;
		
		// Clan related checks are here
		if (player.getClan() != null)
		{
			player.sendPacket(new PledgeSkillList(player.getClan()));
			notifyClanMembers(player);
			
			notifySponsorOrApprentice(player);
			
			final AuctionableHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(player.getClan());
			if (clanHall != null)
			{
				if (!clanHall.getPaid())
				{
					player.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
				}
			}
			
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.isInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(player.getClan()))
				{
					player.setSiegeState((byte) 1);
					player.setSiegeSide(siege.getCastle().getResidenceId());
				}
				
				else if (siege.checkIsDefender(player.getClan()))
				{
					player.setSiegeState((byte) 2);
					player.setSiegeSide(siege.getCastle().getResidenceId());
				}
			}
			
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				if (!siege.isInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(player.getClan()))
				{
					player.setSiegeState((byte) 1);
					player.setSiegeSide(siege.getFort().getResidenceId());
				}
				
				else if (siege.checkIsDefender(player.getClan()))
				{
					player.setSiegeState((byte) 2);
					player.setSiegeSide(siege.getFort().getResidenceId());
				}
			}
			
			for (SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values())
			{
				if (!hall.isInSiege())
				{
					continue;
				}
				
				if (hall.isRegistered(player.getClan()))
				{
					player.setSiegeState((byte) 1);
					player.setSiegeSide(hall.getId());
					player.setInHideoutSiege(true);
				}
			}
			
			client.sendPacket(new PledgeShowMemberListAll(player.getClan(), player));
			client.sendPacket(new PledgeStatusChanged(player.getClan()));
			
			// Residential skills support
			if (player.getClan().getCastleId() > 0)
			{
				CastleManager.getInstance().getCastleByOwner(player.getClan()).giveResidentialSkills(player);
			}
			
			if (player.getClan().getFortId() > 0)
			{
				FortManager.getInstance().getFortByOwner(player.getClan()).giveResidentialSkills(player);
			}
			
			showClanNotice = player.getClan().isNoticeEnabled();
		}
		
		if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(player) > 0)
		{
			if (TerritoryWarManager.getInstance().isTWInProgress())
			{
				player.setSiegeState((byte) 1);
			}
			player.setSiegeSide(TerritoryWarManager.getInstance().getRegisteredTerritoryId(player));
		}
		
		// Updating Seal of Strife Buff/Debuff
		if (SevenSigns.getInstance().isSealValidationPeriod() && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL))
		{
			final int cabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
			if (cabal != SevenSigns.CABAL_NULL)
			{
				if (cabal == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
				{
					player.addSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
				}
				else
				{
					player.addSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
				}
			}
		}
		else
		{
			player.removeSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
			player.removeSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
		}
		
		if (Config.ENABLE_VITALITY && Config.RECOVER_VITALITY_ON_RECONNECT)
		{
			final float points = (Config.RATE_RECOVERY_ON_RECONNECT * (Chronos.currentTimeMillis() - player.getLastAccess())) / 60000;
			if (points > 0)
			{
				player.updateVitalityPoints(points, false, true);
			}
		}
		
		if (Config.NEVIT_ENABLED)
		{
			player.checkRecoBonusTask();
		}
		
		player.broadcastUserInfo();
		
		// Send Macro List
		player.getMacros().sendUpdate();
		
		// Apply item skills.
		player.getInventory().applyItemSkills();
		
		// Send Item List
		client.sendPacket(new ItemList(player, false));
		
		// Send Teleport Bookmark List
		client.sendPacket(new ExGetBookMarkInfoPacket(player));
		
		// Send Shortcuts
		client.sendPacket(new ShortCutInit(player));
		
		// Send Action list
		player.sendPacket(ExBasicActionList.STATIC_PACKET);
		
		// Send Skill list
		player.sendSkillList();
		
		// Send Dye Information
		player.sendPacket(new HennaInfo(player));
		Quest.playerEnter(player);
		
		// Faction System
		if (Config.FACTION_SYSTEM_ENABLED)
		{
			if (player.isGood())
			{
				player.getAppearance().setNameColor(Config.FACTION_GOOD_NAME_COLOR);
				player.getAppearance().setTitleColor(Config.FACTION_GOOD_NAME_COLOR);
				player.sendMessage("Welcome " + player.getName() + ", you are fighting for the " + Config.FACTION_GOOD_TEAM_NAME + " faction.");
				player.sendPacket(new ExShowScreenMessage("Welcome " + player.getName() + ", you are fighting for the " + Config.FACTION_GOOD_TEAM_NAME + " faction.", 10000));
				player.broadcastUserInfo(); // for seeing self name color
			}
			else if (player.isEvil())
			{
				player.getAppearance().setNameColor(Config.FACTION_EVIL_NAME_COLOR);
				player.getAppearance().setTitleColor(Config.FACTION_EVIL_NAME_COLOR);
				player.sendMessage("Welcome " + player.getName() + ", you are fighting for the " + Config.FACTION_EVIL_TEAM_NAME + " faction.");
				player.sendPacket(new ExShowScreenMessage("Welcome " + player.getName() + ", you are fighting for the " + Config.FACTION_EVIL_TEAM_NAME + " faction.", 10000));
				player.broadcastUserInfo(); // for seeing self name color
			}
		}
		
		if (!Config.DISABLE_TUTORIAL)
		{
			loadTutorial(player);
		}
		
		player.sendPacket(new QuestList(player));
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			player.setSpawnProtection(true);
		}
		
		player.spawnMe(player.getX(), player.getY(), player.getZ());
		player.sendPacket(new ExRotation(player.getObjectId(), player.getHeading()));
		
		// Wedding Checks
		if (Config.ALLOW_WEDDING)
		{
			engage(player);
			notifyPartner(player);
		}
		
		if (player.isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).cursedOnLogin();
		}
		
		player.updateEffectIcons();
		
		player.sendPacket(new EtcStatusUpdate(player));
		
		// Expand Skill
		player.sendPacket(new ExStorageMaxCount(player));
		client.sendPacket(new FriendList(player));
		SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_FRIEND_S1_JUST_LOGGED_IN);
		sm.addString(player.getName());
		for (int id : player.getFriendList())
		{
			final WorldObject obj = World.getInstance().findObject(id);
			if (obj != null)
			{
				obj.sendPacket(sm);
			}
		}
		
		player.sendPacket(SystemMessageId.WELCOME_TO_THE_WORLD_OF_LINEAGE_II);
		
		SevenSigns.getInstance().sendCurrentPeriodMsg(player);
		AnnouncementsTable.getInstance().showAnnouncements(player);
		
		if ((Config.SERVER_RESTART_SCHEDULE_ENABLED) && (Config.SERVER_RESTART_SCHEDULE_MESSAGE))
		{
			player.sendPacket(new CreatureSay(null, ChatType.BATTLEFIELD, "[SERVER]", "Next restart is scheduled at " + ServerRestartManager.getInstance().getNextRestartTime() + "."));
		}
		
		if (showClanNotice)
		{
			final NpcHtmlMessage notice = new NpcHtmlMessage();
			notice.setFile(player, "data/html/clanNotice.htm");
			notice.replace("%clan_name%", player.getClan().getName());
			notice.replace("%notice_text%", player.getClan().getNotice().replaceAll("\r\n", "<br>"));
			notice.disableValidation();
			client.sendPacket(notice);
		}
		else if (Config.SERVER_NEWS)
		{
			final String serverNews = HtmCache.getInstance().getHtm(player, "data/html/servnews.htm");
			if (serverNews != null)
			{
				client.sendPacket(new NpcHtmlMessage(serverNews));
			}
		}
		
		if (Config.PETITIONING_ALLOWED)
		{
			PetitionManager.getInstance().checkPetitionMessages(player);
		}
		
		if (player.isAlikeDead()) // dead or fake dead
		{
			// no broadcast needed since the player will already spawn dead to others
			client.sendPacket(new Die(player));
		}
		
		player.onPlayerEnter();
		
		client.sendPacket(new SkillCoolTime(player));
		if (Config.NEVIT_ENABLED)
		{
			client.sendPacket(new ExVoteSystemInfo(player));
		}
		client.sendPacket(new ExShowContactList(player));
		for (ItemInstance i : player.getInventory().getItems())
		{
			if (i.isTimeLimitedItem())
			{
				i.scheduleLifeTimeTask();
			}
			if (i.isShadowItem() && i.isEquipped())
			{
				i.decreaseMana(false);
			}
		}
		
		for (ItemInstance i : player.getWarehouse().getItems())
		{
			if (i.isTimeLimitedItem())
			{
				i.scheduleLifeTimeTask();
			}
		}
		
		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getX(), player.getY(), player.getZ(), false))
		{
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
		}
		
		if (player.getClanJoinExpiryTime() > Chronos.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS);
		}
		
		// remove combat flag before teleporting
		if (player.getInventory().getItemByItemId(9819) != null)
		{
			final Fort fort = FortManager.getInstance().getFort(player);
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
		}
		
		// Attacker or spectator logging in to a siege zone.
		// Actually should be checked for inside castle only?
		if (!player.canOverrideCond(PlayerCondOverride.ZONE_CONDITIONS) && player.isInsideZone(ZoneId.SIEGE) && (!player.isInSiege() || (player.getSiegeState() < 2)))
		{
			player.teleToLocation(TeleportWhereType.TOWN);
		}
		
		// Remove demonic weapon if character is not cursed weapon equipped.
		if ((player.getInventory().getItemByItemId(8190) != null) && !player.isCursedWeaponEquipped())
		{
			player.destroyItem("Zariche", player.getInventory().getItemByItemId(8190), null, true);
		}
		if ((player.getInventory().getItemByItemId(8689) != null) && !player.isCursedWeaponEquipped())
		{
			player.destroyItem("Akamanah", player.getInventory().getItemByItemId(8689), null, true);
		}
		
		if (Config.ALLOW_MAIL && MailManager.getInstance().hasUnreadPost(player))
		{
			client.sendPacket(ExNoticePostArrived.valueOf(false));
		}
		
		if (Config.WELCOME_MESSAGE_ENABLED)
		{
			player.sendPacket(new ExShowScreenMessage(Config.WELCOME_MESSAGE_TEXT, Config.WELCOME_MESSAGE_TIME));
		}
		
		ClassMasterInstance.showQuestionMark(player);
		
		final int birthday = player.checkBirthDay();
		if (birthday == 0)
		{
			player.sendPacket(SystemMessageId.HAPPY_BIRTHDAY_ALEGRIA_HAS_SENT_YOU_A_BIRTHDAY_GIFT);
			// player.sendPacket(new ExBirthdayPopup()); Removed in H5?
		}
		else if (birthday != -1)
		{
			sm = new SystemMessage(SystemMessageId.THERE_ARE_S1_DAYS_REMAINING_UNTIL_YOUR_BIRTHDAY_ON_YOUR_BIRTHDAY_YOU_WILL_RECEIVE_A_GIFT_THAT_ALEGRIA_HAS_CAREFULLY_PREPARED);
			sm.addString(Integer.toString(birthday));
			player.sendPacket(sm);
		}
		
		if (!player.getPremiumItemList().isEmpty())
		{
			player.sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
		}
		
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.STORE_OFFLINE_TRADE_IN_REALTIME)
		{
			OfflineTraderTable.onTransaction(player, true, false);
		}
		
		// Check if expoff is enabled.
		if (player.getVariables().getBoolean("EXPOFF", false))
		{
			player.disableExpGain();
			player.sendMessage("Experience gain is disabled.");
		}
		
		// Prevent relogin in game gfx.
		player.sendPacket(new ValidateLocation(player));
		
		// Unstuck players that had client open when server crashed.
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		// Delayed HWID checks.
		if (Config.HARDWARE_INFO_ENABLED)
		{
			ThreadPool.schedule(() ->
			{
				// Generate trace string.
				final StringBuilder sb = new StringBuilder();
				for (int[] i : _tracert)
				{
					for (int j : i)
					{
						sb.append(j);
						sb.append(".");
					}
				}
				final String trace = sb.toString();
				
				// Get hardware info from client.
				ClientHardwareInfoHolder hwInfo = client.getHardwareInfo();
				if (hwInfo != null)
				{
					hwInfo.store(player);
					TRACE_HWINFO.put(trace, hwInfo);
				}
				else
				{
					// Get hardware info from stored tracert map.
					hwInfo = TRACE_HWINFO.get(trace);
					if (hwInfo != null)
					{
						hwInfo.store(player);
						client.setHardwareInfo(hwInfo);
					}
					// Get hardware info from account variables.
					else
					{
						final String storedInfo = player.getAccountVariables().getString(AccountVariables.HWID, "");
						if (!storedInfo.isEmpty())
						{
							hwInfo = new ClientHardwareInfoHolder(storedInfo);
							TRACE_HWINFO.put(trace, hwInfo);
							client.setHardwareInfo(hwInfo);
						}
					}
				}
				
				// Banned?
				if ((hwInfo != null) && PunishmentManager.getInstance().hasPunishment(hwInfo.getMacAddress(), PunishmentAffect.HWID, PunishmentType.BAN))
				{
					Disconnection.of(client).defaultSequence(LeaveWorld.STATIC_PACKET);
					return;
				}
				
				// Check max players.
				if (Config.KICK_MISSING_HWID && (hwInfo == null))
				{
					Disconnection.of(client).defaultSequence(LeaveWorld.STATIC_PACKET);
				}
				else if (Config.MAX_PLAYERS_PER_HWID > 0)
				{
					int count = 0;
					for (PlayerInstance plr : World.getInstance().getPlayers())
					{
						if (plr.isOnlineInt() == 1)
						{
							final ClientHardwareInfoHolder hwi = plr.getClient().getHardwareInfo();
							if ((hwi != null) && hwi.equals(hwInfo))
							{
								count++;
							}
						}
					}
					if (count >= Config.MAX_PLAYERS_PER_HWID)
					{
						Disconnection.of(client).defaultSequence(LeaveWorld.STATIC_PACKET);
					}
				}
			}, 5000);
		}
	}
	
	private void engage(PlayerInstance player)
	{
		final int chaId = player.getObjectId();
		for (Couple cl : CoupleManager.getInstance().getCouples())
		{
			if ((cl.getPlayer1Id() == chaId) || (cl.getPlayer2Id() == chaId))
			{
				if (cl.getMaried())
				{
					player.setMarried(true);
				}
				
				player.setCoupleId(cl.getId());
				
				if (cl.getPlayer1Id() == chaId)
				{
					player.setPartnerId(cl.getPlayer2Id());
				}
				else
				{
					player.setPartnerId(cl.getPlayer1Id());
				}
			}
		}
	}
	
	private void notifyPartner(PlayerInstance player)
	{
		final int objId = player.getPartnerId();
		if (objId != 0)
		{
			final PlayerInstance partner = World.getInstance().getPlayer(objId);
			if (partner != null)
			{
				partner.sendMessage("Your Partner has logged in.");
			}
		}
	}
	
	/**
	 * @param player
	 */
	private void notifyClanMembers(PlayerInstance player)
	{
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.getClanMember(player.getObjectId()).setPlayerInstance(player);
			
			final SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_HAS_LOGGED_INTO_GAME);
			msg.addString(player.getName());
			clan.broadcastToOtherOnlineMembers(msg, player);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(player), player);
		}
	}
	
	/**
	 * @param player
	 */
	private void notifySponsorOrApprentice(PlayerInstance player)
	{
		if (player.getSponsor() != 0)
		{
			final PlayerInstance sponsor = World.getInstance().getPlayer(player.getSponsor());
			if (sponsor != null)
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
				msg.addString(player.getName());
				sponsor.sendPacket(msg);
			}
		}
		else if (player.getApprentice() != 0)
		{
			final PlayerInstance apprentice = World.getInstance().getPlayer(player.getApprentice());
			if (apprentice != null)
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
				msg.addString(player.getName());
				apprentice.sendPacket(msg);
			}
		}
	}
	
	private void loadTutorial(PlayerInstance player)
	{
		final QuestState qs = player.getQuestState("Q00255_Tutorial");
		if (qs != null)
		{
			qs.getQuest().notifyEvent("UC", null, player);
		}
	}
}
