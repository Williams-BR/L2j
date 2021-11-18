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
package handlers.effecthandlers;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.ItemTable;
import org.l2jmobius.gameserver.model.ExtractableProductItem;
import org.l2jmobius.gameserver.model.ExtractableSkill;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Restoration Random effect implementation.<br>
 * This effect is present in item skills that "extract" new items upon usage.<br>
 * This effect has been unhardcoded in order to work on targets as well.
 * @author Zoey76, Mobius
 */
public class RestorationRandom extends AbstractEffect
{
	public RestorationRandom(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if ((info.getEffector() == null) || (info.getEffected() == null) || !info.getEffector().isPlayer() || !info.getEffected().isPlayer())
		{
			return;
		}
		
		final ExtractableSkill exSkill = info.getSkill().getExtractableSkill();
		if (exSkill == null)
		{
			return;
		}
		
		if (exSkill.getProductItems().isEmpty())
		{
			LOGGER.warning("Extractable Skill with no data, probably wrong/empty table in Skill Id: " + info.getSkill().getId());
			return;
		}
		
		final double rndNum = 100 * Rnd.nextDouble();
		double chance = 0;
		double chanceFrom = 0;
		final List<ItemHolder> creationList = new ArrayList<>();
		
		// Explanation for future changes:
		// You get one chance for the current skill, then you can fall into
		// one of the "areas" like in a roulette.
		// Example: for an item like Id1,A1,30;Id2,A2,50;Id3,A3,20;
		// #---#-----#--#
		// 0--30----80-100
		// If you get chance equal 45% you fall into the second zone 30-80.
		// Meaning you get the second production list.
		// Calculate extraction
		for (ExtractableProductItem expi : exSkill.getProductItems())
		{
			chance = expi.getChance();
			if ((rndNum >= chanceFrom) && (rndNum <= (chance + chanceFrom)))
			{
				creationList.addAll(expi.getItems());
				break;
			}
			chanceFrom += chance;
		}
		
		final PlayerInstance player = info.getEffected().getActingPlayer();
		if (creationList.isEmpty())
		{
			player.sendPacket(SystemMessageId.THERE_WAS_NOTHING_FOUND_INSIDE);
			return;
		}
		
		for (ItemHolder item : creationList)
		{
			if ((item.getId() <= 0) || (item.getCount() <= 0))
			{
				continue;
			}
			
			final long itemCount = (long) (item.getCount() * Config.RATE_EXTRACTABLE);
			final Item template = ItemTable.getInstance().getTemplate(item.getId());
			if (template.isStackable())
			{
				player.addItem("Extract", item.getId(), itemCount, info.getEffector(), true);
			}
			else
			{
				for (int i = 0; i < itemCount; i++)
				{
					player.addItem("Extract", item.getId(), 1, info.getEffector(), true);
				}
			}
		}
	}
}
