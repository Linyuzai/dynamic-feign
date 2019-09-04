package com.github.linyuzai.dynamicfeign.mapper;

import com.github.linyuzai.dynamicfeign.factory.DynamicFeignClientFactoryBean;
import com.github.linyuzai.dynamicfeign.targeter.Targeter;
import feign.Client;
import feign.Feign;
import feign.Target;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * feign clients存储配置类
 */
public class DynamicFeignClientMapper {

    /**
     * 微服务对应的feign map
     */
    private static Map<String, ConfigurableFeignClient> feignClientMap = new ConcurrentHashMap<>();

    public static Map<String, ConfigurableFeignClient> getFeignClientMap() {
        return feignClientMap;
    }

    /**
     * 获得所有的feign client
     *
     * @return 所有的feign client
     */
    public static List<ConfigurableFeignClientEntity> getFeignClientEntities() {
        return feignClientMap.values().stream().map(ConfigurableFeignClient::getEntity).collect(Collectors.toList());
    }

    /**
     * 获得某个微服务的feign配置
     *
     * @param key 微服务名称
     * @return 某个微服务的feign配置
     */
    public static ConfigurableFeignClientEntity getConfigurableFeignClientEntity(String key) {
        ConfigurableFeignClient client = feignClientMap.get(key);
        return client == null ? null : client.entity;
    }

    /**
     * 获得某个微服务的feign client
     *
     * @param key 微服务名称
     * @return 某个微服务的feign client
     */
    public static ConfigurableFeignClient getConfigurableFeignClient(String key) {
        return feignClientMap.get(key);
    }

    public static ConfigurableFeignClient getConfigurableFeignClient(Class<?> cls) {
        for (ConfigurableFeignClient client : feignClientMap.values()) {
            if (client.entity.type == cls) {
                return client;
            }
        }
        return null;
    }

    /**
     * 添加一个feign client，一般扫描配置的时候才会用到
     *
     * @param client 需要添加的client
     */
    public static void add(ConfigurableFeignClient client) {
        if (client.entity.key == null) {
            throw new RuntimeException("key is null");
        }
        ConfigurableFeignClient feignClient = feignClientMap.get(client.entity.key);
        if (feignClient == null) {
            if (client.entity.inUrl == null) {
                throw new RuntimeException("inUrl is null");
            }
            if (client.entity.feignOut && client.entity.outUrl == null) {
                throw new RuntimeException("outUrl is null when feignOut=true");
            }
            client.entity.inUrl = getParsedUrl(client.entity.inUrl);
            client.in = client.newInstance(null);
            if (client.entity.outUrl != null) {
                client.entity.outUrl = getParsedUrl(client.entity.outUrl);
                client.out = client.newInstance(client.entity.outUrl);
            }
            feignClientMap.put(client.entity.key, client);
            if (client.entity.methodUrls != null) {
                for (Map.Entry<String, String> entry : client.entity.methodUrls.entrySet()) {
                    addMethodUrl(client.entity.key, entry.getKey(), entry.getValue());
                }
            }
        } else {
            throw new RuntimeException("key exists");
        }
    }

    /**
     * 更新某个feign配置
     *
     * @param entity 需要更新的feign配置
     * @return 是否成功
     */
    public static synchronized boolean update(ConfigurableFeignClientEntity entity) {
        ConfigurableFeignClient client = getExistConfigurableFeignClient(entity.key);
        if (entity.outUrl != null) {
            entity.outUrl = getParsedUrl(entity.outUrl);
            Object out = client.newInstance(entity.outUrl);
            client.entity.outUrl = entity.outUrl;
            client.out = out;
        }
        if (entity.feignOut && client.out == null) {
            throw new RuntimeException("Set outUrl if you want feignOut");
        }
        client.entity.feignOut = entity.feignOut;
        client.entity.feignMethod = entity.feignMethod;
        return true;
    }

    /**
     * 更新某个feign配置
     *
     * @param cls    需要更新的feign类
     * @param entity 需要更新的feign配置
     * @return 是否成功
     */
    public static boolean update(Class<?> cls, ConfigurableFeignClientEntity entity) {
        ConfigurableFeignClient client = getExistConfigurableFeignClient(cls);
        entity.key = client.entity.key;
        return update(entity);
    }

    private static ConfigurableFeignClient getExistConfigurableFeignClient(String key) {
        if (key == null) {
            throw new RuntimeException("key is null");
        }
        ConfigurableFeignClient client = feignClientMap.get(key);
        if (client == null) {
            throw new RuntimeException("key not found");
        }
        return client;
    }

    private static ConfigurableFeignClient getExistConfigurableFeignClient(Class<?> cls) {
        ConfigurableFeignClient client = getConfigurableFeignClient(cls);
        if (client == null) {
            throw new RuntimeException("Class for feign not found");
        }
        return client;
    }

    /**
     * 添加方法对应的url
     *
     * @param key        微服务名称
     * @param methodName 方法名称
     * @param url        url
     * @return 是否成功
     */
    public static synchronized boolean addMethodUrl(String key, String methodName, String url) {
        if (url == null) {
            throw new RuntimeException("url is null");
        }
        if (methodName == null) {
            throw new RuntimeException("method name is null");
        }
        ConfigurableFeignClient client = getExistConfigurableFeignClient(key);
        url = getParsedUrl(url);
        Object out = client.newInstance(url);
        if (client.entity.methodUrls == null) {
            client.entity.methodUrls = new ConcurrentHashMap<>();
        }
        client.entity.methodUrls.put(methodName, url);
        if (client.methodFeigns == null) {
            client.methodFeigns = new ConcurrentHashMap<>();
        }
        client.methodFeigns.put(methodName, out);
        return true;
    }

    public static boolean addMethodUrl(Class<?> cls, String methodName, String url) {
        ConfigurableFeignClient client = getExistConfigurableFeignClient(cls);
        return addMethodUrl(client.entity.key, methodName, url);
    }

    public static synchronized boolean removeMethodUrl(String key, String methodName) {
        if (methodName == null) {
            throw new RuntimeException("method name is null");
        }
        ConfigurableFeignClient client = getExistConfigurableFeignClient(key);
        if (client.entity.methodUrls != null) {
            client.entity.methodUrls.remove(methodName);
        }
        if (client.methodFeigns != null) {
            client.methodFeigns.remove(methodName);
        }
        return true;
    }

    public static boolean removeMethodUrl(Class<?> cls, String methodName) {
        ConfigurableFeignClient client = getExistConfigurableFeignClient(cls);
        return removeMethodUrl(client.entity.key, methodName);
    }

    public static synchronized boolean clearMethodUrl(String key) {
        ConfigurableFeignClient client = getExistConfigurableFeignClient(key);
        if (client.entity.methodUrls != null) {
            client.entity.methodUrls.clear();
        }
        if (client.methodFeigns != null) {
            client.methodFeigns.clear();
        }
        return true;
    }

    public static boolean clearMethodUrl(Class<?> cls) {
        ConfigurableFeignClient client = getExistConfigurableFeignClient(cls);
        return clearMethodUrl(client.entity.key);
    }

    public static String getParsedUrl(String url) {
        url = url.trim();
        while (url.startsWith("/")) {
            url = url.substring(1);
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        return url;
    }

    /**
     * feign client配置类
     */
    public static class ConfigurableFeignClientEntity {
        /**
         * 微服务名称
         */
        private String key;
        /**
         * feign接口
         */
        private Class<?> type;
        /**
         * 内网服务间的url
         */
        private String inUrl;
        /**
         * 指定的url
         */
        private String outUrl;
        /**
         * 是否使用指定url
         */
        private boolean feignOut;
        /**
         * 是否映射方法指定url
         */
        private boolean feignMethod;

        private Map<String, String> methodUrls;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Class<?> getType() {
            return type;
        }

        public void setType(Class<?> type) {
            this.type = type;
        }

        public String getInUrl() {
            return inUrl;
        }

        public void setInUrl(String inUrl) {
            this.inUrl = inUrl;
        }

        public String getOutUrl() {
            return outUrl;
        }

        public void setOutUrl(String outUrl) {
            this.outUrl = outUrl;
        }

        public boolean isFeignOut() {
            return feignOut;
        }

        public void setFeignOut(boolean feignOut) {
            this.feignOut = feignOut;
        }

        public boolean isFeignMethod() {
            return feignMethod;
        }

        public void setFeignMethod(boolean feignMethod) {
            this.feignMethod = feignMethod;
        }

        public Map<String, String> getMethodUrls() {
            return methodUrls;
        }

        public void setMethodOutUrls(Map<String, String> methodUrls) {
            this.methodUrls = methodUrls;
        }
    }

    public static class ConfigurableFeignClient {

        private FeignContext context;
        private Feign.Builder builder;
        private Client client;
        private Targeter targeter;
        private DynamicFeignClientFactoryBean factory;
        /**
         * 默认内网间调用的feign
         */
        private Object in;
        /**
         * 指定url的feign
         */
        private Object out;
        /**
         * 根据方法映射的feign
         */
        private Map<String, Object> methodFeigns;

        private ConfigurableFeignClientEntity entity;

        public ConfigurableFeignClient(FeignContext context, Feign.Builder builder, Client client,
                                       Targeter targeter, DynamicFeignClientFactoryBean factory) {
            if (client == null) {
                throw new IllegalStateException(
                        "No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-netflix-ribbon?");
            }
            this.context = context;
            this.builder = builder;
            this.client = client;
            this.targeter = targeter;
            this.factory = factory;
            this.entity = new ConfigurableFeignClientEntity();
        }

        /**
         * 实例化feign
         *
         * @param url 如果为null，实例化in
         * @return feign的实例
         */
        public synchronized Object newInstance(String url) {
            if (url == null) {
                builder.client(client);
                return targeter.target(factory, builder, context,
                        new Target.HardCodedTarget<>(entity.type, entity.key, entity.inUrl));
            } else {
                if (url.equals(entity.inUrl)) {
                    //cant be null
                    return in;
                }
                if (url.equals(entity.outUrl) && out != null) {
                    return out;
                }
                if (entity.methodUrls != null && methodFeigns != null) {
                    for (Map.Entry<String, String> entry : entity.methodUrls.entrySet()) {
                        if (url.equals(entry.getValue())) {
                            Object feign = methodFeigns.get(entry.getKey());
                            if (feign != null) {
                                return feign;
                            }
                        }
                    }
                }
                if (client instanceof LoadBalancerFeignClient) {
                    builder.client(((LoadBalancerFeignClient) client).getDelegate());
                }
                return targeter.target(factory, builder, context,
                        new Target.HardCodedTarget<>(entity.type, entity.key, url));
            }
        }

        public Object dynamic(Method method) {
            if (entity.feignMethod) {
                String key = method.getName();
                if (entity.feignOut) {
                    return methodFeigns == null ? out : methodFeigns.getOrDefault(key, out);
                } else {
                    return methodFeigns == null ? in : methodFeigns.getOrDefault(key, in);
                }
            } else {
                return entity.feignOut ? out : in;
            }
        }

        public FeignContext getContext() {
            return context;
        }

        public void setContext(FeignContext context) {
            this.context = context;
        }

        public Feign.Builder getBuilder() {
            return builder;
        }

        public void setBuilder(Feign.Builder builder) {
            this.builder = builder;
        }

        public Client getClient() {
            return client;
        }

        public void setClient(Client client) {
            this.client = client;
        }

        public Targeter getTargeter() {
            return targeter;
        }

        public void setTargeter(Targeter targeter) {
            this.targeter = targeter;
        }

        public DynamicFeignClientFactoryBean getFactory() {
            return factory;
        }

        public void setFactory(DynamicFeignClientFactoryBean factory) {
            this.factory = factory;
        }

        public Object getIn() {
            return in;
        }

        public void setIn(Object in) {
            this.in = in;
        }

        public Object getOut() {
            return out;
        }

        public void setOut(Object out) {
            this.out = out;
        }

        public Map<String, Object> getMethodFeigns() {
            return methodFeigns;
        }

        public void setMethodFeigns(Map<String, Object> methodFeigns) {
            this.methodFeigns = methodFeigns;
        }

        public ConfigurableFeignClientEntity getEntity() {
            return entity;
        }

        public void setEntity(ConfigurableFeignClientEntity entity) {
            this.entity = entity;
        }
    }
}
