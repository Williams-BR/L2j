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
package conquerablehalls.DevastatedCastle;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Devastated Castle clan hall siege script.
 * @author BiggBoss
 */
public class DevastatedCastle extends ClanHallSiegeEngine
{
	private static final int GUSTAV = 35410;
	private static final int MIKHAIL = 35409;
	private static final int DIETRICH = 35408;
	
	private final Map<Integer, Integer> _damageToGustav = new ConcurrentHashMap<>();
	
	private DevastatedCastle()
	{
		super(DEVASTATED_CASTLE);
		addKillId(GUSTAV);
		addSpawnId(MIKHAIL, DIETRICH);
		addAttackId(GUSTAV);
	}
	
	@Override
	public void onSiegeStarts()
	{
		_damageToGustav.clear();
		super.onSiegeStarts();
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		if (npc.getId() == MIKHAIL)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.GLORY_TO_ADEN_THE_KINGDOM_OF_THE_LION_GLORY_TO_SIR_GUSTAV_OUR_IMMORTAL_LORD);
		}
		else if (npc.getId() == DIETRICH)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.SOLDIERS_OF_GUSTAV_GO_FORTH_AND_DESTROY_THE_INVADERS);
		}
		return null;
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return null;
		}
		
		final Clan clan = attacker.getClan();
		if ((clan != null) && checkIsAttacker(clan))
		{
			_damageToGustav.merge(clan.getId(), damage, Integer::sum);
		}
		
		synchronized (this)
		{
			if (!npc.isCastingNow() && (npc.getCurrentHp() < (npc.getMaxHp() / 12)))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THIS_IS_UNBELIEVABLE_HAVE_I_REALLY_BEEN_DEFEATED_I_SHALL_RETURN_AND_TAKE_YOUR_HEAD);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, SkillData.getInstance().getSkill(4235, 1), npc);
			}
		}
		
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return null;
		}
		
		_missionAccomplished = true;
		cancelSiegeTask();
		endSiege();
		
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public Clan getWinner()
	{
		if (_damageToGustav.isEmpty())
		{
			return null;
		}
		
		final int clanId = Collections.max(_damageToGustav.entrySet(), Map.Entry.comparingByValue()).getKey();
		return ClanTable.getInstance().getClan(clanId);
	}
	
	public static void main(String[] args)
	{
		new DevastatedCastle();
	}
}