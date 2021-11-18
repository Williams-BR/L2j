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
package ai.others.NpcBuffers;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.TamedBeastInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public class NpcBufferAI implements Runnable
{
	private final Npc _npc;
	private final NpcBufferSkillData _skillData;
	
	protected NpcBufferAI(Npc npc, NpcBufferSkillData skill)
	{
		_npc = npc;
		_skillData = skill;
	}
	
	@Override
	public void run()
	{
		if ((_npc == null) || !_npc.isSpawned() || _npc.isDecayed() || _npc.isDead() || (_skillData == null) || (_skillData.getSkill() == null))
		{
			return;
		}
		
		if ((_npc.getSummoner() == null) || !_npc.getSummoner().isPlayer())
		{
			return;
		}
		
		final Skill skill = _skillData.getSkill();
		final PlayerInstance player = _npc.getSummoner().getActingPlayer();
		
		switch (_skillData.getAffectScope())
		{
			case PARTY:
			{
				if (player.isInParty())
				{
					for (PlayerInstance member : player.getParty().getMembers())
					{
						if (Util.checkIfInRange(skill.getAffectRange(), _npc, member, true) && !member.isDead())
						{
							skill.applyEffects(player, member);
						}
					}
				}
				else
				{
					if (Util.checkIfInRange(skill.getAffectRange(), _npc, player, true) && !player.isDead())
					{
						skill.applyEffects(player, player);
					}
				}
				break;
			}
			case RANGE:
			{
				for (Creature target : World.getInstance().getVisibleObjectsInRange(_npc, Creature.class, skill.getAffectRange()))
				{
					switch (_skillData.getAffectObject())
					{
						case FRIEND:
						{
							if (isFriendly(player, target) && !target.isDead())
							{
								skill.applyEffects(target, target);
							}
							break;
						}
						case NOT_FRIEND:
						{
							if (isEnemy(player, target) && !target.isDead())
							{
								// Update PvP status
								if (target.isPlayable())
								{
									player.updatePvPStatus(target);
								}
								skill.applyEffects(target, target);
							}
							break;
						}
					}
				}
				break;
			}
		}
		ThreadPool.schedule(this, _skillData.getDelay());
	}
	
	/**
	 * Verifies if the character is an friend and can be affected by positive effect.
	 * @param player the player
	 * @param target the target
	 * @return {@code true} if target can be affected by positive effect, {@code false} otherwise
	 */
	private boolean isFriendly(PlayerInstance player, Creature target)
	{
		if (target.isPlayable())
		{
			final PlayerInstance targetPlayer = target.getActingPlayer();
			if (player == targetPlayer)
			{
				return true;
			}
			
			if (player.isInPartyWith(targetPlayer))
			{
				return true;
			}
			
			if (player.isInCommandChannelWith(targetPlayer))
			{
				return true;
			}
			
			if (player.isInClanWith(targetPlayer))
			{
				return true;
			}
			
			if (player.isInAllyWith(targetPlayer))
			{
				return true;
			}
			
			if (player.isOnSameSiegeSideWith(targetPlayer))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Verifies if the character is an enemy and can be affected by negative effect.
	 * @param player the player
	 * @param target the target
	 * @return {@code true} if target can be affected by negative effect, {@code false} otherwise
	 */
	private boolean isEnemy(PlayerInstance player, Creature target)
	{
		if (isFriendly(player, target))
		{
			return false;
		}
		
		if (target instanceof TamedBeastInstance)
		{
			return isEnemy(player, ((TamedBeastInstance) target).getOwner());
		}
		
		if (target.isMonster())
		{
			return true;
		}
		
		if (target.isPlayable())
		{
			final PlayerInstance targetPlayer = target.getActingPlayer();
			if (!isFriendly(player, targetPlayer))
			{
				if (targetPlayer.getPvpFlag() != 0)
				{
					return true;
				}
				
				if (targetPlayer.getKarma() != 0)
				{
					return true;
				}
				
				if (player.isAtWarWith(targetPlayer))
				{
					return true;
				}
				
				if (targetPlayer.isInsideZone(ZoneId.PVP))
				{
					return true;
				}
			}
		}
		return false;
	}
}