package org.example.debug;


import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * 监控服务和主机状态。返回运行时间,主机内存,内存占用
 */

public class Debug_ping {

    public String UsePing() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 获取JVM启动到现在的运行毫秒数
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMillis);
        long totalSeconds = uptime.getSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String uptimeStr = String.format("%d天 %d小时 %d分钟 %d秒", days, hours, minutes, seconds);

        // 使用 OSHI 获取主机物理内存（总内存和可用内存）
        String memoryLine;
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            GlobalMemory mem = hal.getMemory();
            long totalMem = mem.getTotal();
            long availMem = mem.getAvailable();
            long usedMem = totalMem - availMem;

            String totalStr = humanReadableBytes(totalMem);
            String usedStr = humanReadableBytes(usedMem);
            double percent = totalMem > 0 ? (usedMem * 100.0 / totalMem) : 0.0;

            memoryLine = String.format("💾 主机内存：%s / %s (%.2f%%)", usedStr, totalStr, percent);
        } catch (Throwable t) {
            // 如果 OSHI 在极端环境不可用，则退回到 JVM 内存信息
            Runtime rt = Runtime.getRuntime();
            long totalMem = rt.totalMemory();
            long freeMem = rt.freeMemory();
            long usedMem = totalMem - freeMem;
            String totalStr = humanReadableBytes(totalMem);
            String usedStr = humanReadableBytes(usedMem);
            double percent = totalMem > 0 ? (usedMem * 100.0 / totalMem) : 0.0;
            memoryLine = String.format("💾 JVM 内存（降级）：%s / %s (%.2f%%)", usedStr, totalStr, percent);
        }

        return "🕐 当前服务器时间：" + now.format(formatter) + "\n" +
                "📍 时区：UTC+8 (北京时间)" + "\n" +
                "⏱️ 服务器已运行：" + uptimeStr + "\n" +
                memoryLine + "\n" +
                "📊 内存使用: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + " MB";
    }

    private String humanReadableBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.2f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.2f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

}



