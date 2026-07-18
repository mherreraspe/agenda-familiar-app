package com.obusystem.agendafamiliar.agenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AplicacionAgenda {
    public static void main(String[] args) {
        SpringApplication.run(AplicacionAgenda.class, args);
    }
}
