package com.github.linyuzai.dynamicfeign.wrapper;

import feign.codec.Decoder;

public interface DecoderWrapper {

    Decoder wrapper(Decoder decoder);

    class Default implements DecoderWrapper {

        @Override
        public Decoder wrapper(Decoder decoder) {
            return decoder;
        }
    }
}
