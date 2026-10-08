package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * A dataset of a package as its manifest describes it.
 *
 * @param code dataset code
 * @param name dataset name
 * @param group catalogue group
 * @param module owner module
 * @param rows number of rows
 * @param file path of the data file in the package
 * @param sha256 SHA-256 of the data file
 * @param contentSha256 SHA-256 of the compared values (environment columns left out), compared
 *     with the target after the import
 * @param fingerprint promoted columns and their types
 */
public record ManifestDataset(
    String code,
    String name,
    String group,
    String module,
    int rows,
    String file,
    String sha256,
    String contentSha256,
    String fingerprint) {}
