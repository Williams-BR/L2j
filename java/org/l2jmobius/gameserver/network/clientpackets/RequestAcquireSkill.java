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

import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.AcquireSkillType;
import org.l2jmobius.gameserver.enums.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.FishermanInstance;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.VillageMasterInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanPrivilege;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerSkillLearn;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skills.CommonSkill;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillDone;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Util;

/**
 * Request Acquire Skill client packet implementation.
 * @author Zoey76
 */
public class RequestAcquireSkill implements IClientIncomingPacket
{
	private static final String[] QUEST_VAR_NAMES =
	{
		"EmergentAbility65-",
		"EmergentAbility70-",
		"ClassAbility75-",
		"ClassAbility80-"
	};
	
	private int _id;
	private int _level;
	private AcquireSkillType _skillType;
	private int _subType;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_id = packet.readD();
		_level = packet.readD();
		_skillType = AcquireSkillType.getAcquireSkillType(packet.readD());
		if (_skillType == AcquireSkillType.SUBPLEDGE)
		{
			_subType = packet.readD();
		}
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_level < 1) || (_level > 1000) || (_id < 1) || (_id > 32000))
		{
			Util.handleIllegalPlayerAction(player, "Wrong Packet Data in Aquired Skill", Config.DEFAULT_PUNISH);
			LOGGER.warning("Recived Wrong Packet Data in Aquired Skill - id: " + _id + " level: " + _level + " for " + player);
			return;
		}
		
		final Npc trainer = player.getLastFolkNPC();
		if ((trainer == null) || !trainer.isNpc() || (!trainer.canInteract(player) && !player.isGM()))
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_id, _level);
		if (skill == null)
		{
			LOGGER.warning(RequestAcquireSkill.class.getSimpleName() + ": Player " + player.getName() + " is trying to learn a null skill Id: " + _id + " level: " + _level + "!");
			return;
		}
		
		// Hack check. Doesn't apply to all Skill Types
		final int prevSkillLevel = player.getSkillLevel(_id);
		if ((prevSkillLevel > 0) && !((_skillType == AcquireSkillType.TRANSFER) || (_skillType == AcquireSkillType.SUBPLEDGE)))
		{
			if (prevSkillLevel == _level)
			{
				return;
			}
			else if (prevSkillLevel != (_level - 1))
			{
				// The previous level skill has not been learned.
				player.sendPacket(SystemMessageId.THE_PREVIOUS_LEVEL_SKILL_HAS_NOT_BEEN_LEARNED);
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!", IllegalActionPunishmentType.NONE);
				return;
			}
		}
		
		final SkillLearn s = SkillTreeData.getInstance().getSkillLearn(_skillType, _id, _level, player);
		if (s == null)
		{
			return;
		}
		
		switch (_skillType)
		{
			case CLASS:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case TRANSFORM:
			{
				// Hack check.
				if (!canTransform(player))
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_NOT_COMPLETED_THE_NECESSARY_QUEST_FOR_SKILL_ACQUISITION);
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level " + _level + " without required quests!", IllegalActionPunishmentType.NONE);
					return;
				}
				
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case FISHING:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case PLEDGE:
			{
				if (!player.isClanLeader())
				{
					return;
				}
				
				final Clan clan = player.getClan();
				final int repCost = s.getLevelUpSp();
				if (clan.getReputationScore() >= repCost)
				{
					if (Config.LIFE_CRYSTAL_NEEDED)
					{
						for (ItemHolder item : s.getRequiredItems())
						{
							if (!player.destroyItemByItemId("Consume", item.getId(), item.getCount(), trainer, false))
							{
								// Doesn't have required item.
								player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL);
								VillageMasterInstance.showPledgeSkillList(player);
								return;
							}
							
							final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
							sm.addItemName(item.getId());
							sm.addLong(item.getCount());
							player.sendPacket(sm);
						}
					}
					
					clan.takeReputationScore(repCost, true);
					
					final SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
					cr.addInt(repCost);
					player.sendPacket(cr);
					
					clan.addNewSkill(skill);
					
					clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
					player.sendPacket(new AcquireSkillDone());
					VillageMasterInstance.showPledgeSkillList(player);
				}
				else
				{
					player.sendPacket(SystemMessageId.THE_ATTEMPT_TO_ACQUIRE_THE_SKILL_HAS_FAILED_BECAUSE_OF_AN_INSUFFICIENT_CLAN_REPUTATION_SCORE);
					VillageMasterInstance.showPledgeSkillList(player);
				}
				break;
			}
			case SUBPLEDGE:
			{
				if (!player.isClanLeader() || !player.hasClanPrivilege(ClanPrivilege.CL_TROOPS_FAME))
				{
					return;
				}
				
				final Clan clan = player.getClan();
				if ((clan.getFortId() == 0) && (clan.getCastleId() == 0))
				{
					return;
				}
				
				// Hack check. Check if SubPledge can accept the new skill:
				if (!clan.isLearnableSubPledgeSkill(skill, _subType))
				{
					player.sendPacket(SystemMessageId.THIS_SQUAD_SKILL_HAS_ALREADY_BEEN_ACQUIRED);
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!", IllegalActionPunishmentType.NONE);
					return;
				}
				
				final int repCost = s.getLevelUpSp();
				if (clan.getReputationScore() < repCost)
				{
					player.sendPacket(SystemMessageId.THE_ATTEMPT_TO_ACQUIRE_THE_SKILL_HAS_FAILED_BECAUSE_OF_AN_INSUFFICIENT_CLAN_REPUTATION_SCORE);
					return;
				}
				
				for (ItemHolder item : s.getRequiredItems())
				{
					if (!player.destroyItemByItemId("SubSkills", item.getId(), item.getCount(), trainer, false))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL);
						return;
					}
					
					final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
					sm.addItemName(item.getId());
					sm.addLong(item.getCount());
					player.sendPacket(sm);
				}
				
				if (repCost > 0)
				{
					clan.takeReputationScore(repCost, true);
					final SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
					cr.addInt(repCost);
					player.sendPacket(cr);
				}
				
				clan.addNewSkill(skill, _subType);
				clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
				player.sendPacket(new AcquireSkillDone());
				showSubUnitSkillList(player);
				break;
			}
			case TRANSFER:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case SUBCLASS:
			{
				// Hack check.
				if (player.isSubClassActive())
				{
					player.sendPacket(SystemMessageId.THIS_SKILL_CANNOT_BE_LEARNED_WHILE_IN_THE_SUB_CLASS_STATE_PLEASE_TRY_AGAIN_AFTER_CHANGING_TO_THE_MAIN_CLASS);
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level " + _level + " while Sub-Class is active!", IllegalActionPunishmentType.NONE);
					return;
				}
				
				// Certification Skills - Exploit fix
				if ((prevSkillLevel == -1) && (_level > 1))
				{
					// The previous level skill has not been learned.
					player.sendPacket(SystemMessageId.THE_PREVIOUS_LEVEL_SKILL_HAS_NOT_BEEN_LEARNED);
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!", IllegalActionPunishmentType.NONE);
					return;
				}
				
				QuestState qs = player.getQuestState("SubClassSkills");
				if (qs == null)
				{
					final Quest subClassSkilllsQuest = QuestManager.getInstance().getQuest("SubClassSkills");
					if (subClassSkilllsQuest != null)
					{
						qs = subClassSkilllsQuest.newQuestState(player);
					}
					else
					{
						LOGGER.warning("Null SubClassSkills quest, for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + player.getName() + "!");
						return;
					}
				}
				
				for (String varName : QUEST_VAR_NAMES)
				{
					for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
					{
						final String itemOID = player.getVariables().getString(varName + i, "");
						if (!itemOID.isEmpty() && !itemOID.endsWith(";") && !itemOID.equals("0"))
						{
							if (Util.isDigit(itemOID))
							{
								final int itemObjId = Integer.parseInt(itemOID);
								final ItemInstance item = player.getInventory().getItemByObjectId(itemObjId);
								if (item != null)
								{
									for (ItemHolder itemIdCount : s.getRequiredItems())
									{
										if (item.getId() == itemIdCount.getId())
										{
											if (checkPlayerSkill(player, trainer, s))
											{
												giveSkill(player, trainer, skill);
												// Logging the given skill.
												player.getVariables().set(varName + i, skill.getId() + ";");
											}
											return;
										}
									}
								}
								else
								{
									LOGGER.warning("Inexistent item for object Id " + itemObjId + ", for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + player.getName() + "!");
								}
							}
							else
							{
								LOGGER.warning("Invalid item object Id " + itemOID + ", for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + player.getName() + "!");
							}
						}
					}
				}
				
				// Player doesn't have required item.
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL);
				showSkillList(trainer, player);
				break;
			}
			case COLLECT:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			default:
			{
				LOGGER.warning("Recived Wrong Packet Data in Aquired Skill, unknown skill type:" + _skillType);
				break;
			}
		}
	}
	
	public static void showSubUnitSkillList(PlayerInstance player)
	{
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableSubPledgeSkills(player.getClan());
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.SUBPLEDGE);
		int count = 0;
		for (SkillLearn s : skills)
		{
			if (SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel()) != null)
			{
				asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), 0);
				++count;
			}
		}
		
		if (count == 0)
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
		}
		else
		{
			player.sendPacket(asl);
		}
	}
	
	/**
	 * Perform a simple check for current player and skill.<br>
	 * Takes the needed SP if the skill require it and all requirements are meet.<br>
	 * Consume required items if the skill require it and all requirements are meet.
	 * @param player the skill learning player.
	 * @param trainer the skills teaching Npc.
	 * @param skillLearn the skill to be learn.
	 * @return {@code true} if all requirements are meet, {@code false} otherwise.
	 */
	private boolean checkPlayerSkill(PlayerInstance player, Npc trainer, SkillLearn skillLearn)
	{
		if ((skillLearn != null) && (skillLearn.getSkillId() == _id) && (skillLearn.getSkillLevel() == _level))
		{
			// Hack check.
			if (skillLearn.getGetLevel() > player.getLevel())
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_SKILL_LEVEL_REQUIREMENTS);
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + ", level " + player.getLevel() + " is requesting skill Id: " + _id + " level " + _level + " without having minimum required level, " + skillLearn.getGetLevel() + "!", IllegalActionPunishmentType.NONE);
				return false;
			}
			
			// First it checks that the skill require SP and the player has enough SP to learn it.
			final int levelUpSp = skillLearn.getCalculatedLevelUpSp(player.getClassId(), player.getLearningClass());
			if ((levelUpSp > 0) && (levelUpSp > player.getSp()))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_LEARN_THIS_SKILL);
				showSkillList(trainer, player);
				return false;
			}
			
			if (!Config.DIVINE_SP_BOOK_NEEDED && (_id == CommonSkill.DIVINE_INSPIRATION.getId()))
			{
				return true;
			}
			
			// Check for required skills.
			if (!skillLearn.getPreReqSkills().isEmpty())
			{
				for (SkillHolder skill : skillLearn.getPreReqSkills())
				{
					if (player.getSkillLevel(skill.getSkillId()) < skill.getSkillLevel())
					{
						if (skill.getSkillId() == CommonSkill.ONYX_BEAST_TRANSFORMATION.getId())
						{
							player.sendPacket(SystemMessageId.YOU_MUST_LEARN_THE_ONYX_BEAST_SKILL_BEFORE_YOU_CAN_ACQUIRE_FURTHER_SKILLS);
						}
						else
						{
							player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL);
						}
						return false;
					}
				}
			}
			
			// Check for required items.
			if (!skillLearn.getRequiredItems().isEmpty())
			{
				// Then checks that the player has all the items
				long reqItemCount = 0;
				for (ItemHolder item : skillLearn.getRequiredItems())
				{
					reqItemCount = player.getInventory().getInventoryItemCount(item.getId(), -1);
					if (reqItemCount < item.getCount())
					{
						// Player doesn't have required item.
						player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL);
						showSkillList(trainer, player);
						return false;
					}
				}
				// If the player has all required items, they are consumed.
				for (ItemHolder itemIdCount : skillLearn.getRequiredItems())
				{
					if (!player.destroyItemByItemId("SkillLearn", itemIdCount.getId(), itemIdCount.getCount(), trainer, true))
					{
						Util.handleIllegalPlayerAction(player, "Somehow player " + player.getName() + ", level " + player.getLevel() + " lose required item Id: " + itemIdCount.getId() + " to learn skill while learning skill Id: " + _id + " level " + _level + "!", IllegalActionPunishmentType.NONE);
					}
				}
			}
			// If the player has SP and all required items then consume SP.
			if (levelUpSp > 0)
			{
				player.setSp(player.getSp() - levelUpSp);
				final StatusUpdate su = new StatusUpdate(player);
				su.addAttribute(StatusUpdate.SP, (int) player.getSp());
				player.sendPacket(su);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Add the skill to the player and makes proper updates.
	 * @param player the player acquiring a skill.
	 * @param trainer the Npc teaching a skill.
	 * @param skill the skill to be learn.
	 */
	private void giveSkill(PlayerInstance player, Npc trainer, Skill skill)
	{
		// Send message.
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_2);
		sm.addSkillName(skill);
		player.sendPacket(sm);
		
		player.sendPacket(new AcquireSkillDone());
		player.addSkill(skill, true);
		player.sendSkillList();
		
		player.updateShortCuts(_id, _level);
		showSkillList(trainer, player);
		
		// If skill is expand type then sends packet:
		if ((_id >= 1368) && (_id <= 1372))
		{
			player.sendPacket(new ExStorageMaxCount(player));
		}
		
		// Notify scripts of the skill learn.
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSkillLearn(trainer, player, skill, _skillType), trainer);
	}
	
	/**
	 * Wrapper for returning the skill list to the player after it's done with current skill.
	 * @param trainer the Npc which the {@code player} is interacting
	 * @param player the active character
	 */
	private void showSkillList(Npc trainer, PlayerInstance player)
	{
		if ((_skillType == AcquireSkillType.TRANSFORM) || (_skillType == AcquireSkillType.SUBCLASS) || (_skillType == AcquireSkillType.TRANSFER))
		{
			// Managed in Datapack.
			return;
		}
		
		if (trainer instanceof FishermanInstance)
		{
			FishermanInstance.showFishSkillList(player);
		}
		else
		{
			NpcInstance.showSkillList(player, trainer, player.getLearningClass());
		}
	}
	
	/**
	 * Verify if the player can transform.
	 * @param player the player to verify
	 * @return {@code true} if the player meets the required conditions to learn a transformation, {@code false} otherwise
	 */
	public static boolean canTransform(PlayerInstance player)
	{
		if (Config.ALLOW_TRANSFORM_WITHOUT_QUEST)
		{
			return true;
		}
		final QuestState qs = player.getQuestState("Q00136_MoreThanMeetsTheEye");
		return (qs != null) && qs.isCompleted();
	}
}
