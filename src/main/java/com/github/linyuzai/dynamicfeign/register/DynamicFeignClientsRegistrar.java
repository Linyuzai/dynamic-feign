package com.github.linyuzai.dynamicfeign.register;

import com.github.linyuzai.dynamicfeign.annotation.EnableDynamicFeignClients;
import com.github.linyuzai.dynamicfeign.concat.UrlConcat;
import com.github.linyuzai.dynamicfeign.factory.DynamicFeignClientFactoryBean;
import com.github.linyuzai.dynamicfeign.wrapper.DecoderWrapper;
import com.github.linyuzai.dynamicfeign.wrapper.EncoderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.openfeign.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class DynamicFeignClientsRegistrar implements ImportBeanDefinitionRegistrar,
        ResourceLoaderAware, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(DynamicFeignClientsRegistrar.class);
    // patterned after Spring Integration IntegrationComponentScanRegistrar
    // and RibbonClientsConfigurationRegistgrar

    private static String defaultGlobalOutUrl;
    private static UrlConcat defaultGlobalUrlConcat;
    private static boolean defaultGlobalFeignOut;
    private static boolean defaultGlobalFeignMethod;
    private static DecoderWrapper decoderWrapper;
    private static EncoderWrapper encoderWrapper;

    private ResourceLoader resourceLoader;

    private Environment environment;

    public DynamicFeignClientsRegistrar() {
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata,
                                        BeanDefinitionRegistry registry) {
        registerDefaultConfiguration(metadata, registry);
        registerFeignClients(metadata, registry);
    }

    public static String getDefaultGlobalOutUrl() {
        return defaultGlobalOutUrl;
    }

    public static UrlConcat getDefaultGlobalUrlConcat() {
        return defaultGlobalUrlConcat;
    }

    public static boolean isDefaultGlobalFeignOut() {
        return defaultGlobalFeignOut;
    }

    public static boolean isDefaultGlobalFeignMethod() {
        return defaultGlobalFeignMethod;
    }

    public static DecoderWrapper getDecoderWrapper() {
        return decoderWrapper;
    }

    public static EncoderWrapper getEncoderWrapper() {
        return encoderWrapper;
    }

    private void registerDefaultConfiguration(AnnotationMetadata metadata,
                                              BeanDefinitionRegistry registry) {
        Map<String, Object> defaultAttrs = metadata
                .getAnnotationAttributes(EnableDynamicFeignClients.class.getName(), true);
        if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
            String name;
            if (metadata.hasEnclosingClass()) {
                name = "default." + metadata.getEnclosingClassName();
            } else {
                name = "default." + metadata.getClassName();
            }
            registerClientConfiguration(registry, name,
                    defaultAttrs.get("defaultConfiguration"));
        }
    }

    private void configurationOutUrl(Map<String, Object> attrs) {
        defaultGlobalOutUrl = attrs == null ? null : (String) attrs.get("outUrl");
        String outUrlProperty = environment.getProperty("dynamic-feign.out-url");
        if (outUrlProperty != null) {
            defaultGlobalOutUrl = outUrlProperty;
        }
    }

    private void configurationUrlConcat(Map<String, Object> attrs) {
        defaultGlobalUrlConcat = attrs == null ? UrlConcat.SERVICE_LOWER_CASE : (UrlConcat) attrs.get("urlConcat");
    }

    private void configurationFeignOut(Map<String, Object> attrs) {
        defaultGlobalFeignOut = attrs != null && (boolean) attrs.get("feignOut");
        String feignOutProperty = environment.getProperty("dynamic-feign.feign-out");
        if (feignOutProperty != null) {
            try {
                defaultGlobalFeignOut = Boolean.valueOf(feignOutProperty);
            } catch (Exception ignore) {
                logger.error("dynamic-feign.feign-out only accept true or false");
            }
        }
    }

    private void configurationFeignMethod(Map<String, Object> attrs) {
        defaultGlobalFeignMethod = attrs != null && (boolean) attrs.get("feignMethod");
        String feignMethodProperty = environment.getProperty("dynamic-feign.feign-method");
        if (feignMethodProperty != null) {
            try {
                defaultGlobalFeignMethod = Boolean.valueOf(feignMethodProperty);
            } catch (Exception ignore) {
                logger.error("dynamic-feign.feign-method only accept true or false");
            }
        }
    }

    private void configurationEncoderWrapper(Map<String, Object> attrs) {
        @SuppressWarnings("unchecked")
        Class<? extends EncoderWrapper> encoderWrapper = attrs == null ? EncoderWrapper.Default.class :
                (Class<? extends EncoderWrapper>) attrs.get("encoderWrapper");
        try {
            DynamicFeignClientsRegistrar.encoderWrapper = encoderWrapper.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(encoderWrapper.getName() + " newInstance failure with " + e.getMessage());
            DynamicFeignClientsRegistrar.encoderWrapper = new EncoderWrapper.Default();
        }
        String encoderWrapperProperty = environment.getProperty("dynamic-feign.encoder-wrapper");
        if (encoderWrapperProperty != null) {
            try {
                DynamicFeignClientsRegistrar.encoderWrapper = (EncoderWrapper) Class.forName(encoderWrapperProperty).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error(encoderWrapperProperty + " newInstance failure with " + e.getMessage());
            }
        }
    }

    private void configurationDecoderWrapper(Map<String, Object> attrs) {
        @SuppressWarnings("unchecked")
        Class<? extends DecoderWrapper> decoderWrapper = attrs == null ? DecoderWrapper.Default.class :
                (Class<? extends DecoderWrapper>) attrs.get("decoderWrapper");
        try {
            DynamicFeignClientsRegistrar.decoderWrapper = decoderWrapper.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(decoderWrapper.getName() + " newInstance failure with " + e.getMessage());
            DynamicFeignClientsRegistrar.decoderWrapper = new DecoderWrapper.Default();
        }
        String decoderWrapperProperty = environment.getProperty("dynamic-feign.decoder-wrapper");
        if (decoderWrapperProperty != null) {
            try {
                DynamicFeignClientsRegistrar.decoderWrapper = (DecoderWrapper) Class.forName(decoderWrapperProperty).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error(decoderWrapperProperty + " newInstance failure with " + e.getMessage());
            }
        }
    }

    public void registerFeignClients(AnnotationMetadata metadata,
                                     BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);

        Set<String> basePackages;

        Map<String, Object> attrs = metadata
                .getAnnotationAttributes(EnableDynamicFeignClients.class.getName());
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(
                FeignClient.class);
        final Class<?>[] clients = attrs == null ? null
                : (Class<?>[]) attrs.get("clients");

        configurationOutUrl(attrs);
        configurationUrlConcat(attrs);
        configurationFeignOut(attrs);
        configurationFeignMethod(attrs);
        configurationEncoderWrapper(attrs);
        configurationDecoderWrapper(attrs);

        if (clients == null || clients.length == 0) {
            scanner.addIncludeFilter(annotationTypeFilter);
            basePackages = getBasePackages(metadata);
        } else {
            final Set<String> clientClasses = new HashSet<>();
            basePackages = new HashSet<>();
            for (Class<?> clazz : clients) {
                basePackages.add(ClassUtils.getPackageName(clazz));
                clientClasses.add(clazz.getCanonicalName());
            }
            AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
                @Override
                protected boolean match(ClassMetadata metadata) {
                    String cleaned = metadata.getClassName().replaceAll("\\$", ".");
                    return clientClasses.contains(cleaned);
                }
            };
            scanner.addIncludeFilter(
                    new DynamicFeignClientsRegistrar.AllTypeFilter(Arrays.asList(filter, annotationTypeFilter)));
        }

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner
                    .findCandidateComponents(basePackage);
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition) {
                    // verify annotated class is an interface
                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                    Assert.isTrue(annotationMetadata.isInterface(),
                            "@FeignClient can only be specified on an interface");

                    Map<String, Object> attributes = annotationMetadata
                            .getAnnotationAttributes(
                                    FeignClient.class.getCanonicalName());

                    String name = getClientName(attributes);
                    registerClientConfiguration(registry, name,
                            attributes.get("configuration"));

                    registerFeignClient(registry, annotationMetadata, attributes);
                }
            }
        }
    }

    private void registerFeignClient(BeanDefinitionRegistry registry,
                                     AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
        String className = annotationMetadata.getClassName();
        BeanDefinitionBuilder definition = BeanDefinitionBuilder
                .genericBeanDefinition(DynamicFeignClientFactoryBean.class);
        validate(attributes);
        definition.addPropertyValue("url", getUrl(attributes));
        definition.addPropertyValue("path", getPath(attributes));
        String name = getName(attributes);
        definition.addPropertyValue("name", name);
        definition.addPropertyValue("type", className);
        definition.addPropertyValue("decode404", attributes.get("decode404"));
        definition.addPropertyValue("fallback", attributes.get("fallback"));
        definition.addPropertyValue("fallbackFactory", attributes.get("fallbackFactory"));
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        String alias = name + "FeignClient";
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

        boolean primary = (Boolean) attributes.get("primary"); // has a default, won't be null

        beanDefinition.setPrimary(primary);

        String qualifier = getQualifier(attributes);
        if (StringUtils.hasText(qualifier)) {
            alias = qualifier;
        }

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className,
                new String[]{alias});
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    private void validate(Map<String, Object> attributes) {
        AnnotationAttributes annotation = AnnotationAttributes.fromMap(attributes);
        // This blows up if an aliased property is overspecified
        // FIXME annotation.getAliasedString("name", FeignClient.class, null);
        validateFallback(annotation.getClass("fallback"));
        validateFallbackFactory(annotation.getClass("fallbackFactory"));
    }

    static void validateFallback(final Class clazz) {
        Assert.isTrue(
                !clazz.isInterface(),
                "Fallback class must implement the interface annotated by @FeignClient"
        );
    }

    static void validateFallbackFactory(final Class clazz) {
        Assert.isTrue(!clazz.isInterface(),
                "Fallback factory must produce instances of fallback classes that implement the interface annotated by @FeignClient"
        );
    }

    /* for testing */ String getName(Map<String, Object> attributes) {
        String name = (String) attributes.get("serviceId");
        if (!StringUtils.hasText(name)) {
            name = (String) attributes.get("name");
        }
        if (!StringUtils.hasText(name)) {
            name = (String) attributes.get("value");
        }
        name = resolve(name);
        return getName(name);
    }

    static String getName(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }

        String host = null;
        try {
            String url;
            if (!name.startsWith("http://") && !name.startsWith("https://")) {
                url = "http://" + name;
            } else {
                url = name;
            }
            host = new URI(url).getHost();

        } catch (URISyntaxException e) {
        }
        Assert.state(host != null, "Service id not legal hostname (" + name + ")");
        return name;
    }

    private String resolve(String value) {
        if (StringUtils.hasText(value)) {
            return this.environment.resolvePlaceholders(value);
        }
        return value;
    }

    private String getUrl(Map<String, Object> attributes) {
        String url = resolve((String) attributes.get("url"));
        return getUrl(url);
    }

    static String getUrl(String url) {
        if (StringUtils.hasText(url) && !(url.startsWith("#{") && url.contains("}"))) {
            if (!url.contains("://")) {
                url = "http://" + url;
            }
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(url + " is malformed", e);
            }
        }
        return url;
    }

    private String getPath(Map<String, Object> attributes) {
        String path = resolve((String) attributes.get("path"));
        return getPath(path);
    }

    static String getPath(String path) {
        if (StringUtils.hasText(path)) {
            path = path.trim();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
        }
        return path;
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }

    protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableDynamicFeignClients.class.getCanonicalName());

        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(
                    ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }
        return basePackages;
    }

    private String getQualifier(Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        String qualifier = (String) client.get("qualifier");
        if (StringUtils.hasText(qualifier)) {
            return qualifier;
        }
        return null;
    }

    private String getClientName(Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        String value = (String) client.get("value");
        if (!StringUtils.hasText(value)) {
            value = (String) client.get("name");
        }
        if (!StringUtils.hasText(value)) {
            value = (String) client.get("serviceId");
        }
        if (StringUtils.hasText(value)) {
            return value;
        }

        throw new IllegalStateException("Either 'name' or 'value' must be provided in @"
                + FeignClient.class.getSimpleName());
    }

    private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name,
                                             Object configuration) {
        Class<?> cls;
        try {
            cls = Class.forName("org.springframework.cloud.openfeign.FeignClientSpecification");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(cls);
        builder.addConstructorArgValue(name);
        builder.addConstructorArgValue(configuration);
        registry.registerBeanDefinition(
                name + "." + cls.getSimpleName(),
                builder.getBeanDefinition());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Helper class to create a {@link TypeFilter} that matches if all the delegates
     * match.
     *
     * @author Oliver Gierke
     */
    private static class AllTypeFilter implements TypeFilter {

        private final List<TypeFilter> delegates;

        /**
         * Creates a new {@link DynamicFeignClientsRegistrar.AllTypeFilter} to match if all the given delegates match.
         *
         * @param delegates must not be {@literal null}.
         */
        public AllTypeFilter(List<TypeFilter> delegates) {
            Assert.notNull(delegates, "This argument is required, it must not be null");
            this.delegates = delegates;
        }

        @Override
        public boolean match(MetadataReader metadataReader,
                             MetadataReaderFactory metadataReaderFactory) throws IOException {

            for (TypeFilter filter : this.delegates) {
                if (!filter.match(metadataReader, metadataReaderFactory)) {
                    return false;
                }
            }

            return true;
        }
    }
}
