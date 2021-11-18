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

import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.ai.FortSiegeGuardAI;
import org.l2jmobius.gameserver.ai.SiegeGuardAI;
import org.l2jmobius.gameserver.ai.SpecialSiegeGuardAI;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.instancemanager.TerritoryWarManager;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class DefenderInstance extends Attackable
{
	private Castle _castle = null; // the castle which the instance should defend
	private Fort _fort = null; // the fortress which the instance should defend
	private SiegableHall _hall = null; // the siegable hall which the instance should defend
	
	/**
	 * Creates a defender.
	 * @param template the defender NPC template
	 */
	public DefenderInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.DefenderInstance);
	}
	
	@Override
	protected CreatureAI initAI()
	{
		if ((getConquerableHall() == null) && (getCastle(10000) == null))
		{
			return new FortSiegeGuardAI(this);
		}
		else if (getCastle(10000) != null)
		{
			return new SiegeGuardAI(this);
		}
		return new SpecialSiegeGuardAI(this);
	}
	
	/**
	 * Return True if a siege is in progress and the Creature attacker isn't a Defender.
	 * @param attacker The Creature that the SiegeGuardInstance try to attack
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// Attackable during siege by all except defenders
		if (!attacker.isPlayable())
		{
			return false;
		}
		
		final PlayerInstance player = attacker.getActingPlayer();
		
		// Check if siege is in progress
		if (((_fort != null) && _fort.getZone().isActive()) || ((_castle != null) && _castle.getZone().isActive()) || ((_hall != null) && _hall.getSiegeZone().isActive()))
		{
			final int activeSiegeId = (_fort != null ? _fort.getResidenceId() : (_castle != null ? _castle.getResidenceId() : (_hall != null ? _hall.getId() : 0)));
			
			// Check if player is an enemy of this defender npc
			if ((player != null) && (((player.getSiegeState() == 2) && !player.isRegisteredOnThisSiegeField(activeSiegeId)) || ((player.getSiegeState() == 1) && !TerritoryWarManager.getInstance().isAllyField(player, activeSiegeId)) || (player.getSiegeState() == 0)))
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	/**
	 * This method forces guard to return to home location previously set
	 */
	@Override
	public void returnHome()
	{
		if (getWalkSpeed() <= 0)
		{
			return;
		}
		if (getSpawn() == null)
		{
			return;
		}
		if (!isInsideRadius2D(getSpawn(), 40))
		{
			setReturningToSpawnPoint(true);
			clearAggroList();
			
			if (hasAI())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, getSpawn().getLocation());
			}
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		_fort = FortManager.getInstance().getFort(getX(), getY(), getZ());
		_castle = CastleManager.getInstance().getCastle(getX(), getY(), getZ());
		_hall = getConquerableHall();
		if ((_fort == null) && (_castle == null) && (_hall == null))
		{
			LOGGER.warning("DefenderInstance spawned outside of Fortress, Castle or Siegable hall Zone! NpcId: " + getId() + " x=" + getX() + " y=" + getY() + " z=" + getZ());
		}
	}
	
	/**
	 * Custom onAction behaviour. Note that super() is not called because guards need extra check to see if a player should interact or ATTACK them when clicked.
	 */
	@Override
	public void onAction(PlayerInstance player, boolean interact)
	{
		if (!canTarget(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
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
			// this max heigth difference might need some tweaking
			if (isAutoAttackable(player) && !isAlikeDead() && (Math.abs(player.getZ() - getZ()) < 600))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			// Notify the PlayerInstance AI with AI_INTENTION_INTERACT
			if (!isAutoAttackable(player) && !canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
		}
		// Send a Server->Client ActionFailed to the PlayerInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!(attacker instanceof DefenderInstance))
		{
			if ((damage == 0) && (aggro <= 1) && attacker.isPlayable())
			{
				final PlayerInstance player = attacker.getActingPlayer();
				// Check if siege is in progress
				if (((_fort != null) && _fort.getZone().isActive()) || ((_castle != null) && _castle.getZone().isActive()) || ((_hall != null) && _hall.getSiegeZone().isActive()))
				{
					final int activeSiegeId = (_fort != null ? _fort.getResidenceId() : (_castle != null ? _castle.getResidenceId() : (_hall != null ? _hall.getId() : 0)));
					
					// Do not add hate on defenders.
					if ((player.getSiegeState() == 2) && player.isRegisteredOnThisSiegeField(activeSiegeId))
					{
						return;
					}
				}
			}
			super.addDamageHate(attacker, damage, aggro);
		}
	}
}