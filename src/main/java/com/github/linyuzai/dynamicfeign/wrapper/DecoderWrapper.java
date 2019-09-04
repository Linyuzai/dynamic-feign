package com.github.linyuzai.dynamicfeign.wrapper;

import feign.codec.Decoder;

public interface DecoderWrapper {

    Decoder wrapper(String name, Class<?> type, Decoder decoder);

    class Default implements DecoderWrapper {

        @Override
        public Decoder wrapper(String name, Class<?> type, Decoder decoder) {
            return decoder;
        }
    }
}
