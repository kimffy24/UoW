package com.github.kimffy24.uow.export.skeleton;

public interface AggregateRootLifeCycleAware {

	public void postCreation();
	
	public void preCommit();
	
}
