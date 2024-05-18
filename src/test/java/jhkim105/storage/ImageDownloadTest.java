package jhkim105.storage;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
//@ActiveProfiles("s3")
class ImageDownloadTest {

  @Autowired
  private MockMvc mockMvc;

  String bucketName = "company01";
  String objectKey = "a/test.jpg";
  String sourceFilePath = "src/test/resources/files/sample.jpeg";
  @BeforeAll
  void beforeAll() throws Exception{
    upload();
  }

  private void upload() throws Exception {
    MockMultipartFile multipartFile = new MockMultipartFile(
        "file", "sample.jpeg",
        MediaType.IMAGE_JPEG_VALUE,
        Files.readAllBytes(Paths.get(sourceFilePath)));

    mockMvc.perform(multipart(String.format("/%s/%s", bucketName, objectKey))
            .file(multipartFile))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("url").exists());
  }



  @Test
  void download() throws Exception{
    ResultActions resultActions = mockMvc.perform(
        MockMvcRequestBuilders
            .get(String.format("/%s/%s", bucketName, objectKey))
            .param("w", "300")
            .param("h", "300")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.IMAGE_JPEG));

    resultActions
        .andExpect(status().isOk());

    File downloadFile = new File(sourceFilePath);
    FileUtils.writeByteArrayToFile(downloadFile, resultActions.andReturn().getResponse().getContentAsByteArray());



  }



}
