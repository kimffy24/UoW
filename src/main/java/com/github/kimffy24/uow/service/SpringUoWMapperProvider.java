package com.github.kimffy24.uow.service;

import static pro.jk.ejoker.common.system.enhance.StringUtilx.fmt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import com.github.kimffy24.uow.annotation.RBind;
import com.github.kimffy24.uow.export.mapper.ILocatorMapper;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import pro.jk.ejoker.common.system.enhance.MapUtilx;
import pro.jk.ejoker.common.system.task.io.IOExceptionOnRuntime;

public class SpringUoWMapperProvider implements ApplicationContextAware {
	
	private final Map<Class<?>, ILocatorMapper> mapperStore = new ConcurrentHashMap<>();

	private ApplicationContext applicationContext;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	/**
	 * 从给定的聚合根类，找出对应的mapper对象<br />
	 * * 从聚合根类找出RBind注解，从注解中找出声明的Mapper类的，再从spring上下文中找出该Mapper类的实例对象。
	 * @param aggrType
	 * @return
	 */
	public ILocatorMapper getAggrMapper(Class<?> aggrType) {
		ILocatorMapper iLocatorMapper = mapperStore.get(aggrType);
		if(null == iLocatorMapper) {
			iLocatorMapper = MapUtilx.getOrAdd(mapperStore, aggrType, this::getAggrMapperInner);
		}
		return iLocatorMapper;
	}
	
	private ILocatorMapper getAggrMapperInner(Class<?> aggrType) {
		Class<? extends ILocatorMapper> mapperType;
		{
			RBind rBind = aggrType.getAnnotation(RBind.class);
			if(null == rBind) {
				String mapperGenType;
				if(StringUtils.hasLength(mapperGenType = loadUoWGenBindInfoJson(aggrType.getName()))) {
					try {
						Class<?> genMapperType = this.getClass().getClassLoader().loadClass(mapperGenType);
						mapperType = (Class<? extends ILocatorMapper> )genMapperType;
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(fmt("Coundn't found any MapperType for AggregateType[{}] !!! load uow gen Mapper Type faild.", aggrType.getName()), e);
					}
				} else {
					throw new RuntimeException(fmt("Coundn't found any MapperType for AggregateType[{}] !!!", aggrType.getName()));
				}
			} else {
				mapperType = rBind.value();
			}
		}
		
		if(null == mapperType) {
			throw new RuntimeException(fmt("No any MapperType was detected for AggregateType[{}] !!!", aggrType.getName()));
		}
		
		ILocatorMapper bean = applicationContext.getBean(mapperType);
		if(null == bean) {
			throw new RuntimeException(fmt("Coundn't found any instance for MapperType[{}] !!!", mapperType.getName()));
		}
		return bean;
	}
	
	private final static String UoWGenRBindInfoJsonPath = "/uow-gen-rbind-info.json";
	
	private volatile Map<String, String> uowGenRBindInfo = null;
	
	private String loadUoWGenBindInfoJson(String uowClazz) {
		if(null == uowGenRBindInfo) {
			synchronized(UoWGenRBindInfoJsonPath) {
				if(null == uowGenRBindInfo) {
					ClassPathResource resource = new ClassPathResource(UoWGenRBindInfoJsonPath);
					if(!resource.exists()) {
						uowGenRBindInfo = new HashMap<>();
					} else {
						try (InputStream inputStream = resource.getInputStream()) {
//							ByteArrayOutputStream result = new ByteArrayOutputStream();
//							byte[] buffer = new byte[1024];
//							int length;
//							while ((length = inputStream.read(buffer)) != -1) {
//							    result.write(buffer, 0, length);
//							}
							Object parse = JSONValue.parse(inputStream);
							if(parse instanceof Map) {
								uowGenRBindInfo = (Map<String, String> )parse;
							} else {
								throw new RuntimeException("uow-gen-rbind-info.json has wrong format !!!");
							}
						} catch (IOException e) {
							throw new IOExceptionOnRuntime(e);
						}
					}
				}
			}
		} 
		return uowGenRBindInfo.get(uowClazz);
	}
}
