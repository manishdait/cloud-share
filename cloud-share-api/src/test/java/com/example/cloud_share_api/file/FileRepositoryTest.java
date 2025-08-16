package com.example.cloud_share_api.file;

import static com.example.cloud_share_api.TestUtils.createFile;
import static com.example.cloud_share_api.TestUtils.createTestUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FileRepositoryTest {
  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> psqlContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:alpine"));

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FileRepository fileRepository;

  private User user;
  private String fileUUID;

  @BeforeEach
  void setup() {
    user = userRepository.save(createTestUser("user@test.in", "password"));
    
    fileUUID = UUID.randomUUID().toString();
    File file = createFile(fileUUID);
    file.setUser(user);

    fileRepository.save(file);
  }

  @AfterEach
  void purge() {
    fileRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void shouldConnectToDatabase() {
    Assertions.assertThat(psqlContainer.isCreated());
    Assertions.assertThat(psqlContainer.isRunning());
  }

  @Test
  void shouldFindFileByUuid_whenFileExists() {
    final String uuid = fileUUID;
    final Optional<File> result = fileRepository.findByUuid(uuid);
    Assertions.assertThat(result).isPresent();
  }

  @Test
  void shouldReturnEmptyOptional_whenFileDoesNotExist() {
    final String uuid = "random-uuid";
    final Optional<File> result = fileRepository.findByUuid(uuid);
    Assertions.assertThat(result).isEmpty();
  }

  @Test
  void shouldFindAllFilesByUser_whenFilesExist() {
    final List<File> result = fileRepository.findByUser(user);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(1);
  }

  @Test
  void shouldReturnEmptyList_whenNoFilesExistForUser() {
    final User user = createTestUser("anotheruser@test.in", "password");
    user.setId(1L);

    final List<File> result = fileRepository.findByUser(user);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).isEmpty();
  }
}
