package jhkim105.storage.config;

import java.io.File;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

  private String storagePath;
  private String imageMagickPath;
  private String gifsiclePath;

}
