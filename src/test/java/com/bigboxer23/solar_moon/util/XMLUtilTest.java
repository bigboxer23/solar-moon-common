package com.bigboxer23.solar_moon.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@ExtendWith(MockitoExtension.class)
public class XMLUtilTest {

	@Test
	public void testIterableNodeList_withMultipleNodes() {
		NodeList nodeList = createMockNodeList(3);

		List<Node> result = new ArrayList<>();
		for (Node node : XMLUtil.iterableNodeList(nodeList)) {
			result.add(node);
		}

		assertEquals(3, result.size());
	}

	@Test
	public void testIterableNodeList_withEmptyNodeList() {
		NodeList nodeList = createMockNodeList(0);

		List<Node> result = new ArrayList<>();
		for (Node node : XMLUtil.iterableNodeList(nodeList)) {
			result.add(node);
		}

		assertEquals(0, result.size());
	}

	@Test
	public void testIterableNodeList_hasNextReturnsFalseForEmptyList() {
		NodeList nodeList = createMockNodeList(0);

		Iterator<Node> iterator = XMLUtil.iterableNodeList(nodeList).iterator();

		assertFalse(iterator.hasNext());
	}

	@Test
	public void testIterableNodeList_hasNextReturnsTrueWhenNodesRemain() {
		NodeList nodeList = createMockNodeList(2);

		Iterator<Node> iterator = XMLUtil.iterableNodeList(nodeList).iterator();

		assertTrue(iterator.hasNext());
		iterator.next();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testIterableNodeList_nextThrowsNoSuchElementException() {
		NodeList nodeList = createMockNodeList(1);

		Iterator<Node> iterator = XMLUtil.iterableNodeList(nodeList).iterator();
		iterator.next();

		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	public void testIterableNodeList_multipleIterations() {
		NodeList nodeList = createMockNodeList(2);
		Iterable<Node> iterable = XMLUtil.iterableNodeList(nodeList);

		List<Node> firstIteration = new ArrayList<>();
		for (Node node : iterable) {
			firstIteration.add(node);
		}

		List<Node> secondIteration = new ArrayList<>();
		for (Node node : iterable) {
			secondIteration.add(node);
		}

		assertEquals(2, firstIteration.size());
		assertEquals(2, secondIteration.size());
	}

	@Test
	public void testIterableNodeList_singleNode() {
		NodeList nodeList = createMockNodeList(1);

		Iterator<Node> iterator = XMLUtil.iterableNodeList(nodeList).iterator();

		assertTrue(iterator.hasNext());
		assertNotNull(iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testIterableNodeList_nextWithoutHasNextCheck() {
		NodeList nodeList = createMockNodeList(2);

		Iterator<Node> iterator = XMLUtil.iterableNodeList(nodeList).iterator();

		assertNotNull(iterator.next());
		assertNotNull(iterator.next());
	}

	private NodeList createMockNodeList(int size) {
		NodeList nodeList = mock(NodeList.class);
		when(nodeList.getLength()).thenReturn(size);

		for (int i = 0; i < size; i++) {
			Node node = mock(Node.class);
			when(nodeList.item(i)).thenReturn(node);
		}

		return nodeList;
	}
}
