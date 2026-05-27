package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "User")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userNumber")
    private Integer userNumber;

    @Column(name = "firebaseUid", nullable = false, unique = true)
    private String firebaseUid;

    @Column(name = "userName")
    private String userName;

    @Column(name = "userNickname")
    private String userNickname;

    @Column(name = "userEmail")
    private String userEmail;

    @Column(name = "userProfileImage", length = 1000)
    private String userProfileImage;

    @Column(name = "userBio")
    private String userBio;

    @Column(name = "userGender", length = 10)
    private String userGender;

    @Column(name = "userBirth")
    private java.time.LocalDate userBirth;

    @Column(name = "userTemperature")
    private Double userTemperature = 36.5;
}