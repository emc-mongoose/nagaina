package com.emc.nagaina.api;

import com.github.akurilov.commons.collection.Range;

import com.emc.mongoose.api.model.concurrent.Daemon;

import com.emc.nagaina.api.exception.ContainerMockException;
import com.emc.nagaina.api.exception.ContainerMockNotFoundException;
import com.emc.nagaina.api.exception.ObjectMockNotFoundException;
import com.emc.nagaina.api.exception.StorageMockCapacityLimitReachedException;

import java.util.Collection;
import java.util.List;

/**
 Created on 11.07.16. */
public interface StorageMock<T extends DataItemMock>
extends Daemon {

	String DEFAULT_CONTAINER_NAME = "default";

	long getSize();

	long getCapacity();

	int getPort();
	
	boolean sslEnabled();

	boolean dropConnection();

	boolean missResponse();

	StorageIoStats getStats();

	void put(final List<T> dataItems);

	ObjectContainerMock<T> createContainer(final String name);

	ObjectContainerMock<T> getContainer(final String name);

	void deleteContainer(final String name);

	T getObject(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockException;

	void createObject(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException, StorageMockCapacityLimitReachedException;

	void deleteObject(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException;

	void updateObject(
		final String containerName, final String id, final long size, final Range byteRange
	) throws ContainerMockException, ObjectMockNotFoundException;

	T listObjects(
		final String containerName, final String marker,
		final Collection<T> outputBuffer, final int maxCount
	) throws ContainerMockException;
}
