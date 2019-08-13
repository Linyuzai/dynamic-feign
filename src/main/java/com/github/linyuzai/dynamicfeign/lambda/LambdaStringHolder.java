package com.github.linyuzai.dynamicfeign.lambda;

import java.io.Serializable;

@FunctionalInterface
public interface LambdaStringHolder<T, R> extends Serializable {
    R apply(T t, String string);
}
