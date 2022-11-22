package com.example.registration;

import lombok.Data;

@Data
public class Address {
    Long id;
    Long customerId;
    String city;
    String street;
}
