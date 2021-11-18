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
package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.instancemanager.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.BuyList;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;

/**
 * Home board.
 * @author Zoey76, Mobius
 */
public class HomeBoard implements IParseBoardHandler
{
	// SQL Queries
	private static final String COUNT_FAVORITES = "SELECT COUNT(*) AS favorites FROM `bbs_favorites` WHERE `playerId`=?";
	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	
	private static final String[] COMMANDS =
	{
		"_bbshome",
		"_bbstop",
	};
	
	private static final String[] CUSTOM_COMMANDS =
	{
		Config.PREMIUM_SYSTEM_ENABLED && Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED ? "_bbspremium" : null,
		Config.COMMUNITYBOARD_ENABLE_MULTISELLS ? "_bbsexcmultisell" : null,
		Config.COMMUNITYBOARD_ENABLE_MULTISELLS ? "_bbsmultisell" : null,
		Config.COMMUNITYBOARD_ENABLE_MULTISELLS ? "_bbssell" : null,
		Config.COMMUNITYBOARD_ENABLE_TELEPORTS ? "_bbsteleport" : null,
		Config.COMMUNITYBOARD_ENABLE_BUFFS ? "_bbsbuff" : null,
		Config.COMMUNITYBOARD_ENABLE_HEAL ? "_bbsheal" : null,
		Config.COMMUNITYBOARD_ENABLE_DELEVEL ? "_bbsdelevel" : null
	};
	
	private static final BiPredicate<String, PlayerInstance> COMBAT_CHECK = (command, player) ->
	{
		boolean commandCheck = false;
		for (String c : CUSTOM_COMMANDS)
		{
			if ((c != null) && command.startsWith(c))
			{
				commandCheck = true;
				break;
			}
		}
		return commandCheck && (player.isCastingNow() || player.isCastingSimultaneouslyNow() || player.isInCombat() || player.isInDuel() || player.isInOlympiadMode() || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP) || (player.getPvpFlag() > 0) || player.isAlikeDead() || player.isOnEvent());
	};
	
	private static final Predicate<PlayerInstance> KARMA_CHECK = player -> Config.COMMUNITYBOARD_KARMA_DISABLED && (player.getKarma() > 0);
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		final List<String> commands = new ArrayList<>();
		commands.addAll(Arrays.asList(COMMANDS));
		commands.addAll(Arrays.asList(CUSTOM_COMMANDS));
		return commands.stream().filter(Objects::nonNull).toArray(String[]::new);
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, PlayerInstance player)
	{
		// Old custom conditions check move to here
		if (COMBAT_CHECK.test(command, player))
		{
			player.sendMessage("You can't use the Community Board right now.");
			return false;
		}
		
		if (KARMA_CHECK.test(player))
		{
			player.sendMessage("Players with Karma cannot use the Community Board.");
			return false;
		}
		
		String returnHtml = null;
		final String navigation = HtmCache.getInstance().getHtm(player, NAVIGATION_PATH);
		if (command.equals("_bbshome") || command.equals("_bbstop"))
		{
			final String customPath = Config.CUSTOM_CB_ENABLED ? "Custom/" : "";
			CommunityBoardHandler.getInstance().addBypass(player, "Home", command);
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + customPath + "home.html");
			if (!Config.CUSTOM_CB_ENABLED)
			{
				returnHtml = returnHtml.replace("%fav_count%", Integer.toString(getFavoriteCount(player)));
				returnHtml = returnHtml.replace("%region_count%", Integer.toString(getRegionCount(player)));
				returnHtml = returnHtml.replace("%clan_count%", Integer.toString(ClanTable.getInstance().getClanCount()));
			}
		}
		else if (command.startsWith("_bbstop;"))
		{
			final String customPath = Config.CUSTOM_CB_ENABLED ? "Custom/" : "";
			final String path = command.replace("_bbstop;", "");
			if ((path.length() > 0) && path.endsWith(".html"))
			{
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + customPath + path);
			}
		}
		else if (command.startsWith("_bbsmultisell"))
		{
			final String fullBypass = command.replace("_bbsmultisell;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int multisellId = Integer.parseInt(buypassOptions[0]);
			final String page = buypassOptions[1];
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
			MultisellData.getInstance().separateAndSend(multisellId, player, null, false);
		}
		else if (command.startsWith("_bbsexcmultisell"))
		{
			final String fullBypass = command.replace("_bbsexcmultisell;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int multisellId = Integer.parseInt(buypassOptions[0]);
			final String page = buypassOptions[1];
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
			MultisellData.getInstance().separateAndSend(multisellId, player, null, true);
		}
		else if (command.startsWith("_bbssell"))
		{
			final String page = command.replace("_bbssell;", "");
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
			player.sendPacket(new BuyList(BuyListData.getInstance().getBuyList(423), player.getAdena(), 0));
			player.sendPacket(new ExBuySellList(player, false));
		}
		else if (command.startsWith("_bbsteleport"))
		{
			final String teleBuypass = command.replace("_bbsteleport;", "");
			if (player.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < Config.COMMUNITYBOARD_TELEPORT_PRICE)
			{
				player.sendMessage("Not enough currency!");
			}
			else if (Config.COMMUNITY_AVAILABLE_TELEPORTS.get(teleBuypass) != null)
			{
				player.disableAllSkills();
				player.sendPacket(new ShowBoard());
				player.destroyItemByItemId("CB_Teleport", Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_TELEPORT_PRICE, player, true);
				player.setIn7sDungeon(false);
				player.setInstanceId(0);
				player.teleToLocation(Config.COMMUNITY_AVAILABLE_TELEPORTS.get(teleBuypass), 0);
				ThreadPool.schedule(player::enableAllSkills, 3000);
			}
		}
		else if (command.startsWith("_bbsbuff"))
		{
			final String fullBypass = command.replace("_bbsbuff;", "");
			final String[] buypassOptions = fullBypass.split(";");
			final int buffCount = buypassOptions.length - 1;
			final String page = buypassOptions[buffCount];
			if (player.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < (Config.COMMUNITYBOARD_BUFF_PRICE * buffCount))
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				player.destroyItemByItemId("CB_Buff", Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_BUFF_PRICE * buffCount, player, true);
				final Summon pet = player.getSummon();
				final List<Creature> targets = new ArrayList<>(4);
				targets.add(player);
				if (pet != null)
				{
					targets.add(pet);
				}
				
				for (int i = 0; i < buffCount; i++)
				{
					final Skill skill = SkillData.getInstance().getSkill(Integer.parseInt(buypassOptions[i].split(",")[0]), Integer.parseInt(buypassOptions[i].split(",")[1]));
					if (!Config.COMMUNITY_AVAILABLE_BUFFS.contains(skill.getId()))
					{
						continue;
					}
					for (Creature target : targets)
					{
						skill.applyEffects(player, target);
						if (Config.COMMUNITYBOARD_CAST_ANIMATIONS)
						{
							player.sendPacket(new MagicSkillUse(player, target, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
							// not recommend broadcast
							// player.broadcastPacket(new MagicSkillUse(player, target, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
						}
					}
				}
			}
			
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
		}
		else if (command.startsWith("_bbsheal"))
		{
			final String page = command.replace("_bbsheal;", "");
			if (player.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < (Config.COMMUNITYBOARD_HEAL_PRICE))
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				player.destroyItemByItemId("CB_Heal", Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_HEAL_PRICE, player, true);
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				if (player.hasSummon())
				{
					player.getSummon().setCurrentHp(player.getSummon().getMaxHp());
					player.getSummon().setCurrentMp(player.getSummon().getMaxMp());
					player.getSummon().setCurrentCp(player.getSummon().getMaxCp());
				}
				player.sendMessage("You used heal!");
			}
			
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/" + page + ".html");
		}
		else if (command.equals("_bbsdelevel"))
		{
			if (player.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < Config.COMMUNITYBOARD_DELEVEL_PRICE)
			{
				player.sendMessage("Not enough currency!");
			}
			else if (player.getLevel() == 1)
			{
				player.sendMessage("You are at minimum level!");
			}
			else
			{
				player.destroyItemByItemId("CB_Delevel", Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_DELEVEL_PRICE, player, true);
				final int newLevel = player.getLevel() - 1;
				player.setExp(ExperienceData.getInstance().getExpForLevel(newLevel));
				player.getStat().setLevel((byte) newLevel);
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				player.broadcastUserInfo();
				player.checkPlayerSkills(); // Adjust skills according to new level.
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/delevel/complete.html");
			}
		}
		else if (command.startsWith("_bbspremium"))
		{
			final String fullBypass = command.replace("_bbspremium;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int premiumDays = Integer.parseInt(buypassOptions[0]);
			if (player.getInventory().getInventoryItemCount(Config.COMMUNITY_PREMIUM_COIN_ID, -1) < (Config.COMMUNITY_PREMIUM_PRICE_PER_DAY * premiumDays))
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				player.destroyItemByItemId("CB_Premium", Config.COMMUNITY_PREMIUM_COIN_ID, Config.COMMUNITY_PREMIUM_PRICE_PER_DAY * premiumDays, player, true);
				PremiumManager.getInstance().addPremiumTime(player.getAccountName(), premiumDays, TimeUnit.DAYS);
				player.sendMessage("Your account will now have premium status until " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(PremiumManager.getInstance().getPremiumExpiration(player.getAccountName())) + ".");
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/premium/thankyou.html");
			}
		}
		
		if (returnHtml != null)
		{
			if (Config.CUSTOM_CB_ENABLED)
			{
				returnHtml = returnHtml.replace("%navigation%", navigation);
			}
			CommunityBoardHandler.separateAndSend(returnHtml, player);
		}
		return false;
	}
	
	/**
	 * Gets the Favorite links for the given player.
	 * @param player the player
	 * @return the favorite links count
	 */
	private static int getFavoriteCount(PlayerInstance player)
	{
		int count = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(COUNT_FAVORITES))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					count = rs.getInt("favorites");
				}
			}
		}
		catch (Exception e)
		{
			LOG.warning(FavoriteBoard.class.getSimpleName() + ": Coudn't load favorites count for player " + player.getName());
		}
		return count;
	}
	
	/**
	 * Gets the registered regions count for the given player.
	 * @param player the player
	 * @return the registered regions count
	 */
	private static int getRegionCount(PlayerInstance player)
	{
		return 0; // TODO: Implement.
	}
}
