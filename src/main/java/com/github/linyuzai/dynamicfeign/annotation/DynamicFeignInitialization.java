package com.github.linyuzai.dynamicfeign.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
//@Target(ElementType.TYPE)
@Documented
public @interface DynamicFeignInitialization {

    String name() default "";

    Class<?> type() default void.class;

    String outUrl() default "";

    boolean feignOut() default false;

    boolean feignMethod() default false;

    MethodMappingInitialization[] methodMapping() default {};
}
