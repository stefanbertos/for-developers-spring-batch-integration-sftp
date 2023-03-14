package com.example.spring.batch;

import com.example.spring.batch.listener.CustomItemReadListener;
import com.example.spring.batch.listener.CustomItemWriteListener;
import com.example.spring.batch.listener.CustomStepExecutionListener;
import com.example.spring.integration.IntegrationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Slf4j
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    public static final String COLUMN_DELIMITER = ",";
    public static final String[] COLUMN_NAMES = new String[]{"id", "firstname", "lastname", "birthdate", "address", "phone", "email"};

    public static final String DEFAULT_FILE_PATH = "tmp/file.csv";
    @Value("${application.batch.chunkSize}")
    private Integer chunkSize;

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * Note the JobRepository is typically autowired in and not needed to be explicitly
     * configured
     */
    @Bean
    public Job dailyJob(DataSource dataSource, JobRepository jobRepository, PlatformTransactionManager transactionManager, IntegrationConfig.CustomGateway customGateway) {
        return new JobBuilder("dailyJob", jobRepository).start(databaseToFileStep(dataSource, jobRepository, transactionManager)).next(fileToSftpStep(jobRepository, transactionManager, customGateway)).listener(new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                JobExecutionListener.super.beforeJob(jobExecution);
                log.info("beforeJob {}", jobExecution);
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                JobExecutionListener.super.afterJob(jobExecution);
                log.info("afterJob {}", jobExecution);
            }
        }).build();
    }


    /**
     * Note the TransactionManager is typically autowired in and not needed to be explicitly
     * configured
     */
    @Bean
    public Step databaseToFileStep(DataSource dataSource, JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("databaseToFileStep", jobRepository).<ClientVO, ClientVO>chunk(chunkSize, transactionManager).listener(new CustomStepExecutionListener()).listener(new CustomItemReadListener<ClientVO>()).listener(new CustomItemWriteListener<ClientVO>()).reader(itemReader(dataSource)).writer(itemWriter()).build();
    }

    /**
     * Note the TransactionManager is typically autowired in and not needed to be explicitly
     * configured
     */
    @Bean
    public Step fileToSftpStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, IntegrationConfig.CustomGateway sftpGateway) {
        return new StepBuilder("fileToSftpStep", jobRepository).tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                sftpGateway.sendToSftp(new FileSystemResource(DEFAULT_FILE_PATH).getFile());
                return RepeatStatus.FINISHED;
            }
        }, transactionManager).build();
    }

    @Bean
    public JdbcCursorItemReader<ClientVO> itemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<ClientVO>().dataSource(dataSource).name("clientReader").sql("select id, firstname, lastname, birthdate, address, phone, email from client").rowMapper(new RowMapper() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new BatchConfig.ClientVO(rs.getLong("id"), rs.getString("firstname"), rs.getString("lastname"), rs.getDate("birthdate"), rs.getString("address"), rs.getString("phone"), rs.getString("email"));
            }
        }).build();
    }

    @Bean
    public FlatFileItemWriter itemWriter() {
        return new FlatFileItemWriterBuilder<ClientVO>().name("flatFileItemWriter").headerCallback(new FlatFileHeaderCallback() {

            @Override
            public void writeHeader(Writer writer) throws IOException {
                writer.write(String.join(COLUMN_DELIMITER, COLUMN_NAMES));
            }
        }).resource(new FileSystemResource(DEFAULT_FILE_PATH)).lineAggregator(new DelimitedLineAggregator<ClientVO>() {
            {
                setDelimiter(COLUMN_DELIMITER);
                setFieldExtractor(new BeanWrapperFieldExtractor<>() {
                    {
                        setNames(COLUMN_NAMES);
                    }
                });
            }
        }).build();
    }

    public record ClientVO(Long id, String firstname, String lastname, Date birthdate, String address, String phone,
                           String email) {
        public ClientVO() {
            this(null, null, null, null, null, null, null);
        }
    }
}




