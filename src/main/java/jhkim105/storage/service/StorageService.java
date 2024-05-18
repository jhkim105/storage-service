package jhkim105.storage.service;

import jhkim105.storage.common.utils.Gifsicle;
import jhkim105.storage.common.utils.ImageMagick;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public abstract class  StorageService {

  public abstract String upload(String bucketName, String key, MultipartFile multipartFile);

  public Resource load(String bucketName, String key) {
    return getResource(objectPath(bucketName, key));
  }

  public Resource load(String bucketName, String key, int w, int h) {
    var path = objectPath(bucketName, key);
    var resizedPath = resizedImagePath(path, w, h);
    if (existsObject(resizedPath)) {
      return getResource(resizedPath);
    }

    resize(path, resizedPath, w, h);
    return getResource(resizedPath);
  }

  protected String resizedImagePath(String path, int width, int height) {
    String[] pathAndExtension = pathAndExtension(path);
    if (pathAndExtension.length == 0) {
      return String.format("%s_%dx%d", path, width, height);
    }

    String namePath = pathAndExtension[0];
    String extension = pathAndExtension[1];

    return String.format("%s_%dx%d%s", namePath, width, height, extension);
  }

  protected String resizedImagePathPrefix(String path) {
    String[] pathAndExtension = pathAndExtension(path);
    if (pathAndExtension.length == 0) {
      return String.format("%s", path);
    }

    return String.format("%s_", pathAndExtension[0]);
  }

  private String[] pathAndExtension(String path) {
    int dotIndex = path.lastIndexOf('.');
    if (dotIndex == -1) {
      return new String[]{};
    }
    return new String[]{path.substring(0, dotIndex), path.substring(dotIndex)};
  }

  protected abstract String resize(String source, String target, int width, int height);

  protected String resizeFile(String source, String target, int width, int height) {
    boolean animated = ImageMagick.getImageInfo(source).isAnimated();
    if (animated && width > 0 &&  height > 0) {
      Gifsicle.resize(source, target, width, height);
    } else if (!animated) {
      if (width == 0 || height == 0) {
        ImageMagick.convert(source, target);
      } else {
        ImageMagick.resize(source, target, width, height);
      }
    }
    return target;
  }


  
  
  protected abstract Resource getResource(String path);

  public abstract void createBucket(String bucketName);

  public abstract void deleteBucket(String bucketName);

  public abstract void deleteObject(String bucketName, String key);

  public abstract void deleteResizedImages(String bucketName, String key);

  protected abstract String objectPath(String bucketName, String key);
  protected abstract boolean existsObject(String path);


}
