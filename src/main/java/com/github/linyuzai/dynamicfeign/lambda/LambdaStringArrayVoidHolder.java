package com.github.linyuzai.dynamicfeign.lambda;

@FunctionalInterface
public interface LambdaStringArrayVoidHolder<T> {
    void apply(T t, String[] strings);
}
