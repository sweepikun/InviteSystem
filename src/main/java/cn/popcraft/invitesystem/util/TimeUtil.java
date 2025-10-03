package cn.popcraft.invitesystem.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 时间工具类
 */
public class TimeUtil {
    
    /**
     * 将时间字符串转换为秒数
     * 支持格式: 30s, 5m, 2h, 1d
     * @param timeString 时间字符串
     * @return 秒数
     */
    public static long parseTimeStringToSeconds(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }
        
        timeString = timeString.toLowerCase().trim();
        
        try {
            if (timeString.endsWith("s")) {
                return Long.parseLong(timeString.substring(0, timeString.length() - 1));
            } else if (timeString.endsWith("m")) {
                return Long.parseLong(timeString.substring(0, timeString.length() - 1)) * 60;
            } else if (timeString.endsWith("h")) {
                return Long.parseLong(timeString.substring(0, timeString.length() - 1)) * 3600;
            } else if (timeString.endsWith("d")) {
                return Long.parseLong(timeString.substring(0, timeString.length() - 1)) * 86400;
            } else {
                // 默认当作秒处理
                return Long.parseLong(timeString);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 将秒数转换为可读的时间字符串
     * @param seconds 秒数
     * @return 可读的时间字符串
     */
    public static String formatSecondsToReadable(long seconds) {
        if (seconds <= 0) {
            return "0秒";
        }
        
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分钟");
        }
        if (seconds > 0) {
            sb.append(seconds).append("秒");
        }
        
        return sb.toString();
    }
    
    /**
     * 计算两个时间之间的秒数差
     * @param start 起始时间
     * @param end 结束时间
     * @return 秒数差
     */
    public static long calculateSecondsBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.SECONDS.between(start, end);
    }
}