package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "Follow",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"followerNumber", "followingNumber"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "followNumber")
    private Integer followNumber;

    @ManyToOne
    @JoinColumn(name = "followerNumber", nullable = false)
    private User follower;

    @ManyToOne
    @JoinColumn(name = "followingNumber", nullable = false)
    private User following;

    @Column(name = "followDate")
    private LocalDateTime followDate = LocalDateTime.now();
}
