package com.github.kimffy24.uow;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.helper.Ensure;

/**
 * 执行提交的切面<br />
 * 
 * * 这里优先级选择最高值，即最贴近被切方法<br />
 *   这里需要切人到比事务所在层次更加内层的层次。
 * @author jiefzz.lon
 *
 */
@Aspect
@Order(Integer.MAX_VALUE)
public class CommittingService {

	private static final Logger logger = LoggerFactory.getLogger(CommittingService.class);

	@Autowired
	private ExecutingContextFactory executingContextFactory;

	@Pointcut("@annotation(com.github.kimffy24.uow.annotation.AutoCommit)")
	public void pointcut() {
	}

	/**
	 * 环绕增强，相当于MethodInterceptor
	 * @throws Throwable
	 */
	@Around("pointcut()")
	public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Object res = null;
		boolean isExitAutoCommit = false;
		Throwable t = null;
		try {
			IExecutingContext currentContext = executingContextFactory.getCurrentContext();
			MarkRound.trigger(currentContext);
			res =  joinPoint.proceed();
			if(isExitAutoCommit = ExitRoundAndCheckIsLatest.trigger(currentContext))
				executingContextFactory.getCurrentContext().commit();
			return res;
		} catch (Throwable _t) {
			isExitAutoCommit = true;
			t = _t;
		} finally {
			try {
				if(isExitAutoCommit)
					executingContextFactory.cleanContext();
			} catch (Exception e){
				logger.error("Fail on clean context!!!", e);
				if(null != t)
					t.addSuppressed(e);
				else
					t = e;
			}
		}
		throw t;
	}
	
	@PostConstruct
	private void init() {
		Ensure.notNull(MarkRound, "CommittingService.MarkRound");
		Ensure.notNull(ExitRoundAndCheckIsLatest, "CommittingService.ExitRoundAndCheckIsLatest");
	}

	// 以下部分为ExecutingContext部分功能函子直接向本类定向暴露方法的入口
	// 如果写public方法，将会向所有用户暴露出来
	
	private static IVoidFunction1<IExecutingContext> MarkRound = null;
	private static IFunction1<Boolean, IExecutingContext> ExitRoundAndCheckIsLatest = null;
	
	public static final void registerRoundStackAction(
			IVoidFunction1<IExecutingContext> markRound,
			IFunction1<Boolean, IExecutingContext> exitRoundAndCheckIsLatest) {
		if(null != MarkRound)
			throw new RuntimeException("Multi register!!!");
		MarkRound = markRound;
		ExitRoundAndCheckIsLatest = exitRoundAndCheckIsLatest;
	}
	
}
