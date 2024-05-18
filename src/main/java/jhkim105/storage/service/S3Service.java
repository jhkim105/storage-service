package jhkim105.storage.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@RequiredArgsConstructor
@Slf4j
public class S3Service extends StorageService {

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

      log.debug("Bucket created successfully: {}", bucketName);
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
      return uploadMultipart(bucketName, key, multipartFile);
    } else {
      return uploadSingle(bucketName, key, multipartFile);
    }
  }

  private boolean doesBucketExist(String bucketName) {
    return s3.listBuckets().buckets().stream().anyMatch(b -> b.name().equals(bucketName));
  }

  private String uploadSingle(String bucketName, String key, MultipartFile multipartFile) {
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

  private String uploadMultipart(String bucketName, String key, MultipartFile multipartFile) {
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
          ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, (int) bytesRead);

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

  @Override
  public Resource load(String bucketName, String key) {
    return getResource(objectPath(bucketName, key));
  }

  @Override
  protected Resource getResource(String path) {
    return resourceLoader.getResource(String.format("s3://%s", path));
  }

  @Override
  protected String resize(String source, String target, int width, int height) {
    Resource original = getResource(source);
    File file = null;
    File resizedFile = null;
    try {
      file = File.createTempFile("storage-service-original-", ".tmp");
      Files.copy(original.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING );
      resizedFile = File.createTempFile("storage-service-resized-", ".tmp");
      resizeFile(file.getAbsolutePath(), resizedFile.getAbsolutePath(), width, height);
      String[] parts = extractBucketNameAndKey(target);
      upload(parts[0], parts[1], resizedFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      deleteTempFile(file);
      deleteTempFile(resizedFile);
    }
    return target;
  }

  private String[] extractBucketNameAndKey(String path) {
    String[] parts = path.split("/", 2);
    String bucketName = parts[0];
    String key = parts.length > 1 ? parts[1] : "";
    return new String[]{bucketName, key};
  }

  private void deleteTempFile(File file) {
    try {
      if (file != null && file.exists()) {
        file.delete();
      }
    } catch (Exception e) {
      // ignored
      log.warn("delete temp file error. {}", e.getMessage());
    }
  }


  private String upload(String bucketName, String key, File file) {
    s3.putObject(PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentLength(file.length())
        .build(), RequestBody.fromFile(file.toPath()));
    log.debug("Uploaded object: {}", key);
    return key;
  }

  @Override
  protected boolean existsObject(String path) {
    return getResource(path).exists();
  }


  public void deleteObject(String bucketName, String key) {
    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
  }

  @Override
  public void deleteResizedImages(String bucketName, String key) {
    String resizedImagePrefix = resizedImagePathPrefix(key);
    ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .prefix(resizedImagePrefix).build();

    ListObjectsV2Response res = s3.listObjectsV2(listObjectsRequest);
    List<S3Object> objects = res.contents();
    for (S3Object s3Object : objects) {
      deleteObject(bucketName, s3Object.key());
    }

  }

  @Override
  protected String objectPath(String bucketName, String key) {
    return String.format("%s/%s", bucketName, key);
  }


}