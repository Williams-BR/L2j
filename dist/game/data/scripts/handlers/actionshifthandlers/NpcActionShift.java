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
package handlers.actionshifthandlers;

import java.util.Set;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;
import org.l2jmobius.gameserver.instancemanager.WalkingManager;
import org.l2jmobius.gameserver.model.Elementals;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Util;

import handlers.bypasshandlers.NpcViewMod;

public class NpcActionShift implements IActionShiftHandler
{
	/**
	 * Manage and Display the GM console to modify the NpcInstance (GM only).<br>
	 * <br>
	 * <b><u>Actions (If the PlayerInstance is a GM only)</u>:</b><br>
	 * <li>Set the NpcInstance as target of the PlayerInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the PlayerInstance player (display the select window)</li>
	 * <li>If NpcInstance is autoAttackable, send a Server->Client packet StatusUpdate to the PlayerInstance in order to update NpcInstance HP bar</li>
	 * <li>Send a Server->Client NpcHtmlMessage() containing the GM console about this NpcInstance</li><br>
	 * <font color=#FF0000><b><u>Caution</u>: Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid that client wait an other packet</b></font><br>
	 * <br>
	 * <b><u>Example of use</u>:</b><br>
	 * <li>Client packet : Action</li>
	 */
	@Override
	public boolean action(PlayerInstance player, WorldObject target, boolean interact)
	{
		// Check if the PlayerInstance is a GM
		if (player.isGM())
		{
			// Set the target of the PlayerInstance player
			player.setTarget(target);
			
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setFile(player, "data/html/admin/npcinfo.htm");
			
			html.replace("%objid%", String.valueOf(target.getObjectId()));
			html.replace("%class%", target.getClass().getSimpleName());
			html.replace("%race%", ((Npc) target).getTemplate().getRace().toString());
			html.replace("%id%", String.valueOf(((Npc) target).getTemplate().getId()));
			html.replace("%lvl%", String.valueOf(((Npc) target).getTemplate().getLevel()));
			html.replace("%name%", ((Npc) target).getTemplate().getName());
			html.replace("%tmplid%", String.valueOf(((Npc) target).getTemplate().getId()));
			html.replace("%aggro%", String.valueOf(target.isAttackable() ? ((Attackable) target).getAggroRange() : 0));
			html.replace("%hp%", String.valueOf((int) ((Creature) target).getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(((Creature) target).getMaxHp()));
			html.replace("%mp%", String.valueOf((int) ((Creature) target).getCurrentMp()));
			html.replace("%mpmax%", String.valueOf(((Creature) target).getMaxMp()));
			
			html.replace("%patk%", String.valueOf((int) ((Creature) target).getPAtk(null)));
			html.replace("%matk%", String.valueOf((int) ((Creature) target).getMAtk(null, null)));
			html.replace("%pdef%", String.valueOf((int) ((Creature) target).getPDef(null)));
			html.replace("%mdef%", String.valueOf((int) ((Creature) target).getMDef(null, null)));
			html.replace("%accu%", String.valueOf(((Creature) target).getAccuracy()));
			html.replace("%evas%", String.valueOf(((Creature) target).getEvasionRate(null)));
			html.replace("%crit%", String.valueOf(((Creature) target).getCriticalHit(null, null)));
			html.replace("%rspd%", String.valueOf((int) ((Creature) target).getRunSpeed()));
			html.replace("%aspd%", String.valueOf(((Creature) target).getPAtkSpd()));
			html.replace("%cspd%", String.valueOf(((Creature) target).getMAtkSpd()));
			html.replace("%atkType%", String.valueOf(((Creature) target).getTemplate().getBaseAttackType()));
			html.replace("%atkRng%", String.valueOf(((Creature) target).getTemplate().getBaseAttackRange()));
			html.replace("%str%", String.valueOf(((Creature) target).getSTR()));
			html.replace("%dex%", String.valueOf(((Creature) target).getDEX()));
			html.replace("%con%", String.valueOf(((Creature) target).getCON()));
			html.replace("%int%", String.valueOf(((Creature) target).getINT()));
			html.replace("%wit%", String.valueOf(((Creature) target).getWIT()));
			html.replace("%men%", String.valueOf(((Creature) target).getMEN()));
			html.replace("%loc%", target.getX() + " " + target.getY() + " " + target.getZ());
			html.replace("%heading%", String.valueOf(((Creature) target).getHeading()));
			html.replace("%collision_radius%", String.valueOf(((Creature) target).getTemplate().getFCollisionRadius()));
			html.replace("%collision_height%", String.valueOf(((Creature) target).getTemplate().getFCollisionHeight()));
			html.replace("%loc2d%", String.valueOf((int) player.calculateDistance2D(target)));
			html.replace("%loc3d%", String.valueOf((int) player.calculateDistance3D(target)));
			
			final byte attackAttribute = ((Creature) target).getAttackElement();
			html.replace("%ele_atk%", Elementals.getElementName(attackAttribute));
			html.replace("%ele_atk_value%", String.valueOf(((Creature) target).getAttackElementValue(attackAttribute)));
			html.replace("%ele_dfire%", String.valueOf(((Creature) target).getDefenseElementValue(Elementals.FIRE)));
			html.replace("%ele_dwater%", String.valueOf(((Creature) target).getDefenseElementValue(Elementals.WATER)));
			html.replace("%ele_dwind%", String.valueOf(((Creature) target).getDefenseElementValue(Elementals.WIND)));
			html.replace("%ele_dearth%", String.valueOf(((Creature) target).getDefenseElementValue(Elementals.EARTH)));
			html.replace("%ele_dholy%", String.valueOf(((Creature) target).getDefenseElementValue(Elementals.HOLY)));
			html.replace("%ele_ddark%", String.valueOf(((Creature) target).getDefenseElementValue(Elementals.DARK)));
			
			if (((Npc) target).getSpawn() != null)
			{
				html.replace("%territory%", ((Npc) target).getSpawn().getSpawnTerritory() == null ? "None" : ((Npc) target).getSpawn().getSpawnTerritory().getName());
				if (((Npc) target).getSpawn().isTerritoryBased())
				{
					html.replace("%spawntype%", "Random");
					final Location spawnLoc = ((Npc) target).getSpawn();
					html.replace("%spawn%", spawnLoc.getX() + " " + spawnLoc.getY() + " " + spawnLoc.getZ());
				}
				else
				{
					html.replace("%spawntype%", "Fixed");
					html.replace("%spawn%", ((Npc) target).getSpawn().getX() + " " + ((Npc) target).getSpawn().getY() + " " + ((Npc) target).getSpawn().getZ());
				}
				if (((Npc) target).getSpawn().getRespawnMinDelay() == 0)
				{
					html.replace("%resp%", "None");
				}
				else if (((Npc) target).getSpawn().hasRespawnRandom())
				{
					html.replace("%resp%", (((Npc) target).getSpawn().getRespawnMinDelay() / 1000) + "-" + (((Npc) target).getSpawn().getRespawnMaxDelay() / 1000) + " sec");
				}
				else
				{
					html.replace("%resp%", (((Npc) target).getSpawn().getRespawnMinDelay() / 1000) + " sec");
				}
				
				final String spawnFile = SpawnTable.getInstance().getSpawnFile(((Npc) target).getSpawn().getNpcSpawnTemplateId());
				html.replace("%spawnfile%", spawnFile.substring(spawnFile.lastIndexOf('\\') + 1));
			}
			else
			{
				html.replace("%spawnfile%", "<font color=FF0000>--</font>");
				html.replace("%territory%", "<font color=FF0000>--</font>");
				html.replace("%spawntype%", "<font color=FF0000>--</font>");
				html.replace("%spawn%", "<font color=FF0000>null</font>");
				html.replace("%resp%", "<font color=FF0000>--</font>");
			}
			
			if (((Npc) target).hasAI())
			{
				final Set<Integer> clans = ((Npc) target).getTemplate().getClans();
				final Set<Integer> ignoreClanNpcIds = ((Npc) target).getTemplate().getIgnoreClanNpcIds();
				final String clansString = clans != null ? Util.implode(clans.toArray(), ", ") : "";
				final String ignoreClanNpcIdsString = ignoreClanNpcIds != null ? Util.implode(ignoreClanNpcIds.toArray(), ", ") : "";
				
				html.replace("%ai_intention%", "<tr><td><table width=270 border=0 bgcolor=131210><tr><td width=100><font color=FFAA00>Intention:</font></td><td align=right width=170>" + ((Npc) target).getAI().getIntention().name() + "</td></tr></table></td></tr>");
				html.replace("%ai%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>AI</font></td><td align=right width=170>" + ((Npc) target).getAI().getClass().getSimpleName() + "</td></tr></table></td></tr>");
				html.replace("%ai_type%", "<tr><td><table width=270 border=0 bgcolor=131210><tr><td width=100><font color=FFAA00>AIType</font></td><td align=right width=170>" + ((Npc) target).getAiType() + "</td></tr></table></td></tr>");
				html.replace("%ai_clan%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>Clan & Range:</font></td><td align=right width=170>" + clansString + " " + ((Npc) target).getTemplate().getClanHelpRange() + "</td></tr></table></td></tr>");
				html.replace("%ai_enemy_clan%", "<tr><td><table width=270 border=0 bgcolor=131210><tr><td width=100><font color=FFAA00>Ignore & Range:</font></td><td align=right width=170>" + ignoreClanNpcIdsString + " " + ((Npc) target).getTemplate().getAggroRange() + "</td></tr></table></td></tr>");
			}
			else
			{
				html.replace("%ai_intention%", "");
				html.replace("%ai%", "");
				html.replace("%ai_type%", "");
				html.replace("%ai_clan%", "");
				html.replace("%ai_enemy_clan%", "");
			}
			
			final String routeName = WalkingManager.getInstance().getRouteName((Npc) target);
			if (!routeName.isEmpty())
			{
				html.replace("%route%", "<tr><td><table width=270 border=0><tr><td width=100><font color=LEVEL>Route:</font></td><td align=right width=170>" + routeName + "</td></tr></table></td></tr>");
			}
			else
			{
				html.replace("%route%", "");
			}
			player.sendPacket(html);
		}
		else if (Config.ALT_GAME_VIEWNPC)
		{
			if (!target.isNpc() || target.isFakePlayer())
			{
				return false;
			}
			player.setTarget(target);
			NpcViewMod.sendNpcView(player, (Npc) target);
		}
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Npc;
	}
}
