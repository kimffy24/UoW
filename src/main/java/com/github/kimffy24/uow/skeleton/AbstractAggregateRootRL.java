package com.github.kimffy24.uow.skeleton;

import java.util.Date;

/**
 * 支持逻辑删除的
 * @author kimffy
 *
 * @param <TID>
 */
public class AbstractAggregateRootRL<TID> extends AbstractAggregateRoot<TID> {

	private int deleted = 0;
	
	private Date deletedAt = null;
	
	private final static Date NoDate = new Date(0l);
	
	public AbstractAggregateRootRL() {
		super();
	}
	
	public void markRemovedLogically() {
		this.deleted = 1;
		this.deletedAt = new Date();
	}
	
	public void unmarkRemovedLogically() {
		this.deleted = 0;
		this.deletedAt = NoDate;
	}

	// ======== getter
	
	public boolean isDeleted() {
		return deleted>0;
	}

	public Date getDeletedAt() {
		return deletedAt;
	}
	
}
