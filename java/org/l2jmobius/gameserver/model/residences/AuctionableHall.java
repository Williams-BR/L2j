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
package org.l2jmobius.gameserver.model.residences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.instancemanager.ClanHallAuctionManager;
import org.l2jmobius.gameserver.instancemanager.ClanHallManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class AuctionableHall extends ClanHall
{
	private static final int CH_RATE = 604800000;
	
	protected long _paidUntil;
	private final int _grade;
	protected boolean _paid;
	final int _lease;
	
	public AuctionableHall(StatSet set)
	{
		super(set);
		_paidUntil = set.getLong("paidUntil");
		_grade = set.getInt("grade");
		_paid = set.getBoolean("paid");
		_lease = set.getInt("lease");
		if (getOwnerId() != 0)
		{
			_isFree = false;
			initialyzeTask(false);
			loadFunctions();
		}
	}
	
	/**
	 * @return if clanHall is paid or not
	 */
	public boolean getPaid()
	{
		return _paid;
	}
	
	/** Return lease */
	@Override
	public int getLease()
	{
		return _lease;
	}
	
	/** Return PaidUntil */
	@Override
	public long getPaidUntil()
	{
		return _paidUntil;
	}
	
	/** Return Grade */
	@Override
	public int getGrade()
	{
		return _grade;
	}
	
	@Override
	public void free()
	{
		super.free();
		_paidUntil = 0;
		_paid = false;
	}
	
	@Override
	public void setOwner(Clan clan)
	{
		super.setOwner(clan);
		_paidUntil = Chronos.currentTimeMillis();
		initialyzeTask(true);
	}
	
	/**
	 * Initialize Fee Task
	 * @param forced
	 */
	private final void initialyzeTask(boolean forced)
	{
		final long currentTime = Chronos.currentTimeMillis();
		if (_paidUntil > currentTime)
		{
			ThreadPool.schedule(new FeeTask(), _paidUntil - currentTime);
		}
		else if (!_paid && !forced)
		{
			if ((Chronos.currentTimeMillis() + (3600000 * 24)) <= (_paidUntil + CH_RATE))
			{
				ThreadPool.schedule(new FeeTask(), Chronos.currentTimeMillis() + (3600000 * 24));
			}
			else
			{
				ThreadPool.schedule(new FeeTask(), (_paidUntil + CH_RATE) - Chronos.currentTimeMillis());
			}
		}
		else
		{
			ThreadPool.schedule(new FeeTask(), 0);
		}
	}
	
	/** Fee Task */
	protected class FeeTask implements Runnable
	{
		private final Logger LOGGER = Logger.getLogger(FeeTask.class.getName());
		
		@Override
		public void run()
		{
			try
			{
				final long _time = Chronos.currentTimeMillis();
				if (isFree())
				{
					return;
				}
				
				if (_paidUntil > _time)
				{
					ThreadPool.schedule(new FeeTask(), _paidUntil - _time);
					return;
				}
				
				final Clan clan = ClanTable.getInstance().getClan(getOwnerId());
				if (ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= getLease())
				{
					if (_paidUntil != 0)
					{
						while (_paidUntil <= _time)
						{
							_paidUntil += CH_RATE;
						}
					}
					else
					{
						_paidUntil = _time + CH_RATE;
					}
					ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CH_rental_fee", Inventory.ADENA_ID, getLease(), null, null);
					ThreadPool.schedule(new FeeTask(), _paidUntil - _time);
					_paid = true;
					updateDb();
				}
				else
				{
					_paid = false;
					if (_time > (_paidUntil + CH_RATE))
					{
						if (ClanHallManager.getInstance().loaded())
						{
							ClanHallAuctionManager.getInstance().initNPC(getId());
							ClanHallManager.getInstance().setFree(getId());
							clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
						}
						else
						{
							ThreadPool.schedule(new FeeTask(), 3000);
						}
					}
					else
					{
						updateDb();
						final SystemMessage sm = new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
						sm.addInt(_lease);
						clan.broadcastToOnlineMembers(sm);
						if ((_time + (3600000 * 24)) <= (_paidUntil + CH_RATE))
						{
							ThreadPool.schedule(new FeeTask(), _time + (3600000 * 24));
						}
						else
						{
							ThreadPool.schedule(new FeeTask(), (_paidUntil + CH_RATE) - _time);
						}
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	@Override
	public void updateDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clanhall SET ownerId=?, paidUntil=?, paid=? WHERE id=?"))
		{
			ps.setInt(1, getOwnerId());
			ps.setLong(2, _paidUntil);
			ps.setInt(3, _paid ? 1 : 0);
			ps.setInt(4, getId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + e.getMessage(), e);
		}
	}
}
