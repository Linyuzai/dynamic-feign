package com.github.linyuzai.dynamicfeign.wrapper;

import feign.codec.Encoder;

public interface EncoderWrapper {

    Encoder wrapper(Encoder encoder);

    class Default implements EncoderWrapper {

        @Override
        public Encoder wrapper(Encoder encoder) {
            return encoder;
        }
    }
}
