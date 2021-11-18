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
package ai.others;

import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;

import ai.AbstractNpcAI;

/**
 * Remnants AI.
 * @author DS
 */
public class Remnants extends AbstractNpcAI
{
	private static final int[] NPCS =
	{
		18463,
		18464,
		18465
	};
	private static final int SKILL_HOLY_WATER = 2358;
	
	// TODO: Find retail strings.
	// private static final String MSG = "The holy water affects Remnants Ghost. You have freed his soul.";
	// private static final String MSG_DEREK = "The holy water affects Derek. You have freed his soul.";
	private Remnants()
	{
		addSpawnId(NPCS);
		addSkillSeeId(NPCS);
		// Do not override onKill for Derek here. Let's make global Hellbound manipulations in Engine where it is possible.
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setMortal(false);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onSkillSee(Npc npc, PlayerInstance caster, Skill skill, WorldObject[] targets, boolean isSummon)
	{
		if ((skill.getId() == SKILL_HOLY_WATER) && !npc.isDead() && (targets.length > 0) && (targets[0] == npc) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.02)))
		{
			npc.doDie(caster);
			//@formatter:off
			/*if (npc.getNpcId() == DEREK)
			{
				caster.sendMessage(MSG_DEREK);
			}
			else
			{
				caster.sendMessage(MSG);
			}*/
			//@formatter:on
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}
	
	public static void main(String[] args)
	{
		new Remnants();
	}
}
