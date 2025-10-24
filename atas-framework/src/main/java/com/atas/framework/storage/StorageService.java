package com.atas.framework.storage;

import java.nio.file.Path;

/**
 * Abstraction for uploading files to external storage.  Implementations
 * could upload videos and screenshots to Amazon S3 or another
 * provider.  The returned URL should be publicly accessible or
 * presigned based on the configuration.
 */
public interface StorageService {

    /**
     * Upload the provided file to storage.
     *
     * @param file the local file to upload
     * @param key the key or path within the remote storage
     * @return a URL pointing to the uploaded file
     */
    String upload(Path file, String key);
}