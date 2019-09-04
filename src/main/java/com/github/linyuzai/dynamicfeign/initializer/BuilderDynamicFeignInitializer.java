package com.github.linyuzai.dynamicfeign.initializer;

import com.github.linyuzai.dynamicfeign.concat.UrlConcat;
import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import com.github.linyuzai.dynamicfeign.wrapper.DecoderWrapper;
import com.github.linyuzai.dynamicfeign.wrapper.EncoderWrapper;
import org.springframework.core.env.PropertyResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public abstract class BuilderDynamicFeignInitializer implements DynamicFeignInitializer {

    private BuilderGroup builderGroup = new BuilderGroup();


    @Override
    public void config(PropertyResolver resolver, Map<String, Object> attrs) {
        build();
    }

    @Override
    public void initialize(DynamicFeignClientMapper.ConfigurableFeignClientEntity entity) {
        //TODO

    }

    public BuilderGroup global() {
        return builderGroup;
    }

    public Builder ofType(Class<?> type) {
        Builder builder = new Builder();
        builderGroup.builderMap.put(type.getName(), builder);
        return builder;
    }

    public Builder ofName(String name) {
        Builder builder = new Builder();
        builderGroup.builderMap.put(name, builder);
        return builder;
    }

    public abstract void build();

    public static class Builder {
        private String outUrl;
        private boolean feignOut;
        private boolean feignMethod;
        private Map<String, String> methodUrls = new ConcurrentHashMap<>();

        public Builder outUrl(String outUrl) {
            this.outUrl = outUrl;
            return this;
        }

        public Builder feignOut(boolean feignOut) {
            this.feignOut = feignOut;
            return this;
        }

        public Builder feignMethod(boolean feignMethod) {
            this.feignMethod = feignMethod;
            return this;
        }

        public Builder mappingMethod(String methodName, String url) {
            methodUrls.put(methodName, url);
            return this;
        }
    }

    public static class BuilderGroup {
        private String outUrl;

        private UrlConcat urlConcat;

        private EncoderWrapper encoderWrapper;

        private DecoderWrapper decoderWrapper;

        private Map<String, Builder> builderMap = new ConcurrentHashMap<>();

        public BuilderGroup outUrl(String outUrl) {
            this.outUrl = outUrl;
            return this;
        }

        public BuilderGroup urlConcat(UrlConcat urlConcat) {
            this.urlConcat = urlConcat;
            return this;
        }

        public BuilderGroup encoderWrapper(EncoderWrapper encoderWrapper) {
            this.encoderWrapper = encoderWrapper;
            return this;
        }

        public BuilderGroup decoderWrapper(DecoderWrapper decoderWrapper) {
            this.decoderWrapper = decoderWrapper;
            return this;
        }
    }
}
