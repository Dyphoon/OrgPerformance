package com.cmbchina.termgoal.minio;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Component
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-templates}")
    private String bucketTemplates;

    @Value("${minio.bucket-reports}")
    private String bucketReports;

    @Value("${minio.bucket-uploads}")
    private String bucketUploads;

    @Value("${minio.bucket-collectors}")
    private String bucketCollectors;

    public void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists: " + bucketName, e);
        }
    }

    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            System.out.println("bucketExists error: " + e.getMessage());
            return false;
        }
    }

    public String uploadTemplate(MultipartFile file, Long systemId) {
        try {
            ensureBucketExists(bucketTemplates);
            String objectName = String.format("templates/%d/template_%d.xlsx",
                    systemId, System.currentTimeMillis());

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketTemplates)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload template file", e);
        }
    }

    public String uploadReport(byte[] data, Long systemId, Integer year, Integer month, String orgId) {
        try {
            ensureBucketExists(bucketReports);
            String objectName = String.format("%d/%d/%02d/%s/report_%d%02d.xlsx",
                    systemId, year, month, orgId, year, month);
            System.out.println("DEBUG uploadReport: bucket=" + bucketReports + ", objectName=" + objectName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketReports)
                            .object(objectName)
                            .stream(new java.io.ByteArrayInputStream(data), data.length, -1)
                            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload report file", e);
        }
    }

    public String uploadDataCollection(MultipartFile file, Long systemId, Integer year, Integer month, String orgId) {
        try {
            ensureBucketExists(bucketUploads);
            String objectName = String.format("%d/%d/%02d/%s/data_%d.xlsx",
                    systemId, year, month, orgId, System.currentTimeMillis());

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketUploads)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload data collection file", e);
        }
    }

    public String copyFile(String sourceBucket, String sourceObject, String destBucket, String destObject) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(destBucket)
                            .object(destObject)
                            .source(CopySource.builder().bucket(sourceBucket).object(sourceObject).build())
                            .build()
            );
            return destObject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy file", e);
        }
    }

    public InputStream downloadFile(String bucket, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + objectName, e);
        }
    }

    public String getPresignedUrl(String bucket, String objectName, int expirationMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(expirationMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    public byte[] downloadAsBytes(String bucket, String objectName) {
        try (InputStream is = downloadFile(bucket, objectName)) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file as bytes", e);
        }
    }

    public void deleteFile(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public String uploadCollectorDocument(byte[] data, Long monitoringId, Long collectorUserId, Long institutionId) {
        try {
            ensureBucketExists(bucketCollectors);
            String objectName = String.format("%d/%d/collector_%d.xlsx",
                    monitoringId, collectorUserId, System.currentTimeMillis());

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketCollectors)
                            .object(objectName)
                            .stream(new java.io.ByteArrayInputStream(data), data.length, -1)
                            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload collector document", e);
        }
    }

    public String getBucketCollectors() {
        return bucketCollectors;
    }

    public String getBucketTemplates() {
        return bucketTemplates;
    }

    public String getBucketReports() {
        return bucketReports;
    }
}
