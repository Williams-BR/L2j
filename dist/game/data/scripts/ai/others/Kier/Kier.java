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
package ai.others.Kier;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.QuestState;

import ai.AbstractNpcAI;
import quests.Q00115_TheOtherSideOfTruth.Q00115_TheOtherSideOfTruth;
import quests.Q10283_RequestOfIceMerchant.Q10283_RequestOfIceMerchant;

/**
 * Kier AI.
 * @author Adry_85
 * @since 2.6.0.0
 */
public class Kier extends AbstractNpcAI
{
	// NPC
	private static final int KIER = 32022;
	
	private Kier()
	{
		addFirstTalkId(KIER);
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		final QuestState qs115 = player.getQuestState(Q00115_TheOtherSideOfTruth.class.getSimpleName());
		if (qs115 == null)
		{
			htmltext = "32022-02.html";
		}
		else if (!qs115.isCompleted())
		{
			htmltext = "32022-01.html";
		}
		
		final QuestState qs10283 = player.getQuestState(Q10283_RequestOfIceMerchant.class.getSimpleName());
		if (qs10283 != null)
		{
			if (qs10283.isMemoState(2))
			{
				htmltext = "32022-03.html";
			}
			else if (qs10283.isCompleted())
			{
				htmltext = "32022-04.html";
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new Kier();
	}
}