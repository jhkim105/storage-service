package jhkim105.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  String upload(String bucketName, String key, MultipartFile multipartFile);

  Resource load(String bucketName, String key);

  void createBucket(String bucketName);

  void deleteBucket(String bucketName);
}
