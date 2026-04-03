package ru.panyukovnn.videoretellingbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
@EnableScheduling
public class SchedulerConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService tgListenerExecutor() {
        return createElasticScheduler(10, 100);
    }

    private static ThreadPoolExecutor createElasticScheduler(int threadsNumber, int queueCapacity) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            threadsNumber, threadsNumber,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity), new ThreadPoolExecutor.CallerRunsPolicy());

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        return threadPoolExecutor;
    }
}
