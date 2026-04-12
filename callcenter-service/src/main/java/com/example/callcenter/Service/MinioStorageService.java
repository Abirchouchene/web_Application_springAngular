package com.example.callcenter.Service;

import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageService(MinioClient minioClient,
                               @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket '{}' created", bucket);
            } else {
                log.info("MinIO bucket '{}' already exists", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO init error: {}", e.getMessage());
        }
    }

    /**
     * Upload a file (byte[]) to MinIO.
     * @param objectName the key/path in the bucket (e.g. "reports/rapport-42.pdf")
     * @param data the file bytes
     * @param contentType MIME type (e.g. "application/pdf")
     */
    public void uploadFile(String objectName, byte[] data, String contentType) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(bais, data.length, -1)
                            .contentType(contentType)
                            .build());
            log.info("Uploaded '{}' to MinIO bucket '{}'", objectName, bucket);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload to MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from MinIO as InputStream.
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download from MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Check if an object exists in MinIO.
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to check MinIO object: " + e.getMessage(), e);
        }
    }
}
