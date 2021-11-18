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
package quests.Q00727_HopeWithinTheDarkness;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.instancemanager.GlobalVariablesManager;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.QuestGuardInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

/**
 * Hope within the Darkness (727)
 * @author GKR, Pandragon
 */
public class Q00727_HopeWithinTheDarkness extends Quest
{
	protected class CAUWorld extends InstanceWorld
	{
		public boolean underAttack = false;
		public boolean allMonstersDead = true;
	}
	
	public static class CastleDungeon
	{
		private final int INSTANCEID;
		private final int _wardenId;
		
		public CastleDungeon(int iId, int wardenId)
		{
			INSTANCEID = iId;
			_wardenId = wardenId;
		}
		
		public int getInstanceId()
		{
			return INSTANCEID;
		}
		
		public long getReEnterTime()
		{
			long reEnterTime = 0;
			if (GlobalVariablesManager.getInstance().hasVariable("CastlePailaka " + _wardenId))
			{
				reEnterTime = GlobalVariablesManager.getInstance().getLong("CastlePailaka " + _wardenId);
			}
			else
			{
				GlobalVariablesManager.getInstance().set("CastlePailaka " + _wardenId, 0);
			}
			return reEnterTime;
		}
		
		public void setReEnterTime(long time)
		{
			GlobalVariablesManager.getInstance().set("CastlePailaka " + _wardenId, time);
		}
	}
	
	// NPCs
	private static final int NPC_KNIGHT = 36562;
	private static final int NPC_RANGER = 36563;
	private static final int NPC_MAGE = 36564;
	private static final int NPC_WARRIOR = 36565;
	
	private static final int[] BOSSES =
	{
		25653,
		25654,
		25655
	};
	private static final int[] MONSTERS =
	{
		25656,
		25657,
		25658
	};
	
	// Reward
	private static final int KNIGHT_EPALUETTE = 9912;
	
	// Skills
	private static Map<Integer, SkillHolder> NPC_BUFFS = new HashMap<>();
	static
	{
		NPC_BUFFS.put(NPC_KNIGHT, new SkillHolder(5970, 1));
		NPC_BUFFS.put(NPC_RANGER, new SkillHolder(5971, 1));
		NPC_BUFFS.put(NPC_MAGE, new SkillHolder(5972, 1));
		NPC_BUFFS.put(NPC_WARRIOR, new SkillHolder(5973, 1));
	}
	private static final SkillHolder RAID_CURSE = new SkillHolder(5456, 1);
	
	// Strings
	protected static final NpcStringId STRINGID_WIN = NpcStringId.YOU_VE_DONE_IT_WE_BELIEVED_IN_YOU_WARRIOR_WE_WANT_TO_SHOW_OUR_SINCERITY_THOUGH_IT_IS_SMALL_PLEASE_GIVE_ME_SOME_OF_YOUR_TIME;
	private static final NpcStringId STRINGID_WELCOME = NpcStringId.WARRIORS_HAVE_YOU_COME_TO_HELP_THOSE_WHO_ARE_IMPRISONED_HERE;
	private static final NpcStringId STRINGID_BOSS_DEATH = NpcStringId.HOW_DARE_YOU;
	private static final NpcStringId[] STRINGID_INJURED =
	{
		NpcStringId.YOUR_MIND_IS_GOING_BLANK,
		NpcStringId.UGH_IT_HURTS_DOWN_TO_THE_BONES
	};
	private static final NpcStringId[] STRINGID_DIE =
	{
		NpcStringId.I_CAN_T_STAND_IT_ANYMORE_AAH,
		NpcStringId.KYAAAK,
		NpcStringId.GASP_HOW_CAN_THIS_BE
	};
	private static final NpcStringId[] STRINGID_BOSS_SPAWN =
	{
		NpcStringId.I_LL_RIP_THE_FLESH_FROM_YOUR_BONES,
		NpcStringId.YOU_LL_FLOUNDER_IN_DELUSION_FOR_THE_REST_OF_YOUR_LIFE,
		NpcStringId.THERE_IS_NO_ESCAPE_FROM_THIS_PLACE
	};
	
	// Misc
	private static final boolean DEBUG = false;
	private static final int MIN_LEVEL = 80;
	private static final boolean CHECK_FOR_CONTRACT = true;
	private static final long REENTER_INTERVAL = 14400000;
	private static final long INITIAL_SPAWN_DELAY = 120000; // Spawn NPCs and 1st Wave bosses (2 min)
	private static final long WAVE_SPAWN_DELAY = 480000; // Spawn next wave's bosses (8 min)
	private static final long PRIVATE_SPAWN_DELAY = 180000; // Spawn monsters (3 min after boss had been spawned)
	private static final Location PLAYER_SPAWN_POINT = new Location(48163, -12195, -9140);
	private static final Map<Integer, CastleDungeon> CASTLE_DUNGEONS = new HashMap<>();
	static
	{
		CASTLE_DUNGEONS.put(36403, new CastleDungeon(80, 36403));
		CASTLE_DUNGEONS.put(36404, new CastleDungeon(81, 36404));
		CASTLE_DUNGEONS.put(36405, new CastleDungeon(82, 36405));
		CASTLE_DUNGEONS.put(36406, new CastleDungeon(83, 36406));
		CASTLE_DUNGEONS.put(36407, new CastleDungeon(84, 36407));
		CASTLE_DUNGEONS.put(36408, new CastleDungeon(85, 36408));
		CASTLE_DUNGEONS.put(36409, new CastleDungeon(86, 36409));
		CASTLE_DUNGEONS.put(36410, new CastleDungeon(87, 36410));
		CASTLE_DUNGEONS.put(36411, new CastleDungeon(88, 36411));
	}
	
	public Q00727_HopeWithinTheDarkness()
	{
		super(727);
		
		for (int i : CASTLE_DUNGEONS.keySet())
		{
			addStartNpc(i);
			addTalkId(i);
		}
		
		for (int i = NPC_KNIGHT; i <= NPC_WARRIOR; i++)
		{
			addSpawnId(i);
			addKillId(i);
			addAttackId(i);
			addTalkId(i);
			addFirstTalkId(i);
		}
		
		for (int i : BOSSES)
		{
			addSpawnId(i);
			addKillId(i);
			addAttackId(i);
		}
		
		for (int i : MONSTERS)
		{
			addKillId(i);
			addAttackId(i);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final String htmltext = event;
		if (event.equalsIgnoreCase("enter"))
		{
			return enterInstance(player, "CastlePailaka.xml", PLAYER_SPAWN_POINT, CASTLE_DUNGEONS.get(npc.getId()), checkEnterConditions(player, npc));
		}
		else if (event.equalsIgnoreCase("suicide"))
		{
			final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc);
			tmpworld.setStatus(5);
			final Instance inst = InstanceManager.getInstance().getInstance(npc.getInstanceId());
			if (inst != null)
			{
				for (Npc _npc : inst.getNpcs())
				{
					if ((_npc != null) && ((_npc.getId() >= NPC_KNIGHT) && (_npc.getId() <= NPC_WARRIOR)))
					{
						cancelQuestTimer("check_for_foes", _npc, null);
						cancelQuestTimer("buff", _npc, null);
						if (!_npc.isDead())
						{
							_npc.doDie(null);
						}
					}
				}
				
				// Destroy instance after 5 minutes
				inst.setDuration(5 * 60000);
				inst.setEmptyDestroyTime(0);
			}
			
			return null;
		}
		else if (event.equalsIgnoreCase("buff"))
		{
			for (PlayerInstance pl : World.getInstance().getVisibleObjects(npc, PlayerInstance.class))
			{
				if ((pl != null) && Util.checkIfInRange(75, npc, pl, false) && (NPC_BUFFS.get(npc.getId()) != null))
				{
					npc.setTarget(pl);
					npc.doCast(NPC_BUFFS.get(npc.getId()).getSkill());
				}
			}
			startQuestTimer("buff", 120000, npc, null);
			return null;
		}
		else if (event.equalsIgnoreCase("check_for_foes"))
		{
			if (npc.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				for (Creature foe : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, npc.getAggroRange()))
				{
					if (foe.isAttackable() && !(foe instanceof QuestGuardInstance))
					{
						((QuestGuardInstance) npc).addDamageHate(foe, 0, 999);
						((QuestGuardInstance) npc).getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, foe, null);
					}
				}
			}
			
			startQuestTimer("check_for_foes", 5000, npc, null);
			return null;
		}
		
		QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			qs = newQuestState(player);
		}
		
		final int cond = qs.getCond();
		if (event.equalsIgnoreCase("CastleWarden-05.htm"))
		{
			if (cond == 0)
			{
				qs.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("CastleWarden-15.htm"))
		{
			qs.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(player);
		if (tmpworld instanceof CAUWorld)
		{
			final CAUWorld world = (CAUWorld) tmpworld;
			if (world.underAttack)
			{
				return "Victim-02.html";
			}
			else if (world.getStatus() == 4)
			{
				return "Victim-03.html";
			}
			else
			{
				return "Victim-01.html";
			}
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		if ((npc.getId() >= NPC_KNIGHT) && (npc.getId() <= NPC_WARRIOR))
		{
			final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(player);
			if (tmpworld instanceof CAUWorld)
			{
				final CAUWorld world = (CAUWorld) tmpworld;
				world.removeAllowed(player);
				final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
				final Location loc = inst.getExitLoc();
				teleportPlayer(player, loc, 0);
				return null;
			}
		}
		
		if ((player.getClan() == null) || (npc.getCastle() == null) || (player.getClan().getCastleId() != npc.getCastle().getResidenceId()))
		{
			return "CastleWarden-03.html";
		}
		else if (qs != null)
		{
			final int npcId = npc.getId();
			int cond = 0;
			if (qs.isCreated())
			{
				qs.setCond(0);
			}
			else
			{
				cond = qs.getCond();
			}
			if (CASTLE_DUNGEONS.containsKey(npcId) && (cond == 0))
			{
				if (player.getLevel() >= 80)
				{
					htmltext = "CastleWarden-01.htm";
				}
				else
				{
					htmltext = "CastleWarden-04.html";
					qs.exitQuest(true);
				}
			}
			else if (CASTLE_DUNGEONS.containsKey(npcId) && (cond > 0) && (qs.isStarted()))
			{
				if (cond == 1)
				{
					htmltext = "CastleWarden-15.htm";
				}
				else if (cond == 3)
				{
					rewardItems(player, KNIGHT_EPALUETTE, 159);
					qs.exitQuest(true, true);
					htmltext = "CastleWarden-16.html";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance player, int damage, boolean isSummon)
	{
		if ((npc.getId() >= NPC_KNIGHT) && (npc.getId() <= NPC_WARRIOR))
		{
			if (npc.getCurrentHp() <= (npc.getMaxHp() * 0.1))
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), STRINGID_INJURED[1]));
			}
			else if (npc.getCurrentHp() <= (npc.getMaxHp() * 0.4))
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), STRINGID_INJURED[0]));
			}
			return null;
		}
		
		if (player != null)
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
		}
		return super.onAttack(npc, player, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		if ((npc.getId() >= NPC_KNIGHT) && (npc.getId() <= NPC_WARRIOR))
		{
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), STRINGID_DIE[npc.getId() - 36562]));
			
			// All other friendly NPCs do suicide - start timer
			startQuestTimer("suicide", 1500, npc, null);
			cancelQuestTimer("check_for_foes", npc, null);
			cancelQuestTimer("buff", npc, null);
			return null;
		}
		
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc);
		if (tmpworld instanceof CAUWorld)
		{
			final CAUWorld world = (CAUWorld) tmpworld;
			if (CommonUtil.contains(BOSSES, npc.getId()))
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), STRINGID_BOSS_DEATH));
			}
			
			if ((tmpworld.getStatus() == 3) && (CommonUtil.contains(BOSSES, npc.getId()) || CommonUtil.contains(MONSTERS, npc.getId())))
			{
				world.allMonstersDead = true;
				final Instance inst = InstanceManager.getInstance().getInstance(tmpworld.getInstanceId());
				if (inst != null)
				{
					for (Npc _npc : inst.getNpcs())
					{
						if ((_npc != null) && !_npc.isDead() && (CommonUtil.contains(BOSSES, _npc.getId()) || CommonUtil.contains(MONSTERS, _npc.getId())))
						{
							world.allMonstersDead = false;
							break;
						}
					}
					
					if (world.allMonstersDead)
					{
						tmpworld.setStatus(4);
						
						// Destroy instance after 5 minutes
						inst.setDuration(5 * 60000);
						inst.setEmptyDestroyTime(0);
						ThreadPool.schedule(new completeDungeon(world, player), 1500);
					}
				}
			}
		}
		
		return null;
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		// Buff players every two minutes, check for foes in aggro range
		if ((npc.getId() >= NPC_KNIGHT) && (npc.getId() <= NPC_WARRIOR))
		{
			startQuestTimer("buff", 120000, npc, null);
			startQuestTimer("check_for_foes", 120000, npc, null);
			if (npc.getId() == NPC_KNIGHT)
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), STRINGID_WELCOME));
			}
		}
		else if (Arrays.binarySearch(BOSSES, npc.getId()) >= 0)
		{
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), STRINGID_BOSS_SPAWN[Arrays.binarySearch(BOSSES, npc.getId())]));
		}
		return null;
	}
	
	private String checkEnterConditions(PlayerInstance player, Npc npc)
	{
		if (DEBUG)
		{
			return null;
		}
		
		final Castle castle = npc.getCastle();
		final CastleDungeon dungeon = CASTLE_DUNGEONS.get(npc.getId());
		boolean haveContract = false;
		if ((player == null) || (castle == null) || (dungeon == null))
		{
			return "CastleWarden-03.html";
		}
		
		// Check if castle have contract with fortress
		if (CHECK_FOR_CONTRACT)
		{
			for (Fort fort : FortManager.getInstance().getForts())
			{
				if (fort.getContractedCastleId() == castle.getResidenceId())
				{
					haveContract = true;
					break;
				}
			}
			
			if (!haveContract)
			{
				return "CastleWarden-13a.html";
			}
		}
		
		QuestState qs = player.getQuestState(getName());
		
		// Check if player has quest
		if ((qs == null) || qs.isCreated())
		{
			// Check if player is from clan, that owns castle
			if ((player.getClan() == null) || (player.getClan().getCastleId() != castle.getResidenceId()))
			{
				return "CastleWarden-08.html";
			}
			
			if (player.getLevel() >= MIN_LEVEL)
			{
				return "CastleWarden-06.htm";
			}
			
			return "CastleWarden-07.html";
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			return "CastleWarden-09.html";
		}
		
		if (party.getLeader() != player)
		{
			return getHtm(player, "CastleWarden-10.html").replace("%leader%", party.getLeader().getName());
		}
		
		for (PlayerInstance partyMember : party.getMembers())
		{
			qs = partyMember.getQuestState(getName());
			
			// Check if each party member has quest
			if ((qs == null) || qs.isCreated())
			{
				return getHtm(player, "CastleWarden-12.html").replace("%player%", partyMember.getName());
			}
			
			if ((player.getClan() == null) || (player.getClan().getCastleId() != castle.getResidenceId()))
			{
				return getHtm(player, "CastleWarden-11.html").replace("%player%", partyMember.getName());
			}
			
			// Check if each party member not very far from leader
			if (!Util.checkIfInRange(1000, player, partyMember, true))
			{
				return getHtm(player, "CastleWarden-17.html").replace("%player%", partyMember.getName());
			}
		}
		
		if (dungeon.getReEnterTime() > Chronos.currentTimeMillis())
		{
			return "CastleWarden-18.html";
		}
		
		return null;
	}
	
	protected String enterInstance(PlayerInstance player, String template, Location coords, CastleDungeon dungeon, String ret)
	{
		// Check for existing instances for this player
		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		// Existing instance
		if (world != null)
		{
			if (!(world instanceof CAUWorld))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON));
				return "";
			}
			teleportPlayer(player, coords, world.getInstanceId());
			return "";
		}
		// New instance
		if (ret != null)
		{
			return ret;
		}
		
		final Party party = player.getParty();
		world = new CAUWorld();
		final Instance instance = InstanceManager.getInstance().createDynamicInstance(dungeon.getInstanceId());
		world.setInstance(instance);
		instance.setExitLoc(new Location(player));
		final int instanceId = world.getInstanceId();
		dungeon.setReEnterTime(Chronos.currentTimeMillis() + REENTER_INTERVAL);
		InstanceManager.getInstance().addWorld(world);
		ThreadPool.schedule(new spawnNpcs((CAUWorld) world), INITIAL_SPAWN_DELAY);
		if (DEBUG)
		{
			LOGGER.info("Castle HopeWithinTheDarkness started " + template + " Instance: " + instanceId + " created by player: " + player.getName());
		}
		
		// Teleport players
		if (player.getParty() == null)
		{
			return "CastleWarden-09.html";
		}
		for (PlayerInstance partyMember : party.getMembers())
		{
			teleportPlayer(partyMember, coords, instanceId);
			world.addAllowed(partyMember);
			if (partyMember.getQuestState(getName()) == null)
			{
				newQuestState(partyMember);
			}
			
			partyMember.getQuestState(getName()).setCond(2);
		}
		return getHtm(player, "CastleWarden-13.html").replace("%clan%", player.getClan().getName());
	}
	
	// Spawns npcs and bosses
	private class spawnNpcs implements Runnable
	{
		private final CAUWorld _world;
		
		public spawnNpcs(CAUWorld world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			try
			{
				final Instance instance = InstanceManager.getInstance().getInstance(_world.getInstanceId());
				if (_world.getStatus() == 0)
				{
					instance.spawnGroup("victims");
					instance.spawnGroup("bosses_1");
					
					ThreadPool.schedule(new spawnNpcs(_world), WAVE_SPAWN_DELAY);
					ThreadPool.schedule(new spawnPrivates(_world), PRIVATE_SPAWN_DELAY);
				}
				else if (_world.getStatus() == 1)
				{
					instance.spawnGroup("bosses_2");
					
					ThreadPool.schedule(new spawnNpcs(_world), WAVE_SPAWN_DELAY);
					ThreadPool.schedule(new spawnPrivates(_world), PRIVATE_SPAWN_DELAY);
				}
				else if (_world.getStatus() == 2)
				{
					instance.spawnGroup("bosses_3");
					
					ThreadPool.schedule(new spawnPrivates(_world), PRIVATE_SPAWN_DELAY);
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("Castle HopeWithinTheDarkness NPC Spawn error: " + e);
			}
		}
	}
	
	// Spawns monsters (minions)
	private class spawnPrivates implements Runnable
	{
		private final CAUWorld _world;
		
		public spawnPrivates(CAUWorld world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			try
			{
				final Instance instance = InstanceManager.getInstance().getInstance(_world.getInstanceId());
				if (_world.getStatus() == 0)
				{
					instance.spawnGroup("monsters_first_wave");
					
					_world.underAttack = true;
				}
				else if (_world.getStatus() == 1)
				{
					instance.spawnGroup("monsters_second_wave");
				}
				else if (_world.getStatus() == 2)
				{
					instance.spawnGroup("monsters_third_wave");
				}
				
				_world.setStatus(_world.getStatus() + 1);
			}
			catch (Exception e)
			{
				LOGGER.warning("Castle HopeWithinTheDarkness Monster Spawn error: " + e);
			}
		}
	}
	
	// Manages complete dungeon event
	private class completeDungeon implements Runnable
	{
		private final CAUWorld _world;
		private final PlayerInstance _player;
		
		public completeDungeon(CAUWorld world, PlayerInstance player)
		{
			_world = world;
			_player = player;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_world.getStatus() == 4)
				{
					_world.underAttack = false;
					
					final Instance inst = InstanceManager.getInstance().getInstance(_world.getInstanceId());
					for (Npc _npc : inst.getNpcs())
					{
						if ((_npc != null) && ((_npc.getId() >= NPC_KNIGHT) && (_npc.getId() <= NPC_WARRIOR)))
						{
							cancelQuestTimer("check_for_foes", _npc, null);
							cancelQuestTimer("buff", _npc, null);
							if (_npc.getId() == NPC_KNIGHT)
							{
								_npc.broadcastPacket(new NpcSay(_npc.getObjectId(), ChatType.SHOUT, _npc.getId(), STRINGID_WIN));
							}
						}
					}
					
					if (_player != null)
					{
						final Party party = _player.getParty();
						if (party == null)
						{
							rewardPlayer(_player);
						}
						else
						{
							for (PlayerInstance partyMember : party.getMembers())
							{
								if ((partyMember != null) && (partyMember.getInstanceId() == _player.getInstanceId()))
								{
									rewardPlayer(partyMember);
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("Complete dungeon manage error: " + e);
			}
		}
	}
	
	protected void rewardPlayer(PlayerInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		if ((qs != null) && (qs.isCond(2)))
		{
			qs.setCond(3);
		}
	}
}
