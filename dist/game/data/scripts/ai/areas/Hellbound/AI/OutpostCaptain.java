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

import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Outpost Captain's AI.
 * @author DS
 */
public class OutpostCaptain extends AbstractNpcAI
{
	// NPCs
	private static final int CAPTAIN = 18466;
	private static final int[] DEFENDERS =
	{
		22357, // Enceinte Defender
		22358, // Enceinte Defender
	};
	
	public OutpostCaptain()
	{
		addKillId(CAPTAIN);
		addSpawnId(CAPTAIN);
		addSpawnId(DEFENDERS);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final DoorInstance door = DoorData.getInstance().getDoor(20250001);
		if (door != null)
		{
			door.openMe();
		}
		if (HellboundEngine.getInstance().getLevel() == 8)
		{
			HellboundEngine.getInstance().setLevel(9);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
		
		if (npc.getId() == CAPTAIN)
		{
			final int hellboundLevel = HellboundEngine.getInstance().getLevel();
			if ((hellboundLevel < 7) || (hellboundLevel > 8))
			{
				npc.deleteMe();
				npc.getSpawn().stopRespawn();
			}
			else
			{
				final DoorInstance door = DoorData.getInstance().getDoor(20250001);
				if (door != null)
				{
					door.closeMe();
				}
			}
		}
		return super.onSpawn(npc);
	}
}