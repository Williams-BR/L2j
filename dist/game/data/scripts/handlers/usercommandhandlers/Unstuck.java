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
package handlers.usercommandhandlers;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Unstuck user command.
 */
public class Unstuck implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		52
	};
	
	@Override
	public boolean useUserCommand(int id, PlayerInstance player)
	{
		if (player.isRegisteredOnEvent())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		else if (player.isJailed())
		{
			player.sendMessage("You cannot use this function while you are jailed.");
			return false;
		}
		
		if (Config.FACTION_SYSTEM_ENABLED && !player.isGood() && !player.isEvil())
		{
			player.sendMessage("You cannot use this function while you are neutral.");
			return false;
		}
		
		final int unstuckTimer = (player.getAccessLevel().isGm() ? 1000 : Config.UNSTUCK_INTERVAL * 1000);
		if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_SKILL_IN_A_GRAND_OLYMPIAD_MATCH);
			return false;
		}
		
		if (player.isCastingNow() || player.isMovementDisabled() || player.isMuted() || player.isAlikeDead() || player.inObserverMode() || player.isCombatFlagEquipped())
		{
			return false;
		}
		
		player.forceIsCasting(GameTimeTaskManager.getInstance().getGameTicks() + (unstuckTimer / GameTimeTaskManager.MILLIS_IN_TICK));
		
		final Skill escape = SkillData.getInstance().getSkill(2099, 1); // 5 minutes escape
		final Skill gmEscape = SkillData.getInstance().getSkill(2100, 1); // 1 second escape
		if (player.getAccessLevel().isGm())
		{
			if (gmEscape != null)
			{
				player.doCast(gmEscape);
				return true;
			}
			player.sendMessage("You use Escape: 1 second.");
		}
		else if ((Config.UNSTUCK_INTERVAL == 300) && (escape != null))
		{
			player.doCast(escape);
			return true;
		}
		else
		{
			if (Config.UNSTUCK_INTERVAL > 100)
			{
				player.sendMessage("You use Escape: " + (unstuckTimer / 60000) + " minutes.");
			}
			else
			{
				player.sendMessage("You use Escape: " + (unstuckTimer / 1000) + " seconds.");
			}
		}
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		// SoE Animation section
		player.setTarget(player);
		player.disableAllSkills();
		
		Broadcast.toSelfAndKnownPlayersInRadius(player, new MagicSkillUse(player, 1050, 1, unstuckTimer, 0), 900);
		player.sendPacket(new SetupGauge(player.getObjectId(), 0, unstuckTimer));
		// End SoE Animation section
		
		// continue execution later
		player.setSkillCast(ThreadPool.schedule(new EscapeFinalizer(player), unstuckTimer));
		return true;
	}
	
	private static class EscapeFinalizer implements Runnable
	{
		private final PlayerInstance _player;
		
		protected EscapeFinalizer(PlayerInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player.isDead())
			{
				return;
			}
			
			_player.setIn7sDungeon(false);
			_player.enableAllSkills();
			_player.setCastingNow(false);
			_player.setInstanceId(0);
			_player.teleToLocation(TeleportWhereType.TOWN);
		}
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}