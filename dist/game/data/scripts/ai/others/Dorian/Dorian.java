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
package ai.others.Dorian;

import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;

import ai.AbstractNpcAI;
import quests.Q00024_InhabitantsOfTheForestOfTheDead.Q00024_InhabitantsOfTheForestOfTheDead;

/**
 * Dorian (Raid Fighter) - Quest AI
 * @author malyelfik
 */
public class Dorian extends AbstractNpcAI
{
	// NPC
	private static final int DORIAN = 25332;
	// Items
	private static final int SILVER_CROSS = 7153;
	private static final int BROKEN_SILVER_CROSS = 7154;
	
	private Dorian()
	{
		addCreatureSeeId(DORIAN);
	}
	
	@Override
	public String onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			final PlayerInstance pl = creature.getActingPlayer();
			final QuestState qs = pl.getQuestState(Q00024_InhabitantsOfTheForestOfTheDead.class.getSimpleName());
			if ((qs != null) && qs.isCond(3))
			{
				takeItems(pl, SILVER_CROSS, -1);
				giveItems(pl, BROKEN_SILVER_CROSS, 1);
				qs.setCond(4, true);
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THAT_SIGN);
			}
		}
		return super.onCreatureSee(npc, creature);
	}
	
	public static void main(String[] args)
	{
		new Dorian();
	}
}