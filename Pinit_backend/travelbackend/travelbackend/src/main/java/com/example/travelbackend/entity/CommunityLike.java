package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "communitylike",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userNumber", "communityNumber"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CommunityLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "likeNumber")
    private Integer likeNumber;

    @ManyToOne
    @JoinColumn(name = "userNumber", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "communityNumber", nullable = false)
    private Community community;

    @Column(name = "likeDate")
    private LocalDateTime likeDate = LocalDateTime.now();
}
