package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "Scrap",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userNumber", "communityNumber"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Scrap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrapNumber")
    private Integer scrapNumber;

    @ManyToOne
    @JoinColumn(name = "userNumber", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "communityNumber", nullable = false)
    private Community community;

    @Column(name = "scrapDate")
    private LocalDateTime scrapDate = LocalDateTime.now();
}
