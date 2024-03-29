package com.github.linyuzai.dynamicfeign.factory;

import java.util.Map;
import java.util.Objects;

import com.github.linyuzai.dynamicfeign.initializer.InitializationConfiguration;
import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import com.github.linyuzai.dynamicfeign.proxy.DynamicFeignProxy;
import com.github.linyuzai.dynamicfeign.register.DynamicFeignClientsRegistrar;
import com.github.linyuzai.dynamicfeign.targeter.FakeTargeter;
import com.github.linyuzai.dynamicfeign.targeter.Targeter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.cloud.openfeign.FeignLoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.Target.HardCodedTarget;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

public class DynamicFeignClientFactoryBean<F> implements FactoryBean<F>, InitializingBean,
        ApplicationContextAware {
    /***********************************
     * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some lifecycle race condition.
     ***********************************/

    private Class<F> type;

    private String name;

    private String url;

    private String path;

    private boolean decode404;

    private ApplicationContext applicationContext;

    private Class<?> fallback = void.class;

    private Class<?> fallbackFactory = void.class;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(this.name, "Name must be set");
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.applicationContext = context;
    }

    protected Feign.Builder feign(FeignContext context) {
        FeignLoggerFactory loggerFactory = get(context, FeignLoggerFactory.class);
        Logger logger = loggerFactory.create(this.type);

        // @formatter:off
        Feign.Builder builder = get(context, Feign.Builder.class)
                // required values
                .logger(logger)
                .encoder(InitializationConfiguration.global().getEncoderWrapper().wrapper(name, type, get(context, Encoder.class)))
                .decoder(InitializationConfiguration.global().getDecoderWrapper().wrapper(name, type, get(context, Decoder.class)))
                .contract(get(context, Contract.class));
        // @formatter:on

        configureFeign(context, builder);

        return builder;
    }

    protected void configureFeign(FeignContext context, Feign.Builder builder) {
        FeignClientProperties properties = applicationContext.getBean(FeignClientProperties.class);
        if (properties != null) {
            if (properties.isDefaultToProperties()) {
                configureUsingConfiguration(context, builder);
                configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
                configureUsingProperties(properties.getConfig().get(this.name), builder);
            } else {
                configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
                configureUsingProperties(properties.getConfig().get(this.name), builder);
                configureUsingConfiguration(context, builder);
            }
        } else {
            configureUsingConfiguration(context, builder);
        }
    }

    protected void configureUsingConfiguration(FeignContext context, Feign.Builder builder) {
        Logger.Level level = getOptional(context, Logger.Level.class);
        if (level != null) {
            builder.logLevel(level);
        }
        Retryer retryer = getOptional(context, Retryer.class);
        if (retryer != null) {
            builder.retryer(retryer);
        }
        ErrorDecoder errorDecoder = getOptional(context, ErrorDecoder.class);
        if (errorDecoder != null) {
            builder.errorDecoder(errorDecoder);
        }
        Request.Options options = getOptional(context, Request.Options.class);
        if (options != null) {
            builder.options(options);
        }
        Map<String, RequestInterceptor> requestInterceptors = context.getInstances(
                this.name, RequestInterceptor.class);
        if (requestInterceptors != null) {
            builder.requestInterceptors(requestInterceptors.values());
        }

        if (decode404) {
            builder.decode404();
        }
    }

    protected void configureUsingProperties(FeignClientProperties.FeignClientConfiguration config, Feign.Builder builder) {
        if (config == null) {
            return;
        }

        if (config.getLoggerLevel() != null) {
            builder.logLevel(config.getLoggerLevel());
        }

        if (config.getConnectTimeout() != null && config.getReadTimeout() != null) {
            builder.options(new Request.Options(config.getConnectTimeout(), config.getReadTimeout()));
        }

        if (config.getRetryer() != null) {
            Retryer retryer = getOrInstantiate(config.getRetryer());
            builder.retryer(retryer);
        }

        if (config.getErrorDecoder() != null) {
            ErrorDecoder errorDecoder = getOrInstantiate(config.getErrorDecoder());
            builder.errorDecoder(errorDecoder);
        }

        if (config.getRequestInterceptors() != null && !config.getRequestInterceptors().isEmpty()) {
            // this will add request interceptor to builder, not replace existing
            for (Class<RequestInterceptor> bean : config.getRequestInterceptors()) {
                RequestInterceptor interceptor = getOrInstantiate(bean);
                builder.requestInterceptor(interceptor);
            }
        }

        if (config.getDecode404() != null) {
            if (config.getDecode404()) {
                builder.decode404();
            }
        }

        if (Objects.nonNull(config.getEncoder())) {
            builder.encoder(getOrInstantiate(config.getEncoder()));
        }

        if (Objects.nonNull(config.getDecoder())) {
            builder.decoder(getOrInstantiate(config.getDecoder()));
        }

        if (Objects.nonNull(config.getContract())) {
            builder.contract(getOrInstantiate(config.getContract()));
        }
    }

    private <T> T getOrInstantiate(Class<T> tClass) {
        try {
            return applicationContext.getBean(tClass);
        } catch (NoSuchBeanDefinitionException e) {
            return BeanUtils.instantiateClass(tClass);
        }
    }

    protected <T> T get(FeignContext context, Class<T> type) {
        T instance = context.getInstance(this.name, type);
        if (instance == null) {
            throw new IllegalStateException("No bean found of type " + type + " for "
                    + this.name);
        }
        return instance;
    }

    protected <T> T getOptional(FeignContext context, Class<T> type) {
        return context.getInstance(this.name, type);
    }

    protected <T> T loadBalance(Feign.Builder builder, FeignContext context,
                                HardCodedTarget<T> target) {
        Client client = getOptional(context, Client.class);
        if (client != null) {
            builder.client(client);
            Targeter targeter = get(context, Targeter.class);
            return targeter.target(this, builder, context, target);
        }

        throw new IllegalStateException(
                "No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-netflix-ribbon?");
    }

    @Override
    public F getObject() throws Exception {
        return getTarget();
    }

    /**
     * @return a {@link Feign} client created with the specified data and the context information
     */
    F getTarget() throws ClassNotFoundException {
        FeignContext context = applicationContext.getBean(FeignContext.class);
        Feign.Builder builder = feign(context);
        Client client = getOptional(context, Client.class);
        Targeter targeter = FakeTargeter.fake(get(context, Class.forName("org.springframework.cloud.openfeign.Targeter")));

        DynamicFeignClientMapper.ConfigurableFeignClient feignClient =
                new DynamicFeignClientMapper.ConfigurableFeignClient(context, builder, client, targeter, this);

        feignClient.getEntity().setKey(this.name);
        feignClient.getEntity().setType(this.type);
        feignClient.getEntity().setInUrl("http://" + this.name);

        DynamicFeignClientsRegistrar.getDynamicFeignInitializer().initialize(feignClient.getEntity());
        DynamicFeignClientMapper.add(feignClient);
        return new DynamicFeignProxy<>(type, feignClient).get();
    }

    private String cleanPath() {
        String path = this.path.trim();
        if (StringUtils.hasLength(path)) {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
        }
        return path;
    }

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<F> type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isDecode404() {
        return decode404;
    }

    public void setDecode404(boolean decode404) {
        this.decode404 = decode404;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public Class<?> getFallback() {
        return fallback;
    }

    public void setFallback(Class<?> fallback) {
        this.fallback = fallback;
    }

    public Class<?> getFallbackFactory() {
        return fallbackFactory;
    }

    public void setFallbackFactory(Class<?> fallbackFactory) {
        this.fallbackFactory = fallbackFactory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicFeignClientFactoryBean that = (DynamicFeignClientFactoryBean) o;
        return Objects.equals(applicationContext, that.applicationContext) &&
                decode404 == that.decode404 &&
                Objects.equals(fallback, that.fallback) &&
                Objects.equals(fallbackFactory, that.fallbackFactory) &&
                Objects.equals(name, that.name) &&
                Objects.equals(path, that.path) &&
                Objects.equals(type, that.type) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationContext, decode404, fallback, fallbackFactory,
                name, path, type, url);
    }

    @Override
    public String toString() {
        return "DynamicFeignClientFactoryBean{" +
                "type=" + type + ", " +
                "name='" + name + "', " +
                "url='" + url + "', " +
                "path='" + path + "', " +
                "decode404=" + decode404 + ", " +
                "applicationContext=" + applicationContext + ", " +
                "fallback=" + fallback + ", " +
                "fallbackFactory=" + fallbackFactory +
                "}";
    }

}
