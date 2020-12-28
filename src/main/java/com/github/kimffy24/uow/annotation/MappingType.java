package com.github.kimffy24.uow.annotation;

import java.lang.annotation.*;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MappingType {
	
	/**
	 * 映射的jdbcType
	 * @return
	 */
    String jdbcType() default "";
    
    /**
     * 数据表上的类型
     * @return
     */
    String tableType() default "";
    
    String tableAttr() default "DEFAULT NULL";
}
