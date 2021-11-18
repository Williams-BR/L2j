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
package quests.Q00511_AwlUnderFoot;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.RaidBossInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.Util;

/**
 * Awl Under Foot (511)
 * @author Gigiikun
 */
public class Q00511_AwlUnderFoot extends Quest
{
	protected class FAUWorld extends InstanceWorld
	{
	}
	
	public static class FortDungeon
	{
		private final int INSTANCEID;
		private long _reEnterTime = 0;
		
		public FortDungeon(int iId)
		{
			INSTANCEID = iId;
		}
		
		public int getInstanceId()
		{
			return INSTANCEID;
		}
		
		public long getReEnterTime()
		{
			return _reEnterTime;
		}
		
		public void setReEnterTime(long time)
		{
			_reEnterTime = time;
		}
	}
	
	private class spawnRaid implements Runnable
	{
		private final FAUWorld _world;
		
		public spawnRaid(FAUWorld world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			try
			{
				int spawnId;
				if (_world.getStatus() == 0)
				{
					spawnId = RAIDS1[getRandom(RAIDS1.length)];
				}
				else if (_world.getStatus() == 1)
				{
					spawnId = RAIDS2[getRandom(RAIDS2.length)];
				}
				else
				{
					spawnId = RAIDS3[getRandom(RAIDS3.length)];
				}
				final Npc raid = addSpawn(spawnId, 53319, 245814, -6576, 0, false, 0, false, _world.getInstanceId());
				if (raid instanceof RaidBossInstance)
				{
					((RaidBossInstance) raid).setUseRaidCurse(false);
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("Fortress AwlUnderFoot Raid Spawn error: " + e);
			}
		}
	}
	
	private static final boolean DEBUG = false;
	private static final long REENTERTIME = 14400000;
	
	private static final long RAID_SPAWN_DELAY = 120000;
	
	private final Map<Integer, FortDungeon> _fortDungeons = new HashMap<>(21);
	// QUEST ITEMS
	private static final int DL_MARK = 9797;
	// REWARDS
	private static final int KNIGHT_EPALUETTE = 9912;
	// MONSTER TO KILL -- Only last 3 Raids (lvl ordered) give DL_MARK
	protected static final int[] RAIDS1 =
	{
		25572,
		25575,
		25578
	};
	protected static final int[] RAIDS2 =
	{
		25579,
		25582,
		25585,
		25588
	};
	protected static final int[] RAIDS3 =
	{
		25589,
		25592,
		25593
	};
	
	// Skill
	private static final SkillHolder RAID_CURSE = new SkillHolder(5456, 1);
	
	public Q00511_AwlUnderFoot()
	{
		super(511);
		_fortDungeons.put(35666, new FortDungeon(22));
		_fortDungeons.put(35698, new FortDungeon(23));
		_fortDungeons.put(35735, new FortDungeon(24));
		_fortDungeons.put(35767, new FortDungeon(25));
		_fortDungeons.put(35804, new FortDungeon(26));
		_fortDungeons.put(35835, new FortDungeon(27));
		_fortDungeons.put(35867, new FortDungeon(28));
		_fortDungeons.put(35904, new FortDungeon(29));
		_fortDungeons.put(35936, new FortDungeon(30));
		_fortDungeons.put(35974, new FortDungeon(31));
		_fortDungeons.put(36011, new FortDungeon(32));
		_fortDungeons.put(36043, new FortDungeon(33));
		_fortDungeons.put(36081, new FortDungeon(34));
		_fortDungeons.put(36118, new FortDungeon(35));
		_fortDungeons.put(36149, new FortDungeon(36));
		_fortDungeons.put(36181, new FortDungeon(37));
		_fortDungeons.put(36219, new FortDungeon(38));
		_fortDungeons.put(36257, new FortDungeon(39));
		_fortDungeons.put(36294, new FortDungeon(40));
		_fortDungeons.put(36326, new FortDungeon(41));
		_fortDungeons.put(36364, new FortDungeon(42));
		
		for (int i : _fortDungeons.keySet())
		{
			addStartNpc(i);
			addTalkId(i);
		}
		
		addKillId(RAIDS1);
		addKillId(RAIDS2);
		addKillId(RAIDS3);
		
		for (int i = 25572; i <= 25595; i++)
		{
			addAttackId(i);
		}
	}
	
	private String checkConditions(PlayerInstance player)
	{
		if (DEBUG)
		{
			return null;
		}
		final Party party = player.getParty();
		if (party == null)
		{
			return "FortressWarden-03.htm";
		}
		if (party.getLeader() != player)
		{
			return getHtm(player, "FortressWarden-04.htm").replace("%leader%", party.getLeader().getName());
		}
		for (PlayerInstance partyMember : party.getMembers())
		{
			final QuestState qs = getQuestState(partyMember, false);
			if ((qs == null) || (qs.getInt("cond") < 1))
			{
				return getHtm(player, "FortressWarden-05.htm").replace("%player%", partyMember.getName());
			}
			if (!Util.checkIfInRange(1000, player, partyMember, true))
			{
				return getHtm(player, "FortressWarden-06.htm").replace("%player%", partyMember.getName());
			}
		}
		return null;
	}
	
	private String checkFortCondition(PlayerInstance player, Npc npc, boolean isEnter)
	{
		final Fort fortress = npc.getFort();
		final FortDungeon dungeon = _fortDungeons.get(npc.getId());
		if ((player == null) || (fortress == null) || (dungeon == null))
		{
			return "FortressWarden-01.htm";
		}
		if ((player.getClan() == null) || (player.getClan().getFortId() != fortress.getResidenceId()))
		{
			return "FortressWarden-01.htm";
		}
		else if (fortress.getFortState() == 0)
		{
			return "FortressWarden-02a.htm";
		}
		else if (fortress.getFortState() == 2)
		{
			return "FortressWarden-02b.htm";
		}
		else if (isEnter && (dungeon.getReEnterTime() > Chronos.currentTimeMillis()))
		{
			return "FortressWarden-07.htm";
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			return "FortressWarden-03.htm";
		}
		for (PlayerInstance partyMember : party.getMembers())
		{
			if ((partyMember.getClan() == null) || (partyMember.getClan().getFortId() == 0) || (partyMember.getClan().getFortId() != fortress.getResidenceId()))
			{
				return getHtm(player, "FortressWarden-05.htm").replace("%player%", partyMember.getName());
			}
		}
		return null;
	}
	
	protected String enterInstance(PlayerInstance player, int[] coords, FortDungeon dungeon, String retValue)
	{
		// check for existing instances for this player
		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		// existing instance
		if (world != null)
		{
			if (!(world instanceof FAUWorld))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON);
				return "";
			}
			teleportPlayer(player, coords, world.getInstanceId());
			return "";
		}
		
		// New instance
		if (retValue != null)
		{
			return retValue;
		}
		
		final String ret = checkConditions(player);
		if (ret != null)
		{
			return ret;
		}
		
		final Party party = player.getParty();
		world = new FAUWorld();
		final Instance instance = InstanceManager.getInstance().createDynamicInstance(dungeon.getInstanceId());
		world.setInstance(instance);
		instance.setExitLoc(new Location(player));
		dungeon.setReEnterTime(Chronos.currentTimeMillis() + REENTERTIME);
		InstanceManager.getInstance().addWorld(world);
		final int instanceId = world.getInstanceId();
		LOGGER.info("Fortress AwlUnderFoot started " + dungeon.getInstanceId() + " Instance: " + instanceId + " created by player: " + player.getName());
		ThreadPool.schedule(new spawnRaid((FAUWorld) world), RAID_SPAWN_DELAY);
		
		// teleport players
		if (player.getParty() == null)
		{
			teleportPlayer(player, coords, instanceId);
			world.addAllowed(player);
		}
		else
		{
			for (PlayerInstance partyMember : party.getMembers())
			{
				teleportPlayer(partyMember, coords, instanceId);
				world.addAllowed(partyMember);
				getQuestState(partyMember, true);
			}
		}
		return getHtm(player, "FortressWarden-08.htm").replace("%clan%", player.getClan().getName());
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final String htmltext = event;
		if (event.equalsIgnoreCase("enter"))
		{
			final int[] tele = new int[3];
			tele[0] = 53322;
			tele[1] = 246380;
			tele[2] = -6580;
			return enterInstance(player, tele, _fortDungeons.get(npc.getId()), checkFortCondition(player, npc, true));
		}
		final QuestState qs = getQuestState(player, true);
		if (event.equalsIgnoreCase("FortressWarden-10.htm"))
		{
			if (qs.isCond(0))
			{
				qs.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("FortressWarden-15.htm"))
		{
			qs.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance player, int damage, boolean isSummon)
	{
		final Playable attacker = (isSummon ? player.getSummon() : player);
		if ((attacker.getLevel() - npc.getLevel()) >= 9)
		{
			if ((attacker.getBuffCount() > 0) || (attacker.getDanceCount() > 0))
			{
				npc.setTarget(attacker);
				npc.doSimultaneousCast(RAID_CURSE.getSkill());
			}
			else if (player.getParty() != null)
			{
				for (PlayerInstance pmember : player.getParty().getMembers())
				{
					if ((pmember.getBuffCount() > 0) || (pmember.getDanceCount() > 0))
					{
						npc.setTarget(pmember);
						npc.doSimultaneousCast(RAID_CURSE.getSkill());
					}
				}
			}
		}
		return super.onAttack(npc, player, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc);
		if (tmpworld instanceof FAUWorld)
		{
			final FAUWorld world = (FAUWorld) tmpworld;
			if (CommonUtil.contains(RAIDS3, npc.getId()))
			{
				if (player.getParty() != null)
				{
					for (PlayerInstance pl : player.getParty().getMembers())
					{
						rewardPlayer(pl);
					}
				}
				else
				{
					rewardPlayer(player);
				}
				
				final Instance instanceObj = InstanceManager.getInstance().getInstance(world.getInstanceId());
				instanceObj.setDuration(360000);
				instanceObj.removeNpcs();
			}
			else
			{
				world.incStatus();
				ThreadPool.schedule(new spawnRaid(world), RAID_SPAWN_DELAY);
			}
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		final String ret = checkFortCondition(player, npc, false);
		String htmltext = getNoQuestMsg(player);
		if (ret != null)
		{
			return ret;
		}
		else if (qs != null)
		{
			final int npcId = npc.getId();
			int cond = 0;
			if (qs.getState() == State.CREATED)
			{
				qs.set("cond", "0");
			}
			else
			{
				cond = qs.getInt("cond");
			}
			if (_fortDungeons.containsKey(npcId) && (cond == 0))
			{
				if (player.getLevel() >= 60)
				{
					htmltext = "FortressWarden-09.htm";
				}
				else
				{
					htmltext = "FortressWarden-00.htm";
					qs.exitQuest(true);
				}
			}
			else if (_fortDungeons.containsKey(npcId) && (cond > 0) && (qs.getState() == State.STARTED))
			{
				final long count = getQuestItemsCount(player, DL_MARK);
				if ((cond == 1) && (count > 0))
				{
					htmltext = "FortressWarden-14.htm";
					takeItems(player, DL_MARK, -1);
					rewardItems(player, KNIGHT_EPALUETTE, count);
				}
				else if ((cond == 1) && (count == 0))
				{
					htmltext = "FortressWarden-10.htm";
				}
			}
		}
		return htmltext;
	}
	
	private void rewardPlayer(PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs.isCond(1))
		{
			giveItems(player, DL_MARK, 140);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
	
	private void teleportPlayer(PlayerInstance player, int[] coords, int instanceId)
	{
		player.setInstanceId(instanceId);
		player.teleToLocation(coords[0], coords[1], coords[2]);
	}
}
