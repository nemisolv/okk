package com.vht.kafkamonitoring;


import com.vht.kafkamonitoring.util.LogUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AppRunner implements ApplicationRunner {
    // This method will be called after the application context is loaded
    AppScheduler appScheduler;
    @Override
    public void run(ApplicationArguments args)    {


        // chờ khoảng 1s để đo CPU, RAM không bị ảnh hưởng bởi việc khởi động ứng dụng, chỉ chờ 1s lúc khởi chạy thôi, còn các lần cron thì không cần
        try {
            Thread.sleep(1000); //
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            System.out.println("Thread was interrupted: " + e.getMessage());
        }

        LogUtil.info("🚀 Application started. Initializing monitoring task...");

        appScheduler.runMonitoring();

        LogUtil.info("✅ Monitoring task completed at startup.");    }
}
