package com.github.linyuzai.dynamicfeign.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
//@Target(ElementType.TYPE)
@Documented
public @interface MethodMappingInitialization {

    String name();

    String url();
}
