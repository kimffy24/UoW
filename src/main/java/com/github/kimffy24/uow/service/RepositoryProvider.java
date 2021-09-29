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
}
