package com.vyaparsetu.common.storage;

/**
 * Storage abstraction. Local in dev; an S3 implementation can be added for prod.
 */
public interface StorageService {
    /**
     * Store text content under a relative key and return a retrievable URL/path.
     */
    String store(String key, String content, String contentType);

    /**
     * Read previously stored text content by key. Returns null if it does not exist.
     */
    String read(String key);
}
