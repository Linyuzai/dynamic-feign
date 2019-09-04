package com.github.linyuzai.dynamicfeign.initializer;

import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import org.springframework.core.env.PropertyResolver;

import java.util.Map;

public interface DynamicFeignInitializer {

    void config(PropertyResolver resolver, Map<String, Object> attrs);

    void initialize(DynamicFeignClientMapper.ConfigurableFeignClientEntity entity);
}
