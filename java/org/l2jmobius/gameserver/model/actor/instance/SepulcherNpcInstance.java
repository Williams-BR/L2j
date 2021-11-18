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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.instancemanager.FourSepulchersManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.Util;

/**
 * @author sandman
 */
public class SepulcherNpcInstance extends Npc
{
	protected Future<?> _closeTask = null;
	protected Future<?> _spawnNextMysteriousBoxTask = null;
	protected Future<?> _spawnMonsterTask = null;
	
	private static final String HTML_FILE_PATH = "data/html/SepulcherNpc/";
	private static final int HALLS_KEY = 7260;
	
	/**
	 * Creates a sepulcher.
	 * @param template the sepulcher NPC template
	 */
	public SepulcherNpcInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.SepulcherNpcInstance);
		setShowSummonAnimation(true);
		
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
		}
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
		}
		if (_spawnMonsterTask != null)
		{
			_spawnMonsterTask.cancel(true);
		}
		_closeTask = null;
		_spawnNextMysteriousBoxTask = null;
		_spawnMonsterTask = null;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		setShowSummonAnimation(false);
	}
	
	@Override
	public boolean deleteMe()
	{
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
			_closeTask = null;
		}
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
			_spawnNextMysteriousBoxTask = null;
		}
		if (_spawnMonsterTask != null)
		{
			_spawnMonsterTask.cancel(true);
			_spawnMonsterTask = null;
		}
		return super.deleteMe();
	}
	
	@Override
	public void onAction(PlayerInstance player, boolean interact)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		// Check if the PlayerInstance already target the NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the PlayerInstance player
			player.setTarget(this);
		}
		else if (interact)
		{
			// Check if the player is attackable (without a forced attack) and
			// isn't dead
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				// Check the height difference
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth
				// difference might
				// need some tweaking
				{
					// Set the PlayerInstance Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
				else
				{
					// Send a Server->Client packet ActionFailed (target is out
					// of attack range) to the PlayerInstance player
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			
			if (!isAutoAttackable(player))
			{
				// Calculate the distance between the PlayerInstance and the
				// NpcInstance
				if (!canInteract(player))
				{
					// Notify the PlayerInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Send a Server->Client packet SocialAction to the all
					// PlayerInstance on the _knownPlayer of the NpcInstance
					// to display a social action of the NpcInstance on their
					// client
					broadcastPacket(new SocialAction(getObjectId(), Rnd.get(8)));
					doAction(player);
				}
			}
			// Send a Server->Client ActionFailed to the PlayerInstance in order
			// to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	private void doAction(PlayerInstance player)
	{
		if (isDead())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		switch (getId())
		{
			case 31468:
			case 31469:
			case 31470:
			case 31471:
			case 31472:
			case 31473:
			case 31474:
			case 31475:
			case 31476:
			case 31477:
			case 31478:
			case 31479:
			case 31480:
			case 31481:
			case 31482:
			case 31483:
			case 31484:
			case 31485:
			case 31486:
			case 31487:
			{
				setInvul(false);
				reduceCurrentHp(getMaxHp() + 1, player, null);
				if (_spawnMonsterTask != null)
				{
					_spawnMonsterTask.cancel(true);
				}
				_spawnMonsterTask = ThreadPool.schedule(new SpawnMonster(getId()), 3500);
				break;
			}
			case 31455:
			case 31456:
			case 31457:
			case 31458:
			case 31459:
			case 31460:
			case 31461:
			case 31462:
			case 31463:
			case 31464:
			case 31465:
			case 31466:
			case 31467:
			{
				setInvul(false);
				reduceCurrentHp(getMaxHp() + 1, player, null);
				PlayerInstance leader = player;
				if ((player.getParty() != null) && !player.getParty().isLeader(player))
				{
					leader = player.getParty().getLeader();
				}
				leader.addItem("Quest", HALLS_KEY, 1, leader, true);
				break;
			}
			default:
			{
				if (hasListener(EventType.ON_NPC_QUEST_START))
				{
					player.setLastQuestNpcObject(getObjectId());
				}
				
				if (hasListener(EventType.ON_NPC_FIRST_TALK))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnNpcFirstTalk(this, player), this);
				}
				else
				{
					showChatWindow(player, 0);
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String getHtmlPath(int npcId, int value)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		return HTML_FILE_PATH + pom + ".htm";
	}
	
	@Override
	public void showChatWindow(PlayerInstance player, int value)
	{
		final String filename = getHtmlPath(getId(), value);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		if (isBusy())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, "data/html/npcbusy.htm");
			html.replace("%busymessage%", getBusyMessage());
			html.replace("%npcname%", getName());
			html.replace("%playername%", player.getName());
			player.sendPacket(html);
		}
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (Exception e)
			{
				// Handled above.
			}
			showChatWindow(player, val);
		}
		else if (command.startsWith("open_gate"))
		{
			final ItemInstance hallsKey = player.getInventory().getItemByItemId(HALLS_KEY);
			if (hallsKey == null)
			{
				showHtmlFile(player, "Gatekeeper-no.htm");
			}
			else if (FourSepulchersManager.getInstance().isAttackTime())
			{
				switch (getId())
				{
					case 31929:
					case 31934:
					case 31939:
					case 31944:
					{
						FourSepulchersManager.getInstance().spawnShadow(getId());
						// Fallthrou?
					}
					default:
					{
						openNextDoor(getId());
						if (player.getParty() != null)
						{
							for (PlayerInstance mem : player.getParty().getMembers())
							{
								if ((mem != null) && (mem.getInventory().getItemByItemId(HALLS_KEY) != null))
								{
									mem.destroyItemByItemId("Quest", HALLS_KEY, mem.getInventory().getItemByItemId(HALLS_KEY).getCount(), mem, true);
								}
							}
						}
						else
						{
							player.destroyItemByItemId("Quest", HALLS_KEY, hallsKey.getCount(), player, true);
						}
					}
				}
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public void openNextDoor(int npcId)
	{
		final int doorId = FourSepulchersManager.getInstance().getHallGateKeepers().get(npcId);
		DoorData.getInstance().getDoor(doorId).openMe();
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
		}
		_closeTask = ThreadPool.schedule(new CloseNextDoor(doorId), 10000);
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
		}
		_spawnNextMysteriousBoxTask = ThreadPool.schedule(new SpawnNextMysteriousBox(npcId), 0);
	}
	
	private static class CloseNextDoor implements Runnable
	{
		final DoorData _DoorTable = DoorData.getInstance();
		
		private final int _DoorId;
		
		public CloseNextDoor(int doorId)
		{
			_DoorId = doorId;
		}
		
		@Override
		public void run()
		{
			try
			{
				_DoorTable.getDoor(_DoorId).closeMe();
			}
			catch (Exception e)
			{
				LOGGER.warning(e.getMessage());
			}
		}
	}
	
	private static class SpawnNextMysteriousBox implements Runnable
	{
		private final int _NpcId;
		
		public SpawnNextMysteriousBox(int npcId)
		{
			_NpcId = npcId;
		}
		
		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnMysteriousBox(_NpcId);
		}
	}
	
	private static class SpawnMonster implements Runnable
	{
		private final int _NpcId;
		
		public SpawnMonster(int npcId)
		{
			_NpcId = npcId;
		}
		
		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnMonster(_NpcId);
		}
	}
	
	public void sayInShout(NpcStringId msg)
	{
		if (msg == null)
		{
			return; // wrong usage
		}
		
		final CreatureSay creatureSay = new CreatureSay(this, ChatType.NPC_SHOUT, msg);
		for (PlayerInstance player : World.getInstance().getPlayers())
		{
			if (Util.checkIfInRange(15000, player, this, true))
			{
				player.sendPacket(creatureSay);
			}
		}
	}
	
	public void showHtmlFile(PlayerInstance player, String file)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, "data/html/SepulcherNpc/" + file);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
