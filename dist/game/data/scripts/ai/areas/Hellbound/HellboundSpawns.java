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
package ai.areas.Hellbound;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;

/**
 * Hellbound Spawns parser.
 * @author Zoey76
 */
public class HellboundSpawns implements IXmlReader
{
	private final List<Spawn> _spawns = new ArrayList<>();
	private final Map<Spawn, int[]> _spawnLevels = new HashMap<>();
	
	public HellboundSpawns()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_spawns.clear();
		_spawnLevels.clear();
		parseDatapackFile("data/scripts/ai/areas/Hellbound/hellboundSpawns.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _spawns.size() + " Hellbound spawns.");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node node = doc.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equals(node.getNodeName()))
			{
				for (Node npc = node.getFirstChild(); npc != null; npc = npc.getNextSibling())
				{
					parseSpawn(npc);
				}
			}
		}
	}
	
	/**
	 * Parses the spawn.
	 * @param npc the NPC to parse
	 */
	private void parseSpawn(Node npc)
	{
		if ("npc".equals(npc.getNodeName()))
		{
			final Node id = npc.getAttributes().getNamedItem("id");
			if (id == null)
			{
				LOGGER.severe(getClass().getSimpleName() + ":  Missing NPC ID, skipping record!");
				return;
			}
			
			final int npcId = Integer.parseInt(id.getNodeValue());
			Location loc = null;
			int delay = 0;
			int randomInterval = 0;
			int minLevel = 1;
			int maxLevel = 100;
			for (Node element = npc.getFirstChild(); element != null; element = element.getNextSibling())
			{
				if (element.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				final NamedNodeMap attrs = element.getAttributes();
				minLevel = 1;
				maxLevel = 100;
				switch (element.getNodeName())
				{
					case "location":
					{
						loc = new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"), parseInteger(attrs, "heading", 0));
						break;
					}
					case "respawn":
					{
						delay = parseInteger(attrs, "delay");
						randomInterval = attrs.getNamedItem("randomInterval") != null ? parseInteger(attrs, "randomInterval") : 1;
						break;
					}
					case "hellboundLevel":
					{
						minLevel = parseInteger(attrs, "min", 1);
						maxLevel = parseInteger(attrs, "max", 100);
						break;
					}
				}
			}
			
			try
			{
				final Spawn spawn = new Spawn(npcId);
				spawn.setAmount(1);
				if (loc == null)
				{
					LOGGER.warning("Hellbound spawn location is null!");
				}
				spawn.setLocation(loc);
				spawn.setRespawnDelay(delay, randomInterval);
				_spawnLevels.put(spawn, new int[]
				{
					minLevel,
					maxLevel
				});
				SpawnTable.getInstance().addNewSpawn(spawn, false);
				_spawns.add(spawn);
			}
			catch (SecurityException | ClassNotFoundException | NoSuchMethodException e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Couldn't load spawns: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Gets all Hellbound spawns.
	 * @return the list of Hellbound spawns.
	 */
	public List<Spawn> getSpawns()
	{
		return _spawns;
	}
	
	/**
	 * Gets the spawn minimum level.
	 * @param spawn the NPC Spawn
	 * @return the spawn minimum level
	 */
	public int getSpawnMinLevel(Spawn spawn)
	{
		return _spawnLevels.containsKey(spawn) ? _spawnLevels.get(spawn)[0] : 1;
	}
	
	/**
	 * Gets the spawn maximum level.
	 * @param spawn the NPC Spawn
	 * @return the spawn maximum level
	 */
	public int getSpawnMaxLevel(Spawn spawn)
	{
		return _spawnLevels.containsKey(spawn) ? _spawnLevels.get(spawn)[1] : 1;
	}
	
	public static HellboundSpawns getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HellboundSpawns INSTANCE = new HellboundSpawns();
	}
}