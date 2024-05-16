package jhkim105.storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@RequiredArgsConstructor
@Slf4j
public class S3Service implements StorageService {

  private static final long S3_SINGLE_UPLOAD_LIMIT = 3 * 1024 * 1024 * 1024L; //S3 Allowed Size is 5G

  private final ResourceLoader resourceLoader;
  private final S3Client s3;

  @Override
  public void createBucket(String bucketName) {
    try {
      CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
          .bucket(bucketName)
          .build();

      s3.createBucket(createBucketRequest);

      System.out.println("Bucket created successfully: " + bucketName);
    } catch (S3Exception e) {
      throw new RuntimeException("Error creating bucket: " + e.awsErrorDetails().errorMessage(), e);
    }
  }

  @Override
  public void deleteBucket(String bucketName) {
    s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
  }


  @Override
  public String upload(String bucketName, String key, MultipartFile multipartFile) {
    if (!doesBucketExist(bucketName)) {
      createBucket(bucketName);
    }
    if (multipartFile.getSize() > S3_SINGLE_UPLOAD_LIMIT) {
      return uploadObjectMultipart(bucketName, key, multipartFile);
    } else {
      return uploadObject(bucketName, key, multipartFile);
    }
  }

  private boolean doesBucketExist(String bucketName) {
    return s3.listBuckets().buckets().stream().anyMatch(b -> b.name().equals(bucketName));
  }

  private String uploadObject(String bucketName, String key, MultipartFile multipartFile) {
    try {
      s3.putObject(PutObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .contentType(multipartFile.getContentType())
          .contentLength(multipartFile.getSize())
          .build(), RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
      log.debug("Uploaded object: {}", key);
      return key;
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload object to S3", e);
    }
  }

  private String uploadObjectMultipart(String bucketName, String key, MultipartFile multipartFile) {
    var partSize = 100 * 1024 * 1024;
    log.info("uploadObjectMultipart start: {}", key);

    try {
      List<CompletedPart> completedParts = new ArrayList<>();
      CreateMultipartUploadResponse response = s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
          .bucket(bucketName)
          .key(key)
          .build());

      String uploadId = response.uploadId();

      try (InputStream inputStream = multipartFile.getInputStream()) {
        long bytesRead;
        byte[] buffer = new byte[(int) partSize];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, (int)bytesRead);

          UploadPartRequest uploadRequest = UploadPartRequest.builder()
              .bucket(bucketName)
              .key(key)
              .uploadId(uploadId)
              .partNumber(completedParts.size() + 1)
              .contentLength(bytesRead)
              .build();

          RequestBody requestBody = RequestBody.fromByteBuffer(byteBuffer);

          UploadPartResponse uploadPartResponse = s3.uploadPart(uploadRequest, requestBody);
          completedParts.add(CompletedPart.builder()
              .partNumber(completedParts.size() + 1)
              .eTag(uploadPartResponse.eTag())
              .build());
          log.debug("completedParts.size: {}", completedParts.size());

        }
      }

      CompleteMultipartUploadResponse finalResponse = s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
          .bucket(bucketName)
          .key(key)
          .uploadId(uploadId)
          .multipartUpload(CompletedMultipartUpload.builder()
              .parts(completedParts)
              .build())
          .build());

      log.info("uploadMultipart end: {}, etag: {}", key, finalResponse.eTag());
      return key;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<String> getObjectKeyList(String bucketName) {
    List<String> keys = new ArrayList<>();
    ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .build();

    ListObjectsV2Response listObjectsV2Response = s3.listObjectsV2(listObjectsV2Request);
    for (S3Object object : listObjectsV2Response.contents()) {
      keys.add(object.key());
    }
    return keys;
  }

  public Resource loadAsResource(String bucketName, String key) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .build();

    try (ResponseInputStream<GetObjectResponse> responseInputStream =
        s3.getObject(getObjectRequest, ResponseTransformer.toInputStream())) {
      return new InputStreamResource(responseInputStream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load object as resource from S3", e);
    }
  }


  @Override
  public Resource load(String bucketName, String key) {
    var s3Url = String.format("s3://%s/%s", bucketName, key);
    return resourceLoader.getResource(s3Url);
  }

  public List<String> getBucketNameList() {
    ListBucketsResponse response = s3.listBuckets();
    return response.buckets().stream().map(Bucket::name).toList();
  }

  public void deleteObject(String bucketName, String key) {
    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
  }


}