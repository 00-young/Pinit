package com.example.travelbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Place")
@Getter
@Setter
public class Place {
    @Id
    @Column(name = "placeNumber")
    private Integer placeNumber;

    @Column(name = "placeName")
    private String placeName;

    @Column(name = "placeAddress")
    private String placeAddress;

    @Column(name = "placeCategory")
    private String placeCategory;

    @Column(name = "placeContent", length = 1000)
    private String placeContent;

    @Column(name = "placeInfo", length = 1000)
    private String placeInfo;

    @Column(name = "placeTag")
    private String placeTag;

    @Column(name = "placeLongitude")
    private Double placeLongitude;

    @Column(name = "placeLatitude")
    private Double placeLatitude;

    @Column(name = "placeReviewCount")
    private Integer placeReviewCount;
}