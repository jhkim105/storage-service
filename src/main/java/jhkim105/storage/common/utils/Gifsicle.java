package jhkim105.storage.common.utils;

import jhkim105.storage.common.config.SystemPropertiesConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Gifsicle {

  public static void resize(String sourcePath, String targetPath, int maxWidth, int maxHeight) {
    ImageMagick.orient(sourcePath, sourcePath);
    makeDir(targetPath);
    CommandUtil.run(gifsicleCommand(), "--resize-fit", maxWidth + "x" + maxHeight, sourcePath, "-o", targetPath);
  }

  private static void makeDir(String targetPath) {
    FileUtils.mkdirs(targetPath);
  }

  private static String gifsicleCommand() {
    return System.getProperty(SystemPropertiesConfig.GIFSICLE_PATH) + "/gifsicle";
  }

}
