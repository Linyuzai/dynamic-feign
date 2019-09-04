package com.github.linyuzai.dynamicfeign.initializer;

import com.github.linyuzai.dynamicfeign.concat.UrlConcat;
import com.github.linyuzai.dynamicfeign.wrapper.DecoderWrapper;
import com.github.linyuzai.dynamicfeign.wrapper.EncoderWrapper;

public class InitializationConfiguration {

    private static InitializationConfiguration sGlobal = new InitializationConfiguration();

    private String outUrl;
    private UrlConcat urlConcat;
    private boolean feignOut;
    private boolean feignMethod;
    private DecoderWrapper decoderWrapper;
    private EncoderWrapper encoderWrapper;

    public static InitializationConfiguration global() {
        return sGlobal;
    }

    public String getOutUrl() {
        return outUrl;
    }

    public void setOutUrl(String outUrl) {
        this.outUrl = outUrl;
    }

    public UrlConcat getUrlConcat() {
        return urlConcat;
    }

    public void setUrlConcat(UrlConcat urlConcat) {
        this.urlConcat = urlConcat;
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

    public DecoderWrapper getDecoderWrapper() {
        return decoderWrapper;
    }

    public void setDecoderWrapper(DecoderWrapper decoderWrapper) {
        this.decoderWrapper = decoderWrapper;
    }

    public EncoderWrapper getEncoderWrapper() {
        return encoderWrapper;
    }

    public void setEncoderWrapper(EncoderWrapper encoderWrapper) {
        this.encoderWrapper = encoderWrapper;
    }
}
