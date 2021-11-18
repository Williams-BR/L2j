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

import java.util.Arrays;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CtrlEvent;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.PetSkillData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.MountType;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.instancemanager.AirShipManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.BabyPetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.SiegeFlagInstance;
import org.l2jmobius.gameserver.model.actor.instance.StaticObjectInstance;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skills.AbnormalType;
import org.l2jmobius.gameserver.model.skills.BuffInfo;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ChairSit;
import org.l2jmobius.gameserver.network.serverpackets.ExAskCoupleAction;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopManageList;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * This class manages the action use request packet.
 * @author Zoey76
 */
public class RequestActionUse implements IClientIncomingPacket
{
	private static final int SIN_EATER_ID = 12564;
	private static final int SWITCH_STANCE_ID = 6054;
	private static final NpcStringId[] NPC_STRINGS =
	{
		NpcStringId.USING_A_SPECIAL_SKILL_HERE_COULD_TRIGGER_A_BLOODBATH,
		NpcStringId.HEY_WHAT_DO_YOU_EXPECT_OF_ME,
		NpcStringId.UGGGGGH_PUSH_IT_S_NOT_COMING_OUT,
		NpcStringId.AH_I_MISSED_THE_MARK
	};
	
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	private GameClient _client;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_actionId = packet.readD();
		_ctrlPressed = (packet.readD() == 1);
		_shiftPressed = (packet.readC() == 1);
		_client = client;
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
		
		// Don't do anything if player is dead or confused
		if ((player.isFakeDeath() && (_actionId != 0)) || player.isDead() || player.isOutOfControl())
		{
			client.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final BuffInfo info = player.getEffectList().getBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
		if (info != null)
		{
			for (AbstractEffect effect : info.getEffects())
			{
				if (!effect.checkCondition(_actionId))
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_SO_YOUR_ACTIONS_HAVE_BEEN_RESTRICTED);
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		
		// Don't allow to do some action if player is transformed
		if (player.isTransformed())
		{
			final int[] allowedActions = player.isTransformed() ? ExBasicActionList.ACTIONS_ON_TRANSFORM : ExBasicActionList.DEFAULT_ACTION_LIST;
			if (Arrays.binarySearch(allowedActions, _actionId) < 0)
			{
				client.sendPacket(ActionFailed.STATIC_PACKET);
				LOGGER.warning("Player " + player + " used action which he does not have! Id = " + _actionId + " transform: " + player.getTransformation().getId());
				return;
			}
		}
		
		final Summon summon = player.getSummon();
		final WorldObject target = player.getTarget();
		switch (_actionId)
		{
			case 0: // Sit/Stand
			{
				if (player.isSitting() || !player.isMoving() || player.isFakeDeath())
				{
					useSit(player, target);
				}
				else
				{
					// Sit when arrive using next action.
					// Creating next action class.
					final NextAction nextAction = new NextAction(CtrlEvent.EVT_ARRIVED, CtrlIntention.AI_INTENTION_MOVE_TO, () -> useSit(player, target));
					// Binding next action to AI.
					player.getAI().setNextAction(nextAction);
				}
				break;
			}
			case 1: // Walk/Run
			{
				if (player.isRunning())
				{
					player.setWalking();
				}
				else
				{
					player.setRunning();
				}
				break;
			}
			case 10: // Private Store - Sell
			{
				player.tryOpenPrivateSellStore(false);
				break;
			}
			case 15: // Change Movement Mode (Pets)
			{
				if (validateSummon(summon, true))
				{
					((SummonAI) summon.getAI()).notifyFollowStatusChange();
				}
				break;
			}
			case 16: // Attack (Pets)
			{
				if (validateSummon(summon, true) && summon.canAttack(_ctrlPressed))
				{
					summon.doSummonAttack(target);
				}
				break;
			}
			case 17: // Stop (Pets)
			{
				if (validateSummon(summon, true))
				{
					summon.cancelAction();
				}
				break;
			}
			case 19: // Unsummon Pet
			{
				if (!validateSummon(summon, true))
				{
					break;
				}
				if (summon.isDead())
				{
					client.sendPacket(SystemMessageId.DEAD_PETS_CANNOT_BE_RETURNED_TO_THEIR_SUMMONING_ITEM);
					break;
				}
				if (summon.isAttackingNow() || summon.isInCombat() || summon.isMovementDisabled())
				{
					client.sendPacket(SystemMessageId.A_PET_CANNOT_BE_UNSUMMONED_DURING_BATTLE);
					break;
				}
				if (summon.isHungry())
				{
					if (summon.isPet() && !((PetInstance) summon).getPetData().getFood().isEmpty())
					{
						client.sendPacket(SystemMessageId.YOU_MAY_NOT_RESTORE_A_HUNGRY_PET);
					}
					else
					{
						client.sendPacket(SystemMessageId.THE_HUNTING_HELPER_PET_CANNOT_BE_RETURNED_BECAUSE_THERE_IS_NOT_MUCH_TIME_REMAINING_UNTIL_IT_LEAVES);
					}
					break;
				}
				summon.unSummon(player);
				break;
			}
			case 21: // Change Movement Mode (Servitors)
			{
				if (validateSummon(summon, false))
				{
					((SummonAI) summon.getAI()).notifyFollowStatusChange();
				}
				break;
			}
			case 22: // Attack (Servitors)
			{
				if (validateSummon(summon, false) && summon.canAttack(_ctrlPressed))
				{
					summon.doSummonAttack(target);
				}
				break;
			}
			case 23: // Stop (Servitors)
			{
				if (validateSummon(summon, false))
				{
					summon.cancelAction();
				}
				break;
			}
			case 28: // Private Store - Buy
			{
				player.tryOpenPrivateBuyStore();
				break;
			}
			case 32: // Wild Hog Cannon - Wild Cannon
			{
				useSkill("DDMagic", false);
				break;
			}
			case 36: // Soulless - Toxic Smoke
			{
				useSkill("RangeDebuff", false);
				break;
			}
			case 37: // Dwarven Manufacture
			{
				if (player.isAlikeDead() || player.isSellingBuffs())
				{
					client.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (player.getPrivateStoreType() != PrivateStoreType.NONE)
				{
					player.setPrivateStoreType(PrivateStoreType.NONE);
					player.broadcastUserInfo();
				}
				if (player.isSitting())
				{
					player.standUp();
				}
				client.sendPacket(new RecipeShopManageList(player, true));
				break;
			}
			case 38: // Mount/Dismount
			{
				player.mountPlayer(summon);
				break;
			}
			case 39: // Soulless - Parasite Burst
			{
				useSkill("RangeDD", false);
				break;
			}
			case 41: // Wild Hog Cannon - Attack
			{
				if (validateSummon(summon, false))
				{
					if ((target != null) && (target.isDoor() || (target instanceof SiegeFlagInstance)))
					{
						useSkill(4230, false);
					}
					else
					{
						client.sendPacket(SystemMessageId.INVALID_TARGET);
					}
				}
				break;
			}
			case 42: // Kai the Cat - Self Damage Shield
			{
				useSkill("HealMagic", false);
				break;
			}
			case 43: // Merrow the Unicorn - Hydro Screw
			{
				useSkill("DDMagic", false);
				break;
			}
			case 44: // Big Boom - Boom Attack
			{
				useSkill("DDMagic", false);
				break;
			}
			case 45: // Boxer the Unicorn - Master Recharge
			{
				useSkill("HealMagic", player, false);
				break;
			}
			case 46: // Mew the Cat - Mega Storm Strike
			{
				useSkill("DDMagic", false);
				break;
			}
			case 47: // Silhouette - Steal Blood
			{
				useSkill("DDMagic", false);
				break;
			}
			case 48: // Mechanic Golem - Mech. Cannon
			{
				useSkill("DDMagic", false);
				break;
			}
			case 51: // General Manufacture
			{
				// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
				if (player.isAlikeDead())
				{
					client.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (player.getPrivateStoreType() != PrivateStoreType.NONE)
				{
					player.setPrivateStoreType(PrivateStoreType.NONE);
					player.broadcastUserInfo();
				}
				if (player.isSitting())
				{
					player.standUp();
				}
				client.sendPacket(new RecipeShopManageList(player, false));
				break;
			}
			case 52: // Unsummon Servitor
			{
				if (validateSummon(summon, false))
				{
					if (summon.isAttackingNow() || summon.isInCombat())
					{
						client.sendPacket(SystemMessageId.A_SERVITOR_WHOM_IS_ENGAGED_IN_BATTLE_CANNOT_BE_DE_ACTIVATED);
						break;
					}
					summon.unSummon(player);
				}
				break;
			}
			case 53: // Move to target (Servitors)
			{
				if (validateSummon(summon, false) && (target != null) && (summon != target) && !summon.isMovementDisabled())
				{
					summon.setFollowStatus(false);
					summon.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, target.getLocation());
				}
				break;
			}
			case 54: // Move to target (Pets)
			{
				if (validateSummon(summon, true) && (target != null) && (summon != target) && !summon.isMovementDisabled())
				{
					summon.setFollowStatus(false);
					summon.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, target.getLocation());
				}
				break;
			}
			case 61: // Private Store Package Sell
			{
				player.tryOpenPrivateSellStore(true);
				break;
			}
			case 65: // Bot Report Button
			{
				if (Config.BOTREPORT_ENABLE)
				{
					BotReportTable.getInstance().reportBot(player);
				}
				else
				{
					player.sendMessage("This feature is disabled.");
				}
				break;
			}
			case 67: // Steer
			{
				if (player.isInAirShip() && player.getAirShip().setCaptain(player))
				{
					player.broadcastUserInfo();
				}
				break;
			}
			case 68: // Cancel Control
			{
				if (player.isInAirShip() && player.getAirShip().isCaptain(player) && player.getAirShip().setCaptain(null))
				{
					player.broadcastUserInfo();
				}
				break;
			}
			case 69: // Destination Map
			{
				AirShipManager.getInstance().sendAirShipTeleportList(player);
				break;
			}
			case 70: // Exit Airship
			{
				if (player.isInAirShip())
				{
					if (player.getAirShip().isCaptain(player))
					{
						if (player.getAirShip().setCaptain(null))
						{
							player.broadcastUserInfo();
						}
					}
					else if (player.getAirShip().isInDock())
					{
						player.getAirShip().oustPlayer(player);
					}
				}
				break;
			}
			case 71:
			case 72:
			case 73:
			{
				useCoupleSocial(_actionId - 55);
				break;
			}
			case 1000: // Siege Golem - Siege Hammer
			{
				if ((target != null) && target.isDoor())
				{
					useSkill(4079, false);
				}
				break;
			}
			case 1001: // Sin Eater - Ultimate Bombastic Buster
			{
				if (validateSummon(summon, true) && (summon.getId() == SIN_EATER_ID))
				{
					summon.broadcastPacket(new NpcSay(summon.getObjectId(), ChatType.NPC_GENERAL, summon.getId(), NPC_STRINGS[Rnd.get(NPC_STRINGS.length)]));
				}
				break;
			}
			case 1003: // Wind Hatchling/Strider - Wild Stun
			{
				useSkill("PhysicalSpecial", true);
				break;
			}
			case 1004: // Wind Hatchling/Strider - Wild Defense
			{
				useSkill("Buff", player, true);
				break;
			}
			case 1005: // Star Hatchling/Strider - Bright Burst
			{
				useSkill("DDMagic", true);
				break;
			}
			case 1006: // Star Hatchling/Strider - Bright Heal
			{
				useSkill("Heal", player, true);
				break;
			}
			case 1007: // Feline Queen - Blessing of Queen
			{
				useSkill("Buff1", player, false);
				break;
			}
			case 1008: // Feline Queen - Gift of Queen
			{
				useSkill("Buff2", player, false);
				break;
			}
			case 1009: // Feline Queen - Cure of Queen
			{
				useSkill("DDMagic", false);
				break;
			}
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
			{
				useSkill("Buff1", player, false);
				break;
			}
			case 1011: // Unicorn Seraphim - Gift of Seraphim
			{
				useSkill("Buff2", player, false);
				break;
			}
			case 1012: // Unicorn Seraphim - Cure of Seraphim
			{
				useSkill("DDMagic", false);
				break;
			}
			case 1013: // Nightshade - Curse of Shade
			{
				useSkill("DeBuff1", false);
				break;
			}
			case 1014: // Nightshade - Mass Curse of Shade
			{
				useSkill("DeBuff2", false);
				break;
			}
			case 1015: // Nightshade - Shade Sacrifice
			{
				useSkill("Heal", false);
				break;
			}
			case 1016: // Cursed Man - Cursed Blow
			{
				useSkill("PhysicalSpecial1", false);
				break;
			}
			case 1017: // Cursed Man - Cursed Strike
			{
				useSkill("PhysicalSpecial2", false);
				break;
			}
			case 1031: // Feline King - Slash
			{
				useSkill("PhysicalSpecial1", false);
				break;
			}
			case 1032: // Feline King - Spinning Slash
			{
				useSkill("PhysicalSpecial2", false);
				break;
			}
			case 1033: // Feline King - Hold of King
			{
				useSkill("PhysicalSpecial3", false);
				break;
			}
			case 1034: // Magnus the Unicorn - Whiplash
			{
				useSkill("PhysicalSpecial1", false);
				break;
			}
			case 1035: // Magnus the Unicorn - Tridal Wave
			{
				useSkill("PhysicalSpecial2", false);
				break;
			}
			case 1036: // Spectral Lord - Corpse Kaboom
			{
				useSkill("PhysicalSpecial1", false);
				break;
			}
			case 1037: // Spectral Lord - Dicing Death
			{
				useSkill("PhysicalSpecial2", false);
				break;
			}
			case 1038: // Spectral Lord - Dark Curse
			{
				useSkill("PhysicalSpecial3", false);
				break;
			}
			case 1039: // Swoop Cannon - Cannon Fodder
			{
				useSkill(5110, false);
				break;
			}
			case 1040: // Swoop Cannon - Big Bang
			{
				useSkill(5111, false);
				break;
			}
			case 1041: // Great Wolf - Bite Attack
			{
				useSkill("Skill01", true);
				break;
			}
			case 1042: // Great Wolf - Maul
			{
				useSkill("Skill03", true);
				break;
			}
			case 1043: // Great Wolf - Cry of the Wolf
			{
				useSkill("Skill02", true);
				break;
			}
			case 1044: // Great Wolf - Awakening
			{
				useSkill("Skill04", true);
				break;
			}
			case 1045: // Great Wolf - Howl
			{
				useSkill(5584, true);
				break;
			}
			case 1046: // Strider - Roar
			{
				useSkill(5585, true);
				break;
			}
			case 1047: // Divine Beast - Bite
			{
				useSkill(5580, false);
				break;
			}
			case 1048: // Divine Beast - Stun Attack
			{
				useSkill(5581, false);
				break;
			}
			case 1049: // Divine Beast - Fire Breath
			{
				useSkill(5582, false);
				break;
			}
			case 1050: // Divine Beast - Roar
			{
				useSkill(5583, false);
				break;
			}
			case 1051: // Feline Queen - Bless The Body
			{
				useSkill("buff3", false);
				break;
			}
			case 1052: // Feline Queen - Bless The Soul
			{
				useSkill("buff4", false);
				break;
			}
			case 1053: // Feline Queen - Haste
			{
				useSkill("buff5", false);
				break;
			}
			case 1054: // Unicorn Seraphim - Acumen
			{
				useSkill("buff3", false);
				break;
			}
			case 1055: // Unicorn Seraphim - Clarity
			{
				useSkill("buff4", false);
				break;
			}
			case 1056: // Unicorn Seraphim - Empower
			{
				useSkill("buff5", false);
				break;
			}
			case 1057: // Unicorn Seraphim - Wild Magic
			{
				useSkill("buff6", false);
				break;
			}
			case 1058: // Nightshade - Death Whisper
			{
				useSkill("buff3", false);
				break;
			}
			case 1059: // Nightshade - Focus
			{
				useSkill("buff4", false);
				break;
			}
			case 1060: // Nightshade - Guidance
			{
				useSkill("buff5", false);
				break;
			}
			case 1061: // Wild Beast Fighter, White Weasel - Death blow
			{
				useSkill(5745, true);
				break;
			}
			case 1062: // Wild Beast Fighter - Double attack
			{
				useSkill(5746, true);
				break;
			}
			case 1063: // Wild Beast Fighter - Spin attack
			{
				useSkill(5747, true);
				break;
			}
			case 1064: // Wild Beast Fighter - Meteor Shower
			{
				useSkill(5748, true);
				break;
			}
			case 1065: // Fox Shaman, Wild Beast Fighter, White Weasel, Fairy Princess - Awakening
			{
				useSkill(5753, true);
				break;
			}
			case 1066: // Fox Shaman, Spirit Shaman - Thunder Bolt
			{
				useSkill(5749, true);
				break;
			}
			case 1067: // Fox Shaman, Spirit Shaman - Flash
			{
				useSkill(5750, true);
				break;
			}
			case 1068: // Fox Shaman, Spirit Shaman - Lightning Wave
			{
				useSkill(5751, true);
				break;
			}
			case 1069: // Fox Shaman, Fairy Princess - Flare
			{
				useSkill(5752, true);
				break;
			}
			case 1070: // White Weasel, Fairy Princess, Improved Baby Buffalo, Improved Baby Kookaburra, Improved Baby Cougar, Spirit Shaman, Toy Knight, Turtle Ascetic - Buff control
			{
				useSkill(5771, true);
				break;
			}
			case 1071: // Tigress - Power Strike
			{
				useSkill("DDMagic", true);
				break;
			}
			case 1072: // Toy Knight - Piercing attack
			{
				useSkill(6046, true);
				break;
			}
			case 1073: // Toy Knight - Whirlwind
			{
				useSkill(6047, true);
				break;
			}
			case 1074: // Toy Knight - Lance Smash
			{
				useSkill(6048, true);
				break;
			}
			case 1075: // Toy Knight - Battle Cry
			{
				useSkill(6049, true);
				break;
			}
			case 1076: // Turtle Ascetic - Power Smash
			{
				useSkill(6050, true);
				break;
			}
			case 1077: // Turtle Ascetic - Energy Burst
			{
				useSkill(6051, true);
				break;
			}
			case 1078: // Turtle Ascetic - Shockwave
			{
				useSkill(6052, true);
				break;
			}
			case 1079: // Turtle Ascetic - Howl
			{
				useSkill(6053, true);
				break;
			}
			case 1080: // Phoenix Rush
			{
				useSkill(6041, false);
				break;
			}
			case 1081: // Phoenix Cleanse
			{
				useSkill(6042, false);
				break;
			}
			case 1082: // Phoenix Flame Feather
			{
				useSkill(6043, false);
				break;
			}
			case 1083: // Phoenix Flame Beak
			{
				useSkill(6044, false);
				break;
			}
			case 1084: // Switch State
			{
				if (summon instanceof BabyPetInstance)
				{
					useSkill(6054, true);
				}
				break;
			}
			case 1086: // Panther Cancel
			{
				useSkill(6094, false);
				break;
			}
			case 1087: // Panther Dark Claw
			{
				useSkill(6095, false);
				break;
			}
			case 1088: // Panther Fatal Claw
			{
				useSkill(6096, false);
				break;
			}
			case 1089: // Deinonychus - Tail Strike
			{
				useSkill(6199, true);
				break;
			}
			case 1090: // Guardian's Strider - Strider Bite
			{
				useSkill(6205, true);
				break;
			}
			case 1091: // Guardian's Strider - Strider Fear
			{
				useSkill(6206, true);
				break;
			}
			case 1092: // Guardian's Strider - Strider Dash
			{
				useSkill(6207, true);
				break;
			}
			case 1093: // Maguen - Maguen Strike
			{
				useSkill(6618, true);
				break;
			}
			case 1094: // Maguen - Maguen Wind Walk
			{
				useSkill(6681, true);
				break;
			}
			case 1095: // Elite Maguen - Maguen Power Strike
			{
				useSkill(6619, true);
				break;
			}
			case 1096: // Elite Maguen - Elite Maguen Wind Walk
			{
				useSkill(6682, true);
				break;
			}
			case 1097: // Maguen - Maguen Return
			{
				useSkill(6683, true);
				break;
			}
			case 1098: // Elite Maguen - Maguen Party Return
			{
				useSkill(6684, true);
				break;
			}
			case 5000: // Baby Rudolph - Reindeer Scratch
			{
				useSkill(23155, true);
				break;
			}
			case 5001: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Rosy Seduction
			{
				useSkill(23167, true);
				break;
			}
			case 5002: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Critical Seduction
			{
				useSkill(23168, true);
				break;
			}
			case 5003: // Hyum, Lapham, Hyum, Lapham - Thunder Bolt
			{
				useSkill(5749, true);
				break;
			}
			case 5004: // Hyum, Lapham, Hyum, Lapham - Flash
			{
				useSkill(5750, true);
				break;
			}
			case 5005: // Hyum, Lapham, Hyum, Lapham - Lightning Wave
			{
				useSkill(5751, true);
				break;
			}
			case 5006: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum, Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Buff Control
			{
				useSkill(5771, true);
				break;
			}
			case 5007: // Deseloph, Lilias, Deseloph, Lilias - Piercing Attack
			{
				useSkill(6046, true);
				break;
			}
			case 5008: // Deseloph, Lilias, Deseloph, Lilias - Spin Attack
			{
				useSkill(6047, true);
				break;
			}
			case 5009: // Deseloph, Lilias, Deseloph, Lilias - Smash
			{
				useSkill(6048, true);
				break;
			}
			case 5010: // Deseloph, Lilias, Deseloph, Lilias - Ignite
			{
				useSkill(6049, true);
				break;
			}
			case 5011: // Rekang, Mafum, Rekang, Mafum - Power Smash
			{
				useSkill(6050, true);
				break;
			}
			case 5012: // Rekang, Mafum, Rekang, Mafum - Energy Burst
			{
				useSkill(6051, true);
				break;
			}
			case 5013: // Rekang, Mafum, Rekang, Mafum - Shockwave
			{
				useSkill(6052, true);
				break;
			}
			case 5014: // Rekang, Mafum, Rekang, Mafum - Ignite
			{
				useSkill(6053, true);
				break;
			}
			case 5015: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum, Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Switch Stance
			{
				useSkill(6054, true);
				break;
			}
			// Social Packets
			case 12: // Greeting
			{
				tryBroadcastSocial(2);
				break;
			}
			case 13: // Victory
			{
				tryBroadcastSocial(3);
				break;
			}
			case 14: // Advance
			{
				tryBroadcastSocial(4);
				break;
			}
			case 24: // Yes
			{
				tryBroadcastSocial(6);
				break;
			}
			case 25: // No
			{
				tryBroadcastSocial(5);
				break;
			}
			case 26: // Bow
			{
				tryBroadcastSocial(7);
				break;
			}
			case 29: // Unaware
			{
				tryBroadcastSocial(8);
				break;
			}
			case 30: // Social Waiting
			{
				tryBroadcastSocial(9);
				break;
			}
			case 31: // Laugh
			{
				tryBroadcastSocial(10);
				break;
			}
			case 33: // Applaud
			{
				tryBroadcastSocial(11);
				break;
			}
			case 34: // Dance
			{
				tryBroadcastSocial(12);
				break;
			}
			case 35: // Sorrow
			{
				tryBroadcastSocial(13);
				break;
			}
			case 62: // Charm
			{
				tryBroadcastSocial(14);
				break;
			}
			case 66: // Shyness
			{
				tryBroadcastSocial(15);
				break;
			}
		}
	}
	
	/**
	 * Use the sit action.
	 * @param player the player trying to sit
	 * @param target the target to sit, throne, bench or chair
	 * @return {@code true} if the player can sit, {@code false} otherwise
	 */
	protected boolean useSit(PlayerInstance player, WorldObject target)
	{
		if (player.getMountType() != MountType.NONE)
		{
			return false;
		}
		
		if (!player.isSitting() && (target instanceof StaticObjectInstance) && (((StaticObjectInstance) target).getType() == 1) && player.isInsideRadius2D(target, StaticObjectInstance.INTERACTION_DISTANCE))
		{
			final ChairSit cs = new ChairSit(player, target.getId());
			_client.sendPacket(cs);
			player.sitDown();
			player.broadcastPacket(cs);
			return true;
		}
		
		if (player.isFakeDeath())
		{
			player.stopEffects(EffectType.FAKE_DEATH);
		}
		else if (player.isSitting())
		{
			player.standUp();
		}
		else
		{
			player.sitDown();
		}
		return true;
	}
	
	/**
	 * Cast a skill for active summon.<br>
	 * Target is specified as a parameter but can be overwrited or ignored depending on skill type.
	 * @param skillId the skill Id to be casted by the summon
	 * @param target the target to cast the skill on, overwritten or ignored depending on skill type
	 * @param pet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 */
	private void useSkill(int skillId, WorldObject target, boolean pet)
	{
		final PlayerInstance player = _client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Summon summon = player.getSummon();
		if (!validateSummon(summon, pet))
		{
			return;
		}
		
		if (!canControl(summon))
		{
			return;
		}
		
		int level = 0;
		if (summon.isPet())
		{
			level = PetDataTable.getInstance().getPetData(summon.getId()).getAvailableLevel(skillId, summon.getLevel());
		}
		else
		{
			level = PetSkillData.getInstance().getAvailableLevel(summon, skillId);
		}
		
		if (level > 0)
		{
			summon.setTarget(target);
			summon.useMagic(SkillData.getInstance().getSkill(skillId, level), _ctrlPressed, _shiftPressed);
		}
		
		if (skillId == SWITCH_STANCE_ID)
		{
			summon.switchMode();
		}
	}
	
	private void useSkill(String skillName, WorldObject target, boolean pet)
	{
		final PlayerInstance player = _client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Summon summon = player.getSummon();
		if (!validateSummon(summon, pet))
		{
			return;
		}
		
		if (!canControl(summon))
		{
			return;
		}
		
		if ((summon instanceof BabyPetInstance) && !((BabyPetInstance) summon).isInSupportMode())
		{
			_client.sendPacket(SystemMessageId.A_PET_ON_AUXILIARY_MODE_CANNOT_USE_SKILLS);
			return;
		}
		
		final SkillHolder skillHolder = summon.getTemplate().getParameters().getSkillHolder(skillName);
		if (skillHolder == null)
		{
			return;
		}
		
		final Skill skill = skillHolder.getSkill();
		if (skill != null)
		{
			summon.setTarget(target);
			summon.useMagic(skill, _ctrlPressed, _shiftPressed);
			if (skill.getId() == SWITCH_STANCE_ID)
			{
				summon.switchMode();
			}
		}
	}
	
	private boolean canControl(Summon summon)
	{
		if ((summon instanceof BabyPetInstance) && !((BabyPetInstance) summon).isInSupportMode())
		{
			_client.sendPacket(SystemMessageId.A_PET_ON_AUXILIARY_MODE_CANNOT_USE_SKILLS);
			return false;
		}
		
		if (summon.isPet() && ((summon.getLevel() - _client.getPlayer().getLevel()) > 20))
		{
			_client.sendPacket(SystemMessageId.YOUR_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Cast a skill for active summon.<br>
	 * Target is retrieved from owner's target, then validated by overloaded method useSkill(int, Creature).
	 * @param skillId the skill Id to use
	 * @param pet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 */
	private void useSkill(int skillId, boolean pet)
	{
		final PlayerInstance player = _client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		useSkill(skillId, player.getTarget(), pet);
	}
	
	/**
	 * Cast a skill for active summon.<br>
	 * Target is retrieved from owner's target, then validated by overloaded method useSkill(int, Creature).
	 * @param skillName the skill name to use
	 * @param pet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 */
	private void useSkill(String skillName, boolean pet)
	{
		final PlayerInstance player = _client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		useSkill(skillName, player.getTarget(), pet);
	}
	
	/**
	 * Validates the given summon and sends a system message to the master.
	 * @param summon the summon to validate
	 * @param checkPet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 * @return {@code true} if the summon is not null and whether is a pet or a servitor depending on {@code checkPet} value, {@code false} otherwise
	 */
	private boolean validateSummon(Summon summon, boolean checkPet)
	{
		if ((summon != null) && ((checkPet && summon.isPet()) || summon.isServitor()))
		{
			if (summon.isPet() && ((PetInstance) summon).isUncontrollable())
			{
				_client.sendPacket(SystemMessageId.ONLY_A_CLAN_LEADER_THAT_IS_A_NOBLESSE_CAN_VIEW_THE_SIEGE_WAR_STATUS_WINDOW_DURING_A_SIEGE_WAR);
				return false;
			}
			if (summon.isBetrayed())
			{
				_client.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
				return false;
			}
			return true;
		}
		
		if (checkPet)
		{
			_client.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
		}
		else
		{
			_client.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
		}
		return false;
	}
	
	/**
	 * Try to broadcast SocialAction packet.
	 * @param id the social action Id to broadcast
	 */
	private void tryBroadcastSocial(int id)
	{
		final PlayerInstance player = _client.getPlayer();
		if (player == null)
		{
			return;
		}
		if (player.isFishing())
		{
			_client.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
			return;
		}
		
		if (player.canMakeSocialAction())
		{
			player.broadcastPacket(new SocialAction(player.getObjectId(), id));
		}
	}
	
	/**
	 * Perform a couple social action.
	 * @param id the couple social action Id
	 */
	private void useCoupleSocial(int id)
	{
		final PlayerInstance requester = _client.getPlayer();
		if (requester == null)
		{
			return;
		}
		
		final WorldObject target = requester.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			_client.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final int distance = (int) requester.calculateDistance2D(target);
		if ((distance > 125) || (distance < 15) || (requester.getObjectId() == target.getObjectId()))
		{
			_client.sendPacket(SystemMessageId.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_TARGET_DOES_NOT_MEET_LOCATION_REQUIREMENTS);
			return;
		}
		
		SystemMessage sm;
		if (requester.isInStoreMode() || requester.isCrafting())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_PRIVATE_SHOP_MODE_OR_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
			return;
		}
		
		if (requester.isInCombat() || requester.isInDuel() || AttackStanceTaskManager.getInstance().hasAttackStanceTask(requester))
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
			return;
		}
		
		if (requester.isFishing())
		{
			_client.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
			return;
		}
		
		if (requester.getKarma() > 0)
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_CHAOTIC_STATE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
			return;
		}
		
		if (requester.isInOlympiadMode())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_PARTICIPATING_IN_THE_OLYMPIAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
			return;
		}
		
		if (requester.isInSiege())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_CASTLE_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
			return;
		}
		
		if (requester.isInHideoutSiege())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_PARTICIPATING_IN_A_HIDEOUT_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
		}
		
		if (requester.isMounted() || requester.isFlyingMounted() || requester.isInBoat() || requester.isInAirShip())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_RIDING_A_SHIP_STEED_OR_STRIDER_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
			return;
		}
		
		if (requester.isTransformed())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_TRANSFORMING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
			return;
		}
		
		if (requester.isAlikeDead())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_DEAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(requester);
			_client.sendPacket(sm);
			return;
		}
		
		// Checks for partner.
		final PlayerInstance partner = target.getActingPlayer();
		if (partner.isInStoreMode() || partner.isCrafting())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_PRIVATE_SHOP_MODE_OR_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isInCombat() || partner.isInDuel() || AttackStanceTaskManager.getInstance().hasAttackStanceTask(partner))
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.getMultiSociaAction() > 0)
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_PARTICIPATING_IN_A_COUPLE_ACTION_AND_CANNOT_BE_REQUESTED_FOR_ANOTHER_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isFishing())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_FISHING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.getKarma() > 0)
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_CHAOTIC_STATE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isInOlympiadMode())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_PARTICIPATING_IN_THE_OLYMPIAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isInHideoutSiege())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_PARTICIPATING_IN_A_HIDEOUT_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isInSiege())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_CASTLE_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isMounted() || partner.isFlyingMounted() || partner.isInBoat() || partner.isInAirShip())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_RIDING_A_SHIP_STEED_OR_STRIDER_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isTeleporting())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_TELEPORTING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isTransformed())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_TRANSFORMING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (partner.isAlikeDead())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_DEAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			_client.sendPacket(sm);
			return;
		}
		
		if (requester.isAllSkillsDisabled() || partner.isAllSkillsDisabled())
		{
			_client.sendPacket(SystemMessageId.THE_COUPLE_ACTION_WAS_CANCELLED);
			return;
		}
		
		requester.setMultiSocialAction(id, partner.getObjectId());
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_REQUESTED_A_COUPLE_ACTION_WITH_C1);
		sm.addPcName(partner);
		_client.sendPacket(sm);
		
		if ((requester.getAI().getIntention() != CtrlIntention.AI_INTENTION_IDLE) || (partner.getAI().getIntention() != CtrlIntention.AI_INTENTION_IDLE))
		{
			requester.getAI().setNextAction(new NextAction(CtrlEvent.EVT_ARRIVED, CtrlIntention.AI_INTENTION_MOVE_TO, () -> partner.sendPacket(new ExAskCoupleAction(requester.getObjectId(), id))));
			return;
		}
		
		if (requester.isCastingNow() || requester.isCastingSimultaneouslyNow())
		{
			requester.getAI().setNextAction(new NextAction(CtrlEvent.EVT_FINISH_CASTING, CtrlIntention.AI_INTENTION_CAST, () -> partner.sendPacket(new ExAskCoupleAction(requester.getObjectId(), id))));
			return;
		}
		
		partner.sendPacket(new ExAskCoupleAction(requester.getObjectId(), id));
	}
}
