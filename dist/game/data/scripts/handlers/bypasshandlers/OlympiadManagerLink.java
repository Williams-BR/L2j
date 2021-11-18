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

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.OlympiadManagerInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.olympiad.CompetitionType;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExHeroList;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Util;

/**
 * @author DS
 */
public class OlympiadManagerLink implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"olympiaddesc",
		"olympiadnoble",
		"olybuff",
		"olympiad"
	};
	
	private static final String FEWER_THAN = "Fewer than " + Config.ALT_OLY_REG_DISPLAY;
	private static final String MORE_THAN = "More than " + Config.ALT_OLY_REG_DISPLAY;
	private static final int GATE_PASS = Config.ALT_OLY_COMP_RITEM;
	
	private static final SkillHolder[] ALLOWED_BUFFS =
	{
		new SkillHolder(4357, 2), // Haste Lv2
		new SkillHolder(4342, 2), // Wind Walk Lv2
		new SkillHolder(4356, 3), // Empower Lv3
		new SkillHolder(4355, 3), // Acumen Lv3
		new SkillHolder(4351, 6), // Concentration Lv6
		new SkillHolder(4345, 3), // Might Lv3
		new SkillHolder(4358, 3), // Guidance Lv3
		new SkillHolder(4359, 3), // Focus Lv3
		new SkillHolder(4360, 3), // Death Whisper Lv3
		new SkillHolder(4352, 2), // Berserker Spirit Lv2
	};
	
	@Override
	public boolean useBypass(String command, PlayerInstance player, Creature target)
	{
		if (!(target instanceof OlympiadManagerInstance))
		{
			return false;
		}
		
		try
		{
			if (command.toLowerCase().startsWith("olympiaddesc"))
			{
				final int val = Integer.parseInt(command.substring(13, 14));
				final String suffix = command.substring(14);
				((OlympiadManagerInstance) target).showChatWindow(player, val, suffix);
			}
			else if (command.toLowerCase().startsWith("olympiadnoble"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
				if (player.isCursedWeaponEquipped())
				{
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_cursed_weapon.htm");
					player.sendPacket(html);
					return false;
				}
				if (player.getClassIndex() != 0)
				{
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_sub.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					player.sendPacket(html);
					return false;
				}
				if (!player.isNoble() || (player.getClassId().level() < 3))
				{
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_thirdclass.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					player.sendPacket(html);
					return false;
				}
				
				final int val = Integer.parseInt(command.substring(14));
				switch (val)
				{
					case 0: // H5 match selection
					{
						if (!OlympiadManager.getInstance().isRegistered(player))
						{
							html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_desc2a.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							html.replace("%olympiad_period%", String.valueOf(Olympiad.getInstance().getPeriod()));
							html.replace("%olympiad_cycle%", String.valueOf(Olympiad.getInstance().getCurrentCycle()));
							html.replace("%olympiad_opponent%", String.valueOf(OlympiadManager.getInstance().getCountOpponents()));
							player.sendPacket(html);
						}
						else
						{
							html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_unregister.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							player.sendPacket(html);
						}
						break;
					}
					case 1: // unregister
					{
						OlympiadManager.getInstance().unRegisterNoble(player);
						break;
					}
					case 2: // show waiting list | TODO: cleanup (not used anymore)
					{
						final int nonClassed = OlympiadManager.getInstance().getRegisteredNonClassBased().size();
						final int teams = OlympiadManager.getInstance().getRegisteredTeamsBased().size();
						final Collection<List<Integer>> allClassed = OlympiadManager.getInstance().getRegisteredClassBased().values();
						int classed = 0;
						if (!allClassed.isEmpty())
						{
							for (List<Integer> cls : allClassed)
							{
								if (cls != null)
								{
									classed += cls.size();
								}
							}
						}
						html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
						if (Config.ALT_OLY_REG_DISPLAY > 0)
						{
							html.replace("%listClassed%", classed < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
							html.replace("%listNonClassedTeam%", teams < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
							html.replace("%listNonClassed%", nonClassed < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
						}
						else
						{
							html.replace("%listClassed%", String.valueOf(classed));
							html.replace("%listNonClassedTeam%", String.valueOf(teams));
							html.replace("%listNonClassed%", String.valueOf(nonClassed));
						}
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						player.sendPacket(html);
						break;
					}
					case 3: // There are %points% Grand Olympiad points granted for this event. | TODO: cleanup (not used anymore)
					{
						final int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
						html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
						html.replace("%points%", String.valueOf(points));
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						player.sendPacket(html);
						break;
					}
					case 4: // register non classed
					{
						OlympiadManager.getInstance().registerNoble(player, CompetitionType.NON_CLASSED);
						break;
					}
					case 5: // register classed
					{
						OlympiadManager.getInstance().registerNoble(player, CompetitionType.CLASSED);
						break;
					}
					case 6: // request tokens reward
					{
						if (player.getVariables().getInt(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR, 0) > 0)
						{
							html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_settle.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							player.sendPacket(html);
						}
						else
						{
							html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_nopoints2.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							player.sendPacket(html);
						}
						break;
					}
					case 7: // Equipment Rewards
					{
						MultisellData.getInstance().separateAndSend(102, player, (Npc) target, false);
						break;
					}
					case 8: // Misc. Rewards
					{
						MultisellData.getInstance().separateAndSend(103, player, (Npc) target, false);
						break;
					}
					case 9: // Your Grand Olympiad Score from the previous period is %points% point(s) | TODO: cleanup (not used anymore)
					{
						final int point = Olympiad.getInstance().getLastNobleOlympiadPoints(player.getObjectId());
						html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_points2.htm");
						html.replace("%points%", String.valueOf(point));
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						player.sendPacket(html);
						break;
					}
					case 10: // give tokens to player
					{
						final int passes = player.getVariables().getInt(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR, 0);
						if (passes > 0)
						{
							player.getVariables().remove(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR);
							player.addItem("Olympiad", GATE_PASS, passes * Config.ALT_OLY_GP_PER_POINT, player, true);
						}
						break;
					}
					case 11: // register team
					{
						OlympiadManager.getInstance().registerNoble(player, CompetitionType.TEAMS);
						break;
					}
					default:
					{
						LOGGER.warning("Olympiad System: Couldnt send packet for request " + val);
						break;
					}
				}
			}
			else if (command.toLowerCase().startsWith("olybuff"))
			{
				int buffCount = player.getOlympiadBuffCount();
				if (buffCount <= 0)
				{
					return false;
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
				final String[] params = command.split(" ");
				if (!Util.isDigit(params[1]))
				{
					LOGGER.warning("Olympiad Buffer Warning: npcId = " + target.getId() + " has invalid buffGroup set in the bypass for the buff selected: " + params[1]);
					return false;
				}
				
				final int index = Integer.parseInt(params[1]);
				if ((index < 0) || (index >= ALLOWED_BUFFS.length))
				{
					LOGGER.warning("Olympiad Buffer Warning: npcId = " + target.getId() + " has invalid index sent in the bypass: " + index);
					return false;
				}
				
				if (buffCount > 0)
				{
					final Skill skill = ALLOWED_BUFFS[index].getSkill();
					if (skill != null)
					{
						target.setTarget(player);
						
						player.setOlympiadBuffCount(--buffCount);
						
						target.broadcastPacket(new MagicSkillUse(target, player, skill.getId(), skill.getLevel(), 0, 0));
						skill.applyEffects(player, player);
						final Summon summon = player.getSummon();
						if (summon != null)
						{
							target.broadcastPacket(new MagicSkillUse(target, summon, skill.getId(), skill.getLevel(), 0, 0));
							skill.applyEffects(summon, summon);
						}
					}
				}
				
				if (buffCount > 0)
				{
					html.setFile(player, buffCount == Config.ALT_OLY_MAX_BUFFS ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					player.sendPacket(html);
				}
				else
				{
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					player.sendPacket(html);
					target.decayMe();
				}
			}
			else if (command.toLowerCase().startsWith("olympiad"))
			{
				final int val = Integer.parseInt(command.substring(9, 10));
				final NpcHtmlMessage reply = new NpcHtmlMessage(target.getObjectId());
				
				switch (val)
				{
					case 2: // show rank for a specific class
					{
						// for example >> Olympiad 1_88
						final int classId = Integer.parseInt(command.substring(11));
						if (((classId >= 88) && (classId <= 118)) || ((classId >= 131) && (classId <= 134)) || (classId == 136))
						{
							final List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
							reply.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "olympiad_ranking.htm");
							int index = 1;
							for (String name : names)
							{
								reply.replace("%place" + index + "%", String.valueOf(index));
								reply.replace("%rank" + index + "%", name);
								index++;
								if (index > 10)
								{
									break;
								}
							}
							for (; index <= 10; index++)
							{
								reply.replace("%place" + index + "%", "");
								reply.replace("%rank" + index + "%", "");
							}
							reply.replace("%objectId%", String.valueOf(target.getObjectId()));
							player.sendPacket(reply);
						}
						break;
					}
					case 4: // hero list
					{
						player.sendPacket(new ExHeroList());
						break;
					}
					case 5: // Hero Certification
					{
						if (Hero.getInstance().isUnclaimedHero(player.getObjectId()))
						{
							Hero.getInstance().claimHero(player);
							reply.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "hero_receive.htm");
						}
						else
						{
							reply.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "hero_notreceive.htm");
						}
						player.sendPacket(reply);
						break;
					}
					default:
					{
						LOGGER.warning("Olympiad System: Couldnt send packet for request " + val);
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
		}
		
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
