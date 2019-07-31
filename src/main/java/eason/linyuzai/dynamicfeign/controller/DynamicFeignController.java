package eason.linyuzai.dynamicfeign.controller;

import eason.linyuzai.dynamicfeign.mapper.DynamicFeignClientMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
