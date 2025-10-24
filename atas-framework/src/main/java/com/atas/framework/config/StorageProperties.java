package com.atas.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for external storage such as Amazon S3.
 * Values can be supplied via application.yml or environment
 * variables.  The {@code prefix} attribute defines the root in
 * configuration under which the properties are bound.
 */
@Configuration
@ConfigurationProperties(prefix = "atas.storage")
@Data
public class StorageProperties {
    /** Name of the S3 bucket where videos and screenshots are stored */
    private String bucket;

    /** Region of the S3 bucket */
    private String region;

    /** Folder within the bucket where videos should be uploaded */
    private String videoFolder = "videos";

    /** Folder within the bucket where screenshots should be uploaded */
    private String screenshotFolder = "screenshots";
}