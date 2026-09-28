import { useMutation } from '@tanstack/react-query';
import { Copy, Download } from 'lucide-react';
import { useState } from 'react';
import { budgetApi } from '@/api/budget';
import { saveFile } from '@/api/client';
import type { Budget } from '@/api/budget';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';

interface Props {
  budget: Budget;
  onUpdated: (budget: Budget, message: string) => void;
}

/** Bulk line tools of a draft budget: CSV import and copy from prior-year actuals. */
export function BudgetTools({ budget, onUpdated }: Readonly<Props>) {
  const [sourceYear, setSourceYear] = useState(budget.fiscalYear - 1);
  const [percent, setPercent] = useState(0);
  const importCsv = useMutation({
    mutationFn: (text: string) => budgetApi.importCsv(budget.id, text),
    onSuccess: (b) => onUpdated(b, `${b.lineCount} lines imported`),
  });
  const importFile = useMutation({
    mutationFn: (file: File) => budgetApi.importFile(budget.id, file),
    onSuccess: (b) => onUpdated(b, `${b.lineCount} lines imported`),
  });
  const template = useMutation({
    mutationFn: budgetApi.template,
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
  const copyActuals = useMutation({
    mutationFn: () => budgetApi.copyActuals(budget.id, sourceYear, percent),
    onSuccess: (b) => onUpdated(b, `Actuals of ${sourceYear} copied`),
  });
  const onFile = (files: File[]) => {
    const file = files[0];
    if (file?.name.toLowerCase().endsWith('.xlsx')) {
      importFile.mutate(file);
    } else if (file) {
      void file.text().then((text) => importCsv.mutate(text));
    }
  };

  return (
    <div className="grid-2">
      <Card
        title="Import Lines"
        actions={
          <Button
            variant="ghost"
            icon={<Download size={16} />}
            busy={template.isPending}
            onClick={() => template.mutate()}
          >
            Download Template
          </Button>
        }
      >
        <ErrorAlert error={importCsv.error ?? importFile.error ?? template.error} />
        <p className="muted">
          The Excel template (monthly or annual amounts, the annual spread evenly), or a CSV file
          with the header <code>account_code,cost_centre,m01…m12</code> or{' '}
          <code>account_code,cost_centre,annual</code>. Replaces all lines.
        </p>
        <Field label="Excel or CSV file">
          {(id) => (
            <FileDropZone id={id} accept=".xlsx,.csv,text/csv" onChange={onFile} />
          )}
        </Field>
      </Card>
      <Card title="Copy from prior-year actuals">
        <ErrorAlert error={copyActuals.error} />
        <div className="form-grid">
          <Field label="Source fiscal year" required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="number"
                value={sourceYear}
                onChange={(e) => setSourceYear(Number(e.target.value))}
              />
            )}
          </Field>
          <Field label="Adjustment %" hint="e.g. 5 for +5 %">
            {(id) => (
              <input
                id={id}
                className="input num"
                type="number"
                value={percent}
                onChange={(e) => setPercent(Number(e.target.value))}
              />
            )}
          </Field>
          <Button
            variant="secondary"
            icon={<Copy size={16} />}
            busy={copyActuals.isPending}
            onClick={() => copyActuals.mutate()}
            style={{ alignSelf: 'end' }}
          >
            Copy Actuals
          </Button>
        </div>
        <p className="muted">
          Replaces all lines with the income and expense actuals of the source year by account, cost
          centre and month.
        </p>
      </Card>
    </div>
  );
}
