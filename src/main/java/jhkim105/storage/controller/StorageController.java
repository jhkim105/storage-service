package jhkim105.storage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jhkim105.storage.service.StorageService;
import jhkim105.storage.common.utils.FileUtils;
import jhkim105.storage.common.utils.RequestUtils;
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
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class StorageController {

  private final StorageService storageService;

  @PostMapping("/{bucketName}")
  public void createBucket(@PathVariable String bucketName) {
    storageService.createBucket(bucketName);
  }


  @DeleteMapping("/{bucketName}")
  public void deleteBucket(@PathVariable String bucketName) {
    storageService.deleteBucket(bucketName);
  }

  @PostMapping("/{bucketName}/**")
  public ResponseEntity<UploadResponse> upload(MultipartHttpServletRequest multipartHttpServletRequest, @PathVariable String bucketName) {
    MultipartFile multipartFile = multipartHttpServletRequest.getFile("file");
    if (multipartFile == null || multipartFile.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    String key = RequestUtils.extractPath(multipartHttpServletRequest);
    storageService.upload(bucketName, key, multipartFile);
    String url = String.format("%s/%s/%s",
        MvcUriComponentsBuilder.fromController(this.getClass()).build().toUri(), bucketName, key);

    return ResponseEntity.ok(UploadResponse.of(url));
  }

  @GetMapping("/{bucketName}/**")
  public ResponseEntity<Resource> download(@PathVariable String bucketName, ResizeRequest resizeRequest, HttpServletRequest request) {
    var key = RequestUtils.extractPath(request);
    var resource = storageService.load(bucketName, key);
    if(resizeRequest.hasValue()) {
      resource = storageService.load(bucketName, key, resizeRequest.w(), resizeRequest.h());
    } else {
      resource = storageService.load(bucketName, key);
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, FileUtils.contentType(key))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
            .filename(resource.getFilename())
            .build()
            .toString())
        .body(resource);
  }

  @DeleteMapping("/{bucketName}/**")
  public void deleteObject(@PathVariable String bucketName,
      HttpServletRequest request) {
    String key = RequestUtils.extractPath(request);
    storageService.deleteResizedImages(bucketName, key);
    storageService.deleteObject(bucketName, key);
  }



}