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
package org.l2jmobius.gameserver.model.votereward;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Anarchy
 */
public abstract class VoteSystem implements Runnable
{
	protected static final Logger LOGGER = Logger.getLogger(VoteSystem.class.getName());
	
	private static List<VoteSystem> voteSystems = new ArrayList<>();
	
	private final int votesDiff;
	private final boolean allowReport;
	private final int boxes;
	private final Map<Integer, Integer> rewards;
	private int lastVotes = 0;
	private final Map<String, Integer> playerIps = new HashMap<>();
	
	public static void initialize()
	{
		if (Config.ALLOW_NETWORK_VOTE_REWARD || Config.ALLOW_TOPZONE_VOTE_REWARD || Config.ALLOW_HOPZONE_VOTE_REWARD || Config.ALLOW_L2TOP_VOTE_REWARD || Config.ALLOW_L2JBRASIL_VOTE_REWARD)
		{
			LOGGER.info("VoteSystem: Initialized.");
			if (Config.ALLOW_NETWORK_VOTE_REWARD)
			{
				voteSystems.add(new Network(Config.NETWORK_VOTES_DIFFERENCE, Config.ALLOW_NETWORK_GAME_SERVER_REPORT, Config.NETWORK_DUALBOXES_ALLOWED, Config.NETWORK_REWARD, Config.NETWORK_REWARD_CHECK_TIME));
				LOGGER.info("VoteSystem: L2network.eu votes enabled.");
			}
			else
			{
				LOGGER.info("VoteSystem: L2network.eu votes disabled.");
			}
			if (Config.ALLOW_TOPZONE_VOTE_REWARD)
			{
				voteSystems.add(new Topzone(Config.TOPZONE_VOTES_DIFFERENCE, Config.ALLOW_TOPZONE_GAME_SERVER_REPORT, Config.TOPZONE_DUALBOXES_ALLOWED, Config.TOPZONE_REWARD, Config.TOPZONE_REWARD_CHECK_TIME));
				LOGGER.info("VoteSystem: Topzone.com votes enabled.");
			}
			else
			{
				LOGGER.info("VoteSystem: Topzone.com votes disabled.");
			}
			if (Config.ALLOW_HOPZONE_VOTE_REWARD)
			{
				voteSystems.add(new Hopzone(Config.HOPZONE_VOTES_DIFFERENCE, Config.ALLOW_HOPZONE_GAME_SERVER_REPORT, Config.HOPZONE_DUALBOXES_ALLOWED, Config.HOPZONE_REWARD, Config.HOPZONE_REWARD_CHECK_TIME));
				LOGGER.info("VoteSystem: Hopzone.net votes enabled.");
			}
			else
			{
				LOGGER.info("VoteSystem: Hopzone.net votes disabled.");
			}
			if (Config.ALLOW_L2TOP_VOTE_REWARD)
			{
				voteSystems.add(new L2top(Config.L2TOP_VOTES_DIFFERENCE, Config.ALLOW_L2TOP_GAME_SERVER_REPORT, Config.L2TOP_DUALBOXES_ALLOWED, Config.L2TOP_REWARD, Config.L2TOP_REWARD_CHECK_TIME));
				LOGGER.info("VoteSystem: L2top.co votes enabled.");
			}
			else
			{
				LOGGER.info("VoteSystem: L2top.co votes disabled.");
			}
			if (Config.ALLOW_L2JBRASIL_VOTE_REWARD)
			{
				voteSystems.add(new L2jbrasil(Config.L2JBRASIL_VOTES_DIFFERENCE, Config.ALLOW_L2JBRASIL_GAME_SERVER_REPORT, Config.L2JBRASIL_DUALBOXES_ALLOWED, Config.L2JBRASIL_REWARD, Config.L2JBRASIL_REWARD_CHECK_TIME));
				LOGGER.info("VoteSystem: L2JBrasil votes enabled.");
			}
			else
			{
				LOGGER.info("VoteSystem: L2JBrasil votes disabled.");
			}
		}
		else
		{
			LOGGER.info("VoteSystem: Disabled.");
		}
	}
	
	public VoteSystem(int votesDiff, boolean allowReport, int boxes, Map<Integer, Integer> rewards, int checkMins)
	{
		this.votesDiff = votesDiff;
		this.allowReport = allowReport;
		this.boxes = boxes;
		this.rewards = rewards;
		ThreadPool.scheduleAtFixedRate(this, checkMins * 1000 * 60, checkMins * 1000 * 60);
	}
	
	protected void reward()
	{
		final int currentVotes = getVotes();
		if (currentVotes == -1)
		{
			LOGGER.info("VoteSystem: There was a problem on getting server votes.");
			return;
		}
		
		if (lastVotes == 0)
		{
			lastVotes = currentVotes;
			announce(getSiteName() + ": Current vote count is " + currentVotes + ".");
			announce(getSiteName() + ": We need " + ((lastVotes + votesDiff) - currentVotes) + " vote(s) for reward.");
			if (allowReport)
			{
				LOGGER.info("VoteSystem: Server votes on " + getSiteName() + ": " + currentVotes);
				LOGGER.info("VoteSystem: Votes needed for reward: " + ((lastVotes + votesDiff) - currentVotes));
			}
			return;
		}
		
		if (currentVotes >= (lastVotes + votesDiff))
		{
			final Collection<PlayerInstance> pls = World.getInstance().getPlayers();
			if (allowReport)
			{
				LOGGER.info("VoteSystem: Server votes on " + getSiteName() + ": " + currentVotes);
				LOGGER.info("VoteSystem: Votes needed for next reward: " + ((currentVotes + votesDiff) - currentVotes));
			}
			announce(getSiteName() + ": Everyone has been rewarded.");
			announce(getSiteName() + ": Current vote count is " + currentVotes + ".");
			announce(getSiteName() + ": We need " + votesDiff + " vote(s) for next reward.");
			for (PlayerInstance p : pls)
			{
				if ((p.getClient() == null) || p.getClient().isDetached())
				{
					continue;
				}
				
				boolean canReward = false;
				final String pIp = p.getClient().getConnectionAddress().getHostAddress();
				if (playerIps.containsKey(pIp))
				{
					final int count = playerIps.get(pIp);
					if (count < boxes)
					{
						playerIps.remove(pIp);
						playerIps.put(pIp, count + 1);
						canReward = true;
					}
				}
				else
				{
					canReward = true;
					playerIps.put(pIp, 1);
				}
				if (canReward)
				{
					for (Entry<Integer, Integer> entry : rewards.entrySet())
					{
						p.addItem("Vote reward.", entry.getKey(), entry.getValue(), p, true);
					}
				}
				else
				{
					p.sendMessage("Already " + boxes + " character(s) of your ip have been rewarded, so this character won't be rewarded.");
				}
			}
			playerIps.clear();
			
			lastVotes = currentVotes;
		}
		else
		{
			if (allowReport)
			{
				LOGGER.info("VoteSystem: Server votes on " + getSiteName() + ": " + currentVotes);
				LOGGER.info("VoteSystem: Votes needed for next reward: " + ((lastVotes + votesDiff) - currentVotes));
			}
			announce(getSiteName() + ": Current vote count is " + currentVotes + ".");
			announce(getSiteName() + ": We need " + ((lastVotes + votesDiff) - currentVotes) + " vote(s) for reward.");
		}
	}
	
	private void announce(String msg)
	{
		Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.CRITICAL_ANNOUNCE, "", msg));
	}
	
	public abstract int getVotes();
	
	public abstract String getSiteName();
}