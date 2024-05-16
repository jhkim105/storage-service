package jhkim105.storage;

import static org.assertj.core.api.Assertions.assertThat;

import jhkim105.storage.common.config.ServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StorageServiceApplicationTests {

  @Autowired
  ServiceProperties serviceProperties;

  @Test
  void serviceProperties() {
    assertThat(serviceProperties.getStoragePath()).isNotBlank();
  }


}
