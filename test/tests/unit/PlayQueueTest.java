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

package tests.unit;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import com.musicott.player.PlayQueue;

/**
 * @author Octavio Calleya
 *
 */
public class PlayQueueTest {

	@Test(expected = NoSuchElementException.class)
	public void dequeueOnEmptyQueueWithElementTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		assertTrue(pq.size() == 0);
		int i = pq.element();
	}
	
	@Test
	public void dequeueOnEmptyQueueWithPollTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		assertTrue(pq.size() == 0);
		assertEquals(null, pq.poll());
	}
	
	@Test(expected = NoSuchElementException.class)
	public void dequeueOnEmptyQueueWithRemoveTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		assertTrue(pq.size() == 0);
		assertEquals(null, pq.remove());
	}
	
	@Test
	public void enqueueDequeueElementTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		int i = 1;
		pq.add(i);
		assertTrue(pq.size() == 1);
		assertTrue(i == pq.peek());
		assertTrue(pq.size() == 1);
		assertTrue(i == pq.poll());
		assertTrue(pq.size() == 0);
		pq.offer(i);
		assertTrue(pq.size() == 1);
		assertTrue(i == pq.peek());
		assertTrue(pq.size() == 1);
		assertTrue(i == pq.poll());
		assertTrue(pq.size() == 0);
	}
	
	@Test
	public void clearQueueTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		pq.add(1);
		pq.add(2);
		pq.add(3);
		pq.add(123456);
		assertTrue(pq.size() == 4);
		pq.clear();
		assertTrue(pq.size() == 0);
		assertTrue(pq.isEmpty());
	}
	
	@Test
	public void afterEnsureCapacityTest1() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		for(int i=0;i<8;i++)
			pq.add(i);
		assertTrue(pq.size() == 8);
		Iterator<Integer> it = pq.iterator();
		assertTrue(it.hasNext());
		for(int i=0;i<8;i++)
			assertTrue(i == it.next());
		assertTrue(!it.hasNext());
		for(int i=0;i<8;i++)
			assertTrue(i == pq.poll());
		assertTrue(pq.isEmpty());
	}
	
	@Test
	public void afterEnsureCapacityTest2() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		for(int i=0;i<8;i++)
			pq.add(i);
		assertTrue(pq.size() == 8);
		Iterator<Integer> it = pq.iterator();
		assertTrue(it.hasNext());
		for(int i=0;i<8;i++)
			assertTrue(i == it.next());
		assertTrue(!it.hasNext());
		for(int i=8;i<16;i++)
			pq.add(i);
		it = pq.iterator();
		assertTrue(it.hasNext());
		for(int i=0;i<16;i++)
			assertTrue(i == it.next());
		assertTrue(!it.hasNext());
		for(int i=0;i<16;i++)
			assertTrue(i == pq.poll());
		assertTrue(pq.isEmpty());
	}
	
	@Test
	public void iteratorTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		Iterator<Integer> it = pq.iterator();
		assertTrue(!it.hasNext());
		pq.add(1);
		pq.add(2);
		pq.add(45);
		it = pq.iterator();
		assertTrue(3 == pq.size());
		assertTrue(it.hasNext());
		assertTrue(1 == it.next());
		assertTrue(3 == pq.size());
		assertTrue(2 == it.next());
		assertTrue(3 == pq.size());
		assertTrue(45 == it.next());
		assertTrue(3 == pq.size());
		assertTrue(!it.hasNext());
		assertTrue(!pq.isEmpty());
		assertTrue(pq.poll() == 1);
		assertTrue(pq.poll() == 2);
		assertTrue(pq.poll() == 45);
		assertTrue(pq.isEmpty());
	}
	
	@Test(expected = NoSuchElementException.class)
	public void iteratorExceptionTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		Iterator<Integer> it = pq.iterator();
		assertTrue(!it.hasNext());
		pq.add(1);;
		it = pq.iterator();
		assertTrue(1 == pq.size());
		assertTrue(it.hasNext());
		it.next();
		it.next();
	}
	
	@Test
	public void iteratorRemoveTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		for(int i=0;i<8;i++)
			pq.add(i);
		assertTrue(pq.size() == 8);
		Iterator<Integer> it = pq.iterator();
		assertTrue(it.hasNext());
		it.next();
		it.remove();
		assertTrue(pq.size() == 7);
		for(int i=1;i<8;i++)
			assertTrue(pq.poll() == i);
		assertTrue(pq.isEmpty());
		

		pq = new PlayQueue<Integer>();
		for(int i=0;i<17;i++)
			pq.add(i);
		assertTrue(pq.size() == 17);
		it = pq.iterator();
		while(it.hasNext()) {
			it.next();
			it.remove();
		}
		assertTrue(!it.hasNext());
		assertTrue(pq.size() == 0);
	}
	
	@Test
	public void removeAllcontainsTest() {
		PlayQueue<Integer> pq = new PlayQueue<Integer>();
		for(int i=0;i<12;i++)
			pq.add(i);
		
		List<Integer> li = new ArrayList<Integer>();
		for(int i=0;i<12;i++)
			if(i%2 == 0)
				li.add(i);
		pq.removeAll(li);
		assertTrue(pq.size() == 6);
		for(int i=0;i<12;i++)
			if(i%2==0) {
				assertTrue(!pq.contains(i));
			}
			else
				assertTrue(pq.contains(i));				
	}
}