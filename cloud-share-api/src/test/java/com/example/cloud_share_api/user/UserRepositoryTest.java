package com.example.cloud_share_api.user;

import static com.example.cloud_share_api.TestUtils.createTestUser;

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

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {
  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> psqlContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:alpine"));

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setup() {
    userRepository.save(createTestUser("user@test.in", "password")); 
  }

  @AfterEach
  void purge() {
    userRepository.deleteAll();
  }

  @Test
  void shouldConnectToDatabase() {
    Assertions.assertThat(psqlContainer.isCreated());
    Assertions.assertThat(psqlContainer.isRunning());
  }

  @Test
  void shouldFindUserByEmail_whenUserExists() {
    final String email = "user@test.in";
    final Optional<User> result = userRepository.findByEmail(email);

    Assertions.assertThat(result).isPresent();
  }

  @Test
  void shouldReturnEmptyOptional_whenUserDoesNotExist() {
    final String email = "anotheruser@test.in";
    final Optional<User> result = userRepository.findByEmail(email);

    Assertions.assertThat(result).isEmpty();
  }
}
