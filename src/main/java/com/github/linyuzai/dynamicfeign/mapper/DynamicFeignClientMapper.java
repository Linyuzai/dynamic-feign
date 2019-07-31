package com.github.linyuzai.dynamicfeign.mapper;

import com.github.linyuzai.dynamicfeign.factory.DynamicFeignClientFactoryBean;
import com.github.linyuzai.dynamicfeign.targeter.Targeter;
import feign.Client;
import feign.Feign;
import feign.Target;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DynamicFeignClientMapper {

    private static Map<String, ConfigurableFeignClient> feignClientMap = new ConcurrentHashMap<>();

    public static List<ConfigurableFeignClientEntity> getFeignClientEntities() {
        return feignClientMap.values().stream().map(ConfigurableFeignClient::getEntity).collect(Collectors.toList());
    }

    public static boolean isFeignOut(ConfigurableFeignClient client) {
        return client != null && client.entity.feignOut;
    }

    public ConfigurableFeignClient get(String key) {
        return feignClientMap.get(key);
    }

    public synchronized static void add(ConfigurableFeignClient client) {
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
            client.in = client.newInstance(false);
            if (client.entity.outUrl != null) {
                client.out = client.newInstance(true);
            }
            feignClientMap.put(client.entity.key, client);
        } else {
            throw new RuntimeException("key exists");
        }
    }

    public synchronized static boolean update(ConfigurableFeignClientEntity entity) {
        if (entity.key == null) {
            throw new RuntimeException("key is null");
        }
        ConfigurableFeignClient client = feignClientMap.get(entity.key);
        if (client == null) {
            throw new RuntimeException("key not found");
        }
        if (entity.outUrl != null) {
            client.entity.outUrl = entity.outUrl;
            client.out = client.newInstance(true);
        }
        client.entity.feignOut = entity.feignOut;
        return true;
    }

    public static class ConfigurableFeignClientEntity {
        private String key;
        private Class<?> type;
        private String inUrl;
        private String outUrl;
        private boolean feignOut;

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
    }

    public static class ConfigurableFeignClient {

        private FeignContext context;
        private Feign.Builder builder;
        private Client client;
        private Targeter targeter;
        private DynamicFeignClientFactoryBean factory;
        private Object in;
        private Object out;

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

        public Object newInstance(boolean out) {
            if (out) {
                if (client instanceof LoadBalancerFeignClient) {
                    builder.client(((LoadBalancerFeignClient) client).getDelegate());
                }
                if (entity.outUrl == null) {
                    throw new RuntimeException("out url is null");
                }
                String url = entity.outUrl;
                if (!entity.outUrl.startsWith("http")) {
                    url = "http://" + entity.outUrl;
                }
                return targeter.target(factory, builder, context,
                        new Target.HardCodedTarget<>(entity.type, entity.key, url));
            } else {
                builder.client(client);
                return targeter.target(factory, builder, context,
                        new Target.HardCodedTarget<>(entity.type, entity.key, entity.inUrl));
            }
        }

        public Object dynamic() {
            return entity.feignOut ? out : in;
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

        public ConfigurableFeignClientEntity getEntity() {
            return entity;
        }

        public void setEntity(ConfigurableFeignClientEntity entity) {
            this.entity = entity;
        }
    }
}
