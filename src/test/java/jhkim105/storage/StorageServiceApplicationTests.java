package jhkim105.storage;

import static org.assertj.core.api.Assertions.assertThat;

import jhkim105.storage.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StorageServiceApplicationTests {

  @Autowired
  StorageProperties storageProperties;

  @Test
  void serviceProperties() {
    assertThat(storageProperties.getStoragePath()).isNotBlank();
  }


}
