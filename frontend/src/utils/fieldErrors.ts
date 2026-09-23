/** "accountCode" -> "account code". */
function words(camel: string): string {
  return camel
    .replace(/([a-z\d])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .toLowerCase();
}

/** "lines" -> "line", "entries" -> "entry" (element name of a collection). */
function singular(name: string): string {
  if (name.endsWith('ies')) {
    return `${name.slice(0, -3)}y`;
  }
  return name.endsWith('s') ? name.slice(0, -1) : name;
}

/**
 * Readable label of a bean-validation field path: "lines[0].amount" becomes "Line 1 amount",
 * "roleCodes" becomes "Role codes", "risks[2].sumInsured" becomes "Risk 3 sum insured".
 */
export function humanizeField(path: string): string {
  const label = path
    .split('.')
    .map((segment) => {
      const indexed = /^(\w+)\[(\d+)\]$/.exec(segment);
      if (indexed === null) {
        return words(segment);
      }
      return `${words(singular(indexed[1] ?? ''))} ${Number(indexed[2]) + 1}`;
    })
    .join(' ');
  return label.charAt(0).toUpperCase() + label.slice(1);
}

/** Field errors of a VALIDATION_FAILED response as readable lines, in the server's order. */
export function fieldErrorLines(errors: Record<string, string>): string[] {
  return Object.entries(errors).map(([field, message]) => `${humanizeField(field)}: ${message}`);
}
