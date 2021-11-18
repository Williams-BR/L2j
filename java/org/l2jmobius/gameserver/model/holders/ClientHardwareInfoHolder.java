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
package org.l2jmobius.gameserver.model.holders;

import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.variables.AccountVariables;

/**
 * @author Mobius
 */
public class ClientHardwareInfoHolder
{
	private final String _macAddress;
	private final int _windowsPlatformId;
	private final int _windowsMajorVersion;
	private final int _windowsMinorVersion;
	private final int _windowsBuildNumber;
	private final int _directxVersion;
	private final int _directxRevision;
	private final String _cpuName;
	private final int _cpuSpeed;
	private final int _cpuCoreCount;
	private final int _vgaCount;
	private final int _vgaPcxSpeed;
	private final int _physMemorySlot1;
	private final int _physMemorySlot2;
	private final int _physMemorySlot3;
	private final int _videoMemory;
	private final int _vgaVersion;
	private final String _vgaName;
	private final String _vgaDriverVersion;
	
	public ClientHardwareInfoHolder(String macAddress, int windowsPlatformId, int windowsMajorVersion, int windowsMinorVersion, int windowsBuildNumber, int directxVersion, int directxRevision, String cpuName, int cpuSpeed, int cpuCoreCount, int vgaCount, int vgaPcxSpeed, int physMemorySlot1, int physMemorySlot2, int physMemorySlot3, int videoMemory, int vgaVersion, String vgaName, String vgaDriverVersion)
	{
		_macAddress = macAddress;
		_windowsPlatformId = windowsPlatformId;
		_windowsMajorVersion = windowsMajorVersion;
		_windowsMinorVersion = windowsMinorVersion;
		_windowsBuildNumber = windowsBuildNumber;
		_directxVersion = directxVersion;
		_directxRevision = directxRevision;
		_cpuName = cpuName;
		_cpuSpeed = cpuSpeed;
		_cpuCoreCount = cpuCoreCount;
		_vgaCount = vgaCount;
		_vgaPcxSpeed = vgaPcxSpeed;
		_physMemorySlot1 = physMemorySlot1;
		_physMemorySlot2 = physMemorySlot2;
		_physMemorySlot3 = physMemorySlot3;
		_videoMemory = videoMemory;
		_vgaVersion = vgaVersion;
		_vgaName = vgaName;
		_vgaDriverVersion = vgaDriverVersion;
	}
	
	public ClientHardwareInfoHolder(String info)
	{
		final String[] split = info.split(AccountVariables.HWIDSLIT_VAR);
		_macAddress = split[0];
		_windowsPlatformId = Integer.valueOf(split[1]);
		_windowsMajorVersion = Integer.valueOf(split[2]);
		_windowsMinorVersion = Integer.valueOf(split[3]);
		_windowsBuildNumber = Integer.valueOf(split[4]);
		_directxVersion = Integer.valueOf(split[5]);
		_directxRevision = Integer.valueOf(split[6]);
		_cpuName = split[7];
		_cpuSpeed = Integer.valueOf(split[8]);
		_cpuCoreCount = Integer.valueOf(split[9]);
		_vgaCount = Integer.valueOf(split[10]);
		_vgaPcxSpeed = Integer.valueOf(split[11]);
		_physMemorySlot1 = Integer.valueOf(split[12]);
		_physMemorySlot2 = Integer.valueOf(split[13]);
		_physMemorySlot3 = Integer.valueOf(split[14]);
		_videoMemory = Integer.valueOf(split[15]);
		_vgaVersion = Integer.valueOf(split[16]);
		_vgaName = split[17];
		_vgaDriverVersion = split[18];
	}
	
	/**
	 * Save hardware info to account variables for later use.
	 * @param player The PlayerInstance related with this hardware info.
	 */
	public void store(PlayerInstance player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(_macAddress);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsPlatformId);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsMajorVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsMinorVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsBuildNumber);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_directxVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_directxRevision);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuName);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuSpeed);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuCoreCount);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaCount);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaPcxSpeed);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot1);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot2);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot3);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_videoMemory);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaName);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaDriverVersion);
		player.getAccountVariables().set(AccountVariables.HWID, sb.toString());
	}
	
	/**
	 * @return the macAddress
	 */
	public String getMacAddress()
	{
		return _macAddress;
	}
	
	/**
	 * @return the windowsPlatformId
	 */
	public int getWindowsPlatformId()
	{
		return _windowsPlatformId;
	}
	
	/**
	 * @return the windowsMajorVersion
	 */
	public int getWindowsMajorVersion()
	{
		return _windowsMajorVersion;
	}
	
	/**
	 * @return the windowsMinorVersion
	 */
	public int getWindowsMinorVersion()
	{
		return _windowsMinorVersion;
	}
	
	/**
	 * @return the windowsBuildNumber
	 */
	public int getWindowsBuildNumber()
	{
		return _windowsBuildNumber;
	}
	
	/**
	 * @return the directxVersion
	 */
	public int getDirectxVersion()
	{
		return _directxVersion;
	}
	
	/**
	 * @return the directxRevision
	 */
	public int getDirectxRevision()
	{
		return _directxRevision;
	}
	
	/**
	 * @return the cpuName
	 */
	public String getCpuName()
	{
		return _cpuName;
	}
	
	/**
	 * @return the cpuSpeed
	 */
	public int getCpuSpeed()
	{
		return _cpuSpeed;
	}
	
	/**
	 * @return the cpuCoreCount
	 */
	public int getCpuCoreCount()
	{
		return _cpuCoreCount;
	}
	
	/**
	 * @return the vgaCount
	 */
	public int getVgaCount()
	{
		return _vgaCount;
	}
	
	/**
	 * @return the vgaPcxSpeed
	 */
	public int getVgaPcxSpeed()
	{
		return _vgaPcxSpeed;
	}
	
	/**
	 * @return the physMemorySlot1
	 */
	public int getPhysMemorySlot1()
	{
		return _physMemorySlot1;
	}
	
	/**
	 * @return the physMemorySlot2
	 */
	public int getPhysMemorySlot2()
	{
		return _physMemorySlot2;
	}
	
	/**
	 * @return the physMemorySlot3
	 */
	public int getPhysMemorySlot3()
	{
		return _physMemorySlot3;
	}
	
	/**
	 * @return the videoMemory
	 */
	public int getVideoMemory()
	{
		return _videoMemory;
	}
	
	/**
	 * @return the vgaVersion
	 */
	public int getVgaVersion()
	{
		return _vgaVersion;
	}
	
	/**
	 * @return the vgaName
	 */
	public String getVgaName()
	{
		return _vgaName;
	}
	
	/**
	 * @return the vgaDriverVersion
	 */
	public String getVgaDriverVersion()
	{
		return _vgaDriverVersion;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ClientHardwareInfoHolder)
		{
			final ClientHardwareInfoHolder info = (ClientHardwareInfoHolder) obj;
			if ((_macAddress.equals(info.getMacAddress())) && //
				(_windowsPlatformId == info.getWindowsPlatformId()) && //
				(_windowsMajorVersion == info.getWindowsMajorVersion()) && //
				(_windowsMinorVersion == info.getWindowsMinorVersion()) && //
				(_windowsBuildNumber == info.getWindowsBuildNumber()) && //
				(_directxVersion == info.getDirectxVersion()) && //
				(_directxRevision == info.getDirectxRevision()) && //
				(_cpuName.equals(info.getCpuName())) && //
				(_cpuSpeed == info.getCpuSpeed()) && //
				(_cpuCoreCount == info.getCpuCoreCount()) && //
				(_vgaCount == info.getVgaCount()) && //
				(_vgaPcxSpeed == info.getVgaPcxSpeed()) && //
				(_physMemorySlot1 == info.getPhysMemorySlot1()) && //
				(_physMemorySlot2 == info.getPhysMemorySlot2()) && //
				(_physMemorySlot3 == info.getPhysMemorySlot3()) && //
				(_videoMemory == info.getVideoMemory()) && //
				(_vgaVersion == info.getVgaVersion()) && //
				(_vgaName.equals(info.getVgaName())) && //
				(_vgaDriverVersion.equals(info.getVgaDriverVersion())))
			{
				return true;
			}
		}
		return false;
	}
}