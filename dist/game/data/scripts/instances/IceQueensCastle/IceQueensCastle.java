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
package instances.IceQueensCastle;

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.Movie;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;

import instances.AbstractInstance;
import quests.Q10285_MeetingSirra.Q10285_MeetingSirra;

/**
 * Ice Queen's Castle instance zone.
 * @author Adry_85
 */
public class IceQueensCastle extends AbstractInstance
{
	// NPCs
	private static final int FREYA = 18847;
	private static final int BATTALION_LEADER = 18848;
	private static final int LEGIONNAIRE = 18849;
	private static final int MERCENARY_ARCHER = 18926;
	private static final int ARCHERY_KNIGHT = 22767;
	private static final int JINIA = 32781;
	// Locations
	private static final Location START_LOC = new Location(114000, -112357, -11200, 0, 0);
	private static final Location EXIT_LOC = new Location(113883, -108777, -848, 0, 0);
	private static final Location FREYA_LOC = new Location(114730, -114805, -11200, 50, 0);
	// Skill
	private static SkillHolder ETHERNAL_BLIZZARD = new SkillHolder(6276, 1);
	// Misc
	private static final int TEMPLATE_ID = 137;
	private static final int ICE_QUEEN_DOOR = 23140101;
	private static final int MIN_LV = 82;
	
	private IceQueensCastle()
	{
		addStartNpc(JINIA);
		addTalkId(JINIA);
		addSpawnId(FREYA);
		addSpellFinishedId(FREYA);
		addCreatureSeeId(BATTALION_LEADER, LEGIONNAIRE, MERCENARY_ARCHER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "ATTACK_KNIGHT":
			{
				World.getInstance().forEachVisibleObject(npc, Creature.class, character ->
				{
					if ((character.getId() == ARCHERY_KNIGHT) && !character.isDead() && !((Attackable) character).isDecayed())
					{
						npc.setRunning();
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, character);
						((Attackable) npc).addDamageHate(character, 0, 999999);
					}
				});
				startQuestTimer("ATTACK_KNIGHT", 3000, npc, null);
				break;
			}
			case "TIMER_MOVING":
			{
				if (npc != null)
				{
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, FREYA_LOC);
				}
				break;
			}
			case "TIMER_BLIZZARD":
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_CAN_NO_LONGER_STAND_BY);
				npc.stopMove(null);
				npc.setTarget(player);
				npc.doCast(ETHERNAL_BLIZZARD.getSkill());
				break;
			}
			case "TIMER_SCENE_21":
			{
				if (npc != null)
				{
					playMovie(player, Movie.SC_BOSS_FREYA_FORCED_DEFEAT);
					startQuestTimer("TIMER_PC_LEAVE", 24000, null, player);
					npc.deleteMe();
				}
				break;
			}
			case "TIMER_PC_LEAVE":
			{
				final QuestState qs = player.getQuestState(Q10285_MeetingSirra.class.getSimpleName());
				if ((qs != null))
				{
					qs.setMemoState(3);
					qs.setCond(10, true);
					final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
					world.removeAllowed(player);
					player.setInstanceId(0);
					player.teleToLocation(EXIT_LOC, 0);
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() && npc.isScriptValue(0))
		{
			World.getInstance().forEachVisibleObject(npc, Creature.class, character ->
			{
				if ((character.getId() == ARCHERY_KNIGHT) && !character.isDead() && !((Attackable) character).isDecayed())
				{
					npc.setRunning();
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, character);
					((Attackable) npc).addDamageHate(character, 0, 999999);
					npc.setScriptValue(1);
					startQuestTimer("ATTACK_KNIGHT", 5000, npc, null);
				}
			});
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.S1_MAY_THE_PROTECTION_OF_THE_GODS_BE_UPON_YOU, creature.getName());
		}
		return super.onCreatureSee(npc, creature);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		startQuestTimer("TIMER_MOVING", 60000, npc, null);
		startQuestTimer("TIMER_BLIZZARD", 180000, npc, null);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onSpellFinished(Npc npc, PlayerInstance player, Skill skill)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if (world != null)
		{
			final PlayerInstance leader = world.getParameters().getObject("player", PlayerInstance.class);
			if ((skill == ETHERNAL_BLIZZARD.getSkill()) && (leader != null))
			{
				startQuestTimer("TIMER_SCENE_21", 1000, npc, leader);
			}
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		enterInstance(talker, TEMPLATE_ID);
		return super.onTalk(npc, talker);
	}
	
	@Override
	public void onEnterInstance(PlayerInstance player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
			world.setParameter("player", player);
			world.openDoor(ICE_QUEEN_DOOR);
		}
		teleportPlayer(player, START_LOC, world.getInstanceId(), false);
	}
	
	@Override
	protected boolean checkConditions(PlayerInstance player)
	{
		if (player.getLevel() < MIN_LV)
		{
			player.sendPacket(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
			return false;
		}
		return true;
	}
	
	public static void main(String[] args)
	{
		new IceQueensCastle();
	}
}
