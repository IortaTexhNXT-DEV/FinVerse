"""Text of the iNXT BrokerVerse User Manual, edition 2.0 (screen by screen).

Every screen section is assembled from two sources:
  * screens/inventory.json: what the screen showed when it was walked (menu path, address, screenshots, columns,
    buttons, fields with their type, required marker, list values and the exact validation message);
  * the SCREENS table below: the written part (purpose, who uses it, business rules, statuses, and what each
    button does where the general meaning in ACTIONS is not enough).

The blocks follow the format read by build_user_manual.py (see content.py), plus ("shot", file, caption, crop).
Only what the screens show is described. The manual names no client and no client brand.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
INVENTORY = json.loads((HERE / "screens" / "inventory.json").read_text(encoding="utf-8"))

FIELD_WIDTHS = [3.4, 1.9, 1.9, 5.1, 4.3]
ACTION_WIDTHS = [4.2, 12.4]

# ---------------------------------------------------------------- general meaning of buttons

ACTIONS = {
    "Add": "Opens a blank form to add a new record.",
    "Save": "Checks the form and saves it. If a required field is empty or wrong, the form stays open and the "
            "message is shown under the field.",
    "Update": "Saves the changes made to the record.",
    "Upload": "Loads several records at once from a file.",
    "Bulk Upload": "Loads several records at once from a file.",
    "Search": "Runs the search with the criteria entered.",
    "Clear": "Clears the search criteria.",
    "Reset": "Clears the entries and returns the screen to its starting values.",
    "Export": "Downloads the list as a file.",
    "Export CSV": "Downloads the list as a CSV file.",
    "Export Report": "Downloads the figures on the screen as a report.",
    "Generate": "Produces the report for the criteria entered.",
    "Generate Report": "Opens the report dialog. Pick a report category, then Generate & Download.",
    "Close": "Closes the dialog without saving.",
    "Cancel": "Closes the form without saving.",
    "Back": "Returns to the previous screen.",
    "Next": "Checks this step and moves to the next one.",
    "Previous": "Returns to the previous step.",
    "Refresh": "Reloads the list.",
    "Show Filters": "Shows more filter fields for the list.",
    "Apply Filters": "Refreshes the figures for the filters chosen.",
    "View": "Opens the record in read-only view.",
    "Edit": "Opens the record for changes.",
    "Delete": "Removes the record after you confirm.",
    "Print All": "Prints every line in the list.",
    "Print Selected": "Prints the lines you have ticked.",
    "Save Configuration": "Saves the account set-up rule.",
    "Duplicate Config": "Copies the rule on screen so you can change it and save it as a new rule.",
    "Save Draft": "Keeps the work so far without submitting it.",
    "Validate": "Checks the selected items before they are processed.",
    "Import": "Loads lines from a file.",
    "Load": "Loads the lines for the criteria entered.",
    "Share": "Sends the quotation to the client.",
    "Choose": "Changes how many rows the list shows per page.",
}
NOISE = {"First Page", "Previous Page", "Next Page", "Last Page", "Language", "Search by", "Choose", "Select",
         "Select ", "1", "2", "3", "4", "5", "Expand null", "Status", "Definition", "Risk sections"}

# ---------------------------------------------------------------- written part, per screen

# purpose, users, rules (list), flow (status text), actions (button -> effect), skip (state labels to leave out)
SCREENS: dict[str, dict] = {
    # -- sign in
    "login": dict(
        title="Sign-in page",
        purpose="The sign-in page is the entry point to BrokerVerse. You enter your user ID and password and "
                "choose the language for the session.",
        users="Every user.",
        rules=["Both fields must be filled in before the system tries to sign you in.",
               "The password must have at least 6 characters. A shorter password is stopped on the page with "
               "the message **Password must be at least 6 characters**.",
               "Your user ID decides the menus and screens you see after signing in. Your administrator sets "
               "this up in Master > Generals > User Management."],
        actions={"Login": "Checks the two fields and signs you in. You land on the home dashboard.",
                 "Forgot password?": "Link for users who cannot remember their password. Ask your administrator "
                                     "to reset it if the link does not open a reset page for you."},
        skip=["form:Forgot password?", "main:blocked-post"]),

    # -- dashboards
    "dashboard-executive-dashboard": dict(
        purpose="A one-page view of business performance for management: revenue, active policies, customer "
                "satisfaction, claims rate, new business and retention against target, with trends by month, "
                "revenue by product line and performance by region, product and agent.",
        users="Management, branch heads and team leaders.",
        rules=["Each tile shows the figure for the chosen period, the change against the previous period and "
               "the target, with a bar that fills as the target is reached.",
               "Change the period with the date range or the period list (for example This Month); the whole "
               "page refreshes for that period."],
        actions={"Settings": "Chooses which tiles and charts the dashboard shows.",
                 "View Claims": "Quick link to the claims area.", "Underwriting": "Quick link to underwriting.",
                 "New Quote": "Quick link to start a quotation.", "Reports": "Quick link to Reports.",
                 "Policies": "Quick link to the policy list.", "Analytics": "Quick link to analytics."},
        skip=["form:New Quote"]),
    "dashboard-claims-dashboard": dict(
        purpose="Tracks open claims: the number open, overdue and reported today, the largest claims and where "
                "claims come from, with a list of recent claims.",
        users="Claims staff and claims team leaders.",
        rules=["The Recent Claims list shows the claim number, line of business, customer, policy, loss and "
               "report dates, who reported it, its priority, status and amount.",
               "Use the date range to limit the figures to a period."],
        flow="Claims in the list show **Pending**, **Processing** or **Settled**.",
        skip=["record"]),
    "dashboard-underwriting-dashboard": dict(
        purpose="The underwriter's workbench: new and older submissions, average cycle time, open alerts (for "
                "example a duplicate submission or missing details) and the workload of each assignment group, "
                "with the list of submissions.",
        users="Underwriters and underwriting team leaders.",
        rules=["Alerts point to submissions that need attention before they can move on, such as a duplicate "
               "submission or missing dates, line of business, type or broker.",
               "Each submission carries a risk score, the next requirement and its due date, and a priority."],
        flow="Submissions show their stage, for example **In Review**, and can be closed with Mark as Closed.",
        actions={"New Submission": "Starts a new underwriting submission.",
                 "Mark as Closed": "Closes the selected submission."},
        skip=["form:New Submission", "record"]),
    "dashboard-agent-dashboard": dict(
        purpose="The agent's home page: leads, clients and policies sold, commission for the year, upcoming "
                "events from the activity monitor and the agent's earned commission, collected premium, "
                "receivables and gross premium.",
        users="Agents and account officers.",
        rules=["Create Quote lists the products you can quote: Motor, Fire and Allied Perils, Travel, Employee "
               "Benefit and Property.",
               "After you choose a product, the system asks whether the quote is for a **New Lead** or an "
               "**Existing Client**."],
        actions={"Create Quote": "Starts a quotation for the product chosen from the list.",
                 "See More": "Opens the full list behind the tile."},
        skip=["form:Create Quote>Fire and Allied Perils", "form:Create Quote>Travel",
              "form:Create Quote>Employee Benefit", "form:Create Quote>Property"]),

    # -- product configurator
    "product-configurator-dashboard": dict(
        purpose="The starting point for product set-up: active products, total premium, average loss ratio and "
                "average commission, with the product list, performance analytics and quick links to each "
                "configuration area.",
        users="Product managers and business analysts.",
        rules=["Each product shows its code, name, category, line of business, version, status, base rate and "
               "commission.", "Filter the list by category or search by product."],
        actions={"Create New Product": "Opens Product Templates to create the product from a template.",
                 "Filter by Category": "Limits the list to one category."}),
    "product-configurator-product-templates": dict(
        purpose="Keeps the product templates that quotations are built from. A template holds the risk "
                "information, premium rates, compulsory third party liability (CTPL) premiums, statutory taxes "
                "and fees, and rating factors for a product.",
        users="Product managers and business analysts.",
        rules=["A template has a code, product name, category, version and status.",
               "Open a template to maintain it tab by tab: Risk Information, Premium Rates, CTPL setting, Taxes "
               "and fees, and Rating Factors. Save Template keeps the changes.",
               "Premium Rates hold the own damage rate as a percentage of vehicle value for each vehicle type, "
               "and the rates of add-on covers such as Acts of Nature, Roadside Assistance and Personal "
               "Accident.",
               "Taxes and fees hold the Documentary Stamp Tax (DST), Value Added Tax (VAT) and Local Government "
               "Tax (LGT) percentages.",
               "Rating Factors hold the driver age multipliers and the vehicle depreciation rate and maximum "
               "years."],
        flow="Templates are **Active** or **Inactive**.",
        actions={"Create Template": "Opens the Create Product Template dialog.",
                 "Create": "Creates the template from the dialog.",
                 "Save Template": "Saves the changes on all tabs of the template.",
                 "Add Label": "Adds a label to the template."}),
    "product-configurator-coverage-builder": dict(
        purpose="Keeps the covers that products can include, with their type, deductible, waiting period and "
                "effect on the premium.",
        users="Product managers.",
        rules=["A cover is **Mandatory** (always included) or **Optional** (the client may add it).",
               "Premium Impact shows how the cover changes the premium, for example Base or +0.5%."],
        actions={"Add Coverage": "Opens the Configure Coverage dialog.",
                 "Save Coverage": "Adds the cover to the list."}),
    "product-configurator-rating-engine": dict(
        purpose="Keeps the rating factors used to price a product, such as vehicle age, driver age, no claim "
                "bonus and construction type, with the rules behind each factor.",
        users="Product managers and pricing staff.",
        rules=["A factor is Multiplicative (it multiplies the premium) or a Discount.",
               "Expand a factor to see its rules. Test Calculator lets you try a premium with sample values."],
        actions={"Test Calculator": "Opens a calculator to test the rating with sample values.",
                 "Add Factor": "Opens the Configure Rating Factor dialog.",
                 "Save Factor": "Adds the factor to the list."}),
    "product-configurator-underwriting-rules": dict(
        purpose="Keeps the automatic underwriting rules, grouped as acceptance, validation and loading rules.",
        users="Underwriting managers and product managers.",
        rules=["Each rule has a code, name, type, a condition and the action taken when the condition is met."],
        actions={"Add Rule": "Opens the Configure Underwriting Rule dialog.", "Save Rule": "Adds the rule.",
                 "Acceptance Rules": "Shows the acceptance rules. The number on the button is how many there are.",
                 "Validation Rules": "Shows the validation rules.",
                 "Loading Rules": "Shows the loading rules."}),
    "product-configurator-document-manager": dict(
        purpose="Keeps the document templates used for each product, such as the policy schedule, the CTPL "
                "certificate and the member enrolment form, and the stage at which each one is produced.",
        users="Product managers.",
        rules=["Each template shows its format, code, name, type, the stage it belongs to (for example Policy "
               "Issuance or Quotation) and whether it is required."],
        actions={"Upload Template": "Uploads a new document template."}),
    "product-configurator-approval-workflows": dict(
        purpose="Shows the approval workflows for product changes: who approves, in what order and within what "
                "time (SLA), and what triggers the workflow.",
        users="Product managers and compliance.",
        rules=["New Product Approval runs in sequence: Product Manager (2 days), Compliance Officer (1 day), "
               "Chief Product Officer (1 day). It is triggered by a new product or a major version change.",
               "Rate Change Approval runs in parallel: Actuarial Team (3 days) and Sales Head (2 days). It is "
               "triggered by a rate change of more than 10%."],
        actions={"Create Workflow": "Starts a new approval workflow.",
                 "New Product Approval": "Shows the new product workflow.",
                 "Rate Change Approval": "Shows the rate change workflow."},
        skip=["form:Create Workflow", "form:New Product Approval"]),
    "product-configurator-market-mapping": dict(
        purpose="Maps products to the insurers that carry them, with the commission and override agreed with "
                "each insurer, the production target and the year-to-date performance.",
        users="Product managers and the placement team.",
        actions={"Map Product": "Opens the dialog to map a product to an insurer."}),
    "product-configurator-risk-mapping": dict(
        purpose="Lists each product definition and how its risk is described: by liability cover fields, "
                "property risk fields, health or life cover fields, or by risk sections.",
        users="Product managers and business analysts.",
        rules=["Industrial All Risks (IAR) is defined by risk sections instead of vehicle details: one policy, "
               "many independent sections."],
        actions={"Search": "Searches by product code, name or line of business."},
        skip=["record"]),
    "product-configurator-product-analytics": dict(
        purpose="Shows how each product performs: premium trend, category distribution and, for the top "
                "products, policies, premium, average premium, loss ratio and profit margin.",
        users="Product managers and management."),

    # -- master: system settings
    "master-system-settings": dict(
        purpose="Sets the look and the regional settings of the platform: application title, logo and favicon, "
                "display currency, default language, and the primary and secondary colours.",
        users="System administrators.",
        rules=["The colour can be picked from the preset list or entered as a hex code (for example #0056b3); "
               "the picker and the preset list stay in step.",
               "The theme preview shows the colours before you save.",
               "Add Company Logo adds a new logo preset: enter the company or client name and either a logo "
               "address or a logo file."],
        actions={"Save": "Saves the settings for all users.", "Add Company Logo": "Opens the Add Company Logo "
                 "dialog.", "Upload Logo": "Uploads the logo image.", "Upload Favicon": "Uploads the "
                 "browser-tab icon."}),

    # -- master: organisation
    "master-generals-organization-company": dict(
        purpose="Keeps the companies of the organisation, with their licence number, contact details and "
                "address.",
        users="System administrators.",
        rules=["The Status switch in the list makes a company active or inactive.",
               "Use the eye icon to view a company and the pencil to edit it."],
        flow="Companies are **Active** or **Inactive**."),
    "master-generals-organization-branch": dict(
        purpose="Keeps the branches of each company and the departments within each branch.",
        users="System administrators.",
        rules=["A branch belongs to one company.", "Departments are added on the branch record, in the "
               "Department List."],
        flow="Branches and departments are **Active** or **Inactive**."),
    # -- master: insurance management
    "master-generals-insurance-management-insurance-company": dict(
        purpose="Keeps the insurance companies you place business with, with their contact details and "
                "address.",
        users="System administrators and the placement team.",
        skip=["record"],
        flow="Insurance companies are **Active** or **Inactive**."),
    "master-generals-insurance-management-line-of-business": dict(
        purpose="Keeps the lines of business, such as Motor, Fire and Allied Perils, Marine, Personal Accident "
                "and Engineering.",
        users="System administrators."),
    "master-generals-insurance-management-product": dict(
        purpose="Keeps the insurance products and the line of business and commission code of each.",
        users="System administrators."),
    "master-generals-insurance-management-cover": dict(
        purpose="Keeps the covers, such as compulsory third party liability, own damage, acts of nature, "
                "voluntary third party liability and personal accident.",
        users="System administrators."),
    "master-generals-insurance-management-signatories": dict(
        purpose="Keeps the people who sign policy and other documents.",
        users="System administrators."),
    "master-generals-insurance-management-vehicle": dict(
        purpose="Keeps the vehicle list used in motor quotations: name, variant, model, brand and seating "
                "capacity.",
        users="System administrators."),
    # -- master: location
    "master-generals-location-country": dict(
        purpose="Keeps the countries with their ISO code and phone code.", users="System administrators."),
    "master-generals-location-state": dict(
        purpose="Keeps the states or provinces of each country.", users="System administrators.",
        rules=["A state belongs to one country, chosen from the Country list."]),
    "master-generals-location-city-master": dict(
        purpose="Keeps the cities of each state or province.", users="System administrators.",
        rules=["A city belongs to one state, chosen from the State list."]),
    # -- commission
    "master-generals-commission-commission-dashboard": dict(
        purpose="Live figures from the commission ledger: brokerage income, commission paid to referrers "
                "(comsub), net margin, outstanding payable and withholding tax, by referrer and by status.",
        users="Finance and management.",
        rules=["The figures update as commission lines are approved and payouts are run.",
               "Switch between the Accounting and Management views, and group by Insurer or Product."],
        flow="Commission lines move **Accrued** > **Eligible** > **Approved** > **Paid**; a line can also be "
             "**Reversed**.",
        actions={"Accounting": "Shows the accounting view.", "Management": "Shows the management view.",
                 "Insurer": "Groups the figures by insurer.", "Product": "Groups the figures by product."}),
    "master-generals-commission-agents-referrer-accounts": dict(
        purpose="Lists the agents and referrers who earn commission, with their level, number of policies, "
                "open net payable, withholding tax type and bank account. Opening a referrer shows their "
                "policies split by payment cycle (current, future and past).",
        users="Finance.",
        rules=["The referrer record holds identity only; the rate lives on each policy line and can be "
               "overridden per policy from inside the account.",
               "Apply WHT (3% in the example) is ticked by default. Untick it when withholding tax does not "
               "apply to the referrer.",
               "Run the lifecycle from the account: mark lines eligible, approve them, then generate the payout."],
        flow="Policy lines move **Accrued** > **Eligible** > **Approved** > **Paid**.",
        actions={"← Referrers": "Returns to the referrer list.", "Approve": "Approves the eligible lines. "
                 "The button shows how many lines it will approve.", "Generate payout": "Creates the payout for "
                 "the approved lines.", "Mark eligible": "Marks accrued lines as eligible for payment."}),
    # -- employee / user
    "master-generals-employee-management-hierarchy": dict(
        purpose="Keeps the ranks of the sales and staff hierarchy and the level of each rank.",
        users="System administrators and HR."),
    "master-generals-employee-management-designation": dict(
        purpose="Keeps the designations (job titles), the department each belongs to, its level and the level "
                "it reports to.", users="System administrators and HR."),
    "master-generals-employee-management-employee": dict(
        purpose="Keeps the employees and agents: name, type, designation, who they report to, branch and "
                "department, identity document and address.", users="System administrators and HR."),
    "master-generals-user-management-user": dict(
        purpose="Keeps the user accounts that can sign in: user name, e-mail, display name, roles, the "
                "branches the user may work in and any additional roles.",
        users="System administrators.",
        rules=["At least one role must be ticked: Sales, Underwriting, Customer Services, Claims, Finance, IT "
               "Admin or BA.",
               "Multi Branch Access lists the branches and departments the user may work in, with the "
               "transaction number range for each.",
               "Additional Role gives the user more roles for set active hours."]),
    "master-generals-user-management-role": dict(
        purpose="Keeps the roles and what each role can reach: menu access, sub-menu access and permissions.",
        users="System administrators."),

    # -- master: finance
    "master-finance-premium-account-setup": dict(
        purpose="Sets which general ledger accounts premium transactions post to, for a range of companies, "
                "offices, departments, business types and sources, products, sections, covers and documents.",
        users="Finance administrators.",
        rules=["Every range has a From and a To value; the rule applies to everything inside the range.",
               "Fields marked with an asterisk are required.",
               "Force Company, Force Office and Force Department post to the company, office or department "
               "given here instead of the one on the transaction.",
               "New, Renewal or Both says which business the rule applies to.",
               "The rule applies between the Effective From and Effective To dates."]),
    "master-finance-miscellaneous-account-setup": dict(
        purpose="Sets the general ledger accounts for miscellaneous charges and accruals, by cover, business, "
                "document, product, section, accrual source and organisation range.",
        users="Finance administrators.",
        rules=["Cover Type is required.", "Force Company, Force Office and Force Department work as in Premium "
               "Account Setup."]),
    "master-finance-customer-account-setup": dict(
        purpose="Sets the receivable accounts for customers, by customer category, organisation range, "
                "business type, insurance type and document type.",
        users="Finance administrators.",
        rules=["Ranges marked with an asterisk are required.",
               "A/C Receivable Type decides the receivable account used."]),
    "master-finance-ri-claims-account-setup": dict(
        purpose="Sets the accounts for reinsurance and claims transactions, by business type, document type, "
                "company, division, department, product range and peril class.",
        users="Finance administrators."),
    "master-finance-transaction-code": dict(
        purpose="Keeps the transaction codes used for receipts, vouchers and journals, with the account, "
                "branch and department they post to, their numbering by accounting period and which user roles "
                "may use them within what amount.",
        users="Finance administrators.",
        rules=["Transaction Code Setup gives, for each accounting period, the first and last transaction "
               "number and the last number used.",
               "User Group Access sets the minimum and maximum transaction amount for each user role."]),
    "master-finance-currency": dict(
        purpose="Keeps the currencies, with their ISO code, smallest unit, format and number of decimals.",
        users="Finance administrators."),
    "master-finance-exchange-rate": dict(
        purpose="Keeps the exchange rate between two currencies for a date range.",
        users="Finance administrators.",
        rules=["A rate applies between its Effective From and Effective To dates."]),
    "master-finance-bank": dict(
        purpose="Keeps the bank accounts the organisation uses, with branch, bank code, address and contact "
                "details.", users="Finance administrators."),
    "master-finance-account-category": dict(
        purpose="Keeps the account categories, such as debtor, creditor, supplier, customer and vendor.",
        users="Finance administrators."),
    "master-finance-main-account": dict(
        purpose="Keeps the main (general ledger) accounts: type, whether it holds open entries and how, its "
                "category, and the companies and currencies it can be used for.",
        users="Finance administrators.",
        rules=["Open Entry (Yes or No) says whether items on the account are matched one against another, as "
               "with receivables and payables."]),
    "master-finance-sub-account": dict(
        purpose="Keeps the sub-accounts under main accounts and the currencies each can be used in.",
        users="Finance administrators."),
    "master-finance-taxation": dict(
        purpose="Keeps the taxes, their rate, basis and the dates they apply.", users="Finance administrators."),
    "master-finance-petty-cash": dict(
        purpose="Keeps the petty cash funds: size, minimum cash box and transaction limit.",
        users="Finance administrators.",
        rules=["The transaction limit is the most a single petty cash payment can be."]),
    "master-finance-remittance-master": dict(
        purpose="Keeps the remittance rules, such as automated remittance rules with their frequency, "
                "processing day, cut-off and general ledger mapping.",
        users="Finance administrators.",
        rules=["Choose the master type to list the rules of that type.",
               "An automated remittance rule has a frequency, a processing day, the number of cut-off days "
               "before the period end and the minimum number of transactions needed to run.",
               "GL Mapping gives the main account, sub-account and branch or department for the rule."]),
    "master-finance-incentive-programs": dict(
        purpose="Keeps the sales incentive programmes: who they apply to, the dates, the target and how the "
                "incentive is paid.",
        users="Sales management and finance.",
        rules=["Basic Information holds the code, name, type, who the programme applies to, the dates, "
               "status and currency.",
               "Target Configuration holds the target metric, how often it is calculated, the base target and "
               "an optional stretch target.",
               "Incentive Structure gives, for each achievement level, the incentive type, rate or amount and "
               "the maximum payout."]),
    "master-finance-reinsurance-treaty": dict(
        purpose="Keeps the reinsurance treaties: number, name, type, line of business, reinsurers, effective "
                "and expiry dates and how much of the treaty is used.",
        users="Reinsurance staff.",
        rules=["The Add Treaty dialog has three tabs: Basic Information, Coverage & Limits and Commission."]),

    # -- operations
    "operations-leads-prospects": dict(
        purpose="Lists the leads (prospective clients) by product, and is where new leads are created.",
        users="Agents, account officers and sales staff.",
        rules=["Leads are listed on a tab for each product: Motor, Fire and Allied Perils and Industrial All "
               "Risks.",
               "Create Lead lists the products; choose one to open the lead form for it.",
               "Fire and Allied Perils and Industrial All Risks use the same personal details form as Motor, with "
               "**Next** instead of Save & Continue.",
               "The lead form asks whether the lead is **Retail** or **Corporate** (for Employee Benefit, "
               "**Individual** or **Company**) and then the name, date of birth, gender, contact details and "
               "address. Fields marked with an asterisk are required."],
        actions={"Create Lead": "Lists the products; choosing one opens the lead form for that product.",
                 "Save & Continue": "Checks the lead details and moves to the next step.",
                 "Show Filters": "Shows more filters for the lead list."},
        skip=["form:Create Lead>Travel", "form:Create Lead>Travel:validation", "form:Create Lead>Property",
              "form:Create Lead>Employee Benefit:validation", "form:Create Lead>Fire and Allied Perils",
              "form:Create Lead>Fire and Allied Perils:validation", "form:Create Lead>Industrial All Risks",
              "form:Create Lead>Industrial All Risks:validation"]),
    "operations-clients": dict(
        purpose="Lists your clients, retail and corporate, and opens each client's record with their policies, "
                "claims, renewals and endorsements.",
        users="Agents, account officers and customer service.",
        rules=["The All, Retail and Corporate tabs filter the list by client type.",
               "Search by name or the other options in the search list.",
               "The client record has four tabs: Policy, Claim, Renewal and Endorsement."],
        flow="Clients show **Draft** until their details are complete, then **Active**.",
        actions={"Create Lead": "Starts a lead for a new client (see Leads/Prospects)."},
        skip=["form:Create Lead>Travel", "form:Create Lead>Property", "form:Create Lead>Motor",
              "form:Create Lead>Motor:validation", "form:Create Lead>Fire and Allied Perils",
              "form:Create Lead>Fire and Allied Perils:validation"]),
    "operations-quotation": dict(
        purpose="Lists the quotations with their lead, policy type, gross premium, date and status, and opens "
                "each quotation with its details and audit trail.",
        users="Agents, account officers and underwriters.",
        rules=["Create Quote lists the products (Motor, Fire and Allied Perils, Industrial All Risks). For "
               "Motor and Fire, you first pick the lead from the lead list; for Industrial All Risks, the lead "
               "form opens.",
               "The Audit Trail tab records every change: date and time, action, field, previous and new "
               "value, and who made it."],
        flow="A quotation moves **Draft** > **Pending Customer** > **Customer Accepted**.",
        actions={"Create Quote": "Lists the products you can quote.", "View Details": "Opens the quotation.",
                 "Edit Quotation": "Opens the quotation for changes.", "Share": "Sends the quotation to the "
                 "client."},
        skip=["form:Create Quote>Motor Quote", "form:Create Quote>Motor Quote:validation",
              "form:Create Quote>Fire and Allied Perils", "form:Create Quote>Fire and Allied Perils:validation",
              "tab:Fire and Allied Perils", "tab:Industrial All Risks"]),
    "operations-policy": dict(
        purpose="Lists the policies with client, gross premium, issue and expiry dates, product and payment "
                "status.",
        users="Agents, account officers, processing and customer service.",
        rules=["Create Policy lists the products (Motor Policy, Fire and Allied Perils). You then pick the "
               "lead the policy is for from the lead list.",
               "More Actions on a policy gives the follow-up actions available for it."],
        actions={"Create Policy": "Lists the products you can create a policy for.",
                 "View Policy": "Opens the policy.", "More Actions": "Shows the other actions for the policy."},
        skip=["form:Create Policy>Motor Policy", "form:Create Policy>Motor Policy:validation",
              "form:Create Policy>Fire and Allied Perils", "form:Create Policy>Fire and Allied "
              "Perils:validation", "tab:Fire and Allied Perils", "tab:Industrial All Risks"]),
    "operations-claims": dict(
        purpose="Lists the claims with client, policy, issue date, product and status, and opens each claim.",
        users="Claims staff and account officers.",
        flow="Claims show **Pending**, **Processing** or **Settled**."),
    "operations-renewals-renewal-policy": dict(
        purpose="Lists the policies that have expired or are due for renewal.",
        users="Agents, account officers and the renewal team.",
        flow="Policies here show **Expired**."),
    "operations-renewals-renewal-batch": dict(
        purpose="Groups expiring policies into a renewal batch and sends the renewal notices for the batch.",
        users="Renewal team.",
        rules=["Create Batch filters the policies by expiry date range, insurer, product type, premium range, "
               "client and payment status; Generate Policy List shows the matching policies to tick.",
               "Open a batch to see each policy's notice status, send the renewal notices and retry any that "
               "failed."],
        flow="Notices move **Not Sent** > **Queued** > **Sent**; a notice that cannot be delivered shows "
             "**Failed**.",
        actions={"Create Batch": "Opens the Create Batch dialog.", "Generate Policy List": "Lists the policies "
                 "that match the filters.", "Send Renewal Notices": "Sends the notices for the ticked "
                 "policies.", "Retry Failed": "Sends the failed notices again."}),
    "operations-renewals-renewal-queue": dict(
        purpose="The renewal team's work queue: every policy due for renewal with days to expiry, premium, "
                "status, risk, agent and the number of contact attempts.",
        users="Renewal team and agents.",
        rules=["Filter by policy or insured name, status, risk level, agent and expiry date range.",
               "Open a policy to see its policy information, contact information and claims history."]),
    "operations-renewals-retention-analytics": dict(
        purpose="Shows how well policies are being kept: trends, and performance by product, by agent and by "
                "risk.", users="Renewal managers and management.",
        actions={"Take Action": "Opens the follow-up actions for the figures shown."}),
    "operations-renewals-at-risk-policies": dict(
        purpose="Lists the policies at risk of not renewing, with a risk score and level, and opens each to "
                "show the risk factors, recommended actions and communication history.",
        users="Renewal team and agents."),
    "operations-renewals-negotiations": dict(
        purpose="The workspace for renewal negotiations: the timeline, details and communications for each "
                "negotiation.",
        users="Renewal team and agents.",
        rules=["Add Update records a step in the negotiation: the update type, how you communicated, a "
               "description, the outcome, the next action and a follow-up date."],
        actions={"Add Update": "Opens the dialog to record a negotiation update.",
                 "Save Update": "Adds the update to the timeline.",
                 "Request Approval": "Asks for approval of the negotiated terms.",
                 "Send Communication": "Sends a message to the client."}),
    "operations-renewals-lapse-management": dict(
        purpose="Lists lapsed policies with the days lapsed, premium lost and reason, and runs win-back "
                "campaigns to bring clients back.",
        users="Renewal team and management.",
        rules=["A campaign has a name, target segment, dates, discount, budget and the offers included, such "
               "as a waived reinstatement fee, free add-on cover, extended payment terms or a premium freeze."],
        actions={"Create Campaign": "Opens the dialog to create a win-back campaign."}),
    "operations-renewals-performance": dict(
        purpose="Shows renewal performance: trends, performance by agent and by product, and a KPI scorecard "
                "of target against achieved.", users="Renewal managers and management."),
    "operations-open-items": dict(
        purpose="Lists the items waiting for you: quotes pending, expiring policies, renewal requests and "
                "upcoming events.", users="Agents and account officers."),
    "operations-payments": dict(
        purpose="Lists premium payments by status, with the client, policy, gross premium and policy dates.",
        users="Agents, account officers and finance.",
        rules=["The Paid, Pending and Reviewing tabs filter the list by payment status."],
        skip=["record"]),

    # -- accounts
    "accounts-receipts": dict(
        purpose="Lists the receipts with transaction code and number, policy, customer, date, amount, paid and "
                "unpaid amounts and status. Opening a receipt shows the policies it covers with the premium, "
                "taxes and amounts.",
        users="Cashiers and finance.",
        rules=["The receipt detail shows, per policy, net premium, paid, unpaid, discounts, DST, LGT, VAT, "
               "expanded withholding tax (EWT), and the foreign and local currency amounts."]),
    "accounts-collections": dict(
        purpose="Lists outstanding premiums by client and policy, aged into Current, 1-30, 31-60, 61-90 and "
                "over 90 days, with the due date, status and days overdue.",
        users="Collections staff and account officers.",
        rules=["Open a client to see the insurers and shares on the policy, the payments received and the "
               "follow-up history.",
               "From the record you can send an e-mail, add a note or set a commitment date."],
        flow="Overdue items show **Overdue** with their overdue level.",
        actions={"Manual Trigger Renewal Reminder Now": "Sends the reminders now instead of waiting for the "
                 "scheduled run.", "Send Email": "Sends a collection e-mail to the client.",
                 "Add Note": "Records a follow-up note.", "Set Commitment Date": "Records the date the client "
                 "promised to pay.", "Back to Collections": "Returns to the list."}),
    "accounts-accounting-query": dict(
        purpose="Finds accounting entries by policy, client, entry type, reference type, status, date range "
                "and general ledger code.", users="Finance."),
    "accounts-all-clients-accounting": dict(
        purpose="Shows the accounting entries of all clients with the totals, filtered by client, entry type "
                "and dates.", users="Finance."),
    "accounts-open-entry-matching": dict(
        purpose="Matches open items on an account, for example a receipt against the invoices it pays.",
        users="Finance.",
        rules=["Choose the sub-account and whether you match debits or credits, then Pull to load the open "
               "items.",
               "Tick the items to match (Ok), enter any adjustment, and Match.",
               "A difference can be written off with a write-off code and amount, or taken as a cash discount.",
               "The short labels on this screen stand for: Anly = analysis, Acty = activity, Ctrl Acnt = control "
               "account, Divn = division, Dept = department, Doc = document, Dt = date, FC = foreign currency, "
               "LC = local currency, Cr = credit."],
        actions={"Pull": "Loads the open items for the account.", "Match": "Matches the ticked items.",
                 "Cash Discount": "Takes the difference as a cash discount."}),
    "accounts-open-entry-un-matching": dict(
        purpose="Reverses a match made in Open Entry Matching, so the items are open again.", users="Finance.",
        rules=["The screen works like Open Entry Matching and uses the same short labels."],
        actions={"Pull": "Loads the matched items for the account.", "Unmatch": "Reverses the ticked "
                 "matches.", "Cash Discount": "Shows the cash discount taken on the match."}),
    "accounts-disbursement": dict(
        purpose="Lists the disbursements (payments out) and opens each with its payee, payment details and "
                "instruments.", users="Finance."),
    "accounts-petty-cash-initiate": dict(
        purpose="Opens (initiates) a petty cash fund for a branch and department.", users="Finance."),
    "accounts-petty-cash-request": dict(
        purpose="Records requests for petty cash, with the requester and the amounts by narration.",
        users="Staff requesting petty cash and finance.",
        rules=["Tick Cash in advance when the money is given before the expense is incurred."]),
    "accounts-petty-cash-disbursement": dict(
        purpose="Pays out petty cash against requests, with VAT and withholding tax accounts.",
        users="Petty cash custodians and finance.",
        rules=["Petty Cash Code and Criteria are required."], shots_skip=["record"]),
    "accounts-petty-cash-receipts": dict(
        purpose="Records money received into petty cash.", users="Petty cash custodians and finance.",
        skip=["form:Receipts", "form:Receipts:validation"]),
    "accounts-petty-cash-replenish": dict(
        purpose="Tops the petty cash fund back up from the bank for the disbursements made in a period.",
        users="Finance.", shots_skip=["record"]),
    "accounts-journal-voucher": dict(
        purpose="Lists the journal vouchers and opens each with its accounting lines.", users="Finance.",
        rules=["Total debit and total credit must balance; Net shows any difference."]),
    "accounts-correction-jv": dict(
        purpose="Corrects a posted journal voucher: pick the voucher, the correction transaction code and a "
                "description, then Next to make the corrections.", users="Finance."),
    "accounts-reversal-jv": dict(
        purpose="Reverses a posted journal voucher in full: pick the voucher, the reversal transaction code "
                "and a description, then Next.", users="Finance."),
    "accounts-remittance-automated-processing": dict(
        purpose="Runs the scheduled remittances to insurers: shows what is due, validates it and processes the "
                "ticked schedules, or moves them to later.", users="Finance.",
        actions={"Process Selected": "Processes the ticked remittances.", "Schedule for Later": "Moves the "
                 "ticked remittances to a later date.", "View History": "Shows earlier runs."}),
    "accounts-remittance-tracking": dict(
        purpose="Tracks each remittance to insurers: date, insurer, policies, gross amount, commission, net "
                "amount and status, with the insurer details, the policies and an activity log.",
        users="Finance."),
    "accounts-remittance-statements": dict(
        purpose="Produces remittance statements in three steps: selection, preview and generate.",
        users="Finance.", rules=["Choose the statement type, the period and all insurers, selected insurers "
                                 "or an insurer group."]),
    "accounts-remittance-settlement": dict(
        purpose="Prepares the settlement of premium to an insurer: choose the insurer, add the policies, "
                "calculate the commission, tax and net amount, then save a draft or submit for approval.",
        users="Finance."),
    "accounts-remittance-reconciliation": dict(
        purpose="Reconciles bank transactions with remittance records, automatically or by hand, and lists the "
                "exceptions and earlier reconciliations.", users="Finance.",
        actions={"Auto Match": "Matches the lines that agree automatically.", "Match Selected": "Matches the "
                 "ticked lines.", "Force Match": "Matches the ticked lines even though they differ."}),
    "accounts-remittance-bulk-processing": dict(
        purpose="Processes many remittances from one file in four steps: upload, validate, process and "
                "complete.", users="Finance."),
    "accounts-remittance-scheduling": dict(
        purpose="Shows the remittance schedule on a calendar and lets you add a schedule or run one now.",
        users="Finance."),
    "accounts-remittance-electronic-transfer": dict(
        purpose="Manages the electronic transfers that pay insurers.", users="Finance."),
    "accounts-remittance-approval-workflow": dict(
        purpose="Lists the remittance items waiting for approval, with approval history and delegation.",
        users="Finance approvers."),
    "accounts-remittance-exception-management": dict(
        purpose="Lists the remittance exceptions to be resolved, by type, severity and status.",
        users="Finance."),
    "accounts-remittance-agency-bill-processing": dict(
        purpose="Processes agency bills in steps: choose the agencies, check the bill details, make "
                "adjustments and finalise.", users="Finance."),
    "accounts-remittance-direct-bill-processing": dict(
        purpose="Produces direct bills to insureds in four steps: select the policies, check the bill "
                "details, apply late charges and generate the bills.", users="Finance.",
        rules=["Insurer is required on the first step.",
               "On the last step choose the bill format and how bills go out: e-mail, print or the customer "
               "portal; you can include a statement and create the general ledger entries."]),
    "accounts-remittance-adjustments": dict(
        purpose="Records adjustments to remittance amounts, such as a premium change, and tracks them to "
                "approval.", users="Finance.",
        actions={"New Adjustment": "Opens the dialog to record an adjustment.", "Create Adjustment": "Adds "
                 "the adjustment."}),
    "accounts-remittance-notifications": dict(
        purpose="The inbox of remittance notifications, with sent messages, templates and analytics.",
        users="Finance.", actions={"Compose": "Writes a new notification.", "Back to Master": "Returns to the "
                                   "remittance main page."}),
    "accounts-remittance-history": dict(
        purpose="The history and audit trail of remittance records: who changed what and when, with each "
                "version.", users="Finance and audit.",
        actions={"Export History": "Downloads the history.", "Back to Master": "Returns to the remittance "
                 "main page."}),
    "accounts-remittance-analytics": dict(
        purpose="Remittance analytics: volumes and values, top performers, trends and forecasts, and alerts.",
        users="Finance and management."),
    "accounts-incentive-my-programs": dict(
        purpose="Shows the agent's incentive programmes with target, achievement and potential earning.",
        users="Agents."),
    "accounts-incentive-calculations": dict(
        purpose="Runs the incentive calculation for a period and lists the calculation batches.",
        users="Sales management and finance.",
        actions={"New Calculation": "Opens the dialog to run a calculation for a period."}),
    "accounts-incentive-approvals": dict(
        purpose="Lists the incentive batches waiting for approval, with the amount per agent, and approves or "
                "rejects them with a comment.", users="Approvers.",
        actions={"Bulk Approve": "Approves the ticked batches."}),
    "accounts-incentive-statement": dict(
        purpose="Shows the incentive statement for a month: programme, target, achievement, rate and amount "
                "earned.", users="Agents and finance."),

    # -- reinsurance
    "reinsurance-treaty-dashboard": dict(
        purpose="Shows each treaty with its utilisation, premium ceded and claims recovered, with charts of "
                "utilisation, the renewal timeline and capacity.",
        users="Reinsurance staff and management.",
        rules=["Open a treaty to see its cessions, claims and documents."],
        actions={"Add Treaty": "Opens the treaty master to add a treaty."},
        skip=["form:Add Treaty", "form:Add Treaty:validation", "record", "tab:Claims", "tab:Documents"]),
    "reinsurance-cession-tracking": dict(
        purpose="Tracks the cessions of each policy to treaties: cession percentage, ceded premium, commission "
                "and status.", users="Reinsurance staff.",
        actions={"Process Cession": "Processes the pending cessions.", "Generate Bordereau": "Produces the "
                 "bordereau (the list of ceded risks) for the reinsurer."}),
    "reinsurance-claims-recovery": dict(
        purpose="Tracks what can be recovered from reinsurers on claims, and what has been recovered.",
        users="Reinsurance and claims staff."),
    "reinsurance-reconciliation": dict(
        purpose="Reconciles reinsurance balances with each reinsurer: our amount, their amount and the "
                "variance.", users="Reinsurance staff and finance."),
    "reinsurance-analytics": dict(
        purpose="Reinsurance analytics for management.", users="Reinsurance staff and management."),
}

REPORTS = {
    "reports-operational-reports-production": "Production: the business written in the period.",
    "reports-operational-reports-claims": "Claims: the claims reported and their progress.",
    "reports-operational-reports-renewal": "Renewal: policies due for renewal and their outcome.",
    "reports-operational-reports-remittance": "Remittance: premium remitted to insurers.",
    "reports-operational-reports-broker-commission": "Broker Commission: commission earned.",
    "reports-financial-reports-soa-premium-receivable": "SOA/Premium Receivable: the statement of account and "
                                                        "premium still to be collected.",
    "reports-financial-reports-collection-report": "Collection Report: premium collected in the period.",
    "reports-financial-reports-payables": "Payables: amounts owed, for example to insurers.",
    "reports-financial-reports-journal": "Journal: the journal entries posted.",
    "reports-financial-reports-trail-balance": "Trial balance (shown in the menu as Trail Balance): the balance "
                                               "of every account.",
}

# ---------------------------------------------------------------- building blocks from the inventory

TYPE_NAMES = {"text": "Text", "textarea": "Text (several lines)", "dropdown": "List", "multi-select":
              "List (several)", "date": "Date", "number": "Number", "checkbox": "Tick box", "radio": "Option",
              "switch": "Switch", "password": "Password", "email": "Email address", "tel": "Phone number"}
BRAND = re.compile(r"\bBDO\w*|\bBIBS\b|banco de oro|eastwest|china bank|\bbank\b", re.I)


def clean_label(label: str) -> str:
    return re.sub(r"\s*\*+\s*$", "", re.sub(r"\*\*", "", label or "")).strip().rstrip(":").strip()


def label_state(label: str) -> str:
    if label == "main":
        return "list"
    if label == "record":
        return "record"
    if label.startswith("tab:"):
        return f"{label[4:]} tab"
    if label.startswith("form:"):
        name = label[5:].split(":")[0]
        return name.replace(">", ": ")
    return label


DATA_LISTS = re.compile(r"client|agent|company|branch|insurer|insurance company|city|province|state|country|"
                        r"sub account|main account|requester|reporting to|designation|department|bank|petty cash|"
                        r"currency code|transaction code|logo preset|employee|user|office|division|referrer|"
                        r"treaty|account code|gl account|analysis code|activity code|period", re.I)


def allowed(field: dict) -> str:
    if field["type"] in ("dropdown", "multi-select") and DATA_LISTS.search(field["label"]):
        return "Choose from the list. The entries come from your own set-up data."
    opts = [o for o in field.get("options", []) if o and not BRAND.search(o)]
    if opts:
        shown = ", ".join(opts[:8])
        return shown + (f" and {len(opts) - 8} more" if len(opts) > 8 else "")
    kind, name = field["type"], field["label"].lower()
    if kind == "date":
        return "Pick a date from the calendar."
    if kind == "number":
        return "Numbers only."
    if kind == "checkbox":
        return "Ticked or not ticked."
    if kind == "switch":
        return "On or off."
    if "e-mail" in name or "email" in name:
        return "An e-mail address."
    if "phone" in name or "contact number" in name or name == "fax":
        return "Phone number with the country code."
    if kind in ("dropdown", "multi-select"):
        return "Choose from the list."
    if field.get("disabled") or field.get("readonly"):
        return "Filled in by the system."
    return "Free text."


def field_rows(fields: list[dict]) -> list[list[str]]:
    rows, seen, radios = [], set(), []

    def flush_radios():
        if radios:
            rows.append([" / ".join(r["label"] for r in radios), "Option", "Yes" if any(r["required"] for r in
                         radios) else "No", "Choose one: " + ", ".join(r["label"] for r in radios) + ".",
                         next((r["error"] for r in radios if r["error"]), "")])
            radios.clear()

    for f in fields:
        label = clean_label(f["label"])
        if not label or re.fullmatch(r"\d+|Search.*|All.*|Choose|Select", label):
            continue
        f = {**f, "label": label}
        if f["type"] == "radio":
            radios.append(f)
            continue
        flush_radios()
        if label in seen:
            continue
        seen.add(label)
        req = "Yes" if f["required"] else "No"
        msg = f["error"] or ""
        rows.append([label, TYPE_NAMES.get(f["type"], f["type"].title()), req, allowed(f),
                     f"**{msg}**" if msg else "None shown."])
    flush_radios()
    return rows


def action_rows(buttons: list[str], extra: dict) -> list[list[str]]:
    rows = []
    for b in buttons:
        name = re.sub(r"\s*\(\d+\)$|\s+\d+$", "", b.strip())
        if not name or name in NOISE or name.startswith("Select ") or re.search(r"\w+\.\w+", name):
            continue
        text = extra.get(name) or ACTIONS.get(name)
        if text is None:
            continue
        if name not in [r[0] for r in rows]:
            rows.append([name, text])
    for name, text in extra.items():
        if name not in [r[0] for r in rows] and name not in buttons:
            rows.append([name, text])
    return rows


def menu_path(menu: list[str]) -> str:
    return " > ".join(f"**{m}**" for m in menu)


def screen_blocks(key: str, level: str = "h2") -> list:
    inv = INVENTORY[key]
    meta = SCREENS.get(key, {})
    title = meta.get("title") or inv["menu"][-1]
    blocks: list = [(level, title)]
    blocks.append(("p", meta.get("purpose", "")))
    blocks.append(("keep",))
    info = [["Used by", meta.get("users", "Users with access to this menu.")],
            ["Where to find it", menu_path(inv["menu"]) if key != "login" else "The address of your "
             "BrokerVerse site, before you sign in."]]
    blocks.append(("info", info))

    states = inv["states"]
    skip = set(meta.get("skip", []))
    main = states.get("main", {})
    shots_done, last_text = set(), None
    buttons: list[str] = []
    for label, st in states.items():
        if label in skip or "error" in st and len(st) == 1:
            continue
        buttons += st.get("buttons", [])
        is_validation = label.endswith(":validation")
        if is_validation and not st.get("messages"):
            continue
        if st.get("shot") in shots_done or not st.get("shot") or label in meta.get("shots_skip", []):
            continue
        if not is_validation and st.get("text") == last_text:
            continue
        last_text = st.get("text")
        shots_done.add(st["shot"])
        name = label_state(label.replace(":validation", ""))
        if label == "main":
            caption = f"{title}"
            crop = "full"
        elif is_validation:
            caption = f"{title}: messages shown when {st.get('pressed', 'Save')} is pressed with the form empty"
            crop = "full" if key == "login" else "content"
        else:
            caption = f"{title}: {name}"
            crop = "content"
        blocks.append(("shot", st["shot"], caption, crop))

    # Fields: one table for the list filters when they matter, one per form or record.
    tables = inv.get("field_tables", [])
    for t in tables:
        if t["state"] in skip:
            continue
        rows = field_rows(t["fields"])
        if not rows:
            continue
        blocks.append(("h3", f"Fields: {label_state(t['state'])}"))
        blocks.append(("table", ["Field", "Type", "Required", "Allowed values / format", "Message if wrong"],
                       rows, FIELD_WIDTHS))
    cols = main.get("columns") or []
    if cols:
        blocks.append(("h3", "Columns in the list"))
        blocks.append(("p", ", ".join(c for c in cols if c) + "."))
    actions = action_rows(buttons, meta.get("actions", {}))
    if actions:
        blocks.append(("h3", "Buttons and actions"))
        blocks.append(("table", ["Button", "What it does"], actions, ACTION_WIDTHS))
    if meta.get("rules"):
        blocks.append(("h3", "Business rules"))
        blocks.append(("bullets", meta["rules"]))
    if meta.get("flow"):
        blocks.append(("h3", "Status"))
        blocks.append(("p", meta["flow"]))
    return blocks


def group(keys: list[str]) -> list:
    out = []
    for k in keys:
        if k in INVENTORY:
            out += screen_blocks(k)
    return out


def keys_starting(prefix: str) -> list[str]:
    return [k for k in INVENTORY if k.startswith(prefix)]


# ---------------------------------------------------------------- chapters

INTRODUCTION = [
    ("h1", "Introduction"),
    ("h2", "About iNXT BrokerVerse"),
    ("p", "iNXT BrokerVerse is the insurance broking platform from iorta TechNXT. It covers the work of a broker "
          "from the first contact with a prospect to the payment of commission: leads and clients, quotations "
          "and policies, claims and renewals, receipts and collections, remittance to insurers, accounting, "
          "commission and incentives, reinsurance, product set-up and reports."),
    ("h2", "About this edition"),
    ("p", "Edition 2.0 describes the platform screen by screen. For every screen in the menu you will find what "
          "it is for, who uses it, where to find it, a picture of it, its fields with the values they accept "
          "and the message shown when an entry is missing or wrong, what each button does, the business rules "
          "you can see on the screen and the statuses a record goes through."),
    ("p", "The pictures were taken on a test site with sample data. Names, amounts and dates in them are "
          "examples only. Your screens show your own data and may differ slightly, depending on your "
          "organisation's set-up and your role."),
    ("h2", "How to read the screen pages"),
    ("table", ["Part", "What it tells you"], [
        ["Used by", "The people who normally work on the screen. Your administrator decides who can open it."],
        ["Where to find it", "The menu path, starting from the menu on the left of the screen."],
        ["Fields", "Each field with its type, whether it is required, the values it accepts and the exact "
                   "message shown if it is left empty or filled in wrongly. **None shown** means the screen "
                   "showed no message for that field when the form was sent empty."],
        ["Buttons and actions", "What happens when you press each button."],
        ["Business rules", "The rules the screen applies or explains."],
        ["Status", "The statuses a record shows and the order it moves through them."],
    ], [4.2, 12.4]),
    ("tip", "A field marked with an asterisk (*) on the screen is required. The form will not move on until it "
            "is filled in."),
]

GETTING_STARTED = [
    ("h1", "Getting started"),
    ("p", "You need a user ID and password from your administrator, and a current web browser such as Chrome, "
          "Edge or Safari. BrokerVerse runs in the browser; there is nothing to install."),
] + screen_blocks("login") + [
    ("h2", "Your profile and signing out"),
    ("p", "Select your picture at the top right of any screen. The menu shows your name and e-mail address, "
          "with **Profile**, **Help** and **Logout**."),
    ("shot", "header--profile-menu.png", "The profile menu", "content"),
    ("bullets", ["**Profile** opens your profile: name, preferred name, date of birth, gender, contact details "
                 "and address, with **Edit Profile** to change them. First Name and Preferred Name are "
                 "required.",
                 "**Logout** signs you out and returns you to the sign-in page. Always sign out on a shared "
                 "computer."]),
    ("shot", "profile--profile.png", "Your profile", "content"),
]

FINDING_YOUR_WAY = [
    ("h1", "Finding your way around"),
    ("h2", "The screen layout"),
    ("p", "Every screen has the same frame. The **menu** is on the left. The **top bar** holds the language "
          "list, the notification bell and your picture. The **work area** shows the screen you opened, with "
          "its title and a breadcrumb that shows where you are, for example Master > Currency > Add Currency."),
    ("shot", "dashboard-executive-dashboard.png", "The screen frame: menu on the left, top bar, work area", "full"),
    ("h2", "The menu"),
    ("p", "The menu has eight groups. Select a group to open it and select an entry to open the screen. Type "
          "in **Search menu** to find a screen by name."),
    ("table", ["Menu group", "What you find there"], [
        ["Dashboard", "Executive, Claims, Underwriting and Agent dashboards"],
        ["Product Configurator", "Product dashboard, templates, covers, rating, underwriting rules, documents, "
                                 "approval workflows, market and risk mapping, product analytics"],
        ["Master", "System settings, and the Generals and Finance reference data"],
        ["Operations", "Home, leads, clients, quotations, policies, claims, renewals, open items and payments"],
        ["Accounts", "Receipts, collections, accounting queries, open entry matching, disbursement, petty "
                     "cash, journal vouchers, remittance and incentives"],
        ["Commission", "Commission dashboard and agent or referrer accounts"],
        ["Reinsurance", "Treaties, cessions, claims recovery, reconciliation and analytics"],
        ["Reports", "Operational and financial reports"],
    ], [4.2, 12.4]),
    ("h2", "Working with lists"),
    ("bullets", [
        "Type in the **Search** box to find a record. Where there is a **Search by** list next to it, choose "
        "which field to search first.",
        "Use **Row count** or **Rows per page** to show more rows, and the arrows under the list to move "
        "between pages. The text next to the arrows shows which rows you are looking at, for example 1 - 5 of "
        "10.",
        "Use the icons at the end of a row: the eye opens the record, the pencil opens it for changes and the "
        "arrow opens the full record.",
        "On master lists, the **Status** switch makes a record active or inactive.",
    ]),
    ("h2", "Forms and messages"),
    ("bullets", [
        "Required fields are marked with an asterisk (*).",
        "When you press Save, Next or a similar button, the form checks your entries. A missing or wrong entry "
        "is shown in red under the field, for example **This field is required**. Correct it and press the "
        "button again.",
        "A short message in the top right corner confirms that data was loaded or saved.",
        "Close or Cancel on a dialog leaves without saving.",
    ]),
    ("h2", "Language and notifications"),
    ("p", "Choose the language in the list at the top of the screen: **English** or **Thai**. The bell shows the number of notifications waiting for you."),
]


def chapter(title: str, intro: str, keys: list[str]) -> list:
    return [("h1", title), ("p", intro)] + group(keys)


DASHBOARDS = chapter("Dashboards", "The dashboards give each team a one-page view of its work. They open from "
                     "the Dashboard group of the menu.", keys_starting("dashboard-"))
OPERATIONS = chapter("Operations", "Operations holds the day-to-day broking work: leads and clients, "
                     "quotations, policies, claims, renewals and payments.", keys_starting("operations-"))
ACCOUNTS = chapter("Accounts", "Accounts holds the finance work: receipts, collections, accounting queries, "
                   "matching, disbursement, petty cash, journal vouchers, remittance to insurers and "
                   "incentives.", keys_starting("accounts-"))
COMMISSION = chapter("Commission", "Commission shows what agents and referrers earn and runs their payouts. "
                     "The same two screens are also listed under Master > Generals > Commission.",
                     keys_starting("master-generals-commission-"))
REINSURANCE = chapter("Reinsurance", "Reinsurance follows the treaties, the risks ceded under them and what is "
                      "recovered on claims.", keys_starting("reinsurance-"))


def reports_chapter() -> list:
    keys = keys_starting("reports-")
    blocks = [("h1", "Reports"),
              ("p", "Reports are grouped as Operational Reports and Financial Reports. Every report uses the same "
                    "criteria page: choose the report criteria and the period, narrow it down if needed, and "
                    "press Generate.")]
    if not keys:
        return blocks
    page = screen_blocks(keys[0])
    page[0] = ("h2", "The report criteria page")
    blocks += page
    blocks += [("h2", "Available reports"),
               ("table", ["Menu path", "What the report shows"],
                [[" > ".join(INVENTORY[k]["menu"][1:]), REPORTS.get(k, "")] for k in keys], [5.6, 11.0])]
    return blocks


PRODUCT = chapter("Product Configurator", "The Product Configurator is where products are designed: templates, "
                  "covers, rating, underwriting rules, documents, approvals and the mapping of products to "
                  "insurers.", keys_starting("product-configurator-"))
MASTER = ([("h1", "Master data and settings"),
           ("p", "Master holds the reference data the rest of the platform uses. Set it up before you start, and "
                 "keep it current. Most master screens work the same way: a list with a search box, **Add** to "
                 "create a record, the eye to view it and the pencil to change it.")]
          + group(["master-system-settings"])
          + group([k for k in keys_starting("master-generals-") if "commission" not in k])
          + group(keys_starting("master-finance-")))

GLOSSARY = [
    ("h1", "Glossary"),
    ("table", ["Term", "Meaning"], [
        ["CTPL", "Compulsory Third Party Liability, the motor cover required by law."],
        ["DST", "Documentary Stamp Tax."],
        ["EWT", "Expanded Withholding Tax."],
        ["LGT", "Local Government Tax."],
        ["VAT", "Value Added Tax."],
        ["WHT", "Withholding tax, deducted from commission paid to agents and referrers."],
        ["Comsub", "Commission paid on to a sub-agent or referrer."],
        ["IAR", "Industrial All Risks, a property policy defined by risk sections."],
        ["Lead", "A prospect who has not yet bought a policy."],
        ["Open entry", "An item on an account that waits to be matched, such as an unpaid invoice."],
        ["Remittance", "Premium paid on to the insurer, after commission."],
        ["Cession", "The part of a risk passed to a reinsurer under a treaty."],
        ["Bordereau", "The list of risks or claims ceded, sent to the reinsurer."],
        ["SLA", "The time allowed for a step, for example an approval."],
        ["SOA", "Statement of account."],
    ], [3.4, 13.2]),
]

CHAPTERS = [INTRODUCTION, GETTING_STARTED, FINDING_YOUR_WAY, DASHBOARDS, OPERATIONS, ACCOUNTS, COMMISSION,
            REINSURANCE, reports_chapter(), PRODUCT, MASTER, GLOSSARY]
