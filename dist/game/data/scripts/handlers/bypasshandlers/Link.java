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
package handlers.bypasshandlers;

import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.TeleporterInstance;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Link implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Link"
	};
	
	private static final String[] VALID_LINKS =
	{
		"adventurer_guildsman/AboutHighLevelGuilds.htm",
		"adventurer_guildsman/AboutNewLifeCrystals.htm",
		"clanHallDoorman/evolve.htm",
		"common/augmentation_01.htm",
		"common/augmentation_02.htm",
		"common/crafting_01.htm",
		"common/duals_01.htm",
		"common/duals_02.htm",
		"common/duals_03.htm",
		"common/g_cube_warehouse001.htm",
		"common/skill_enchant_help.htm",
		"common/skill_enchant_help_01.htm",
		"common/skill_enchant_help_02.htm",
		"common/skill_enchant_help_03.htm",
		"common/weapon_sa_01.htm",
		"common/welcomeback002.htm",
		"common/welcomeback003.htm",
		"default/BlessingOfProtection.htm",
		"default/SupportMagic.htm",
		"default/SupportMagicServitor.htm",
		"fisherman/fishing_championship.htm",
		"fortress/foreman.htm",
		"guard/kamaloka_help.htm",
		"guard/kamaloka_level.htm",
		"olympiad/hero_main2.htm",
		"petmanager/evolve.htm",
		"petmanager/exchange.htm",
		"petmanager/instructions.htm",
		"seven_signs/blkmrkt_1.htm",
		"seven_signs/blkmrkt_2.htm",
		"seven_signs/mammblack_1a.htm",
		"seven_signs/mammblack_1b.htm",
		"seven_signs/mammblack_1c.htm",
		"seven_signs/mammblack_2a.htm",
		"seven_signs/mammblack_2b.htm",
		"seven_signs/mammmerch_1.htm",
		"seven_signs/mammmerch_1a.htm",
		"seven_signs/mammmerch_1b.htm",
		"teleporter/separatedsoul.htm",
		"warehouse/clanwh.htm",
		"warehouse/privatewh.htm",
	};
	
	@Override
	public boolean useBypass(String command, PlayerInstance player, Creature target)
	{
		final String htmlPath = command.substring(4).trim();
		if (htmlPath.isEmpty())
		{
			LOGGER.warning("Player " + player.getName() + " sent empty link html!");
			return false;
		}
		
		if (htmlPath.contains(".."))
		{
			LOGGER.warning("Player " + player.getName() + " sent invalid link html: " + htmlPath);
			return false;
		}
		
		String content = CommonUtil.contains(VALID_LINKS, htmlPath) ? HtmCache.getInstance().getHtm(player, "data/html/" + htmlPath) : null;
		// Precaution.
		if (htmlPath.startsWith("teleporter/") && !(player.getTarget() instanceof TeleporterInstance))
		{
			content = null;
		}
		final NpcHtmlMessage html = new NpcHtmlMessage(target != null ? target.getObjectId() : 0);
		if (content != null)
		{
			html.setHtml(content.replace("%objectId%", String.valueOf(target != null ? target.getObjectId() : 0)));
		}
		player.sendPacket(html);
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
