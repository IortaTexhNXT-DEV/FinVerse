/** A quotation request as captured in the inbox dialog (BRNB.041). */
export interface RequestForm {
  channel: string;
  externalRef: string;
  clientCode: string;
  prospectName: string;
  prospectEmail: string;
  prospectMobile: string;
  productCode: string;
  marketSegment: string;
  requestedCover: string;
}

/** A plausible e-mail address: text, one @, and a dot in the domain part. */
export function isEmail(value: string): boolean {
  const at = value.indexOf('@');
  const dot = value.lastIndexOf('.');
  return (
    at > 0 &&
    at === value.lastIndexOf('@') &&
    dot > at + 1 &&
    dot < value.length - 1 &&
    !value.includes(' ')
  );
}

/** Field errors of a request: a client or a prospect name, a valid e-mail and the cover. */
export function requestErrors(f: RequestForm): Record<string, string> {
  const errors: Record<string, string> = {};
  if (f.clientCode.trim() === '' && f.prospectName.trim() === '') {
    errors.client = 'Choose the client or give the prospect name';
  }
  if (f.prospectEmail.trim() !== '' && !isEmail(f.prospectEmail.trim())) {
    errors.prospectEmail = 'Enter a valid e-mail address';
  }
  if (f.requestedCover.trim() === '') {
    errors.requestedCover = 'Describe the requested cover';
  }
  return errors;
}

/** The URL that opens the quotation wizard for a request. */
export function quotationLinkOf(r: {
  id: number;
  clientId?: number;
  productCode?: string;
  marketSegment?: string;
  channel: string;
}): string {
  const params = new URLSearchParams({ request: String(r.id), channel: r.channel });
  if (r.clientId !== undefined) {
    params.set('client', String(r.clientId));
  }
  if (r.productCode) {
    params.set('product', r.productCode);
  }
  if (r.marketSegment) {
    params.set('segment', r.marketSegment);
  }
  return `/quotations/new?${params.toString()}`;
}
