package com.iortatechnxt.brokerverse.security.service.directory;

/**
 * The answer of an authenticator (FR-UA-003): success, a refused password, an account locked in the
 * directory, or a directory that could not be reached. The message is the text of the directory,
 * shown to the user as it is (EUA message pass-through, UAM-NFR-17).
 *
 * @param outcome outcome
 * @param message message for the user; null on success
 */
public record DirectoryResult(Outcome outcome, String message) {

  /** Outcome of an authentication. */
  public enum Outcome {
    SUCCESS,
    INVALID,
    LOCKED,
    ERROR
  }

  /**
   * The credentials were accepted.
   *
   * @return result
   */
  public static DirectoryResult success() {
    return new DirectoryResult(Outcome.SUCCESS, null);
  }

  /**
   * The password was refused.
   *
   * @param message message of the authenticator
   * @return result
   */
  public static DirectoryResult invalid(String message) {
    return new DirectoryResult(Outcome.INVALID, message);
  }

  /**
   * The account is locked in the directory.
   *
   * @param message message of the directory
   * @return result
   */
  public static DirectoryResult locked(String message) {
    return new DirectoryResult(Outcome.LOCKED, message);
  }

  /**
   * The authenticator could not answer (directory not reachable or not configured).
   *
   * @param message service message
   * @return result
   */
  public static DirectoryResult error(String message) {
    return new DirectoryResult(Outcome.ERROR, message);
  }

  /**
   * Whether the credentials were accepted.
   *
   * @return true on success
   */
  public boolean succeeded() {
    return outcome == Outcome.SUCCESS;
  }
}
