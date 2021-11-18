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
package quests.Q00106_ForgottenTruth;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.QuestSound;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.Util;

import quests.Q00281_HeadForTheHills.Q00281_HeadForTheHills;

/**
 * Forgotten Truth (106)
 * @author janiko
 */
public class Q00106_ForgottenTruth extends Quest
{
	// NPCs
	private static final int THIFIELL = 30358;
	private static final int KARTA = 30133;
	// Monster
	private static final int TUMRAN_ORC_BRIGAND = 27070;
	// Items
	private static final int ONYX_TALISMAN1 = 984;
	private static final int ONYX_TALISMAN2 = 985;
	private static final int ANCIENT_SCROLL = 986;
	private static final int ANCIENT_CLAY_TABLET = 987;
	private static final int KARTAS_TRANSLATION = 988;
	// Misc
	private static final int MIN_LEVEL = 10;
	// Rewards
	private static final int ELDRITCH_DAGGER = 989;
	private static final int ELDRITCH_STAFF = 2373;
	private static final ItemHolder[] REWARDS =
	{
		new ItemHolder(1060, 100), // Lesser Healing Potion
		new ItemHolder(4412, 10), // Echo Crystal - Theme of Battle
		new ItemHolder(4413, 10), // Echo Crystal - Theme of Love
		new ItemHolder(4414, 10), // Echo Crystal - Theme of Solitude
		new ItemHolder(4415, 10), // Echo Crystal - Theme of Feast
		new ItemHolder(4416, 10), // Echo Crystal - Theme of Celebration
	};
	
	public Q00106_ForgottenTruth()
	{
		super(106);
		addStartNpc(THIFIELL);
		addTalkId(THIFIELL, KARTA);
		addKillId(TUMRAN_ORC_BRIGAND);
		registerQuestItems(KARTAS_TRANSLATION, ONYX_TALISMAN1, ONYX_TALISMAN2, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET);
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
			case "30358-04.htm":
			{
				htmltext = event;
				break;
			}
			case "30358-05.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, ONYX_TALISMAN1, 1);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(2) && Util.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, true) && (getRandom(100) < 20) && hasQuestItems(killer, ONYX_TALISMAN2))
		{
			if (!hasQuestItems(killer, ANCIENT_SCROLL))
			{
				giveItems(killer, ANCIENT_SCROLL, 1);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
			}
			else if (!hasQuestItems(killer, ANCIENT_CLAY_TABLET))
			{
				qs.setCond(3, true);
				giveItems(killer, ANCIENT_CLAY_TABLET, 1);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		
		switch (npc.getId())
		{
			case THIFIELL:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						if (talker.getRace() == Race.DARK_ELF)
						{
							htmltext = talker.getLevel() >= MIN_LEVEL ? "30358-03.htm" : "30358-02.htm";
						}
						else
						{
							htmltext = "30358-01.htm";
						}
						break;
					}
					case State.STARTED:
					{
						if (hasAtLeastOneQuestItem(talker, ONYX_TALISMAN1, ONYX_TALISMAN2) && !hasQuestItems(talker, KARTAS_TRANSLATION))
						{
							htmltext = "30358-06.html";
						}
						else if (qs.isCond(4) && hasQuestItems(talker, KARTAS_TRANSLATION))
						{
							Q00281_HeadForTheHills.giveNewbieReward(talker);
							talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
							giveAdena(talker, 10266, true);
							addExpAndSp(talker, 24195, 2074);
							
							giveItems(talker, qs.getPlayer().isMageClass() ? ELDRITCH_STAFF : ELDRITCH_DAGGER, 1);
							
							for (ItemHolder reward : REWARDS)
							{
								giveItems(talker, reward);
							}
							
							qs.exitQuest(false, true);
							htmltext = "30358-07.html";
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(talker);
						break;
					}
				}
				break;
			}
			case KARTA:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							if (hasQuestItems(talker, ONYX_TALISMAN1))
							{
								qs.setCond(2, true);
								takeItems(talker, ONYX_TALISMAN1, -1);
								giveItems(talker, ONYX_TALISMAN2, 1);
								htmltext = "30133-01.html";
							}
							break;
						}
						case 2:
						{
							if (hasQuestItems(talker, ONYX_TALISMAN2))
							{
								htmltext = "30133-02.html";
							}
							break;
						}
						case 3:
						{
							if (hasQuestItems(talker, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET))
							{
								qs.setCond(4, true);
								takeItems(talker, -1, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET, ONYX_TALISMAN2);
								giveItems(talker, KARTAS_TRANSLATION, 1);
								htmltext = "30133-03.html";
							}
							break;
						}
						case 4:
						{
							if (hasQuestItems(talker, KARTAS_TRANSLATION))
							{
								htmltext = "30133-04.html";
							}
							break;
						}
					}
				}
				break;
			}
		}
		return htmltext;
	}
}