package jhkim105.storage.common.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "service")
public class ServiceProperties {
  private String storagePath;
//  private StorageType storageType;
  private String imageMagickPath;
  private String gifsiclePath;

}
