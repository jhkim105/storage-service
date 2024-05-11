package jhkim105.storage.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class CommandUtil {
  public static String run(String... command) {
    if (log.isDebugEnabled()) {
      List<String> commandList = new ArrayList<>();
      commandList.addAll(Arrays.asList(command));
      log.debug("command:{}", commandList);
    }

    Process p = null;
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      p = processBuilder.start();
      BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String result = IOUtils.toString(buf);
      return StringUtils.removeEnd(result, System.lineSeparator());
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    } finally {
      if (p != null)
        p.destroy();
    }
  }

}
