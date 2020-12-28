package com.github.kimffy24.uow.repos;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.kimffy24.uow.SpringUoWMapperBinder;
import com.github.kimffy24.uow.component.StdConverter;
import com.github.kimffy24.uow.mapper.ILocatorMapper;
import com.github.kimffy24.uow.skeleton.AbstractAggregateRoot;
import com.github.kimffy24.uow.skeleton.AggregateActionBinder;
import com.github.kimffy24.uow.util.KeyMapperStore;
import com.github.kimffy24.uow.util.KeyMapperStore.Item;
import com.github.kimffy24.uow.util.KeyMapperStore.KMStore;

import pro.jk.ejoker.common.system.enhance.MapUtilx;

public class RepositoryHub {
	
	private final static KMStore KMInstance = KeyMapperStore.getIntance();
	
	private StdConverter stdConverter = StdConverter.getInstance();
	
	@Autowired
	private SpringUoWMapperBinder springUoWMapperBinder;
	
	private final Map<
				Class<? extends AbstractAggregateRoot<?>>,
				Repository<? extends AbstractAggregateRoot<?>>> reposStore =  new ConcurrentHashMap<>();
	
	@SuppressWarnings("unchecked")
	public <A extends AbstractAggregateRoot<?>> Repository<A> getRepos(Class<A> aggrType) {
		return (Repository<A> )MapUtilx.getOrAdd(reposStore, aggrType, () -> new Repository<>(aggrType));
	}
	
 	public final class Repository<T extends AbstractAggregateRoot<?>> {
		
		private final Class<T> aggrRootType;
		
		public Repository(Class<T> aggrRootType) {
			this.aggrRootType = aggrRootType;
		}
		
		private T revertToAggr(Map<String, Object> originalDict) {
			T revert = stdConverter.revert(originalDict, this.aggrRootType);
			AggregateActionBinder.adoptOriginalDict(revert, stdConverter.convert(revert));
			return revert;
		}
		
		public T fetch(Object id) {
			if(null == id)
				return null;
			Map<String, Object> reMap = new HashMap<>();
			{
				ILocatorMapper provideLocatorMapper = springUoWMapperBinder.getAggrMapper(aggrRootType);
				Map<String, Object> originalDict = provideLocatorMapper.fetchById(id);
				if(null == originalDict)
					return null;
				List<Item> anaResult = KMInstance.getAnaResult(aggrRootType);
				for(Item i : anaResult) {
					Object sqlValue = originalDict.get(i.sqlClName);
					if(null != sqlValue)
						reMap.put(i.key, sqlValue);
				}
			}
			
			Iterator<Map.Entry<String, Object>> iterator = reMap.entrySet().iterator();
			while(iterator.hasNext()) {
				Map.Entry<String, Object> entry = iterator.next();
				Object value = entry.getValue();
				// 针对mysql时间类型作调整
				if(value instanceof Timestamp)
					entry.setValue(new Date(((Timestamp )value).getTime()));
				if("id".equals(entry.getKey())) {
					// mysql驱动返回的类型，不一定跟java内的一致
					entry.setValue(id);
				}
			}
	
			return revertToAggr(reMap);
		}
	
		public List<T> fetchMatched(Map<String, Object> match) {
			ILocatorMapper provideLocatorMapper = springUoWMapperBinder.getAggrMapper(aggrRootType);
			List<Map<String, Object>> mi = provideLocatorMapper.locateId(match);
			if(null == mi || mi.isEmpty())
				return new ArrayList<>();
			return mi.stream().map(i -> Repository.this.fetch(i.get("id"))).collect(Collectors.toList());
		}
		
	//	public void add(T aggr) {
	//		Map<String, Object> convert = stdConverter.convert(aggr);
	//		// 由于spring三层架构的基因问题 ，不好支持， Repository 的 收录对象的功能有context承担.
	//	}
	
		public Class<?> getIdType() {
			return aggrRootType;
		}
		
		public Map<String, Object> getAggrOriginalDict(AbstractAggregateRoot<?> aggr) {
			return AggregateActionBinder.getOriginalDict(aggr);
		}
		
		public ILocatorMapper provideLocatorMapper() {
			return springUoWMapperBinder.getAggrMapper(aggrRootType);
		}
	}
}
