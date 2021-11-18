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
package ai.areas.StakatoNest;

import java.util.List;

import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.AbnormalType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.Util;

import ai.AbstractNpcAI;

/**
 * Stakato Nest AI.
 * @author Gnacik
 */
public class StakatoNest extends AbstractNpcAI
{
	// @formatter:off
	// List of all mobs just for register
	private static final int[] STAKATO_MOBS =
	{
		18793, 18794, 18795, 18796, 18797, 18798, 22617, 22618, 22619, 22620,
		22621, 22622, 22623, 22624, 22625, 22626, 22627, 22628, 22629, 22630,
		22631, 22632, 22633, 25667
	};
	// Coocons
	private static final int[] COCOONS =
	{
		18793, 18794, 18795, 18796, 18797, 18798
	};
	// @formatter:on
	// Cannibalistic Stakato Leader
	private static final int STAKATO_LEADER = 22625;
	
	// Spike Stakato Nurse
	private static final int STAKATO_NURSE = 22630;
	// Spike Stakato Nurse (Changed)
	private static final int STAKATO_NURSE_2 = 22631;
	// Spiked Stakato Baby
	private static final int STAKATO_BABY = 22632;
	// Spiked Stakato Captain
	private static final int STAKATO_CAPTAIN = 22629;
	
	// Female Spiked Stakato
	private static final int STAKATO_FEMALE = 22620;
	// Male Spiked Stakato
	private static final int STAKATO_MALE = 22621;
	// Male Spiked Stakato (Changed)
	private static final int STAKATO_MALE_2 = 22622;
	// Spiked Stakato Guard
	private static final int STAKATO_GUARD = 22619;
	
	// Cannibalistic Stakato Chief
	private static final int STAKATO_CHIEF = 25667;
	// Growth Accelerator
	private static final int GROWTH_ACCELERATOR = 2905;
	// Small Stakato Cocoon
	private static final int SMALL_COCOON = 14833;
	// Large Stakato Cocoon
	private static final int LARGE_COCOON = 14834;
	
	private StakatoNest()
	{
		registerMobs(STAKATO_MOBS);
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		if ((npc.getId() == STAKATO_LEADER) && (npc.getEffectList().getBuffInfoByAbnormalType(AbnormalType.SILENCE) == null) && (getRandom(1000) < 100) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.3)))
		{
			final MonsterInstance follower = checkMinion(npc);
			if (follower != null)
			{
				final double hp = follower.getCurrentHp();
				if (hp > (follower.getMaxHp() * 0.3))
				{
					npc.abortAttack();
					npc.abortCast();
					npc.setHeading(Util.calculateHeadingFrom(npc, follower));
					npc.doCast(SkillData.getInstance().getSkill(4484, 1));
					npc.setCurrentHp(npc.getCurrentHp() + hp);
					follower.doDie(follower);
					follower.deleteMe();
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final MonsterInstance monster;
		switch (npc.getId())
		{
			case STAKATO_NURSE:
			{
				monster = checkMinion(npc);
				if (monster != null)
				{
					Broadcast.toSelfAndKnownPlayers(npc, new MagicSkillUse(npc, 2046, 1, 1000, 0));
					for (int i = 0; i < 3; i++)
					{
						final Npc spawned = addSpawn(STAKATO_CAPTAIN, monster, true);
						addAttackDesire(spawned, killer);
					}
				}
				break;
			}
			case STAKATO_BABY:
			{
				monster = ((MonsterInstance) npc).getLeader();
				if ((monster != null) && !monster.isDead())
				{
					startQuestTimer("nurse_change", 5000, monster, killer);
				}
				break;
			}
			case STAKATO_MALE:
			{
				monster = checkMinion(npc);
				if (monster != null)
				{
					Broadcast.toSelfAndKnownPlayers(npc, new MagicSkillUse(npc, 2046, 1, 1000, 0));
					for (int i = 0; i < 3; i++)
					{
						final Npc spawned = addSpawn(STAKATO_GUARD, monster, true);
						addAttackDesire(spawned, killer);
					}
				}
				break;
			}
			case STAKATO_FEMALE:
			{
				monster = ((MonsterInstance) npc).getLeader();
				if ((monster != null) && !monster.isDead())
				{
					startQuestTimer("male_change", 5000, monster, killer);
				}
				break;
			}
			case STAKATO_CHIEF:
			{
				if (killer.isInParty())
				{
					final List<PlayerInstance> party = killer.getParty().getMembers();
					for (PlayerInstance member : party)
					{
						giveCocoon(member, npc);
					}
				}
				else
				{
					giveCocoon(killer, npc);
				}
				break;
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSkillSee(Npc npc, PlayerInstance caster, Skill skill, WorldObject[] targets, boolean isSummon)
	{
		if (CommonUtil.contains(COCOONS, npc.getId()) && CommonUtil.contains(targets, npc) && (skill.getId() == GROWTH_ACCELERATOR))
		{
			npc.doDie(caster);
			final Npc spawned = addSpawn(STAKATO_CHIEF, npc.getX(), npc.getY(), npc.getZ(), Util.calculateHeadingFrom(npc, caster), false, 0, true);
			addAttackDesire(spawned, caster);
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if ((npc == null) || (player == null) || npc.isDead())
		{
			return null;
		}
		
		int npcId = 0;
		switch (event)
		{
			case "nurse_change":
			{
				npcId = STAKATO_NURSE_2;
				break;
			}
			case "male_change":
			{
				npcId = STAKATO_MALE_2;
				break;
			}
		}
		if (npcId > 0)
		{
			npc.getSpawn().decreaseCount(npc);
			final Npc spawned = addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, true);
			addAttackDesire(spawned, player);
			npc.deleteMe();
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	private static MonsterInstance checkMinion(Npc npc)
	{
		final MonsterInstance mob = (MonsterInstance) npc;
		if (mob.hasMinions())
		{
			final List<MonsterInstance> minion = mob.getMinionList().getSpawnedMinions();
			if ((minion != null) && !minion.isEmpty() && (minion.get(0) != null) && !minion.get(0).isDead())
			{
				return minion.get(0);
			}
		}
		return null;
	}
	
	private void giveCocoon(PlayerInstance player, Npc npc)
	{
		player.addItem("StakatoCocoon", ((getRandom(100) > 80) ? LARGE_COCOON : SMALL_COCOON), 1, npc, true);
	}
	
	public static void main(String[] args)
	{
		new StakatoNest();
	}
}
