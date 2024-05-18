package jhkim105.storage.service;


import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import jhkim105.storage.common.config.ServiceProperties;
import jhkim105.storage.common.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
public class LocalService extends StorageService {

  private final ServiceProperties serviceProperties;
  private final ResourceLoader resourceLoader;


  @Override
  public String upload(String bucketName, String key, MultipartFile multipartFile) {
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("key is required.");
    }

    String bucketPath = absoluteBucketPath(bucketName);
    if (!Paths.get(bucketPath).toFile().exists()) {
      FileUtils.mkdirs(bucketPath);
    }

    key = StringUtils.removeStart(key, "/");
    String destFilePath = String.format("%s/%s", bucketPath, key);
    File destFile = new File(destFilePath);
    if (!destFile.getParentFile().exists()) {
      FileUtils.mkdirs(destFilePath);
    }
    try {
      multipartFile.transferTo(destFile);
    } catch (IOException ex) {
      throw new IllegalStateException(String.format("file upload error:%s", destFilePath), ex);
    }

    return key;
  }

  @Override
  protected String resize(String source, String target, int width, int height) {
    return resizeFile(source, target, width, height);
  }

  @Override
  protected Resource getResource(String path) {
    return resourceLoader.getResource(String.format("file://%s", path));
  }

  @Override
  protected boolean existsObject(String absolutePath) {
    return Paths.get(absolutePath).toFile().exists();
  }

  @Override
  public void createBucket(String bucketName) {
    String bucketPath = absoluteBucketPath(bucketName);
    File bucketDir = Paths.get(bucketPath).toFile();
    boolean made = bucketDir.mkdirs();
    if (!made) {
      throw new IllegalStateException(String.format("make directory(%s) fail", bucketPath));
    }
  }

  @Override
  public void deleteBucket(String bucketName) {
    String bucketPath = absoluteBucketPath(bucketName);
    if (!FileUtils.isEmptyDir(bucketPath)) {
      throw new IllegalStateException(String.format("Could not delete non-empty bucket. bucketName: %s", bucketName));
    }

    FileUtils.deleteEmptyDirs(bucketPath);
    File bucketDir = Paths.get(bucketPath).toFile();
    bucketDir.delete();
  }


  protected String objectPath(String bucketName, String key) {
    return String.format("%s/%s", absoluteBucketPath(bucketName), key);
  }

  private String absoluteBucketPath(String bucketName) {
    return String.format("%s/%s", serviceProperties.getStoragePath(), bucketName);
  }

  @Override
  public void deleteObject(String bucketName, String key) {
    String path = objectPath(bucketName, key);
    FileUtils.delete(path);
  }

  @Override
  public void deleteResizedImages(String bucketName, String key) {
    Path dirPath = Paths.get(objectPath(bucketName, key)).getParent();
    String filenamePrefix = Paths.get(resizedImagePathPrefix(objectPath(bucketName, key))).getFileName().toString();

    if (!Files.isDirectory(dirPath)) {
      throw new RuntimeException(("Provided path is not a directory: " + dirPath));
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, filenamePrefix + "*")) {
      for (Path entry : stream) {
        Files.delete(entry);
        log.debug("Deleted: {}", entry);
      }
    } catch (IOException e) {
      log.warn("Error deleting files with prefix: {}, {}", filenamePrefix, e.getMessage());
    }

  }


}
