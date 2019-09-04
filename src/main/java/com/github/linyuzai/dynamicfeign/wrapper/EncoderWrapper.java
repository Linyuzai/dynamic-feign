package com.github.linyuzai.dynamicfeign.wrapper;

import feign.codec.Encoder;

public interface EncoderWrapper {

    Encoder wrapper(String name, Class<?> type, Encoder encoder);

    class Default implements EncoderWrapper {

        @Override
        public Encoder wrapper(String name, Class<?> type, Encoder encoder) {
            return encoder;
        }
    }
}
