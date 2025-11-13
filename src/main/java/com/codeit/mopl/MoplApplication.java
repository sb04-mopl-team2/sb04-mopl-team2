package com.codeit.mopl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MoplApplication {

  public static void main(String[] args) {
    SpringApplication.run(MoplApplication.class, args);
  }

}
