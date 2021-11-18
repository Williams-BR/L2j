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
package ai.areas.Gracia.AI.NPC.EkimusMouth;

import org.l2jmobius.gameserver.instancemanager.SoIManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.QuestState;

import ai.AbstractNpcAI;

public class EkimusMouth extends AbstractNpcAI
{
	// NPC
	private static final int EKIMUS_MOUTH = 32537;
	
	public EkimusMouth()
	{
		addStartNpc(EKIMUS_MOUTH);
		addFirstTalkId(EKIMUS_MOUTH);
		addTalkId(EKIMUS_MOUTH);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = event;
		QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			qs = newQuestState(player);
		}
		
		if (event.equalsIgnoreCase("hos_enter"))
		{
			if (SoIManager.getCurrentStage() == 1)
			{
				htmltext = "32537-1.htm";
			}
			else if (SoIManager.getCurrentStage() == 4)
			{
				htmltext = "32537-2.htm";
			}
		}
		else if (event.equalsIgnoreCase("hoe_enter"))
		{
			if (SoIManager.getCurrentStage() == 1)
			{
				htmltext = "32537-3.htm";
			}
			else if (SoIManager.getCurrentStage() == 4)
			{
				htmltext = "32537-4.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			qs = newQuestState(player);
		}
		return "32537.htm";
	}
}