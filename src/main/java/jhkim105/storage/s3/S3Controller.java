package jhkim105.storage.s3;

import jhkim105.storage.UploadResponse;
import jhkim105.storage.utils.FileUtils;
import jhkim105.storage.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;


@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
@Slf4j
public class S3Controller {

  private final S3Service s3Service;

  @PostMapping("/{bucketName}")
  public void createBucket(@PathVariable String bucketName) {
    s3Service.createBucket(bucketName);
  }

  @DeleteMapping("/{bucketName}")
  public void deleteBucket(@PathVariable String bucketName){
    s3Service.deleteBucket(bucketName);
  }

  @PostMapping("/{bucketName}/**")
  public ResponseEntity<UploadResponse> upload(MultipartHttpServletRequest multipartHttpServletRequest,
      @PathVariable String bucketName) {
    MultipartFile multipartFile = multipartHttpServletRequest.getFile("file");
    if (multipartFile == null || multipartFile.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    String key = RequestUtils.extractPath(multipartHttpServletRequest);
    s3Service.upload(bucketName, key, multipartFile);
    String url = String.format("%s/%s/%s",
        MvcUriComponentsBuilder.fromController(this.getClass()).build().toUri(), bucketName, key);

    return ResponseEntity.ok(UploadResponse.of(url));
  }

  @GetMapping("/{bucketName}/**")
  public ResponseEntity<Resource> download(@PathVariable String bucketName, HttpServletRequest request) {
    var key = RequestUtils.extractPath(request);
    Resource resource = s3Service.load(bucketName, key);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, FileUtils.contentType(key))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
            .filename(resource.getFilename())
            .build()
            .toString())
        .body(resource);
  }


  @DeleteMapping("/{bucketName}/**")
  public void delete(@PathVariable String bucketName, HttpServletRequest request) {
    String key = RequestUtils.extractPath(request);
    s3Service.deleteObject(bucketName, key);
  }


}
