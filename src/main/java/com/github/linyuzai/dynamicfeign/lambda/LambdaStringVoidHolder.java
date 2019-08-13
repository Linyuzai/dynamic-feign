package com.github.linyuzai.dynamicfeign.lambda;

@FunctionalInterface
public interface LambdaStringVoidHolder<T> {
    void apply(T t, String string);
}
