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
package ai.areas.GiantsCave;

import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.AbstractNpcAI;

/**
 * Giant's Cave AI.
 * @author Gnacik, St3eT
 */
public class GiantsCave extends AbstractNpcAI
{
	// NPC
	private static final int[] SCOUTS =
	{
		22668, // Gamlin (Scout)
		22669, // Leogul (Scout)
	};
	
	private GiantsCave()
	{
		addAttackId(SCOUTS);
		addAggroRangeEnterId(SCOUTS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equals("ATTACK") && (player != null) && (npc != null) && !npc.isDead())
		{
			if (npc.getId() == SCOUTS[0]) // Gamlin
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.INTRUDER_DETECTED);
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.OH_GIANTS_AN_INTRUDER_HAS_BEEN_DISCOVERED);
			}
			
			World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, 450, characters ->
			{
				if ((getRandomBoolean()))
				{
					addAttackDesire(characters, player);
				}
			});
		}
		else if (event.equals("CLEAR") && (npc != null) && !npc.isDead())
		{
			npc.setScriptValue(0);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			startQuestTimer("ATTACK", 6000, npc, attacker);
			startQuestTimer("CLEAR", 120000, npc, null);
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onAggroRangeEnter(Npc npc, PlayerInstance player, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			if (getRandomBoolean())
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_GUYS_ARE_DETECTED);
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WHAT_KIND_OF_CREATURES_ARE_YOU);
			}
			startQuestTimer("ATTACK", 6000, npc, player);
			startQuestTimer("CLEAR", 120000, npc, null);
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new GiantsCave();
	}
}