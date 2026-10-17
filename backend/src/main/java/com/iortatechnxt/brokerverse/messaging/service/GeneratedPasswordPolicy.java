package com.iortatechnxt.brokerverse.messaging.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Random 12-character password without look-alike characters (0/O, 1/l/I). */
@Component
public class GeneratedPasswordPolicy implements DocumentPasswordPolicy {

  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
  private static final int LENGTH = 12;
  private final SecureRandom random = new SecureRandom();

  @Override
  public String newPassword() {
    StringBuilder sb = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) {
      sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return sb.toString();
  }
}
