package com.github.linyuzai.dynamicfeign.lambda;

import java.util.Map;

@FunctionalInterface
public interface LambdaMapVoidHolder<T> {
    void apply(T t, Map map);
}
