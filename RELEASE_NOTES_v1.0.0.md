# PDFixa Core v1.0.0

**Deterministic, zero-dependency PDF engine for Java.**  
Byte-for-byte reproducible output. No bloat. No surprises.

---

## Highlights

- **Stable release** — API is frozen for the entire 1.x line.
- **Deterministic by design** — identical bytes on every run, every environment. SHA-256 `/ID`, no timestamps, no hidden runtime state.
- **Zero dependencies** — pure Java 17+. No transitive risk.
- **JPMS modular** — ships with `module-info.java`, safe for modular builds.
- **Production-ready** — used for invoices, reports, certificates and document pipelines.

---

## What's included

### Text
- Base-14 fonts (Helvetica, Times, Courier and all variants)
- Word-wrapping layout via `drawTextBox`
- Unicode raw API — UTF-16BE hex literals via `showTextUnicodeRaw`

### Images
- JPEG embedding — DCTDecode, zero re-compression
- PNG embedding — 8-bit RGB, FlateDecode with predictor

### Graphics
- Lines, rectangles, paths
- Fill, stroke, RGB and grayscale colors

### Document
- Standard page sizes: A3, A4, A5, Letter, Legal and custom
- Metadata builder — title, author, subject, keywords, dates
- Deterministic SHA-256 `/ID`
- PDF 1.7 compliant output

---

## Installation

**Maven**
```xml
<dependency>
    <groupId>io.offixa</groupId>
    <artifactId>pdfixa-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle**
```groovy
implementation 'io.offixa:pdfixa-core:1.0.0'
```

---

## Quick example

```java
PdfDocument doc = new PdfDocument();
PdfPage page = doc.addPage();

page.drawTextBox(72, 760, 468, 16, "Helvetica-Bold", 18, "Hello from PDFixa!");

try (var out = new FileOutputStream("hello.pdf")) {
    doc.save(out);
}
```

Same input → same bytes. Every time.

---

## Runnable examples

**10 self-contained example projects** — from hello world to paginated reports and Spring Boot HTTP delivery.

| Example | What it builds |
|---|---|
| `hello-world` | One page, one line of text |
| `invoice-generator` | Business invoice with line items and totals |
| `table-invoice` | Invoice with structured table |
| `table-report` | Analytics report with tabular data |
| `pagination-table-report` | Multi-page report, auto-pagination, repeated headers |
| `spring-boot-download` | Spring Boot endpoint that streams PDF on `GET` |

Clone and run in under a minute:

```bash
git clone https://github.com/offixa/pdfixa-examples
cd pdfixa-examples
mvn -pl hello-world exec:java -Dexec.mainClass="example.HelloWorldExample"
```

**[github.com/offixa/pdfixa-examples](https://github.com/offixa/pdfixa-examples)**

---

## Links

- **Source** — [github.com/offixa/PDFixa](https://github.com/offixa/PDFixa)
- **Examples** — [github.com/offixa/pdfixa-examples](https://github.com/offixa/pdfixa-examples)
- **Maven Central** — [io.offixa:pdfixa-core:1.0.0](https://central.sonatype.com/artifact/io.offixa/pdfixa-core)
- **Documentation** — [offixa.io/pdfixa](https://offixa.io/pdfixa)
- **PDFixa Pro** — [offixa.io/pdfixa-pro](https://offixa.io/pdfixa-pro)
