package com.iortatechnxt.brokerverse.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Stored duplicate-detection keys of a client (BRNB.032), recomputed from the client data on every
 * change so duplicate queries use plain indexed equality.
 */
@Embeddable
public class ClientKeys {

  @Column(name = "id_key", length = 110)
  private String idKey;

  @Column(name = "mobile_key", length = 20)
  private String mobileKey;

  @Column(name = "name_key", length = 260)
  private String nameKey;

  @Column(name = "corporate_key", length = 260)
  private String corporateKey;

  protected ClientKeys() {}

  /**
   * Keys of client data.
   *
   * @param identity ID type and number
   * @param mobile mobile number
   * @param name person or corporate name
   * @return keys
   */
  static ClientKeys of(
      ClientDetails.Identity identity, String mobile, ClientDetails.PersonName name) {
    ClientKeys keys = new ClientKeys();
    keys.idKey = DuplicateKeys.idKey(identity.idType(), identity.idNumber());
    keys.mobileKey = DuplicateKeys.mobileKey(mobile);
    keys.nameKey = DuplicateKeys.nameKey(name.lastName(), name.firstName());
    keys.corporateKey = DuplicateKeys.corporateKey(name.corporateName());
    return keys;
  }

  public String getIdKey() {
    return idKey;
  }

  public String getMobileKey() {
    return mobileKey;
  }

  public String getNameKey() {
    return nameKey;
  }

  public String getCorporateKey() {
    return corporateKey;
  }
}
