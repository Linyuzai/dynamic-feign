# dynamic-feign

```
implementation 'com.github.linyuzai:dynamic-feign:0.2.1'
```
## 将`@EnableFeignClients`替换成`@EnableDynamicFeignClients`来支持动态配置

### 动态配置类
```
public static class ConfigurableFeignClientEntity {
        /**
         * 微服务名称
         */
        private String key;
        /*
         * 格式为http://key，相当于feign的默认实现，支持负载均衡
         */
        private String inUrl;
        /**
         * 指定的url
         */
        private String outUrl;
        /**
         * 是否使用指定outUrl
         */
        private boolean feignOut;
        /**
         * 是否映射方法指定url
         */
        private boolean feignMethod;
        /**
         * 方法与url的映射关系
         */
        private Map<String, String> methodUrls;
        
        /*
         * get set
         * ......
         */
}
```
### 动态配置接口
|功能|方式|代码|必传字段|其他|
|-|-|-|-|-|
|查询配置|代码|`DynamicFeignClientMapper.getFeignClientEntities();`|||
|更新配置|代码|`DynamicFeignClientMapper.update(ConfigurableFeignClientEntity);`|`key`|只能修改`outUrl`，`feignOut`，`feignMethod`|

### `@EnableDynamicFeignClients`的额外属性
- `@EnableDynamicFeignClients(outUrl="")`用于统一配置

### v0.2.1
- 新增UrlConcat指定统一out url的拼接方式
- 修复统一out url未拼接服务名的bug

### v0.2.0
- 支持注解统一配置所有feign的out url，feign out，feign method
- 复用相同url的feign
- 如果url格式不标准则修改url，更方便复用

### v0.1.0
- 提供@EnableDynamicFeignClients支持@FeignClient
- 默认注入服务间负载均衡的feign(in url)
- 支持动态配置feign的额外url(out url)
- 支持方法级别的配置，可通过(methodName,url)来指定每个feign对应方法调用的url
