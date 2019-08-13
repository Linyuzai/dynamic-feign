package com.github.linyuzai.dynamicfeign.lambda;

import java.io.Serializable;
import java.util.Map;

@FunctionalInterface
public interface LambdaMapHolder<T, R> extends Serializable {
    R apply(T t, Map map);
}
