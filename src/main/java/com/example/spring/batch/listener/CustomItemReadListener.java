package com.example.spring.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;

@Slf4j
public class CustomItemReadListener<T> implements ItemReadListener {
    @Override
    public void beforeRead() {
        log.info("beforeRead");
        ItemReadListener.super.beforeRead();
    }

    @Override
    public void onReadError(Exception ex) {
        log.error("onReadError {}", ex);
        ItemReadListener.super.onReadError(ex);
    }
}
