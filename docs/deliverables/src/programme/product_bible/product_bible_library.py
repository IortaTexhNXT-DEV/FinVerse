"""Library content of the BDOI Product Bible: product catalogue, covers and perils, clauses, rating and charges,
underwriting data dictionary, documents and KYC, glossary, migration checks, sources and open questions.
Rows that do not come from a BDOI document carry the market-practice label (to confirm with BDOI)."""

from __future__ import annotations

from product_bible_data import MKT

FRS = "BDOI FRS Product Maintenance v1.0"
NB = "BDOI NB BRD Appendix A/B"

# ---------------------------------------------------------------------------------------------------------------
# Per-LOB defaults: taxes, renewal considerations, typical claims documents, required placement documents.
TAX_FIRE = "DST 12.5% of net premium; VAT 12% of net premium (NB BRD labels it PTX for Fire); FST 2% of net premium; LGT = net premium x LGT rate of the insurer branch"
TAX_STD = "DST 12.5% of premium; VAT 12% of premium (or premium tax / exempt per insurer - confirm); LGT = premium x LGT rate of the insurer branch; no FST"
TAX_MOTOR = "DST 12.5% of net premium; VAT 12% of basic premium; LGT = basic premium x LG rate; CTP02 is non-VAT; no FST"
TAX_BOND = "DST on bonds (12.5% of premium - confirm legal basis and rate); VAT 12%; LGT per insurer branch; no FST"
TAX_HEALTH = "HMO: VAT 12% on membership fees, DST / LGT treatment to confirm with insurer / HMO; health insurance written by a non-life insurer: as standard lines"
TAX_MARINE = "DST 12.5%; VAT 12% (export cargo may be zero-rated - see non-VAT product MON05); LGT per insurer branch; no FST"
LOB_DEFAULTS = {
    "Property": dict(tax=TAX_FIRE, renew="Re-valuation of sums insured; mortgagee endorsement carried forward; multi-year allowed up to 5 years for bank packages (BIBS catalogue)", claims="Notice of loss, fire bureau certificate (BFP), photos, sworn statement of loss, inventory, repair estimates, police report (RSMD)", src_tax=NB),
    "Bond": dict(tax=TAX_BOND, renew="Bonds are issued for the obligation period; extension by endorsement; BDOI FRS: NB and endorsement only, no renewal", claims="Obligee's demand letter, proof of principal's default, contract and bond copy, statement of account", src_tax=MKT),
    "Personal Accident": dict(tax=TAX_STD, renew="Updated member list and ages; age limits", claims="Claim form, medical certificate, hospital bills, police / accident report, death certificate, proof of relationship", src_tax=MKT),
    "Motor": dict(tax=TAX_MOTOR, renew="Depreciated sum insured (BIBS: 90% of TSI annual, 81% multi-year OD/theft basis per NB BRD); free first year (FFY) packages; LTO registration month for CTPL", claims="Claim form, OR/CR, driver's licence, police report, photos, repair estimate, LTO / TPL claimant documents", src_tax=NB),
    "Health Insurance": dict(tax=TAX_HEALTH, renew="Census update, claims experience / loss ratio, plan re-pricing", claims="HMO letter of authorisation or reimbursement form, official receipts, medical abstract", src_tax=MKT),
    "Liability": dict(tax=TAX_STD, renew="Updated turnover / payroll, claims made retroactive date continuity", claims="Third-party demand, incident report, legal documents, settlement agreement", src_tax=MKT),
    "Engineering": dict(tax=TAX_STD + " (FST on fire peril of engineering - confirm)", renew="Usually project period (no renewal); extension / maintenance period by endorsement (BDOI FRS: NB and endorsement only)", claims="Notice of loss, incident report, photos, contractor's progress report, repair cost, police / BFP report", src_tax=MKT),
    "Electronic Equipment": dict(tax=TAX_STD, renew="Updated equipment list and values", claims="Claim form, technician report, repair quotation, purchase invoice, police report (theft)", src_tax=MKT),
    "Others / Miscellaneous": dict(tax=TAX_STD, renew="Per product; event covers do not renew", claims="Per product (e.g. hole-in-one: certified scorecard and witness statements)", src_tax=MKT),
    "Marine (Cargo)": dict(tax=TAX_MARINE, renew="Open cover declarations and annual premium adjustment", claims="Bill of lading / airway bill, commercial invoice, packing list, survey report, notice of claim to carrier, letter of subrogation", src_tax=MKT),
    "Crime": dict(tax=TAX_STD, renew="Updated cash-handling and employee counts", claims="Police report, audit report, employee statements, proof of loss", src_tax=MKT),
    "Marine Hull": dict(tax=TAX_STD, renew="Class / statutory certificates, condition survey, fleet changes", claims="Master's protest, survey report, repair accounts, class certificate", src_tax=MKT),
    "Property / Equipment Floater": dict(tax=TAX_STD + " (FST if fire peril included - confirm)", renew="Updated equipment schedule; lease contract changes", claims="Notice of loss, police / BFP report, repair estimate, equipment invoice", src_tax=MKT),
    "Aviation": dict(tax=TAX_STD, renew="Updated pilot hours, aircraft airworthiness certificate", claims="CAAP incident report, pilot statement, repair estimate, third-party demands", src_tax=MKT),
    "Casualty": dict(tax=TAX_STD, renew="Updated values and security measures", claims="Police report, inventory of items lost, proof of ownership", src_tax=MKT),
}

# Product catalogue. code: proposed product code (to confirm), with the BIBS code where one exists.
# (lob, code, name, description, insured interest, standard covers, extensions, key exclusions, SI basis, rating,
#  deductible, term, UW info, documents, source)
P = []


def prod(lob, code, name, desc, interest, covers, ext, excl, si, rating, ded, term, uw, docs, src=MKT):
    P.append(dict(lob=lob, code=code, name=name, desc=desc, interest=interest, covers=covers, ext=ext, excl=excl,
                  si=si, rating=rating, ded=ded, term=term, uw=uw, docs=docs, src=src))


PROPOSAL = "Proposal / application form; client KYC (ID, TIN, SEC / DTI registration for entities)"
# Property
prod("Property", "PRO-FL", "Fire and Lightning (BIBS FIRE_LIGHTNING)", "Standard fire policy on buildings, contents and stocks", "Owner, mortgagee (bank) as loss payee", "Fire, lightning", "Allied perils, RSMD, earthquake, typhoon and flood, extended coverage", "War, nuclear, consequential loss unless endorsed, wear and tear", "Replacement or actual cash value per item", "Rate x TSI; minimum premium per package", "Typically none for fire; per package", "Annual or multi-year (bank packages)", "Location, occupancy, construction class, items and SI", PROPOSAL + "; property photos; title / loan details for mortgaged risks", "BDOI FRS (Type of Cover: F/L Only); BIBS PAR packages")
prod("Property", "PRO-NP", "Fire and Allied Perils / Broad Named Perils (BIBS BROAD_NAMED_PERILS)", "Fire policy extended to named allied perils", "Owner, mortgagee", "Fire, lightning, earthquake fire and shock, typhoon, flood, extended coverage, RSMD", "Debris removal, professional fees, temporary removal", "Unnamed perils, basement flood (clause), war", "Replacement or actual cash value", "Rate x TSI per peril; minimum premium", "EQ 2% of value at risk; typhoon / flood 1% of loss min PHP 20,000 (BIBS PAR08 v2)", "Annual or multi-year", "As Fire plus flood zone and elevation", PROPOSAL + "; inspection for large risks", "BDOI FRS (Named Perils)")
prod("Property", "PRO-AR", "Property All Risks (BIBS ALL_RISK)", "All risks of physical loss or damage unless excluded", "Owner, mortgagee", "All risks of physical loss or damage", "BI, machinery breakdown, money", "Wear and tear, gradual deterioration, mysterious disappearance, war, cyber", "Replacement value", "Rate x TSI; minimum premium", "Per occurrence deductible", "Annual", "Statement of values, risk survey", PROPOSAL + "; risk survey report; statement of values", "BDOI FRS (All-Risk)")
prod("Property", "PRO-IAR", "Industrial All Risks", "All risks on industrial / commercial plants", "Corporate owner, lenders", "Material damage all risks", "Business interruption, machinery breakdown, EEI sections", "As all risks plus process failure", "Replacement value; BI on gross profit", "Rate x TSI by section; minimum premium", "Per section deductibles", "Annual", "Plant survey, loss history, BI worksheet", PROPOSAL + "; risk engineering survey; BI worksheet")
prod("Property", "PRO-BI", "Business Interruption / Consequential Loss", "Loss of gross profit following insured damage", "Business owner", "Loss of gross profit, increased cost of working", "Suppliers / customers extension, public utilities", "Losses without insured material damage", "Annual gross profit x indemnity period", "Rate x gross profit SI", "Time excess (e.g. 3-7 days)", "Annual", "Financial statements, indemnity period", PROPOSAL + "; audited financial statements")
prod("Property", "PRO-HH", "Householders / Homeowners", "Package for dwelling and contents", "Homeowner", "Fire and allied perils, burglary, personal liability", "Earthquake, typhoon and flood", "Unoccupancy beyond limit, valuables over limit", "Replacement value", "Package premium or rate x SI", "Fixed deductible", "Annual", "Address, construction, contents values", PROPOSAL)
# Bond
for code, name, desc, src in [
    ("BND-SUR", "Surety Bond (general)", "Guarantee of an obligation of the principal to the obligee", "BDOI FRS (Surety Bond)"),
    ("BND-BID", "Bidder's Bond", "Guarantees the bidder enters into the contract if awarded", "BDOI FRS (Bidder's Bond)"),
    ("BND-PER", "Performance Bond", "Guarantees full performance of a contract", "BDOI FRS (Performance Bond)"),
    ("BND-WAR", "Warranty Bond", "Guarantees correction of faulty work during the warranty period", "BDOI FRS (Warranty Bond)"),
    ("BND-JUD", "Judicial / Court Bond", "Bonds required in court proceedings (replevin, attachment, counter-bond)", "BDOI FRS (Judicial Bond)"),
    ("BND-HEI", "Heirs Bond", "Guarantees heirs' obligations in extrajudicial settlement of estate", "BDOI FRS (Heirs Bond)"),
    ("BND-PAY", "Payment / Advance Payment Bond", "Guarantees repayment of advance or payment to sub-contractors", MKT),
    ("BND-JCL", "Government (JCL) contractor bonds", "Bonds to government agencies in IC-accredited forms (JCL / G(13) / G(16) - confirm forms)", MKT),
    ("BND-CUS", "Customs Bond", "Guarantees duties and taxes to the Bureau of Customs", MKT),
]:
    prod("Bond", code, name, desc, "Obligee (beneficiary); principal (client)", "Bond amount payable on principal's default", "Extension of term, increase of amount (endorsement)", "Obligations outside the bond wording; after expiry", "Bond amount (often % of contract price)", "Rate x bond amount; minimum premium", "n/a", "Obligation period (not annual)", "Principal's financial statements, contract / tender documents, indemnity agreement, collateral", PROPOSAL + "; indemnity agreement; contract or court order", src)
# Personal Accident
prod("Personal Accident", "PAC-IND", "Individual PA (BIBS INDIVIDUAL_PA, PA001)", "Accidental death, disablement and medical reimbursement", "Insured person; beneficiaries", "Accidental death, permanent disablement, medical reimbursement", "Murder and assault, motorcycling, burial benefit", "Suicide, war, pre-existing conditions, hazardous sports unless endorsed", "Fixed benefit amount", "Premium per person by occupation class / plan", "n/a", "Annual", "Name, birth date, occupation, beneficiary", "Application, valid ID", "BDOI FRS (Personal Accident); BIBS PA001")
prod("Personal Accident", "PAC-GRP", "Group PA (BIBS GROUP_PA; EB GPA)", "PA cover for employees or members", "Group members", "As individual PA", "24-hour worldwide, medical", "As individual PA", "Fixed benefit or salary multiple", "Premium per head; minimum group size", "n/a", "Annual", "Member census, occupation classes", "Application, census", "BIBS catalogue")
prod("Personal Accident", "PAC-STU", "Student PA", "Group PA for school enrollees", "Students", "Accidental death, disablement, medical", "Food poisoning, school trips", "As PA", "Fixed benefit", "Premium per student", "n/a", "School year", "School enrolment count", "School application")
prod("Personal Accident", "PAC-TRV", "Travel PA / Travel insurance (BIBS TRAVEL_PA)", "PA and travel inconvenience for trips", "Traveller", "Accidental death, medical expenses abroad, evacuation", "Trip cancellation, baggage, Schengen compliance", "Pre-existing conditions, travel against medical advice", "Plan limits", "Premium per trip by destination / days", "Small excess on baggage", "Trip period", "Destination, travel dates, ages", "Application, passport", "BDOI FRS (Travel)")
prod("Personal Accident", "PAC-MPA", "Marine PA (BIBS MARINE_PA)", "PA for passengers / crew of vessels", "Passengers / crew", "Accidental death and disablement at sea", "Medical expenses", "War, nuclear", "Per seat benefit", "Premium per seat or per voyage", "n/a", "Annual", "Vessel, seats, route", "Application, vessel documents", "BIBS catalogue")
# Motor
prod("Motor", "MTR-CTPL", "CTPL (BIBS CTP01 / CTP02)", "Compulsory third party liability for death / injury to third parties", "Vehicle owner", "Bodily injury / death of third parties up to the IC limit", "None (statutory form)", "Property damage, own damage, passengers / employees as defined", "Statutory limit", "IC-prescribed premium tariff per vehicle class (confirm current IC circular)", "n/a", "Annual, aligned with LTO registration (multi-year for new cars - confirm)", "Plate / conduction sticker, MV file no., engine and chassis no., vehicle type", "OR/CR or sales invoice, owner ID", "BDOI NB BRD; BIBS CTP01 / CTP02")
prod("Motor", "MTR-COMP", "Comprehensive (BIBS COMPREHENSIVE, MTR packages)", "Own damage and theft plus excess BI / PD, optional AON and auto PA", "Owner; bank as mortgagee / loss payee for auto loans", "OD / theft, excess BI, PD", "Acts of nature, auto PA, RSCC, depreciation waiver, accredited repair", "Unauthorised driver, DUI, wear and tear, mechanical breakdown", "Market value; OD/theft SI 90% of TSI annual, 81% multi-year (NB BRD)", "OD/PV rate x OD/theft SI + BI / PD premiums; minimum premium per package", "Participation fee per OD claim (e.g. PHP 2,000 in MTR12 v2)", "Annual or multi-year (bank packages, up to 3 years)", "Make, model, year, engine / chassis no., plate, use, TSI", "OR/CR, sales invoice for new cars, photos / inspection for used cars, IDF", "BDOI NB BRD Appendix A/B")
prod("Motor", "MTR-OD", "Own Damage / Theft only", "OD and theft without third-party cover", "Owner / mortgagee", "Own damage, theft", "AON", "As comprehensive", "Market value", "Rate x SI", "Participation fee", "Annual", "As comprehensive", "As comprehensive")
prod("Motor", "MTR-AON", "Acts of Nature (extension)", "Flood, typhoon, earthquake damage to the vehicle", "Owner", "AON perils", "n/a", "Wear and tear", "As OD SI", "Rate x SI (BIBS: deductible 1% of SI)", "1% of SI (MTR12 v2)", "With base policy", "As comprehensive", "As comprehensive", "BIBS catalogue (coverage AON)")
prod("Motor", "MTR-VTPL", "Excess BI / Property Damage (VTPL)", "Voluntary third-party liability above CTPL", "Owner", "Excess bodily injury, third-party property damage", "Higher limits", "Contractual liability", "Limit of liability", "Fixed premium per limit band", "n/a", "Annual", "Vehicle details, limits", "OR/CR", "BDOI NB BRD (BI / PD cover)")
prod("Motor", "MTR-APA", "Auto Personal Accident", "PA for driver and passengers", "Occupants", "Accidental death / disablement, medical", "n/a", "As PA", "Per seat benefit", "Premium per seat", "n/a", "With base policy", "Seating capacity", "As comprehensive", "BIBS catalogue (coverage AUTO_PA)")
# Health
prod("Health Insurance", "HLT-GRP", "Group HMO / Health (BIBS EBHMO01)", "Corporate health programme with an HMO or insurer", "Employees and dependants", "In-patient, out-patient, emergency, preventive care", "Dental, maternity, pre-existing waiver, executive check-up", "Cosmetic, experimental, self-inflicted, as per plan", "Maximum benefit limit per illness / year", "Premium per member per plan / age band", "n/a (co-pay where applicable)", "Annual", "Census, plan, current provider, claims experience", "Enrolment forms, census, signed proposal", "BIBS catalogue (Employee Benefits)")
prod("Health Insurance", "HLT-IND", "Individual / Family Health", "Individual HMO or health insurance", "Individual and family", "As group plan", "Riders", "Pre-existing conditions (waiting period)", "Maximum benefit limit", "Premium by age band", "n/a", "Annual", "Health declaration, ages", "Application, health declaration")
# Liability
for code, name, cov, src in [
    ("LIA-CGL", "Comprehensive General Liability (BIBS CGL)", "Third-party bodily injury and property damage from premises and operations", "BDOI FRS; BIBS CGL01-CGL09"),
    ("LIA-CPL", "Comprehensive Personal Liability (BIBS CPL)", "Personal liability of individuals / households", "BIBS catalogue"),
    ("LIA-EMP", "Employer's Liability (BIBS EMPLOYERS)", "Liability for employee injury beyond statutory benefits", FRS),
    ("LIA-PRD", "Products Liability (BIBS PRODUCTS)", "Liability for injury / damage caused by products sold", FRS),
    ("LIA-PI", "Professional Liability / Indemnity (BIBS PROFESSIONAL)", "Negligent professional advice or service", FRS),
    ("LIA-MM", "Medical Malpractice", "Liability of doctors and hospitals for medical negligence", FRS),
    ("LIA-DO", "Directors and Officers (BIBS DNO)", "Personal liability of directors and officers for wrongful acts", FRS),
    ("LIA-EO", "Errors and Omissions (BIBS ENO)", "Financial loss from errors in professional services", FRS),
    ("LIA-CYB", "Cyber Liability (BIBS CYBER)", "Data breach, network security and privacy liability and first-party costs", FRS),
    ("LIA-POL", "Pollution Legal / Contractor's Pollution (BIBS POLLUTION_LEGAL, CONTRACTORS_POLLUTION)", "Pollution clean-up and third-party liability", FRS),
    ("LIA-CPR", "Contaminated Products (BIBS CONTAMINATED_PRODUCTS)", "Recall costs and losses from product contamination", "BIBS catalogue"),
    ("LIA-MAR", "Marine Liability", "Liabilities of ship / port / logistics operators", FRS),
    ("LIA-XAL", "Excess Auto Liability", "Liability above underlying motor limits", FRS),
]:
    prod("Liability", code, name, cov, "Insured; third-party claimants", cov, "Cross liability, contractual liability, worldwide jurisdiction (per product)", "Intentional acts, contractual liability unless endorsed, pollution (CGL), fines and penalties", "Limit of liability per occurrence / aggregate", "Rate x turnover / payroll or flat premium per limit", "Per claim deductible", "Annual (claims-made for D&O, PI, E&O, cyber)", "Nature of business, turnover, limits, claims history, location of premises", PROPOSAL + "; financial statements for D&O / PI", src)
# Engineering
prod("Engineering", "ENG-CAR", "Contractor's All Risks (BIBS CAR, CAR00-CAR29, CARI)", "Civil works under construction", "Principal, contractors, sub-contractors, lender", "Material damage all risks, third-party liability (section II)", "Maintenance period, cross liability, strikes riot, extra charges", "Faulty design, wear and tear, penalties, consequential loss", "Total contract value", "Rate x contract value; minimum premium (BIBS PHP 2,000 default)", "Per occurrence; higher for AON", "Project period plus maintenance", "Contractor's name, project description, location, contract value, period", "Proposal, contract / bill of quantities, project schedule", "BDOI FRS (CARI)")
prod("Engineering", "ENG-EAR", "Erection All Risks (BIBS EAR)", "Erection and installation of plant and machinery", "Principal, erector, lender", "Material damage all risks, testing and commissioning, TPL", "Maintenance, cross liability", "Defective design, testing beyond period", "Value of machinery plus erection cost", "Rate x value; minimum premium", "Per occurrence; testing deductible", "Erection period plus testing / maintenance", "As CAR plus machinery specifications", "Proposal, erection contract", "BDOI FRS (EAR)")
prod("Engineering", "ENG-MBD", "Machinery Breakdown", "Sudden breakdown of machinery in operation", "Owner / operator", "Mechanical and electrical breakdown", "Expediting expenses, loss of profit (MLOP)", "Wear and tear, gradual deterioration", "New replacement value", "Rate x SI", "Per machine", "Annual", "Machinery list, age, maintenance contracts", PROPOSAL + "; machinery schedule")
prod("Engineering", "ENG-CPM", "Contractor's Plant and Machinery", "Mobile construction equipment", "Owner / lessor", "All risks on site and in transit", "TPL", "Breakdown unless external cause", "Replacement value", "Rate x SI", "Per item", "Annual", "Equipment list, serial numbers", PROPOSAL + "; equipment list")
prod("Engineering", "ENG-BPV", "Boiler and Pressure Vessel", "Explosion / collapse of boilers and pressure vessels", "Owner", "Explosion, collapse, TPL", "Surrounding property", "Wear and tear", "Replacement value", "Rate x SI", "Per item", "Annual", "Inspection certificates", PROPOSAL + "; inspection certificate")
prod("Engineering", "ENG-DSU", "Delay in Start-Up / ALOP", "Loss of anticipated profit from project delay after insured damage", "Principal / lender", "Loss of gross profit / debt service", "n/a", "Delays without insured damage", "Anticipated gross profit", "Rate x SI", "Time excess", "Project period", "Project financials", PROPOSAL + "; project financial model")
# EEI
prod("Electronic Equipment", "EEI-STD", "Electronic Equipment Insurance (BIBS EEI01, EEI03, EEI04)", "All risks on electronic equipment", "Owner / lessor", "Material damage all risks", "External data media, increased cost of working, EAR extension (BDOI EEI01 / EEI04)", "Wear and tear, items under maintenance contract, software", "New replacement value", "Rate x SI; minimum premium", "Per occurrence; % for portable items", "Annual", "Equipment list with serial numbers and values, location", "Proposal, equipment schedule, purchase invoices", "BDOI FRS (EEI); BIBS catalogue")
# Others
for code, name, desc, src in [
    ("MSC-GIP", "Golfer's Insurance / Hole-in-One (BIBS GIP01)", "Golfer's liability, equipment and hole-in-one celebration expenses", "BDOI FRS"),
    ("MSC-ST", "Sabotage and Terrorism (BIBS S_AND_T)", "Property damage from sabotage and terrorism", "BDOI FRS"),
    ("MSC-PV", "Political Violence (BIBS PV)", "Damage from riots, insurrection, war-like acts", "BDOI FRS"),
    ("MSC-TC", "Trade Credit (BIBS TRADE_CREDIT)", "Non-payment by buyers", "BDOI FRS"),
    ("MSC-PAR", "Parametric (BIBS PARAMETRIC)", "Payout on trigger index (e.g. typhoon wind speed, earthquake magnitude)", "BDOI FRS"),
    ("MSC-FA", "Fine Arts (BIBS FINE_ARTS)", "All risks on artworks and collections", "BDOI FRS"),
    ("MSC-JB", "Jewellers Block (BIBS JEWELERS_BLOCK)", "All risks on jewellers' stock", "BIBS catalogue"),
    ("MSC-EVT", "Event Cancellation / Event Liability", "Cancellation costs and liability of events", MKT),
]:
    prod("Others / Miscellaneous", code, name, desc, "Insured", desc, "Per product", "Per product wording", "Per product (SI, limit or index payout)", "Rate x SI or flat premium", "Per product", "Annual or event period", "Description of the risk; properties insured and basis of valuation (BDOI FRS for S&T, PV, parametric)", PROPOSAL, src)
# Marine cargo
for code, name, desc, src in [
    ("MAR-INL", "Inland Transit / Inland Marine (BIBS INLAND, INL01-INL06)", "Goods transported by land within the Philippines", "BIBS catalogue"),
    ("MAR-ISL", "Inter-Island (BIBS INTER_ISLAND)", "Domestic sea shipments between islands", "BIBS catalogue"),
    ("MAR-IMP", "Import Cargo - single shipment or open cover (BIBS IMPORT, COMBINED, MOP02-MOP14)", "Imports under ICC (A), (B) or (C)", "BDOI FRS (Marine Cargo)"),
    ("MAR-EXP", "Export Cargo (BIBS EXPORT, MON05 non-VAT)", "Exports under ICC (A), (B) or (C)", "BIBS catalogue"),
    ("MAR-TR", "Trust Receipt Programme (BIBS TRUST_RECEIPT, TRP01-TRP03)", "Goods under bank trust receipt arrangements", "BIBS catalogue"),
    ("MAR-STP", "Stock Throughput (BIBS STOCK_THROUGHPUT)", "Stocks in transit and in storage under one policy", "BDOI FRS (Stock Throughput)"),
    ("MAR-PC", "Project Cargo (BIBS PROJECT_CARGO)", "Cargo for construction projects, with DSU option", "BIBS catalogue"),
    ("MAR-LIA", "Charterer's / Terminal Operator's / Freight Forwarder's Liability", "Logistics operators' legal liability", "BIBS catalogue"),
]:
    prod("Marine (Cargo)", code, name, desc, "Cargo owner (importer / exporter), bank under L/C or trust receipt", "ICC (A) all risks, (B) or (C) named perils; inland transit clauses", "War and strikes clauses, warehouse to warehouse, refrigeration", "Inherent vice, insufficient packing, delay, wilful misconduct", "CIF value plus 10% (market practice)", "Rate x shipment value; open cover deposit premium and declarations", "Per shipment deductible", "Single voyage or annual open cover", "Details of cargo and packaging, conveyance, limit per shipment, turnover past / next 12 months", PROPOSAL + "; invoice, bill of lading, L/C", src)
# Crime
for code, name, desc, src in [
    ("CRM-MSP", "Money, Securities and Payroll (BIBS MSP)", "Loss of money and securities on premises and in transit", FRS),
    ("CRM-FG", "Fidelity Guarantee (BIBS FG)", "Employee dishonesty", FRS),
    ("CRM-MFG", "Combined FG and MSP (BIBS MSP_FG)", "MSP and fidelity in one policy", FRS),
    ("CRM-3D", "Dishonesty, Disappearance and Destruction (BIBS DDD)", "Comprehensive crime", FRS),
    ("CRM-BBB", "Bankers Blanket Bond (BIBS BBB)", "Financial institution crime cover", FRS),
    ("CRM-KR", "Kidnap and Ransom", "Ransom, response consultants, extortion", FRS),
]:
    prod("Crime", code, name, desc, "Insured entity", desc, "Messenger / armoured car, forgery, computer crime (per product)", "Losses discovered outside the period, consequential loss, losses by owners", "Limit per loss / per employee / per messenger", "Rate x limit; per-employee premium for FG", "Per loss deductible", "Annual", "Nature of business, limit of liability, cash handling procedures, employee counts", PROPOSAL + "; cash handling procedures", src)
# Marine hull
for code, name, desc, src in [
    ("HUL-HM", "Hull and Machinery (BIBS HULL_MACHINERY)", "Physical damage to vessels", FRS),
    ("HUL-PI", "Protection and Indemnity (BIBS P_AND_I)", "Shipowner's third-party liabilities", "BIBS catalogue"),
    ("HUL-BR", "Builder's Risk (BIBS BUILDERS_RISK)", "Vessels under construction", "BIBS catalogue"),
    ("HUL-SR", "Ship Repairer's Liability (BIBS SHIP_REPAIRERS)", "Liability of shipyards for vessels in their care", "BIBS catalogue"),
]:
    prod("Marine Hull", code, name, desc, "Shipowner / operator, mortgagee", desc, "Collision liability (3/4 RDC), war risks, increased value", "Unseaworthiness with owner's privity, wear and tear", "Agreed value", "Rate x agreed value", "Per occurrence deductible", "Annual", "Vessel name, operator, type, year of construction, tonnage, cargo carried, seats, construction material", PROPOSAL + "; class / MARINA certificates, condition survey", src)
# Floater
prod("Property / Equipment Floater", "FLT-EPF", "Equipment / Property Floater (BIBS FLOATER, EPF02, EPF04, EPF05)", "All risks on mobile or leased equipment anywhere in the territory", "Owner, lessor (e.g. leasing company)", "All risks of physical loss or damage", "Political violence (EPF05), transit", "Mechanical breakdown, wear and tear, unexplained loss", "Replacement or actual cash value", "Rate x SI; minimum premium", "Per occurrence", "Annual / lease term", "Equipment description, SI, basis of valuation, locations", PROPOSAL + "; equipment list, lease contract", "BDOI FRS (Floater); BIBS catalogue")
# Aviation
for code, name, desc, src in [
    ("AVN-GA", "General Aviation Hull and Liability (BIBS GENERAL_AVIATION)", "Aircraft hull and third-party / passenger liability", FRS),
    ("AVN-DRN", "Drone (BIBS DRONE)", "Drone hull and liability", "BIBS catalogue"),
    ("AVN-APT", "Airport Owner's / Operator's / Contractor's Liability", "Airport premises liability", "BIBS catalogue"),
    ("AVN-PRL", "Aviation Products / Refuellers / Hangar Keepers Liability", "Aviation trade liabilities", "BIBS catalogue"),
    ("AVN-PLL", "Pilot Loss of Licence (BIBS PILOT_LOSS_LICENSE)", "Benefit when a pilot loses the medical licence", "BIBS catalogue"),
]:
    prod("Aviation", code, name, desc, "Operator, owner, lessor", desc, "War and allied perils (AVN 52), spares", "Unairworthy aircraft, unlicensed pilots, use outside agreed purpose", "Agreed value; liability limit", "Rate x agreed value; flat premium for liability", "Per occurrence / ingestion deductible", "Annual", "Type of aircraft, make / model, registration mark, serial no., year of manufacture, pilot details, operator, hangar, geographical limits", PROPOSAL + "; airworthiness and registration certificates, pilot licences", src)
# Casualty
for code, name, desc in [("CAS-BUR", "Burglary and Robbery", "Theft with forcible entry and robbery of contents"),
                         ("CAS-GLS", "Plate Glass", "Accidental breakage of fixed glass"),
                         ("CAS-ARP", "All Risks on Personal Property", "Gadgets, jewellery, personal items")]:
    prod("Casualty", code, name, desc, "Owner", desc, "Per product", "Theft without forcible entry, employee theft", "Replacement or actual cash value", "Rate x SI; minimum premium", "Per loss deductible", "Annual", "Location, security measures, values", PROPOSAL)

# ---------------------------------------------------------------------------------------------------------------
# Covers and perils matrix: peril -> {LOB: S/O}; LOBs not listed are n/a.
S, O = "Standard", "Optional"
PERILS = [
    ("Fire and lightning", {"Property": S, "Property / Equipment Floater": S, "Engineering": S, "Electronic Equipment": S, "Marine (Cargo)": S, "Marine Hull": S, "Motor": S, "Aviation": S, "Others / Miscellaneous": O}),
    ("Earthquake fire and shock", {"Property": O, "Engineering": O, "Electronic Equipment": O, "Property / Equipment Floater": O, "Motor": O}),
    ("Typhoon and flood", {"Property": O, "Engineering": O, "Electronic Equipment": O, "Property / Equipment Floater": O, "Motor": O}),
    ("Extended coverage (windstorm, explosion, aircraft, vehicle impact, smoke)", {"Property": O}),
    ("Riot, strike, malicious damage / civil commotion", {"Property": O, "Motor": O, "Engineering": O, "Marine (Cargo)": O, "Property / Equipment Floater": O}),
    ("Political violence / sabotage and terrorism", {"Others / Miscellaneous": S, "Property / Equipment Floater": O, "Property": O, "Motor": O}),
    ("All risks of physical loss or damage", {"Property": O, "Engineering": S, "Electronic Equipment": S, "Property / Equipment Floater": S, "Marine (Cargo)": O, "Casualty": O, "Others / Miscellaneous": O}),
    ("Business interruption / loss of profit", {"Property": O, "Engineering": O, "Electronic Equipment": O}),
    ("Machinery / electrical breakdown", {"Engineering": S, "Property": O, "Electronic Equipment": S}),
    ("Own damage and theft (vehicle)", {"Motor": S}),
    ("Acts of nature (vehicle)", {"Motor": O}),
    ("CTPL (statutory third-party injury)", {"Motor": S}),
    ("Excess bodily injury / third-party property damage", {"Motor": O}),
    ("Third-party liability (general)", {"Liability": S, "Engineering": O, "Marine Hull": O, "Aviation": S, "Casualty": O, "Others / Miscellaneous": O}),
    ("Professional / financial loss liability", {"Liability": O}),
    ("Products liability", {"Liability": O, "Aviation": O}),
    ("Employer's liability", {"Liability": O}),
    ("Accidental death and disablement", {"Personal Accident": S, "Motor": O, "Health Insurance": O}),
    ("Medical reimbursement / hospitalisation", {"Personal Accident": O, "Health Insurance": S}),
    ("Out-patient and preventive care", {"Health Insurance": S}),
    ("Travel inconvenience (trip cancellation, baggage)", {"Personal Accident": O}),
    ("Perils of the sea / ICC (A) all risks", {"Marine (Cargo)": S, "Marine Hull": S}),
    ("ICC (B) / (C) named perils", {"Marine (Cargo)": O}),
    ("War and strikes (marine / aviation)", {"Marine (Cargo)": O, "Marine Hull": O, "Aviation": O}),
    ("Collision liability (running down clause)", {"Marine Hull": S}),
    ("Protection and indemnity", {"Marine Hull": O}),
    ("Aircraft hull (ground and flight)", {"Aviation": S}),
    ("Passenger liability", {"Aviation": S, "Marine Hull": O}),
    ("Money in premises / in transit", {"Crime": S}),
    ("Employee dishonesty / fidelity", {"Crime": S}),
    ("Forgery, computer and funds transfer fraud", {"Crime": O}),
    ("Kidnap, ransom and extortion", {"Crime": O}),
    ("Burglary / robbery", {"Casualty": S, "Property": O, "Crime": O}),
    ("Plate glass breakage", {"Casualty": O}),
    ("Surety guarantee (principal's default)", {"Bond": S}),
    ("Cyber / data breach", {"Liability": O, "Others / Miscellaneous": O}),
    ("Hole-in-one / golfer's liability", {"Others / Miscellaneous": O}),
]

# Clauses: (lob, clause, type, purpose, source)
CLAUSES = [
    ("Property", "Mortgagee / loss payee clause", "Clause", "Bank as mortgagee receives claim proceeds; required for BDO-financed collateral", MKT),
    ("Property", "Fire extinguishing appliances warranty", "Warranty", "Appliances kept in working order (BIBS clause PAR_WARRANTY_EXT)", "BIBS catalogue"),
    ("Property", "Flood exclusion (basement)", "Exclusion", "Excludes flood loss to basement property (BIBS clause PAR_FLOOD_EXCL)", "BIBS catalogue"),
    ("Property", "72-hour clause", "Clause", "Typhoon / flood / earthquake losses within 72 hours are one occurrence", MKT),
    ("Property", "Earthquake fire and shock endorsement", "Endorsement", "Adds earthquake perils", MKT),
    ("Property", "Typhoon and flood endorsement", "Endorsement", "Adds typhoon and flood perils", MKT),
    ("Property", "Riot, strike and malicious damage (RSMD)", "Endorsement", "Adds RSMD perils", MKT),
    ("Property", "Average / under-insurance (co-insurance) clause", "Condition", "Pro-rata claim if under-insured", MKT),
    ("Property", "Debris removal, professional fees, temporary removal", "Extension", "Additional costs after a loss", MKT),
    ("Property", "Reinstatement value clause", "Clause", "Claims on new-for-old basis", MKT),
    ("Property", "Automatic reinstatement of sum insured", "Clause", "SI restored after a loss (additional premium)", MKT),
    ("Motor", "Accredited repair shop clause", "Clause", "Repairs at insurer-accredited shops (BIBS clause MTR_ACCREDITED_REPAIR)", "BIBS catalogue"),
    ("Motor", "Depreciation waiver clause", "Clause", "No depreciation on parts in the first year (BIBS MTR_DEPRECIATION)", "BIBS catalogue"),
    ("Motor", "Participation fee", "Deductible wording", "Insured bears a fee per OD claim (BIBS MTR_PARTICIPATION)", "BIBS catalogue"),
    ("Motor", "Mortgagee / loss payee clause (auto loan)", "Clause", "Bank named as loss payee for financed vehicles", MKT),
    ("Motor", "Authorised driver clause", "Condition", "Cover only for licensed authorised drivers", MKT),
    ("Motor", "Acts of nature endorsement", "Endorsement", "Adds AON", "BDOI NB BRD"),
    ("All lines", "Sanctions limitation and exclusion clause", "Exclusion", "No cover or payment where it breaches sanctions (BIBS GEN_SANCTIONS)", "BIBS catalogue"),
    ("All lines", "Waiver of subrogation", "Clause", "Insurer waives recovery against named parties (e.g. lessee, contractor)", MKT),
    ("All lines", "Cancellation clause (short period scale)", "Condition", "Short-period rate on insured's cancellation; pro-rata on insurer's (NB BRD: short term period rate)", "BDOI NB BRD"),
    ("All lines", "Premium payment warranty (Insurance Code s.77)", "Warranty", "No cover until premium is paid unless credit is granted (confirm IC rules on credit extension)", MKT),
    ("All lines", "Electronic date recognition / cyber exclusion", "Exclusion", "Excludes cyber and date-related losses", MKT),
    ("All lines", "Communicable disease exclusion", "Exclusion", "Excludes losses from communicable disease", MKT),
    ("Engineering", "Maintenance visits / extended maintenance", "Endorsement", "Covers maintenance period after completion", MKT),
    ("Engineering", "Cross liability", "Endorsement", "Each insured party treated as separately insured", MKT),
    ("Engineering", "Designer's risk (LEG 2 / DE 3)", "Endorsement", "Limited cover for defective design consequences", MKT),
    ("Engineering", "Testing and commissioning", "Condition", "Cover during testing (EAR) for a stated period", MKT),
    ("Electronic Equipment", "External data media / increased cost of working", "Extension", "Sections 2 and 3 of EEI", MKT),
    ("Marine (Cargo)", "Institute Cargo Clauses (A), (B), (C)", "Wording", "Scope of cargo perils", MKT),
    ("Marine (Cargo)", "Institute War / Strikes Clauses (Cargo)", "Endorsement", "War and strikes risks", MKT),
    ("Marine (Cargo)", "Warehouse to warehouse / transit clause", "Clause", "Duration of transit cover", MKT),
    ("Marine (Cargo)", "Inland transit clauses (rail / road)", "Wording", "Domestic land transit cover", MKT),
    ("Marine Hull", "Institute Time Clauses - Hulls", "Wording", "Hull and machinery cover", MKT),
    ("Marine Hull", "3/4ths collision liability (running down clause)", "Clause", "Collision liability share", MKT),
    ("Aviation", "War, hi-jacking and other perils exclusion (AVN 48) / write-back (AVN 52)", "Exclusion / endorsement", "Aviation war risks", MKT),
    ("Liability", "Cross liability", "Endorsement", "Separate insured treatment", MKT),
    ("Liability", "Retroactive date (claims-made)", "Condition", "Claims-made trigger for D&O, PI, E&O, cyber", MKT),
    ("Liability", "Additional insured / principal's indemnity", "Endorsement", "Mall / lessor / principal named (BDOI mall tenants CGL)", MKT),
    ("Crime", "Messenger / armoured car limit", "Condition", "Limits for money in transit", MKT),
    ("Crime", "Discovery period", "Condition", "Losses discovered within a set period after expiry", MKT),
    ("Bond", "Bond conditions per obligee form (e.g. government agency form)", "Wording", "Forms prescribed by obligee / IC (confirm)", MKT),
    ("Personal Accident", "Age limit and occupation class clause", "Condition", "Eligibility limits", MKT),
    ("Health Insurance", "Pre-existing condition waiver", "Endorsement", "Waives waiting period for groups above a size", MKT),
]

# Rating and charges per LOB: (lob, rating basis, minimum premium, short period, taxes rule, commission, source)
COMMISSION = "Commission = commission rate x net premium; VAT on commission 12% (BDOI NB BRD Appendix A). Rates per insurer x product (product matrix); incentive amount or incentive commission rate per FRS PM v1.0"


# Underwriting data dictionary: (field, type, mandatory, LOV, LOBs, source)
ALL = "All lines"
DICT = [
    ("Line of Insurance", "Dropdown", "Yes", "15 BDOI LOBs (FRS uses 14 product lines incl. Stock Throughput)", ALL, FRS),
    ("Transaction Type", "Dropdown", "Yes", "New Business, Renewal, Endorsement (Engineering and Bonds: NB and Endorsement only)", ALL, FRS),
    ("Expiring / Existing Policy No.", "Text", "Conditional (renewal / endorsement)", "n/a", ALL, FRS),
    ("Product Type", "Dropdown", "Yes", "Package, Non-Package", ALL, FRS),
    ("Policy Type", "Dropdown", "Yes", "Individual, Fleet, Group, Single Location, Various Locations", "Motor, Property, EEI, Floater, Marine Cargo, Marine Hull, Aviation, Stock Throughput, Personal Accident, Miscellaneous", FRS),
    ("Sub-Line", "Dropdown", "Yes", "Per line (see Comparison sheet: Engineering, Liability, Crime, Bonds, PA, Miscellaneous values)", "Engineering, Liability, Crime, Bonds, Personal Accident, Miscellaneous", FRS),
    ("Package Name / Package Description", "Text", "Yes (package)", "n/a", ALL, FRS),
    ("Insurer", "Dropdown", "Yes", "Accredited insurers", ALL, FRS),
    ("Business Origin", "Dropdown", "Yes", "Business origin LOV", ALL, FRS),
    ("Account Officer / Unit Head", "Dropdown", "Yes", "User LOV", ALL, FRS),
    ("Insured's Name / Mailing Address", "Text", "Yes", "n/a", ALL, FRS),
    ("Client / Market segment", "Lookup", "Yes", "Market segment LOV (e.g. CBG, RETAIL)", ALL, "BIBS catalogue field rules"),
    ("Effectivity Date / Expiry Date", "Date", "Yes", "dd-Mmm-yyyy", ALL, FRS),
    ("Currency", "Dropdown", "Yes", "Currency LOV", "Motor, Property, Engineering, EEI, Floater, Miscellaneous, Marine Cargo, Stock Throughput, Liability, Crime, Bonds", FRS),
    ("Premium / Premium Rates / Commission Rates", "Currency / Percentage", "Yes", "n/a", ALL, FRS),
    ("Warranties, Clauses", "Dropdown / Text", "Optional", "Clause library", ALL, FRS),
    ("Other Conditions, Subjectivities, Exclusions", "Text", "Optional", "n/a", ALL, FRS),
    ("Deductibles", "Text", "Optional", "n/a", ALL, FRS),
    ("Extensions of Cover", "Text", "Optional", "n/a", "Property, Engineering, EEI, Floater, Marine Cargo, Marine Hull, Aviation, Stock Throughput, Liability, Crime, Personal Accident, Miscellaneous", FRS),
    ("Type of Cover", "Dropdown", "Yes", "All-Risk, F/L Only, Named Perils", "Property", FRS),
    ("Maximum Limit/s", "Text", "Optional", "n/a", "Property", FRS),
    ("Description of Cover", "Text", "Yes", "n/a", "Property, Personal Accident", FRS),
    ("Cover / Undertaking", "Text", "Yes", "n/a", "Engineering, EEI, Floater, Stock Throughput, Liability, Crime, Miscellaneous, Bonds", FRS),
    ("Perils / Coverages", "Text", "Yes", "Coverage / peril master", "Motor, Marine Cargo, Marine Hull, Aviation", FRS),
    ("Benefits / Coverages", "Dropdown", "Yes", "Benefit LOV", "Personal Accident", FRS),
    ("Vehicle Type", "Dropdown", "Yes", "PC Sedan, PC SUV/AUV, CV, Motorcycle, LTO, Various", "Motor", FRS),
    ("Engine Type", "Dropdown", "Yes", "Gas, Diesel, Hybrid, Fully-Electric, Various", "Motor", FRS),
    ("Year Model / Manufacturer / Model-Variant", "Number / Dropdown", "Yes", "Vehicle master", "Motor", FRS),
    ("Plate No. or Conduction Sticker", "Text", "Yes (one of)", "n/a", "Motor", "BIBS catalogue field rules"),
    ("Engine No. / Chassis No.", "Text", "Yes", "n/a", "Motor", "BIBS catalogue field rules"),
    ("Sum Insured per Peril", "Currency", "Yes", "n/a", "Motor", FRS),
    ("Location/s of Risk (address, city, province)", "Text / Dropdown", "Yes", "City / province LOV", "Property, EEI, Floater, Stock Throughput, Crime, Miscellaneous", FRS),
    ("Properties Insured", "Dropdown", "Yes", "Building / improvements, Contents, Machinery / Equipment, Stocks (free text for Golfer's, Fine Arts)", "Property, Miscellaneous", FRS),
    ("Description of Properties Insured", "Text", "Yes", "n/a", "Property, EEI, Floater, Miscellaneous", FRS),
    ("Sum Insured per Property Insured", "Currency", "Yes", "n/a", "Property, EEI, Floater, Miscellaneous", FRS),
    ("Basis of Valuation", "Dropdown", "Yes", "Sound value / ACV, Replacement value, Cost price, Selling price", "Property, EEI, Floater, Stock Throughput, Miscellaneous (S&T, PV, Parametric)", FRS),
    ("Total Sum Insured", "Currency (computed)", "Computed", "Sum of SI per property", "Property, EEI, Floater, Stock Throughput", FRS),
    ("Building Description (construction class)", "Dropdown", "Yes", "Class A, Class B, Light Material", "Property, Stock Throughput", FRS),
    ("Building Occupancy", "Dropdown", "Yes", "Residential, General, Warehouse, Industrial", "Property", FRS),
    ("Contractor's Name / Project Description / Project Location", "Text", "Yes", "n/a", "Engineering", FRS),
    ("Total Contract Value / Sum Insured per Cover", "Currency", "Yes", "n/a", "Engineering", FRS),
    ("Nature of Business", "Text", "Yes", "n/a", "Marine Cargo, Marine Hull, Liability, Crime, Bonds, Miscellaneous", FRS),
    ("Details of Cargo / Packaging", "Text", "Yes", "n/a", "Marine Cargo, Stock Throughput", FRS),
    ("Type of Conveyance", "Dropdown", "Yes", "Sea, air, land (confirm LOV)", "Marine Cargo", FRS),
    ("Limit per Conveyance / Shipment; Frequency of Delivery", "Currency / Text", "Yes", "n/a", "Marine Cargo", FRS),
    ("Turnover past 12 months / next 12 months", "Currency", "Yes", "n/a", "Marine Cargo", FRS),
    ("Vessel Name / Operator and Manager / Type of Vessel", "Text / Dropdown", "Yes", "Vessel type LOV", "Marine Hull", FRS),
    ("Year of construction (FRS field for vessels and aircraft)", "Number", "Yes", "n/a", "Marine Hull, Aviation", FRS),
    ("Gross / Net Tonnage; Cargoes Carried; Passenger and Crew Seats; Construction Material", "Number / Text / Dropdown", "Yes", "n/a", "Marine Hull", FRS),
    ("Agreed Value", "Currency", "Yes", "n/a", "Marine Hull, Aviation", FRS),
    ("Type of Aircraft / Make-Model / Registration Mark No. / Serial No.", "Dropdown / Text", "Yes", "n/a", "Aviation", FRS),
    ("Pilot Details / Aircraft Operator / Hangar Location / Geographical Limits", "Text", "Yes", "n/a", "Aviation", FRS),
    ("Origin / Destination of Goods; Average and Maximum Monthly Inventory; Annual Sales Turnover; Description of Stocks", "Text / Currency", "Yes", "n/a", "Stock Throughput", FRS),
    ("Location of Premises", "Text", "Yes", "n/a", "Liability", FRS),
    ("Limit of Liability", "Currency", "Yes", "n/a", "Liability, Crime", FRS),
    ("Bond Amount", "Currency", "Yes", "n/a", "Bonds", FRS),
    ("Obligee / Contract reference", "Text", "Yes", "n/a", "Bonds", MKT),
    ("Occupation / Nature of Business; No. of Persons Covered", "Text / Number", "Yes", "n/a", "Personal Accident", FRS),
    ("Insured person / Birth date", "Text / Date", "Yes", "n/a", "Personal Accident", "BIBS catalogue field rules"),
    ("Member census (name, birth date, plan, dependants)", "File / Table", "Yes", "Plan LOV", "Health Insurance", MKT),
    ("Mortgagee / loss payee", "Lookup", "Conditional (mortgaged risks)", "Bank / branch LOV", "Property, Motor", "BIBS catalogue (mortgage-applicable products)"),
    ("Incentive Eligible / Incentive Amount / Incentive Commission Rate", "Boolean / Currency / Percentage", "Conditional", "Default No", ALL, FRS),
]

# Documents and KYC per LOB: (lob, proposal, kyc, survey, valuation, other, claims, source)
KYC = "Individuals: valid government ID, TIN; entities: SEC / DTI registration, GIS, board resolution / secretary's certificate, TIN; AMLA customer due diligence and sanction screening (BDOI Sanction Screening BRD)"

# Migration checks: (id, check, rule, source, severity, how to verify)
CHECKS = [
    ("MC-01", "Mapping completeness", "Every legacy LOB, product, package / plan and risk / coverage code in use maps to exactly one BIBS code (or is marked Retire)", "BRID_MIG 3.1", "Blocking", "Count of distinct legacy codes = count of mapping rows with status Mapped or Retire"),
    ("MC-02", "No one-to-many conflicts", "A legacy code is not mapped to two BIBS codes; many legacy codes to one BIBS code are allowed only with a remark", "BRID_MIG 3.1", "Blocking", "Group mapping rows by legacy system + legacy code"),
    ("MC-03", "Target codes exist and are active", "Every BIBS line / product / cover type / coverage code in the mapping exists in the BIBS catalogue with status Active (authorised)", "BRID_MIG 3.1", "Blocking", "Look up each target code on the Products and Coverages screens"),
    ("MC-04", "No orphan policies", "Every migrated in-force policy header carries a line of business and product that resolve through the mapping", "BRID_MIG 4.1", "Blocking", "Policies without a mapped product = 0"),
    ("MC-05", "Inactive / retired products", "Legacy products without in-force policies are marked Retire or migrated as inactive (history only); none can be sold after cutover", "BRID_MIG 1.1", "High", "Active policies count = 0 for every Retire row"),
    ("MC-06", "Mapping version control and sign-off", "The mapping list is versioned and signed off by the data steward and the product owner before the load", "BRID_MIG 3.1", "Blocking", "Signed mapping version referenced in the load log"),
    ("MC-07", "Unmapped / invalid codes reported", "Any code found in the data but not in the mapping is reported as fall-out, not loaded", "BRID_MIG 3.1, 1.4", "High", "Fall-out report with zero unmapped product codes after remigration"),
    ("MC-08", "Reconciliation counts", "Policy counts and premium totals per LOB and product reconcile between legacy and BIBS", "BRID_MIG 1.2", "Blocking", "Migration reconciliation report per LOB / product"),
    ("MC-09", "Insurer x product validity", "Each mapped product has its insurer(s) on the package panel with commission rate effective on the policy dates", "BRID_MIG 3.1", "High", "Insurer-product commission rows exist for all policies"),
    ("MC-10", "Rating and tax attributes", "Each product carries rate, minimum premium, commission rate, VAT / non-VAT regime and FST applicability (Property) before the load", "NB BRD Appendix A", "High", "No product with missing rate / tax regime"),
    ("MC-11", "Mandatory fields per LOB", "Migrated in-force risks hold the mandatory underwriting fields of their LOB (see Data Dictionary) or are flagged for completion at first endorsement / renewal", "BRID_MIG 4.1, FRS PM v1.0", "Medium", "Exception list per LOB"),
    ("MC-12", "LOV values mapped", "Legacy LOVs used by products (vehicle type, occupancy, construction class, basis of valuation, policy type) map to BIBS LOV values", "BRID_MIG 3.1", "High", "LOV mapping sheet complete"),
    ("MC-13", "MIS / business-line mapping", "Each BIBS product maps to one MIS business line (FIRE, MOTOR, MARINE, ENGG, CASUALTY, PA, HEALTH, BONDS) for reporting", "BRID_MIG 3.1", "High", "Every product has an MIS line"),
    ("MC-14", "Migration identifier and source", "Every migrated product-related record carries the migrated flag and the legacy source system", "BRID_MIG 1.3", "High", "Records without source system = 0"),
    ("MC-15", "Package versions and effective dates", "Packages with in-force policies have a BIBS package version effective on the policy inception date", "BRD-03 FRS v1.3", "Medium", "Policies without an effective package version = 0"),
    ("MC-16", "Renewal readiness", "Products with policies expiring in the first renewal cycle after cutover are Mapped (not Needs confirmation)", "Renewal BRD", "High", "Renewal list by product vs mapping status"),
]

GLOSSARY = [
    ("ACV", "Actual cash value: replacement cost less depreciation"), ("AON / AOG", "Acts of nature / acts of God (motor extension: typhoon, flood, earthquake)"),
    ("BBB", "Bankers blanket bond: crime cover for financial institutions"), ("BFP", "Bureau of Fire Protection; issues fire certificates for claims"),
    ("BI", "Business interruption (property) or bodily injury (motor) - read in context"), ("BIBS", "BDOI Broker System, the new core system"),
    ("Broker", "Intermediary that places risks with insurers for the client; BDOI is a broker, not an insurer"), ("CAR / CARI", "Contractor's all risks insurance"),
    ("CGL", "Comprehensive general liability"), ("Cover type", "BIBS level under a product line (e.g. Liability > CGL)"),
    ("Coverage / peril", "BIBS level under a line: a cover or peril with its own limit, rate or deductible"), ("CPM", "Contractor's plant and machinery"),
    ("CTPL", "Compulsory third party liability for motor vehicles, required for LTO registration"), ("DSU / ALOP", "Delay in start-up / advance loss of profits"),
    ("DST", "Documentary stamp tax on insurance premiums"), ("EAR", "Erection all risks"), ("EEI", "Electronic equipment insurance"),
    ("EPF", "Equipment / property floater"), ("FFY", "Free first year (motor bank packages)"), ("FG", "Fidelity guarantee"),
    ("FST", "Fire service tax (RA 9514) on fire insurance premiums"), ("HMO", "Health maintenance organisation"),
    ("IC", "Insurance Commission of the Philippines"), ("ICC (A)/(B)/(C)", "Institute Cargo Clauses: all risks / wider named perils / restricted named perils"),
    ("IAR", "Industrial all risks"), ("JCL", "Bond forms for government contracts (confirm with BDOI)"), ("LGT", "Local government (business) tax on premiums, rate per insurer branch locality"),
    ("LOB", "Line of business"), ("LOV", "List of values (drop-down)"), ("MBD", "Machinery breakdown"),
    ("MIS", "Management information system business-line classification used in reports"), ("MSP", "Money, securities and payroll"),
    ("OOTB", "Out of the box: the iNXT BrokerVerse platform product set before BDOI configuration"), ("OD", "Own damage"),
    ("Package", "Pre-agreed product with set terms, rates and insurer panel (BDOI FRS: Package / Non-Package)"),
    ("P&I", "Protection and indemnity (shipowner liabilities)"), ("PTX", "Premium tax label used in BDOI NB BRD Fire computation (12%)"),
    ("Risk code", "BDOI product code of a programme / package (e.g. CAR00, MOP05, PAR08)"), ("RSMD / RSCC", "Riot, strike and malicious damage / civil commotion"),
    ("SI / TSI", "Sum insured / total sum insured"), ("Short period", "Premium scale for cancellations by the insured before expiry"),
    ("TSU", "Technical services unit (underwriting) of BDOI"), ("VAT", "Value added tax, 12%"), ("VTPL", "Voluntary third party liability (excess BI / PD)"),
]

SOURCES = [
    ("Source document", "FRS - BDOI BROKERSYS Product Maintenance v1.0 (BDOI, 19-Aug-2026).docx", "Lines of insurance, sub-line values per line, underwriting fields per line (Data Dictionary, Comparison)"),
    ("Source document", "Product Maintenance.pdf (BRD-03)", "Product hierarchy and product matrix requirements"),
    ("Source document", "New Business (NB) BRD 05132026.pdf", "Appendix A computation guide (Fire and Motor taxes, DST rounding, minimum premium, short period, commission and VAT on commission); Appendix B product matrix (PAR / MTR packages)"),
    ("Source document", "Renewal (RN) BRD.pdf", "Renewal considerations"),
    ("Source document", "BDO Insure Core Modernization - Data Migration - BRD V0.03.pdf (BRD-13)", "BRID_MIG 1.1-4.1 reconciliation and reference-data mapping requirements (Migration checks); legacy systems Ebix, QPS, Broker, iSys"),
    ("Source document", "Employee Benefits.pdf", "Employee benefits lines (HMO, group life, group PA)"),
    ("Source document", "00 - BRD BDOI Core Replacement v01.pdf (BRD-00)", "Programme scope"),
    ("Source document", "Sanction Screening and Risk Profiling BRD", "KYC / sanction screening reference"),
    ("Programme deliverable", "BIBS FRS-BDOI BRD-03 Product Maintenance v1.3 pack (Programme/BDOI_Template_FRS/BRD-03_Product_Maintenance)", "Product / package / coverage levels and package versions"),
    ("BIBS configuration", "BIBS New Business catalogue: product lines, cover types, risk products (Annex I, Appendix B), coverages / perils, clauses, package versions, Employee Benefits lines, business-line lookups", "BIBS product set (read 12-Oct-2026)"),
    ("OOTB reference", "iNXT BrokerVerse platform (branch brokerverse-platform): line-of-business, product category, product, policy type, cover, risk section, taxation masters and product templates", "OOTB product set (read 12-Oct-2026)"),
    ("Market practice", "Philippine non-life market practice: Insurance Code (PD 612 as amended by RA 10607), IC circulars, NIRC, RA 9514, Local Government Code, standard wordings", "Product library rows marked 'Market practice (to confirm with BDOI)'"),
]
ASSUMPTIONS = [
    "Proposed LOB and product codes (e.g. PRO, MTR-COMP) are suggestions for the mapping only; BDOI's legacy codes take precedence.",
    "IC class shown per LOB follows the usual IC annual-statement lines (Fire, Marine, Motor Car, Casualty, Suretyship, Aviation); confirm BDOI's MIS classification.",
    "DST 12.5% applies to all non-life lines as in BDOI's Fire and Motor computation; the legal basis and rate for bonds, PA and health are to be confirmed.",
    "VAT 12% applies to non-life premiums of VAT-registered insurers; non-VAT products (CTP02, MON05) and premium-tax insurers to be confirmed per insurer.",
    "FST 2% applies to fire insurance premiums (Property); whether it applies to the fire peril of engineering, floater, EEI or householder covers is to be confirmed.",
    "LGT rate depends on the insurer branch (BDOI NB BRD); no rate is assumed.",
    "Short-period scale percentages are not stated in BDOI documents; insurer scales apply.",
    "CTPL premium follows the current IC tariff per vehicle class; no amount is quoted.",
    "Health (HMO) is handled through the Employee Benefits lines in BIBS; individual health is assumed out of scope until BDOI confirms.",
    "Casualty is assumed to hold burglary, plate glass and personal all risks; BDOI documents give no product list for it.",
    "Stock Throughput is treated under Marine (Cargo) in this library, while BDOI's FRS lists it as its own product line.",
]
QUESTIONS = [
    "Q1 Product matrix files MTR08-MTR38 and PAR01-PAR25 (NB BRD Appendix B): please share so the packages can be loaded and mapped.",
    "Q2 Legacy code lists: LOB, product, package / plan, risk and coverage codes from Ebix, QPS, Broker and iSys, with active policy counts per code.",
    "Q3 Bond: confirm bond types placed (surety, bidder's, performance, warranty, judicial, heirs, payment, JCL, customs) and add Bond as a BIBS product line.",
    "Q4 Casualty: which products sit under Casualty (burglary, plate glass, personal all risks, CPL)?",
    "Q5 Health: does BDOI place individual health / HMO, or only group programmes under Employee Benefits?",
    "Q6 Stock Throughput: own LOB (as in the FRS) or under Marine (Cargo) (as in BDOI's LOB list)?",
    "Q7 Motor: are OD-only, AON-only or VTPL-only policies issued, or always as coverages of Comprehensive?",
    "Q8 Engineering: are MBD, CPM, boiler and DSU placed? Under Engineering or Floater?",
    "Q9 Liability: medical malpractice, marine liability and excess auto liability need cover types - confirm names and codes.",
    "Q10 Crime: kidnap and ransom - confirm and provide code.",
    "Q11 Taxes: confirm VAT / premium-tax regime per insurer, FST scope beyond Fire, DST on bonds / PA / health, LGT rates per insurer branch.",
    "Q12 Group PA: one home (Personal Accident GROUP_PA or Employee Benefits GPA)?",
    "Q13 MIS business lines: confirm the mapping of the 15 LOBs to the MIS lines (FIRE, MOTOR, MARINE, ENGG, CASUALTY, PA, HEALTH, BONDS).",
    "Q14 Inactive programmes (e.g. 2021 / 2023 CAR programmes): migrate as history-only or retire?",
]
