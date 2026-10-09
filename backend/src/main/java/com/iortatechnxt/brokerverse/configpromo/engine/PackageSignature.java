package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * The signature file of a package.
 *
 * @param algorithm signature algorithm
 * @param keyId identifier of the signing key (not the key)
 * @param value signature of the manifest, hex
 */
public record PackageSignature(String algorithm, String keyId, String value) {}
