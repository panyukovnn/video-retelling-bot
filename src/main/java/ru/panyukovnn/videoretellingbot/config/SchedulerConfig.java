package ru.panyukovnn.videoretellingbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public ExecutorService sourceParsingScheduler() {
        return createDiscardPolicySingleThreadExecutor();
    }

    @Bean
    public ExecutorService retellingScheduler() {
        return createDiscardPolicySingleThreadExecutor();
    }

    @Bean
    public ExecutorService rateRawMaterialScheduler() {
        return createDiscardPolicySingleThreadExecutor();
    }

    @Bean
    public ExecutorService publicationScheduler() {
        return createDiscardPolicySingleThreadExecutor();
    }

    @Bean
    public ExecutorService promptingExecutor() {
        return createElasticScheduler(30, 1000);
    }

    private static ThreadPoolExecutor createDiscardPolicySingleThreadExecutor() {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardPolicy());
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
