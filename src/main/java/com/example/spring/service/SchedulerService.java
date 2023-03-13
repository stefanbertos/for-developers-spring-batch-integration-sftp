package com.example.spring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulerService {
    @Scheduled(cron= "${application.schedule.cron}")
    public void scheduleBatch() {
        log.info("scheduledBatch");
    }
}
