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
package org.l2jmobius.gameserver.model.actor;

import org.l2jmobius.gameserver.ai.CtrlEvent;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.stat.PlayableStat;
import org.l2jmobius.gameserver.model.actor.status.PlayableStatus;
import org.l2jmobius.gameserver.model.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureKill;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * This class represents all Playable characters in the world.<br>
 * Playable:
 * <ul>
 * <li>PlayerInstance</li>
 * <li>Summon</li>
 * </ul>
 */
public abstract class Playable extends Creature
{
	private Creature _lockedTarget = null;
	private PlayerInstance transferDmgTo = null;
	
	/**
	 * Creates an abstract playable creature.
	 * @param objectId the playable object ID
	 * @param template the creature template
	 */
	public Playable(int objectId, CreatureTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Playable);
		setInvul(false);
	}
	
	public Playable(CreatureTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Playable);
		setInvul(false);
	}
	
	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}
	
	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		final TerminateReturn returnBack = EventDispatcher.getInstance().notifyEvent(new OnCreatureKill(killer, this), this, TerminateReturn.class);
		if ((returnBack != null) && returnBack.terminate())
		{
			return false;
		}
		
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
			{
				return false;
			}
			// now reset currentHp to zero
			setCurrentHp(0);
			setDead(true);
		}
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();
		
		boolean deleteBuffs = true;
		if (isNoblesseBlessedAffected())
		{
			stopEffects(EffectType.NOBLESSE_BLESSING);
			deleteBuffs = false;
		}
		if (isResurrectSpecialAffected())
		{
			stopEffects(EffectType.RESURRECTION_SPECIAL);
			deleteBuffs = false;
		}
		if (isPlayer())
		{
			final PlayerInstance player = getActingPlayer();
			if (player.hasCharmOfCourage())
			{
				if (player.isInSiege())
				{
					getActingPlayer().reviveRequest(getActingPlayer(), false, 0);
				}
				player.setCharmOfCourage(false);
				player.sendPacket(new EtcStatusUpdate(player));
			}
		}
		
		if (deleteBuffs)
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other PlayerInstance to inform
		broadcastStatusUpdate();
		
		ZoneManager.getInstance().getRegion(this).onDeath(this);
		
		// Notify Quest of Playable's death
		final PlayerInstance actingPlayer = getActingPlayer();
		if (!actingPlayer.isNotifyQuestOfDeathEmpty())
		{
			for (QuestState qs : actingPlayer.getNotifyQuestOfDeath())
			{
				qs.getQuest().notifyDeath(killer == null ? this : killer, this, qs);
			}
		}
		// Notify instance
		if ((getInstanceId() > 0) && isPlayer())
		{
			final Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
			if (instance != null)
			{
				instance.notifyDeath(actingPlayer);
			}
		}
		
		if (killer != null)
		{
			final PlayerInstance player = killer.getActingPlayer();
			if (player != null)
			{
				player.onKillUpdatePvPKarma(this);
			}
		}
		
		// Notify Creature AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		updateEffectIcons();
		return true;
	}
	
	public boolean checkIfPvP(Creature target)
	{
		if (target == null)
		{
			return false; // Target is null
		}
		
		if (target == this)
		{
			return false; // Target is self
		}
		
		if (!target.isPlayable())
		{
			return false; // Target is not a Playable
		}
		
		final PlayerInstance player = getActingPlayer();
		if (player == null)
		{
			return false; // Active player is null
		}
		
		if (player.getKarma() != 0)
		{
			return false; // Active player has karma
		}
		
		final PlayerInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
		{
			return false; // Target player is null
		}
		
		if (targetPlayer == this)
		{
			return false; // Target player is self
		}
		
		if (targetPlayer.getKarma() != 0)
		{
			return false; // Target player has karma
		}
		
		if (targetPlayer.getPvpFlag() == 0)
		{
			return false; // Target player is not flagged
		}
		
		return true;
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	// Support for Noblesse Blessing skill, where buffs are retained after resurrect
	public boolean isNoblesseBlessedAffected()
	{
		return isAffected(EffectFlag.NOBLESS_BLESSING);
	}
	
	/**
	 * @return {@code true} if char can resurrect by himself, {@code false} otherwise
	 */
	public boolean isResurrectSpecialAffected()
	{
		return isAffected(EffectFlag.RESURRECTION_SPECIAL);
	}
	
	/**
	 * @return {@code true} if the Silent Moving mode is active, {@code false} otherwise
	 */
	public boolean isSilentMovingAffected()
	{
		return isAffected(EffectFlag.SILENT_MOVE);
	}
	
	/**
	 * For Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you.
	 * @return
	 */
	public boolean isProtectionBlessingAffected()
	{
		return isAffected(EffectFlag.PROTECTION_BLESSING);
	}
	
	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		getEffectList().updateEffectIcons(partyOnly);
	}
	
	public boolean isLockedTarget()
	{
		return _lockedTarget != null;
	}
	
	public Creature getLockedTarget()
	{
		return _lockedTarget;
	}
	
	public void setLockedTarget(Creature creature)
	{
		_lockedTarget = creature;
	}
	
	public void setTransferDamageTo(PlayerInstance val)
	{
		transferDmgTo = val;
	}
	
	public PlayerInstance getTransferingDamageTo()
	{
		return transferDmgTo;
	}
	
	public abstract void doPickupItem(WorldObject object);
	
	public abstract byte getPvpFlag();
	
	public abstract boolean useMagic(Skill skill, boolean forceUse, boolean dontMove);
	
	public abstract void storeMe();
	
	public abstract void storeEffect(boolean storeEffects);
	
	public abstract void restoreEffects();
	
	public boolean isOnEvent()
	{
		return false;
	}
	
	@Override
	public boolean isPlayable()
	{
		return true;
	}
}
