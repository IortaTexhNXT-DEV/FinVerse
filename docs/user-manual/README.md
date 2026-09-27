# iNXT BrokerVerse User Manual

The customer-facing user manual of iNXT BrokerVerse, on the iorta TechNXT template (logo, blue and teal palette,
document control, contents, "Page x of y" footer).

| File | Content |
|---|---|
| `iNXT_BrokerVerse_User_Manual_v2.0.docx` | Edition 2.0 (Word master): every menu screen with its screenshot, fields, validation messages, buttons, business rules and statuses |
| `iNXT_BrokerVerse_User_Manual_v1.0.docx` | Edition 1.0, kept as issued |

The text is in [`tools/user-manual/content_v2.py`](../../tools/user-manual/content_v2.py) (edition 2.0) and
[`tools/user-manual/content.py`](../../tools/user-manual/content.py) (edition 1.0). The document is built by
[`tools/user-manual/build_user_manual.py`](../../tools/user-manual/build_user_manual.py):

```bash
pip install python-docx pillow pypdfium2
apt-get install -y libreoffice-writer          # paginates the table of contents
python tools/user-manual/build_user_manual.py --pdf                 # edition 2.0
python tools/user-manual/build_user_manual.py --edition 1.0 --pdf   # edition 1.0
```

`--pdf` keeps a PDF next to the Word file for circulation; PDFs are not committed.

## Screens and inventory (edition 2.0)

`tools/user-manual/screens/` holds the screen captures used in edition 2.0 and `inventory.json`, the record of
what each screen showed: menu path, address, screenshots per state (list, tabs, forms, record, validation),
columns, buttons, and every field with its type, required marker, list values and the exact validation message.

The screens were captured from the development site with a read-only browser walk (1600 x 1000): every menu
entry, each tab, each Add / Create form and the first record of each list were opened, and Save / Next / Submit
was pressed on empty forms to record the validation messages. Every write request to the API was blocked during
the walk, so no data was created or changed. Before each capture the sidebar logo was shown as the iorta TechNXT
logo and client names were replaced with neutral text, so the manual names no client.

The manual is kept product-level: it names no client, and screen names and button labels match the screens.
