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

import org.l2jmobius.gameserver.data.MerchantPriceConfigTable;
import org.l2jmobius.gameserver.data.MerchantPriceConfigTable.MerchantPriceConfig;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.buylist.BuyListHolder;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.BuyList;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;

/**
 * @version $Revision: 1.10.4.9 $ $Date: 2005/04/11 10:06:08 $
 */
public class MerchantInstance extends NpcInstance
{
	private MerchantPriceConfig _mpc;
	
	/**
	 * Creates a merchant,
	 * @param template the merchant NPC template
	 */
	public MerchantInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.MerchantInstance);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_mpc = MerchantPriceConfigTable.getInstance().getMerchantPriceConfig(this);
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
		return "data/html/merchant/" + pom + ".htm";
	}
	
	/**
	 * @return Returns the mpc.
	 */
	public MerchantPriceConfig getMpc()
	{
		return _mpc;
	}
	
	public void showBuyWindow(PlayerInstance player, int value)
	{
		showBuyWindow(player, value, true);
	}
	
	public void showBuyWindow(PlayerInstance player, int value, boolean applyTax)
	{
		final BuyListHolder buyList = BuyListData.getInstance().getBuyList(value);
		if (buyList == null)
		{
			LOGGER.warning("BuyList not found! BuyListId:" + value);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!buyList.isNpcAllowed(getId()))
		{
			LOGGER.warning("Npc not allowed in BuyList! BuyListId:" + value + " NpcId:" + getId());
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final double taxRate = (applyTax) ? _mpc.getTotalTaxRate() : 0;
		player.setInventoryBlockingStatus(true);
		
		player.sendPacket(new BuyList(buyList, player.getAdena(), taxRate));
		player.sendPacket(new ExBuySellList(player, false));
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
