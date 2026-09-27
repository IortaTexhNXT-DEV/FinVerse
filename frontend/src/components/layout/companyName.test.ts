import { shortCompanyName } from './companyName';

describe('company selector', () => {
  it('leaves the legal form out of the short name', () => {
    expect(shortCompanyName('BDO Insurance Brokers, Inc.')).toBe('BDO Insurance Brokers');
    expect(shortCompanyName('Acme Corporation')).toBe('Acme');
    expect(shortCompanyName('BIBS Holdings')).toBe('BIBS Holdings');
  });
});
