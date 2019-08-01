package com.github.linyuzai.dynamicfeign.controller;

import com.github.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dynamic-feign")
public class DynamicFeignController {

    @PostMapping("/config")
    public Object setConfig(DynamicFeignClientMapper.ConfigurableFeignClientEntity entity) {
        try {
            return DynamicFeignClientMapper.update(entity);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    @GetMapping("/config")
    public Object getConfig() {
        return DynamicFeignClientMapper.getFeignClientEntities();
    }

    public Object addMethodOutUrl(@RequestParam String key, @RequestParam String methodName,
                                  @RequestParam String outUrl) {
        try {
            return DynamicFeignClientMapper.addMethodUrl(key, methodName, outUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }
}
