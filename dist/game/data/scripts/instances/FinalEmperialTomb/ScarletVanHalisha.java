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
package instances.FinalEmperialTomb;

import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.DecoyInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.util.Util;

import ai.AbstractNpcAI;

/**
 * @author Micr0, Zerox
 */
public class ScarletVanHalisha extends AbstractNpcAI
{
	private Creature _target;
	private Skill _skill;
	private long _lastRangedSkillTime;
	private final int _rangedSkillMinCoolTime = 60000; // 1 minute
	
	// NPCs
	private static final int HALISHA2 = 29046;
	private static final int HALISHA3 = 29047;
	
	public ScarletVanHalisha()
	{
		addAttackId(HALISHA2, HALISHA3);
		addKillId(HALISHA2, HALISHA3);
		addSpellFinishedId(HALISHA2, HALISHA3);
		registerMobs(HALISHA2, HALISHA3);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "attack":
			{
				if (npc != null)
				{
					getSkillAI(npc);
				}
				break;
			}
			case "random_target":
			{
				_target = getRandomTarget(npc, null);
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpellFinished(Npc npc, PlayerInstance player, Skill skill)
	{
		getSkillAI(npc);
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		startQuestTimer("random_Target", 5000, npc, null, true);
		startQuestTimer("attack", 500, npc, null, true);
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		cancelQuestTimers("attack");
		cancelQuestTimers("random_Target");
		return super.onKill(npc, killer, isSummon);
	}
	
	private Skill getRndSkills(Npc npc)
	{
		switch (npc.getId())
		{
			case HALISHA2:
			{
				if (Rnd.get(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 2);
				}
				else if (Rnd.get(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 5);
				}
				else if (Rnd.get(100) < 2)
				{
					return SkillData.getInstance().getSkill(5016, 1);
				}
				else
				{
					return SkillData.getInstance().getSkill(5014, 2);
				}
			}
			case HALISHA3:
			{
				if (Rnd.get(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 3);
				}
				else if (Rnd.get(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 6);
				}
				else if (Rnd.get(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 2);
				}
				else if (((_lastRangedSkillTime + _rangedSkillMinCoolTime) < Chronos.currentTimeMillis()) && (Rnd.get(100) < 10))
				{
					return SkillData.getInstance().getSkill(5019, 1);
				}
				else if (((_lastRangedSkillTime + _rangedSkillMinCoolTime) < Chronos.currentTimeMillis()) && (Rnd.get(100) < 10))
				{
					return SkillData.getInstance().getSkill(5018, 1);
				}
				else if (Rnd.get(100) < 2)
				{
					return SkillData.getInstance().getSkill(5016, 1);
				}
				else
				{
					return SkillData.getInstance().getSkill(5014, 3);
				}
			}
		}
		return SkillData.getInstance().getSkill(5014, 1);
	}
	
	private synchronized void getSkillAI(Npc npc)
	{
		if (npc.isInvul() || npc.isCastingNow())
		{
			return;
		}
		if ((Rnd.get(100) < 30) || (_target == null) || _target.isDead())
		{
			_skill = getRndSkills(npc);
			_target = getRandomTarget(npc, _skill);
		}
		final Creature target = _target;
		Skill skill = _skill;
		if (skill == null)
		{
			skill = getRndSkills(npc);
		}
		
		if (npc.isPhysicalMuted())
		{
			return;
		}
		
		if ((target == null) || target.isDead())
		{
			npc.setCastingNow(false);
			return;
		}
		
		if (Util.checkIfInRange(skill.getCastRange(), npc, target, true))
		{
			npc.getAI().setIntention(AI_INTENTION_IDLE);
			npc.setTarget(target);
			npc.setCastingNow(true);
			_target = null;
			npc.doCast(skill);
		}
		else
		{
			npc.getAI().setIntention(AI_INTENTION_FOLLOW, target, null);
			npc.getAI().setIntention(AI_INTENTION_ATTACK, target, null);
			npc.setCastingNow(false);
		}
	}
	
	private Creature getRandomTarget(Npc npc, Skill skill)
	{
		final List<Creature> result = new ArrayList<>();
		for (WorldObject obj : World.getInstance().getVisibleObjects(npc, WorldObject.class))
		{
			if (obj.isPlayable() || (obj instanceof DecoyInstance))
			{
				if (obj.isPlayer() && obj.getActingPlayer().isInvisible())
				{
					continue;
				}
				
				if (((((Creature) obj).getZ() < (npc.getZ() - 100)) && (((Creature) obj).getZ() > (npc.getZ() + 100))) || !GeoEngine.getInstance().canSeeTarget(obj, npc))
				{
					continue;
				}
			}
			if (obj.isPlayable() || (obj instanceof DecoyInstance))
			{
				int skillRange = 150;
				if (skill != null)
				{
					switch (skill.getId())
					{
						case 5014:
						{
							skillRange = 150;
							break;
						}
						case 5015:
						{
							skillRange = 400;
							break;
						}
						case 5016:
						{
							skillRange = 200;
							break;
						}
						case 5018:
						case 5019:
						{
							_lastRangedSkillTime = Chronos.currentTimeMillis();
							skillRange = 550;
							break;
						}
					}
				}
				if (Util.checkIfInRange(skillRange, npc, obj, true) && !((Creature) obj).isDead())
				{
					result.add((Creature) obj);
				}
			}
		}
		if (!result.isEmpty() && (result.size() != 0))
		{
			return getRandomEntry(result);
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new ScarletVanHalisha();
	}
}