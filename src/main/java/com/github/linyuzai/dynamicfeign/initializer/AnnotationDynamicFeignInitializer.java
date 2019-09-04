package com.github.linyuzai.dynamicfeign.initializer;

import com.github.linyuzai.dynamicfeign.annotation.DynamicFeignInitialization;
import com.github.linyuzai.dynamicfeign.annotation.MethodMappingInitialization;
import com.github.linyuzai.dynamicfeign.concat.UrlConcat;
import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import com.github.linyuzai.dynamicfeign.wrapper.DecoderWrapper;
import com.github.linyuzai.dynamicfeign.wrapper.EncoderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationDynamicFeignInitializer implements DynamicFeignInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationDynamicFeignInitializer.class);

    private Map<String, Object> attrs;

    @Override
    public void config(PropertyResolver resolver, Map<String, Object> attrs) {
        this.attrs = attrs;
        InitializationConfiguration.global().setOutUrl(attrs == null ? null : (String) attrs.get("outUrl"));
        InitializationConfiguration.global().setUrlConcat(attrs == null ? UrlConcat.SERVICE_LOWER_CASE :
                (UrlConcat) attrs.get("urlConcat"));
        InitializationConfiguration.global().setFeignOut(attrs != null && (boolean) attrs.get("feignOut"));
        InitializationConfiguration.global().setFeignMethod(attrs != null && (boolean) attrs.get("feignMethod"));
        @SuppressWarnings("unchecked")
        Class<? extends EncoderWrapper> encoderWrapper = attrs == null ? EncoderWrapper.Default.class :
                (Class<? extends EncoderWrapper>) attrs.get("encoderWrapper");
        try {
            InitializationConfiguration.global().setEncoderWrapper(encoderWrapper.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(encoderWrapper.getName() + " newInstance failure with " + e.getMessage());
            InitializationConfiguration.global().setEncoderWrapper(new EncoderWrapper.Default());
        }
        @SuppressWarnings("unchecked")
        Class<? extends DecoderWrapper> decoderWrapper = attrs == null ? DecoderWrapper.Default.class :
                (Class<? extends DecoderWrapper>) attrs.get("decoderWrapper");
        try {
            InitializationConfiguration.global().setDecoderWrapper(decoderWrapper.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(decoderWrapper.getName() + " newInstance failure with " + e.getMessage());
            InitializationConfiguration.global().setDecoderWrapper(new DecoderWrapper.Default());
        }
    }

    @Override
    public void initialize(DynamicFeignClientMapper.ConfigurableFeignClientEntity entity) {
        AnnotationAttributes[] dynamicFeignInitializations = (AnnotationAttributes[]) attrs.get("annotationInitializer");
        AnnotationAttributes target = null;
        for (AnnotationAttributes dynamicFeignInitialization : dynamicFeignInitializations) {
            String name = dynamicFeignInitialization.getString("name");
            Class<?> type = dynamicFeignInitialization.getClass("type");
            if (name.equals(entity.getKey()) || type == entity.getType()) {
                target = dynamicFeignInitialization;
                break;
            }
        }
        if (target == null) {
            return;
        }
        entity.setFeignOut(target.getBoolean("feignOut"));
        entity.setFeignMethod(target.getBoolean("feignMethod"));
        String outUrl = target.getString("outUrl");
        if (StringUtils.hasText(outUrl)) {
            entity.setOutUrl(outUrl);
        }
        AnnotationAttributes[] methodMappingInitializations = target.getAnnotationArray("methodMapping");
        if (methodMappingInitializations.length > 0) {
            Map<String, String> methodUrls = new ConcurrentHashMap<>();
            for (AnnotationAttributes methodMappingInitialization : methodMappingInitializations) {
                String name = methodMappingInitialization.getString("name");
                String url = methodMappingInitialization.getString("url");
                methodUrls.put(name, url);
            }
            entity.setMethodOutUrls(methodUrls);
        }
    }

}
