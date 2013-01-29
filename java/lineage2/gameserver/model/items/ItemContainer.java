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
package lineage2.gameserver.model.items;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lineage2.commons.math.SafeMath;
import lineage2.gameserver.dao.ItemsDAO;
import lineage2.gameserver.idfactory.IdFactory;
import lineage2.gameserver.utils.ItemFunctions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ItemContainer
{
	@SuppressWarnings("unused")
	private static final Logger _log = LoggerFactory.getLogger(ItemContainer.class);
	protected static final ItemsDAO _itemsDAO = ItemsDAO.getInstance();
	protected final List<ItemInstance> _items = new ArrayList<>();
	protected final ReadWriteLock lock = new ReentrantReadWriteLock();
	protected final Lock readLock = lock.readLock();
	protected final Lock writeLock = lock.writeLock();
	
	protected ItemContainer()
	{
	}
	
	public int getSize()
	{
		return _items.size();
	}
	
	public ItemInstance[] getItems()
	{
		readLock();
		try
		{
			return _items.toArray(new ItemInstance[_items.size()]);
		}
		finally
		{
			readUnlock();
		}
	}
	
	public void clear()
	{
		writeLock();
		try
		{
			_items.clear();
		}
		finally
		{
			writeUnlock();
		}
	}
	
	public final void writeLock()
	{
		writeLock.lock();
	}
	
	public final void writeUnlock()
	{
		writeLock.unlock();
	}
	
	public final void readLock()
	{
		readLock.lock();
	}
	
	public final void readUnlock()
	{
		readLock.unlock();
	}
	
	public ItemInstance getItemByObjectId(int objectId)
	{
		readLock();
		try
		{
			ItemInstance item;
			for (int i = 0; i < _items.size(); i++)
			{
				item = _items.get(i);
				if (item.getObjectId() == objectId)
				{
					return item;
				}
			}
		}
		finally
		{
			readUnlock();
		}
		return null;
	}
	
	public ItemInstance getItemByItemId(int itemId)
	{
		readLock();
		try
		{
			ItemInstance item;
			for (int i = 0; i < _items.size(); i++)
			{
				item = _items.get(i);
				if (item.getItemId() == itemId)
				{
					return item;
				}
			}
		}
		finally
		{
			readUnlock();
		}
		return null;
	}
	
	public List<ItemInstance> getItemsByItemId(int itemId)
	{
		List<ItemInstance> result = new ArrayList<>();
		readLock();
		try
		{
			ItemInstance item;
			for (int i = 0; i < _items.size(); i++)
			{
				item = _items.get(i);
				if (item.getItemId() == itemId)
				{
					result.add(item);
				}
			}
		}
		finally
		{
			readUnlock();
		}
		return result;
	}
	
	public long getCountOf(int itemId)
	{
		long count = 0L;
		readLock();
		try
		{
			ItemInstance item;
			for (int i = 0; i < _items.size(); i++)
			{
				item = _items.get(i);
				if (item.getItemId() == itemId)
				{
					count = SafeMath.addAndLimit(count, item.getCount());
				}
			}
		}
		finally
		{
			readUnlock();
		}
		return count;
	}
	
	public ItemInstance addItem(int itemId, long count)
	{
		if (count < 1)
		{
			return null;
		}
		ItemInstance item;
		writeLock();
		try
		{
			item = getItemByItemId(itemId);
			if ((item != null) && item.isStackable())
			{
				synchronized (item)
				{
					item.setCount(SafeMath.addAndLimit(item.getCount(), count));
					onModifyItem(item);
				}
			}
			else
			{
				item = ItemFunctions.createItem(itemId);
				item.setCount(count);
				_items.add(item);
				onAddItem(item);
			}
		}
		finally
		{
			writeUnlock();
		}
		return item;
	}
	
	public ItemInstance addItem(ItemInstance item)
	{
		if (item == null)
		{
			return null;
		}
		if (item.getCount() < 1)
		{
			return null;
		}
		ItemInstance result = null;
		writeLock();
		try
		{
			if (getItemByObjectId(item.getObjectId()) != null)
			{
				return null;
			}
			if (item.isStackable())
			{
				int itemId = item.getItemId();
				result = getItemByItemId(itemId);
				if (result != null)
				{
					synchronized (result)
					{
						result.setCount(SafeMath.addAndLimit(item.getCount(), result.getCount()));
						onModifyItem(result);
						onDestroyItem(item);
					}
				}
			}
			if (result == null)
			{
				_items.add(item);
				result = item;
				onAddItem(result);
			}
		}
		finally
		{
			writeUnlock();
		}
		return result;
	}
	
	public ItemInstance removeItemByObjectId(int objectId, long count)
	{
		if (count < 1)
		{
			return null;
		}
		ItemInstance result;
		writeLock();
		try
		{
			ItemInstance item;
			if ((item = getItemByObjectId(objectId)) == null)
			{
				return null;
			}
			synchronized (item)
			{
				result = removeItem(item, count);
			}
		}
		finally
		{
			writeUnlock();
		}
		return result;
	}
	
	public ItemInstance removeItemByItemId(int itemId, long count)
	{
		if (count < 1)
		{
			return null;
		}
		ItemInstance result;
		writeLock();
		try
		{
			ItemInstance item;
			if ((item = getItemByItemId(itemId)) == null)
			{
				return null;
			}
			synchronized (item)
			{
				result = removeItem(item, count);
			}
		}
		finally
		{
			writeUnlock();
		}
		return result;
	}
	
	public ItemInstance removeItem(ItemInstance item, long count)
	{
		if (item == null)
		{
			return null;
		}
		if (count < 1)
		{
			return null;
		}
		if (item.getCount() < count)
		{
			return null;
		}
		writeLock();
		try
		{
			if (!_items.contains(item))
			{
				return null;
			}
			if (item.getCount() > count)
			{
				item.setCount(item.getCount() - count);
				onModifyItem(item);
				ItemInstance newItem = new ItemInstance(IdFactory.getInstance().getNextId(), item.getItemId());
				newItem.setCount(count);
				return newItem;
			}
			return removeItem(item);
		}
		finally
		{
			writeUnlock();
		}
	}
	
	public ItemInstance removeItem(ItemInstance item)
	{
		if (item == null)
		{
			return null;
		}
		writeLock();
		try
		{
			if (!_items.remove(item))
			{
				return null;
			}
			onRemoveItem(item);
			return item;
		}
		finally
		{
			writeUnlock();
		}
	}
	
	public boolean destroyItemByObjectId(int objectId, long count)
	{
		writeLock();
		try
		{
			ItemInstance item;
			if ((item = getItemByObjectId(objectId)) == null)
			{
				return false;
			}
			synchronized (item)
			{
				return destroyItem(item, count);
			}
		}
		finally
		{
			writeUnlock();
		}
	}
	
	public boolean destroyItemByItemId(int itemId, long count)
	{
		writeLock();
		try
		{
			ItemInstance item;
			if ((item = getItemByItemId(itemId)) == null)
			{
				return false;
			}
			synchronized (item)
			{
				return destroyItem(item, count);
			}
		}
		finally
		{
			writeUnlock();
		}
	}
	
	public boolean destroyItem(ItemInstance item, long count)
	{
		if (item == null)
		{
			return false;
		}
		if (count < 1)
		{
			return false;
		}
		if (item.getCount() < count)
		{
			return false;
		}
		writeLock();
		try
		{
			if (!_items.contains(item))
			{
				return false;
			}
			if (item.getCount() > count)
			{
				item.setCount(item.getCount() - count);
				onModifyItem(item);
				return true;
			}
			return destroyItem(item);
		}
		finally
		{
			writeUnlock();
		}
	}
	
	public boolean destroyItem(ItemInstance item)
	{
		if (item == null)
		{
			return false;
		}
		writeLock();
		try
		{
			if (!_items.remove(item))
			{
				return false;
			}
			onRemoveItem(item);
			onDestroyItem(item);
			return true;
		}
		finally
		{
			writeUnlock();
		}
	}
	
	protected abstract void onAddItem(ItemInstance item);
	
	protected abstract void onModifyItem(ItemInstance item);
	
	protected abstract void onRemoveItem(ItemInstance item);
	
	protected abstract void onDestroyItem(ItemInstance item);
}