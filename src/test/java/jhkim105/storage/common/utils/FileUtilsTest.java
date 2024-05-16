package jhkim105.storage.common.utils;

import static org.junit.jupiter.api.Assertions.*;

import jhkim105.storage.common.utils.FileUtils;
import org.junit.jupiter.api.Test;

class FileUtilsTest {


  @Test
  void getDirPath() {
    assertEquals(FileUtils.getDirPath("a/b/c/d.exe"), "a/b/c");
    assertEquals(FileUtils.getDirPath("a/b/c/"), "a/b/c");
  }


}