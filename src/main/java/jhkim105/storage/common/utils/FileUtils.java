package jhkim105.storage.common.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileUtils {


  public static boolean isImageFile(File file) {
    try {
      BufferedImage bufferedImage = ImageIO.read(file);
      return bufferedImage != null;
    } catch (IOException e) {
      return false;
    }
  }

  public static void delete(String filePath) {
    try {
      Files.deleteIfExists(Paths.get(filePath));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void deleteEmptyDirs(String dir) {
    try(Stream<Path> pathStream = Files.walk(Paths.get(dir))) {
      pathStream
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .filter(File::isDirectory)
          .filter(f -> !StringUtils.endsWith(dir, f.getName()))
          .forEach(File::delete);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean isEmptyDir(String dir) {
    Path path = Paths.get(dir);
    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException("This is not directory.");
    }

    try (Stream<Path> entries = Files.list(path)) {
      return entries.noneMatch(p -> p.toFile().isFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getDirPath(String path) {
    return FilenameUtils.getFullPathNoEndSeparator(path);
  }

  public static void mkdirs(String path) {
    File dir = new File(getDirPath(path));
    try {
      org.apache.commons.io.FileUtils.forceMkdir(dir);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void moveFile(String source, String dest) {
    try {
      org.apache.commons.io.FileUtils.moveFile(new File(source), new File(dest));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String contentType(Resource resource) {
    try {
      return contentType(resource.getFile().toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String contentType(Path path) {
    try {
      String contentType =  Files.probeContentType(path); // jdk 8, MacOS 에서 null
      if (contentType == null) {
        contentType = URLConnection.guessContentTypeFromName(path.toString());
      }
      return contentType;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String contentType(String path) {
    try {
      String contentType =  Files.probeContentType(Paths.get(path));
      if (contentType == null) {
        contentType = URLConnection.guessContentTypeFromName(path);
      }
      return contentType;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
