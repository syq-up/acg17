package com.shiyq.config;

import com.shiyq.service.RecycleCleanupService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimerConfigTest {

    @Test
    void oneIllustrationFailureDoesNotStopTheRemainingCandidates() throws Exception {
        RecycleCleanupService cleanupService = mock(RecycleCleanupService.class);
        when(cleanupService.getExpiredIllustrationIds(any(Date.class))).thenReturn(Arrays.asList(1, 2, 3));
        when(cleanupService.cleanIllustration(org.mockito.ArgumentMatchers.eq(1), any(Date.class))).thenReturn(true);
        doThrow(new IOException("test failure"))
                .when(cleanupService).cleanIllustration(org.mockito.ArgumentMatchers.eq(2), any(Date.class));
        when(cleanupService.cleanIllustration(org.mockito.ArgumentMatchers.eq(3), any(Date.class))).thenReturn(false);

        TimerConfig timer = new TimerConfig();
        timer.setRecycleCleanupService(cleanupService);
        timer.deleteExpiredIllustrations();

        verify(cleanupService).cleanIllustration(org.mockito.ArgumentMatchers.eq(1), any(Date.class));
        verify(cleanupService).cleanIllustration(org.mockito.ArgumentMatchers.eq(2), any(Date.class));
        verify(cleanupService).cleanIllustration(org.mockito.ArgumentMatchers.eq(3), any(Date.class));
    }

    @Test
    void fileResidueTaskDelegatesToTheCleanupService() throws Exception {
        RecycleCleanupService cleanupService = mock(RecycleCleanupService.class);
        when(cleanupService.cleanupFileResidues(any(Date.class))).thenReturn(4);
        TimerConfig timer = new TimerConfig();
        timer.setRecycleCleanupService(cleanupService);

        timer.cleanFileResidues();

        verify(cleanupService).cleanupFileResidues(any(Date.class));
    }

    @Test
    void oneGameFailureDoesNotStopTheRemainingCandidates() throws Exception {
        RecycleCleanupService cleanupService = mock(RecycleCleanupService.class);
        when(cleanupService.getExpiredGameIds(any(Date.class))).thenReturn(Arrays.asList(11, 12, 13));
        when(cleanupService.cleanGame(org.mockito.ArgumentMatchers.eq(11), any(Date.class))).thenReturn(true);
        doThrow(new IOException("test failure"))
                .when(cleanupService).cleanGame(org.mockito.ArgumentMatchers.eq(12), any(Date.class));
        when(cleanupService.cleanGame(org.mockito.ArgumentMatchers.eq(13), any(Date.class))).thenReturn(false);

        TimerConfig timer = new TimerConfig();
        timer.setRecycleCleanupService(cleanupService);
        timer.deleteExpiredGames();

        verify(cleanupService).cleanGame(org.mockito.ArgumentMatchers.eq(11), any(Date.class));
        verify(cleanupService).cleanGame(org.mockito.ArgumentMatchers.eq(12), any(Date.class));
        verify(cleanupService).cleanGame(org.mockito.ArgumentMatchers.eq(13), any(Date.class));
    }
}
