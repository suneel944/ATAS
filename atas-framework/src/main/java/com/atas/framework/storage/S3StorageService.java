package com.atas.framework.storage;

import com.atas.framework.config.StorageProperties;
import java.nio.file.Path;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Implementation of {@link StorageService} backed by Amazon S3. This service uploads files to the
 * configured bucket and returns a presigned URL valid for a limited duration. The default AWS
 * credentials provider chain is used to obtain credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

  private final StorageProperties storageProperties;

  @Override
  public String upload(Path file, String key) {
    Region region = Region.of(storageProperties.getRegion());
    // Create S3 client on-demand (thread-safe) using default credentials
    try (S3Client s3 =
        S3Client.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()) {
      String bucket = storageProperties.getBucket();
      PutObjectRequest putReq = PutObjectRequest.builder().bucket(bucket).key(key).build();
      s3.putObject(putReq, RequestBody.fromFile(file));
      log.info("Uploaded file {} to s3://{}/{}", file, bucket, key);
    }
    // Generate a presigned URL valid for 7 days
    try (S3Presigner presigner =
        S3Presigner.builder()
            .region(Region.of(storageProperties.getRegion()))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()) {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(storageProperties.getBucket()).key(key).build();
      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .getObjectRequest(getObjectRequest)
              .signatureDuration(Duration.ofDays(7))
              .build();
      String url = presigner.presignGetObject(presignRequest).url().toString();
      log.info("Generated presigned URL for s3 key {}: {}", key, url);
      return url;
    }
  }
}
