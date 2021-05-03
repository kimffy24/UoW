package com.github.kimffy24.uow.skeleton;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;

/**
 * binder类主要是给目标对象的某些功能以函数的方式供用户重定义，
 * 且让相关的方法不暴露在目标类自身的方发表中。
 * 通常都是加载时一次性注册的，
 * @author kimffy
 * 
 */

/**
 * 此处binder只是定向把某些方法注册出来而已。
 * @author kimffy
 *
 */

public class AggregateActionBinder {

	/**
	 * 获取有UoW构建时，留下的数据原始输出数据
	 */
	private static IFunction1<Map<String, Object>, AbstractAggregateRoot<?>> originalDictFetcher = null;

	/**
	 * 用于UoW构建聚合对象后，把db原始数据也放到聚合类上。
	 */
	private static IVoidFunction2<AbstractAggregateRoot<?>, Map<String, Object>> originalDictSetter = null;
	
	/**
	 * 聚合id设置的函子
	 */
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
		if(!ai.compareAndSet(0, 1)) {
			throw new RuntimeException("Multi register call!!! [func: registerOriginalDictAccessor]");
		}
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
	
	private AggregateActionBinder() {}
	
	private final static AtomicInteger ai = new AtomicInteger(0);
}
