package jhkim105.storage.local;


import jhkim105.storage.StorageService;
import jhkim105.storage.config.StorageProperties;
import jhkim105.storage.utils.FileUtils;
import jhkim105.storage.utils.Gifsicle;
import jhkim105.storage.utils.ImageMagick;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class LocalService implements StorageService {

  private final StorageProperties storageProperties;


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

  private void resize(File file, int width, int height) {
    String inputFilePath = file.getAbsolutePath();
    String outputFilePath = file.getAbsolutePath() + ".tmp";
    boolean resized = false;
    boolean animated = ImageMagick.getImageInfo(inputFilePath).isAnimated();
    // TODO: 원본이미지 사이즈 비교하여 resize 필요한 경우에만
    if (animated && width > 0 &&  height > 0) {
        Gifsicle.resize(inputFilePath, outputFilePath, width, height);
        resized = true;
    } else if (!animated) {
      if (width == 0 || height == 0) {
        ImageMagick.convert(inputFilePath, outputFilePath);
      } else {
        ImageMagick.resize(inputFilePath, outputFilePath, width, height);
      }
      resized = true;
    }

    if (resized) {
      String originalPath = inputFilePath + ".original";
      FileUtils.moveFile(inputFilePath, originalPath);
      FileUtils.moveFile(outputFilePath, inputFilePath);
      FileUtils.delete(originalPath);
    }

  }


  @Override
  public Resource load(String bucketName, String key) {
    return new FileSystemResource(absoluteObjectPath(bucketName, key));
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

  private String absoluteBucketPath(String bucketName) {
    return String.format("%s/%s", storageProperties.getStoragePath(), bucketName);
  }

  private String absoluteObjectPath(String bucketName, String key) {
    return String.format("%s/%s", absoluteBucketPath(bucketName), key);
  }

  public void delete(String bucketName, String key) {
    String path = absoluteObjectPath(bucketName, key);
    FileUtils.delete(path);
  }
}
