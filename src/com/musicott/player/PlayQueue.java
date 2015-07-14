/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.player;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Octavio Calleya
 *
 */
public class PlayQueue<T> extends AbstractQueue<T> {

	/**
	 * Default initial capacity
	 */
	private static final int DEFAULT_CAPACITY = 8;
	
	/**
	 * The array buffer with the elements
	 */
	private T[] queue;
	
	private int head;
	private int tail;
	private int size;
	
	/**
	 * Default constructor
	 */
	public PlayQueue() {
		super();
		queue = newArray(DEFAULT_CAPACITY);
		head =  0;
		tail = 0;
		size = 0;
	}

    @SuppressWarnings("unchecked")
    private T[] newArray(int size) {
        return (T[]) new Object[size];
    }
    
    private void ensureCapacity() {
    	if(size() == queue.length) {
    		T[] extension = newArray(queue.length * 2);
    		for(int i=0; i<size(); i++) {
    			extension[i] = queue[head];
    			head = (head+1) % queue.length;
			}
    		queue = extension;
			head = 0;
			tail = size-1;
    	}
    }
	
	@Override
	public boolean offer(T e) {
		ensureCapacity();
		tail = (tail+1) % queue.length; // circular increment
		queue[tail] = e;
		if(isEmpty())
			head = tail;
		size++;	
		return true;
	}

	@Override
	public T poll() {
		if(isEmpty())
			return null;
		else {
			T item = queue[head];
			head = (head+1) % queue.length;
			size--;
			return item;
		}
	}

	@Override
	public T peek() {
		if(isEmpty())
			return null;
		else {
			return queue[head];
		}
	}
	
	@Override
	public Iterator<T> iterator() {
		return new PlayQueueIterator();
	}

	@Override
	public int size() {
		return size;
	}
	
	private class PlayQueueIterator implements Iterator<T> {
		
		private int index = head;
		private int iteratorSize = 0;
		private int lastIndex = -1;

		@Override
		public boolean hasNext() {
			return iteratorSize < size;
		}

		@Override
		public T next() {
			if(iteratorSize < size) {
				T item = queue[index];
				lastIndex = index;
				index = (index+1) % queue.length;
				iteratorSize++;
				return item;
			}
			else
				throw new NoSuchElementException();
		}		
		
		@Override
		public void remove() {
			if(lastIndex != -1) { // Ensures next was called at least 1 time
				int count = 0;
				for(int i = head; count < size; i= (i+1) % queue.length) {
					if(i >= lastIndex)
						queue[i] = queue[(i+1) % queue.length];
					count++;
				}
				if(count >= iteratorSize)
					iteratorSize--;
				size--;
				tail = tail-1;
				if(tail < 0)
					tail = queue.length-1;
			}
			else
				throw new IllegalStateException();
		}
	}
}