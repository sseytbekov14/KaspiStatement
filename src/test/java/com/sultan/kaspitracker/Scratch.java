package com.sultan.kaspitracker;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Scratch {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("changeme"));
    }
}
