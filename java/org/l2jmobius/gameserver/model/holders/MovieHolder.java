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

import java.util.Collection;

import org.l2jmobius.gameserver.enums.Movie;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author St3eT
 */
public class MovieHolder
{
	private final Movie _movie;
	private final Collection<PlayerInstance> _players;
	
	public MovieHolder(Collection<PlayerInstance> players, Movie movie)
	{
		_players = players;
		_movie = movie;
		_players.forEach(p -> p.playMovie(this));
	}
	
	public Movie getMovie()
	{
		return _movie;
	}
	
	public Collection<PlayerInstance> getPlayers()
	{
		return _players;
	}
}