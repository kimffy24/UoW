package com.github.kimffy24.uow;

import java.util.List;
import java.util.Map;

import com.github.kimffy24.uow.skeleton.AbstractAggregateRoot;

public interface IExecutingContext {
	
	public <T extends AbstractAggregateRoot<?>> T fetch(Object id, Class<T> prototype);

	/**
	 * 少用为好，应该多通过IdLocator获得其id，然后再调用fetch方法获得聚合
	 */
	@Deprecated
	public <T extends AbstractAggregateRoot<?>> List<T> fetchMatcheds(Map<String, Object> conditions, Class<T> prototype);
	
	public void add(AbstractAggregateRoot<?> aggr);
	
	public void commit();
	
}
