package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewNumber")
    private Integer reviewNumber;

    @ManyToOne
    @JoinColumn(name = "placeNumber", nullable = false)
    private Place place;

    @ManyToOne
    @JoinColumn(name = "userNumber", nullable = false)
    private User user;

    @Column(name = "reviewContent", length = 1000)
    private String reviewContent;

    @Column(name = "reviewRating")
    private Integer reviewRating;

    @Column(name = "reviewDate")
    private LocalDate reviewDate;
}
