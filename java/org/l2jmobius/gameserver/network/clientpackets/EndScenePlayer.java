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

import org.l2jmobius.commons.network.PacketReader;
import org.l2jmobius.gameserver.enums.Movie;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.MovieHolder;
import org.l2jmobius.gameserver.network.GameClient;

/**
 * @author JIV
 */
public class EndScenePlayer implements IClientIncomingPacket
{
	private int _movieId;
	
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		_movieId = packet.readD();
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if ((player == null) || (_movieId == 0))
		{
			return;
		}
		final MovieHolder movieHolder = player.getMovieHolder();
		if (movieHolder == null)
		{
			return;
		}
		final Movie movie = movieHolder.getMovie();
		if (movie.getClientId() != _movieId)
		{
			return;
		}
		player.stopMovie();
		player.setTeleporting(true, false); // avoid to get player removed from World
		player.decayMe();
		player.spawnMe(player.getX(), player.getY(), player.getZ());
		player.setTeleporting(false, false);
	}
}
