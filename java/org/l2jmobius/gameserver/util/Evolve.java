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
package org.l2jmobius.gameserver.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.model.PetData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * UnAfraid: TODO: MOVE IT TO DP AI
 */
public class Evolve
{
	protected static final Logger LOGGER = Logger.getLogger(Evolve.class.getName());
	
	public static boolean doEvolve(PlayerInstance player, Npc npc, int itemIdtake, int itemIdgive, int petminLevel)
	{
		if ((itemIdtake == 0) || (itemIdgive == 0) || (petminLevel == 0))
		{
			return false;
		}
		
		if (!player.hasPet())
		{
			return false;
		}
		
		final PetInstance currentPet = (PetInstance) player.getSummon();
		if (currentPet.isAlikeDead())
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to use death pet exploit!", Config.DEFAULT_PUNISH);
			return false;
		}
		
		ItemInstance item = null;
		long petexp = currentPet.getStat().getExp();
		final String oldname = currentPet.getName();
		final int oldX = currentPet.getX();
		final int oldY = currentPet.getY();
		final int oldZ = currentPet.getZ();
		final PetData oldData = PetDataTable.getInstance().getPetDataByItemId(itemIdtake);
		if (oldData == null)
		{
			return false;
		}
		
		final int oldnpcID = oldData.getNpcId();
		if ((currentPet.getStat().getLevel() < petminLevel) || (currentPet.getId() != oldnpcID))
		{
			return false;
		}
		
		final PetData petData = PetDataTable.getInstance().getPetDataByItemId(itemIdgive);
		if (petData == null)
		{
			return false;
		}
		
		final int npcID = petData.getNpcId();
		if (npcID == 0)
		{
			return false;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcID);
		currentPet.unSummon(player);
		
		// deleting old pet item
		currentPet.destroyControlItem(player, true);
		item = player.getInventory().addItem("Evolve", itemIdgive, 1, player, npc);
		
		// Summoning new pet
		final PetInstance petSummon = PetInstance.spawnPet(npcTemplate, player, item);
		if (petSummon == null)
		{
			return false;
		}
		
		// Fix for non-linear baby pet exp
		final long _minimumexp = petSummon.getStat().getExpForLevel(petminLevel);
		if (petexp < _minimumexp)
		{
			petexp = _minimumexp;
		}
		
		petSummon.getStat().addExp(petexp);
		petSummon.setCurrentHp(petSummon.getMaxHp());
		petSummon.setCurrentMp(petSummon.getMaxMp());
		petSummon.setCurrentFed(petSummon.getMaxFed());
		petSummon.setTitle(player.getName());
		petSummon.setName(oldname);
		petSummon.setRunning();
		petSummon.storeMe();
		
		player.setPet(petSummon);
		
		player.sendPacket(new MagicSkillUse(npc, 2046, 1, 1000, 600000));
		player.sendPacket(SystemMessageId.SUMMONING_YOUR_PET);
		// World.getInstance().storeObject(petSummon);
		petSummon.spawnMe(oldX, oldY, oldZ);
		petSummon.startFeed();
		item.setEnchantLevel(petSummon.getLevel());
		
		ThreadPool.schedule(new EvolveFinalizer(player, petSummon), 900);
		if (petSummon.getCurrentFed() <= 0)
		{
			ThreadPool.schedule(new EvolveFeedWait(player, petSummon), 60000);
		}
		else
		{
			petSummon.startFeed();
		}
		
		return true;
	}
	
	public static boolean doRestore(PlayerInstance player, Npc npc, int itemIdtake, int itemIdgive, int petminLevel)
	{
		if ((itemIdtake == 0) || (itemIdgive == 0) || (petminLevel == 0))
		{
			return false;
		}
		
		final ItemInstance item = player.getInventory().getItemByItemId(itemIdtake);
		if (item == null)
		{
			return false;
		}
		
		final int oldpetlvl = item.getEnchantLevel() < petminLevel ? petminLevel : item.getEnchantLevel();
		final PetData oldData = PetDataTable.getInstance().getPetDataByItemId(itemIdtake);
		if (oldData == null)
		{
			return false;
		}
		
		final PetData petData = PetDataTable.getInstance().getPetDataByItemId(itemIdgive);
		if (petData == null)
		{
			return false;
		}
		
		final int npcId = petData.getNpcId();
		if (npcId == 0)
		{
			return false;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
		
		// deleting old pet item
		final ItemInstance removedItem = player.getInventory().destroyItem("PetRestore", item, player, npc);
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
		sm.addItemName(removedItem);
		player.sendPacket(sm);
		
		// Give new pet item
		final ItemInstance addedItem = player.getInventory().addItem("PetRestore", itemIdgive, 1, player, npc);
		
		// Summoning new pet
		final PetInstance petSummon = PetInstance.spawnPet(npcTemplate, player, addedItem);
		if (petSummon == null)
		{
			return false;
		}
		
		final long _maxexp = petSummon.getStat().getExpForLevel(oldpetlvl);
		petSummon.getStat().addExp(_maxexp);
		petSummon.setCurrentHp(petSummon.getMaxHp());
		petSummon.setCurrentMp(petSummon.getMaxMp());
		petSummon.setCurrentFed(petSummon.getMaxFed());
		petSummon.setTitle(player.getName());
		petSummon.setRunning();
		petSummon.storeMe();
		
		player.setPet(petSummon);
		
		player.sendPacket(new MagicSkillUse(npc, 2046, 1, 1000, 600000));
		player.sendPacket(SystemMessageId.SUMMONING_YOUR_PET);
		// World.getInstance().storeObject(petSummon);
		petSummon.spawnMe(player.getX(), player.getY(), player.getZ());
		petSummon.startFeed();
		addedItem.setEnchantLevel(petSummon.getLevel());
		
		// Inventory update
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(removedItem);
		player.sendPacket(iu);
		
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		player.broadcastUserInfo();
		
		final World world = World.getInstance();
		world.removeObject(removedItem);
		
		ThreadPool.schedule(new EvolveFinalizer(player, petSummon), 900);
		if (petSummon.getCurrentFed() <= 0)
		{
			ThreadPool.schedule(new EvolveFeedWait(player, petSummon), 60000);
		}
		else
		{
			petSummon.startFeed();
		}
		
		// pet control item no longer exists, delete the pet from the db
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
		{
			ps.setInt(1, removedItem.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			// Ignore.
		}
		return true;
	}
	
	static final class EvolveFeedWait implements Runnable
	{
		private final PlayerInstance _player;
		private final PetInstance _petSummon;
		
		EvolveFeedWait(PlayerInstance player, PetInstance petSummon)
		{
			_player = player;
			_petSummon = petSummon;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_petSummon.getCurrentFed() <= 0)
				{
					_petSummon.unSummon(_player);
				}
				else
				{
					_petSummon.startFeed();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "", e);
			}
		}
	}
	
	static final class EvolveFinalizer implements Runnable
	{
		private final PlayerInstance _player;
		private final PetInstance _petSummon;
		
		EvolveFinalizer(PlayerInstance player, PetInstance petSummon)
		{
			_player = player;
			_petSummon = petSummon;
		}
		
		@Override
		public void run()
		{
			try
			{
				_player.sendPacket(new MagicSkillLaunched(_player, 2046, 1));
				_petSummon.setFollowStatus(true);
				_petSummon.setShowSummonAnimation(false);
			}
			catch (Throwable e)
			{
				LOGGER.log(Level.WARNING, "", e);
			}
		}
	}
}