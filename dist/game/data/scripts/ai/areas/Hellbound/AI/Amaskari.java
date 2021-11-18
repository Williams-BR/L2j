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

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Manages Amaskari's and minions' chat and some skill usage.
 * @author GKR
 */
public class Amaskari extends AbstractNpcAI
{
	// NPCs
	private static final int AMASKARI = 22449;
	private static final int AMASKARI_PRISONER = 22450;
	// Skills
	// private static SkillHolder INVINCIBILITY = new SkillHolder(5417, 1);
	private static final int BUFF_ID = 4632;
	private static SkillHolder[] BUFF =
	{
		new SkillHolder(BUFF_ID, 1),
		new SkillHolder(BUFF_ID, 2),
		new SkillHolder(BUFF_ID, 3)
	};
	// Misc
	private static final NpcStringId[] AMASKARI_NPCSTRING_ID =
	{
		NpcStringId.I_LL_MAKE_EVERYONE_FEEL_THE_SAME_SUFFERING_AS_ME,
		NpcStringId.HA_HA_YES_DIE_SLOWLY_WRITHING_IN_PAIN_AND_AGONY,
		NpcStringId.MORE_NEED_MORE_SEVERE_PAIN,
		NpcStringId.SOMETHING_IS_BURNING_INSIDE_MY_BODY
	};
	private static final NpcStringId[] MINIONS_NPCSTRING_ID =
	{
		NpcStringId.AHH_MY_LIFE_IS_BEING_DRAINED_OUT,
		NpcStringId.THANK_YOU_FOR_SAVING_ME,
		NpcStringId.IT_WILL_KILL_EVERYONE,
		NpcStringId.EEEK_I_FEEL_SICK_YOW
	};
	
	public Amaskari()
	{
		addKillId(AMASKARI, AMASKARI_PRISONER);
		addAttackId(AMASKARI);
		addSpawnId(AMASKARI_PRISONER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equalsIgnoreCase("stop_toggle"))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, AMASKARI_NPCSTRING_ID[2]);
			((MonsterInstance) npc).clearAggroList();
			((MonsterInstance) npc).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			npc.setInvul(false);
			// npc.doCast(INVINCIBILITY.getSkill())
		}
		else if (event.equalsIgnoreCase("onspawn_msg") && (npc != null) && !npc.isDead())
		{
			if (getRandom(100) > 20)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, MINIONS_NPCSTRING_ID[2]);
			}
			else if (getRandom(100) > 40)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, MINIONS_NPCSTRING_ID[3]);
			}
			startQuestTimer("onspawn_msg", (getRandom(8) + 1) * 30000, npc, null);
		}
		return null;
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((npc.getId() == AMASKARI) && (getRandom(1000) < 25))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, AMASKARI_NPCSTRING_ID[0]);
			for (MonsterInstance minion : ((MonsterInstance) npc).getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead() && (getRandom(10) == 0))
				{
					minion.broadcastSay(ChatType.NPC_GENERAL, MINIONS_NPCSTRING_ID[0]);
					minion.setCurrentHp(minion.getCurrentHp() - (minion.getCurrentHp() / 5));
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		if (npc.getId() == AMASKARI_PRISONER)
		{
			final MonsterInstance master = ((MonsterInstance) npc).getLeader();
			if ((master != null) && !master.isDead())
			{
				master.broadcastSay(ChatType.NPC_GENERAL, AMASKARI_NPCSTRING_ID[1]);
				final BuffInfo info = master.getEffectList().getBuffInfoBySkillId(BUFF_ID);
				if ((info != null) && (info.getSkill().getAbnormalLevel() == 3) && master.isInvul())
				{
					master.setCurrentHp(master.getCurrentHp() + (master.getCurrentHp() / 5));
				}
				else
				{
					master.clearAggroList();
					master.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					if (info == null)
					{
						master.doCast(BUFF[0].getSkill());
					}
					else if (info.getSkill().getAbnormalLevel() < 3)
					{
						master.doCast(BUFF[info.getSkill().getAbnormalLevel()].getSkill());
					}
					else
					{
						master.broadcastSay(ChatType.NPC_GENERAL, AMASKARI_NPCSTRING_ID[3]);
						// master.doCast(INVINCIBILITY.getSkill())
						master.setInvul(true);
						startQuestTimer("stop_toggle", 10000, master, null);
					}
				}
			}
		}
		else if (npc.getId() == AMASKARI)
		{
			for (MonsterInstance minion : ((MonsterInstance) npc).getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead())
				{
					if (getRandom(1000) > 300)
					{
						minion.broadcastSay(ChatType.NPC_GENERAL, MINIONS_NPCSTRING_ID[1]);
					}
					HellboundEngine.getInstance().updateTrust(30, true);
					minion.deleteMe();
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		startQuestTimer("onspawn_msg", (getRandom(3) + 1) * 30000, npc, null);
		return super.onSpawn(npc);
	}
}