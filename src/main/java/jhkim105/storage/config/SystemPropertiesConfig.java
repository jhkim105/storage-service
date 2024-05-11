package jhkim105.storage.config;

import java.util.Properties;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemPropertiesConfig {

  public static final String IMAGEMAGICK_PATH = "IMAGEMAGICK_PATH";
  public static final String GIFSICLE_PATH = "GIFSICLE_PATH";
  public static final String STORAGE_PATH = "STORAGE_PATH";
  @Bean
  public MethodInvokingFactoryBean systemPropertiesBean(StorageProperties storageProperties) {
    MethodInvokingFactoryBean bean = new MethodInvokingFactoryBean();
    bean.setTargetObject(System.getProperties());
    bean.setTargetMethod("putAll");

    Properties props = new Properties();
    props.setProperty(STORAGE_PATH, storageProperties.getStoragePath());
    props.setProperty(IMAGEMAGICK_PATH, storageProperties.getImageMagickPath());
    props.setProperty(GIFSICLE_PATH, storageProperties.getGifsiclePath());
    bean.setArguments(props);
    return bean;
  }
}
