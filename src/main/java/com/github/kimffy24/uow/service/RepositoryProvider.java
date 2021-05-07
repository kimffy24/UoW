package com.github.kimffy24.uow.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.kimffy24.uow.component.StdConverter;
import com.github.kimffy24.uow.export.skeleton.AbstractAggregateRoot;
import com.github.kimffy24.uow.repos.Repository;
import com.github.kimffy24.uow.util.KeyMapperStore;
import com.github.kimffy24.uow.util.KeyMapperStore.Item;
import com.github.kimffy24.uow.util.KeyMapperStore.KMStore;

import pro.jk.ejoker.common.system.enhance.MapUtilx;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;

public class RepositoryProvider {
	
	private final static KMStore KMInstance = KeyMapperStore.getIntance();
	
	private StdConverter stdConverter = StdConverter.getInstance();
	
	/**
	 *  数据库直接出的类型无法转java基本类型，导致报错，释放出使用自定义的转换方法的入口<br />
	 * * key: 目标聚合根类
	 * * value: 用户提供的副作用方法，入参为从数据去除的原始数据，用户侧只需要找到目标key，并修改对应的value即可，对其他不要管。
	 */
	private final Map<Class<?>, IVoidFunction1<Map<String, Object>>> preModifierStore = new ConcurrentHashMap<>();
	
	@Autowired
	private SpringUoWMapperProvider springUoWMapperBinder;
	
	private final Map<
				Class<? extends AbstractAggregateRoot<?>>,
				Repository<? extends AbstractAggregateRoot<?>>> reposStore =  new ConcurrentHashMap<>();
	
	@SuppressWarnings("unchecked")
	public <A extends AbstractAggregateRoot<?>> Repository<A> getRepos(Class<A> aggrType) {
		return (Repository<A> )MapUtilx.getOrAdd(reposStore, aggrType, t -> {
			List<Item> anaResult = KMInstance.getAnaResult(t);
			return new Repository<>(stdConverter, springUoWMapperBinder, () -> preModifierStore.get(t), t, anaResult);
		});
	}
	
	public void registerOncePreModifier(Class<?> t, IVoidFunction1<Map<String, Object>> m) {
		IVoidFunction1<Map<String, Object>> previous = this.preModifierStore.putIfAbsent(t, m);
		if(null != previous) {
			throw new RuntimeException(StringUtilx.fmt("Multi preModifier is set!!! [type: {}]", t.getName()));
		}
	}
//	
// 	public final class Repository<T extends AbstractAggregateRoot<?>> {
//		
//		private final Class<T> aggrRootType;
//		
//		public Repository(Class<T> aggrRootType) {
//			this.aggrRootType = aggrRootType;
//		}
//		
//		private T revertToAggr(Map<String, Object> originalDict) {
//			T revert = stdConverter.revert(originalDict, this.aggrRootType);
//			AggregateActionBinder.adoptOriginalDict(revert, stdConverter.convert(revert));
//			return revert;
//		}
//		
//		public T fetch(Object id) {
//			if(null == id)
//				return null;
//			Map<String, Object> reMap = new HashMap<>();
//			{
//				ILocatorMapper provideLocatorMapper = springUoWMapperBinder.getAggrMapper(aggrRootType);
//				Map<String, Object> originalDict = provideLocatorMapper.fetchById(id);
//				if(null == originalDict)
//					return null;
//				List<Item> anaResult = KMInstance.getAnaResult(aggrRootType);
//				for(Item i : anaResult) {
//					Object sqlValue = originalDict.get(i.sqlClName);
//					if(null != sqlValue)
//						reMap.put(i.key, sqlValue);
//				}
//			}
//			
//			IVoidFunction1<Map<String, Object>> preModifier = RepositoryProvider.this.preModifierStore.get(this.aggrRootType);
//			if(null != preModifier) {
//				preModifier.trigger(reMap);
//			}
//			
//			Iterator<Map.Entry<String, Object>> iterator = reMap.entrySet().iterator();
//			while(iterator.hasNext()) {
//				Map.Entry<String, Object> entry = iterator.next();
//				Object value = entry.getValue();
//				// 针对mysql时间类型作调整
//				if(value instanceof Timestamp)
//					entry.setValue(new Date(((Timestamp )value).getTime()));
//				if("id".equals(entry.getKey())) {
//					// mysql驱动返回的类型，不一定跟java内的一致
//					entry.setValue(id);
//				}
//			}
//	
//			return revertToAggr(reMap);
//		}
//	
//		/**
//		 * 按照match中的key=value多个条件，捞出符合条件的实例对象<br />
//		 * @deprecated 请尽量通过id捞对象数据
//		 * @param match
//		 * @return
//		 */
//		public List<T> fetchMatched(Map<String, Object> match) {
//			ILocatorMapper provideLocatorMapper = springUoWMapperBinder.getAggrMapper(aggrRootType);
//			List<Map<String, Object>> mi = provideLocatorMapper.locateId(match);
//			if(null == mi || mi.isEmpty())
//				return new ArrayList<>();
//			return mi.stream().map(i -> Repository.this.fetch(i.get("id"))).collect(Collectors.toList());
//		}
//		
//	//	public void add(T aggr) {
//	//		Map<String, Object> convert = stdConverter.convert(aggr);
//	//		// 由于spring三层架构的基因问题 ，不好支持， Repository 的 收录对象的功能有context承担.
//	//	}
////	
////		public Class<?> getIdType() {
////			return aggrRootType;
////		}
//		
//		public Map<String, Object> getAggrOriginalDict(AbstractAggregateRoot<?> aggr) {
//			return AggregateActionBinder.getOriginalDict(aggr);
//		}
//		
//		public ILocatorMapper provideLocatorMapper() {
//			return springUoWMapperBinder.getAggrMapper(aggrRootType);
//		}
//	}
}
