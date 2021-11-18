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
package quests.Q00311_ExpulsionOfEvilSpirits;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.instancemanager.GlobalVariablesManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.Util;

/**
 * Expulsion of Evil Spirits (311)
 * @author Zoey76
 */
public class Q00311_ExpulsionOfEvilSpirits extends Quest
{
	// NPCs
	private static final int CHAIREN = 32655;
	private final static int ALTAR = 18811;
	private final static int VARANGKA = 18808;
	// Items
	private static final int PROTECTION_SOULS_PENDANT = 14848;
	private static final int SOUL_CORE_CONTAINING_EVIL_SPIRIT = 14881;
	private static final int RAGNA_ORCS_AMULET = 14882;
	// Monsters
	private static final Map<Integer, Double> MONSTERS = new HashMap<>();
	static
	{
		MONSTERS.put(22691, 0.694); // Ragna Orc
		MONSTERS.put(22692, 0.716); // Ragna Orc Warrior
		MONSTERS.put(22693, 0.736); // Ragna Orc Hero
		MONSTERS.put(22694, 0.712); // Ragna Orc Commander
		MONSTERS.put(22695, 0.698); // Ragna Orc Healer
		MONSTERS.put(22696, 0.692); // Ragna Orc Shaman
		MONSTERS.put(22697, 0.640); // Ragna Orc Seer
		MONSTERS.put(22698, 0.716); // Ragna Orc Archer
		MONSTERS.put(22699, 0.752); // Ragna Orc Sniper
		MONSTERS.put(22701, 0.716); // Varangka's Dre Vanul
		MONSTERS.put(22702, 0.662); // Varangka's Destroyer
		MONSTERS.put(18808, 0.7);
		MONSTERS.put(18809, 0.694);
		MONSTERS.put(18810, 0.694);
	}
	// Misc
	private static final int MIN_LEVEL = 80;
	private static final int SOUL_CORE_COUNT = 10;
	private static final int RAGNA_ORCS_KILLS_COUNT = 100;
	private static final int RAGNA_ORCS_AMULET_COUNT = 10;
	protected static final ZoneType ALTARZONE = ZoneManager.getInstance().getZoneById(20201);
	protected static Npc _altar;
	private static Npc _varangka;
	private static Npc _varangkaMinion1;
	private static Npc _varangkaMinion2;
	private static long respawnTime = 0;
	
	public Q00311_ExpulsionOfEvilSpirits()
	{
		super(311);
		addStartNpc(CHAIREN);
		addTalkId(CHAIREN);
		addKillId(MONSTERS.keySet());
		registerQuestItems(SOUL_CORE_CONTAINING_EVIL_SPIRIT, RAGNA_ORCS_AMULET);
		addEnterZoneId(ALTARZONE.getId());
		addAttackId(ALTAR);
		
		try
		{
			respawnTime = GlobalVariablesManager.getInstance().getLong("VarangkaRespawn", 0);
		}
		catch (Exception e)
		{
			
		}
		GlobalVariablesManager.getInstance().set("VarangkaRespawn", respawnTime);
		if ((respawnTime == 0) || ((respawnTime - Chronos.currentTimeMillis()) < 0))
		{
			startQuestTimer("altarSpawn", 5000, null, null);
		}
		else
		{
			startQuestTimer("altarSpawn", respawnTime - Chronos.currentTimeMillis(), null, null);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equalsIgnoreCase("altarSpawn"))
		{
			if (!checkIfSpawned(ALTAR))
			{
				_altar = addSpawn(ALTAR, 74120, -101920, -960, 32760, false, 0);
				_altar.setInvul(true);
				GlobalVariablesManager.getInstance().set("VarangkaRespawn", 0);
				if (Util.checkIfInRange(1200, npc, player, true))
				{
					ThreadPool.schedule(new zoneCheck(player), 1000);
				}
			}
			return null;
		}
		else if (event.equalsIgnoreCase("minion1") && checkIfSpawned(VARANGKA))
		{
			if (!checkIfSpawned(VARANGKA + 1) && checkIfSpawned(VARANGKA))
			{
				_varangkaMinion1 = addSpawn(VARANGKA + 1, player.getX() + Rnd.get(10, 50), player.getY() + Rnd.get(10, 50), -967, 0, false, 0);
				_varangkaMinion1.setRunning();
				((Attackable) _varangkaMinion1).addDamageHate(_varangka.getTarget().getActingPlayer(), 1, 99999);
				_varangkaMinion1.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _varangka.getTarget().getActingPlayer());
			}
			return null;
		}
		else if (event.equalsIgnoreCase("minion2"))
		{
			if (!checkIfSpawned(VARANGKA + 2) && checkIfSpawned(VARANGKA))
			{
				_varangkaMinion2 = addSpawn(VARANGKA + 2, player.getX() + Rnd.get(10, 50), player.getY() + Rnd.get(10, 50), -967, 0, false, 0);
				_varangkaMinion2.setRunning();
				((Attackable) _varangkaMinion2).addDamageHate(_varangka.getTarget().getActingPlayer(), 1, 99999);
				_varangkaMinion2.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _varangka.getTarget().getActingPlayer());
			}
			return null;
		}
		
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		if (player.getLevel() < MIN_LEVEL)
		{
			return null;
		}
		
		switch (event)
		{
			case "32655-03.htm":
			case "32655-15.html":
			{
				htmltext = event;
				break;
			}
			case "32655-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32655-11.html":
			{
				if (getQuestItemsCount(player, SOUL_CORE_CONTAINING_EVIL_SPIRIT) >= SOUL_CORE_COUNT)
				{
					takeItems(player, SOUL_CORE_CONTAINING_EVIL_SPIRIT, SOUL_CORE_COUNT);
					giveItems(player, PROTECTION_SOULS_PENDANT, 1);
					htmltext = event;
				}
				else
				{
					htmltext = "32655-12.html";
				}
				break;
			}
			case "32655-13.html":
			{
				if (!hasQuestItems(player, SOUL_CORE_CONTAINING_EVIL_SPIRIT) && (getQuestItemsCount(player, RAGNA_ORCS_AMULET) >= RAGNA_ORCS_AMULET_COUNT))
				{
					qs.exitQuest(true, true);
					htmltext = event;
				}
				else
				{
					htmltext = "32655-14.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 1, 2, npc);
		if (qs != null)
		{
			final PlayerInstance member = qs.getPlayer();
			if (npc.getId() == VARANGKA)
			{
				if ((qs.getInt("cond") != 1))
				{
					return null;
				}
				_altar.doDie(killer);
				_altar = null;
				_varangka = null;
				if (checkIfSpawned(VARANGKA + 1))
				{
					_varangkaMinion1.doDie(killer);
				}
				if (checkIfSpawned(VARANGKA + 2))
				{
					_varangkaMinion2.doDie(killer);
				}
				cancelQuestTimers("minion1");
				cancelQuestTimers("minion2");
				_varangkaMinion1 = null;
				_varangkaMinion2 = null;
				final long respawn = Rnd.get(14400000, 28800000);
				GlobalVariablesManager.getInstance().set("VarangkaRespawn", Chronos.currentTimeMillis() + respawn);
				startQuestTimer("altarSpawn", respawn, null, null);
				takeItems(member, PROTECTION_SOULS_PENDANT, 1);
				return super.onKill(npc, killer, isSummon);
			}
			else if (npc.getId() == (VARANGKA + 1))
			{
				_varangkaMinion1 = null;
				startQuestTimer("minion1", Rnd.get(60000, 120000), npc, killer);
				return super.onKill(npc, killer, isSummon);
			}
			else if (npc.getId() == (VARANGKA + 2))
			{
				_varangkaMinion2 = null;
				startQuestTimer("minion2", Rnd.get(60000, 120000), npc, killer);
				return super.onKill(npc, killer, isSummon);
			}
			
			final int count = qs.getMemoStateEx(1) + 1;
			if ((count >= RAGNA_ORCS_KILLS_COUNT) && (getRandom(20) < ((count % 100) + 1)))
			{
				qs.setMemoStateEx(1, 0);
				giveItems(member, SOUL_CORE_CONTAINING_EVIL_SPIRIT, 1);
				playSound(member, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				qs.setMemoStateEx(1, count);
			}
			
			if (MONSTERS.get(npc.getId()) < Rnd.get(1d))
			{
				giveItems(member, RAGNA_ORCS_AMULET, 1);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			htmltext = (player.getLevel() >= MIN_LEVEL) ? "32655-01.htm" : "32655-02.htm";
		}
		else if (qs.isStarted())
		{
			htmltext = hasAtLeastOneQuestItem(player, RAGNA_ORCS_AMULET, SOUL_CORE_CONTAINING_EVIL_SPIRIT) ? "32655-06.html" : "32655-05.html";
		}
		return htmltext;
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance player, int damage, boolean isPet)
	{
		QuestState qs = player.getQuestState(Q00311_ExpulsionOfEvilSpirits.class.getSimpleName());
		if (qs == null)
		{
			return null;
		}
		
		if (hasQuestItems(player, PROTECTION_SOULS_PENDANT) && (Rnd.get(100) < 20))
		{
			if ((_varangka == null) && !checkIfSpawned(VARANGKA))
			{
				_varangka = addSpawn(VARANGKA, 74914, -101922, -967, 0, false, 0);
				if ((_varangkaMinion1 == null) && !checkIfSpawned(VARANGKA + 1))
				{
					_varangkaMinion1 = addSpawn(VARANGKA + 1, 74914 + Rnd.get(10, 50), -101922 + Rnd.get(10, 50), -967, 0, false, 0);
				}
				if ((_varangkaMinion2 == null) && !checkIfSpawned(VARANGKA + 2))
				{
					_varangkaMinion2 = addSpawn(VARANGKA + 2, 74914 + Rnd.get(10, 50), -101922 + Rnd.get(10, 50), -967, 0, false, 0);
				}
				
				for (Creature creature : ALTARZONE.getCharactersInside())
				{
					if ((creature instanceof Attackable) && (creature.getId() >= VARANGKA) && (creature.getId() <= (VARANGKA + 2)))
					{
						creature.setRunning();
						((Attackable) creature).addDamageHate(player, 1, 99999);
						creature.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
					}
				}
			}
		}
		else if (!hasQuestItems(player, PROTECTION_SOULS_PENDANT))
		{
			ThreadPool.schedule(new zoneCheck(player), 1000);
		}
		return super.onAttack(npc, player, damage, isPet);
	}
	
	@Override
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			ThreadPool.schedule(new zoneCheck(creature.getActingPlayer()), 1000);
		}
		return super.onEnterZone(creature, zone);
	}
	
	private class zoneCheck implements Runnable
	{
		private final static int DEBUFF_SKILL_ID = 6148;
		private final static int DEBUFF_SKILL_LEVEL = 1;
		
		private final PlayerInstance _player;
		
		protected zoneCheck(PlayerInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if ((_altar != null) && ALTARZONE.isCharacterInZone(_player))
			{
				final QuestState qs = _player.getQuestState(Q00311_ExpulsionOfEvilSpirits.class.getSimpleName());
				if (qs == null)
				{
					castDebuff(_player);
					ThreadPool.schedule(new zoneCheck(_player), 3000);
				}
				else if (!hasQuestItems(_player, PROTECTION_SOULS_PENDANT))
				{
					castDebuff(_player);
					ThreadPool.schedule(new zoneCheck(_player), 3000);
				}
			}
		}
		
		private void castDebuff(PlayerInstance player)
		{
			for (BuffInfo info : player.getEffectList().getDebuffs())
			{
				if (info.getSkill().getId() == DEBUFF_SKILL_ID)
				{
					info.getEffected().getEffectList().stopSkillEffects(SkillFinishType.REMOVED, DEBUFF_SKILL_ID);
				}
			}
			_altar.broadcastPacket(new MagicSkillUse(_altar, player, DEBUFF_SKILL_ID, DEBUFF_SKILL_LEVEL, 1000, 0));
			SkillData.getInstance().getSkill(DEBUFF_SKILL_ID, 1).applyEffects(_altar, player);
		}
	}
	
	private boolean checkIfSpawned(int npcId)
	{
		for (Creature creature : ALTARZONE.getCharactersInside())
		{
			if (creature.getId() == npcId)
			{
				return true;
			}
		}
		return false;
	}
}
