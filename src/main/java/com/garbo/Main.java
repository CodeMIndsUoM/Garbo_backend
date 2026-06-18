package com.garbo;

import org.springframework.boot.SpringApplication;
import com.google.ortools.Loader;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {
    public static void main(String[] args) {
        // Must load before Spring starts — native libs fail on Docker until explicitly loaded.
        Loader.loadNativeLibraries();
        SpringApplication.run(Main.class, args);
    }
}






























