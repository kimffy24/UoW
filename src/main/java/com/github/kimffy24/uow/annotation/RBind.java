package com.github.kimffy24.uow.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.github.kimffy24.uow.export.mapper.ILocatorMapper;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RBind {

    Class<? extends ILocatorMapper> value();
	
}
