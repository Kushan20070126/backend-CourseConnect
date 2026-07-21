package com.kushan.aut_svc.Model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("lecturer")
@PrimaryKeyJoinColumn(name = "user_id")
public class Lecturer extends User {

    private String title;
    private Integer experience;
    private String area;
    private String bio;

    @Column(nullable = false, columnDefinition = "VARCHAR2(16) DEFAULT 'ACTIVE'")
    private String status = "ACTIVE";
}
