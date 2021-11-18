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
package ai.areas.Hellbound.AI.Zones.BaseTower;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;

import ai.AbstractNpcAI;

/**
 * Base Tower.
 * @author GKR
 */
public class BaseTower extends AbstractNpcAI
{
	// NPCs
	private static final int GUZEN = 22362;
	private static final int KENDAL = 32301;
	private static final int BODY_DESTROYER = 22363;
	// Skills
	private static final SkillHolder DEATH_WORD = new SkillHolder(5256, 1);
	// Misc
	private static final Map<Integer, PlayerInstance> BODY_DESTROYER_TARGET_LIST = new ConcurrentHashMap<>();
	
	public BaseTower()
	{
		addKillId(GUZEN);
		addKillId(BODY_DESTROYER);
		addFirstTalkId(KENDAL);
		addAggroRangeEnterId(BODY_DESTROYER);
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		final ClassId classId = player.getClassId();
		if (classId.equalsOrChildOf(ClassId.HELL_KNIGHT) || classId.equalsOrChildOf(ClassId.SOULTAKER))
		{
			return "32301-02.htm";
		}
		return "32301-01.htm";
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equalsIgnoreCase("CLOSE"))
		{
			DoorData.getInstance().getDoor(20260004).closeMe();
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAggroRangeEnter(Npc npc, PlayerInstance player, boolean isSummon)
	{
		if (!BODY_DESTROYER_TARGET_LIST.containsKey(npc.getObjectId()))
		{
			BODY_DESTROYER_TARGET_LIST.put(npc.getObjectId(), player);
			npc.setTarget(player);
			npc.doSimultaneousCast(DEATH_WORD.getSkill());
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case GUZEN:
			{
				// Should Kendal be despawned before Guzen's spawn? Or it will be crowd of Kendal's
				addSpawn(KENDAL, npc.getSpawn().getLocation(), false, npc.getSpawn().getRespawnDelay(), false);
				DoorData.getInstance().getDoor(20260003).openMe();
				DoorData.getInstance().getDoor(20260004).openMe();
				startQuestTimer("CLOSE", 60000, npc, null, false);
				break;
			}
			case BODY_DESTROYER:
			{
				if (BODY_DESTROYER_TARGET_LIST.containsKey(npc.getObjectId()))
				{
					final PlayerInstance pl = BODY_DESTROYER_TARGET_LIST.get(npc.getObjectId());
					if ((pl != null) && pl.isOnline() && !pl.isDead())
					{
						pl.stopSkillEffects(SkillFinishType.REMOVED, DEATH_WORD.getSkillId());
					}
					BODY_DESTROYER_TARGET_LIST.remove(npc.getObjectId());
				}
				break;
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
}