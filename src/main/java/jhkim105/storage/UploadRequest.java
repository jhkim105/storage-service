package jhkim105.storage;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class UploadRequest {

  private String key;
  private int width;
  private int height;

}
