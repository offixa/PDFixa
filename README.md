# PDFixa Core

**Deterministic, zero-dependency PDF engine for Java.** \
Byte-for-byte reproducible output. No bloat. No surprises.

[![CI](https://img.shields.io/github/actions/workflow/status/offixa/PDFixa/ci.yml?branch=master&label=CI&logo=githubactions&logoColor=white)](https://github.com/offixa/PDFixa/actions)
[![Java 17+](https://img.shields.io/badge/Java-17+-blue?logo=openjdk&logoColor=white)](#)
[![Dependencies: 0](https://img.shields.io/badge/Dependencies-0-brightgreen)](#)
[![JPMS](https://img.shields.io/badge/JPMS-modular-blueviolet)](#)
[![License](https://img.shields.io/badge/License-Apache_2.0-orange?logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Deterministic](https://img.shields.io/badge/Output-Deterministic-success)](#)
[![Maven Central](https://img.shields.io/maven-central/v/io.offixa/pdfixa-core?label=Maven%20Central&color=blue)](https://central.sonatype.com/artifact/io.offixa/pdfixa-core)

If you find PDFixa useful, consider ⭐ starring the repository.

---

## Quick Start

Generate a deterministic PDF in a few lines:

```java
PdfDocument doc = new PdfDocument();
PdfPage page = doc.addPage();

page.drawTextBox(
    72, 760, 468, 16,
    "Helvetica-Bold", 18,
    "Hello from PDFixa!"
);

try (var out = new FileOutputStream("hello.pdf")) {
    doc.save(out);
}
```

Output:

```
hello.pdf
```

> Want a runnable project right now? Clone the [examples repository](https://github.com/offixa/pdfixa-examples) and run:
> ```
> mvn -pl hello-world exec:java -Dexec.mainClass="example.HelloWorldExample"
> ```

---

## Installation

### Maven

```xml
<dependency>
    <groupId>io.offixa</groupId>
    <artifactId>pdfixa-core</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.offixa:pdfixa-core:1.1.0'
```

---

## What's New in 1.1.0

* **Latin-1 support** — `é`, `ñ`, `ü`, `ö`, `ç` and the full `U+0000–U+00FF` range work out of the box.
* **WinAnsiEncoding** declared in all Base-14 font dictionaries — compliant, zero-config.
* **Extended font metrics** — Helvetica and Times-Roman now carry correct Adobe AFM widths for all 256 WinAnsi code points.
* **Determinism preserved** — layout and byte output remain bit-for-bit reproducible.

## 1.0.0 Highlights

* **Stable release** — production-ready engine foundation.
* **API frozen** for the 1.x line.
* **Deterministic by design** — identical bytes on every run.
* **Unicode-aware API** — UTF-16 literal support.
* **Zero dependencies** — pure Java 17+.
* **JPMS modular design**.

---

## Why PDFixa?

PDFixa Core is a deterministic PDF engine built for **predictability, reproducibility and clean architecture**.

Every byte of output is determined entirely by your input.

No timestamps.
No UUIDs.
No hidden runtime state.

The same code always produces the same file — **bit for bit**.

### How it compares

| | PDFixa | iText | Apache PDFBox | OpenPDF |
|---|---|---|---|---|
| License | Apache 2.0 | AGPL / commercial | Apache 2.0 | LGPL |
| API style | Coordinate-based, simple | Powerful, complex | Low-level | iText 2 fork |
| Deterministic output | **Yes** | Depends on version | No guarantee | No guarantee |
| Predictable layout | Yes — you place everything | Partial (flow layout) | Manual | Partial |
| Pure Java, no native deps | Yes | Yes | Yes | Yes |
| Learning curve | Low | High | High | Medium |

**Choose PDFixa when** you need pixel-exact, reproducible PDFs (invoices, reports, certificates) and want plain Java without a DSL or template syntax.

**Choose something else when** you need to read/edit/sign existing PDFs (PDFBox) or need PDF/A, digital signatures, and forms (iText).

---

## Determinism Example

```java
byte[] first = generate();
byte[] second = generate();

assert Arrays.equals(first, second); // always true
```

Object numbers, cross-reference offsets and document `/ID` remain identical across runs.

This makes PDFixa ideal for:

* reproducible builds
* document pipelines
* deterministic testing
* compliance workflows

---

## Features

### Text

* Base-14 fonts (Helvetica, Times, Courier and variants)
* **Latin-1 support** — `WinAnsiEncoding` for accented characters (`é`, `ñ`, `ü`, `ö`, `ç`, full `U+0000–U+00FF`)
* Word wrapping with `drawTextBox`
* Unicode-aware API (UTF-16 hex literals via `showTextUnicodeRaw`)

### Graphics

* Paths and shapes (lines, rectangles)
* Fill & stroke
* RGB and grayscale colors

### Images

* JPEG embedding (DCTDecode, zero re-compression)
* PNG embedding (8-bit RGB, FlateDecode with predictor)

### Document

* Standard page sizes (A3, A4, A5, Letter, Legal) and custom sizes
* Metadata builder (title, author, subject, keywords, dates)
* Deterministic SHA-256 `/ID`
* PDF 1.7 compliant output

---

## Examples

**11 runnable example projects** covering real-world use cases — from hello world to multi-page paginated reports and Spring Boot HTTP delivery.

👉 **[github.com/offixa/pdfixa-examples](https://github.com/offixa/pdfixa-examples)**

| Module | What it builds | Run command |
|---|---|---|
| [hello-world](https://github.com/offixa/pdfixa-examples/tree/main/hello-world) | Minimal first PDF — one page, one line of text | `mvn -pl hello-world exec:java` |
| [invoice-generator](https://github.com/offixa/pdfixa-examples/tree/main/invoice-generator) | Business invoice with line items and totals | `mvn -pl invoice-generator exec:java` |
| [report-generator](https://github.com/offixa/pdfixa-examples/tree/main/report-generator) | Multi-section report with body text and footer | `mvn -pl report-generator exec:java` |
| [multi-language-pdf](https://github.com/offixa/pdfixa-examples/tree/main/multi-language-pdf) | 8 Latin-script languages in one document | `mvn -pl multi-language-pdf exec:java` |
| [batch-pdf](https://github.com/offixa/pdfixa-examples/tree/main/batch-pdf) | Generate 10 PDFs in a loop | `mvn -pl batch-pdf exec:java` |
| [images-demo](https://github.com/offixa/pdfixa-examples/tree/main/images-demo) | PNG and JPEG images with position and size control | `mvn -pl images-demo exec:java` |
| [table-invoice](https://github.com/offixa/pdfixa-examples/tree/main/table-invoice) | Invoice with a structured table of items | `mvn -pl table-invoice exec:java` |
| [table-report](https://github.com/offixa/pdfixa-examples/tree/main/table-report) | Analytics report with tabular data | `mvn -pl table-report exec:java` |
| [pagination-table-report](https://github.com/offixa/pdfixa-examples/tree/main/pagination-table-report) | Multi-page report with auto-pagination and repeated headers | `mvn -pl pagination-table-report exec:java` |
| [spring-boot-download](https://github.com/offixa/pdfixa-examples/tree/main/spring-boot-download) | Spring Boot endpoint that returns PDF on `GET` | `mvn -pl spring-boot-download spring-boot:run` |
| [latin1-demo](https://github.com/offixa/pdfixa-examples/tree/main/latin1-demo) ⭐ **New in 1.1.0** | Latin-1 characters with WinAnsiEncoding — accented text across French, Spanish, Turkish, German and more | `mvn -pl latin1-demo exec:java` |

### Visual previews

| | | |
|---|---|---|
| ![hello-world](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/hello-world.png) | ![invoice-generator](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/invoice-generator.png) | ![table-invoice](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/table-invoice.png) |
| hello-world | invoice-generator | table-invoice |
| ![table-report](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/table-report.png) | ![pagination-table-report](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/pagination-table-report.png) | ![spring-boot-download](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/spring-boot-download.png) |
| table-report | pagination-table-report | spring-boot-download |
| ![latin1-demo](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/latin1-demo.png) | ![multi-language-pdf](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/multi-language-pdf.png) | |
| latin1-demo | multi-language-pdf | |

> Each example is self-contained and runnable in under a minute. Clone, run, open the PDF.

---

## Mental Model

PDFixa has three building blocks:

```
PdfDocument            ← the file itself
 └─ PdfPage            ← one page inside the document
     └─ ContentStream  ← low-level drawing surface (lines, shapes)
```

| Concept | What it is | How you get it |
|---|---|---|
| `PdfDocument` | The PDF file in memory | `new PdfDocument()` |
| `PdfPage` | A single page | `doc.addPage()` |
| `ContentStream` | Raw drawing commands for a page | `page.getContent()` |
| `save()` | Writes the finished file | `doc.save(outputStream)` |

For most tasks you only need `PdfDocument` and `PdfPage`.
`ContentStream` is used when you need to draw lines, shapes or place images directly.

---

## Core vs Pro

| Capability | Core | Pro |
|---|---|---|
| Deterministic output | ✅ | ✅ |
| Zero dependencies | ✅ | ✅ |
| Unicode-aware API | ✅ | ✅ |
| Latin-1 / WinAnsiEncoding (`U+0000–U+00FF`) | ✅ | ✅ |
| Full Unicode rendering (CIDFont, ToUnicode) | — | ✅ |
| Font embedding | — | ✅ |
| Font subsetting | — | ✅ |
| Advanced layout engine | — | ✅ |

PDFixa Pro is a commercial extension built on top of Core. See the [comparison table above](#core-vs-pro).

---

## Documentation

### Getting Started

Add the dependency, create a `PdfDocument`, add a page, draw content, save:

```java
PdfDocument doc = new PdfDocument();
PdfPage page = doc.addPage();

page.drawTextBox(72, 760, 468, 16, "Helvetica-Bold", 18, "Hello from PDFixa!");

try (var out = new FileOutputStream("output.pdf")) {
    doc.save(out);
}
```

### Pages and Page Sizes

```java
// Standard sizes
PdfPage a4     = doc.addPage(PdfPageSize.A4);
PdfPage letter = doc.addPage(PdfPageSize.LETTER);
PdfPage a3     = doc.addPage(PdfPageSize.A3);

// Custom size (width x height in points, 1pt = 1/72 inch)
PdfPage custom = doc.addPage(new PdfPageSize(595, 842));
```

### Fonts (Base-14)

PDFixa supports the 14 built-in PDF fonts — no embedding required.

| Font name | Variants |
|---|---|
| `Helvetica` | `Helvetica-Bold`, `Helvetica-Oblique`, `Helvetica-BoldOblique` |
| `Times-Roman` | `Times-Bold`, `Times-Italic`, `Times-BoldItalic` |
| `Courier` | `Courier-Bold`, `Courier-Oblique`, `Courier-BoldOblique` |
| `Symbol` | — |
| `ZapfDingbats` | — |

```java
page.drawTextBox(72, 700, 400, 14, "Times-Bold", 14, "Section title");
page.drawTextBox(72, 680, 400, 12, "Helvetica",  11, "Body paragraph text.");
```

### Images (JPEG and PNG)

```java
// Embed a JPEG (DCTDecode — zero re-compression)
byte[] jpegBytes = Files.readAllBytes(Path.of("photo.jpg"));
page.drawImage(jpegBytes, PdfImageType.JPEG, 72, 500, 200, 150);

// Embed a PNG (8-bit RGB, FlateDecode)
byte[] pngBytes = Files.readAllBytes(Path.of("logo.png"));
page.drawImage(pngBytes, PdfImageType.PNG, 72, 300, 100, 100);
```

Parameters: `(imageBytes, type, x, y, width, height)` — coordinates in points from the bottom-left.

### Metadata

```java
PdfDocument doc = new PdfDocument(
    PdfInfo.builder()
        .title("Q1 Sales Report")
        .author("Offixa")
        .subject("Monthly analytics")
        .keywords("sales, report, 2026")
        .build()
);
```

### Latin-1 Characters (WinAnsiEncoding)

PDFixa 1.1.0 supports accented Latin characters out of the box — no configuration needed:

```java
page.getContent()
    .beginText()
    .setFont("Helvetica", 12)
    .moveText(72, 700)
    .showText("café résumé niño über könnten français")
    .endText();
```

```java
// Works with drawTextBox too
page.drawTextBox(72, 700, 450, 14, "Times-Roman", 11,
    "Résumé — Español — Türkçe — Français — Português");
```

Supported range: `U+0000–U+00FF` (full WinAnsi / Latin-1).
Characters in `U+0080–U+009F` (C1 control range) are rejected — consistent with the WinAnsi standard.

> Full Unicode (Cyrillic, Arabic, CJK) is available in **PDFixa Pro**.

---

### Deterministic Output

PDFixa guarantees byte-for-byte identical output for identical input:

```java
byte[] first  = generatePdf();
byte[] second = generatePdf();

assert Arrays.equals(first, second); // always true
```

What is fixed across runs:
- PDF object numbers and byte offsets
- Document `/ID` — SHA-256 hash of content
- No embedded timestamps
- No random UUIDs or runtime state

---

## Requirements

Java **17+**

---

## License

Apache License 2.0
