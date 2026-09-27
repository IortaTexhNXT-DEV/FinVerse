# iNXT BrokerVerse User Manual

The customer-facing user manual of iNXT BrokerVerse, on the iorta TechNXT template (logo, blue and teal palette,
document control, contents, "Page x of y" footer).

| File | Content |
|---|---|
| `iNXT_BrokerVerse_User_Manual_v1.0.docx` | The manual (Word master) |

The text is in [`tools/user-manual/content.py`](../../tools/user-manual/content.py) and the document is built by
[`tools/user-manual/build_user_manual.py`](../../tools/user-manual/build_user_manual.py):

```bash
pip install python-docx pillow pypdfium2
apt-get install -y libreoffice-writer          # paginates the table of contents
python tools/user-manual/build_user_manual.py --pdf
```

`--pdf` keeps a PDF next to the Word file for circulation; PDFs are not committed. The manual is kept
product-level: it names no client, and screen names and button labels match the screens as built.
