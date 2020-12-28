package com.github.kimffy24.uow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.kimffy24.uow.component.StdConverter;
import com.github.kimffy24.uow.mapper.ILocatorMapper;
import com.github.kimffy24.uow.repos.RepositoryHub;
import com.github.kimffy24.uow.repos.RepositoryHub.Repository;
import com.github.kimffy24.uow.skeleton.AbstractAggregateRoot;
import com.github.kimffy24.uow.skeleton.AggregateActionBinder;

import pro.jk.ejoker.common.system.enhance.EachUtilx;
import pro.jk.ejoker.common.system.enhance.MapUtilx;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;

public class ExecutingContextFactory {

//	private static final Logger logger = LoggerFactory.getLogger(ExecutingContextFactory.class);
	
	private StdConverter stdConverter = StdConverter.getInstance();

	@Autowired
	private RepositoryHub reposHub;
	
	private ThreadLocal<IExecutingContext> tl = new ThreadLocal<>();
	
	static {
		CommittingService.registerRoundStackAction(ec -> {
			((UoWContext )ec).markRound();
		}, ec -> {
			return ((UoWContext )ec).exitRoundAndCheckIsLatest();
		});
	}
	
	@PostConstruct
	private void init() {
		ContextLocatorBinder.setOnce(this);
	}
	
	/**
	 * ExecutingContext 目前使用ThreadLocal绑定执行关系，因此不要在某个地方存下来，需要的时候通过调用获取以确保正确性
	 * @return
	 */
	public IExecutingContext getCurrentContext() {
		IExecutingContext cxt = tl.get();
		if(null == cxt) {
			cxt = new UoWContext();
			tl.set(cxt);
		}
		return cxt;
	}
	
	public final class UoWContext implements IExecutingContext {

				private final AtomicInteger depth = new AtomicInteger(0);
				
				private void markRound() {	
					depth.getAndIncrement();
				}
				
				private boolean exitRoundAndCheckIsLatest() {
					// 先减后再对比
					return depth.decrementAndGet() == 0;
				}
				
//				private List<AbstractAggregateRoot<?>> affectAggregates = new ArrayList<>();
				
				private AbstractAggregateRoot<?> newAggr = null;
				
				private Map<Object, AbstractAggregateRoot<?>> trackingAggregates = new HashMap<>();
				
				@SuppressWarnings("unchecked")
				@Override
				public <T extends AbstractAggregateRoot<?>> T fetch(Object id, Class<T> prototype) {
					return (T )MapUtilx.getOrAdd(trackingAggregates, getTypeIdKey(prototype, id), () -> {
						Repository<T> repository = reposHub.getRepos(prototype);
						T fetch = repository.fetch(id);
						return fetch;
					});
				}
				
				@SuppressWarnings("unchecked")
				@Override
				public <T extends AbstractAggregateRoot<?>> List<T> fetchMatcheds(
						Map<String, Object> conditions,
						Class<T> prototype) {
					Repository<T> repository = reposHub.getRepos(prototype);
					List<T> fetchMatched = repository.fetchMatched(conditions);
					List<T> newArrayList = new ArrayList<>();
					EachUtilx.forEach(fetchMatched, a -> {
						T previous = (T )trackingAggregates
								.putIfAbsent(getTypeIdKey(a.getClass(), a.getId()), a);
						if(null != previous) {
							newArrayList.add(previous);
						} else {
							newArrayList.add(a);
						}
					});
					return newArrayList;
				}
	
				@Override
				public void add(AbstractAggregateRoot<?> aggr) {
					if(null == newAggr)
						newAggr = aggr;
					else
						throw new RuntimeException("Only one creation or All updating in a Action is permitted!!!");
				}
	
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void commit() {
					Map<Map<String, Object>, ILocatorMapper> cStore = new HashMap<>();
					Map<IdVersionTuple, ILocatorMapper> uStore =  new HashMap<>();
					IVoidFunction1<Object> idAdopt = null;
					
					if(null != newAggr) {
						// 新增
						Repository<?> repository = reposHub.getRepos(newAggr.getClass());
						ILocatorMapper provideLocatorMapper = repository.provideLocatorMapper();
						Map<String, Object> convert = stdConverter.convert(newAggr);
						convert.put("version", 1);
						cStore.put(convert, provideLocatorMapper);
						idAdopt = id -> AggregateActionBinder.setGeneratedId(newAggr, id);
					}
					
					Collection<AbstractAggregateRoot<?>> affectAggregates = trackingAggregates.values();
					for(AbstractAggregateRoot<?> aggr : affectAggregates) {
						if(AggregateActionBinder.isDirty(aggr))
							throw new RuntimeException(StringUtilx.fmt(
									"AggregateRoot is changed before commit!!! [type: {}, id: {}, version: {}]",
									aggr.getClass().getSimpleName(),
									aggr.getId(),
									aggr.getVersion()
									));
						Repository repository = reposHub.getRepos(aggr.getClass());
						ILocatorMapper provideLocatorMapper = repository.provideLocatorMapper();
	
						Map<String, Object> convert = stdConverter.convert(aggr);
						Map aggrOriginalDict = repository.getAggrOriginalDict(aggr);
						if(null == aggrOriginalDict) {
							// 逻辑上不会出现这种情况
							throw new RuntimeException("Only one creation or All updating in a Action is permitted!!!");
						} else {
							Map<String, Object> diffMap = new HashMap<>();
							// 更新
							Iterator<Entry<String, Object>> iterator = convert.entrySet().iterator();
							while(iterator.hasNext()) {
								Entry<String, Object> ety = iterator.next();
								Object originalValue = aggrOriginalDict.get(ety.getKey());
								Object newValue = ety.getValue();
								if(Objects.equals(originalValue, newValue))
									iterator.remove();
							}
							if(!convert.isEmpty()) {
								// 如果没有变化，则不参与本次更新
								convert.put("version", aggr.getVersion()+1);
								uStore.put(
										IdVersionTuple.of(aggr.getId(), aggr.getVersion(), convert),
										provideLocatorMapper);
							}
						}
					}
					
					if(cStore.size()>1 || (!uStore.isEmpty() && !cStore.isEmpty())) {
						// 1. 新增对象多于1个会失败
						// 2. 同时存在新增或更新对象，会失败
						throw new RuntimeException("Only one creation or All updating in a Action is permitted!!!");
					}
					
					if(!cStore.isEmpty()) {
						IVoidFunction1<Object> idAdopt_ = idAdopt;
						// transactionProvider.doStorage(() -> {
							Entry<Map<String, Object>, ILocatorMapper> ety = cStore.entrySet().iterator().next();
							Map<String, Object> d = ety.getKey();
							ILocatorMapper driver = ety.getValue();
							driver.createNew(d);
							// 把数据库自增id拿回来。
							if(d.containsKey("__new_id__"))
								idAdopt_.trigger(d.get("__new_id__"));
						// });
					} else {
						// transactionProvider.doStorage(() ->
								uStore.forEach((d, driver) -> {
									// 针对带version的聚合更新，若没能准确更新1个对象，则视为并发冲突
									if(1 != driver.updateByIdAndVersion(d.id, d.version, d.update))
										throw new RuntimeException("Concurrent request conflicted!!! Please retry later.");
						 		});
						//		}));
					}
				}
	
			}

	public void cleanContext() {
		tl.remove();
	}
	
	private final static class IdVersionTuple {
		public final Object id;
		
		public final int version;
		
		public final Map<String, Object> update;

		public IdVersionTuple(Object id, int version, Map<String, Object> update) {
			this.id = id;
			this.version = version;
			this.update = update;
		}
		
		public static IdVersionTuple of(Object id, int version, Map<String, Object> update) {
			return new IdVersionTuple(id, version, update);
		}
	}
	
	public final static String getTypeIdKey(Class<?> type, Object id) {
		return id + "_" + type.getName();
	}
}
