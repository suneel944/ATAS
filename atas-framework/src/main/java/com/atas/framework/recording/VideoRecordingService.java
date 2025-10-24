package com.atas.framework.recording;

import com.atas.framework.model.AttachmentType;
import com.atas.framework.model.TestAttachment;
import com.atas.framework.model.TestResult;
import com.atas.framework.repository.TestAttachmentRepository;
import com.atas.framework.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Video;  // ✅ add this

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for handling video recordings created by
 * Playwright. Once a page is closed and a video is available, this
 * service copies the file to a local temporary directory, uploads it
 * using the configured {@link StorageService} and persists a
 * {@link TestAttachment} record.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoRecordingService {

    private final StorageService storageService;
    private final TestAttachmentRepository attachmentRepository;

    public TestAttachment processVideo(Page page, TestResult result) {
        Video video = page.video();  // ✅ use com.microsoft.playwright.Video
        if (video == null) {
            log.debug("No video recording configured for this page");
            return null;
        }
        Path videoPath = video.path();
        if (videoPath == null || !Files.exists(videoPath)) {
            log.warn("Video file does not exist for page {}", page);
            return null;
        }
        try {
            Path tempDir = Files.createTempDirectory("atas-video");
            String fileName = String.format("%s-%s.mp4", result.getTestId(), UUID.randomUUID());
            Path localCopy = tempDir.resolve(fileName);
            Files.copy(videoPath, localCopy, StandardCopyOption.REPLACE_EXISTING);

            String key = String.format("%s/%s", result.getTestId(), fileName);
            String url = storageService.upload(localCopy, key);

            TestAttachment attachment = TestAttachment.builder()
                .result(result)
                .type(AttachmentType.VIDEO)
                .fileName(fileName)
                .mimeType("video/mp4")
                .url(url)
                .createdAt(LocalDateTime.now())
                .build();

            attachment = attachmentRepository.save(attachment);
            log.info("Uploaded video for test {} to {}", result.getTestId(), url);
            return attachment;
        } catch (IOException e) {
            throw new RuntimeException("Failed to process video", e);
        }
    }
}
