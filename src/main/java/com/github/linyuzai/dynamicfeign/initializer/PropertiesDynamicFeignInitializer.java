package com.github.linyuzai.dynamicfeign.initializer;

import com.github.linyuzai.dynamicfeign.concat.UrlConcat;
import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import com.github.linyuzai.dynamicfeign.resolver.RelaxedPropertyResolver;
import com.github.linyuzai.dynamicfeign.wrapper.DecoderWrapper;
import com.github.linyuzai.dynamicfeign.wrapper.EncoderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PropertiesDynamicFeignInitializer implements DynamicFeignInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesDynamicFeignInitializer.class);

    private PropertyResolver resolver;

    @Override
    public void config(PropertyResolver resolver, Map<String, Object> attrs) {
        this.resolver = resolver;
        String outUrlProperty = resolver.getProperty("dynamic-feign.out-url");
        if (outUrlProperty != null) {
            InitializationConfiguration.global().setOutUrl(outUrlProperty);
        }
        String urlConcatProperty = resolver.getProperty("dynamic-feign.url-concat");
        if (urlConcatProperty != null) {
            try {
                int o = Integer.parseInt(urlConcatProperty);
                InitializationConfiguration.global().setUrlConcat(UrlConcat.values()[o]);
            } catch (Exception e) {
                try {
                    InitializationConfiguration.global().setUrlConcat(UrlConcat.valueOf(urlConcatProperty));
                } catch (Exception ignore) {
                }
            }
        }
        String feignOutProperty = resolver.getProperty("dynamic-feign.feign-out");
        if (feignOutProperty != null) {
            try {
                InitializationConfiguration.global().setFeignOut(Boolean.valueOf(feignOutProperty));
            } catch (Exception e) {
                logger.error("dynamic-feign.feign-out only accept true or false");
            }
        }
        String feignMethodProperty = resolver.getProperty("dynamic-feign.feign-method");
        if (feignMethodProperty != null) {
            try {
                InitializationConfiguration.global().setFeignMethod(Boolean.valueOf(feignMethodProperty));
            } catch (Exception e) {
                logger.error("dynamic-feign.feign-method only accept true or false");
            }
        }
        String encoderWrapperProperty = resolver.getProperty("dynamic-feign.encoder-wrapper");
        if (encoderWrapperProperty != null) {
            try {
                InitializationConfiguration.global().setEncoderWrapper((EncoderWrapper) Class.forName(encoderWrapperProperty).newInstance());
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error(encoderWrapperProperty + " newInstance failure with " + e.getMessage());
            }
        }
        String decoderWrapperProperty = resolver.getProperty("dynamic-feign.decoder-wrapper");
        if (decoderWrapperProperty != null) {
            try {
                InitializationConfiguration.global().setDecoderWrapper((DecoderWrapper) Class.forName(decoderWrapperProperty).newInstance());
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error(decoderWrapperProperty + " newInstance failure with " + e.getMessage());
            }
        }
    }

    @Override
    public void initialize(DynamicFeignClientMapper.ConfigurableFeignClientEntity entity) {
        RelaxedPropertyResolver dynamicFeign = new RelaxedPropertyResolver(resolver);
        Map<String, Object> dynamicFeignMap = dynamicFeign.getSubProperties("dynamic-feign." + entity.getKey() + ".");
        if (dynamicFeignMap == null || dynamicFeignMap.isEmpty()) {
            return;
        }
        String outUrl = (String) dynamicFeignMap.get("out-url");
        if (StringUtils.hasText(outUrl)) {
            entity.setOutUrl(outUrl);
            entity.setFeignOut(true);
        }

        String feignOutProperty = (String) dynamicFeignMap.get("feign-out");
        if (StringUtils.hasText(feignOutProperty)) {
            try {
                entity.setFeignOut(Boolean.parseBoolean(feignOutProperty));
            } catch (Exception ignore) {
            }
        }
        String feignMethod = (String) dynamicFeignMap.get("feign-method");
        if (StringUtils.hasText(feignMethod)) {
            try {
                entity.setFeignMethod(Boolean.parseBoolean(feignMethod));
            } catch (Exception ignore) {
            }
        }
        RelaxedPropertyResolver methodMapping = new RelaxedPropertyResolver(resolver);
        Map<String, Object> methodMappingMap = methodMapping.getSubProperties("dynamic-feign." + entity.getKey() + ".method-mapping.");
        if (methodMappingMap != null && !methodMappingMap.isEmpty()) {
            Map<String, String> methodUrls = new ConcurrentHashMap<>();
            for (Map.Entry<String, Object> entry : methodMappingMap.entrySet()) {
                methodUrls.put(entry.getKey(), (String) entry.getValue());
            }
            entity.setMethodOutUrls(methodUrls);
            if (!StringUtils.hasText(feignMethod)) {
                entity.setFeignMethod(true);
            }
        }
    }
}
