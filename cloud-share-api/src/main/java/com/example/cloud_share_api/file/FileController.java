package com.example.cloud_share_api.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class FileController {
  private final FileService fileService;

  @PostMapping()
  public ResponseEntity<List<FileDto>> uploadFile(List<MultipartFile> files, Authentication authentication) {
    return ResponseEntity.ok().body(fileService.uploadFile(files, authentication));
  }

  @GetMapping("/my")
  public ResponseEntity<List<FileDto>> getCurrentUserFiles(Authentication authentication) {
    return ResponseEntity.status(HttpStatus.OK).body(fileService.getCurrentUserFiles(authentication));
  }

  @GetMapping("/{id}")
  public ResponseEntity<FileDto> getFileById(@PathVariable String id, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.OK).body(fileService.getFileById(id, authentication));
  }

  @GetMapping("/public/{id}")
  public ResponseEntity<FileDto> getPublicFileById(@PathVariable String id) {
    return ResponseEntity.status(HttpStatus.OK).body(fileService.getPublicFileById(id));
  }

  @GetMapping("/public/download/{id}")
  public ResponseEntity<Resource> downloadPublicFileById(@PathVariable String id) {
    File fileMetadata = fileService.downloadPublicFile(id);
    Resource resource;
    try {
      resource = new UrlResource(fileMetadata.getLocation());
      String contentType = fileMetadata.getType();
      if (contentType == null) {
        contentType = "application/octet-stream";
      }

      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, contentType);
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileMetadata.getName() + "\"");

      return ResponseEntity.ok()
        .headers(headers)
        .contentLength(resource.contentLength())
        .contentType(MediaType.parseMediaType(contentType))
        .build();
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/download/{id}")
  public ResponseEntity<Resource> downloadFileById(@PathVariable String id, Authentication authentication) {
    File fileMetadata = fileService.downloadFile(id, authentication);
    
    try {
      Path filePath = Paths.get(fileMetadata.getLocation()).normalize();
      Resource resource = new UrlResource(filePath.toUri());
      String contentType = fileMetadata.getType();
      if (contentType == null) {
        contentType = "application/octet-stream";
      }

      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, contentType);
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileMetadata.getName() + "\"");

      return ResponseEntity.ok()
        .headers(headers)
        .contentLength(resource.contentLength())
        .contentType(MediaType.parseMediaType(contentType))
        .body(resource);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String id, Authentication authentication) {
    fileService.deleteFile(id, authentication);
    return ResponseEntity.status(HttpStatus.OK).body(Map.of("deleted", id));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<FileDto> updateVisiblity(@PathVariable String id, @RequestParam(required = true) boolean toggle, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.OK).body(fileService.updateVisiblity(id, toggle, authentication));
  }
}
