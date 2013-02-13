/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package lineage2.gameserver.network.serverpackets;

import lineage2.gameserver.model.Player;
import lineage2.gameserver.utils.Location;

/**
 * @author Mobius
 * @version $Revision: 1.0 $
 */
public class ExStopMoveInShuttlePacket extends L2GameServerPacket
{
	/**
	 * Field _playerHeading. Field _shuttleId. Field _playerObjectId.
	 */
	private final int _playerObjectId, _shuttleId, _playerHeading;
	/**
	 * Field _loc.
	 */
	private final Location _loc;
	
	/**
	 * Constructor for ExStopMoveInShuttlePacket.
	 * @param cha Player
	 */
	public ExStopMoveInShuttlePacket(Player cha)
	{
		_playerObjectId = cha.getObjectId();
		_shuttleId = cha.getBoat().getBoatId();
		_loc = cha.getInBoatPosition();
		_playerHeading = cha.getHeading();
	}
	
	/**
	 * Method writeImpl.
	 */
	@Override
	protected final void writeImpl()
	{
		writeEx(0xCF);
		writeD(_playerObjectId);
		writeD(_shuttleId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_playerHeading);
	}
}