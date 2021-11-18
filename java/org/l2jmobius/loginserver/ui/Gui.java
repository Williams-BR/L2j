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
package org.l2jmobius.loginserver.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.l2jmobius.Config;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.LimitLinesDocumentListener;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.LoginController;
import org.l2jmobius.loginserver.LoginServer;
import org.l2jmobius.loginserver.network.gameserverpackets.ServerStatus;

/**
 * @author Mobius
 */
public class Gui
{
	JTextArea txtrConsole;
	
	JCheckBoxMenuItem chckbxmntmEnabled;
	JCheckBoxMenuItem chckbxmntmDisabled;
	JCheckBoxMenuItem chckbxmntmGmOnly;
	
	static final String[] shutdownOptions =
	{
		"Shutdown",
		"Cancel"
	};
	static final String[] restartOptions =
	{
		"Restart",
		"Cancel"
	};
	
	public Gui()
	{
		if (Config.DARK_THEME)
		{
			DarkTheme.activate();
		}
		
		// Initialize console.
		txtrConsole = new JTextArea();
		txtrConsole.setEditable(false);
		txtrConsole.setLineWrap(true);
		txtrConsole.setWrapStyleWord(true);
		txtrConsole.setDropMode(DropMode.INSERT);
		txtrConsole.setFont(new Font("Monospaced", Font.PLAIN, 16));
		txtrConsole.getDocument().addDocumentListener(new LimitLinesDocumentListener(500));
		
		// Initialize menu items.
		final JMenuBar menuBar = new JMenuBar();
		menuBar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		
		final JMenu mnActions = new JMenu("Actions");
		mnActions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnActions);
		
		final JMenuItem mntmShutdown = new JMenuItem("Shutdown");
		mntmShutdown.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmShutdown.addActionListener(arg0 ->
		{
			if (JOptionPane.showOptionDialog(null, "Shutdown LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, shutdownOptions, shutdownOptions[1]) == 0)
			{
				LoginServer.getInstance().shutdown(false);
			}
		});
		mnActions.add(mntmShutdown);
		
		final JMenuItem mntmRestart = new JMenuItem("Restart");
		mntmRestart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmRestart.addActionListener(arg0 ->
		{
			if (JOptionPane.showOptionDialog(null, "Restart LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, restartOptions, restartOptions[1]) == 0)
			{
				LoginServer.getInstance().shutdown(true);
			}
		});
		mnActions.add(mntmRestart);
		
		final JMenu mnReload = new JMenu("Reload");
		mnReload.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnReload);
		
		final JMenuItem mntmBannedIps = new JMenuItem("Banned IPs");
		mntmBannedIps.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmBannedIps.addActionListener(arg0 ->
		{
			LoginController.getInstance().getBannedIps().clear();
			LoginServer.getInstance().loadBanFile();
		});
		mnReload.add(mntmBannedIps);
		
		final JMenu mnStatus = new JMenu("Status");
		mnStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnStatus);
		
		chckbxmntmEnabled = new JCheckBoxMenuItem("Enabled");
		chckbxmntmEnabled.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		chckbxmntmEnabled.addActionListener(arg0 ->
		{
			chckbxmntmEnabled.setSelected(true);
			chckbxmntmDisabled.setSelected(false);
			chckbxmntmGmOnly.setSelected(false);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_NORMAL);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_NORMAL);
			}
			LoginServer.getInstance().LOGGER.info("Status changed to enabled.");
		});
		chckbxmntmEnabled.setSelected(true);
		mnStatus.add(chckbxmntmEnabled);
		
		chckbxmntmDisabled = new JCheckBoxMenuItem("Disabled");
		chckbxmntmDisabled.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		chckbxmntmDisabled.addActionListener(arg0 ->
		{
			chckbxmntmEnabled.setSelected(false);
			chckbxmntmDisabled.setSelected(true);
			chckbxmntmGmOnly.setSelected(false);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_DOWN);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_DOWN);
			}
			LoginServer.getInstance().LOGGER.info("Status changed to disabled.");
		});
		mnStatus.add(chckbxmntmDisabled);
		
		chckbxmntmGmOnly = new JCheckBoxMenuItem("GM only");
		chckbxmntmGmOnly.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		chckbxmntmGmOnly.addActionListener(arg0 ->
		{
			chckbxmntmEnabled.setSelected(false);
			chckbxmntmDisabled.setSelected(false);
			chckbxmntmGmOnly.setSelected(true);
			LoginServer.getInstance().setStatus(ServerStatus.STATUS_GM_ONLY);
			for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
			{
				gsi.setStatus(ServerStatus.STATUS_GM_ONLY);
			}
			LoginServer.getInstance().LOGGER.info("Status changed to GM only.");
		});
		mnStatus.add(chckbxmntmGmOnly);
		
		final JMenu mnFont = new JMenu("Font");
		mnFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnFont);
		
		final String[] fonts =
		{
			"16",
			"21",
			"27",
			"33"
		};
		for (String font : fonts)
		{
			final JMenuItem mntmFont = new JMenuItem(font);
			mntmFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
			mntmFont.addActionListener(arg0 -> txtrConsole.setFont(new Font("Monospaced", Font.PLAIN, Integer.parseInt(font))));
			mnFont.add(mntmFont);
		}
		
		final JMenu mnHelp = new JMenu("Help");
		mnHelp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnHelp);
		
		final JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAbout.addActionListener(arg0 -> new frmAbout());
		mnHelp.add(mntmAbout);
		
		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
		
		final JScrollPane scrollPanel = new JScrollPane(txtrConsole);
		scrollPanel.setBounds(0, 0, 800, 550);
		
		// Set frame.
		final JFrame frame = new JFrame("Mobius - LoginServer");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent ev)
			{
				if (JOptionPane.showOptionDialog(null, "Shutdown LoginServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, shutdownOptions, shutdownOptions[1]) == 0)
				{
					LoginServer.getInstance().shutdown(false);
				}
			}
		});
		frame.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent ev)
			{
				scrollPanel.setSize(frame.getContentPane().getSize());
			}
		});
		frame.setJMenuBar(menuBar);
		frame.setIconImages(icons);
		frame.add(scrollPanel, BorderLayout.CENTER);
		frame.getContentPane().setPreferredSize(new Dimension(Config.DARK_THEME ? 815 : 800, 550));
		frame.pack();
		frame.setLocationRelativeTo(null);
		
		// Redirect output to text area.
		redirectSystemStreams();
		
		// Show SplashScreen.
		new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 5000, frame);
	}
	
	// Set where the text is redirected. In this case, txtrConsole.
	void updateTextArea(String text)
	{
		SwingUtilities.invokeLater(() ->
		{
			txtrConsole.append(text);
			txtrConsole.setCaretPosition(txtrConsole.getText().length());
		});
	}
	
	// Method that manages the redirect.
	private void redirectSystemStreams()
	{
		final OutputStream out = new OutputStream()
		{
			@Override
			public void write(int b)
			{
				updateTextArea(String.valueOf((char) b));
			}
			
			@Override
			public void write(byte[] b, int off, int len)
			{
				updateTextArea(new String(b, off, len));
			}
			
			@Override
			public void write(byte[] b)
			{
				write(b, 0, b.length);
			}
		};
		
		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}
}
