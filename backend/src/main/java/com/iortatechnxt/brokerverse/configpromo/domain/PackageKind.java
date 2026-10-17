package com.iortatechnxt.brokerverse.configpromo.domain;

/** Origin of a configuration package kept by this environment. */
public enum PackageKind {
  /** Exported from this environment. */
  EXPORT,
  /** Uploaded from another environment for an import. */
  UPLOAD,
  /** The configuration of this environment taken just before an import, for a rollback. */
  SNAPSHOT
}
