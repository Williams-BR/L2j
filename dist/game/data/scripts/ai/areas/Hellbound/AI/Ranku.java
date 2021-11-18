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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.util.MinionList;

import ai.AbstractNpcAI;

/**
 * Ranku's AI.
 * @author GKR
 */
public class Ranku extends AbstractNpcAI
{
	// NPCs
	private static final int RANKU = 25542;
	private static final int MINION = 32305;
	private static final int MINION_2 = 25543;
	// Misc
	private static final Set<Integer> MY_TRACKING_SET = ConcurrentHashMap.newKeySet();
	
	public Ranku()
	{
		addAttackId(RANKU);
		addKillId(RANKU, MINION);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equalsIgnoreCase("checkup") && (npc.getId() == RANKU) && !npc.isDead())
		{
			for (MonsterInstance minion : ((MonsterInstance) npc).getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead() && MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					final PlayerInstance killer = getRandomEntry(World.getInstance().getVisibleObjects(minion, PlayerInstance.class));
					minion.reduceCurrentHp(minion.getMaxHp() / 100, killer, null);
				}
			}
			startQuestTimer("checkup", 1000, npc, null);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getId() == RANKU)
		{
			for (MonsterInstance minion : ((MonsterInstance) npc).getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead() && !MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					minion.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DON_T_KILL_ME_PLEASE_SOMETHING_S_STRANGLING_ME);
					startQuestTimer("checkup", 1000, npc, null);
					MY_TRACKING_SET.add(minion.getObjectId());
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		if (npc.getId() == MINION)
		{
			if (MY_TRACKING_SET.contains(npc.getObjectId()))
			{
				MY_TRACKING_SET.remove(npc.getObjectId());
			}
			
			final MonsterInstance master = ((MonsterInstance) npc).getLeader();
			if ((master != null) && !master.isDead())
			{
				final MonsterInstance minion2 = MinionList.spawnMinion(master, MINION_2);
				minion2.teleToLocation(npc.getLocation());
			}
		}
		else if (npc.getId() == RANKU)
		{
			for (MonsterInstance minion : ((MonsterInstance) npc).getMinionList().getSpawnedMinions())
			{
				if (MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					MY_TRACKING_SET.remove(minion.getObjectId());
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
}