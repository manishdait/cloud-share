package com.example.cloud_share_api.user;

import static com.example.cloud_share_api.TestUtils.PETER_USERNAME;
import static com.example.cloud_share_api.TestUtils.createTestUser;
import static org.mockito.ArgumentMatchers.eq;
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
    user = createTestUser(PETER_USERNAME);
    userService = new UserService(userRepository);
  }

  @AfterEach
  void purge() {
    user = null;
    userService = null;
  }

  @Test
  void shoulReturn_userDetails_onLoadUser_ifUserExists() {
    user.setId(101L);

    final String username = PETER_USERNAME;
    
    when(userRepository.findByEmail(username))
      .thenReturn(Optional.of(user));
    
    final UserDetails result = userService.loadUserByUsername(username);

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getUsername()).isEqualTo(username);
    
    verify(userRepository, times(1))
      .findByEmail(eq(PETER_USERNAME));
  }

  @Test
  void shoulThrow_exception_onLoadUser_ifUserNotExists() {
    final String username = PETER_USERNAME;
    
    when(userRepository.findByEmail(username)).thenReturn(Optional.empty());
    
    Assertions.assertThatThrownBy(() -> userService.loadUserByUsername(username))
      .isInstanceOf(UsernameNotFoundException.class);
  }
}
