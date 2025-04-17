package test;

import com.sun.management.OperatingSystemMXBean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.management.ManagementFactory;

@SpringBootTest(classes = SystemInfoCheckerTest.class)
public class SystemInfoCheckerTest {

    @Test
    public void systemInfoCheckerTest() {
        // Get runtime instance for memory usage
        int maxCount = 10;

        try {
            Thread.sleep(1000); // Sleep for 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            System.out.println("Thread was interrupted: " + e.getMessage());
    }
       while(maxCount-- > 0) {
           Runtime runtime = Runtime.getRuntime();
           long usedMemory = runtime.totalMemory() - runtime.freeMemory(); // JVM memory usage in bytes

           // Get OperatingSystemMXBean for CPU usage
           OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
           double processCpuLoad = osBean.getProcessCpuLoad(); // CPU usage of JVM process (0.0 to 1.0)

           // Convert CPU load to percentage
           double cpuUsagePercentage = processCpuLoad * 100;

           // Print JVM-specific CPU and RAM usage
           String msg = String.format("JVM CPU Usage: %.2f%%, RAM Usage: %.2f MB", cpuUsagePercentage, usedMemory / (1024.0 * 1024.0));
              System.out.println(msg);

              // Sleep for a while before checking again
              try {
                Thread.sleep(5000); // Sleep for 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.out.println("Thread was interrupted: " + e.getMessage());
              }
       }
    }
}