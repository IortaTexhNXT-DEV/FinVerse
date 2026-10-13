import { useQuery } from '@tanstack/react-query';
import { systemApi } from '@/api/system';
import { Modal } from '@/components/ui/Modal';
import { BRAND } from '@/branding';
import { formatDateTime } from '@/utils/format';

/** About BIBS: product, release, last update, platform, licensee and vendor. */
export function AboutDialog({ open, onClose }: Readonly<{ open: boolean; onClose: () => void }>) {
  const about = useQuery({ queryKey: ['about'], queryFn: systemApi.about, enabled: open });
  const a = about.data;
  return (
    <Modal title={`About ${BRAND.product}`} open={open} onClose={onClose}>
      <div className="about-body">
        <div className="about-head">
          <img className="about-client-logo" src={BRAND.clientLogo} alt={BRAND.client} />
          <h2>
            {BRAND.product} – {BRAND.productName}
          </h2>
          <p className="muted">Insurance broking and finance for {BRAND.client}</p>
        </div>
        <dl className="about-facts">
          <dt>Version</dt>
          <dd>{BRAND.release}</dd>
          <dt>Last update</dt>
          <dd>{a?.buildTime === undefined ? '…' : formatDateTime(a.buildTime)}</dd>
          <dt>Platform</dt>
          <dd>{BRAND.platform}</dd>
          <dt>Licensed to</dt>
          <dd>{BRAND.legalName}</dd>
        </dl>
        <div className="about-foot">
          <div className="powered-by">
            <span>Powered by</span>
            <img src={BRAND.vendorLogo} alt={BRAND.vendor} />
          </div>
          <p className="muted">
            © {new Date().getFullYear()} {BRAND.vendor}. All rights reserved.
          </p>
        </div>
      </div>
    </Modal>
  );
}
