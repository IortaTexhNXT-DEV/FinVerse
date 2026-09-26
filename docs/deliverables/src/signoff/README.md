# Business sign-off packs

One pack per BRD release set: the data behind the screen specifications and business chapters of FRS v2.0, the
sign-off workbook and the screen and message cases of the test plan (deliverables README, "Release and sign-off per
BRD").

| File | Content |
|---|---|
| `signoff_pack.py` | Loads a pack, checks it against the code and the FRS, renders the FRS chapters (```pack blocks) and builds the sign-off workbook |
| `RELEASE_NOTE_<BRD>.md` | Release note of the set (Word source) |
| `<brd>/pack.yaml` | Metadata, personas and their SIT users, sections of the BRD, the screen-flow links, common screen elements |
| `<brd>/screens/*.yaml` | One file per process area; one entry per screen |
| `<brd>/messages.yaml` | Where each message appears and the fix; the texts are read from the code |
| `<brd>/notifications.yaml`, `contract.yaml`, `documents.yaml`, `walkthroughs.yaml` | Notifications, cross-BRD interface contract, generated documents, end-to-end walkthroughs |
| `<brd>/screenshots/` | PNG screenshots captured on the SIT environment (`tools/screenshots/capture_pack.cjs`) |

## Build and check

```bash
python docs/deliverables/src/signoff/signoff_pack.py brd01/pack.yaml --check          # checks only
python docs/deliverables/src/signoff/signoff_pack.py brd01/pack.yaml                  # sign-off workbook
python docs/deliverables/src/signoff/signoff_pack.py brd01/pack.yaml --manifest m.json  # screenshot manifest
python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD01_NEW_BUSINESS.md  # FRS v2.0
python docs/deliverables/src/testplans/build_test_plan.py brd01_cases.yaml               # test plan v2.0
python tools/deliverables/bdoi_docx.py docs/deliverables/src/signoff/RELEASE_NOTE_BRD01.md   # release note
python tools/deliverables/drop_index.py
```

The check refuses: a field label or button that is not a text of `frontend/src` (or a workflow stage or action), a
route that is not a screen of the menu, a screen no persona can open, an FR that is not in the FRS, a test-plan screen
alias that does not exist, an error or validation message without a fix, a flow or walkthrough link to an unknown
screen, and the words the writing standard bans. A label in `[square brackets]` describes screen content (tiles, tags,
a panel) instead of quoting a screen text and is not checked; the brackets are not printed.

## Screen entry

```yaml
- id: SCR-NB-02                     # SCR-<code>-nn, numbered in process order
  title: New Client and Edit Client
  route: /crm/clients/new           # route of navigation/modules.ts; decides personas, permission and menu path
  also: [/crm/clients/:id/edit]     # other routes of the same screen (optional)
  parent: /crm/clients                  # for a screen without a menu entry: the screen it is reached from
  public: true                      # only for the sign-in screen (no menu, no permission)
  purpose: >-
    What the screen is for.
  entry: ["Clients › New Client", "Client record › Edit"]   # other ways in (the menu path is added)
  shots:                            # screenshots; the first carries the numbered callouts of the fields
    - {state: filled, user: ao, fill: new_client, caption: New Client form with the identity entered}
    - {state: error, user: ao, fill: new_client_invalid, submit: "^save as prospect$", caption: Field messages}
                                    # keys for the capture: open (first / a reference), tab, click, fill, submit
  fields:                           # section | label | type | length / format | mandatory | source / list |
    - "Identity | Client type | Drop-down | Individual or Corporate | Y | Fixed list | Individual | New prospect | - | -"
                                    #   default | editable in | validation | message when it fails
  actions:                          # button | who | enabled when | what happens | resulting status | notification
    - "Save as prospect | CLIENT_MAINTAIN | No blocking duplicate | Saves the prospect | Prospect | -"
  rules: ["Only the client type and the name are needed to save a prospect (FR-NB-031)."]
  outcome: The prospect exists with its code.
  frs: [FR-NB-031, FR-NB-033]
  tests: [NEWCLIENT]                # screen aliases of the test plan YAML
```

Screenshots are named `<screen id>-<nn>-<state>.png` (walkthrough steps and documents by their slug) and are captured
with seed data only. Until a screenshot exists, the FRS shows a framed placeholder with its caption.
