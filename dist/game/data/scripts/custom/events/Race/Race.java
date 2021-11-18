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
package custom.events.Race;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Event;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skills.AbnormalType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Gnacik
 */
public class Race extends Event
{
	// Event NPCs list
	private final Set<Npc> _npcs = ConcurrentHashMap.newKeySet();
	// Npc
	private Npc _npc;
	// Player list
	private final Set<PlayerInstance> _players = ConcurrentHashMap.newKeySet();
	// Event Task
	ScheduledFuture<?> _eventTask = null;
	// Event state
	private static boolean _isactive = false;
	// Race state
	private static boolean _isRaceStarted = false;
	// 5 min for register
	private static final int REGISTER_TIME = 5;
	// 5 min for race
	private static final int RACE_TIME = 10;
	// NPCs
	private static final int START_NPC = 900103;
	private static final int STOP_NPC = 900104;
	// Skills (Frog by default)
	private static int _skill = 6201;
	// We must keep second NPC spawn for radar
	private static int[] _randspawn = null;
	// Locations
	private static final String[] _locations =
	{
		"Heretic catacomb enterance",
		"Dion castle bridge",
		"Floran village enterance",
		"Floran fort gate"
	};
	
	// @formatter:off
	private static final int[][] _coords =
	{
		// x, y, z, heading
		{ 39177, 144345, -3650, 0 },
		{ 22294, 155892, -2950, 0 },
		{ 16537, 169937, -3500, 0 },
		{  7644, 150898, -2890, 0 }
	};
	private static final int[][] _rewards =
	{
		{ 6622, 2 }, // Giant's Codex
		{ 9625, 2 }, // Giant's Codex -
		{ 9626, 2 }, // Giant's Codex -
		{ 9627, 2 }, // Giant's Codex -
		{ 9546, 5 }, // Attr stones
		{ 9547, 5 },
		{ 9548, 5 },
		{ 9549, 5 },
		{ 9550, 5 },
		{ 9551, 5 },
		{ 9574, 3 }, // Mid-Grade Life Stone: level 80
		{ 9575, 2 }, // High-Grade Life Stone: level 80
		{ 9576, 1 }, // Top-Grade Life Stone: level 80
		{ 20034,1 }  // Revita pop
	};
	// @formatter:on
	
	private Race()
	{
		addStartNpc(START_NPC);
		addFirstTalkId(START_NPC);
		addTalkId(START_NPC);
		addStartNpc(STOP_NPC);
		addFirstTalkId(STOP_NPC);
		addTalkId(STOP_NPC);
	}
	
	@Override
	public boolean eventStart(PlayerInstance eventMaker)
	{
		// Don't start event if its active
		if (_isactive)
		{
			return false;
		}
		
		// Check Custom Table - we use custom NPCs
		if (!Config.CUSTOM_NPC_DATA)
		{
			LOGGER.info(getName() + ": Event can't be started, because custom npc table is disabled!");
			eventMaker.sendMessage("Event " + getName() + " can't be started because custom NPC table is disabled!");
			return false;
		}
		
		// Set Event active
		_isactive = true;
		// Spawn Manager
		_npc = recordSpawn(START_NPC, 18429, 145861, -3090, 0, false, 0);
		
		// Announce event start
		Broadcast.toAllOnlinePlayers("* Race Event started! *");
		Broadcast.toAllOnlinePlayers("Visit Event Manager in Dion village and signup, you have " + REGISTER_TIME + " min before Race Start...");
		
		// Schedule Event end
		_eventTask = ThreadPool.schedule(this::StartRace, REGISTER_TIME * 60 * 1000);
		return true;
	}
	
	protected void StartRace()
	{
		// Abort race if no players signup
		if (_players.isEmpty())
		{
			Broadcast.toAllOnlinePlayers("Race aborted, nobody signup.");
			eventStop();
			return;
		}
		// Set state
		_isRaceStarted = true;
		// Announce
		Broadcast.toAllOnlinePlayers("Race started!");
		// Get random Finish
		final int location = getRandom(0, _locations.length - 1);
		_randspawn = _coords[location];
		// And spawn NPC
		recordSpawn(STOP_NPC, _randspawn[0], _randspawn[1], _randspawn[2], _randspawn[3], false, 0);
		// Transform players and send message
		for (PlayerInstance player : _players)
		{
			if (player.isOnline())
			{
				if (player.isInsideRadius2D(_npc, 500))
				{
					sendMessage(player, "Race started! Go find Finish NPC as fast as you can... He is located near " + _locations[location]);
					transformPlayer(player);
					player.getRadar().addMarker(_randspawn[0], _randspawn[1], _randspawn[2]);
				}
				else
				{
					sendMessage(player, "I told you stay near me right? Distance was too high, you are excluded from race");
					_players.remove(player);
				}
			}
		}
		// Schedule timeup for Race
		_eventTask = ThreadPool.schedule(this::timeUp, RACE_TIME * 60 * 1000);
	}
	
	@Override
	public boolean eventStop()
	{
		// Don't stop inactive event
		if (!_isactive)
		{
			return false;
		}
		
		// Set inactive
		_isactive = false;
		_isRaceStarted = false;
		
		// Cancel task if any
		if (_eventTask != null)
		{
			_eventTask.cancel(true);
			_eventTask = null;
		}
		// Untransform players
		// Teleport to event start point
		for (PlayerInstance player : _players)
		{
			if (player.isOnline())
			{
				player.untransform();
				player.teleToLocation(_npc.getX(), _npc.getY(), _npc.getZ(), true);
			}
		}
		_players.clear();
		// Despawn NPCs
		for (Npc npc : _npcs)
		{
			npc.deleteMe();
		}
		_npcs.clear();
		// Announce event end
		Broadcast.toAllOnlinePlayers("* Race Event finished *");
		return true;
	}
	
	@Override
	public boolean eventBypass(PlayerInstance player, String bypass)
	{
		if (bypass.startsWith("skill"))
		{
			if (_isRaceStarted)
			{
				player.sendMessage("Race already started, you cannot change transform skill now");
			}
			else
			{
				final int number = Integer.parseInt(bypass.substring(5));
				final Skill skill = SkillData.getInstance().getSkill(number, 1);
				if (skill != null)
				{
					_skill = number;
					player.sendMessage("Transform skill set to:");
					player.sendMessage(skill.getName());
				}
				else
				{
					player.sendMessage("Error while changing transform skill");
				}
			}
		}
		else if (bypass.startsWith("tele"))
		{
			if ((Integer.parseInt(bypass.substring(4)) > 0) && (_randspawn != null))
			{
				player.teleToLocation(_randspawn[0], _randspawn[1], _randspawn[2]);
			}
			else
			{
				player.teleToLocation(18429, 145861, -3090);
			}
		}
		showMenu(player);
		return true;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("transform"))
		{
			transformPlayer(player);
			return null;
		}
		else if (event.equalsIgnoreCase("untransform"))
		{
			player.untransform();
			return null;
		}
		else if (event.equalsIgnoreCase("showfinish"))
		{
			player.getRadar().addMarker(_randspawn[0], _randspawn[1], _randspawn[2]);
			return null;
		}
		else if (event.equalsIgnoreCase("signup"))
		{
			if (_players.contains(player))
			{
				return "900103-onlist.htm";
			}
			_players.add(player);
			return "900103-signup.htm";
		}
		else if (event.equalsIgnoreCase("quit"))
		{
			player.untransform();
			if (_players.contains(player))
			{
				_players.remove(player);
			}
			return "900103-quit.htm";
		}
		else if (event.equalsIgnoreCase("finish"))
		{
			if (player.isAffectedBySkill(_skill))
			{
				winRace(player);
				return "900104-winner.htm";
			}
			return "900104-notrans.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		getQuestState(player, true);
		if (npc.getId() == START_NPC)
		{
			if (_isRaceStarted)
			{
				return START_NPC + "-started-" + isRacing(player) + ".htm";
			}
			return START_NPC + "-" + isRacing(player) + ".htm";
		}
		else if ((npc.getId() == STOP_NPC) && _isRaceStarted)
		{
			return STOP_NPC + "-" + isRacing(player) + ".htm";
		}
		return npc.getId() + ".htm";
	}
	
	private int isRacing(PlayerInstance player)
	{
		return _players.contains(player) ? 1 : 0;
	}
	
	private Npc recordSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffSet, long despawnDelay)
	{
		final Npc npc = addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay);
		_npcs.add(npc);
		return npc;
	}
	
	private void transformPlayer(PlayerInstance player)
	{
		if (player.isTransformed() || player.isInStance())
		{
			player.untransform();
		}
		if (player.isSitting())
		{
			player.standUp();
		}
		
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.SPEED_UP);
		player.stopSkillEffects(SkillFinishType.REMOVED, 268);
		player.stopSkillEffects(SkillFinishType.REMOVED, 298); // Rabbit Spirit Totem
		SkillData.getInstance().getSkill(_skill, 1).applyEffects(player, player);
	}
	
	private void sendMessage(PlayerInstance player, String text)
	{
		player.sendPacket(new CreatureSay(_npc, ChatType.BATTLEFIELD, _npc.getName(), text));
	}
	
	private void showMenu(PlayerInstance player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		final String content = getHtm(player, "admin_menu.htm");
		html.setHtml(content);
		player.sendPacket(html);
	}
	
	protected void timeUp()
	{
		Broadcast.toAllOnlinePlayers("Time up, nobody wins!");
		eventStop();
	}
	
	private void winRace(PlayerInstance player)
	{
		final int[] _reward = _rewards[getRandom(_rewards.length - 1)];
		player.addItem("eventModRace", _reward[0], _reward[1], _npc, true);
		Broadcast.toAllOnlinePlayers(player.getName() + " is a winner!");
		eventStop();
	}
	
	public static void main(String[] args)
	{
		new Race();
	}
}