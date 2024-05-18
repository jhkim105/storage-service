package jhkim105.storage.common.config;


import jhkim105.storage.service.LocalService;
import jhkim105.storage.service.S3Service;
import jhkim105.storage.service.StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class StorageConfig {

  @Bean
  @ConditionalOnProperty(name = "service.storage-type", havingValue = "LOCAL")
  public StorageService localStorageService(ServiceProperties serviceProperties, ResourceLoader resourceLoader) {
    return new LocalService(serviceProperties, resourceLoader);
  }

  @Bean
  @ConditionalOnProperty(name = "service.storage-type", havingValue = "S3")
  public StorageService s3StorageService(ResourceLoader resourceLoader, S3Client s3) {
    return new S3Service(resourceLoader, s3);
  }


}
