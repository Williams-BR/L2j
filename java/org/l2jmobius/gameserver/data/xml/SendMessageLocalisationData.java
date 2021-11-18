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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author Mobius
 */
public class SendMessageLocalisationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SendMessageLocalisationData.class.getName());
	
	private static final String SPLIT_STRING = "XXX";
	private static final Map<String, Map<String[], String[]>> SEND_MESSAGE_LOCALISATIONS = new ConcurrentHashMap<>();
	private static String _lang;
	
	protected SendMessageLocalisationData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		SEND_MESSAGE_LOCALISATIONS.clear();
		
		if (Config.MULTILANG_ENABLE)
		{
			for (String lang : Config.MULTILANG_ALLOWED)
			{
				final File file = new File("data/lang/" + lang + "/SendMessageLocalisation.xml");
				if (!file.isFile())
				{
					continue;
				}
				
				SEND_MESSAGE_LOCALISATIONS.put(lang, new ConcurrentHashMap<String[], String[]>());
				_lang = lang;
				parseDatapackFile("data/lang/" + lang + "/SendMessageLocalisation.xml");
				final int count = SEND_MESSAGE_LOCALISATIONS.get(lang).values().size();
				if (count == 0)
				{
					SEND_MESSAGE_LOCALISATIONS.remove(lang);
				}
				else
				{
					LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Loaded localisations for [" + lang + "].");
				}
			}
		}
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "localisation", localisationNode ->
		{
			final StatSet set = new StatSet(parseAttributes(localisationNode));
			SEND_MESSAGE_LOCALISATIONS.get(_lang).put(set.getString("message").split(SPLIT_STRING), set.getString("translation").split(SPLIT_STRING));
		}));
	}
	
	public static String getLocalisation(PlayerInstance player, String message)
	{
		if (Config.MULTILANG_ENABLE)
		{
			final Map<String[], String[]> localisations = SEND_MESSAGE_LOCALISATIONS.get(player.getLang());
			if (localisations != null)
			{
				// No pretty way of doing something like this.
				// Consider using proper SystemMessages where possible.
				String[] searchMessage;
				String[] replacementMessage;
				String localisation = message;
				boolean found;
				for (Entry<String[], String[]> entry : localisations.entrySet())
				{
					searchMessage = entry.getKey();
					replacementMessage = entry.getValue();
					
					// Exact match.
					if (searchMessage.length == 1)
					{
						if (searchMessage[0].equals(localisation))
						{
							return replacementMessage[0];
						}
					}
					else // Split match.
					{
						found = true;
						for (String part : searchMessage)
						{
							if (!localisation.contains(part))
							{
								found = false;
								break;
							}
						}
						if (found)
						{
							for (int i = 0; i < searchMessage.length; i++)
							{
								localisation = localisation.replace(searchMessage[i], replacementMessage[i]);
							}
							break;
						}
					}
				}
				return localisation;
			}
		}
		return message;
	}
	
	public static SendMessageLocalisationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SendMessageLocalisationData INSTANCE = new SendMessageLocalisationData();
	}
}
