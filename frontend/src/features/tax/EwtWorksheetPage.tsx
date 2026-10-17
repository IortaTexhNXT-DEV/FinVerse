import { WorksheetView } from './WorksheetView';

/** Expanded withholding tax worksheet (0619-E monthly, 1601-EQ quarterly) with the QAP export. */
export default function EwtWorksheetPage() {
  return (
    <WorksheetView
      kind="EWT"
      title="Expanded Withholding Tax (1601-EQ / 0619-E)"
      description="Income payments and tax withheld per ATC and payee from supplier invoices and commissions. QAP CSV per quarter."
      granularity="QUARTER"
    />
  );
}
