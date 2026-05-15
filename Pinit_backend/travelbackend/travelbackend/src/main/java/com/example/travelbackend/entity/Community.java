package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "Community")
@Getter
@Setter
@NoArgsConstructor
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "communityNumber")
    private Integer communityNumber;

    @ManyToOne
    @JoinColumn(name = "userNumber", nullable = false)
    private User user;

    @Column(name = "communityTitle")
    private String communityTitle;

    @Column(name = "communityContent", length = 2000)
    private String communityContent;

    @Column(name = "communityDate")
    private LocalDate communityDate;

    @Column(name = "communityCategory")
    private String communityCategory;
}
