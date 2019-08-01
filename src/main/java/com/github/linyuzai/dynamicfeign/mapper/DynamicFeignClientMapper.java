package com.github.linyuzai.dynamicfeign.mapper;

import com.github.linyuzai.dynamicfeign.factory.DynamicFeignClientFactoryBean;
import com.github.linyuzai.dynamicfeign.targeter.Targeter;
import feign.Client;
import feign.Feign;
import feign.Target;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DynamicFeignClientMapper {

    private static Map<String, ConfigurableFeignClient> feignClientMap = new ConcurrentHashMap<>();

    public static List<ConfigurableFeignClientEntity> getFeignClientEntities() {
        return feignClientMap.values().stream().map(ConfigurableFeignClient::getEntity).collect(Collectors.toList());
    }

    public ConfigurableFeignClientEntity getConfigurableFeignClientEntity(String key) {
        ConfigurableFeignClient client = feignClientMap.get(key);
        return client == null ? null : client.entity;
    }

    public ConfigurableFeignClient getConfigurableFeignClient(String key) {
        return feignClientMap.get(key);
    }

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
            client.in = client.newInstance(null);
            if (client.entity.outUrl != null) {
                client.out = client.newInstance(client.entity.outUrl);
            }
            feignClientMap.put(client.entity.key, client);
        } else {
            throw new RuntimeException("key exists");
        }
    }

    public static synchronized boolean update(ConfigurableFeignClientEntity entity) {
        if (entity.key == null) {
            throw new RuntimeException("key is null");
        }
        ConfigurableFeignClient client = feignClientMap.get(entity.key);
        if (client == null) {
            throw new RuntimeException("key not found");
        }
        if (entity.outUrl != null) {
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

    public static synchronized boolean addMethodUrl(String key, String methodName, String outUrl) {
        if (key == null) {
            throw new RuntimeException("key is null");
        }
        if (outUrl == null) {
            throw new RuntimeException("outUrl is null");
        }
        ConfigurableFeignClient client = feignClientMap.get(key);
        if (client == null) {
            throw new RuntimeException("key not found");
        }
        Object out = client.newInstance(outUrl);
        if (client.entity.methodOutUrls == null) {
            client.entity.methodOutUrls = new ConcurrentHashMap<>();
        }
        client.entity.methodOutUrls.put(methodName, outUrl);
        if (client.methodOuts == null) {
            client.methodOuts = new ConcurrentHashMap<>();
        }
        client.methodOuts.put(methodName, out);
        return true;
    }

    public static class ConfigurableFeignClientEntity {
        private String key;
        private Class<?> type;
        private String inUrl;
        private String outUrl;
        private boolean feignOut;
        private boolean feignMethod;

        private Map<String, String> methodOutUrls = Collections.emptyMap();

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

        public Map<String, String> getMethodOutUrls() {
            return methodOutUrls;
        }

        public void setMethodOutUrls(Map<String, String> methodOutUrls) {
            this.methodOutUrls = methodOutUrls;
        }
    }

    public static class ConfigurableFeignClient {

        private FeignContext context;
        private Feign.Builder builder;
        private Client client;
        private Targeter targeter;
        private DynamicFeignClientFactoryBean factory;
        private Object in;
        private Object out;
        private Map<String, Object> methodOuts = Collections.emptyMap();

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

        public Object newInstance(String url) {
            if (url == null) {
                builder.client(client);
                return targeter.target(factory, builder, context,
                        new Target.HardCodedTarget<>(entity.type, entity.key, entity.inUrl));
            } else {
                if (client instanceof LoadBalancerFeignClient) {
                    builder.client(((LoadBalancerFeignClient) client).getDelegate());
                }
                if (!url.startsWith("http")) {
                    url = "http://" + entity.outUrl;
                }
                if (url.equals(entity.inUrl)) {
                    return in;
                }
                return targeter.target(factory, builder, context,
                        new Target.HardCodedTarget<>(entity.type, entity.key, url));
            }
        }

        public Object dynamic(Method method) {
            if (entity.feignMethod) {
                String key = method.getName();
                if (entity.feignOut) {
                    return methodOuts.getOrDefault(key, out);
                } else {
                    return methodOuts.getOrDefault(key, in);
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

        public Map<String, Object> getMethodOuts() {
            return methodOuts;
        }

        public void setMethodOuts(Map<String, Object> methodOuts) {
            this.methodOuts = methodOuts;
        }

        public ConfigurableFeignClientEntity getEntity() {
            return entity;
        }

        public void setEntity(ConfigurableFeignClientEntity entity) {
            this.entity = entity;
        }
    }
}
