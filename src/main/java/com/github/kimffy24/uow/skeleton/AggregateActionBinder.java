package com.github.kimffy24.uow.skeleton;

import java.util.Map;

import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;

public class AggregateActionBinder {

	private static IFunction1<Map<String, Object>, AbstractAggregateRoot<?>> originalDictFetcher = null;

	private static IVoidFunction2<AbstractAggregateRoot<?>, Map<String, Object>> originalDictSetter = null;
	
	private static IVoidFunction2<AbstractAggregateRoot<?>, Object> setGeneratedIdAction = null;

	private static IFunction1<Boolean, AbstractAggregateRoot<?>> dirtyChecker = null;
	private static IVoidFunction1<AbstractAggregateRoot<?>> dirtyMaker = null;
	
	public static void registerOriginalDictAccessor(
			IFunction1<Map<String, Object>, AbstractAggregateRoot<?>> f,
			IVoidFunction2<AbstractAggregateRoot<?>, Map<String, Object>> as,
			IVoidFunction2<AbstractAggregateRoot<?>, Object> setGi,
			IFunction1<Boolean, AbstractAggregateRoot<?>> dc,
			IVoidFunction1<AbstractAggregateRoot<?>> dm
			) {
		originalDictFetcher = f;
		originalDictSetter = as;
		setGeneratedIdAction = setGi;
		dirtyChecker = dc;
		dirtyMaker = dm;
	}
	
	public static void adoptOriginalDict(AbstractAggregateRoot<?> aggr, Map<String, Object> dict) {
		originalDictSetter.trigger(aggr, dict);
	}
	
	public static Map<String, Object> getOriginalDict(AbstractAggregateRoot<?> aggr) {
		return originalDictFetcher.trigger(aggr);
	}
	
	public static void setGeneratedId(AbstractAggregateRoot<?> aggr, Object id) {
		setGeneratedIdAction.trigger(aggr, id);
	}

	public static boolean isDirty(AbstractAggregateRoot<?> aggr) {
		return dirtyChecker.trigger(aggr);
	}

	public static void markDirty(AbstractAggregateRoot<?> aggr) {
		dirtyMaker.trigger(aggr);
	}
}
