package com.github.kimffy24.uow.annotation;

import java.lang.annotation.*;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoCommit {
    String value() default "";
}
