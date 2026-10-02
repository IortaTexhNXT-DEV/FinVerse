import { humanize } from '@/utils/format';

/**
 * What a catalog rule applies to, in words: every product, or the line or product by its name
 * ("Line Property", "Product Motor Comprehensive"), never the code alone.
 */
export function scopeText(scope: string, code: string, nameOf: (code: string) => string): string {
  if (scope === 'ALL') {
    return 'All products';
  }
  return `${humanize(scope)} ${nameOf(code) || code}`;
}
