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
package handlers.bypasshandlers;

import java.util.List;
import java.util.logging.Level;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class SkillList implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"SkillList"
	};
	
	@Override
	public boolean useBypass(String command, PlayerInstance player, Creature target)
	{
		if ((target == null) || !target.isNpc())
		{
			return false;
		}
		
		if (Config.ALT_GAME_SKILL_LEARN)
		{
			try
			{
				final String id = command.substring(9).trim();
				if (id.length() != 0)
				{
					NpcInstance.showSkillList(player, (Npc) target, ClassId.getClassId(Integer.parseInt(id)));
				}
				else
				{
					boolean ownClass = false;
					
					final List<ClassId> classesToTeach = ((NpcInstance) target).getClassesToTeach();
					for (ClassId cid : classesToTeach)
					{
						if (cid.equalsOrChildOf(player.getClassId()))
						{
							ownClass = true;
							break;
						}
					}
					
					String text = "<html><body><center>Skill learning:</center><br>";
					if (!ownClass)
					{
						final String charType = player.getClassId().isMage() ? "fighter" : "mage";
						text += "Skills of your class are the easiest to learn.<br>Skills of another class of your race are a little harder.<br>Skills for classes of another race are extremely difficult.<br>But the hardest of all to learn are the  " + charType + "skills!<br>";
					}
					
					// make a list of classes
					if (!classesToTeach.isEmpty())
					{
						int count = 0;
						ClassId classCheck = player.getClassId();
						
						while ((count == 0) && (classCheck != null))
						{
							for (ClassId cid : classesToTeach)
							{
								if (cid.level() > classCheck.level())
								{
									continue;
								}
								
								if (SkillTreeData.getInstance().getAvailableSkills(player, cid, false, false).isEmpty())
								{
									continue;
								}
								
								text += "<a action=\"bypass -h npc_%objectId%_SkillList " + cid.getId() + "\">Learn " + cid + "'s class Skills</a><br>\n";
								count++;
							}
							classCheck = classCheck.getParent();
						}
						classCheck = null;
					}
					else
					{
						text += "No Skills.<br>";
					}
					text += "</body></html>";
					
					final NpcHtmlMessage html = new NpcHtmlMessage(((Npc) target).getObjectId());
					html.setHtml(text);
					html.replace("%objectId%", String.valueOf(((Npc) target).getObjectId()));
					player.sendPacket(html);
					
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
			}
		}
		else
		{
			NpcInstance.showSkillList(player, (Npc) target, player.getClassId());
		}
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
