/** The mode of payment of a request reads as its list label (Credit to CA / SA), never its code. */
import { describe, expect, it } from 'vitest';
import detail from './RequestDetailPage.tsx?raw';
import tabs from './RequestTabs.tsx?raw';

describe('request texts', () => {
  it('shows the mode of payment by its label', () => {
    for (const source of [detail, tabs]) {
      expect(source).not.toContain('humanize(r.payee.mode)');
      expect(source).toContain("useLovLabel('PRQ_PAYMENT_MODE')");
    }
  });
});
