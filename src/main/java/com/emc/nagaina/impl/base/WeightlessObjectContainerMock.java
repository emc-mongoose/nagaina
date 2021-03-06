package com.emc.nagaina.impl.base;

import com.github.akurilov.commons.collection.ListingLRUMap;
import com.emc.nagaina.api.DataItemMock;
import com.emc.nagaina.api.ObjectContainerMock;

import java.io.IOException;
import java.util.Collection;

/**
 Created on 20.07.16.
 */
public final class WeightlessObjectContainerMock<T extends DataItemMock>
implements ObjectContainerMock<T> {

	private final ListingLRUMap<String, T> containerMap;

	public WeightlessObjectContainerMock(final int capacity) {
		this.containerMap = new ListingLRUMap<String, T>(capacity) {
			@Override @SuppressWarnings("unchecked")
			protected final boolean removeLRU(final LinkEntry entry) {
				if(super.removeLRU(entry)) {
					decrementSize();
					return true;
				} else {
					return false;
				}
			}
		};
	}

	@Override
	public synchronized int size() {
		return containerMap.size();
	}

	@Override
	public synchronized T list(
		final String afterObjectId, final Collection<T> outputBuffer, final int limit
	) {
		return containerMap.list(afterObjectId, outputBuffer, limit);
	}

	@Override
	public synchronized Collection<T> values() {
		return containerMap.values();
	}

	@Override
	public synchronized T get(final String key) {
		return containerMap.get(key);
	}

	@Override
	public synchronized T put(final String key, final T value) {
		return containerMap.put(key, value);
	}

	@Override
	public synchronized T remove(final String key) {
		return containerMap.remove(key);
	}
	
	@Override
	public void close()
	throws IOException {
		containerMap.clear();
	}
}
