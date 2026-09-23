package com.iortatechnxt.finverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the iNXT FinVerse insurance finance platform. */
@SpringBootApplication
public class FinVerseApplication {

  /**
   * Starts the application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(FinVerseApplication.class, args);
  }
}
