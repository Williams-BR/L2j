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
package quests.Q00636_TruthBeyond;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * The Truth Beyond the Gate (636)<br>
 * Original Jython script by Polo, BiTi and DrLecter.
 * @author DS
 */
public class Q00636_TruthBeyond extends Quest
{
	private static final int ELIYAH = 31329;
	private static final int FLAURON = 32010;
	private static final int ZONE = 30100;
	private static final int VISITOR_MARK = 8064;
	private static final int FADED_MARK = 8065;
	private static final int MARK = 8067;
	
	public Q00636_TruthBeyond()
	{
		super(636);
		addStartNpc(ELIYAH);
		addTalkId(ELIYAH, FLAURON);
		addEnterZoneId(ZONE);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		if ("31329-04.htm".equals(event))
		{
			qs.startQuest();
		}
		else if ("32010-02.htm".equals(event))
		{
			giveItems(player, VISITOR_MARK, 1);
			qs.exitQuest(true, true);
		}
		return event;
	}
	
	@Override
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		// QuestState already null on enter because quest is finished
		if (creature.isPlayer() && creature.getActingPlayer().destroyItemByItemId("Mark", VISITOR_MARK, 1, creature, false))
		{
			creature.getActingPlayer().addItem("Mark", FADED_MARK, 1, creature, true);
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		if (npc.getId() == ELIYAH)
		{
			if (hasQuestItems(player, VISITOR_MARK) || hasQuestItems(player, FADED_MARK) || hasQuestItems(player, MARK))
			{
				qs.exitQuest(true);
				return "31329-mark.htm";
			}
			if (qs.getState() == State.CREATED)
			{
				if (player.getLevel() > 72)
				{
					return "31329-02.htm";
				}
				
				qs.exitQuest(true);
				return "31329-01.htm";
			}
			else if (qs.getState() == State.STARTED)
			{
				return "31329-05.htm";
			}
		}
		else if (qs.getState() == State.STARTED) // Flauron only
		{
			if (qs.isCond(1))
			{
				return "32010-01.htm";
			}
			qs.exitQuest(true);
			return "32010-03.htm";
		}
		return getNoQuestMsg(player);
	}
}
