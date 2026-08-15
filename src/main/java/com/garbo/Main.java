package com.garbo;

import org.springframework.boot.SpringApplication;
import com.google.ortools.Loader;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {
    public static void main(String[] args) {
        // Must load before Spring — OR-Tools native libs need libgomp1 in Docker.
        Loader.loadNativeLibraries();
        SpringApplication.run(Main.class, args);
    }
}






























