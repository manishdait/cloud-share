package com.example.cloud_share_api.file;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cloud_share_api.user.User;


public interface FileRepository extends JpaRepository<File, Long> {
  Optional<File> findByUuid(String uuid);
  List<File> findByUser(User user); 
}
