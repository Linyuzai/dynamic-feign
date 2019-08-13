package com.github.linyuzai.dynamicfeign.lambda;

@FunctionalInterface
public interface LambdaNoneVoidHolder<T> {
    void apply(T t);
}
