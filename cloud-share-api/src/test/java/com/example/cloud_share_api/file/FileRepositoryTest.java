package com.example.cloud_share_api.file;

import static com.example.cloud_share_api.TestUtils.LOUIS_USERNAME;
import static com.example.cloud_share_api.TestUtils.PETER_USERNAME;
import static com.example.cloud_share_api.TestUtils.createFile;
import static com.example.cloud_share_api.TestUtils.createTestUser;

import java.util.List;
import java.util.Optional;

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

  @BeforeEach
  void setup() {
    user = userRepository.save(createTestUser(PETER_USERNAME));
    
    File file = createFile("uuid");
    file.setUser(user);

    fileRepository.save(file);
  }

  @AfterEach
  void purge() {
    fileRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void canEstablishConncection() {
    Assertions.assertThat(psqlContainer.isCreated());
    Assertions.assertThat(psqlContainer.isRunning());
  }

  @Test
  void shouldReturn_fileOptional_ifFileExistsWithUUID() {
    final String uuid = "uuid";
    final Optional<File> result = fileRepository.findByUuid(uuid);
    Assertions.assertThat(result).isPresent();
  }

  @Test
  void shouldReturn_emptyOptional_ifFileNotExistsWithUUID() {
    final String uuid = "random-uuid";
    final Optional<File> result = fileRepository.findByUuid(uuid);
    Assertions.assertThat(result).isEmpty();
  }

  @Test
  void shouldReturn_fileList_ifFileExistsWithUser() {
    final User _user = user;
    final List<File> result = fileRepository.findByUser(_user);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(1);
  }

  @Test
  void shouldReturn_emptyFileList_ifFileNotExistsWithUser() {
    final User _user = createTestUser(LOUIS_USERNAME);
    _user.setId(300L);
    final List<File> result = fileRepository.findByUser(_user);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).isEmpty();
  }
}
