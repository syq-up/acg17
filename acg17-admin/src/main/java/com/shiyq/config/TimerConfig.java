package com.shiyq.config;

import com.shiyq.service.RecycleCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
public class TimerConfig {

    private static final Duration RECYCLE_RETENTION = Duration.ofDays(14);
    private static final Duration FILE_RESIDUE_GRACE = Duration.ofHours(24);
    private RecycleCleanupService recycleCleanupService;

    @Autowired
    public void setRecycleCleanupService(RecycleCleanupService recycleCleanupService) {
        this.recycleCleanupService = recycleCleanupService;
    }

    /**
     * 每天03：00执行，对逻辑删除达到最大保留时间的插画进行删除
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void deleteExpiredIllustrations() {
        Date cutoff = cutoff();
        List<Integer> ids = recycleCleanupService.getExpiredIllustrationIds(cutoff);
        int cleaned = 0;
        int skipped = 0;
        int failed = 0;
        for (Integer id : ids) {
            try {
                if (recycleCleanupService.cleanIllustration(id, cutoff)) {
                    cleaned++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.error("清理过期插画失败，id={}", id, e);
            }
        }
        log.info("过期插画清理完成，候选={}，成功={}，跳过={}，失败={}", ids.size(), cleaned, skipped, failed);
    }

    /**
     * 每天03：10执行，对逻辑删除达到最大保留时间的漫画进行删除
     */
    @Scheduled(cron = "0 10 3 * * ?", zone = "Asia/Shanghai")
    public void deleteExpiredMangas() {
        Date cutoff = cutoff();
        List<Integer> ids = recycleCleanupService.getExpiredMangaIds(cutoff);
        int cleaned = 0;
        int skipped = 0;
        int failed = 0;
        for (Integer id : ids) {
            try {
                if (recycleCleanupService.cleanManga(id, cutoff)) {
                    cleaned++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.error("清理过期漫画失败，id={}", id, e);
            }
        }
        log.info("过期漫画清理完成，候选={}，成功={}，跳过={}，失败={}", ids.size(), cleaned, skipped, failed);
    }

    /**
     * 每天04：00清理上传失败残留、待重试删除以及没有数据库引用的旧文件。
     */
    @Scheduled(cron = "0 0 4 * * ?", zone = "Asia/Shanghai")
    public void cleanFileResidues() {
        Date cutoff = Date.from(Instant.now().minus(FILE_RESIDUE_GRACE));
        try {
            int cleaned = recycleCleanupService.cleanupFileResidues(cutoff);
            log.info("文件残留清理完成，清理数量={}", cleaned);
        } catch (Exception e) {
            log.error("文件残留清理失败", e);
        }
    }

    private Date cutoff() {
        return Date.from(Instant.now().minus(RECYCLE_RETENTION));
    }

}
