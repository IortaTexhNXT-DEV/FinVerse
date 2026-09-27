package com.iortatechnxt.brokerverse.common.storage;

/**
 * The bucket classes of an environment (DOCUMENT_STORAGE_DECISION section 3). Each class is one
 * bucket with its own lifecycle and its own customer-managed KMS key.
 */
public enum BucketClass {
  /** Attachments, generated documents and renditions; versioning and Object Lock (governance). */
  DOCUMENTS,
  /** Report runs, scheduled files and batch ZIPs; expiry per report archive setting. */
  REPORTS,
  /** Bulk upload files, bank and insurer files, watchlist feeds; scanned before use. */
  INBOUND,
  /** Migration extracts and staging files; expire after 5 days. */
  MIGRATION
}
