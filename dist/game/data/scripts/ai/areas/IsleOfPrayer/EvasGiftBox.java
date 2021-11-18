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
package ai.areas.IsleOfPrayer;

import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.ItemHolder;

import ai.AbstractNpcAI;

/**
 * Eva's Gift Box AI.
 * @author St3eT
 */
public class EvasGiftBox extends AbstractNpcAI
{
	// NPC
	private static final int BOX = 32342; // Eva's Gift Box
	// Skill
	private static final int BUFF = 1073; // Kiss of Eva
	// Items
	private static final ItemHolder CORAL = new ItemHolder(9692, 1); // Red Coral
	private static final ItemHolder CRYSTAL = new ItemHolder(9693, 1); // Crystal Fragment
	
	private EvasGiftBox()
	{
		addKillId(BOX);
		addSpawnId(BOX);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		if (killer.isAffectedBySkill(BUFF))
		{
			if (getRandomBoolean())
			{
				npc.dropItem(killer, CRYSTAL);
			}
			
			if (getRandom(100) < 33)
			{
				npc.dropItem(killer, CORAL);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
		((Attackable) npc).setOnKillDelay(0);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new EvasGiftBox();
	}
}