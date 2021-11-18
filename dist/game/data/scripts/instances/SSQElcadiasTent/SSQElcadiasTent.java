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
package instances.SSQElcadiasTent;

import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.quest.QuestState;

import instances.AbstractInstance;
import quests.Q10292_SevenSignsGirlOfDoubt.Q10292_SevenSignsGirlOfDoubt;
import quests.Q10293_SevenSignsForbiddenBookOfTheElmoreAdenKingdom.Q10293_SevenSignsForbiddenBookOfTheElmoreAdenKingdom;
import quests.Q10294_SevenSignsToTheMonasteryOfSilence.Q10294_SevenSignsToTheMonasteryOfSilence;
import quests.Q10296_SevenSignsOneWhoSeeksThePowerOfTheSeal.Q10296_SevenSignsOneWhoSeeksThePowerOfTheSeal;

/**
 * Elcadia's Tent instance zone.
 * @author Adry_85
 */
public class SSQElcadiasTent extends AbstractInstance
{
	// NPCs
	private static final int ELCADIA = 32784;
	private static final int GRUFF_LOOKING_MAN = 32862;
	// Locations
	private static final Location START_LOC = new Location(89797, -238081, -9632);
	private static final Location EXIT_LOC = new Location(43347, -87923, -2820);
	// Misc
	private static final int TEMPLATE_ID = 158;
	
	private SSQElcadiasTent()
	{
		addFirstTalkId(GRUFF_LOOKING_MAN, ELCADIA);
		addStartNpc(GRUFF_LOOKING_MAN, ELCADIA);
		addTalkId(GRUFF_LOOKING_MAN, ELCADIA);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		if (npc.getId() == GRUFF_LOOKING_MAN)
		{
			final QuestState qs10292 = talker.getQuestState(Q10292_SevenSignsGirlOfDoubt.class.getSimpleName());
			final QuestState qs10293 = talker.getQuestState(Q10293_SevenSignsForbiddenBookOfTheElmoreAdenKingdom.class.getSimpleName());
			final QuestState qs10294 = talker.getQuestState(Q10294_SevenSignsToTheMonasteryOfSilence.class.getSimpleName());
			final QuestState qs10296 = talker.getQuestState(Q10296_SevenSignsOneWhoSeeksThePowerOfTheSeal.class.getSimpleName());
			if (((qs10292 != null) && (qs10292.getMemoState() > 1) && (qs10292.getMemoState() < 9)) //
				|| ((qs10292 != null) && qs10292.isCompleted() && (qs10293 == null)) //
				|| ((qs10293 != null) && qs10293.isStarted()) //
				|| ((qs10293 != null) && qs10293.isCompleted() && (qs10294 == null)) //
				|| ((qs10296 != null) && (qs10296.getMemoState() > 2) && (qs10296.getMemoState() < 4)))
			{
				enterInstance(talker, TEMPLATE_ID);
			}
			else
			{
				return "32862-01.html";
			}
		}
		else
		{
			final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(talker);
			world.removeAllowed(talker);
			talker.setInstanceId(0);
			talker.teleToLocation(EXIT_LOC);
		}
		return super.onTalk(npc, talker);
	}
	
	@Override
	public void onEnterInstance(PlayerInstance player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
		}
		teleportPlayer(player, START_LOC, world.getInstanceId(), false);
	}
	
	public static void main(String[] args)
	{
		new SSQElcadiasTent();
	}
}