import { Plus, Trash2 } from 'lucide-react';
import type { InsuredItem, ItemInput, Location, Person, Vehicle } from '@/api/accounts';
import type { RiskItemKind } from '@/api/catalog';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { NumberInput, TextInput } from '@/features/assets/FormControls';
import { emptyItem } from './accountForm';

type Change<T> = (patch: Partial<T>) => void;

function VehicleFields({ v, set }: Readonly<{ v: Vehicle; set: Change<Vehicle> }>) {
  return (
    <>
      <TextInput
        label="Plate no."
        upper
        value={v.plateNo}
        onChange={(plateNo) => set({ plateNo })}
      />
      <TextInput
        label="Conduction sticker"
        upper
        value={v.conductionSticker}
        onChange={(conductionSticker) => set({ conductionSticker })}
      />
      <TextInput
        label="Engine no."
        upper
        value={v.engineNo}
        onChange={(engineNo) => set({ engineNo })}
      />
      <TextInput
        label="Chassis no."
        upper
        value={v.chassisNo}
        onChange={(chassisNo) => set({ chassisNo })}
      />
      <TextInput label="Make" value={v.make} onChange={(make) => set({ make })} />
      <TextInput label="Model" value={v.model} onChange={(model) => set({ model })} />
      <NumberInput
        label="Year model"
        step="1"
        value={v.yearModel}
        onChange={(yearModel) => set({ yearModel })}
      />
      <Field label="Body type">
        {(id) => (
          <LovSelect
            id={id}
            type="VEHICLE_BODY_TYPE"
            value={v.bodyType ?? ''}
            onChange={(bodyType) => set({ bodyType })}
          />
        )}
      </Field>
      <TextInput label="Colour" value={v.colour} onChange={(colour) => set({ colour })} />
      <NumberInput
        label="Seats"
        step="1"
        value={v.seatingCapacity}
        onChange={(seatingCapacity) => set({ seatingCapacity })}
      />
    </>
  );
}

function InsuredItems({
  rows,
  onChange,
}: Readonly<{ rows: InsuredItem[]; onChange: (rows: InsuredItem[]) => void }>) {
  return (
    <div className="stack" style={{ gridColumn: '1 / -1' }}>
      <strong>Insured items at this location</strong>
      {rows.map((row, index) => (
        <div className="row" key={`insured-${index + 1}`}>
          <TextInput
            label={`Item ${index + 1}`}
            value={row.description}
            onChange={(description) =>
              onChange(rows.map((r, i) => (i === index ? { ...r, description } : r)))
            }
          />
          <NumberInput
            label="Sum insured"
            value={row.sumInsured}
            onChange={(sumInsured) =>
              onChange(
                rows.map((r, i) => (i === index ? { ...r, sumInsured: sumInsured ?? 0 } : r)),
              )
            }
          />
          <Button
            size="sm"
            variant="ghost"
            icon={<Trash2 size={14} />}
            aria-label={`Remove insured item ${index + 1}`}
            onClick={() => onChange(rows.filter((_, i) => i !== index))}
          />
        </div>
      ))}
      <div>
        <Button
          size="sm"
          variant="secondary"
          icon={<Plus size={14} />}
          onClick={() => onChange([...rows, { description: '', sumInsured: 0 }])}
        >
          Add insured item
        </Button>
      </div>
    </div>
  );
}

function LocationFields({ l, set }: Readonly<{ l: Location; set: Change<Location> }>) {
  return (
    <>
      <TextInput
        label="Address"
        required
        value={l.address}
        onChange={(address) => set({ address })}
      />
      <TextInput label="City" value={l.city} onChange={(city) => set({ city })} />
      <TextInput label="Province" value={l.province} onChange={(province) => set({ province })} />
      <Field label="Occupancy">
        {(id) => (
          <LovSelect
            id={id}
            type="OCCUPANCY"
            value={l.occupancy ?? ''}
            onChange={(occupancy) => set({ occupancy })}
          />
        )}
      </Field>
      <Field label="Construction class">
        {(id) => (
          <LovSelect
            id={id}
            type="CONSTRUCTION_CLASS"
            value={l.constructionClass ?? ''}
            onChange={(constructionClass) => set({ constructionClass })}
          />
        )}
      </Field>
      <InsuredItems
        rows={l.insuredItems ?? []}
        onChange={(insuredItems) => set({ insuredItems })}
      />
    </>
  );
}

function PersonFields({ p, set }: Readonly<{ p: Person; set: Change<Person> }>) {
  return (
    <>
      <TextInput label="Name" required value={p.name} onChange={(name) => set({ name })} />
      <TextInput
        label="Birth date"
        type="date"
        value={p.birthDate}
        onChange={(birthDate) => set({ birthDate })}
      />
      <TextInput
        label="Relationship"
        value={p.relationship}
        onChange={(relationship) => set({ relationship })}
      />
    </>
  );
}

function KindFields({ item, set }: Readonly<{ item: ItemInput; set: Change<ItemInput> }>) {
  if (item.vehicle) {
    const v = item.vehicle;
    return <VehicleFields v={v} set={(p) => set({ vehicle: { ...v, ...p } })} />;
  }
  if (item.location) {
    const l = item.location;
    return <LocationFields l={l} set={(p) => set({ location: { ...l, ...p } })} />;
  }
  if (item.person) {
    const p = item.person;
    return <PersonFields p={p} set={(patch) => set({ person: { ...p, ...patch } })} />;
  }
  return null;
}

interface Props {
  kind: RiskItemKind;
  motor: boolean;
  fleet: boolean;
  items: ItemInput[];
  onChange: (items: ItemInput[]) => void;
}

const NOUN: Record<RiskItemKind, string> = {
  VEHICLE: 'vehicle',
  PROPERTY_LOCATION: 'location',
  PERSON: 'insured person',
  GENERIC: 'item',
};

/** Step 4: the risk items of the product line's kind (vehicles, locations, persons, items). */
export function RiskItemsStep({ kind, motor, fleet, items, onChange }: Readonly<Props>) {
  const setItem = (index: number, patch: Partial<ItemInput>) =>
    onChange(items.map((it, i) => (i === index ? { ...it, ...patch } : it)));
  const canAdd = fleet || kind !== 'VEHICLE' || items.length === 0;
  return (
    <div className="stack">
      {items.map((item, index) => (
        <fieldset key={`item-${index + 1}`} className="card" style={{ padding: 'var(--space-4)' }}>
          <legend>
            {NOUN[kind]} {index + 1}
          </legend>
          <div className="form-grid">
            <KindFields item={item} set={(patch) => setItem(index, patch)} />
            <TextInput
              label="Description"
              value={item.description}
              onChange={(description) => setItem(index, { description })}
            />
            <NumberInput
              label="Sum insured"
              value={item.sumInsured}
              onChange={(sumInsured) => setItem(index, { sumInsured })}
            />
            {motor ? (
              <>
                <NumberInput
                  label="Excess BI limit"
                  value={item.biLimit}
                  onChange={(biLimit) => setItem(index, { biLimit })}
                />
                <NumberInput
                  label="PD limit"
                  value={item.pdLimit}
                  onChange={(pdLimit) => setItem(index, { pdLimit })}
                />
              </>
            ) : (
              <NumberInput
                label="Rate %"
                step="0.0001"
                hint="Blank = product default"
                value={item.rate}
                onChange={(rate) => setItem(index, { rate })}
              />
            )}
          </div>
          <Button
            size="sm"
            variant="ghost"
            icon={<Trash2 size={14} />}
            onClick={() => onChange(items.filter((_, i) => i !== index))}
          >
            Remove {NOUN[kind]}
          </Button>
        </fieldset>
      ))}
      {canAdd && (
        <div>
          <Button
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => onChange([...items, emptyItem(kind)])}
          >
            Add {NOUN[kind]}
          </Button>
        </div>
      )}
    </div>
  );
}
