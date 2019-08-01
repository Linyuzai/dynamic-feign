package com.github.linyuzai.dynamicfeign.proxy;

import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DynamicFeignProxy<T> implements InvocationHandler {

    private Class<T> feignInterface;

    private DynamicFeignClientMapper.ConfigurableFeignClient feignClient;

    public DynamicFeignProxy(Class<T> feignInterface, DynamicFeignClientMapper.ConfigurableFeignClient feignClient) {
        if (feignInterface == null) {
            throw new RuntimeException("Dynamic feign interface is null");
        }
        if (feignClient == null) {
            throw new RuntimeException("Dynamic feign client is null");
        }
        this.feignInterface = feignInterface;
        this.feignClient = feignClient;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        return (T) Proxy.newProxyInstance(feignInterface.getClassLoader(), new Class[]{feignInterface}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(feignClient.dynamic(method), args);
    }
}
