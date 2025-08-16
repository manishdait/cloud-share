package com.example.cloud_share_api.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.cloud_share_api.infrastructure.exceptions.AccessDeniedException;
import com.example.cloud_share_api.infrastructure.exceptions.InsufficentCreditException;
import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
  private final UserRepository userRepository;
  private final FileRepository fileRepository;

  @Value("${spring.application.file.upload-dir}")
  private String uploadDir;
  
  @Transactional
  public List<FileDto> uploadFile(List<MultipartFile> fileRequest, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    
    if (user.getCredit() < fileRequest.size()) {
      throw new InsufficentCreditException();
    }
    
    List<File> files = handleFileUpload(user, fileRequest);
    user.setCredit(user.getCredit() - files.size());
    
    userRepository.save(user);
    fileRepository.saveAll(files);

    return files.stream()
      .map(f -> mapTODto(f))
      .toList();
  }

  public List<FileDto> getCurrentUserFiles(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    return fileRepository.findByUser(user).stream()
      .map(f -> mapTODto(f))
      .toList();
  }

  public FileDto getFileById(String uuid, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    File file = findByUUID(uuid);

    if (!file.getUser().getUsername().equals(user.getUsername())) {
      throw new AccessDeniedException();
    }
    
    return mapTODto(file);
  }

  public FileDto getPublicFileById(String uuid) {
    File file = findByUUID(uuid);

    if (!file.isPublic()) {
      throw new AccessDeniedException();
    }

    return mapTODto(file);
  }

  @Transactional
  public FileDto updateVisiblity(String uuid, boolean isPublic, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    File file = findByUUID(uuid);

    if (!file.getUser().getUsername().equals(user.getUsername())) {
      throw new AccessDeniedException();
    }

    if (file.isPublic() == isPublic) {
      return mapTODto(file);
    }

    file.setPublic(isPublic);
    return mapTODto(file);
  }

  public void deleteFile(String uuid, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    File file = findByUUID(uuid);

    if (!file.getUser().getUsername().equals(user.getUsername())) {
      throw new AccessDeniedException();
    }

    try {
      Files.delete(Path.of(file.getLocation()));
      fileRepository.delete(file);
    } catch (IOException e) {
      log.error("Error deleting file: {}", e.getMessage());
      e.printStackTrace();
    }
  }

  public File downloadPublicFile(String uuid) {
    File file = findByUUID(uuid);
    if (!file.isPublic()) {
      throw new AccessDeniedException();
    }
    
    return file;
  }

    public File downloadFile(String uuid, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    File file = findByUUID(uuid);

    if (!file.getUser().getUsername().equals(user.getUsername())) {
      throw new AccessDeniedException();
    }
    
    return file;
  }

  private List<File> handleFileUpload(User user, List<MultipartFile> files) {
    List<File> list = new ArrayList<>();

    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

    try {
      Files.createDirectories(uploadPath);
    } catch (IOException e) {
      log.error("Error creating directory: {}", e.getMessage());
      e.printStackTrace();
    }

    for (MultipartFile file : files) {
      String randomUUID = UUID.randomUUID().toString();
      String fileName = randomUUID + "." + StringUtils.getFilenameExtension(file.getOriginalFilename());

      try {
        Files.copy(file.getInputStream(), Paths.get(uploadPath.toAbsolutePath().toString(), fileName));
        File metadata = File.builder()
          .uuid(randomUUID)
          .name(file.getOriginalFilename())
          .type(file.getContentType())
          .size(file.getSize())
          .isPublic(false)
          .user(user)
          .uploadedAt(Instant.now())
          .location(uploadPath.toAbsolutePath().toString() + "/" + fileName)
          .build();
        
        list.add(metadata);
      } catch (IOException e) {
        log.error("Error saving file: {}", e.getMessage());
        e.printStackTrace();
      }
    }

    return list;
  }

  private File findByUUID(String uuid) {
    return fileRepository.findByUuid(uuid).orElseThrow(
      () -> new EntityNotFoundException("File not found")
    );
  }

  private FileDto mapTODto(File file) {
    return new FileDto(
      file.getUuid(), 
      file.getName(), 
      file.getType(), 
      file.getSize(), 
      file.isPublic(), 
      file.getUploadedAt()
    );
  }
}
