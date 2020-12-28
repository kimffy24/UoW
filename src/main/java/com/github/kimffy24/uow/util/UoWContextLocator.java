package com.github.kimffy24.uow.util;

import com.github.kimffy24.uow.ContextLocatorBinder;
import com.github.kimffy24.uow.ExecutingContextFactory;
import com.github.kimffy24.uow.IExecutingContext;
import com.github.kimffy24.uow.skeleton.AbstractAggregateRoot;

public final class UoWContextLocator {

	public static IExecutingContext get() {
		IExecutingContext cxt;
		ExecutingContextFactory facInstance = ContextLocatorBinder.getFacInstance();
		if(null == facInstance || null == (cxt = facInstance.getCurrentContext()))
			throw new RuntimeException("No AutoCommit context found !!! Are you in AutoCommit processing ?");
		return cxt;
	}
	
	public static void addToContext(AbstractAggregateRoot<?> aggr) {
		IExecutingContext cxt = get();
		cxt.add(aggr);
	}
	
	public static <T> AbstractAggregateRoot<T> fetch(T id, Class<? extends AbstractAggregateRoot<T>> aggrType) {
		return get().fetch(id, aggrType);
	}
}
