package com.github.kimffy24.uow.service.skeleton;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.kimffy24.uow.core.ExecutingContextFactory;
import com.github.kimffy24.uow.export.IExecutingContext;
import com.github.kimffy24.uow.export.skeleton.AbstractAggregateRoot;
import com.github.kimffy24.uow.service.CommittingService;
import com.github.kimffy24.uow.service.CommittingService.IClosure;

import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.functional.IVoidFunction;

@Deprecated
public abstract class AbstractUoWService {

	@Autowired
	private ExecutingContextFactory executingContextFactory;
	
	@Autowired
	private CommittingService autoCommittingService; 

	/**
	 * 按照主键id在持久层查找出对象
	 * @param <T>
	 * @param id
	 * @param prototype
	 * @return
	 */
	protected final <T extends AbstractAggregateRoot<?>> T fetchFromContext(Object id, Class<T> prototype) {
		return executingContextFactory.getExecutingContext().fetch(id, prototype);
	}

	/**
	 * 按条件在持久层中查找对象，条件通常是字段与值对应，condition最后会流到SQL查询的的where条件上，并检索出对应的对象。<br />
	 * * 严格要求匹配对象仅存在1个，否则抛出异常
	 * @param <T>
	 * @param conditions
	 * @param prototype
	 * @return
	 */
	protected final <T extends AbstractAggregateRoot<?>> T fetchOneFromContext(Map<String, Object> conditions, Class<T> prototype) {
		List<T> fetchMatcheds = executingContextFactory.getExecutingContext().fetchMatcheds(conditions, prototype);
		if(null == fetchMatcheds || fetchMatcheds.isEmpty())
			throw new RuntimeException(StringUtilx.fmt(
					"Cannot find any Aggregate with conditions!!! [type: {}, contitions: {}]",
					prototype.getName(),
					conditions
					));
		if(1 != fetchMatcheds.size())
			throw new RuntimeException(StringUtilx.fmt(
					"Found more than one Aggregate with conditions!!! [type: {}, contitions: {}]",
					prototype.getName(),
					conditions
					));
		
		return fetchMatcheds.get(0);
	}

	/**
	 * 按条件在持久层中查找对象，条件通常是字段与值对应，condition最后会流到SQL查询的的where条件上，并检索出对应的对象
	 * @param <T>
	 * @param conditions
	 * @param prototype
	 * @return
	 */
	protected final <T extends AbstractAggregateRoot<?>> List<T> fetchMatchedFromContext(Map<String, Object> conditions, Class<T> prototype) {
		return executingContextFactory.getExecutingContext().fetchMatcheds(conditions, prototype);
	}

	/**
	 * 新增对象到持久层，每次新增仅仅能添加1个对象，且不能影响其它对象
	 * @param aggr
	 */
	protected final void addToContext(AbstractAggregateRoot<?> aggr) {
		executingContextFactory.getUoWContext(false).add(aggr);
	}
	
//	/**
//	 * 当不使用AutoCommit注解的时候，需要主动提交到数据库，则可以通过这个方法提交
//	 */
//	protected final void commitAndFlush() {
//		try {
//			getCurrentContext().commit();
//		} finally {
//			executingContextFactory.cleanContext();
//		}
//	}
	
	/**
	 * 当不使用AutoCommit注解的时候，需要在UoW上下文中执行自己的业务方法，用户只需传入业务函子即可。
	 * @param uf 用户执行执行的闭包函数
	 * @throws Throwable
	 */
	protected final void doUnderContext(IVoidFunction uf) throws Throwable {
		IExecutingContext uoWContext = executingContextFactory.getUoWContext(true);
		autoCommittingService.surroundExec(new IClosure() {
			@Override
			public Object exec() throws Throwable {
				uf.trigger();
				return null;
			}
		});
	}
}
