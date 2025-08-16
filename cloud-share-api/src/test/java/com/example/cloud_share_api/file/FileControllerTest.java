package com.example.cloud_share_api.file;

import static com.example.cloud_share_api.TestUtils.createTestUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.cloud_share_api.auth.AuthRequest;
import com.example.cloud_share_api.auth.AuthResponse;
import com.example.cloud_share_api.infrastructure.handler.ErrorResponse;
import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "test")
class FileControllerTest {
  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> psqlContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:alpine"));

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FileRepository fileRepository;
  
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private TestRestTemplate restTemplate;

  private final String BASE_URL = "/api/v1/uploads";
  private final String AUTH_URL = "/api/v1/auth";

  private String authToken;
  private User user;
  private Path uploadPath;

  @BeforeEach
  void setup() throws IOException {
    fileRepository.deleteAll();
    userRepository.deleteAll();

    this.uploadPath = Path.of("uploads-test");
    if (!Files.exists(uploadPath)) {
      Files.createDirectories(uploadPath);
    }
    
    this.user = createTestUser("user@test.in", passwordEncoder.encode("password"));
    user.setVerify(true);
    userRepository.save(user);

    this.authToken = getAuthToken("user@test.in", "password");
  }

  @AfterEach
  void purge() throws IOException {
    fileRepository.deleteAll();
    userRepository.deleteAll();

    if (Files.exists(uploadPath)) {
      try (Stream<Path> files = Files.list(uploadPath)) {
        files.forEach(file -> {
          try {
            Files.delete(file);
          } catch (IOException e) {
              throw new RuntimeException("Failed to delete test file", e);
          }
        });
      }
      Files.delete(uploadPath);
    }
  }

  @Test
  void testUploadFile_Success() throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authToken);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("files", new ByteArrayResource("test content".getBytes()) {
      @Override
      public String getFilename() {
        return "test_file.txt";
      }
    });

    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

    ResponseEntity<List<FileDto>> response = restTemplate.exchange(
      BASE_URL,
      HttpMethod.POST,
      request,
      new ParameterizedTypeReference<List<FileDto>>() {}
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(response.getBody()).isNotNull();
    Assertions.assertThat(response.getBody()).hasSize(1);
    Assertions.assertThat(response.getBody().get(0).name()).isEqualTo("test_file.txt");
    
    Path uploadedFilePath = findFileInUploadsDir(response.getBody().get(0).uuid());
    Assertions.assertThat(Files.exists(uploadedFilePath)).isTrue();
    
    User updatedUser = userRepository.findById(user.getId()).orElseThrow();
    Assertions.assertThat(updatedUser.getCredit()).isEqualTo(4);
  }

  @Test
  void testUploadFile_WithoutToken_ShouldReturnForbidden() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("files", new ByteArrayResource("test content".getBytes()) {
      @Override
      public String getFilename() {
        return "test_file.txt";
      }
    });

    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
      BASE_URL, 
      HttpMethod.POST, 
      request, 
      ErrorResponse.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void testUploadFile_WithInsufficientCredit_ShouldReturnBadRequest() {
    User lowCreditUser = createTestUser("lowcredit@user.com", passwordEncoder.encode("password"));
    lowCreditUser.setCredit(0);
    lowCreditUser.setVerify(true);
    userRepository.save(lowCreditUser);

    String lowCreditAuthToken = getAuthToken("lowcredit@user.com", "password");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(lowCreditAuthToken);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("files", new ByteArrayResource("test content".getBytes()) {
      @Override
      public String getFilename() {
        return "test_file.txt";
      }
    });

    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(BASE_URL, HttpMethod.POST, request, ErrorResponse.class);

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void testGetCurrentUserFiles_Success() {
    File uploadedFile = uploadTestFile("test_file.txt", user);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authToken);

    ResponseEntity<List<FileDto>> response = restTemplate.exchange(
      BASE_URL + "/my",
      HttpMethod.GET,
      new HttpEntity<>(null, headers),
      new org.springframework.core.ParameterizedTypeReference<List<FileDto>>() {}
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(response.getBody()).isNotNull();
    Assertions.assertThat(response.getBody()).hasSize(1);
    Assertions.assertThat(response.getBody().get(0).name()).isEqualTo(uploadedFile.getName());
  }

  @Test
  void testGetFileById_Success() {
    File uploadedFile = uploadTestFile("test_file.txt", user);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authToken);

    ResponseEntity<FileDto> response = restTemplate.exchange(
      BASE_URL + "/" + uploadedFile.getUuid(),
      HttpMethod.GET,
      new HttpEntity<>(null, headers),
      FileDto.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(response.getBody()).isNotNull();
    Assertions.assertThat(response.getBody().uuid()).isEqualTo(uploadedFile.getUuid());
  }

  @Test
  void testGetFileById_NonExistentFile_ShouldReturnNotFound() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authToken);
    String nonExistentUuid = UUID.randomUUID().toString();

    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
      BASE_URL + "/" + nonExistentUuid,
      HttpMethod.GET,
      new HttpEntity<>(null, headers),
      ErrorResponse.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void testGetFileById_AccessDenied_ShouldReturnForbidden() {
    File uploadedFile = uploadTestFile("other_user_file.txt", user);

    // Create a second user and get their token
    User otherUser = createTestUser("otheruser@test.in", passwordEncoder.encode("password"));
    otherUser.setVerify(true);
    userRepository.save(otherUser);
    String otherUserToken = getAuthToken("otheruser@test.in", "password");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(otherUserToken);

    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
      BASE_URL + "/" + uploadedFile.getUuid(),
      HttpMethod.GET,
      new HttpEntity<>(null, headers),
      ErrorResponse.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
  
  @Test
  void testGetPublicFileById_Success() {
    File publicFile = uploadTestFile("public_file.txt", user);
    publicFile.setPublic(true);
    fileRepository.save(publicFile);

    ResponseEntity<FileDto> response = restTemplate.getForEntity(
      BASE_URL + "/public/" + publicFile.getUuid(),
      FileDto.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(response.getBody()).isNotNull();
    Assertions.assertThat(response.getBody().uuid()).isEqualTo(publicFile.getUuid());
    Assertions.assertThat(response.getBody().isPublic()).isTrue();
  }

  @Test
  void testGetPublicFileById_PrivateFile_ShouldReturnForbidden() {
    File privateFile = uploadTestFile("private_file.txt", user);
    
    ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
      BASE_URL + "/public/" + privateFile.getUuid(),
      ErrorResponse.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void testDownloadFileById_Success() {
    File uploadedFile = uploadTestFile("download_me.txt", user);
    
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authToken);

    ResponseEntity<Resource> response = restTemplate.exchange(
      BASE_URL + "/download/" + uploadedFile.getUuid(),
      HttpMethod.GET,
      new HttpEntity<>(null, headers),
      Resource.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("download_me.txt");
    Assertions.assertThat(response.getHeaders().getContentLength()).isEqualTo(12L);
  }

  @Test
  void testDownloadPublicFileById_PrivateFile_ShouldReturnForbidden() {
    File privateFile = uploadTestFile("private_file.txt", user);

    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
      BASE_URL + "/public/download/" + privateFile.getUuid(),
      HttpMethod.GET,
      HttpEntity.EMPTY,
      ErrorResponse.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void testUpdateVisibility_Success() {
    File uploadedFile = uploadTestFile("private_file.txt", user);
    Assertions.assertThat(uploadedFile.isPublic()).isFalse();
    
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authToken);

    ResponseEntity<FileDto> response = restTemplate.exchange(
      BASE_URL + "/" + uploadedFile.getUuid() + "?toggle=true",
      HttpMethod.PATCH,
      new HttpEntity<>(null, headers),
      FileDto.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(response.getBody()).isNotNull();
    Assertions.assertThat(response.getBody().isPublic()).isTrue();
    
    // Verify persistence
    File updatedFile = fileRepository.findByUuid(uploadedFile.getUuid()).orElseThrow();
    Assertions.assertThat(updatedFile.isPublic()).isTrue();
  }

  @Test
  void testUpdateVisibility_AccessDenied_ShouldReturnForbidden() {
    File uploadedFile = uploadTestFile("other_user_file.txt", user);
    
    User otherUser = createTestUser("otheruser@test.in", passwordEncoder.encode("password"));
    otherUser.setVerify(true);
    userRepository.save(otherUser);
    String otherUserToken = getAuthToken("otheruser@test.in", "password");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(otherUserToken);

    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
      BASE_URL + "/" + uploadedFile.getUuid() + "?toggle=true",
      HttpMethod.PATCH,
      new HttpEntity<>(null, headers),
      ErrorResponse.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void testDeleteFile_Success() {
    File uploadedFile = uploadTestFile("to_delete.txt", user);
    String fileUuid = uploadedFile.getUuid();
    Path uploadedFilePath = findFileInUploadsDir(fileUuid);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(authToken);

    ResponseEntity<Map<String, String>> response = restTemplate.exchange(
      BASE_URL + "/" + fileUuid,
      HttpMethod.DELETE,
      new HttpEntity<>(null, headers),
      new org.springframework.core.ParameterizedTypeReference<Map<String, String>>() {}
    );
    
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertThat(response.getBody()).containsEntry("deleted", fileUuid);
    
    // Verify file is gone from filesystem and database
    Assertions.assertThat(Files.exists(uploadedFilePath)).isFalse();
    Assertions.assertThat(fileRepository.findByUuid(fileUuid)).isEmpty();
  }
  
  @Test
  void testDeleteFile_AccessDenied_ShouldReturnForbidden() {
    File uploadedFile = uploadTestFile("other_user_file.txt", user);
    
    User otherUser = createTestUser("otheruser@test.in", passwordEncoder.encode("password"));
    otherUser.setVerify(true);
    userRepository.save(otherUser);
    String otherUserToken = getAuthToken("otheruser@test.in", "password");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(otherUserToken);

    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
      BASE_URL + "/" + uploadedFile.getUuid(),
      HttpMethod.DELETE,
      new HttpEntity<>(null, headers),
      ErrorResponse.class
    );

    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
  
  // Helper methods

  private String getAuthToken(String email, String password) {
    AuthRequest authRequest = new AuthRequest(email, password);
    ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
      AUTH_URL + "/login", 
      authRequest, 
      AuthResponse.class
    );
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody().accessToken();
  }

  private File uploadTestFile(String fileName, User fileOwner) {
    String randomUuid = UUID.randomUUID().toString();
    String location = Path.of(uploadPath.toString(), randomUuid + ".txt").toString();
    try {
      Files.writeString(Path.of(location), "test content");
    } catch (IOException e) {
      throw new RuntimeException("Failed to write test file", e);
    }

    File file = File.builder()
      .uuid(randomUuid)
      .name(fileName)
      .type("text/plain")
      .size(12L)
      .isPublic(false)
      .user(fileOwner)
      .uploadedAt(Instant.now())
      .location(location)
      .build();
    return fileRepository.save(file);
  }

  private Path findFileInUploadsDir(String uuid) {
    System.out.println(Files.exists(uploadPath));
    try (Stream<Path> paths = Files.list(uploadPath)) {
      return paths
        .filter(p -> {
          System.out.println(p.getFileName().toString());
          System.out.println(uuid);
          System.out.println(p.getFileName().toString().contains(uuid));
          return p.getFileName().toString().contains(uuid);
        })
        .findFirst()
        .orElse(null);
    } catch (IOException e) {
      throw new RuntimeException("Error listing files in test upload directory", e);
    }
  }
}