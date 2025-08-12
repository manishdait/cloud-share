package com.example.cloud_share_api.file;

import java.time.Instant;

import com.example.cloud_share_api.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "file")
public class File {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_seq_generator")
  @SequenceGenerator(name = "file_seq_generator", sequenceName = "file_seq", allocationSize = 1, initialValue = 101)
  private Long id;

  @Column(name = "uuid", unique = true, nullable = false)
  private String uuid;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "size", nullable = false)
  private Long size;

  @Column(name = "location", nullable = false)
  private String location;

  @Column(name = "is_public")
  private boolean isPublic;

  @Column(name = "uploaded_at", nullable = false, updatable = false)
  private Instant uploadedAt;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
