package com.github.kimffy24.uow;

import static pro.jk.ejoker.common.system.enhance.StringUtilx.fmt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.github.kimffy24.uow.annotation.RBind;
import com.github.kimffy24.uow.mapper.ILocatorMapper;

import pro.jk.ejoker.common.system.enhance.MapUtilx;

public class SpringUoWMapperBinder implements ApplicationContextAware {
	
	private final Map<Class<?>, ILocatorMapper> mapperStore = new ConcurrentHashMap<>();

	private ApplicationContext applicationContext;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	public ILocatorMapper getAggrMapper(Class<?> aggrType) {
		ILocatorMapper iLocatorMapper = mapperStore.get(aggrType);
		if(null == iLocatorMapper) {
			iLocatorMapper = MapUtilx.getOrAdd(mapperStore, aggrType, this::getAggrMapperInner);
		}
		return iLocatorMapper;
	}
	
	private ILocatorMapper getAggrMapperInner(Class<?> aggrType) {
		RBind rBind = aggrType.getAnnotation(RBind.class);
		if(null == rBind) {
			throw new RuntimeException(fmt("Coundn't found any MapperType for AggregateType[{}] !!!", aggrType.getName()));
		}
		Class<? extends ILocatorMapper> mapperType = rBind.value();
		if(null == mapperType) {
			throw new RuntimeException(fmt("No any MapperType was detected for AggregateType[{}] !!!", aggrType.getName()));
		}
		ILocatorMapper bean = applicationContext.getBean(mapperType);
		if(null == bean) {
			throw new RuntimeException(fmt("Coundn't found any instance for MapperType[{}] !!!", mapperType.getName()));
		}
		return bean;
	}
	
}
