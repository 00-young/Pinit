package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Budget")
@Getter
@Setter
@NoArgsConstructor
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budgetNumber")
    private Integer budgetNumber;

    @ManyToOne
    @JoinColumn(name = "plannerNumber")
    private Planner planner;

    @Column(name = "budgetTitle")
    private String budgetTitle;

    @Column(name = "budgetAmount")
    private Double budgetAmount;

    @Column(name = "budgetCategory")
    private String budgetCategory;

    @Column(name = "budgetDate")
    private java.time.LocalDate budgetDate;

    @Column(name = "budgetType")
    private String budgetType;
}