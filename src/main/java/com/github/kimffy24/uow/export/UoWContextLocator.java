package com.github.kimffy24.uow.export;

import com.github.kimffy24.uow.core.ExecutingContextFactory;
import com.github.kimffy24.uow.core.ExecutingContextFactory.ContextLocatorBinder;
import com.github.kimffy24.uow.export.skeleton.AbstractAggregateRoot;

public final class UoWContextLocator {

	public static IExecutingContext curr() {
		IExecutingContext cxt;
		ExecutingContextFactory FacInstance = ContextLocatorBinder.getFacInstance();
		if(null == FacInstance || null == (cxt = FacInstance.getExecutingContext()))
			throw new RuntimeException("No AutoCommit context found !!! Are you in AutoCommit processing ?");
		return cxt;
	}
	
	public static void addToContext(AbstractAggregateRoot<?> aggr) {
		IExecutingContext cxt = curr();
		cxt.add(aggr);
	}
}
