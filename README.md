# dynamic-feign

### v0.2.1
- 新增UrlConcat指定统一out url的拼接方式
- 修复统一out url未拼接服务名的bug

### v0.2.0
- 支持注解统一配置所有feign的out url，feign out，feign method
- 复用相同url的feign

### v0.1.0
- 提供@EnableDynamicFeignClients支持@FeignClient
- 默认注入服务间负载均衡的feign(in url)
- 支持动态配置feign的额外url(out url)
- 支持方法级别的配置，可通过(methodName,url)来指定每个feign对应方法调用的url
