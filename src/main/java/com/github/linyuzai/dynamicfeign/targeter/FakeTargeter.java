package com.github.linyuzai.dynamicfeign.targeter;

public class FakeTargeter {

    private static DefaultTargeter defaultTargeter = new DefaultTargeter();
    private static HystrixTargeter hystrixTargeter = new HystrixTargeter();

    @SuppressWarnings("unchecked")
    public static <T> T fake(Object real) {
        if (real.getClass().getName().equals("org.springframework.cloud.openfeign.DefaultTargeter")) {
            return (T) defaultTargeter;
        } else if (real.getClass().getName().equals("org.springframework.cloud.openfeign.HystrixTargeter")) {
            return (T) hystrixTargeter;
        }
        return null;
    }
}
