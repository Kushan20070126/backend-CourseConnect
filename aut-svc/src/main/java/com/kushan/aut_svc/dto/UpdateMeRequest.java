package com.kushan.aut_svc.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Fields a user may update on their own profile. Any {@code null} field is left
 * unchanged. Email and password are intentionally excluded here (they need
 * dedicated verification flows).
 */
@Setter
@Getter
public class UpdateMeRequest {

    private String firstName;
    private String lastName;
    private Integer age;

    // Student-only fields
    private String educationLevel;
    private String interest;
    private String goal;

    // Lecturer-only fields
    private String title;
    private Integer experience;
    private String area;
    private String bio;

}
