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
package ai.bosses.SeerUgoros;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

import ai.AbstractNpcAI;
import quests.Q00288_HandleWithCare.Q00288_HandleWithCare;
import quests.Q00423_TakeYourBestShot.Q00423_TakeYourBestShot;

/**
 * @author RobikBobik
 */
public class SeerUgoros extends AbstractNpcAI
{
	// NPCs
	private static final int SEER_UGOROS = 18863;
	private static final int BATRACOS = 32740;
	private static final int WEED_ID = 18867;
	// Items
	private static final int SEER_UGOROS_PASS = 15496;
	private static final int HIGH_GRADE_LIZARD_SCALE = 15497;
	private static final int MIDDLE_GRADE_LIZARD_SCALE = 15498;
	// Skill
	private static final Skill UGOROS_SKILL = SkillData.getInstance().getSkill(6426, 1);
	// Locations
	private static final Location UGOROS_SPAWN_LOCATION = new Location(96804, 85604, -3720, 34360);
	private static final Location BATRACOS_SPAWN_LOCATION = new Location(96782, 85918, -3720, 34360);
	private static final Location ENTER_LOCATION = new Location(95984, 85692, -3720);
	private static final Location EXIT_LOCATION = new Location(94763, 83562, -3425);
	// State
	private static final byte ALIVE = 0;
	private static final byte FIGHTING = 1;
	private static final byte DEAD = 2;
	// Misc
	protected static byte _state = DEAD;
	protected static boolean _weedAttack = false;
	private static boolean _weedKilledByPlayer = false;
	private static boolean _killedOneWeed = false;
	protected static Attackable _weed = null;
	protected static Attackable _ugoros = null;
	protected static PlayerInstance _attacker = null;
	protected static ScheduledFuture<?> _thinkTask = null;
	
	private SeerUgoros()
	{
		addStartNpc(BATRACOS);
		addTalkId(BATRACOS);
		addKillId(SEER_UGOROS);
		addAttackId(WEED_ID);
		
		startQuestTimer("ugoros_respawn", 60000, null, null);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "ugoros_respawn":
			{
				if (_ugoros == null)
				{
					_ugoros = (Attackable) addSpawn(SEER_UGOROS, UGOROS_SPAWN_LOCATION, false, 0);
					_state = ALIVE;
					broadcastInRegion(_ugoros, NpcStringId.LISTEN_OH_TANTAS_I_HAVE_RETURNED_THE_PROPHET_YUGOROS_OF_THE_BLACK_ABYSS_IS_WITH_ME_SO_DO_NOT_BE_AFRAID);
					startQuestTimer("ugoros_shout", 120000, null, null);
				}
				break;
			}
			case "ugoros_shout":
			{
				if (_state == FIGHTING)
				{
					if (_attacker == null)
					{
						_state = ALIVE;
					}
					else if ((_ugoros == null) || (_attacker.calculateDistance2D(_ugoros) > 2000))
					{
						_state = ALIVE;
						_attacker = null;
					}
				}
				else if (_state == ALIVE)
				{
					broadcastInRegion(_ugoros, NpcStringId.LISTEN_OH_TANTAS_THE_BLACK_ABYSS_IS_FAMISHED_FIND_SOME_FRESH_OFFERINGS);
				}
				startQuestTimer("ugoros_shout", 120000, null, null);
				break;
			}
			case "ugoros_attack":
			{
				if (_attacker != null)
				{
					changeAttackTarget(_attacker);
					final NpcSay packet = new NpcSay(_ugoros.getObjectId(), ChatType.NPC_GENERAL, _ugoros.getId(), NpcStringId.WELCOME_S1_LET_US_SEE_IF_YOU_HAVE_BROUGHT_A_WORTHY_OFFERING_FOR_THE_BLACK_ABYSS);
					packet.addStringParameter(_attacker.getName());
					_ugoros.broadcastPacket(packet);
					if (_thinkTask != null)
					{
						_thinkTask.cancel(true);
					}
					_thinkTask = ThreadPool.scheduleAtFixedRate(new ThinkTask(), 1000, 3000);
				}
				break;
			}
			case "weed_check":
			{
				if (_weedAttack && (_ugoros != null) && (_weed != null))
				{
					if (_weed.isDead() && !_weedKilledByPlayer)
					{
						_killedOneWeed = true;
						_weed = null;
						_weedAttack = false;
						_ugoros.getStatus().setCurrentHp(_ugoros.getStatus().getCurrentHp() + (_ugoros.getMaxHp() * 0.2));
						_ugoros.broadcastPacket(new NpcSay(_ugoros.getObjectId(), ChatType.NPC_GENERAL, _ugoros.getId(), NpcStringId.WHAT_A_FORMIDABLE_FOE_BUT_I_HAVE_THE_ABYSS_WEED_GIVEN_TO_ME_BY_THE_BLACK_ABYSS_LET_ME_SEE));
					}
					else
					{
						startQuestTimer("weed_check", 2000, null, null);
					}
				}
				else
				{
					_weed = null;
					_weedAttack = false;
				}
				break;
			}
			case "ugoros_expel":
			{
				if (_attacker != null)
				{
					_attacker.teleToLocation(EXIT_LOCATION);
					_attacker = null;
				}
				break;
			}
			case "teleportInside":
			{
				if ((player != null) && (_state == ALIVE))
				{
					if (hasAtLeastOneQuestItem(player, SEER_UGOROS_PASS))
					{
						_state = FIGHTING;
						_attacker = player;
						_killedOneWeed = false;
						takeItems(player, SEER_UGOROS_PASS, 1);
						player.teleToLocation(ENTER_LOCATION);
						startQuestTimer("ugoros_attack", 2000, null, null);
						
						final QuestState qs = player.getQuestState(Q00288_HandleWithCare.class.getSimpleName());
						if (qs != null)
						{
							qs.set("drop", "1");
						}
					}
					else
					{
						final QuestState qs = player.getQuestState(Q00423_TakeYourBestShot.class.getSimpleName());
						if (qs == null)
						{
							return "<html><body>Gatekeeper Batracos:<br>You look too inexperienced to make a journey to see Tanta Seer Ugoros. If you can convince Chief Investigator Johnny that you should go, then I will let you pass. Johnny has been everywhere and done everything. He may not be of my people but he has my respect, and anyone who has his will in turn have mine as well.<br></body></html>";
						}
						return "<html><body>Gatekeeper Batracos:<br>Tanta Seer Ugoros is hard to find. You'll just have to keep looking.<br></body></html>";
					}
				}
				else
				{
					return "<html><body>Gatekeeper Batracos:<br>Tanta Seer Ugoros is hard to find. You'll just have to keep looking.<br></body></html>";
				}
				break;
			}
			case "teleport_back":
			{
				if (player != null)
				{
					player.teleToLocation(EXIT_LOCATION);
					_attacker = null;
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isPet)
	{
		if (npc.isDead())
		{
			return null;
		}
		
		if ((_ugoros != null) && (_weed != null) && npc.equals(_weed))
		{
			// Reset weed
			_weed = null;
			// Reset attack state
			_weedAttack = false;
			// Set it
			_weedKilledByPlayer = true;
			// Complain
			_ugoros.broadcastPacket(new NpcSay(_ugoros.getObjectId(), ChatType.NPC_GENERAL, _ugoros.getId(), NpcStringId.NO_HOW_DARE_YOU_STOP_ME_FROM_USING_THE_ABYSS_WEED_DO_YOU_KNOW_WHAT_YOU_HAVE_DONE));
			// Cancel current think-task
			if (_thinkTask != null)
			{
				_thinkTask.cancel(true);
			}
			// Re-setup task to re-think attack again
			_thinkTask = ThreadPool.scheduleAtFixedRate(new ThinkTask(), 500, 3000);
		}
		npc.doDie(attacker);
		
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		if (_thinkTask != null)
		{
			_thinkTask.cancel(true);
			_thinkTask = null;
		}
		
		_state = DEAD;
		broadcastInRegion(_ugoros, NpcStringId.AH_HOW_COULD_I_LOSE_OH_BLACK_ABYSS_RECEIVE_ME);
		_ugoros = null;
		
		addSpawn(BATRACOS, BATRACOS_SPAWN_LOCATION, false, 60000);
		
		startQuestTimer("ugoros_expel", 50000, null, null);
		startQuestTimer("ugoros_respawn", 60000, null, null);
		
		final QuestState qs = killer.getQuestState(Q00288_HandleWithCare.class.getSimpleName());
		if ((qs != null) && qs.isCond(1) && (qs.getInt("drop") == 1))
		{
			if (_killedOneWeed)
			{
				giveItems(killer, MIDDLE_GRADE_LIZARD_SCALE, 1);
				qs.setCond(2, true);
			}
			else
			{
				giveItems(killer, HIGH_GRADE_LIZARD_SCALE, 1);
				qs.setCond(3, true);
			}
			qs.unset("drop");
		}
		
		return super.onKill(npc, killer, isSummon);
	}
	
	private void broadcastInRegion(Npc npc, NpcStringId npcString)
	{
		if (npc == null)
		{
			return;
		}
		
		final NpcSay npcSay = new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), npcString);
		for (PlayerInstance player : World.getInstance().getVisibleObjectsInRange(npc, PlayerInstance.class, 6000))
		{
			player.sendPacket(npcSay);
		}
	}
	
	private class ThinkTask implements Runnable
	{
		public ThinkTask()
		{
		}
		
		@Override
		public void run()
		{
			if ((_state == FIGHTING) && (_attacker != null) && !_attacker.isDead())
			{
				if (_weedAttack && (_weed != null))
				{
					// Dummy, just wait.
				}
				else if (Rnd.get(10) < 6)
				{
					_weed = null;
					for (Attackable attackable : World.getInstance().getVisibleObjectsInRange(_ugoros, Attackable.class, 2000))
					{
						if (!attackable.isDead() && (attackable.getId() == WEED_ID))
						{
							_weedAttack = true;
							_weed = attackable;
							changeAttackTarget(_weed);
							startQuestTimer("weed_check", 1000, null, null);
							break;
						}
					}
					if (_weed == null)
					{
						changeAttackTarget(_attacker);
					}
				}
				else
				{
					changeAttackTarget(_attacker);
				}
			}
			else
			{
				_state = ALIVE;
				_attacker = null;
				if (_thinkTask != null)
				{
					_thinkTask.cancel(true);
					_thinkTask = null;
				}
			}
		}
	}
	
	protected void changeAttackTarget(Creature attacker)
	{
		_ugoros.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		_ugoros.clearAggroList();
		_ugoros.setTarget(attacker);
		
		if (attacker instanceof Attackable)
		{
			_weedKilledByPlayer = false;
			_ugoros.disableSkill(UGOROS_SKILL, 100000);
			_ugoros.setRunning();
			_ugoros.addDamageHate(attacker, 0, Integer.MAX_VALUE);
		}
		else
		{
			_ugoros.enableSkill(UGOROS_SKILL);
			_ugoros.addDamageHate(attacker, 0, 99);
			_ugoros.setWalking();
		}
		
		_ugoros.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
	}
	
	public static void main(String[] args)
	{
		new SeerUgoros();
	}
}
