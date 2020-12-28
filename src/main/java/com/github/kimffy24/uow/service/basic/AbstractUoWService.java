package com.github.kimffy24.uow.service.basic;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.kimffy24.uow.ExecutingContextFactory;
import com.github.kimffy24.uow.IExecutingContext;
import com.github.kimffy24.uow.skeleton.AbstractAggregateRoot;

import pro.jk.ejoker.common.system.enhance.StringUtilx;

public abstract class AbstractUoWService {

	@Autowired
	protected ExecutingContextFactory executingContextFactory;

	/**
	 * 获取工作单元上下文。工作单元上下文是对仓储以及执行上下文的包装，<br />
	 * 执行上下文记录着对象的进入工作区前的状态以及退出工作区时的状态，<br />
	 * 而通过differ算法能找出在工作区发生变化，<br />
	 * 然后将变化生成sql语句并在spring的事务上下文中提交到数据库，否则视为执行失败<br />
	 * <br />
	 * * 1. 通过AutoCommit注解标识方法是在工作区下执行，退出方法后会自动提交到数据库<br />
	 * * 2. 通过主动调用本对象的 commitAndFlush() 方法主动把所有从上下文获得的对象的变化提交到数据库<br />
	 * @return
	 */
	protected IExecutingContext getCurrentContext() {
		return executingContextFactory.getCurrentContext();
	}

	/**
	 * 按照主键id在持久层查找出对象
	 * @param <T>
	 * @param id
	 * @param prototype
	 * @return
	 */
	protected <T extends AbstractAggregateRoot<?>> T fetchFromContext(Object id, Class<T> prototype) {
		return getCurrentContext().fetch(id, prototype);
	}

	/**
	 * 按条件在持久层中查找对象，条件通常是字段与值对应，condition最后会流到SQL查询的的where条件上，并检索出对应的对象。<br />
	 * * 严格要求匹配对象仅存在1个，否则抛出异常
	 * @param <T>
	 * @param conditions
	 * @param prototype
	 * @return
	 */
	protected <T extends AbstractAggregateRoot<?>> T fetchOneFromContext(Map<String, Object> conditions, Class<T> prototype) {
		List<T> fetchMatcheds = getCurrentContext().fetchMatcheds(conditions, prototype);
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
	protected <T extends AbstractAggregateRoot<?>> List<T> fetchMatchedFromContext(Map<String, Object> conditions, Class<T> prototype) {
		return getCurrentContext().fetchMatcheds(conditions, prototype);
	}

	/**
	 * 新增对象到持久层，每次新增仅仅能添加1个对象，且不能影响其它对象
	 * @param aggr
	 */
	protected void addToContext(AbstractAggregateRoot<?> aggr) {
		getCurrentContext().add(aggr);
	}
	
	/**
	 * 当不使用AutoCommit注解的时候，需要主动提交到数据库，则可以通过这个方法提交
	 */
	protected void commitAndFlush() {
		try {
			getCurrentContext().commit();
		} finally {
			executingContextFactory.cleanContext();
		}
	}
}
