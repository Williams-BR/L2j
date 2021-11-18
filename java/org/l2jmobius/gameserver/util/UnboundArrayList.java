/*
 * Copyright (c) 2020 Pantelis Andrianakis
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.l2jmobius.gameserver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Unbound and synchronized implementation of the {@code ArrayList} class.<br>
 * In addition to implementing the {@code ArrayList} class,<br>
 * this class provides synchronization on adding and removing elements.<br>
 * Further more, get() does not throw IndexOutOfBoundsException, a {@code null} element is returned instead.<br>
 * An equivalent Iterator constructor is introduced under the same logic.
 * @param <E> the type of elements in this list
 * @version September 4th 2020
 * @author Pantelis Andrianakis
 */
public class UnboundArrayList<E>extends ArrayList<E>
{
	/**
	 * Returns the element at the specified position in this list.
	 * @param index index of the element to return
	 * @return the element at the specified position in this list,<br>
	 *         or {@code null} if this list does not have this specified position.
	 */
	@Override
	public E get(int index)
	{
		E element = null;
		
		try
		{
			// The try performance impact itself is insignificant.
			// Benchmark test on an i7-2600 machine with a list of 100.000.000 random elements.
			// get: Execution time was 116 milliseconds.
			// get with try: Execution time was 124 milliseconds.
			element = super.get(index);
		}
		catch (Exception e)
		{
			// Continue with execution.
			// This impacts performance only if the Exception is created.
			// Benchmark test on an i7-2600 machine resulted with a 187 millisecond total delay,
			// in a list of 10.000 elements, with IndexOutOfBoundsException thrown on 5.000 of them.
		}
		
		return element;
	}
	
	/**
	 * Replaces the element at the specified position in this list with the specified element.
	 * @param index index of the element to replace
	 * @param element element to be stored at the specified position
	 * @return the element previously at the specified position,<br>
	 *         or {@code null} if this list does not have this specified position.
	 */
	@Override
	public E set(int index, E element)
	{
		synchronized (this)
		{
			if ((index >= 0) && (index < size()))
			{
				return super.set(index, element);
			}
			return null;
		}
	}
	
	/**
	 * Appends the specified element to the end of this list.
	 * @param e element to be appended to this list
	 * @return {@code true} (as specified by {@link Collection#add})
	 */
	@Override
	public boolean add(E e)
	{
		synchronized (this)
		{
			return super.add(e);
		}
	}
	
	/**
	 * Inserts the specified element at the specified position in this list. Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
	 * @param index index at which the specified element is to be inserted
	 * @param element element to be inserted
	 */
	@Override
	public void add(int index, E element)
	{
		synchronized (this)
		{
			if ((index >= 0) && (index < size()))
			{
				super.add(index, element);
			}
			else
			{
				super.add(element);
			}
		}
	}
	
	/**
	 * Appends the specified element to the end of this list if this list does not contain the specified element.
	 * @param e element to be appended to this list
	 * @return {@code true} (as specified by {@link Collection#add})
	 */
	public boolean addIfAbsent(E e)
	{
		synchronized (this)
		{
			if (!contains(e))
			{
				return super.add(e);
			}
			return false;
		}
	}
	
	/**
	 * Removes the first occurrence of the specified element from this list, if it is present. If the list does not contain the element, it is unchanged. More formally, removes the element with the lowest index {@code i} such that {@code Objects.equals(o, get(i))} (if such an element exists).
	 * Returns {@code true} if this list contained the specified element (or equivalently, if this list changed as a result of the call).
	 * @param o element to be removed from this list, if present
	 * @return {@code true} if this list contained the specified element
	 */
	@Override
	public boolean remove(Object o)
	{
		synchronized (this)
		{
			return super.remove(o);
		}
	}
	
	/**
	 * Removes the element at the specified position in this list. Shifts any subsequent elements to the left (subtracts one from their indices).
	 * @param index the index of the element to be removed
	 * @return the element that was removed from the list,<br>
	 *         or {@code null} if this list does not have this specified position.
	 */
	@Override
	public E remove(int index)
	{
		synchronized (this)
		{
			if ((index >= 0) && (index < size()))
			{
				return super.remove(index);
			}
			return null;
		}
	}
	
	/**
	 * Removes all of the elements from this list.<br>
	 * The list will be empty after this call returns.
	 */
	@Override
	public void clear()
	{
		synchronized (this)
		{
			super.clear();
		}
	}
	
	/**
	 * Appends all of the elements in the specified collection to the end of this list, in the order that they are returned by the specified collection's Iterator. The behavior of this operation is undefined if the specified collection is modified while the operation is in progress. (This implies
	 * that the behavior of this call is undefined if the specified collection is this list, and this list is nonempty.)
	 * @param c collection containing elements to be added to this list
	 * @return {@code true} if this list changed as a result of the call
	 * @throws NullPointerException if the specified collection is null
	 */
	@Override
	public boolean addAll(Collection<? extends E> c)
	{
		synchronized (this)
		{
			return super.addAll(c);
		}
	}
	
	/**
	 * Inserts all of the elements in the specified collection into this list, starting at the specified position. Shifts the element currently at that position (if any) and any subsequent elements to the right (increases their indices). The new elements will appear in the list in the order that
	 * they are returned by the specified collection's iterator.
	 * @param index index at which to insert the first element from the specified collection
	 * @param c collection containing elements to be added to this list
	 * @return {@code true} if this list changed as a result of the call
	 * @throws NullPointerException if the specified collection is null
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c)
	{
		synchronized (this)
		{
			if ((index >= 0) && (index < size()))
			{
				return super.addAll(index, c);
			}
			return super.addAll(c);
		}
	}
	
	/**
	 * Removes from this list all of the elements whose index is between {@code fromIndex}, inclusive, and {@code toIndex}, exclusive. Shifts any succeeding elements to the left (reduces their index). This call shortens the list by {@code (toIndex - fromIndex)} elements.
	 */
	@Override
	protected void removeRange(int fromIndex, int toIndex)
	{
		synchronized (this)
		{
			if (fromIndex > size())
			{
				return;
			}
			super.removeRange(fromIndex < 0 ? 0 : fromIndex, size() < toIndex ? size() - 1 : toIndex);
		}
	}
	
	/**
	 * Removes from this list all of its elements that are contained in the specified collection.
	 * @param c collection containing elements to be removed from this list
	 * @return {@code true} if this list changed as a result of the call
	 * @throws ClassCastException if the class of an element of this list is incompatible with the specified collection (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if this list contains a null element and the specified collection does not permit null elements (<a href="Collection.html#optional-restrictions">optional</a>), or if the specified collection is null
	 * @see Collection#contains(Object)
	 */
	@Override
	public boolean removeAll(Collection<?> c)
	{
		synchronized (this)
		{
			return super.removeAll(c);
		}
	}
	
	/**
	 * Retains only the elements in this list that are contained in the specified collection. In other words, removes from this list all of its elements that are not contained in the specified collection.
	 * @param c collection containing elements to be retained in this list
	 * @return {@code true} if this list changed as a result of the call
	 * @throws ClassCastException if the class of an element of this list is incompatible with the specified collection (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if this list contains a null element and the specified collection does not permit null elements (<a href="Collection.html#optional-restrictions">optional</a>), or if the specified collection is null
	 * @see Collection#contains(Object)
	 */
	@Override
	public boolean retainAll(Collection<?> c)
	{
		synchronized (this)
		{
			return super.retainAll(c);
		}
	}
	
	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 * @return an iterator over the elements in this list in proper sequence.<br>
	 *         Non existing elements are returned as null.
	 */
	@Override
	public Iterator<E> iterator()
	{
		return new Itr();
	}
	
	/**
	 * An optimized version of AbstractList.Itr
	 */
	private class Itr implements Iterator<E>
	{
		private int _cursor = 0; // index of next element to return
		
		// prevent creating a synthetic constructor
		Itr()
		{
		}
		
		@Override
		public boolean hasNext()
		{
			return _cursor < size();
		}
		
		@Override
		public E next()
		{
			return get(_cursor++);
		}
		
		@Override
		public void remove()
		{
			UnboundArrayList.this.remove(get(_cursor - 1));
		}
		
		@Override
		public void forEachRemaining(Consumer<? super E> action)
		{
			if (action == null)
			{
				return;
			}
			for (int i = _cursor; i < size(); i++)
			{
				final E next = get(i);
				if (next != null)
				{
					action.accept(next);
				}
				_cursor++;
			}
		}
	}
}
