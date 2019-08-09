package com.github.linyuzai.dynamicfeign.controller;

import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dynamic-feign")
public class DynamicFeignController {

    /**
     * 更新且只限于以下属性
     * outUrl
     * feignMethod
     * feignOut
     *
     * @param entity 需要更新的属性
     * @return 成功或异常
     * @see com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper.ConfigurableFeignClientEntity#setOutUrl(String)
     * @see com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper.ConfigurableFeignClientEntity#setFeignMethod(boolean)
     * @see com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper.ConfigurableFeignClientEntity#setFeignOut(boolean)
     */
    @PostMapping("/config")
    public Object setConfig(DynamicFeignClientMapper.ConfigurableFeignClientEntity entity) {
        try {
            return DynamicFeignClientMapper.update(entity);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    /**
     * 获得当前配置属性
     *
     * @return 当前属性
     * @see com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper.ConfigurableFeignClientEntity
     */
    @GetMapping("/config")
    public Object getConfig() {
        return DynamicFeignClientMapper.getFeignClientEntities();
    }

    /**
     * 添加方法对应的url
     *
     * @param key        微服务名称@FeignClient中的值
     * @param methodName 方法名
     * @param url        对应的feign地址u
     * @return 成功或异常
     */
    @PostMapping("/method-url/add")
    public Object addMethodUrl(@RequestParam String key, @RequestParam String methodName,
                                  @RequestParam String url) {
        try {
            return DynamicFeignClientMapper.addMethodUrl(key, methodName, url);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    @PostMapping("/method-url/remove")
    public Object removeMethodUrl(@RequestParam String key, @RequestParam String methodName) {
        try {
            return DynamicFeignClientMapper.removeMethodUrl(key, methodName);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    @PostMapping("/method-url/clear")
    public Object clearMethodUrl(@RequestParam String key) {
        try {
            return DynamicFeignClientMapper.clearMethodUrl(key);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }
}
