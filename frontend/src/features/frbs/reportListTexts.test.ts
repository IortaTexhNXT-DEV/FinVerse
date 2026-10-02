/**
 * The report lists of Finance name each report by its title (and, for a BIR output, what it holds),
 * never by its internal report code or a reference to the requirement it comes from.
 */
import { describe, expect, it } from 'vitest';
import pack from './FrbsHomePage.tsx?raw';
import bir from '../tax/BirOutputsPage.tsx?raw';

describe('report list texts', () => {
  it('does not show the report code or the requirement reference under a report of the pack', () => {
    expect(pack).not.toMatch(/\{e\.scheduleCode \?\? e\.reportCode\}/);
    expect(pack).not.toMatch(/e\.sourceRef \?/);
  });

  it('does not show the report code under a BIR output', () => {
    expect(bir).not.toMatch(/\{o\.code\} ·/);
  });
});
