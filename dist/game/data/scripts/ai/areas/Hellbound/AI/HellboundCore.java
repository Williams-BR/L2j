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
package ai.areas.Hellbound.AI;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Manages Naia's cast on the Hellbound Core
 * @author GKR
 */
public class HellboundCore extends AbstractNpcAI
{
	// NPCs
	private static final int NAIA = 18484;
	private static final int HELLBOUND_CORE = 32331;
	// Skills
	private static SkillHolder BEAM = new SkillHolder(5493, 1);
	
	public HellboundCore()
	{
		addSpawnId(HELLBOUND_CORE, NAIA);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equalsIgnoreCase("cast") && (HellboundEngine.getInstance().getLevel() <= 6))
		{
			World.getInstance().forEachVisibleObjectInRange(npc, Creature.class, 900, naia ->
			{
				if ((naia != null) && naia.isMonster() && (naia.getId() == NAIA) && !naia.isDead() && !naia.isChanneling())
				{
					naia.setTarget(npc);
					naia.doSimultaneousCast(BEAM.getSkill());
				}
			});
			startQuestTimer("cast", 10000, npc, null);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		if (npc.getId() == NAIA)
		{
			npc.setRandomWalking(false);
		}
		else
		{
			startQuestTimer("cast", 10000, npc, null);
		}
		return super.onSpawn(npc);
	}
}