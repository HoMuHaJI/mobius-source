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
package lineage2.gameserver.model.actor.instances.player;

import java.util.Collections;
import java.util.Map;

import lineage2.gameserver.dao.MentoringDAO;
import lineage2.gameserver.model.Player;
import lineage2.gameserver.model.World;
import lineage2.gameserver.network.serverpackets.ExMentorList;
import lineage2.gameserver.network.serverpackets.SystemMessage2;
import lineage2.gameserver.network.serverpackets.components.SystemMsg;
import lineage2.gameserver.utils.Mentoring;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenteeList
{
	private Map<Integer, Mentee> _menteeList = Collections.emptyMap();
	private final Player _owner;
	@SuppressWarnings("unused")
	private static final Logger _log = LoggerFactory.getLogger(MenteeList.class);
	
	public MenteeList(Player owner)
	{
		_owner = owner;
	}
	
	public void restore()
	{
		_menteeList = MentoringDAO.getInstance().selectMenteeList(_owner);
	}
	
	public void remove(String name, boolean isMentor, boolean notify)
	{
		if (StringUtils.isEmpty(name))
		{
			return;
		}
		int objectId = removeMentee0(name);
		if ((objectId > 0) && notify)
		{
			Player otherSideMentee = World.getPlayer(name);
			if (otherSideMentee != null)
			{
				otherSideMentee.sendPacket(new SystemMessage2(SystemMsg.THE_MENTORING_RELATIONSHIP_WITH_S1_HAS_BEEN_CANCELED).addString(isMentor ? name : _owner.getName()));
			}
			_owner.sendPacket(new SystemMessage2(SystemMsg.THE_MENTORING_RELATIONSHIP_WITH_S1_HAS_BEEN_CANCELED).addString(isMentor ? name : _owner.getName()));
		}
	}
	
	public void notifyUp()
	{
		for (Mentee mentee : _menteeList.values())
		{
			Player menteePlayer = World.getPlayer(mentee.getObjectId());
			if (menteePlayer != null)
			{
				Mentee thisMentee = menteePlayer.getMenteeList().getList().get(_owner.getObjectId());
				if (thisMentee == null)
				{
					continue;
				}
				thisMentee.update(_owner, true);
				menteePlayer.sendPacket(new ExMentorList(menteePlayer));
				mentee.update(menteePlayer, true);
			}
		}
	}
	
	public void notify(boolean login)
	{
		for (Mentee mentee : _menteeList.values())
		{
			Player menteePlayer = World.getPlayer(mentee.getName());
			if (menteePlayer != null)
			{
				Mentee thisMentee = menteePlayer.getMenteeList().getList().get(_owner.getObjectId());
				if (thisMentee == null)
				{
					continue;
				}
				thisMentee.update(_owner, login);
				if (login)
				{
					menteePlayer.sendPacket(new SystemMessage2(mentee.isMentor() ? SystemMsg.YOU_MENTEE_S1_HAS_CONNECTED : SystemMsg.YOU_MENTOR_S1_HAS_CONNECTED).addString(_owner.getName()));
				}
				else
				{
					menteePlayer.sendPacket(new SystemMessage2(mentee.isMentor() ? SystemMsg.YOU_MENTEE_S1_HAS_DISCONNECTED : SystemMsg.YOU_MENTOR_S1_HAS_DISCONNECTED).addString(_owner.getName()));
				}
				mentee.update(menteePlayer, login);
				menteePlayer.sendPacket(new ExMentorList(menteePlayer));
			}
		}
	}
	
	public void addMentee(Player menteePlayer)
	{
		_menteeList.put(menteePlayer.getObjectId(), new Mentee(menteePlayer));
		MentoringDAO.getInstance().insert(_owner, menteePlayer);
	}
	
	public void addMentor(Player mentorPlayer)
	{
		_menteeList.put(mentorPlayer.getObjectId(), new Mentee(mentorPlayer, true));
		Mentoring.addMentoringSkills(mentorPlayer);
	}
	
	private int removeMentee0(String name)
	{
		if (name == null)
		{
			return 0;
		}
		Integer objectId = 0;
		for (Map.Entry<Integer, Mentee> entry : _menteeList.entrySet())
		{
			if (name.equalsIgnoreCase(entry.getValue().getName()))
			{
				objectId = entry.getKey();
				break;
			}
		}
		if (objectId > 0)
		{
			_menteeList.remove(objectId);
			MentoringDAO.getInstance().delete(_owner.getObjectId(), objectId);
			return objectId;
		}
		return 0;
	}
	
	public boolean someOneOnline(boolean login)
	{
		for (Mentee mentee : _menteeList.values())
		{
			Player menteePlayer = World.getPlayer(mentee.getName());
			if (menteePlayer != null)
			{
				Mentee thisMentee = menteePlayer.getMenteeList().getList().get(_owner.getObjectId());
				if (thisMentee == null)
				{
					continue;
				}
				thisMentee.update(_owner, login);
				if (menteePlayer.isOnline())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public int getMentor()
	{
		for (Map.Entry<Integer, Mentee> entry : _menteeList.entrySet())
		{
			if (entry.getValue().isMentor())
			{
				return entry.getValue().getObjectId();
			}
		}
		return 0;
	}
	
	public Map<Integer, Mentee> getList()
	{
		return _menteeList;
	}
	
	@Override
	public String toString()
	{
		return "MenteeList[owner=" + _owner.getName() + "]";
	}
}