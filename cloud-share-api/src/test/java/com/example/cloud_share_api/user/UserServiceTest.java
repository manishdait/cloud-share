package com.example.cloud_share_api.user;

import static com.example.cloud_share_api.TestUtils.createTestUser;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @Mock
  private UserRepository userRepository;

  private UserService userService;
  
  private User user;

  @BeforeEach
  void setup() {
    user = createTestUser("user@test.in", "password"); 
    userService = new UserService(userRepository);
  }

  @AfterEach
  void purge() {
    user = null;
    userService = null;
  }

  @Test
  void shouldReturnUserDetails_whenUserExists() {
    final String username = "user@test.in";
    when(userRepository.findByEmail(username)).thenReturn(Optional.of(user));
  
    final UserDetails result = userService.loadUserByUsername(username);

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getUsername()).isEqualTo(username);
    
    verify(userRepository, times(1)).findByEmail(username);
  }

  @Test
  void shouldThrowUsernameNotFoundException_whenUserDoesNotExist() {
    final String username = "anotheruser@test.in";
    when(userRepository.findByEmail(username)).thenReturn(Optional.empty());
    
    Assertions.assertThatThrownBy(() -> userService.loadUserByUsername(username))
      .isInstanceOf(UsernameNotFoundException.class);
    
    verify(userRepository, times(1)).findByEmail(username);
  }
}
