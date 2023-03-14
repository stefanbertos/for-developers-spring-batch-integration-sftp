package com.example.spring.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulerService {

    private final JobLauncher jobLauncher;


    private final Job job;

    public SchedulerService(JobLauncher jobLauncher, Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    @Scheduled(cron = "${application.schedule.cron}")
    public void scheduleBatch() {
        log.info("scheduledBatch");
        try {
            jobLauncher.run(job, new JobParameters());
        } catch (Exception e) {
            log.error("Unable to launch job", e);
        }
    }
}
