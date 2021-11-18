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
package org.l2jmobius.gameserver.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.enums.PartyDistributionType;
import org.l2jmobius.gameserver.instancemanager.DuelManager;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.ServitorInstance;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.partymatching.PartyMatchRoomList;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAskModifyPartyLooting;
import org.l2jmobius.gameserver.network.serverpackets.ExCloseMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExOpenMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.ExSetPartyLooting;
import org.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import org.l2jmobius.gameserver.network.serverpackets.PartyMemberPosition;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAll;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Util;

/**
 * This class serves as a container for player parties.
 * @author nuocnam
 */
public class Party extends AbstractPlayerGroup
{
	private static final Logger LOGGER = Logger.getLogger(Party.class.getName());
	
	// @formatter:off
	private static final double[] BONUS_EXP_SP =
	{
		1.0, 1.10, 1.20, 1.30, 1.40, 1.50, 2.0, 2.10, 2.20
	};
	// @formatter:on
	
	private static final Duration PARTY_POSITION_BROADCAST_INTERVAL = Duration.ofSeconds(12);
	private static final Duration PARTY_DISTRIBUTION_TYPE_REQUEST_TIMEOUT = Duration.ofSeconds(15);
	
	private final List<PlayerInstance> _members = new CopyOnWriteArrayList<>();
	private boolean _pendingInvitation = false;
	private long _pendingInviteTimeout;
	private int _partyLvl = 0;
	private PartyDistributionType _distributionType = PartyDistributionType.FINDERS_KEEPERS;
	private PartyDistributionType _changeRequestDistributionType;
	private Future<?> _changeDistributionTypeRequestTask = null;
	private Set<Integer> _changeDistributionTypeAnswers = null;
	private int _itemLastLoot = 0;
	private CommandChannel _commandChannel = null;
	private DimensionalRift _dr;
	private Future<?> _positionBroadcastTask = null;
	protected PartyMemberPosition _positionPacket;
	private boolean _disbanding = false;
	
	/**
	 * The message type send to the party members.
	 */
	public enum MessageType
	{
		EXPELLED,
		LEFT,
		NONE,
		DISCONNECTED
	}
	
	/**
	 * Construct a new Party object with a single member - the leader.
	 * @param leader the leader of this party
	 * @param partyDistributionType the item distribution rule of this party
	 */
	public Party(PlayerInstance leader, PartyDistributionType partyDistributionType)
	{
		_members.add(leader);
		_partyLvl = leader.getLevel();
		_distributionType = partyDistributionType;
	}
	
	/**
	 * Check if another player can start invitation process.
	 * @return {@code true} if this party waits for a response on an invitation, {@code false} otherwise
	 */
	public boolean getPendingInvitation()
	{
		return _pendingInvitation;
	}
	
	/**
	 * Set invitation process flag and store time for expiration.<br>
	 * Happens when a player joins party or declines to join.
	 * @param value the pending invitation state to set
	 */
	public void setPendingInvitation(boolean value)
	{
		_pendingInvitation = value;
		_pendingInviteTimeout = GameTimeTaskManager.getInstance().getGameTicks() + (PlayerInstance.REQUEST_TIMEOUT * GameTimeTaskManager.TICKS_PER_SECOND);
	}
	
	/**
	 * Check if a player invitation request is expired.
	 * @return {@code true} if time is expired, {@code false} otherwise
	 * @see org.l2jmobius.gameserver.model.actor.instance.PlayerInstance#isRequestExpired()
	 */
	public boolean isInvitationRequestExpired()
	{
		return _pendingInviteTimeout <= GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Get a random member from this party.
	 * @param itemId the ID of the item for which the member must have inventory space
	 * @param target the object of which the member must be within a certain range (must not be null)
	 * @return a random member from this party or {@code null} if none of the members have inventory space for the specified item
	 */
	private PlayerInstance getCheckedRandomMember(int itemId, Creature target)
	{
		final List<PlayerInstance> availableMembers = new ArrayList<>();
		for (PlayerInstance member : _members)
		{
			if (member.getInventory().validateCapacityByItemId(itemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE, target, member, true))
			{
				availableMembers.add(member);
			}
		}
		return !availableMembers.isEmpty() ? availableMembers.get(Rnd.get(availableMembers.size())) : null;
	}
	
	/**
	 * get next item looter
	 * @param itemId
	 * @param target
	 * @return
	 */
	private PlayerInstance getCheckedNextLooter(int itemId, Creature target)
	{
		for (int i = 0; i < getMemberCount(); i++)
		{
			if (++_itemLastLoot >= getMemberCount())
			{
				_itemLastLoot = 0;
			}
			PlayerInstance member;
			try
			{
				member = _members.get(_itemLastLoot);
				if (member.getInventory().validateCapacityByItemId(itemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE, target, member, true))
				{
					return member;
				}
			}
			catch (Exception e)
			{
				// continue, take another member if this just logged off
			}
		}
		return null;
	}
	
	/**
	 * get next item looter
	 * @param player
	 * @param itemId
	 * @param spoil
	 * @param target
	 * @return
	 */
	private PlayerInstance getActualLooter(PlayerInstance player, int itemId, boolean spoil, Creature target)
	{
		PlayerInstance looter = null;
		
		switch (_distributionType)
		{
			case RANDOM:
			{
				if (!spoil)
				{
					looter = getCheckedRandomMember(itemId, target);
				}
				break;
			}
			case RANDOM_INCLUDING_SPOIL:
			{
				looter = getCheckedRandomMember(itemId, target);
				break;
			}
			case BY_TURN:
			{
				if (!spoil)
				{
					looter = getCheckedNextLooter(itemId, target);
				}
				break;
			}
			case BY_TURN_INCLUDING_SPOIL:
			{
				looter = getCheckedNextLooter(itemId, target);
				break;
			}
		}
		return looter != null ? looter : player;
	}
	
	/**
	 * Broadcasts UI update and User Info for new party leader.
	 */
	public void broadcastToPartyMembersNewLeader()
	{
		for (PlayerInstance member : _members)
		{
			if (member != null)
			{
				member.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
				member.sendPacket(new PartySmallWindowAll(member, this));
				member.broadcastUserInfo();
			}
		}
	}
	
	/**
	 * Send a Server->Client packet to all other PlayerInstance of the Party.
	 * @param player
	 * @param msg
	 */
	public void broadcastToPartyMembers(PlayerInstance player, IClientOutgoingPacket msg)
	{
		for (PlayerInstance member : _members)
		{
			if ((member != null) && (member.getObjectId() != player.getObjectId()))
			{
				member.sendPacket(msg);
			}
		}
	}
	
	/**
	 * adds new member to party
	 * @param player
	 */
	public void addPartyMember(PlayerInstance player)
	{
		if (_members.contains(player))
		{
			return;
		}
		
		if (_changeRequestDistributionType != null)
		{
			finishLootRequest(false); // cancel on invite
		}
		
		// add player to party
		_members.add(player);
		
		// sends new member party window for all members
		// we do all actions before adding member to a list, this speeds things up a little
		player.sendPacket(new PartySmallWindowAll(player, this));
		
		// sends pets/summons of party members
		for (PlayerInstance pMember : _members)
		{
			if ((pMember != null) && pMember.hasSummon())
			{
				player.sendPacket(new ExPartyPetWindowAdd(pMember.getSummon()));
			}
		}
		
		SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_S1_S_PARTY);
		msg.addString(getLeader().getName());
		player.sendPacket(msg);
		
		msg = new SystemMessage(SystemMessageId.C1_HAS_JOINED_THE_PARTY);
		msg.addString(player.getName());
		broadcastPacket(msg);
		
		for (PlayerInstance member : _members)
		{
			if (member != player)
			{
				member.sendPacket(new PartySmallWindowAdd(player, this));
			}
		}
		
		// send the position of all party members to the new party member
		// player.sendPacket(new PartyMemberPosition(this));
		// send the position of the new party member to all party members (except the new one - he knows his own position)
		// broadcastToPartyMembers(player, new PartyMemberPosition(this));
		
		// if member has pet/summon add it to other as well
		if (player.hasSummon())
		{
			broadcastPacket(new ExPartyPetWindowAdd(player.getSummon()));
		}
		
		// adjust party level
		if (player.getLevel() > _partyLvl)
		{
			_partyLvl = player.getLevel();
		}
		
		// update partySpelled
		Summon summon;
		for (PlayerInstance member : _members)
		{
			if (member != null)
			{
				member.updateEffectIcons(true); // update party icons only
				summon = member.getSummon();
				member.broadcastUserInfo();
				if (summon != null)
				{
					summon.updateEffectIcons();
				}
			}
		}
		
		if (isInDimensionalRift())
		{
			_dr.partyMemberInvited();
		}
		
		// open the CCInformationwindow
		if (isInCommandChannel())
		{
			player.sendPacket(ExOpenMPCC.STATIC_PACKET);
		}
		
		if (_positionBroadcastTask == null)
		{
			_positionBroadcastTask = ThreadPool.scheduleAtFixedRate(() ->
			{
				if (_positionPacket == null)
				{
					_positionPacket = new PartyMemberPosition(this);
				}
				else
				{
					_positionPacket.reuse(this);
				}
				broadcastPacket(_positionPacket);
			}, PARTY_POSITION_BROADCAST_INTERVAL.toMillis() / 2, PARTY_POSITION_BROADCAST_INTERVAL.toMillis());
		}
	}
	
	/**
	 * Removes a party member using its name.
	 * @param name player the player to be removed from the party.
	 * @param type the message type {@link MessageType}.
	 */
	public void removePartyMember(String name, MessageType type)
	{
		removePartyMember(getPlayerByName(name), type);
	}
	
	/**
	 * Removes a party member instance.
	 * @param player the player to be removed from the party.
	 * @param type the message type {@link MessageType}.
	 */
	public void removePartyMember(PlayerInstance player, MessageType type)
	{
		if (_members.contains(player))
		{
			final boolean isLeader = isLeader(player);
			if (!_disbanding && ((_members.size() == 2) || (isLeader && !Config.ALT_LEAVE_PARTY_LEADER && (type != MessageType.DISCONNECTED))))
			{
				disbandParty();
				return;
			}
			
			_members.remove(player);
			recalculatePartyLevel();
			
			if (player.isFestivalParticipant())
			{
				SevenSignsFestival.getInstance().updateParticipants(player, this);
			}
			
			if (player.isInDuel())
			{
				DuelManager.getInstance().onRemoveFromParty(player);
			}
			
			try
			{
				// Channeling a player!
				if (player.isChanneling() && (player.getSkillChannelizer().hasChannelized()))
				{
					player.abortCast();
				}
				else if (player.isChannelized())
				{
					player.getSkillChannelized().abortChannelization();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "", e);
			}
			
			SystemMessage msg;
			if (type == MessageType.EXPELLED)
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_EXPELLED_FROM_THE_PARTY);
				msg = new SystemMessage(SystemMessageId.C1_WAS_EXPELLED_FROM_THE_PARTY);
				msg.addString(player.getName());
				broadcastPacket(msg);
			}
			else if ((type == MessageType.LEFT) || (type == MessageType.DISCONNECTED))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_THE_PARTY);
				msg = new SystemMessage(SystemMessageId.C1_HAS_LEFT_THE_PARTY);
				msg.addString(player.getName());
				broadcastPacket(msg);
			}
			
			// UI update.
			player.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
			player.setParty(null);
			broadcastPacket(new PartySmallWindowDelete(player));
			if (player.hasSummon())
			{
				broadcastPacket(new ExPartyPetWindowDelete(player.getSummon()));
			}
			
			if (isInDimensionalRift())
			{
				_dr.partyMemberExited(player);
			}
			
			// Close the CCInfoWindow
			if (isInCommandChannel())
			{
				player.sendPacket(new ExCloseMPCC());
			}
			if (isLeader && (_members.size() > 1) && (Config.ALT_LEAVE_PARTY_LEADER || (type == MessageType.DISCONNECTED)))
			{
				msg = new SystemMessage(SystemMessageId.C1_HAS_BECOME_THE_PARTY_LEADER);
				msg.addString(getLeader().getName());
				broadcastPacket(msg);
				broadcastToPartyMembersNewLeader();
			}
			else if (_members.size() == 1)
			{
				if (isInCommandChannel())
				{
					// delete the whole command channel when the party who opened the channel is disbanded
					if (_commandChannel.getLeader().getObjectId() == getLeader().getObjectId())
					{
						_commandChannel.disbandChannel();
					}
					else
					{
						_commandChannel.removeParty(this);
					}
				}
				
				final PlayerInstance leader = getLeader();
				if (leader != null)
				{
					leader.setParty(null);
					if (leader.isInDuel())
					{
						DuelManager.getInstance().onRemoveFromParty(leader);
					}
				}
				
				if (_changeDistributionTypeRequestTask != null)
				{
					_changeDistributionTypeRequestTask.cancel(true);
					_changeDistributionTypeRequestTask = null;
				}
				if (_positionBroadcastTask != null)
				{
					_positionBroadcastTask.cancel(false);
					_positionBroadcastTask = null;
				}
				_members.clear();
			}
		}
	}
	
	/**
	 * Disperse a party and send a message to all its members.
	 */
	public void disbandParty()
	{
		_disbanding = true;
		broadcastPacket(new SystemMessage(SystemMessageId.THE_PARTY_HAS_DISPERSED));
		for (PlayerInstance member : _members)
		{
			if (member != null)
			{
				removePartyMember(member, MessageType.NONE);
			}
		}
	}
	
	/**
	 * Change party leader (used for string arguments)
	 * @param name the name of the player to set as the new party leader
	 */
	public void changePartyLeader(String name)
	{
		setLeader(getPlayerByName(name));
	}
	
	@Override
	public void setLeader(PlayerInstance player)
	{
		if ((player != null) && !player.isInDuel())
		{
			if (_members.contains(player))
			{
				if (isLeader(player))
				{
					player.sendPacket(SystemMessageId.SLOW_DOWN_YOU_ARE_ALREADY_THE_PARTY_LEADER);
				}
				else
				{
					// Swap party members
					final PlayerInstance temp = getLeader();
					final int p1 = _members.indexOf(player);
					_members.set(0, player);
					_members.set(p1, temp);
					SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_BECOME_THE_PARTY_LEADER);
					msg.addString(getLeader().getName());
					broadcastPacket(msg);
					broadcastToPartyMembersNewLeader();
					if (isInCommandChannel() && _commandChannel.isLeader(temp))
					{
						_commandChannel.setLeader(getLeader());
						msg = new SystemMessage(SystemMessageId.COMMAND_CHANNEL_AUTHORITY_HAS_BEEN_TRANSFERRED_TO_C1);
						msg.addString(_commandChannel.getLeader().getName());
						_commandChannel.broadcastPacket(msg);
					}
					if (player.isInPartyMatchRoom())
					{
						PartyMatchRoomList.getInstance().getPlayerRoom(player).changeLeader(player);
					}
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_MAY_ONLY_TRANSFER_PARTY_LEADERSHIP_TO_ANOTHER_MEMBER_OF_THE_PARTY);
			}
		}
	}
	
	/**
	 * finds a player in the party by name
	 * @param name
	 * @return
	 */
	private PlayerInstance getPlayerByName(String name)
	{
		for (PlayerInstance member : _members)
		{
			if (member.getName().equalsIgnoreCase(name))
			{
				return member;
			}
		}
		return null;
	}
	
	/**
	 * distribute item(s) to party members
	 * @param player
	 * @param item
	 */
	public void distributeItem(PlayerInstance player, ItemInstance item)
	{
		if (item.getId() == Inventory.ADENA_ID)
		{
			distributeAdena(player, item.getCount(), player);
			ItemTable.getInstance().destroyItem("Party", item, player, null);
			return;
		}
		
		final PlayerInstance target = getActualLooter(player, item.getId(), false, player);
		target.addItem("Party", item, player, true);
		if (item.getCount() <= 0)
		{
			return;
		}
		
		final SystemMessage msg;
		if (item.getCount() > 1)
		{
			msg = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S3_S2);
			msg.addString(target.getName());
			msg.addItemName(item);
			msg.addLong(item.getCount());
		}
		else
		{
			msg = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2);
			msg.addString(target.getName());
			msg.addItemName(item);
		}
		broadcastToPartyMembers(target, msg);
	}
	
	/**
	 * Distributes item loot between party members.
	 * @param player the reference player
	 * @param itemId the item ID
	 * @param itemCount the item count
	 * @param spoil {@code true} if it's spoil loot
	 * @param target the NPC target
	 */
	public void distributeItem(PlayerInstance player, int itemId, long itemCount, boolean spoil, Attackable target)
	{
		if (itemId == Inventory.ADENA_ID)
		{
			distributeAdena(player, itemCount, target);
			return;
		}
		
		final PlayerInstance looter = getActualLooter(player, itemId, spoil, target);
		looter.addItem(spoil ? "Sweeper Party" : "Party", itemId, itemCount, target, true);
		if (itemCount <= 0)
		{
			return;
		}
		
		final SystemMessage msg;
		if (itemCount > 1)
		{
			msg = spoil ? new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S3_S2_BY_USING_SWEEPER) : new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S3_S2);
			msg.addString(looter.getName());
			msg.addItemName(itemId);
			msg.addLong(itemCount);
		}
		else
		{
			msg = spoil ? new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2_BY_USING_SWEEPER) : new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2);
			msg.addString(looter.getName());
			msg.addItemName(itemId);
		}
		broadcastToPartyMembers(looter, msg);
	}
	
	/**
	 * Method overload for {@link Party#distributeItem(PlayerInstance, int, long, boolean, Attackable)}
	 * @param player the reference player
	 * @param item the item holder
	 * @param spoil {@code true} if it's spoil loot
	 * @param target the NPC target
	 */
	public void distributeItem(PlayerInstance player, ItemHolder item, boolean spoil, Attackable target)
	{
		distributeItem(player, item.getId(), item.getCount(), spoil, target);
	}
	
	/**
	 * distribute adena to party members
	 * @param player
	 * @param adena
	 * @param target
	 */
	public void distributeAdena(PlayerInstance player, long adena, Creature target)
	{
		// Check the number of party members that must be rewarded
		// (The party member must be in range to receive its reward)
		final List<PlayerInstance> toReward = new LinkedList<>();
		for (PlayerInstance member : _members)
		{
			if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, target, member, true))
			{
				toReward.add(member);
			}
		}
		
		if (!toReward.isEmpty())
		{
			// Now we can actually distribute the adena reward
			// (Total adena splitted by the number of party members that are in range and must be rewarded)
			final long count = adena / toReward.size();
			for (PlayerInstance member : toReward)
			{
				member.addAdena("Party", count, player, true);
			}
		}
	}
	
	/**
	 * Distribute Experience and SP rewards to PlayerInstance Party members in the known area of the last attacker.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <li>Get the PlayerInstance owner of the ServitorInstance (if necessary)</li>
	 * <li>Calculate the Experience and SP reward distribution rate</li>
	 * <li>Add Experience and SP to the PlayerInstance</li><br>
	 * @param xpRewardValue The Experience reward to distribute
	 * @param spRewardValue The SP reward to distribute
	 * @param rewardedMembers The list of PlayerInstance to reward
	 * @param topLvl
	 * @param partyDmg
	 * @param target
	 */
	public void distributeXpAndSp(double xpRewardValue, double spRewardValue, List<PlayerInstance> rewardedMembers, int topLvl, long partyDmg, Attackable target)
	{
		final List<PlayerInstance> validMembers = getValidMembers(rewardedMembers, topLvl);
		double xpReward = xpRewardValue * getExpBonus(validMembers.size());
		double spReward = spRewardValue * getSpBonus(validMembers.size());
		int sqLevelSum = 0;
		for (PlayerInstance member : validMembers)
		{
			sqLevelSum += member.getLevel() * member.getLevel();
		}
		
		final float vitalityPoints = (target.getVitalityPoints(partyDmg) * Config.RATE_PARTY_XP) / validMembers.size();
		final boolean useVitalityRate = target.useVitalityRate();
		for (PlayerInstance member : rewardedMembers)
		{
			if (member.isDead())
			{
				continue;
			}
			
			// Calculate and add the EXP and SP reward to the member
			if (validMembers.contains(member))
			{
				// The servitor penalty
				final float penalty = member.hasServitor() ? ((ServitorInstance) member.getSummon()).getExpMultiplier() : 1;
				final double sqLevel = member.getLevel() * member.getLevel();
				final double preCalculation = (sqLevel / sqLevelSum) * penalty;
				
				// Add the XP/SP points to the requested party member
				double addexp = Math.round(member.calcStat(Stat.EXPSP_RATE, xpReward * preCalculation, null, null));
				final double addsp = member.calcStat(Stat.EXPSP_RATE, spReward * preCalculation, null, null);
				addexp = calculateExpSpPartyCutoff(member.getActingPlayer(), topLvl, addexp, addsp, useVitalityRate);
				if (addexp > 0)
				{
					member.updateVitalityPoints(vitalityPoints, true, false);
				}
			}
			else
			{
				member.addExpAndSp(0, 0);
			}
		}
	}
	
	private double calculateExpSpPartyCutoff(PlayerInstance player, int topLvl, double addExp, double addSp, boolean vit)
	{
		double xp = addExp;
		double sp = addSp;
		if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("highfive"))
		{
			int i = 0;
			final int levelDiff = topLvl - player.getLevel();
			for (int[] gap : Config.PARTY_XP_CUTOFF_GAPS)
			{
				if ((levelDiff >= gap[0]) && (levelDiff <= gap[1]))
				{
					xp = (addExp * Config.PARTY_XP_CUTOFF_GAP_PERCENTS[i]) / 100;
					sp = (addSp * Config.PARTY_XP_CUTOFF_GAP_PERCENTS[i]) / 100;
					player.addExpAndSp(xp, sp, vit);
					break;
				}
				i++;
			}
		}
		else
		{
			player.addExpAndSp(addExp, addSp, vit);
		}
		return xp;
	}
	
	/**
	 * refresh party level
	 */
	public void recalculatePartyLevel()
	{
		int newLevel = 0;
		for (PlayerInstance member : _members)
		{
			if (member == null)
			{
				_members.remove(member);
				continue;
			}
			
			if (member.getLevel() > newLevel)
			{
				newLevel = member.getLevel();
			}
		}
		_partyLvl = newLevel;
	}
	
	private List<PlayerInstance> getValidMembers(List<PlayerInstance> members, int topLvl)
	{
		final List<PlayerInstance> validMembers = new ArrayList<>();
		
		// Fixed LevelDiff cutoff point
		if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("level"))
		{
			for (PlayerInstance member : members)
			{
				if ((topLvl - member.getLevel()) <= Config.PARTY_XP_CUTOFF_LEVEL)
				{
					validMembers.add(member);
				}
			}
		}
		// Fixed MinPercentage cutoff point
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("percentage"))
		{
			int sqLevelSum = 0;
			for (PlayerInstance member : members)
			{
				sqLevelSum += member.getLevel() * member.getLevel();
			}
			
			for (PlayerInstance member : members)
			{
				if ((member.getLevel() * member.getLevel() * 100) >= (sqLevelSum * Config.PARTY_XP_CUTOFF_PERCENT))
				{
					validMembers.add(member);
				}
			}
		}
		// Automatic cutoff method
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("auto"))
		{
			int sqLevelSum = 0;
			for (PlayerInstance member : members)
			{
				sqLevelSum += member.getLevel() * member.getLevel();
			}
			
			int i = members.size() - 1;
			if (i < 1)
			{
				return members;
			}
			if (i >= BONUS_EXP_SP.length)
			{
				i = BONUS_EXP_SP.length - 1;
			}
			
			for (PlayerInstance member : members)
			{
				if ((member.getLevel() * member.getLevel()) >= (sqLevelSum / (members.size() * members.size())))
				{
					validMembers.add(member);
				}
			}
		}
		// High Five cutoff method
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("highfive"))
		{
			validMembers.addAll(members);
		}
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("none"))
		{
			validMembers.addAll(members);
		}
		return validMembers;
	}
	
	private double getBaseExpSpBonus(int membersCount)
	{
		int i = membersCount - 1;
		if (i < 1)
		{
			return 1;
		}
		if (i >= BONUS_EXP_SP.length)
		{
			i = BONUS_EXP_SP.length - 1;
		}
		return BONUS_EXP_SP[i];
	}
	
	private double getExpBonus(int membersCount)
	{
		return (membersCount < 2) ? getBaseExpSpBonus(membersCount) : (getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_XP);
	}
	
	private double getSpBonus(int membersCount)
	{
		return (membersCount < 2) ? getBaseExpSpBonus(membersCount) : (getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_SP);
	}
	
	@Override
	public int getLevel()
	{
		return _partyLvl;
	}
	
	public PartyDistributionType getDistributionType()
	{
		return _distributionType;
	}
	
	public boolean isInCommandChannel()
	{
		return _commandChannel != null;
	}
	
	public CommandChannel getCommandChannel()
	{
		return _commandChannel;
	}
	
	public void setCommandChannel(CommandChannel channel)
	{
		_commandChannel = channel;
	}
	
	public boolean isInDimensionalRift()
	{
		return _dr != null;
	}
	
	public void setDimensionalRift(DimensionalRift dr)
	{
		_dr = dr;
	}
	
	public DimensionalRift getDimensionalRift()
	{
		return _dr;
	}
	
	/**
	 * @return the leader of this party
	 */
	@Override
	public PlayerInstance getLeader()
	{
		if (_members.isEmpty())
		{
			return null;
		}
		return _members.get(0);
	}
	
	public synchronized void requestLootChange(PartyDistributionType partyDistributionType)
	{
		if (_changeRequestDistributionType != null)
		{
			return;
		}
		_changeRequestDistributionType = partyDistributionType;
		_changeDistributionTypeAnswers = new HashSet<>();
		_changeDistributionTypeRequestTask = ThreadPool.schedule(() -> finishLootRequest(false), PARTY_DISTRIBUTION_TYPE_REQUEST_TIMEOUT.toMillis());
		broadcastToPartyMembers(getLeader(), new ExAskModifyPartyLooting(getLeader().getName(), partyDistributionType));
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.REQUESTING_APPROVAL_FOR_CHANGING_PARTY_LOOT_TO_S1);
		sm.addSystemString(partyDistributionType.getSysStringId());
		getLeader().sendPacket(sm);
	}
	
	public synchronized void answerLootChangeRequest(PlayerInstance member, boolean answer)
	{
		if ((_changeRequestDistributionType == null) || _changeDistributionTypeAnswers.contains(member.getObjectId()))
		{
			return;
		}
		
		if (!answer)
		{
			finishLootRequest(false);
			return;
		}
		
		_changeDistributionTypeAnswers.add(member.getObjectId());
		if (_changeDistributionTypeAnswers.size() >= (getMemberCount() - 1))
		{
			finishLootRequest(true);
		}
	}
	
	protected synchronized void finishLootRequest(boolean success)
	{
		if (_changeRequestDistributionType == null)
		{
			return;
		}
		if (_changeDistributionTypeRequestTask != null)
		{
			_changeDistributionTypeRequestTask.cancel(false);
			_changeDistributionTypeRequestTask = null;
		}
		if (success)
		{
			broadcastPacket(new ExSetPartyLooting(1, _changeRequestDistributionType));
			_distributionType = _changeRequestDistributionType;
			final SystemMessage sm = new SystemMessage(SystemMessageId.PARTY_LOOT_WAS_CHANGED_TO_S1);
			sm.addSystemString(_changeRequestDistributionType.getSysStringId());
			broadcastPacket(sm);
		}
		else
		{
			broadcastPacket(new ExSetPartyLooting(0, _distributionType));
			broadcastPacket(new SystemMessage(SystemMessageId.PARTY_LOOT_CHANGE_WAS_CANCELLED));
		}
		_changeRequestDistributionType = null;
		_changeDistributionTypeAnswers = null;
	}
	
	/**
	 * @return a list of all members of this party
	 */
	@Override
	public List<PlayerInstance> getMembers()
	{
		return _members;
	}
	
	/**
	 * Check whether the leader of this party is the same as the leader of the specified party (which essentially means they're the same group).
	 * @param party the other party to check against
	 * @return {@code true} if this party equals the specified party, {@code false} otherwise
	 */
	public boolean equals(Party party)
	{
		return (party != null) && (getLeaderObjectId() == party.getLeaderObjectId());
	}
}
