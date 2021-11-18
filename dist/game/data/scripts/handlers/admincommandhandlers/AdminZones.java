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
package handlers.admincommandhandlers;

import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.PlayerAction;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.PageResult;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Priority;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerDlgAnswer;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerMoveRequest;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.form.ZoneNPoly;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;
import org.l2jmobius.gameserver.network.serverpackets.ExShowTerritory;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.BuilderUtil;
import org.l2jmobius.gameserver.util.HtmlUtil;
import org.l2jmobius.gameserver.util.Util;

import ai.AbstractNpcAI;

/**
 * @author UnAfraid
 */
public class AdminZones extends AbstractNpcAI implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminZones.class.getName());
	
	private final Map<Integer, ZoneNodeHolder> _zones = new ConcurrentHashMap<>();
	
	private static final String[] COMMANDS =
	{
		"admin_zones",
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		switch (cmd)
		{
			case "admin_zones":
			{
				if (!st.hasMoreTokens())
				{
					buildZonesEditorWindow(activeChar);
					return false;
				}
				final String subCmd = st.nextToken();
				switch (subCmd)
				{
					case "load":
					{
						if (st.hasMoreTokens())
						{
							String name = "";
							while (st.hasMoreTokens())
							{
								name += st.nextToken() + " ";
							}
							loadZone(activeChar, name.trim());
						}
						break;
					}
					case "create":
					{
						buildHtmlWindow(activeChar, 0);
						break;
					}
					case "setname":
					{
						String name = "";
						while (st.hasMoreTokens())
						{
							name += st.nextToken() + " ";
						}
						if (!name.isEmpty())
						{
							name = name.substring(0, name.length() - 1);
						}
						setName(activeChar, name);
						break;
					}
					case "start":
					{
						enablePicking(activeChar);
						break;
					}
					case "finish":
					{
						disablePicking(activeChar);
						break;
					}
					case "show":
					{
						showPoints(activeChar);
						final ConfirmDlg dlg = new ConfirmDlg("When enable show territory you must restart client to remove it, are you sure about that?");
						dlg.addTime(15 * 1000);
						activeChar.sendPacket(dlg);
						activeChar.addAction(PlayerAction.ADMIN_SHOW_TERRITORY);
						break;
					}
					case "hide":
					{
						final ZoneNodeHolder holder = _zones.get(activeChar.getObjectId());
						if (holder != null)
						{
							final ExServerPrimitive exsp = new ExServerPrimitive("DebugPoint_" + activeChar.getObjectId(), activeChar.getX(), activeChar.getY(), activeChar.getZ());
							exsp.addPoint(Color.BLACK, 0, 0, 0);
							activeChar.sendPacket(exsp);
						}
						break;
					}
					case "change":
					{
						if (!st.hasMoreTokens())
						{
							BuilderUtil.sendSysMessage(activeChar, "Missing node index!");
							break;
						}
						final String indexToken = st.nextToken();
						if (!Util.isDigit(indexToken))
						{
							BuilderUtil.sendSysMessage(activeChar, "Node index should be int!");
							break;
						}
						final int index = Integer.parseInt(indexToken);
						changePoint(activeChar, index);
						break;
					}
					case "delete":
					{
						if (!st.hasMoreTokens())
						{
							BuilderUtil.sendSysMessage(activeChar, "Missing node index!");
							break;
						}
						final String indexToken = st.nextToken();
						if (!Util.isDigit(indexToken))
						{
							BuilderUtil.sendSysMessage(activeChar, "Node index should be int!");
							break;
						}
						final int index = Integer.parseInt(indexToken);
						deletePoint(activeChar, index);
						showPoints(activeChar);
						break;
					}
					case "clear":
					{
						_zones.remove(activeChar.getObjectId());
						break;
					}
					case "dump":
					{
						dumpPoints(activeChar);
						break;
					}
					case "list":
					{
						final int page = Util.parseNextInt(st, 0);
						buildHtmlWindow(activeChar, page);
						return false;
					}
				}
				break;
			}
		}
		buildHtmlWindow(activeChar, 0);
		return false;
	}
	
	private void buildZonesEditorWindow(PlayerInstance activeChar)
	{
		final StringBuilder sb = new StringBuilder();
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(activeChar);
		for (ZoneType zone : zones)
		{
			if (zone.getZone() instanceof ZoneNPoly)
			{
				sb.append("<tr>");
				sb.append("<td fixwidth=200><a action=\"bypass -h admin_zones load " + zone.getName() + "\">" + zone.getName() + "</a></td>");
				sb.append("</tr>");
			}
		}
		
		final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setFile(activeChar, "data/html/admin/zone_editor.htm");
		msg.replace("%zones%", sb.toString());
		activeChar.sendPacket(msg);
	}
	
	/**
	 * @param activeChar
	 * @param zoneName
	 */
	private void loadZone(PlayerInstance activeChar, String zoneName)
	{
		BuilderUtil.sendSysMessage(activeChar, "Searching for zone: " + zoneName);
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(activeChar);
		ZoneType zoneType = null;
		for (ZoneType zone : zones)
		{
			if (zone.getName().equalsIgnoreCase(zoneName))
			{
				zoneType = zone;
				BuilderUtil.sendSysMessage(activeChar, "Zone found: " + zone.getId());
				break;
			}
		}
		
		if ((zoneType != null) && (zoneType.getZone() instanceof ZoneNPoly))
		{
			final ZoneNPoly zone = (ZoneNPoly) zoneType.getZone();
			final ZoneNodeHolder holder = _zones.computeIfAbsent(activeChar.getObjectId(), val -> new ZoneNodeHolder());
			holder.getNodes().clear();
			holder.setName(zoneType.getName());
			for (int i = 0; i < zone.getX().length; i++)
			{
				final int x = zone.getX()[i];
				final int y = zone.getY()[i];
				holder.addNode(new Location(x, y, GeoEngine.getInstance().getHeight(x, y, Rnd.get(zone.getLowZ(), zone.getHighZ()))));
			}
			showPoints(activeChar);
		}
	}
	
	/**
	 * @param activeChar
	 * @param name
	 */
	private void setName(PlayerInstance activeChar, String name)
	{
		if (name.contains("<") || name.contains(">") || name.contains("&") || name.contains("\\") || name.contains("\"") || name.contains("$"))
		{
			BuilderUtil.sendSysMessage(activeChar, "You cannot use symbols like: < > & \" $ \\");
			return;
		}
		_zones.computeIfAbsent(activeChar.getObjectId(), key -> new ZoneNodeHolder()).setName(name);
	}
	
	/**
	 * @param activeChar
	 */
	private void enablePicking(PlayerInstance activeChar)
	{
		if (!activeChar.hasAction(PlayerAction.ADMIN_POINT_PICKING))
		{
			activeChar.addAction(PlayerAction.ADMIN_POINT_PICKING);
			BuilderUtil.sendSysMessage(activeChar, "Point picking mode activated!");
		}
		else
		{
			BuilderUtil.sendSysMessage(activeChar, "Point picking mode is already activated!");
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void disablePicking(PlayerInstance activeChar)
	{
		if (activeChar.removeAction(PlayerAction.ADMIN_POINT_PICKING))
		{
			BuilderUtil.sendSysMessage(activeChar, "Point picking mode deactivated!");
		}
		else
		{
			BuilderUtil.sendSysMessage(activeChar, "Point picking mode was not activated!");
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void showPoints(PlayerInstance activeChar)
	{
		final ZoneNodeHolder holder = _zones.get(activeChar.getObjectId());
		if (holder != null)
		{
			if (holder.getNodes().size() < 3)
			{
				BuilderUtil.sendSysMessage(activeChar, "In order to visualize this zone you must have at least 3 points.");
				return;
			}
			final ExServerPrimitive exsp = new ExServerPrimitive("DebugPoint_" + activeChar.getObjectId(), activeChar.getX(), activeChar.getY(), activeChar.getZ());
			int index = 1;
			for (Location loc : holder.getNodes())
			{
				exsp.addPoint("Point: " + index, Color.GREEN, true, loc);
				index++;
			}
			final List<Location> list = holder.getNodes();
			for (int i = 1; i < list.size(); i++)
			{
				final Location prevLoc = list.get(i - 1);
				final Location nextLoc = list.get(i);
				exsp.addLine("Point " + i + " > " + (i + 1), Color.WHITE, true, prevLoc, nextLoc);
			}
			exsp.addLine("Point " + list.size() + " > 1", Color.WHITE, true, list.get(list.size() - 1), list.get(0));
			activeChar.sendPacket(exsp);
		}
	}
	
	/**
	 * @param activeChar
	 * @param index
	 */
	private void changePoint(PlayerInstance activeChar, int index)
	{
		final ZoneNodeHolder holder = _zones.get(activeChar.getObjectId());
		if (holder != null)
		{
			final Location loc = holder.getNodes().get(index);
			if (loc != null)
			{
				enablePicking(activeChar);
				holder.setChangingLoc(loc);
			}
		}
	}
	
	/**
	 * @param activeChar
	 * @param index
	 */
	private void deletePoint(PlayerInstance activeChar, int index)
	{
		final ZoneNodeHolder holder = _zones.get(activeChar.getObjectId());
		if (holder != null)
		{
			final Location loc = holder.getNodes().get(index);
			if (loc != null)
			{
				holder.getNodes().remove(loc);
				BuilderUtil.sendSysMessage(activeChar, "Node " + index + " has been removed!");
				if (holder.getNodes().isEmpty())
				{
					BuilderUtil.sendSysMessage(activeChar, "Since node list is empty destroying session!");
					_zones.remove(activeChar.getObjectId());
				}
			}
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void dumpPoints(PlayerInstance activeChar)
	{
		final ZoneNodeHolder holder = _zones.get(activeChar.getObjectId());
		if ((holder != null) && !holder.getNodes().isEmpty())
		{
			if (holder.getName().isEmpty())
			{
				BuilderUtil.sendSysMessage(activeChar, "Set name first!");
				return;
			}
			
			final Location firstNode = holder.getNodes().get(0);
			final StringBuilder sb = new StringBuilder();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + Config.EOL);
			sb.append("<list enabled=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../../data/xsd/zones.xsd\">" + Config.EOL);
			sb.append("\t<zone name=\"" + holder.getName() + "\" type=\"ScriptZone\" shape=\"NPoly\" minZ=\"" + (firstNode.getZ() - 100) + "\" maxZ=\"" + (firstNode.getZ() + 100) + "\">" + Config.EOL);
			for (Location loc : holder.getNodes())
			{
				sb.append("\t\t<node X=\"" + loc.getX() + "\" Y=\"" + loc.getY() + "\" />" + Config.EOL);
			}
			sb.append("\t</zone>" + Config.EOL);
			sb.append("</list>" + Config.EOL);
			try
			{
				File file = new File(Config.DATAPACK_ROOT, "log/points/" + activeChar.getAccountName() + "/" + holder.getName() + ".xml");
				if (file.exists())
				{
					int i = 0;
					while ((file = new File(Config.DATAPACK_ROOT, "log/points/" + activeChar.getAccountName() + "/" + holder.getName() + i + ".xml")).exists())
					{
						i++;
					}
				}
				if (!file.getParentFile().isDirectory())
				{
					file.getParentFile().mkdirs();
				}
				Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
				BuilderUtil.sendSysMessage(activeChar, "Successfully written on: " + file.getAbsolutePath().replace(Config.DATAPACK_ROOT.getAbsolutePath(), ""));
			}
			catch (Exception e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Failed writing the dump: " + e.getMessage());
				LOGGER.log(Level.WARNING, "Failed writing point picking dump for " + activeChar.getName() + ":" + e.getMessage(), e);
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_MOVE_REQUEST)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	@Priority(Integer.MAX_VALUE)
	public TerminateReturn onPlayerPointPicking(OnPlayerMoveRequest event)
	{
		final PlayerInstance player = event.getPlayer();
		if (player.hasAction(PlayerAction.ADMIN_POINT_PICKING))
		{
			final Location newLocation = event.getLocation();
			final ZoneNodeHolder holder = _zones.computeIfAbsent(player.getObjectId(), key -> new ZoneNodeHolder());
			final Location changeLog = holder.getChangingLoc();
			if (changeLog != null)
			{
				changeLog.setXYZ(newLocation);
				holder.setChangingLoc(null);
				BuilderUtil.sendSysMessage(player, "Location " + (holder.indexOf(changeLog) + 1) + " has been updated!");
				disablePicking(player);
			}
			else
			{
				holder.addNode(newLocation);
				BuilderUtil.sendSysMessage(player, "Location " + (holder.indexOf(changeLog) + 1) + " has been added!");
			}
			// Auto visualization when nodes >= 3
			if (holder.getNodes().size() >= 3)
			{
				showPoints(player);
			}
			buildHtmlWindow(player, 0);
			
			return new TerminateReturn(true, true, false);
		}
		return null;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_DLG_ANSWER)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerDlgAnswer(OnPlayerDlgAnswer event)
	{
		final PlayerInstance player = event.getPlayer();
		if (player.removeAction(PlayerAction.ADMIN_SHOW_TERRITORY) && (event.getAnswer() == 1))
		{
			final ZoneNodeHolder holder = _zones.get(player.getObjectId());
			if (holder != null)
			{
				final List<Location> list = holder.getNodes();
				if (list.size() < 3)
				{
					BuilderUtil.sendSysMessage(player, "You must have at least 3 nodes to use this option!");
					return;
				}
				
				final Location firstLoc = list.get(0);
				final ExShowTerritory exst = new ExShowTerritory(firstLoc.getZ() - 100, firstLoc.getZ() + 100);
				list.forEach(exst::addVertice);
				player.sendPacket(exst);
				BuilderUtil.sendSysMessage(player, "In order to remove the debug you must restart your game client!");
			}
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return COMMANDS;
	}
	
	private void buildHtmlWindow(PlayerInstance activeChar, int page)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setFile(activeChar, "data/html/admin/zone_editor_create.htm");
		final ZoneNodeHolder holder = _zones.computeIfAbsent(activeChar.getObjectId(), key -> new ZoneNodeHolder());
		final AtomicInteger position = new AtomicInteger(page * 20);
		final PageResult result = HtmlUtil.createPage(holder.getNodes(), page, 20, i ->
		{
			return "<td align=center><button action=\"bypass -h admin_zones list " + i + "\" value=\"" + (i + 1) + "\" width=30 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
		}, loc ->
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("<tr>");
			sb.append("<td fixwidth=5></td>");
			sb.append("<td fixwidth=20>" + position.getAndIncrement() + "</td>");
			sb.append("<td fixwidth=60>" + loc.getX() + "</td>");
			sb.append("<td fixwidth=60>" + loc.getY() + "</td>");
			sb.append("<td fixwidth=60>" + loc.getZ() + "</td>");
			sb.append("<td fixwidth=30><a action=\"bypass -h admin_zones change " + holder.indexOf(loc) + "\">[E]</a></td>");
			sb.append("<td fixwidth=30><a action=\"bypass -h admin_move_to " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + "\">[T]</a></td>");
			sb.append("<td fixwidth=30><a action=\"bypass -h admin_zones delete " + holder.indexOf(loc) + "\">[D]</a></td>");
			sb.append("<td fixwidth=5></td>");
			sb.append("</tr>");
			return sb.toString();
		});
		msg.replace("%name%", holder.getName());
		msg.replace("%pages%", result.getPagerTemplate());
		msg.replace("%nodes%", result.getBodyTemplate());
		activeChar.sendPacket(msg);
	}
	
	protected class ZoneNodeHolder
	{
		private String _name = "";
		private Location _changingLoc = null;
		private final List<Location> _nodes = new ArrayList<>();
		
		public void setName(String name)
		{
			_name = name;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public void setChangingLoc(Location loc)
		{
			_changingLoc = loc;
		}
		
		public Location getChangingLoc()
		{
			return _changingLoc;
		}
		
		public void addNode(Location loc)
		{
			_nodes.add(loc);
		}
		
		public List<Location> getNodes()
		{
			return _nodes;
		}
		
		public int indexOf(Location loc)
		{
			return _nodes.indexOf(loc);
		}
	}
}
