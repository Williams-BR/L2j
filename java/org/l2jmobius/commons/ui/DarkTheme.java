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
package org.l2jmobius.commons.ui;

import java.awt.Color;

import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * @author Mobius
 */
public class DarkTheme
{
	public static void activate()
	{
		// Modify existing white Nimbus look and feel to dark.
		UIManager.put("control", new Color(128, 128, 128));
		UIManager.put("info", new Color(128, 128, 128));
		UIManager.put("nimbusBase", Color.DARK_GRAY); // new Color(18, 30, 49)
		UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
		UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
		UIManager.put("nimbusFocus", Color.DARK_GRAY); // new Color(115, 164, 209)
		UIManager.put("nimbusGreen", new Color(176, 179, 50));
		UIManager.put("nimbusInfoBlue", Color.DARK_GRAY); // new Color(66, 139, 221)
		UIManager.put("nimbusLightBackground", Color.DARK_GRAY); // new Color(18, 30, 49)
		UIManager.put("nimbusOrange", new Color(191, 98, 4));
		UIManager.put("nimbusRed", new Color(169, 46, 34));
		UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
		UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
		UIManager.put("text", new Color(230, 230, 230));
		
		// Set look and feel.
		try
		{
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
