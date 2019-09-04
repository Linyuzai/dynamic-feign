package com.github.linyuzai.dynamicfeign.initializer;

import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

import java.util.Map;

public class DefaultDynamicFeignInitializer implements DynamicFeignInitializer {

    private DynamicFeignInitializer dynamicFeignInitializer;
    private PropertiesDynamicFeignInitializer propertiesDynamicFeignInitializer;

    public DefaultDynamicFeignInitializer(Class<? extends DynamicFeignInitializer> cls) {
        try {
            dynamicFeignInitializer = cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        propertiesDynamicFeignInitializer = new PropertiesDynamicFeignInitializer();
    }

    @Override
    public void config(PropertyResolver resolver, Map<String, Object> attrs) {
        dynamicFeignInitializer.config(resolver, attrs);
        propertiesDynamicFeignInitializer.config(resolver, attrs);
    }

    @Override
    public void initialize(DynamicFeignClientMapper.ConfigurableFeignClientEntity entity) {
        entity.setFeignOut(InitializationConfiguration.global().isFeignOut());
        entity.setFeignMethod(InitializationConfiguration.global().isFeignMethod());
        dynamicFeignInitializer.initialize(entity);
        propertiesDynamicFeignInitializer.initialize(entity);
        if (entity.getOutUrl() == null) {
            String defaultGlobalOutUrl = InitializationConfiguration.global().getOutUrl();
            if (StringUtils.hasText(defaultGlobalOutUrl)) {
                if (!defaultGlobalOutUrl.endsWith("/")) {
                    defaultGlobalOutUrl += "/";
                }
                switch (InitializationConfiguration.global().getUrlConcat()) {
                    case SERVICE_LOWER_CASE:
                        defaultGlobalOutUrl += entity.getKey().toLowerCase();
                        break;
                    case SERVICE_UPPER_CASE:
                        defaultGlobalOutUrl += entity.getKey().toUpperCase();
                        break;
                    case NONE:
                        break;
                }
                entity.setOutUrl(defaultGlobalOutUrl);
            }
        }
    }

}
