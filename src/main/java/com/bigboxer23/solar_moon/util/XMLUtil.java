package com.bigboxer23.solar_moon.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** */
public class XMLUtil {
	public static Iterable<Node> iterableNodeList(final NodeList nodeList) {
		return () -> new Iterator<>() {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < nodeList.getLength();
			}

			@Override
			public Node next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				return nodeList.item(index++);
			}
		};
	}
}
