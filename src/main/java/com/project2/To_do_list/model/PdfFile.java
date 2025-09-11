package com.project2.To_do_list.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class PdfFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // REMOVE filePath usage, OR keep it only for debugging
    private String filePath;

    private LocalDateTime uploadedAt;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;   // <-- Store PDF file content here

    // Link PDF to User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
