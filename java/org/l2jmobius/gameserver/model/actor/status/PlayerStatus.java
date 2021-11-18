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
package org.l2jmobius.gameserver.model.actor.status;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.instancemanager.DuelManager;
import org.l2jmobius.gameserver.model.Duel;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

public class PlayerStatus extends PlayableStatus
{
	private double _currentCp = 0; // Current CP of the PlayerInstance
	
	public PlayerStatus(PlayerInstance player)
	{
		super(player);
	}
	
	@Override
	public void reduceCp(int value)
	{
		setCurrentCp(_currentCp > value ? _currentCp - value : 0);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false, false);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		reduceHp(value, attacker, awake, isDOT, isHPConsumption, false);
	}
	
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean ignoreCP)
	{
		if (getActiveChar().isDead())
		{
			return;
		}
		
		// If OFFLINE_MODE_NO_DAMAGE is enabled and player is offline and he is in store/craft mode, no damage is taken.
		if (Config.OFFLINE_MODE_NO_DAMAGE && (getActiveChar().getClient() != null) && getActiveChar().getClient().isDetached() && ((Config.OFFLINE_TRADE_ENABLE && ((getActiveChar().getPrivateStoreType() == PrivateStoreType.SELL) || (getActiveChar().getPrivateStoreType() == PrivateStoreType.BUY))) || (Config.OFFLINE_CRAFT_ENABLE && (getActiveChar().isCrafting() || (getActiveChar().getPrivateStoreType() == PrivateStoreType.MANUFACTURE)))))
		{
			return;
		}
		
		if (getActiveChar().isInvul() && !(isDOT || isHPConsumption))
		{
			return;
		}
		
		if (!isHPConsumption)
		{
			getActiveChar().stopEffectsOnDamage(awake);
			// Attacked players in craft/shops stand up.
			if (getActiveChar().isCrafting() || getActiveChar().isInStoreMode())
			{
				getActiveChar().setPrivateStoreType(PrivateStoreType.NONE);
				getActiveChar().standUp();
				getActiveChar().broadcastUserInfo();
			}
			else if (getActiveChar().isSitting())
			{
				getActiveChar().standUp();
			}
			
			if (!isDOT && getActiveChar().isStunned() && (Rnd.get(10) == 0))
			{
				getActiveChar().stopStunning(true);
			}
		}
		
		double amount = value;
		int fullValue = (int) amount;
		int tDmg = 0;
		int mpDam = 0;
		if ((attacker != null) && (attacker != getActiveChar()))
		{
			final PlayerInstance attackerPlayer = attacker.getActingPlayer();
			if (attackerPlayer != null)
			{
				if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
				{
					return;
				}
				
				if (getActiveChar().isInDuel())
				{
					if (getActiveChar().getDuelState() == Duel.DUELSTATE_DEAD)
					{
						return;
					}
					else if (getActiveChar().getDuelState() == Duel.DUELSTATE_WINNER)
					{
						return;
					}
					
					// cancel duel if player got hit by another player, that is not part of the duel
					if (attackerPlayer.getDuelId() != getActiveChar().getDuelId())
					{
						getActiveChar().setDuelState(Duel.DUELSTATE_INTERRUPTED);
					}
				}
			}
			
			// Check and calculate transfered damage
			final Summon summon = getActiveChar().getSummon();
			if (getActiveChar().hasServitor() && Util.checkIfInRange(1000, getActiveChar(), summon, true))
			{
				tDmg = ((int) amount * (int) getActiveChar().getStat().calcStat(Stat.TRANSFER_DAMAGE_PERCENT, 0, null, null)) / 100;
				
				// Only transfer dmg up to current HP, it should not be killed
				tDmg = Math.min((int) summon.getCurrentHp() - 1, tDmg);
				if (tDmg > 0)
				{
					summon.reduceCurrentHp(tDmg, attacker, null);
					amount -= tDmg;
					fullValue = (int) amount; // reduce the announced value here as player will get a message about summon damage
				}
			}
			
			mpDam = ((int) amount * (int) getActiveChar().getStat().calcStat(Stat.MANA_SHIELD_PERCENT, 0, null, null)) / 100;
			if (mpDam > 0)
			{
				mpDam = (int) (amount - mpDam);
				if (mpDam > getActiveChar().getCurrentMp())
				{
					getActiveChar().sendPacket(SystemMessageId.MP_BECAME_0_AND_THE_ARCANE_SHIELD_IS_DISAPPEARING);
					getActiveChar().stopSkillEffects(SkillFinishType.REMOVED, 1556);
					amount = mpDam - getActiveChar().getCurrentMp();
					getActiveChar().setCurrentMp(0);
				}
				else
				{
					getActiveChar().reduceCurrentMp(mpDam);
					final SystemMessage smsg = new SystemMessage(SystemMessageId.ARCANE_SHIELD_DECREASED_YOUR_MP_BY_S1_INSTEAD_OF_HP);
					smsg.addInt(mpDam);
					getActiveChar().sendPacket(smsg);
					return;
				}
			}
			
			final PlayerInstance caster = getActiveChar().getTransferingDamageTo();
			if ((caster != null) && (getActiveChar().getParty() != null) && Util.checkIfInRange(1000, getActiveChar(), caster, true) && !caster.isDead() && (getActiveChar() != caster) && getActiveChar().getParty().getMembers().contains(caster))
			{
				int transferDmg = Math.min((int) caster.getCurrentHp() - 1, ((int) amount * (int) getActiveChar().getStat().calcStat(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0, null, null)) / 100);
				if (transferDmg > 0)
				{
					int membersInRange = 0;
					for (PlayerInstance member : caster.getParty().getMembers())
					{
						if (Util.checkIfInRange(1000, member, caster, false) && (member != caster))
						{
							membersInRange++;
						}
					}
					
					if ((attacker.isPlayable() || attacker.isFakePlayer()) && (caster.getCurrentCp() > 0))
					{
						if (caster.getCurrentCp() > transferDmg)
						{
							caster.getStatus().reduceCp(transferDmg);
						}
						else
						{
							transferDmg = (int) (transferDmg - caster.getCurrentCp());
							caster.getStatus().reduceCp((int) caster.getCurrentCp());
						}
					}
					
					if (membersInRange > 0)
					{
						caster.reduceCurrentHp(transferDmg / membersInRange, attacker, null);
						amount -= transferDmg;
						fullValue = (int) amount;
					}
				}
			}
			
			if (!ignoreCP && (attacker.isPlayable() || attacker.isFakePlayer()))
			{
				if (_currentCp >= amount)
				{
					setCurrentCp(_currentCp - amount); // Set Cp to diff of Cp vs value
					amount = 0; // No need to subtract anything from Hp
				}
				else
				{
					amount -= _currentCp; // Get diff from value vs Cp; will apply diff to Hp
					setCurrentCp(0, false); // Set Cp to 0
				}
			}
			
			if ((fullValue > 0) && !isDOT)
			{
				// Send a System Message to the PlayerInstance
				SystemMessage smsg = new SystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
				smsg.addString(getActiveChar().getName());
				
				// Localisation related.
				String targetName = attacker.getName();
				if (Config.MULTILANG_ENABLE && attacker.isNpc())
				{
					final String[] localisation = NpcNameLocalisationData.getInstance().getLocalisation(getActiveChar().getLang(), attacker.getId());
					if (localisation != null)
					{
						targetName = localisation[0];
					}
				}
				
				smsg.addString(targetName);
				smsg.addInt(fullValue);
				getActiveChar().sendPacket(smsg);
				
				if ((tDmg > 0) && (attackerPlayer != null))
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_DEALT_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_THE_SERVITOR);
					smsg.addInt(fullValue);
					smsg.addInt(tDmg);
					attackerPlayer.sendPacket(smsg);
				}
			}
		}
		
		if (amount > 0)
		{
			amount = getCurrentHp() - amount;
			if (amount <= 0)
			{
				if (getActiveChar().isInDuel())
				{
					getActiveChar().disableAllSkills();
					stopHpMpRegeneration();
					if (attacker != null)
					{
						attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						attacker.sendPacket(ActionFailed.STATIC_PACKET);
						attacker.setTarget(null);
						attacker.abortAttack();
					}
					
					// let the DuelManager know of his defeat
					DuelManager.getInstance().onPlayerDefeat(getActiveChar());
					amount = 1;
				}
				else
				{
					amount = 0;
				}
			}
			setCurrentHp(amount);
		}
		
		if ((getActiveChar().getCurrentHp() < 0.5) && !isHPConsumption)
		{
			getActiveChar().abortAttack();
			getActiveChar().abortCast();
			
			if (getActiveChar().isInOlympiadMode())
			{
				stopHpMpRegeneration();
				getActiveChar().setDead(true);
				getActiveChar().setIsPendingRevive(true);
				if (getActiveChar().hasSummon())
				{
					getActiveChar().getSummon().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
				}
				return;
			}
			
			getActiveChar().doDie(attacker);
			if (!Config.DISABLE_TUTORIAL)
			{
				final QuestState qs = getActiveChar().getQuestState("Q00255_Tutorial");
				if (qs != null)
				{
					qs.getQuest().notifyEvent("CE30", null, getActiveChar());
				}
			}
		}
	}
	
	@Override
	public boolean setCurrentHp(double newHp, boolean broadcastPacket)
	{
		final boolean result = super.setCurrentHp(newHp, broadcastPacket);
		if (!Config.DISABLE_TUTORIAL && (getCurrentHp() <= (getActiveChar().getStat().getMaxHp() * .3)))
		{
			final QuestState qs = getActiveChar().getQuestState("Q00255_Tutorial");
			if (qs != null)
			{
				qs.getQuest().notifyEvent("CE45", null, getActiveChar());
			}
		}
		return result;
	}
	
	@Override
	public double getCurrentCp()
	{
		return _currentCp;
	}
	
	@Override
	public void setCurrentCp(double newCp)
	{
		setCurrentCp(newCp, true);
	}
	
	public void setCurrentCp(double value, boolean broadcastPacket)
	{
		// Get the Max CP of the Creature
		final int currentCp = (int) _currentCp;
		final int maxCp = getActiveChar().getStat().getMaxCp();
		
		synchronized (this)
		{
			if (getActiveChar().isDead())
			{
				return;
			}
			
			final double newCp = Math.max(0, value);
			if (newCp >= maxCp)
			{
				// Set the RegenActive flag to false
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other PlayerInstance to inform
		if ((currentCp != _currentCp) && broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	@Override
	protected void doRegeneration()
	{
		final PlayerStat charstat = getActiveChar().getStat();
		
		// Modify the current CP of the Creature and broadcast Server->Client packet StatusUpdate
		if (_currentCp < charstat.getMaxRecoverableCp())
		{
			setCurrentCp(_currentCp + Formulas.calcCpRegen(getActiveChar()), false);
		}
		
		// Modify the current HP of the Creature and broadcast Server->Client packet StatusUpdate
		if (getCurrentHp() < charstat.getMaxRecoverableHp())
		{
			setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()), false);
		}
		
		// Modify the current MP of the Creature and broadcast Server->Client packet StatusUpdate
		if (getCurrentMp() < charstat.getMaxRecoverableMp())
		{
			setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()), false);
		}
		
		getActiveChar().broadcastStatusUpdate(); // send the StatusUpdate packet
	}
	
	@Override
	public PlayerInstance getActiveChar()
	{
		return (PlayerInstance) super.getActiveChar();
	}
}
