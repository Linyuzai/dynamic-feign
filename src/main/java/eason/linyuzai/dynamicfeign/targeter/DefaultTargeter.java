package eason.linyuzai.dynamicfeign.targeter;

import eason.linyuzai.dynamicfeign.factory.DynamicFeignClientFactoryBean;
import feign.Feign;
import feign.Target;
import org.springframework.cloud.openfeign.FeignContext;

public class DefaultTargeter implements Targeter {

    @Override
    public <T> T target(DynamicFeignClientFactoryBean factory, Feign.Builder feign, FeignContext context,
                        Target.HardCodedTarget<T> target) {
        return feign.target(target);
    }
}
