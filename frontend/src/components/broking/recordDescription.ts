/**
 * The one-line description under the title of a quotation or proposal request: the product by its
 * name (never its code alone), what the record is and the client ("Fire and Lightning Package
 * quotation for Bautista, Carmela Reyes").
 */
export function recordDescription(productName: string, record: string, clientName: string): string {
  const product = productName.trim();
  return product === '' ? `${record} for ${clientName}` : `${product} ${record} for ${clientName}`;
}
