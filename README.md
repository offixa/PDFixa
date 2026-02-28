# PDFixa Core

Deterministic, zero-dependency PDF engine for Java backends.

## Features
- PDF 1.7 compatible
- Deterministic output
- Zero external dependencies
- Base-14 fonts (Latin-1)
- Basic word wrap (left aligned)
- JPEG / basic PNG images
- Page size configuration
- Metadata (/Info) + deterministic /ID

## Quick start

```java
PdfDocument doc = new PdfDocument();
PdfPage page = doc.addPage();

page.drawTextBox(
    72, 760,
    300,
    14,
    "Helvetica",
    12,
    "Hello from PDFixa OSS!"
);

try (FileOutputStream fos = new FileOutputStream("out.pdf")) {
    doc.save(fos);
}