package com.github.kimffy24.uow.annotation;

import java.lang.annotation.*;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 不要再使用了。
 * @deprecated 在select时无法对位
 * @author kimffy
 *
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MappingColumn {
    String value();
}
