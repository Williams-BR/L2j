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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.file.filter.XMLFilter;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.util.DocumentSkill;

/**
 * Skill data.
 */
public class SkillData
{
	private static final Logger LOGGER = Logger.getLogger(SkillData.class.getName());
	
	private final Map<Integer, Skill> _skills = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _skillMaxLevel = new ConcurrentHashMap<>();
	private final Set<Integer> _enchantable = ConcurrentHashMap.newKeySet();
	private final List<File> _skillFiles = new ArrayList<>();
	private static int count = 0;
	
	protected SkillData()
	{
		processDirectory("data/stats/skills", _skillFiles);
		if (Config.CUSTOM_SKILLS_LOAD)
		{
			processDirectory("data/stats/skills/custom", _skillFiles);
		}
		
		load();
	}
	
	private void processDirectory(String dirName, List<File> list)
	{
		final File dir = new File(Config.DATAPACK_ROOT, dirName);
		if (!dir.exists())
		{
			LOGGER.warning("Dir " + dir.getAbsolutePath() + " does not exist.");
			return;
		}
		final File[] files = dir.listFiles(new XMLFilter());
		for (File file : files)
		{
			list.add(file);
		}
	}
	
	public List<Skill> loadSkills(File file)
	{
		if (file == null)
		{
			LOGGER.warning("Skill file not found.");
			return null;
		}
		final DocumentSkill doc = new DocumentSkill(file);
		doc.parse();
		return doc.getSkills();
	}
	
	public void loadAllSkills(Map<Integer, Skill> allSkills)
	{
		if (Config.THREADS_FOR_LOADING)
		{
			final Collection<ScheduledFuture<?>> jobs = ConcurrentHashMap.newKeySet();
			for (File file : _skillFiles)
			{
				jobs.add(ThreadPool.schedule(() ->
				{
					final List<Skill> skills = loadSkills(file);
					if (skills == null)
					{
						return;
					}
					for (Skill skill : skills)
					{
						allSkills.put(SkillData.getSkillHashCode(skill), skill);
						count++;
					}
				}, 0));
			}
			while (!jobs.isEmpty())
			{
				for (ScheduledFuture<?> job : jobs)
				{
					if ((job == null) || job.isDone() || job.isCancelled())
					{
						jobs.remove(job);
					}
				}
			}
		}
		else
		{
			for (File file : _skillFiles)
			{
				final List<Skill> skills = loadSkills(file);
				if (skills == null)
				{
					return;
				}
				for (Skill skill : skills)
				{
					allSkills.put(SkillData.getSkillHashCode(skill), skill);
					count++;
				}
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + count + " Skill templates from XML files.");
	}
	
	private void load()
	{
		final Map<Integer, Skill> temp = new ConcurrentHashMap<>();
		loadAllSkills(temp);
		
		_skills.clear();
		_skills.putAll(temp);
		
		_skillMaxLevel.clear();
		_enchantable.clear();
		for (Skill skill : _skills.values())
		{
			final int skillId = skill.getId();
			final int skillLevel = skill.getLevel();
			if (skillLevel > 99)
			{
				if (!_enchantable.contains(skillId))
				{
					_enchantable.add(skillId);
				}
				continue;
			}
			
			// only non-enchanted skills
			final int maxLevel = getMaxLevel(skillId);
			if (skillLevel > maxLevel)
			{
				_skillMaxLevel.put(skillId, skillLevel);
			}
		}
	}
	
	public void reload()
	{
		load();
		// Reload Skill Tree as well.
		SkillTreeData.getInstance().load();
	}
	
	/**
	 * Provides the skill hash
	 * @param skill The Skill to be hashed
	 * @return getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}
	
	/**
	 * Centralized method for easier change of the hashing sys
	 * @param skillId The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return (skillId * 1021) + skillLevel;
	}
	
	public Skill getSkill(int skillId, int level)
	{
		final Skill result = _skills.get(getSkillHashCode(skillId, level));
		if (result != null)
		{
			return result;
		}
		
		// skill/level not found, fix for transformation scripts
		final int maxLevel = getMaxLevel(skillId);
		// requested level too high
		if ((maxLevel > 0) && (level > maxLevel))
		{
			return _skills.get(getSkillHashCode(skillId, maxLevel));
		}
		
		LOGGER.warning(getClass().getSimpleName() + ": No skill info found for skill id " + skillId + " and skill level " + level + ".");
		return null;
	}
	
	public int getMaxLevel(int skillId)
	{
		final Integer maxLevel = _skillMaxLevel.get(skillId);
		return maxLevel != null ? maxLevel : 0;
	}
	
	/**
	 * Verifies if the given skill ID correspond to an enchantable skill.
	 * @param skillId the skill ID
	 * @return {@code true} if the skill is enchantable, {@code false} otherwise
	 */
	public boolean isEnchantable(int skillId)
	{
		return _enchantable.contains(skillId);
	}
	
	/**
	 * @param addNoble
	 * @param hasCastle
	 * @return an array with siege skills. If addNoble == true, will add also Advanced headquarters.
	 */
	public Skill[] getSiegeSkills(boolean addNoble, boolean hasCastle)
	{
		final Skill[] temp = new Skill[2 + (addNoble ? 1 : 0) + (hasCastle ? 2 : 0)];
		int i = 0;
		temp[i++] = _skills.get(getSkillHashCode(246, 1));
		temp[i++] = _skills.get(getSkillHashCode(247, 1));
		if (addNoble)
		{
			temp[i++] = _skills.get(getSkillHashCode(326, 1));
		}
		if (hasCastle)
		{
			temp[i++] = _skills.get(getSkillHashCode(844, 1));
			temp[i++] = _skills.get(getSkillHashCode(845, 1));
		}
		return temp;
	}
	
	public static SkillData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillData INSTANCE = new SkillData();
	}
}