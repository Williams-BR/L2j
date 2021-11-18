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
package org.l2jmobius.gameserver.model;

import java.util.Objects;

import org.l2jmobius.commons.util.Point2D;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.interfaces.IPositionable;

/**
 * A datatype used to retain a 3D (x/y/z/heading/instanceId) point. It got the capability to be set and cleaned.
 */
public class Location extends Point2D implements IPositionable
{
	protected volatile int _z;
	protected volatile int _heading;
	protected volatile int _instanceId;
	
	public Location(int x, int y, int z)
	{
		super(x, y);
		_z = z;
		_heading = 0;
		_instanceId = 0;
	}
	
	public Location(int x, int y, int z, int heading)
	{
		super(x, y);
		_z = z;
		_heading = heading;
		_instanceId = 0;
	}
	
	public Location(WorldObject obj)
	{
		this(obj.getX(), obj.getY(), obj.getZ(), obj.getHeading(), obj.getInstanceId());
	}
	
	public Location(int x, int y, int z, int heading, int instanceId)
	{
		super(x, y);
		_z = z;
		_heading = heading;
		_instanceId = instanceId;
	}
	
	public Location(StatSet set)
	{
		super(set.getInt("x", 0), set.getInt("y", 0));
		_z = set.getInt("z", 0);
		_heading = set.getInt("heading", 0);
		_instanceId = set.getInt("instanceId", 0);
	}
	
	/**
	 * Get the x coordinate.
	 * @return the x coordinate
	 */
	@Override
	public int getX()
	{
		return _x;
	}
	
	/**
	 * Get the y coordinate.
	 * @return the y coordinate
	 */
	@Override
	public int getY()
	{
		return _y;
	}
	
	/**
	 * Get the z coordinate.
	 * @return the z coordinate
	 */
	@Override
	public int getZ()
	{
		return _z;
	}
	
	/**
	 * Set the x, y, z coordinates.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 */
	@Override
	public void setXYZ(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	/**
	 * Set the x, y, z coordinates.
	 * @param loc The location.
	 */
	@Override
	public void setXYZ(ILocational loc)
	{
		setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Get the heading.
	 * @return the heading
	 */
	@Override
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * Set the heading.
	 * @param heading the heading
	 */
	@Override
	public void setHeading(int heading)
	{
		_heading = heading;
	}
	
	/**
	 * Get the instance Id.
	 * @return the instance Id
	 */
	@Override
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	/**
	 * Set the instance Id.
	 * @param instanceId the instance Id to set
	 */
	@Override
	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}
	
	@Override
	public IPositionable getLocation()
	{
		return this;
	}
	
	@Override
	public void setLocation(Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
		_heading = loc.getHeading();
		_instanceId = loc.getInstanceId();
	}
	
	@Override
	public void clean()
	{
		super.clean();
		_z = 0;
		_instanceId = 0;
	}
	
	@Override
	public Location clone()
	{
		return new Location(_x, _y, _z);
	}
	
	@Override
	public int hashCode()
	{
		return (31 * super.hashCode()) + Objects.hash(_z);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Location)
		{
			final Location loc = (Location) obj;
			return (getX() == loc.getX()) && (getY() == loc.getY()) && (getZ() == loc.getZ()) && (getHeading() == loc.getHeading()) && (getInstanceId() == loc.getInstanceId());
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] X: " + _x + " Y: " + _y + " Z: " + _z + " Heading: " + _heading + " InstanceId: " + _instanceId;
	}
}
