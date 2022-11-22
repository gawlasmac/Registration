package com.example.registration;

import lombok.*;

import java.util.List;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private String firstName;
    private String lastName;
    private boolean active;

    List<Address> addresses;
}

