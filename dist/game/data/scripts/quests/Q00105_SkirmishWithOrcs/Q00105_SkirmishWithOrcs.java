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
package quests.Q00105_SkirmishWithOrcs;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.Util;

import quests.Q00281_HeadForTheHills.Q00281_HeadForTheHills;

/**
 * Skimirish with Orcs (105)
 * @author janiko
 */
public class Q00105_SkirmishWithOrcs extends Quest
{
	// NPC
	private static final int KENDNELL = 30218;
	// Items
	private static final int KENDELLS_1ST_ORDER = 1836;
	private static final int KENDELLS_2ND_ORDER = 1837;
	private static final int KENDELLS_3RD_ORDER = 1838;
	private static final int KENDELLS_4TH_ORDER = 1839;
	private static final int KENDELLS_5TH_ORDER = 1840;
	private static final int KENDELLS_6TH_ORDER = 1841;
	private static final int KENDELLS_7TH_ORDER = 1842;
	private static final int KENDELLS_8TH_ORDER = 1843;
	private static final int KABOO_CHIEFS_1ST_TORQUE = 1844;
	private static final int KABOO_CHIEFS_2ST_TORQUE = 1845;
	private static final Map<Integer, Integer> MONSTER_DROP = new HashMap<>();
	static
	{
		MONSTER_DROP.put(27059, KENDELLS_1ST_ORDER); // Uoph (Kaboo Chief)
		MONSTER_DROP.put(27060, KENDELLS_2ND_ORDER); // Kracha (Kaboo Chief)
		MONSTER_DROP.put(27061, KENDELLS_3RD_ORDER); // Batoh (Kaboo Chief)
		MONSTER_DROP.put(27062, KENDELLS_4TH_ORDER); // Tanukia (Kaboo Chief)
		MONSTER_DROP.put(27064, KENDELLS_5TH_ORDER); // Turel (Kaboo Chief)
		MONSTER_DROP.put(27065, KENDELLS_6TH_ORDER); // Roko (Kaboo Chief)
		MONSTER_DROP.put(27067, KENDELLS_7TH_ORDER); // Kamut (Kaboo Chief)
		MONSTER_DROP.put(27068, KENDELLS_8TH_ORDER); // Murtika (Kaboo Chief)
	}
	private static final int[] KENDNELLS_ORDERS =
	{
		KENDELLS_1ST_ORDER,
		KENDELLS_2ND_ORDER,
		KENDELLS_3RD_ORDER,
		KENDELLS_4TH_ORDER,
		KENDELLS_5TH_ORDER,
		KENDELLS_6TH_ORDER,
		KENDELLS_7TH_ORDER,
		KENDELLS_8TH_ORDER
	};
	// Misc
	private static final int MIN_LEVEL = 10;
	
	public Q00105_SkirmishWithOrcs()
	{
		super(105);
		addStartNpc(KENDNELL);
		addTalkId(KENDNELL);
		addKillId(MONSTER_DROP.keySet());
		registerQuestItems(KENDNELLS_ORDERS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		switch (event)
		{
			case "30218-04.html":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, KENDNELLS_ORDERS[getRandom(0, 3)], 1);
					htmltext = event;
				}
				break;
			}
			case "30218-05.html":
			{
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && Util.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, true))
		{
			switch (npc.getId())
			{
				case 27059:
				case 27060:
				case 27061:
				case 27062:
				{
					if (qs.isCond(1) && hasQuestItems(killer, MONSTER_DROP.get(npc.getId())))
					{
						giveItems(killer, KABOO_CHIEFS_1ST_TORQUE, 1);
						qs.setCond(2, true);
					}
					break;
				}
				case 27064:
				case 27065:
				case 27067:
				case 27068:
				{
					if (qs.isCond(3) && hasQuestItems(killer, MONSTER_DROP.get(npc.getId())))
					{
						giveItems(killer, KABOO_CHIEFS_2ST_TORQUE, 1);
						qs.setCond(4, true);
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (talker.getRace() == Race.ELF)
				{
					htmltext = (talker.getLevel() >= MIN_LEVEL) ? "30218-03.htm" : "30218-02.htm";
				}
				else
				{
					htmltext = "30218-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (hasAtLeastOneQuestItem(talker, KENDELLS_1ST_ORDER, KENDELLS_2ND_ORDER, KENDELLS_3RD_ORDER, KENDELLS_4TH_ORDER))
				{
					htmltext = "30218-06.html";
				}
				if (qs.isCond(2) && hasQuestItems(talker, KABOO_CHIEFS_1ST_TORQUE))
				{
					for (int i = 0; i < 4; i++)
					{
						takeItems(talker, KENDNELLS_ORDERS[i], -1);
					}
					takeItems(talker, KABOO_CHIEFS_1ST_TORQUE, 1);
					giveItems(talker, KENDNELLS_ORDERS[getRandom(4, 7)], 1);
					qs.setCond(3, true);
					htmltext = "30218-07.html";
				}
				if (hasAtLeastOneQuestItem(talker, KENDELLS_5TH_ORDER, KENDELLS_6TH_ORDER, KENDELLS_7TH_ORDER, KENDELLS_8TH_ORDER))
				{
					htmltext = "30218-08.html";
				}
				if (qs.isCond(4) && hasQuestItems(talker, KABOO_CHIEFS_2ST_TORQUE))
				{
					Q00281_HeadForTheHills.giveNewbieReward(talker);
					talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
					giveAdena(talker, 17599, true);
					addExpAndSp(talker, 41478, 3555);
					qs.exitQuest(false, true);
					htmltext = "30218-09.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(talker);
				break;
			}
		}
		return htmltext;
	}
}