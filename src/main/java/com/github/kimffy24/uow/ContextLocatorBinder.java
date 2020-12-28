package com.github.kimffy24.uow;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public final class ContextLocatorBinder {

	private volatile ExecutingContextFactory facInstance = null;
	
	private final static ContextLocatorBinder BinderInstance;
	
	private static AtomicReferenceFieldUpdater<ContextLocatorBinder, ExecutingContextFactory> facInstanceFieldUpdater =
			AtomicReferenceFieldUpdater.newUpdater(ContextLocatorBinder.class, ExecutingContextFactory.class, "facInstance");
	
	private ContextLocatorBinder() {}
	
	static {
		BinderInstance = new ContextLocatorBinder();
	}
	
	public static void setOnce(ExecutingContextFactory factory) {
		if(!facInstanceFieldUpdater.compareAndSet(BinderInstance, null, factory)) {
			if(!Objects.equals(BinderInstance.facInstance, factory))
				throw new RuntimeException("Multi ExecutingContextFactory creation !!!");
		}
	}
	
	public static ExecutingContextFactory getFacInstance() {
		return BinderInstance.facInstance;
	}
	
}
