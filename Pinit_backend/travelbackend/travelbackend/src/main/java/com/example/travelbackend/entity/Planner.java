package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Planner")
@Getter
@Setter
@NoArgsConstructor
public class Planner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plannerNumber")
    private Integer plannerNumber;

    @ManyToOne
    @JoinColumn(name = "userNumber")
    private User user;

    @Column(name = "firstDate")
    private java.time.LocalDate firstDate;

    @Column(name = "lastDate")
    private java.time.LocalDate lastDate;

    @Column(name = "plannerTitle")
    private String plannerTitle;

    @Column(name = "plannerHit")
    private Integer plannerHit;
}