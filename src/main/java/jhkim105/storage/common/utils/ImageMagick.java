package jhkim105.storage.common.utils;

import jhkim105.storage.common.config.SystemPropertiesConfig;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageMagick {

  private static final String OPTION_AUTO_ORIENT = "-auto-orient";
  private static final String OPTION_RESIZE = "-resize";
  private static final String OPTION_QUALITY = "-quality";

  public static void convert(String sourcePath, String targetPath) {
    makeDir(targetPath);
    String convertCommand = getConvertCommand();
    if (StringUtils.endsWithAny(sourcePath.toLowerCase(), ".jpg", ".jpeg")) {
      CommandUtil.run(convertCommand, OPTION_AUTO_ORIENT, OPTION_QUALITY, "65%", sourcePath, targetPath);
    } else {
      CommandUtil.run(convertCommand, OPTION_AUTO_ORIENT, sourcePath, targetPath);
    }
  }

  private static void makeDir(String targetPath) {
    FileUtils.mkdirs(targetPath);
  }

  public static void resize(String sourcePath, String targetPath, int maxWidth, int maxHeight) {
    makeDir(targetPath);
    String convertCommand = getConvertCommand();
    if (StringUtils.endsWithAny(sourcePath.toLowerCase(), ".jpg", ".jpeg")) {
      CommandUtil.run(convertCommand, OPTION_AUTO_ORIENT, OPTION_RESIZE, maxWidth + "x" + maxHeight, OPTION_QUALITY, "65%", sourcePath, targetPath);
    } else {
      CommandUtil.run(convertCommand, OPTION_AUTO_ORIENT, OPTION_RESIZE, maxWidth + "x" + maxHeight, sourcePath, targetPath);
    }
  }

  private static String getConvertCommand() {
    return String.format("%s/convert", System.getProperty(SystemPropertiesConfig.IMAGEMAGICK_PATH));
  }

  public static void orient(String sourcePath, String targetPath) {
    String convertCommand = getConvertCommand();
    CommandUtil.run(convertCommand, sourcePath, OPTION_AUTO_ORIENT, targetPath);
  }

  public static ImageInfo getImageInfo(String path) {
    String identifyCommand = System.getProperty(SystemPropertiesConfig.IMAGEMAGICK_PATH) + "/identify";
    String identifyResult = CommandUtil.run(identifyCommand, "-format", "%wx%h:", path);
    String[] identifyResults = StringUtils.split(identifyResult, "\n");
    String sizeString = identifyResults[identifyResults.length - 1];
    String[] whArray = StringUtils.split(sizeString, ":");
    int width;
    int height;
    boolean animatedImage = whArray.length > 1;

    if (ArrayUtils.isEmpty(whArray)) {
      throw new IllegalStateException(String.format("getSize fail. identify result is empty or invalid. identifyResult:%s", identifyResult));
    } else {
      String whFirst = whArray[0];
      String[] wh = StringUtils.split(whFirst, "x");
      if (!StringUtils.isNumeric(wh[0])) {
        throw new IllegalStateException(String.format("getSize fail. identify result is invalid. identifyResult:%s", identifyResult));
      }

      if (wh.length == 2) {
        width = Integer.parseInt(wh[0]);
        height = Integer.parseInt(wh[1]);
      } else {
        throw new IllegalStateException(String.format("getSize fail. identify result is invalid. identifyResult:%s", identifyResult));
      }
    }
    if (width == 0 || height == 0) {
      throw new IllegalStateException("getSize fail. image width or height is zero.");
    }
    return new ImageInfo(width, height, animatedImage);
  }


  @Getter
  @ToString
  @RequiredArgsConstructor
  public static class ImageInfo implements Serializable {

    private static final long serialVersionUID = -1762293151619292323L;

    private final int width;
    private final int height;
    private final boolean animated;

  }

}
