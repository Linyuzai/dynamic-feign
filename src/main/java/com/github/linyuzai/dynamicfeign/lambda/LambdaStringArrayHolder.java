package com.github.linyuzai.dynamicfeign.lambda;

import java.io.Serializable;

@FunctionalInterface
public interface LambdaStringArrayHolder<T, R> extends Serializable {
    R apply(T t, String[] strings);
}
