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
package instances.NornilsGarden;

import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

import instances.AbstractInstance;
import quests.Q00179_IntoTheLargeCavern.Q00179_IntoTheLargeCavern;

/**
 * Nornil's Garden instance zone.
 * @author Gnacik
 * @version 2010-10-15 Based on official server Naia
 */
public class NornilsGarden extends AbstractInstance
{
	protected class NornilsWorld extends InstanceWorld
	{
		protected Npc first_npc = null;
		protected boolean spawned_1 = false;
		protected boolean spawned_2 = false;
		protected boolean spawned_3 = false;
		protected boolean spawned_4 = false;
	}
	
	// NPCs
	private static final int GARDEN_GUARD = 32330;
	private static final int[] FINAL_GATES =
	{
		32260,
		32261,
		32262
	};
	// Skills
	private static final Skill SKILL_1 = SkillData.getInstance().getSkill(4322, 1);
	private static final Skill SKILL_2 = SkillData.getInstance().getSkill(4327, 1);
	private static final Skill SKILL_3 = SkillData.getInstance().getSkill(4329, 1);
	private static final Skill SKILL_4 = SkillData.getInstance().getSkill(4324, 1);
	// Locations
	private static final Location SPAWN_PPL = new Location(-111184, 74540, -12430);
	private static final Location EXIT_PPL = new Location(-74058, 52040, -3680);
	// Misc
	private static final int TEMPLATE_ID = 11;
	private static final int DURATION_TIME = 70;
	private static final int EMPTY_DESTROY_TIME = 5;
	private static final int INSTANCE_LEVEL_MIN = 18;
	private static final int INSTANCE_LEVEL_MAX = 22;
	private static final int[][] AUTO_GATES =
	{
		// Warriors gate
		{
			20110,
			16200001
		},
		// Midway gate
		{
			20111,
			16200004
		},
		// Gate
		{
			20112,
			16200013
		}
	};
	private static final int HERB_JAR = 18478;
	// @formatter:off
	private static final int[][] GATEKEEPERS =
	{
		{ 18352, 9703, 0 }, // Kamael Guard
		{ 18353, 9704, 0 }, // Guardian of Records
		{ 18354, 9705, 0 }, // Guardian of Observation
		{ 18355, 9706, 0 }, // Spicula's Guard
		{ 18356, 9707, 16200024 }, // Harkilgamed's Gatekeeper
		{ 18357, 9708, 16200025 }, // Rodenpicula's Gatekeeper
		{ 18358, 9713, 0 }, // Guardian of Secrets
		{ 18359, 9709, 16200023 }, // Arviterre's Guardian
		{ 18360, 9710, 0 }, // Katenar's Gatekeeper
		{ 18361, 9711, 0 }, // Guardian of Prediction
		{ 25528, 9712, 0 }  // Tiberias
	};
	private static final int[][] HP_HERBS_DROPLIST =
	{
		// itemId, count, chance
		{ 8602, 1, 10 },
		{ 8601, 2, 40 },
		{ 8600, 3, 70 }
	};
	private static final int[][] GROUP_1 = 
	{
		{ 18363, -109899, 74431, -12528, 16488 },
		{ 18483, -109701, 74501, -12528, 24576 },
		{ 18483, -109892, 74886, -12528,     0 },
		{ 18363, -109703, 74879, -12528, 49336 }
	};
	private static final int[][] GROUP_2 = 
	{
		{ 18363, -110393, 78276, -12848, 49152 },
		{ 18363, -110561, 78276, -12848, 49152 },
		{ 18362, -110414, 78495, -12905, 48112 },
		{ 18362, -110545, 78489, -12903, 48939 },
		{ 18483, -110474, 78601, -12915, 49488 },
		{ 18362, -110474, 78884, -12915, 49338 },
		{ 18483, -110389, 79131, -12915, 48539 },
		{ 18483, -110551, 79134, -12915, 49151 }
	};
	private static final int[][] GROUP_3 =
	{
		{ 18483, -107798, 80721, -12912, 0 },
		{ 18483, -107798, 80546, -12912, 0 },
		{ 18347, -108033, 80644, -12912, 0 },
		{ 18363, -108520, 80647, -12912, 0 },
		{ 18483, -108740, 80752, -12912, 0 },
		{ 18363, -109016, 80642, -12912, 0 },
		{ 18483, -108740, 80546, -12912, 0 }
	};	
	private static final int[][] GROUP_4 =
	{
		{ 18362, -110082, 83998, -12928, 0 },
		{ 18362, -110082, 84210, -12928, 0 },
		{ 18363, -109963, 84102, -12896, 0 },
		{ 18347, -109322, 84102, -12880, 0 },
		{ 18362, -109131, 84097, -12880, 0 },
		{ 18483, -108932, 84101, -12880, 0 },
		{ 18483, -109313, 84488, -12880, 0 },
		{ 18362, -109122, 84490, -12880, 0 },
		{ 18347, -108939, 84489, -12880, 0 }
	};
	private static final int[][] MP_HERBS_DROPLIST =
	{
		// itemId, count, chance
		{ 8605, 1, 10 },
		{ 8604, 2, 40 },
		{ 8603, 3, 70 }
	};
	// @formatter:on
	
	private void dropHerb(Npc mob, PlayerInstance player, int[][] drop)
	{
		final int chance = getRandom(100);
		for (int[] element : drop)
		{
			if (chance < element[2])
			{
				mob.dropItem(player, element[0], element[1]);
			}
		}
	}
	
	private void giveBuffs(Creature ch)
	{
		if (SKILL_1 != null)
		{
			SKILL_1.applyEffects(ch, ch);
		}
		if (SKILL_2 != null)
		{
			SKILL_2.applyEffects(ch, ch);
		}
		if (SKILL_3 != null)
		{
			SKILL_3.applyEffects(ch, ch);
		}
		if (SKILL_4 != null)
		{
			SKILL_4.applyEffects(ch, ch);
		}
	}
	
	private NornilsGarden()
	{
		addStartNpc(GARDEN_GUARD);
		addFirstTalkId(GARDEN_GUARD);
		addTalkId(GARDEN_GUARD);
		for (int[] i : GATEKEEPERS)
		{
			addKillId(i[0]);
		}
		for (int[] i : AUTO_GATES)
		{
			addEnterZoneId(i[0]);
		}
		addTalkId(FINAL_GATES);
		addAttackId(HERB_JAR);
		addAttackId(18362); // first garden guard
	}
	
	@Override
	public void teleportPlayer(PlayerInstance player, Location loc, int instanceId)
	{
		giveBuffs(player);
		if (player.hasSummon())
		{
			giveBuffs(player.getSummon());
		}
		super.teleportPlayer(player, loc, instanceId);
	}
	
	private void exitInstance(PlayerInstance player)
	{
		final InstanceWorld inst = InstanceManager.getInstance().getWorld(player);
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			world.removeAllowed(player);
			teleportPlayer(player, EXIT_PPL, 0);
		}
	}
	
	private final synchronized String enterInstance(Npc npc, PlayerInstance player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		if (world != null)
		{
			if (!(world instanceof NornilsWorld) || (world.getTemplateId() != TEMPLATE_ID))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON);
				return null;
			}
			// check for level difference again on reenter
			if ((player.getLevel() > INSTANCE_LEVEL_MAX) || (player.getLevel() < INSTANCE_LEVEL_MIN))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
				sm.addPcName(player);
				player.sendPacket(sm);
				return null;
			}
			// check what instance still exist
			final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
			if (inst != null)
			{
				teleportPlayer(player, SPAWN_PPL, world.getInstanceId());
			}
			return null;
		}
		// Creating new instance
		final String result = checkConditions(npc, player);
		if (!(result.equalsIgnoreCase("ok")))
		{
			return result;
		}
		
		final NornilsWorld newWorld = new NornilsWorld();
		final Instance instance = InstanceManager.getInstance().createDynamicInstance(TEMPLATE_ID);
		newWorld.setInstance(instance);
		InstanceManager.getInstance().addWorld(world);
		instance.setExitLoc(new Location(player));
		instance.setAllowSummon(false);
		instance.setDuration(DURATION_TIME * 60000);
		instance.setEmptyDestroyTime(EMPTY_DESTROY_TIME * 60000);
		final int instanceId = newWorld.getInstanceId();
		LOGGER.info("Nornils Garden: started, Instance: " + instanceId + " created by player: " + player.getName());
		newWorld.first_npc = addSpawn(18362, -109702, 74696, -12528, 49568, false, 0, false, newWorld.getInstanceId());
		final DoorInstance door = instance.getDoor(16200010);
		if (door != null)
		{
			door.setTargetable(false);
			door.setMeshIndex(2);
		}
		
		// and finally teleport party into instance
		final Party party = player.getParty();
		if (party != null)
		{
			for (PlayerInstance partyMember : party.getMembers())
			{
				newWorld.addAllowed(partyMember);
				teleportPlayer(partyMember, SPAWN_PPL, instanceId);
			}
		}
		return null;
	}
	
	private void spawn1(Npc npc)
	{
		final InstanceWorld inst = InstanceManager.getInstance().getWorld(npc);
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			if (npc.equals(world.first_npc) && !world.spawned_1)
			{
				world.spawned_1 = true;
				for (int[] mob : GROUP_1)
				{
					addSpawn(mob[0], mob[1], mob[2], mob[3], mob[4], false, 0, false, world.getInstanceId());
				}
			}
		}
	}
	
	private void spawn2(Npc npc)
	{
		final InstanceWorld inst = InstanceManager.getInstance().getWorld(npc);
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			if (!world.spawned_2)
			{
				world.spawned_2 = true;
				for (int[] mob : GROUP_2)
				{
					addSpawn(mob[0], mob[1], mob[2], mob[3], mob[4], false, 0, false, world.getInstanceId());
				}
			}
		}
	}
	
	private void spawn3(Creature creature)
	{
		final InstanceWorld inst = InstanceManager.getInstance().getWorld(creature);
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			if (!world.spawned_3)
			{
				world.spawned_3 = true;
				for (int[] mob : GROUP_3)
				{
					addSpawn(mob[0], mob[1], mob[2], mob[3], mob[4], false, 0, false, world.getInstanceId());
				}
			}
		}
	}
	
	private void spawn4(Creature creature)
	{
		final InstanceWorld inst = InstanceManager.getInstance().getWorld(creature);
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			if (!world.spawned_4)
			{
				world.spawned_4 = true;
				for (int[] mob : GROUP_4)
				{
					addSpawn(mob[0], mob[1], mob[2], mob[3], mob[4], false, 0, false, world.getInstanceId());
				}
			}
		}
	}
	
	public void openDoor(QuestState st, PlayerInstance player, int doorId)
	{
		st.unset("correct");
		final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(player);
		if (tmpworld instanceof NornilsWorld)
		{
			tmpworld.openDoor(doorId);
		}
	}
	
	private static String checkConditions(Npc npc, PlayerInstance player)
	{
		final Party party = player.getParty();
		// player must be in party
		if (party == null)
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			return "32330-05.html";
		}
		// ...and be party leader
		if (party.getLeader() != player)
		{
			player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
			return "32330-08.html";
		}
		boolean kamael = false;
		// for each party member
		for (PlayerInstance partyMember : party.getMembers())
		{
			// player level must be in range
			if (partyMember.getLevel() > INSTANCE_LEVEL_MAX)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
				sm.addPcName(partyMember);
				player.sendPacket(sm);
				return "32330-06.html";
			}
			if (partyMember.getLevel() < INSTANCE_LEVEL_MIN)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
				sm.addPcName(partyMember);
				player.sendPacket(sm);
				return "32330-07.html";
			}
			if (partyMember.getClassId().level() != 0)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
				sm.addPcName(partyMember);
				player.sendPacket(sm);
				return "32330-06.html";
			}
			// player must be near party leader
			if (!partyMember.isInsideRadius3D(player, 500))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED);
				sm.addPcName(partyMember);
				player.sendPacket(sm);
				return "32330-08.html";
			}
			if (partyMember.getRace().ordinal() == 5)
			{
				final QuestState checkst = partyMember.getQuestState(Q00179_IntoTheLargeCavern.class.getSimpleName());
				if ((checkst != null) && (checkst.getState() == State.STARTED))
				{
					kamael = true;
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_QUEST_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED);
					sm.addPcName(partyMember);
					player.sendPacket(sm);
					return "32330-08.html";
				}
			}
		}
		if (!kamael)
		{
			return "32330-08.html";
		}
		return "ok";
	}
	
	@Override
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer() && !creature.isDead() && !creature.isTeleporting() && ((PlayerInstance) creature).isOnline())
		{
			final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(creature);
			if (tmpworld instanceof NornilsWorld)
			{
				for (int[] auto : AUTO_GATES)
				{
					if (zone.getId() == auto[0])
					{
						tmpworld.openDoor(auto[1]);
					}
					if (zone.getId() == 20111)
					{
						spawn3(creature);
					}
					else if (zone.getId() == 20112)
					{
						spawn4(creature);
					}
				}
			}
		}
		return super.onEnterZone(creature, zone);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		if ((npc.getId() == GARDEN_GUARD) && event.equalsIgnoreCase("enter_instance"))
		{
			try
			{
				htmltext = enterInstance(npc, player);
			}
			catch (Exception e)
			{
				// Not Important.
			}
		}
		else if ((npc.getId() == 32258) && event.equalsIgnoreCase("exit"))
		{
			try
			{
				exitInstance(player);
			}
			catch (Exception e)
			{
				// Not Important.
			}
		}
		else if (CommonUtil.contains(FINAL_GATES, npc.getId()))
		{
			if (event.equalsIgnoreCase("32260-02.html") || event.equalsIgnoreCase("32261-02.html") || event.equalsIgnoreCase("32262-02.html"))
			{
				st.unset("correct");
			}
			else if (Util.isDigit(event))
			{
				int correct = st.getInt("correct");
				correct++;
				st.set("correct", String.valueOf(correct));
				htmltext = npc.getId() + "-0" + correct + 2 + ".html";
			}
			else if (event.equalsIgnoreCase("check"))
			{
				final int correct = st.getInt("correct");
				if ((npc.getId() == 32260) && (correct == 3))
				{
					openDoor(st, player, 16200014);
				}
				else if ((npc.getId() == 32261) && (correct == 3))
				{
					openDoor(st, player, 16200015);
				}
				else if ((npc.getId() == 32262) && (correct == 4))
				{
					openDoor(st, player, 16200016);
				}
				else
				{
					return npc.getId() + "-00.html";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		if (CommonUtil.contains(FINAL_GATES, npc.getId()))
		{
			final QuestState cst = player.getQuestState(Q00179_IntoTheLargeCavern.class.getSimpleName());
			if ((cst != null) && (cst.getState() == State.STARTED))
			{
				return npc.getId() + "-01.html";
			}
			return getNoQuestMsg(player);
		}
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		getQuestState(player, true);
		return npc.getId() + ".html";
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		if ((npc.getId() == HERB_JAR) && !npc.isDead())
		{
			dropHerb(npc, attacker, HP_HERBS_DROPLIST);
			dropHerb(npc, attacker, MP_HERBS_DROPLIST);
			npc.doDie(attacker);
		}
		else if ((npc.getId() == 18362) && (npc.getInstanceId() > 0))
		{
			spawn1(npc);
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance player, boolean isSummon)
	{
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return null;
		}
		
		for (int[] gk : GATEKEEPERS)
		{
			if (npc.getId() == gk[0])
			{
				// Drop key
				npc.dropItem(player, gk[1], 1);
				
				// Check if gatekeeper should open bridge, and open it
				if (gk[2] > 0)
				{
					final InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(player);
					if (tmpworld instanceof NornilsWorld)
					{
						tmpworld.openDoor(gk[2]);
					}
				}
			}
			if (npc.getId() == 18355)
			{
				spawn2(npc);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public void onEnterInstance(PlayerInstance player, InstanceWorld world, boolean firstEntrance)
	{
		// Managed by script.
	}
	
	public static void main(String[] args)
	{
		new NornilsGarden();
	}
}
