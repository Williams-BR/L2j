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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestMagicSkillUse implements IClientIncomingPacket
{
	private int _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_magicId = packet.readD(); // Identifier of the used skill
		_ctrlPressed = packet.readD() != 0; // True if it's a ForceAttack : Ctrl pressed
		_shiftPressed = packet.readC() != 0; // True if Shift pressed
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		// Get the current PlayerInstance of the player
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isDead())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isFakeDeath())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Get the level of the used skill
		Skill skill = player.getKnownSkill(_magicId);
		if (skill == null)
		{
			// Player doesn't know this skill, maybe it's the display Id.
			skill = player.getCustomSkill(_magicId);
			if (skill == null)
			{
				skill = player.getTransformSkill(_magicId);
				if (skill == null)
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					// if (_magicId > 0)
					// {
					// LOGGER.warning("Skill Id " + _magicId + " not found in player: " + player);
					// }
					return;
				}
			}
		}
		
		// Avoid Use of Skills in AirShip.
		if (player.isPlayable() && player.isInAirShip())
		{
			player.sendPacket(SystemMessageId.THIS_ACTION_IS_PROHIBITED_WHILE_MOUNTED_OR_ON_AN_AIRSHIP);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if ((player.isTransformed() || player.isInStance()) && !player.hasTransformSkill(skill.getId()))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// If Alternate rule Karma punishment is set to true, forbid skill Return to player with Karma
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (player.getKarma() > 0) && skill.hasEffectType(EffectType.TELEPORT))
		{
			return;
		}
		
		// players mounted on pets cannot use any toggle skills
		if (skill.isToggle() && player.isMounted())
		{
			return;
		}
		
		player.onActionRequest();
		
		// Stop if use self-buff (except if on AirShip or Boat).
		if ((skill.isContinuous() && !skill.isDebuff() && (skill.getTargetType() == TargetType.SELF)) && (!player.isInAirShip() || !player.isInBoat()))
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, player.getLocation());
		}
		
		player.useMagic(skill, _ctrlPressed, _shiftPressed);
	}
}
