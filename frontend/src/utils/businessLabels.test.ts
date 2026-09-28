import { describe, expect, it } from 'vitest';
import { folderLabel, moduleLabel, referenceText, sourceText } from './businessLabels';
import { humanize, statusPhrase } from './format';

describe('business labels', () => {
  it('names modules in words', () => {
    expect(moduleLabel('PRODRECON')).toBe('Production Reconciliation');
    expect(moduleLabel('OPSLEDGER')).toBe('Operations');
    expect(moduleLabel('FRBS')).toBe('Service Fees');
    expect(moduleLabel('CASHIERING')).toBe('Cashiering');
  });

  it('keeps only the document numbers of an internal reference', () => {
    expect(referenceText('PAY:PAY-2026-000011')).toBe('PAY-2026-000011');
    expect(referenceText('RA:ADJUSTMENT:ADJ:ENR-2026-000007:BI-HO-2026-000008')).toBe(
      'ENR-2026-000007 · BI-HO-2026-000008',
    );
    expect(referenceText('SFR-2026-000001:1')).toBe('SFR-2026-000001');
    expect(referenceText('RMB:RMB-INS-MGIC-2026-000003:BI-HO-2026-000008')).toBe(
      'RMB-INS-MGIC-2026-000003 · BI-HO-2026-000008',
    );
    expect(referenceText('APP:5')).toBe('');
    expect(referenceText('DSP:3')).toBe('');
    expect(sourceText('FRBS', 'SFR-2026-000001:1')).toBe('Service Fees · SFR-2026-000001');
    expect(sourceText('CASHIERING', 'DSP:3')).toBe('Cashiering');
  });

  it('names storage folders in words', () => {
    expect(folderLabel('REMITTANCE/WITH_INCENTIVES')).toBe('Remittance › With Incentives');
    expect(folderLabel('FS04/CLX_APPLICATION_TO_INVOICE')).toBe(
      'FS04 › Collections Application to Invoice',
    );
    expect(folderLabel('PRODRECON/INS-MGIC', () => 'Mabuhay General')).toBe(
      'Production Reconciliation › Mabuhay General',
    );
  });

  it('keeps business acronyms in capitals', () => {
    expect(humanize('DV_ASSIGNED')).toBe('DV Assigned');
    expect(humanize('BDOI_ONLY')).toBe('BDOI Only');
    expect(humanize('OTC')).toBe('OTC');
    expect(statusPhrase('DV_ASSIGNED')).toBe('DV assigned');
  });
});
