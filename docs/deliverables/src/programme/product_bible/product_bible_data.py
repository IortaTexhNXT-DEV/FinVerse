"""Content of the BDOI Product Bible (LOB library) workbook.

Source tags used in the rows:
  BDOI-FRS   FRS - BDOI BROKERSYS Product Maintenance v1.0 (BDOI, 19-Aug-2026), product line tables
  BDOI-NB    New Business (NB) BRD 05132026, Appendix A computation guide and Appendix B product matrix
  BDOI-DM    BDO Insure Core Modernization - Data Migration - BRD V0.03 (BRD-13)
  BDOI-PM    Product Maintenance.pdf (BRD-03)
  BIBS-CAT   BIBS New Business product catalogue configuration (product lines, cover types, risk products,
             coverages / perils, clauses, package versions, Employee Benefits lines)
  OOTB       iNXT BrokerVerse OOTB platform reference data (branch brokerverse-platform, read 12-Oct-2026)
  MKT        Market practice (to confirm with BDOI)
"""

from __future__ import annotations

MKT = "Market practice (to confirm with BDOI)"
SRC = {
    "BDOI-FRS": "BDOI FRS Product Maintenance v1.0 (19-Aug-2026)",
    "BDOI-NB": "BDOI New Business BRD (13-May-2026), Appendix A/B",
    "BDOI-DM": "BDOI Data Migration BRD V0.03 (BRD-13)",
    "BDOI-PM": "BDOI Product Maintenance BRD (BRD-03)",
    "BIBS-CAT": "BIBS New Business catalogue configuration",
    "OOTB": "iNXT BrokerVerse OOTB reference data",
    "MKT": MKT,
}

# ---------------------------------------------------------------------------------------------------------------
# BDOI's 15 lines of business. code = proposed BDOI LOB code (to confirm); frs = name in BDOI's FRS line list;
# bibs = BIBS product line codes; ootb = OOTB line / product codes.
LOBS = [
    dict(code="PRO", name="Property", ic="Fire (IC line: Fire and allied perils)",
         desc="Buildings, contents, machinery, stocks and improvements against fire and allied perils, or all risks; "
              "includes industrial all risks, householders and business interruption extensions.",
         seg="Corporate and SME premises; retail homeowners; bank-related mortgage redemption fire on "
             "real-estate loans (BDOI Fire packages PAR01-PAR25 are mortgaged-risk packages per NB BRD Appendix B).",
         compulsory="No by law; required by the bank as loan collateral cover (mortgagee clause)",
         frs="Property", bibs=["PROPERTY"], ootb=["FIRE", "IAR", "HOME"],
         ref="NB BRD 2.3.1 and Appendix A (Fire), Appendix B (PAR packages); FRS PM v1.0 FRPM.003.02; BRD-03 FRS v1.3"),
    dict(code="BND", name="Bond", ic="Suretyship (IC line: Surety)",
         desc="Surety bonds guaranteeing the principal's performance of an obligation to an obligee: contract "
              "(bid, performance, payment, warranty), judicial, heirs, customs and government bonds.",
         seg="Contractors and suppliers of BDO corporate / SME clients; litigants (judicial); estates (heirs bond).",
         compulsory="Required by the obligee (e.g. government procurement, courts); not compulsory by law",
         frs="Bonds", bibs=[], ootb=["BOND"],
         ref="FRS PM v1.0 product line 'Bonds' (Heirs, Performance, Surety, Bidder's, Judicial, Warranty); "
             "transaction types NB / Endorsement only"),
    dict(code="PAC", name="Personal Accident", ic="Casualty (Accident)",
         desc="Accidental death, disablement and medical reimbursement for individuals and groups, including "
              "travel and marine PA.",
         seg="Retail bank clients (PA programme for BDOI clients), employees (group PA), students, travellers.",
         compulsory="No (travel PA may be required for some visas; confirm)",
         frs="Personal Accident", bibs=["PERSONAL_ACCIDENT", "GPA"], ootb=["PA", "GPA", "TRAVEL", "MICRO"],
         ref="FRS PM v1.0 sub-line values Personal Accident, Travel; BIBS PA001"),
    dict(code="MTR", name="Motor", ic="Motor Car",
         desc="Private and commercial vehicles: Compulsory Third Party Liability (CTPL), own damage and theft, "
              "excess bodily injury / property damage, acts of nature, auto PA.",
         seg="Retail car owners; bank auto-loan clients (BDOI Motor packages MTR08-MTR38); corporate fleets.",
         compulsory="Yes - CTPL is compulsory for LTO registration (Insurance Code ch. VI)",
         frs="Motor", bibs=["MOTOR"], ootb=["MOTOR", "CTPL"],
         ref="NB BRD Appendix A (Motor computation), Appendix B (MTR packages); FRS PM v1.0 vehicle fields"),
    dict(code="HLT", name="Health Insurance", ic="Casualty (Health) / HMO (regulated by IC under RA 11223)",
         desc="Group and individual health and HMO programmes: hospitalisation, out-patient, emergency, "
              "dental; for a broker usually corporate employee benefit programmes.",
         seg="Corporate employee benefits; SME groups; individual clients (confirm whether BDOI places them).",
         compulsory="No",
         frs="(not in FRS line list; Employee Benefits BRD)", bibs=["HMO"], ootb=["EB", "HEALTH"],
         ref="Employee Benefits.pdf; BIBS Employee Benefits lines HMO / GLI / GPA"),
    dict(code="LIA", name="Liability", ic="Casualty (Liability)",
         desc="Legal liability to third parties: general, products, professional, D&O, employer's, cyber, "
              "pollution and marine / excess auto liability.",
         seg="Corporate and SME clients, mall tenants and service contractors (BDOI CGL programmes).",
         compulsory="No by law; often required by lessors, malls, LGU business permits and contracts",
         frs="Liability", bibs=["LIABILITY"], ootb=["CGL"],
         ref="FRS PM v1.0 sub-line values (11 liability products); BIBS CGL01-CGL09"),
    dict(code="ENG", name="Engineering", ic="Casualty (Engineering)",
         desc="Construction and erection risks and operational machinery: CAR, EAR, machinery breakdown, "
              "contractor's plant, boiler and pressure vessel, delay in start-up.",
         seg="Contractors and project owners financed by BDO; manufacturing and utilities.",
         compulsory="No; often a loan / contract condition",
         frs="Engineering", bibs=["ENGINEERING"], ootb=["CAR", "EAR", "MB"],
         ref="FRS PM v1.0 sub-line values CARI, EAR; BIBS CAR00-CAR29, CARI; NB / Endorsement only"),
    dict(code="EEI", name="Electronic Equipment", ic="Casualty (Engineering)",
         desc="All-risks cover on electronic equipment (computers, medical, telecom), external data media and "
              "increased cost of working.",
         seg="Hospitals and clinics, BPOs, banks and offices, leasing companies.",
         compulsory="No", frs="EEI", bibs=["ELECTRONIC_EQUIPMENT"], ootb=["EEI (risk section of IAR)"],
         ref="FRS PM v1.0 line EEI; BIBS EEI01, EEI03, EEI04"),
    dict(code="MSC", name="Others / Miscellaneous", ic="Casualty (Miscellaneous)",
         desc="Specialty covers not in another line: golfer's / hole-in-one, sabotage and terrorism, political "
              "violence, trade credit, parametric, fine arts, jewellers block, event and cyber (if placed here).",
         seg="Corporate specialty risks; golf clubs and members; collectors.",
         compulsory="No", frs="Miscellaneous", bibs=["OTHERS"], ootb=[],
         ref="FRS PM v1.0 sub-line values (Golfer's, S&T, PV, Trade Credit, Parametric, Fine Arts)"),
    dict(code="MAR", name="Marine (Cargo)", ic="Marine (Cargo)",
         desc="Goods in transit by sea, air and land: import, export, inter-island and inland transit, open "
              "covers, trust receipt programmes, stock throughput and logistics liabilities.",
         seg="Importers / exporters with BDO trade finance (trust receipts, letters of credit); distributors.",
         compulsory="No; required under L/C and trust receipt terms",
         frs="Marine Cargo (and Stock Throughput)", bibs=["MARINE"], ootb=["MARINE"],
         ref="FRS PM v1.0 lines Marine Cargo, Stock Throughput; BIBS INL / MOP / MON / TRP products"),
    dict(code="CRM", name="Crime", ic="Casualty (Crime / Fidelity)",
         desc="Money, securities and payroll; fidelity guarantee; comprehensive crime (3D); bankers blanket "
              "bond; kidnap and ransom.",
         seg="Corporate and SME clients handling cash; financial institutions.",
         compulsory="No", frs="Crime", bibs=["CRIME"], ootb=["MONEY"],
         ref="FRS PM v1.0 sub-line values (FG, MSP, K&R, BBB, FG+MSP, 3D)"),
    dict(code="HUL", name="Marine Hull", ic="Marine (Hull)",
         desc="Vessels: hull and machinery, protection and indemnity, builder's risk, ship repairer's "
              "liability.",
         seg="Shipping lines, inter-island operators, fishing fleets, shipyards.",
         compulsory="No by law for hull; P&I / passenger liability may be required by MARINA (confirm)",
         frs="Marine Hull", bibs=["MARINE_HULL"], ootb=["HULL"],
         ref="FRS PM v1.0 vessel fields; BIBS cover types H&M, Ship repairers, Builder's risk, P&I"),
    dict(code="FLT", name="Property / Equipment Floater", ic="Casualty (Engineering / Floater)",
         desc="Mobile or leased equipment and property covered wherever located within the territory (equipment "
              "floater, contractor's equipment on lease).",
         seg="Leasing companies (e.g. BDO Leasing), contractors with mobile plant, remedial property programmes.",
         compulsory="No; lessor / lender requirement", frs="Floater", bibs=["FLOATER"], ootb=[],
         ref="FRS PM v1.0 line Floater; BIBS EPF02, EPF04, EPF05"),
    dict(code="AVN", name="Aviation", ic="Aviation",
         desc="Aircraft hull and liability, drones, airport owners / operators, hangar keepers, aviation "
              "products, refuellers and pilot loss of licence.",
         seg="General aviation operators, flying schools, drone operators, airport concessionaires.",
         compulsory="Third party / passenger liability required by CAAP for operators (confirm)",
         frs="Aviation", bibs=["AVIATION"], ootb=[],
         ref="FRS PM v1.0 aircraft fields; BIBS 8 aviation cover types"),
    dict(code="CAS", name="Casualty", ic="Casualty (Miscellaneous casualty)",
         desc="Other casualty covers: burglary and robbery, plate glass, all risks on personal items, "
              "comprehensive personal liability (where BDOI files it here) and similar.",
         seg="SME shops and offices; retail households.",
         compulsory="No", frs="(not in FRS line list)", bibs=[], ootb=["BURGLARY"],
         ref="BDOI LOB list only; no BDOI product list found - confirm scope with BDOI"),
]
LOB_NAMES = [x["name"] for x in LOBS]

# BIBS product line -> BDOI LOB
BIBS_LINE_LOB = {
    "PROPERTY": "Property", "MOTOR": "Motor", "ENGINEERING": "Engineering", "LIABILITY": "Liability",
    "CRIME": "Crime", "FLOATER": "Property / Equipment Floater", "MARINE": "Marine (Cargo)",
    "MARINE_HULL": "Marine Hull", "AVIATION": "Aviation", "PERSONAL_ACCIDENT": "Personal Accident",
    "ELECTRONIC_EQUIPMENT": "Electronic Equipment", "OTHERS": "Others / Miscellaneous",
    "HMO": "Health Insurance", "GLI": "Health Insurance", "GPA": "Personal Accident",
}

# BIBS Employee Benefits lines and products (Employee Benefits catalogue seed)
BIBS_EB_LINES = [("HMO", "Health Maintenance (HMO)"), ("GLI", "Group Life Insurance"),
                 ("GPA", "Group Personal Accident")]
BIBS_EB_PRODUCTS = [("EBHMO01", "Group HMO - corporate programme", "HMO"),
                    ("EBGLI01", "Group Life Insurance - corporate programme", "GLI"),
                    ("EBGPA01", "Group Personal Accident - corporate programme", "GPA")]
# Business-line lookup values (chart of accounts / MIS) that are not product lines
BIBS_LOOKUPS = [("BUSINESS_LINE", "FIRE", "Fire and Allied Perils"), ("BUSINESS_LINE", "MOTOR", "Motor Car"),
                ("BUSINESS_LINE", "MARINE", "Marine Cargo and Hull"), ("BUSINESS_LINE", "ENGG", "Engineering"),
                ("BUSINESS_LINE", "CASUALTY", "Casualty and Liability"),
                ("BUSINESS_LINE", "PA", "Personal Accident"), ("BUSINESS_LINE", "HEALTH", "Health"),
                ("BUSINESS_LINE", "BONDS", "Surety Bonds")]
# Package versions with coverage terms (package version seeds)
BIBS_PACKAGES = [
    ("MTR12", "Version 2 (Draft)", [("OD_THEFT", "Included", "Participation fee PHP 2,000 per claim"),
                                    ("EXCESS_BI", "Included", "Limit PHP 250,000"),
                                    ("PD", "Included", "Limit PHP 250,000"),
                                    ("AON", "Included", "Deductible 1% of the sum insured"),
                                    ("AUTO_PA", "Optional", "Limit PHP 50,000")]),
    ("PAR08", "Version 2 (Released, from 01-Sep-2026)", [("FIRE_LIGHTNING", "Included", "n/a"),
                                                         ("EARTHQUAKE", "Included", "2% of value at risk"),
                                                         ("TYPHOON_FLOOD", "Included", "1% of loss, minimum PHP 20,000"),
                                                         ("RSMD", "Included", "n/a")]),
]

# ---------------------------------------------------------------------------------------------------------------
# OOTB product set (iNXT BrokerVerse platform reference data, read-only). (lob, level, code, name, parent, detail)
OOTB_ROWS = [
    # line-of-business master
    ("Motor", "Line of business", "MOTOR", "Motor", "-", "Line of business master"),
    ("Property", "Line of business", "FIRE", "Fire", "-", "Line of business master"),
    ("Marine (Cargo)", "Line of business", "MARINE", "Marine", "-", "Line of business master (cargo and hull)"),
    ("Casualty", "Line of business", "CASUALTY", "Casualty", "-", "Line of business master (CGL, bond, money, burglary)"),
    ("Personal Accident", "Line of business", "ACCIDENT", "Accident", "-", "Line of business master"),
    ("Health Insurance", "Line of business", "EB", "Employee Benefits", "-", "Line of business master"),
    ("Engineering", "Line of business", "ENGINEERING", "Engineering", "-", "Line of business master"),
    # product categories
    ("Personal Accident", "Product category", "TRAVEL", "Travel", "-", "Product category master"),
    ("Health Insurance", "Product category", "HEALTH", "Health", "-", "Product category master"),
    ("Health Insurance", "Product category", "LIFE", "Life", "-", "Product category master (group life)"),
    # products
    ("Motor", "Product", "MOTOR", "Motor Vehicle Insurance", "MOTOR", "Package, retail"),
    ("Motor", "Product", "CTPL", "Compulsory Third Party Liability", "MOTOR", "Package, retail"),
    ("Property", "Product", "FIRE", "Fire and Allied Perils", "FIRE", "Non-package, corporate"),
    ("Property", "Product", "IAR", "Industrial All Risks", "FIRE", "Non-package, corporate"),
    ("Property", "Product", "HOME", "Householder Insurance", "FIRE", "Package, retail"),
    ("Marine (Cargo)", "Product", "MARINE", "Marine Cargo", "MARINE", "Non-package, corporate"),
    ("Marine Hull", "Product", "HULL", "Marine Hull", "MARINE", "Non-package, corporate"),
    ("Personal Accident", "Product", "PA", "Personal Accident", "ACCIDENT", "Package, retail"),
    ("Personal Accident", "Product", "TRAVEL", "Travel Insurance", "ACCIDENT", "Package, retail"),
    ("Personal Accident", "Product", "MICRO", "Micro-insurance", "ACCIDENT", "Package, retail"),
    ("Personal Accident", "Product", "GPA", "Group Personal Accident", "ACCIDENT", "Package, retail and corporate"),
    ("Liability", "Product", "CGL", "Comprehensive General Liability", "CASUALTY", "Non-package, corporate"),
    ("Bond", "Product", "BOND", "Surety Bond", "CASUALTY", "Non-package, corporate"),
    ("Crime", "Product", "MONEY", "Money and Securities", "CASUALTY", "Non-package, corporate"),
    ("Casualty", "Product", "BURGLARY", "Burglary and Robbery", "CASUALTY", "Package, retail and corporate"),
    ("Engineering", "Product", "CAR", "Contractor's All Risks", "ENGINEERING", "Non-package, corporate"),
    ("Engineering", "Product", "EAR", "Erection All Risks", "ENGINEERING", "Non-package, corporate"),
    ("Engineering", "Product", "MB", "Machinery Breakdown", "ENGINEERING", "Non-package, corporate"),
    ("Health Insurance", "Product", "EB", "Group Employee Benefits", "EB", "Non-package, corporate"),
    # policy types
    ("Motor", "Policy type", "COMP", "Comprehensive", "MOTOR", "Policy type of product MOTOR"),
    ("Motor", "Policy type", "TPL", "Third Party Liability", "MOTOR", "Policy type of product MOTOR"),
    ("Motor", "Policy type", "ODTH", "Own Damage / Theft", "MOTOR", "Policy type of product MOTOR"),
    ("Motor", "Policy type", "CTPL", "CTPL", "CTPL", "Policy type of product CTPL"),
    ("Property", "Policy type", "FIRE-RES", "Residential", "FIRE", "Policy type of product FIRE"),
    ("Property", "Policy type", "FIRE-COM", "Commercial", "FIRE", "Policy type of product FIRE"),
    ("Marine (Cargo)", "Policy type", "MC-IMP", "Import", "MARINE", "Policy type of product MARINE"),
    ("Marine (Cargo)", "Policy type", "MC-DOM", "Domestic", "MARINE", "Policy type of product MARINE"),
    ("Personal Accident", "Policy type", "PA-IND", "Individual", "PA", "Policy type of product PA"),
    ("Personal Accident", "Policy type", "PA-GRP", "Group", "PA", "Policy type of product PA"),
    ("Liability", "Policy type", "CGL-STD", "Standard", "CGL", "Policy type of product CGL"),
    ("Health Insurance", "Policy type", "EB-GRP", "Group Life & Health", "EB", "Policy type of product EB"),
    ("Bond", "Policy type", "BOND-PERF", "Performance Bond", "BOND", "Policy type of product BOND"),
    # product templates (Product Configurator)
    ("Motor", "Product template", "MOT-003-2025", "Motor Insurance Basic Plan", "MOTOR", "Active; taxes DST 12.5, LGT 0.75, VAT 12; CTPL tariff settings"),
    ("Motor", "Product template", "MOT-001-2026", "First Class Motor Insurance", "MOTOR", "Active"),
    ("Motor", "Product template", "MOTOR-COMP-2026", "Motor Comprehensive Insurance", "MOTOR", "Active, v2.1; covers CTPL (mandatory), AOG (optional)"),
    ("Motor", "Product template", "CTPL-2024", "CTPL Standard 2024", "CTPL", "Retired (superseded)"),
    ("Property", "Product template", "PROP-FIRE-2026", "Property Fire Insurance", "FIRE", "Active, v3.0; construction type rating factor"),
    ("Marine (Cargo)", "Product template", "MARINE-CARGO-2026", "Marine Cargo Insurance", "MARINE", "Active, v2.3"),
    ("Personal Accident", "Product template", "1001", "Travel", "PA", "Active"),
    ("Personal Accident", "Product template", "TRAVEL-INT-2026", "International Travel Insurance", "PA", "Active, v1.8"),
    ("Personal Accident", "Product template", "EB-ACCIDENT-2026", "Group Personal Accident", "EB", "Active, v1.8"),
    ("Health Insurance", "Product template", "HEALTH-GROUP-2026", "Group Health Insurance", "EB", "Active, v1.5; cover HOSP"),
    ("Health Insurance", "Product template", "EB-HEALTH-2026", "Employee Health & Medical", "EB", "Active, v2.5"),
    ("Health Insurance", "Product template", "EB-LIFE-2026", "Group Life Insurance", "EB", "Active, v2.0 (life - outside BDOI's 15 non-life LOBs)"),
    ("Health Insurance", "Product template", "EB-RETIREMENT-2026", "Group Retirement Plan", "EB", "Draft (outside BDOI's 15 LOBs)"),
    # cover master and template coverages
    ("Motor", "Cover", "OD", "Own Damage", "MOTOR", "Cover master; mandatory on MOT-003-2025"),
    ("Motor", "Cover", "THEFT", "Theft", "MOTOR", "Cover master; mandatory on MOT-003-2025"),
    ("Motor", "Cover", "AOG", "Acts of Nature", "MOTOR", "Cover master; optional"),
    ("Motor", "Cover", "CTPL", "Compulsory Third Party Liability", "MOTOR", "Cover master"),
    ("Motor", "Cover", "VTPL-BI", "Excess Bodily Injury", "MOTOR", "Cover master; optional"),
    ("Motor", "Cover", "VTPL-PD", "Property Damage", "MOTOR", "Cover master; optional"),
    ("Motor", "Cover", "APA", "Auto Personal Accident", "MOTOR", "Cover master; optional"),
    ("Motor", "Cover", "RSA", "Roadside Assistance", "MOT-003-2025", "Template cover; optional (+0.25%)"),
    ("Motor", "Cover", "PAC", "Personal Accident of the Driver", "MOT-003-2025", "Template cover; optional (+2%)"),
    ("Property", "Cover", "FLEXA", "Fire and Lightning", "FIRE", "Cover master"),
    ("Property", "Cover", "EQ", "Earthquake Shock", "FIRE", "Cover master"),
    ("Property", "Cover", "RSMD", "Riot, Strike, Malicious Damage", "FIRE", "Cover master"),
    ("Health Insurance", "Cover", "HOSP", "Hospitalization Benefit", "HEALTH-GROUP-2026", "Template cover; mandatory"),
    # IAR risk sections
    ("Property", "Risk section", "FIRE", "Fire Material Damage", "IAR", "IAR risk section; default rate 0.15%"),
    ("Property", "Risk section", "BI", "Business Interruption", "IAR", "IAR risk section"),
    ("Engineering", "Risk section", "MB", "Machinery Breakdown", "IAR", "IAR risk section"),
    ("Electronic Equipment", "Risk section", "EEI", "Electronic Equipment", "IAR", "IAR risk section; default rate 0.28%"),
    ("Liability", "Risk section", "CGL", "General Liability", "IAR", "IAR risk section"),
    ("Motor", "Risk section", "MOTOR", "Motor Commercial", "IAR", "IAR risk section; default rate 0.35%"),
    ("Motor", "Risk section", "CTPL", "Compulsory Third Party Liability", "IAR", "IAR risk section"),
    # premium taxes (taxation master)
    ("All lines", "Tax / charge", "DST", "Documentary Stamp Tax", "-", "12.5% of premium"),
    ("All lines", "Tax / charge", "VAT", "Value Added Tax", "-", "12% (master basis: commission; template basis: premium)"),
    ("All lines", "Tax / charge", "PT", "Premium Tax", "-", "2% of gross premium"),
    ("All lines", "Tax / charge", "LGT", "Local Government Tax", "-", "0.75% of premium (default)"),
    ("Property", "Tax / charge", "FST", "Fire Service Tax", "-", "2% of premium"),
    ("All lines", "Tax / charge", "EWT", "Expanded Withholding Tax on Commission", "-", "5% of commission"),
]

# ---------------------------------------------------------------------------------------------------------------
# Comparison: one row per BDOI LOB and market / BDOI product.
# (lob, product, ootb code(s) or '', bibs code(s) or '', product source, remark / action for BDOI)
COMPARISON = [
    ("Property", "Fire and lightning (standard fire policy)", "FIRE, FLEXA", "PROPERTY / FIRE_LIGHTNING (PAR01, PAR08, PAR13, PAR25)", "BDOI-FRS", "Map legacy Fire risk codes; load PAR product matrix (Q01)"),
    ("Property", "Fire and allied perils / broad named perils", "FIRE, EQ, RSMD", "PROPERTY / BROAD_NAMED_PERILS (PAR19); perils EARTHQUAKE, TYPHOON_FLOOD, EXTENDED_COVERAGE, RSMD", "BDOI-FRS", "Map legacy allied-peril codes to BIBS perils"),
    ("Property", "Property all risks", "", "PROPERTY / ALL_RISK (PAR09)", "BDOI-FRS", "Confirm legacy all-risk products"),
    ("Property", "Industrial all risks (IAR)", "IAR", "", "MKT", "Confirm whether BDOI places IAR separately or as Property all risk"),
    ("Property", "Business interruption / consequential loss", "BI (IAR risk section)", "", "MKT", "Confirm if written; add as peril or cover type if yes"),
    ("Property", "Householders / homeowners package", "HOME", "", "MKT", "Confirm retail home package; else map to PROPERTY"),
    ("Bond", "Surety bond (general)", "BOND", "", "BDOI-FRS", "Add product line BOND (only a business-line lookup today)"),
    ("Bond", "Performance bond", "BOND-PERF", "", "BDOI-FRS", "Add product"),
    ("Bond", "Bidder's bond", "", "", "BDOI-FRS", "Add product"),
    ("Bond", "Warranty bond", "", "", "BDOI-FRS", "Add product"),
    ("Bond", "Judicial bond (court bonds)", "", "", "BDOI-FRS", "Add product"),
    ("Bond", "Heirs bond", "", "", "BDOI-FRS", "Add product"),
    ("Bond", "Payment / advance payment / JCL (G(13), G(16)) bonds", "", "", "MKT", "Confirm if BDOI places government JCL bonds"),
    ("Bond", "Customs bonds", "", "", "MKT", "Confirm"),
    ("Personal Accident", "Individual PA", "PA, PA-IND", "PERSONAL_ACCIDENT / INDIVIDUAL_PA (PA001)", "BDOI-FRS", "Map legacy PA codes"),
    ("Personal Accident", "Group PA", "GPA, PA-GRP, EB-ACCIDENT-2026", "PERSONAL_ACCIDENT / GROUP_PA; GPA / EBGPA01", "BIBS-CAT", "Decide one home for group PA (non-life line or Employee Benefits)"),
    ("Personal Accident", "Travel PA", "TRAVEL, 1001, TRAVEL-INT-2026", "PERSONAL_ACCIDENT / TRAVEL_PA", "BDOI-FRS", "Map legacy travel codes"),
    ("Personal Accident", "Marine PA (crew / passengers)", "", "PERSONAL_ACCIDENT / MARINE_PA", "BIBS-CAT", "Confirm volume"),
    ("Personal Accident", "Student PA", "", "", "MKT", "Confirm; can be a GROUP_PA package"),
    ("Personal Accident", "Micro-insurance PA", "MICRO", "", "MKT", "Confirm if BDOI sells micro products"),
    ("Motor", "Compulsory Third Party Liability (CTPL)", "CTPL, CTPL-2024", "MOTOR / TPL_ONLY (CTP01, CTP02 non-VAT)", "BDOI-NB", "Map legacy CTPL codes; confirm VAT / non-VAT split"),
    ("Motor", "Comprehensive (OD / theft + excess BI / PD + AON)", "MOTOR, COMP, MOTOR-COMP-2026, MOT-003-2025", "MOTOR / COMPREHENSIVE (MTR08-MTR38); coverages OD_THEFT, EXCESS_BI, PD", "BDOI-NB", "Load MTR product matrix (Q01)"),
    ("Motor", "Own damage / theft only", "ODTH, OD, THEFT", "coverage OD_THEFT only (no standalone cover type)", "MKT", "Confirm whether OD-only policies exist in legacy"),
    ("Motor", "Excess bodily injury / property damage (VTPL) standalone", "TPL, VTPL-BI, VTPL-PD", "coverages EXCESS_BI, PD only", "BDOI-NB", "Confirm standalone VTPL policies"),
    ("Motor", "Acts of nature", "AOG", "coverage AON", "BDOI-NB", "Covered as coverage"),
    ("Motor", "Auto PA", "APA, PAC", "coverage AUTO_PA", "BDOI-NB", "Covered as coverage"),
    ("Motor", "Riot, strike and civil commotion / political violence", "", "coverage RSCC", "BDOI-NB", "NB BRD rates OD / PV together; confirm PV code"),
    ("Motor", "Roadside assistance", "RSA", "", "MKT", "Confirm if a BDOI add-on"),
    ("Health Insurance", "Group HMO / health programme", "EB, HEALTH-GROUP-2026, EB-HEALTH-2026, HOSP", "HMO / EBHMO01", "BIBS-CAT", "Map legacy EB plan codes"),
    ("Health Insurance", "Individual / family health", "", "", "MKT", "Confirm whether BDOI places individual health"),
    ("Health Insurance", "Group life (EB)", "EB-LIFE-2026", "GLI / EBGLI01", "BIBS-CAT", "Life - outside the 15 non-life LOBs; confirm reporting line"),
    ("Liability", "Comprehensive general liability (CGL)", "CGL, CGL-STD", "LIABILITY / CGL (CGL01-CGL09)", "BDOI-FRS", "Map legacy CGL programmes"),
    ("Liability", "Comprehensive personal liability", "", "LIABILITY / CPL", "BIBS-CAT", "Confirm"),
    ("Liability", "Employer's liability", "", "LIABILITY / EMPLOYERS", "BDOI-FRS", "Map"),
    ("Liability", "Products liability", "", "LIABILITY / PRODUCTS", "BDOI-FRS", "Map"),
    ("Liability", "Professional liability / indemnity", "", "LIABILITY / PROFESSIONAL", "BDOI-FRS", "Map"),
    ("Liability", "Medical malpractice", "", "", "BDOI-FRS", "Add cover type or map to PROFESSIONAL"),
    ("Liability", "Directors and officers (D&O)", "", "LIABILITY / DNO", "BDOI-FRS", "Map"),
    ("Liability", "Errors and omissions (E&O)", "", "LIABILITY / ENO", "BDOI-FRS", "Map"),
    ("Liability", "Cyber liability", "", "LIABILITY / CYBER", "BDOI-FRS", "Map"),
    ("Liability", "Pollution liability (incl. contractor's pollution)", "", "LIABILITY / POLLUTION_LEGAL, CONTRACTORS_POLLUTION", "BDOI-FRS", "Map"),
    ("Liability", "Contaminated products", "", "LIABILITY / CONTAMINATED_PRODUCTS", "BIBS-CAT", "Confirm"),
    ("Liability", "Marine liability", "", "", "BDOI-FRS", "Add cover type or map to MARINE liabilities"),
    ("Liability", "Excess auto liability", "", "", "BDOI-FRS", "Add cover type"),
    ("Engineering", "Contractor's all risks (CAR / CARI)", "CAR", "ENGINEERING / CAR (CAR00-CAR29, CARI)", "BDOI-FRS", "Map legacy CAR programmes"),
    ("Engineering", "Erection all risks (EAR)", "EAR", "ENGINEERING / EAR (CAR26)", "BDOI-FRS", "Map"),
    ("Engineering", "Machinery breakdown (MBD)", "MB", "", "MKT", "Confirm; add cover type if written"),
    ("Engineering", "Contractor's plant and machinery (CPM)", "", "", "MKT", "Confirm (may sit under Floater)"),
    ("Engineering", "Boiler and pressure vessel", "", "", "MKT", "Confirm"),
    ("Engineering", "Delay in start-up / advance loss of profits (DSU / ALOP)", "", "", "MKT", "Confirm"),
    ("Electronic Equipment", "Electronic equipment insurance (EEI)", "EEI (IAR risk section)", "ELECTRONIC_EQUIPMENT / EEI (EEI01, EEI03, EEI04)", "BDOI-FRS", "Map legacy EEI codes"),
    ("Others / Miscellaneous", "Golfer's / hole-in-one", "", "OTHERS / GOLFERS (GIP01)", "BDOI-FRS", "Map"),
    ("Others / Miscellaneous", "Sabotage and terrorism", "", "OTHERS / S_AND_T", "BDOI-FRS", "Map"),
    ("Others / Miscellaneous", "Political violence", "", "OTHERS / PV", "BDOI-FRS", "Map"),
    ("Others / Miscellaneous", "Trade credit", "", "OTHERS / TRADE_CREDIT", "BDOI-FRS", "Map"),
    ("Others / Miscellaneous", "Parametric", "", "OTHERS / PARAMETRIC", "BDOI-FRS", "Map"),
    ("Others / Miscellaneous", "Fine arts", "", "OTHERS / FINE_ARTS", "BDOI-FRS", "Map"),
    ("Others / Miscellaneous", "Jewellers block", "", "OTHERS / JEWELERS_BLOCK", "BIBS-CAT", "Confirm"),
    ("Others / Miscellaneous", "Event cancellation / event liability", "", "", "MKT", "Confirm"),
    ("Marine (Cargo)", "Inland transit / inland marine", "MC-DOM", "MARINE / INLAND (INL01-INL06)", "BIBS-CAT", "Map"),
    ("Marine (Cargo)", "Inter-island", "MC-DOM", "MARINE / INTER_ISLAND", "BIBS-CAT", "Map"),
    ("Marine (Cargo)", "Import (single shipment / open cover)", "MC-IMP, MARINE-CARGO-2026", "MARINE / IMPORT; COMBINED (MOP02-MOP14, MON05)", "BDOI-FRS", "Map legacy MOP codes"),
    ("Marine (Cargo)", "Export", "", "MARINE / EXPORT", "BIBS-CAT", "Confirm zero-rated VAT handling"),
    ("Marine (Cargo)", "Trust receipt programme", "", "MARINE / TRUST_RECEIPT (TRP01-TRP03)", "BIBS-CAT", "Map"),
    ("Marine (Cargo)", "Stock throughput", "", "MARINE / STOCK_THROUGHPUT", "BDOI-FRS", "BDOI FRS treats Stock Throughput as its own line - confirm LOB"),
    ("Marine (Cargo)", "Project cargo", "", "MARINE / PROJECT_CARGO", "BIBS-CAT", "Confirm"),
    ("Marine (Cargo)", "Charterer's / terminal operator's / freight forwarder's liability", "", "MARINE / CHARTERERS_LIABILITY, TERMINAL_OPERATORS, FREIGHT_FORWARDERS", "BIBS-CAT", "Confirm LOB (Marine vs Liability)"),
    ("Crime", "Money, securities and payroll (MSP)", "MONEY", "CRIME / MSP", "BDOI-FRS", "Map"),
    ("Crime", "Fidelity guarantee (FG)", "", "CRIME / FG", "BDOI-FRS", "Map"),
    ("Crime", "Combined FG and MSP", "", "CRIME / MSP_FG", "BDOI-FRS", "Map"),
    ("Crime", "Dishonesty, disappearance and destruction (3D)", "", "CRIME / DDD", "BDOI-FRS", "Map"),
    ("Crime", "Bankers blanket bond", "", "CRIME / BBB", "BDOI-FRS", "Map"),
    ("Crime", "Kidnap and ransom", "", "", "BDOI-FRS", "Add cover type"),
    ("Marine Hull", "Hull and machinery", "HULL", "MARINE_HULL / HULL_MACHINERY", "BDOI-FRS", "Map"),
    ("Marine Hull", "Protection and indemnity (P&I)", "HULL", "MARINE_HULL / P_AND_I", "BIBS-CAT", "Map"),
    ("Marine Hull", "Builder's risk", "", "MARINE_HULL / BUILDERS_RISK", "BIBS-CAT", "Map"),
    ("Marine Hull", "Ship repairer's liability", "", "MARINE_HULL / SHIP_REPAIRERS", "BIBS-CAT", "Map"),
    ("Property / Equipment Floater", "Equipment / property floater", "", "FLOATER / FLOATER (EPF02, EPF04, EPF05)", "BDOI-FRS", "Map legacy EPF codes"),
    ("Aviation", "Aircraft hull and liability (general aviation)", "", "AVIATION / GENERAL_AVIATION", "BDOI-FRS", "Map"),
    ("Aviation", "Drone", "", "AVIATION / DRONE", "BIBS-CAT", "Map"),
    ("Aviation", "Airport owner's / operator's and contractor's liability", "", "AVIATION / AIRPORT_OWNERS, AIRPORT_CONTRACTORS", "BIBS-CAT", "Map"),
    ("Aviation", "Aviation products / refuellers / hangar keepers liability", "", "AVIATION / AVIATION_PRODUCTS, AVIATION_REFUELLERS, HANGAR_KEEPERS", "BIBS-CAT", "Map"),
    ("Aviation", "Pilot loss of licence", "", "AVIATION / PILOT_LOSS_LICENSE", "BIBS-CAT", "Map"),
    ("Casualty", "Burglary and robbery", "BURGLARY", "", "MKT", "Confirm Casualty scope; add product line if written"),
    ("Casualty", "Plate glass", "", "", "MKT", "Confirm"),
    ("Casualty", "All risks on personal property (gadgets, jewellery)", "", "", "MKT", "Confirm"),
]

# LOB-level status (OOTB, BIBS) with the reason
LOB_STATUS = {
    "Property": ("Partial", "Fire, IAR, home; no all-risk or broad named perils as products", "Partial", "Three cover types and five perils; no BI or householder product"),
    "Bond": ("Partial", "One product BOND with policy type Performance", "Missing", "Business-line lookup 'BONDS - Surety Bonds' only; no product line or product"),
    "Personal Accident": ("Covered", "PA, GPA, travel, micro", "Covered", "Four cover types plus Employee Benefits GPA"),
    "Motor": ("Covered", "Comprehensive, TPL, OD/theft, CTPL and all motor covers", "Partial", "TPL only and Comprehensive; OD, AON, VTPL, auto PA as coverages, not standalone"),
    "Health Insurance": ("Partial", "Group health templates; no individual health", "Partial", "Employee Benefits line HMO (EBHMO01); no individual health"),
    "Liability": ("Partial", "CGL only (plus CGL risk section)", "Covered", "11 cover types, 7 CGL risk products; medical malpractice, marine and excess auto liability missing"),
    "Engineering": ("Partial", "CAR, EAR, machinery breakdown", "Partial", "CAR and EAR only; no MBD, CPM, boiler, DSU"),
    "Electronic Equipment": ("Partial", "EEI as a risk section of IAR only", "Covered", "Line and cover type EEI with three products"),
    "Others / Miscellaneous": ("Missing", "No product", "Covered", "Seven cover types, one product (GIP01)"),
    "Marine (Cargo)": ("Partial", "Marine cargo import / domestic", "Covered", "11 cover types, 21 risk products"),
    "Crime": ("Partial", "Money and securities only", "Covered", "Five cover types, no risk product yet; kidnap and ransom missing"),
    "Marine Hull": ("Partial", "One product HULL", "Covered", "Four cover types, no risk product yet"),
    "Property / Equipment Floater": ("Missing", "No product", "Covered", "Line and cover type FLOATER with three products"),
    "Aviation": ("Missing", "No product (upload template example only)", "Covered", "Eight cover types; no product yet"),
    "Casualty": ("Partial", "Burglary and robbery", "Missing", "Business-line lookup 'CASUALTY - Casualty and Liability' only"),
}
