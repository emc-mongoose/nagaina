package com.emc.nagaina.api;

import com.github.akurilov.commons.collection.Listable;

import java.io.Closeable;
import java.util.Collection;

/**
 Created on 19.07.16.
 */
public interface ObjectContainerMock<T extends DataItemMock>
extends Closeable, Listable<T> {

	T get (final String key);

	T put(final String key, final T value);

	T remove(final String key);

	int size();

	Collection<T> values();
}
