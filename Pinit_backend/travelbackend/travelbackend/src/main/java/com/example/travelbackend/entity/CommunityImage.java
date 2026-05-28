package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "communityimage")
@Getter
@Setter
@NoArgsConstructor
public class CommunityImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imageNumber")
    private Integer imageNumber;

    @ManyToOne
    @JoinColumn(name = "communityNumber", nullable = false)
    private Community community;

    @Column(name = "imageCreateDate")
    private LocalDate imageCreateDate;

    @Column(name = "imageSize")
    private Integer imageSize;

    @Column(name = "imageOriginalName")
    private String imageOriginalName;

    @Column(name = "imageStoredName")
    private String imageStoredName;
}
