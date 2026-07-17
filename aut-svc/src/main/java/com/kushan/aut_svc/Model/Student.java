package com.kushan.aut_svc.Model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@DiscriminatorValue("student")
@PrimaryKeyJoinColumn(name = "user_id")
public class Student extends User {

    private String educationLevel;
    private String interest;
    private String goal;
}
