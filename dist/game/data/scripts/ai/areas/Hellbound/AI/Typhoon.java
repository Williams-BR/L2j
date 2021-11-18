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

import org.l2jmobius.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.RaidBossInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;

import ai.AbstractNpcAI;

/**
 * Typhoon's AI.
 * @author GKR
 */
public class Typhoon extends AbstractNpcAI
{
	// NPCs
	private static final int TYPHOON = 25539;
	// Skills
	private static SkillHolder STORM = new SkillHolder(5434, 1); // Gust
	
	public Typhoon()
	{
		addAggroRangeEnterId(TYPHOON);
		addSpawnId(TYPHOON);
		
		final RaidBossInstance boss = RaidBossSpawnManager.getInstance().getBosses().get(TYPHOON);
		if (boss != null)
		{
			onSpawn(boss);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equalsIgnoreCase("CAST") && (npc != null) && !npc.isDead())
		{
			npc.doSimultaneousCast(STORM.getSkill());
			startQuestTimer("CAST", 5000, npc, null);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAggroRangeEnter(Npc npc, PlayerInstance player, boolean isSummon)
	{
		npc.doSimultaneousCast(STORM.getSkill());
		return super.onAggroRangeEnter(npc, player, isSummon);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		startQuestTimer("CAST", 5000, npc, null);
		return super.onSpawn(npc);
	}
}