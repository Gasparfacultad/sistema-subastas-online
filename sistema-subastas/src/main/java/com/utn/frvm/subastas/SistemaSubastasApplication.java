package com.utn.frvm.subastas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SistemaSubastasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaSubastasApplication.class, args);
	}

}

