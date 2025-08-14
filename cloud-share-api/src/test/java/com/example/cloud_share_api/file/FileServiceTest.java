package com.example.cloud_share_api.file;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.example.cloud_share_api.infrastructure.exceptions.AccessDeniedException;
import com.example.cloud_share_api.infrastructure.exceptions.InsufficentCreditException;
import com.example.cloud_share_api.user.User;
import com.example.cloud_share_api.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {
  
  @Mock
  private UserRepository userRepository;
  
  @Mock
  private FileRepository fileRepository;

  @Mock
  private Authentication authentication;
  
  private FileService fileService;

  private User user;
  private File file;
  private String fileUuid;

  @BeforeEach
  void setUp() {
    user = User.builder()
      .id(1L)
      .email("testuser")
      .credit(10)
      .build();

    fileUuid = UUID.randomUUID().toString();
    file = File.builder()
      .id(1L)
      .uuid(fileUuid)
      .name("testfile.txt")
      .type("text/plain")
      .size(1234L)
      .isPublic(false)
      .location("uploads/testfile.txt")
      .user(user)
      .uploadedAt(Instant.now())
      .build();

    fileService = new FileService(userRepository, fileRepository);
  }

  @AfterEach
  void purge() {
    user = null;
    fileUuid = null;
    file = null;
    fileService = null;
  }

  @Test
  void testUploadFile_Success() throws IOException {
    MockMultipartFile mockFile = new MockMultipartFile(
      "file", 
      "test.txt", 
      "text/plain", 
      "some content".getBytes()
    );
    List<MultipartFile> fileList = List.of(mockFile);

    when(authentication.getPrincipal()).thenReturn(user);

    try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
      mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(Paths.get("uploads"));
      mockedFiles.when(() -> Files.copy(any(InputStream.class), any(Path.class))).thenReturn(100L);

      when(fileRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.save(any(User.class))).thenReturn(user);

      List<FileDto> result = fileService.uploadFile(fileList, authentication);

      Assertions.assertThat(result).isNotNull().hasSize(1);
      Assertions.assertThat(result.get(0).name()).isEqualTo("test.txt");
      Assertions.assertThat(user.getCredit()).isEqualTo(9);

      verify(userRepository, times(1)).save(user);
      verify(fileRepository, times(1)).saveAll(anyList());
    }
  }

  @Test
  void testUploadFile_InsufficentCredit() {
    user.setCredit(0);
    MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
    List<MultipartFile> fileList = List.of(mockFile);

    when(authentication.getPrincipal()).thenReturn(user);

    Assertions.assertThatThrownBy(() -> fileService.uploadFile(fileList, authentication))
      .isInstanceOf(InsufficentCreditException.class);
   
    verify(userRepository, never()).save(any(User.class));
    verify(fileRepository, never()).saveAll(anyList());
  }

  @Test
  void testGetCurrentUserFiles_Success() {
    when(authentication.getPrincipal()).thenReturn(user);
    when(fileRepository.findByUser(user)).thenReturn(List.of(file));

    List<FileDto> result = fileService.getCurrentUserFiles(authentication);

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result).hasSize(1);
    Assertions.assertThat(result.get(0).name()).isEqualTo("testfile.txt");
  }

  @Test
  void testGetFileById_Success() {
    when(authentication.getPrincipal()).thenReturn(user);
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.of(file));

    FileDto result = fileService.getFileById(fileUuid, authentication);

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.name()).isEqualTo("testfile.txt");
  }

  @Test
  void testGetFileById_AccessDenied() {
    User anotherUser = User.builder().email("anotheruser").build();
    when(authentication.getPrincipal()).thenReturn(anotherUser);
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.of(file));

    Assertions.assertThatThrownBy(() -> fileService.getFileById(fileUuid, authentication))
      .isInstanceOf(AccessDeniedException.class);
  }
    
  @Test
  void testGetFileById_NotFound() {
    when(authentication.getPrincipal()).thenReturn(user);
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.empty());

    Assertions.assertThatThrownBy(() -> fileService.getFileById(fileUuid, authentication))
      .isInstanceOf(EntityNotFoundException.class);
  }
    
  @Test
  void testGetPublicFileById_Success() {
    file.setPublic(true);
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.of(file));

    FileDto result = fileService.getPublicFileById(fileUuid);

    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.isPublic()).isTrue();
  }

  @Test
  void testGetPublicFileById_AccessDenied() {
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.of(file));

    Assertions.assertThatThrownBy(() -> fileService.getPublicFileById(fileUuid))
      .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void testUpdateVisibility_Success() {
    when(authentication.getPrincipal()).thenReturn(user);
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.of(file));

    FileDto result = fileService.updateVisiblity(fileUuid, true, authentication);
        
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.isPublic()).isTrue();
  }

  @Test
  void testDeleteFile_Success() throws IOException {
    when(authentication.getPrincipal()).thenReturn(user);
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.of(file));

    try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
      mockedFiles.when(() -> Files.delete(any(Path.class))).thenAnswer(invocation -> null);
      doNothing().when(fileRepository).delete(file);

      fileService.deleteFile(fileUuid, authentication);
            
      verify(fileRepository, times(1)).delete(file);
      mockedFiles.verify(() -> Files.delete(Path.of(file.getLocation())));
    }
  }
    
  @Test
  void testDownloadPublicFile_Success() {
    file.setPublic(true);
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.of(file));
        
    File result = fileService.downloadPublicFile(fileUuid);
        
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getName()).isEqualTo("testfile.txt");
  }

  @Test
  void testDownloadFile_Success() {
    when(authentication.getPrincipal()).thenReturn(user);
    when(fileRepository.findByUuid(fileUuid)).thenReturn(Optional.of(file));
        
    File result = fileService.downloadFile(fileUuid, authentication);
        
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getName()).isEqualTo("testfile.txt");
  }
}