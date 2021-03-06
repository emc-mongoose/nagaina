package com.emc.nagaina.impl.base;

import com.emc.mongoose.api.model.item.BasicDataItemFactory;
import com.emc.mongoose.api.model.item.DataItemFactory;

import com.emc.nagaina.api.DataItemMock;

/**
 Created by kurila on 21.09.16.
 */
public class BasicDataItemMockFactory<I extends DataItemMock>
extends BasicDataItemFactory<I>
implements DataItemFactory<I> {
	
	@Override
	public final I getItem(final String name, final long id, final long size) {
		return (I) new BasicDataItemMock(name, id, size);
	}
	
	@Override
	public final I getItem(final String line) {
		return (I) new BasicDataItemMock(line);
	}
	
	@Override
	public final Class<I> getItemClass() {
		return (Class<I>) BasicDataItemMock.class;
	}
}
