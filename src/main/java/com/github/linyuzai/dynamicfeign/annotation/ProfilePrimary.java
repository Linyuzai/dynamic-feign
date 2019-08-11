package com.github.linyuzai.dynamicfeign.annotation;

import java.lang.annotation.*;

/**
 * 配置文件优先
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ProfilePrimary {
}
