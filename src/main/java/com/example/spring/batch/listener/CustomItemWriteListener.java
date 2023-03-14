package com.example.spring.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;

@Slf4j
public class CustomItemWriteListener<T> implements ItemWriteListener {
    @Override
    public void beforeWrite(Chunk items) {
        log.info("beforeWrite {}", items);
        ItemWriteListener.super.beforeWrite(items);
    }

    @Override
    public void afterWrite(Chunk items) {
        log.info("afterWrite {}", items);
        ItemWriteListener.super.afterWrite(items);
    }

    @Override
    public void onWriteError(Exception exception, Chunk items) {
        log.error("onWriteError {}, {}", exception, items);
        ItemWriteListener.super.onWriteError(exception, items);
    }
}
