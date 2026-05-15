package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "User")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userNumber")
    private Integer userNumber;

    @Column(name = "userName")
    private String userName;

    @Column(name = "userID")
    private String userID;

    @Column(name = "userPW")
    private String userPW;

    @Column(name = "userEmail")
    private String userEmail;

    @Column(name = "userGender")
    private String userGender;

    @Column(name = "userBirth")
    private java.time.LocalDate userBirth;

    @Column(name = "userTemperature")
    private Double userTemperature;
}