# dynamic-feign

```
implementation 'com.github.linyuzai:dynamic-feign:0.3.1'
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
|---|---|---|---|---|
|查询配置|代码|`DynamicFeignClientMapper.getFeignClientEntities();`|||
|更新配置|代码|`DynamicFeignClientMapper.update(ConfigurableFeignClientEntity);`|`key`|只能修改`outUrl`，`feignOut`，`feignMethod`|
|添加方法对应的url|代码|`DynamicFeignClientMapper.addMethodUrl(key, methodName, url);`|`key`，`methodName`，`url`|相同的`methodName`会覆盖|
|移除方法对应的url|代码|`DynamicFeignClientMapper.removeMethodUrl(key, methodName);`|`key`，`methodName`||
|清空方法对应的url|代码|`DynamicFeignClientMapper.clearMethodUrl(key);`|`key`||
|查询配置|接口|`/dynamic-feign/config[GET]`|||
|更新配置|接口|`/dynamic-feign/config[POST]`|`key`|只能修改`outUrl`，`feignOut`，`feignMethod`|
|添加方法对应的url|接口|`/dynamic-feign/method-url/add[POST]`|`key`，`methodName`，`url`|相同的`methodName`会覆盖|
|移除方法对应的url|接口|`/dynamic-feign/method-url/remove[POST]`|`key`，`methodName`||
|清空方法对应的url|接口|`/dynamic-feign/method-url/clear[POST]`|`key`||

### `@EnableDynamicFeignClients`的额外属性
- `@EnableDynamicFeignClients(outUrl = "http/https://ip:port/prefix")`用于统一配置`outUrl`
- `@EnableDynamicFeignClients(feignOut = true)`用于统一配置`feignOut`，为true时需要同时配置`outUrl`，默认为false
- `@EnableDynamicFeignClients(feignMethod = true)`用于统一配置`feignMethod`，默认为false
- `@EnableDynamicFeignClients(urlConcat = UrlConcat)`用于配置`outUrl`的拼接规则，`NONE`不拼接，`SERVICE_LOWER_CASE`拼接小写服务名，`SERVICE_UPPER_CASE`拼接大写服务名，默认`SERVICE_LOWER_CASE`
- `@EnableDynamicFeignClients(encoderWrapper = EncoderWrapper)`用于配置`EncoderWrapper`
- `@EnableDynamicFeignClients(decoderWrapper = DecoderWrapper)`用于配置`DecoderWrapper`

### 配置文件属性
- 优先级大于`@EnableDynamicFeignClients`
- `dynamic-feign.out-url`可代替`@EnableDynamicFeignClients(outUrl = "http/https://ip:port/prefix")`
- `dynamic-feign.feign-out`可代替`@EnableDynamicFeignClients(feignOut = true)`
- `dynamic-feign.feign-method`可代替`@EnableDynamicFeignClients(feignMethod = true)`
- `dynamic-feign.encoder-wrapper`可代替`@EnableDynamicFeignClients(encoderWrapper = EncoderWrapper)`
- `dynamic-feign.decoder-wrapper`可代替`@EnableDynamicFeignClients(decoderWrapper = DecoderWrapper)`

## Version logs

### v0.3.2
- 将lambda模块移出至单独的项目

### v0.3.1
- 修复0.3.0版本的jar包问题
- 添加`EncoderWrapper`和`DecoderWrapper`
- 支持lambda配置methodUrl

### v0.3.0
- 新增配置文件代替`@EnableDynamicFeignClients`统一配置`outUrl`，`feignOut`，`feignMethod`
- 新增根据class获得feign和更新feign
- 获得feign由通过实例方法改为通过静态方法
- 修复out复用一直为null的bug
- 删除@SpringBootApplication避免包名相同重复扫描注解

### v0.2.1
- 新增`UrlConcat`指定统一`outUrl`的拼接方式
- 修复统一`outUrl`未拼接服务名的bug

### v0.2.0
- 支持注解统一配置所有feign的`outUrl`，`feignOut`，`feignMethod`
- 复用相同url的feign
- 如果url格式不标准则修改url，更方便复用

### v0.1.0
- 提供`@EnableDynamicFeignClients`支持`@FeignClient`
- 默认注入服务间负载均衡的feign(`inUrl`)
- 支持动态配置feign的额外url(`outUrl`)
- 支持方法级别的配置，可通过`(methodName,url)`来指定每个feign对应方法调用的url
