package com.example.cloud_share_api.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository.findByEmail(username).orElseThrow(
      () -> new UsernameNotFoundException(String.format("User with email '%s' not exists.", username))
    );
  }

  public UserDto getUserSummary(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    return new UserDto(user.getFirstname(), user.getLastname(), user.getEmail(), user.getCredit());
  }
}
