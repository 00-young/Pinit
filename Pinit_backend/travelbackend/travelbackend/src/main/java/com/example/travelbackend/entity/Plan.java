package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Plan")
@Getter
@Setter
@NoArgsConstructor
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planNumber")
    private Integer planNumber;

    @ManyToOne
    @JoinColumn(name = "plannerNumber")
    private Planner planner;

    @Column(name = "planDate")
    private java.time.LocalDate planDate;

    @Column(name = "placeLongitude")
    private Double placeLongitude;

    @Column(name = "placeLatitude")
    private Double placeLatitude;

    @Column(name = "planTime")
    private java.time.LocalTime planTime;

    @Column(name = "planMemo", length = 500)
    private String planMemo;

    @Column(name = "placeName")
    private String placeName;

    @Column(name = "planOrder")
    private Integer planOrder;
}