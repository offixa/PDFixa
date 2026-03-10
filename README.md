# PDFixa Core

**Deterministic, zero-dependency PDF engine for Java.** \
Byte-for-byte reproducible output. No bloat. No surprises.

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
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.offixa:pdfixa-core:1.0.0'
```

---

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

**10 runnable example projects** covering real-world use cases — from hello world to multi-page paginated reports and Spring Boot HTTP delivery.

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

### Visual previews

| | | |
|---|---|---|
| ![hello-world](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/hello-world.png) | ![invoice-generator](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/invoice-generator.png) | ![table-invoice](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/table-invoice.png) |
| hello-world | invoice-generator | table-invoice |
| ![table-report](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/table-report.png) | ![pagination-table-report](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/pagination-table-report.png) | ![spring-boot-download](https://raw.githubusercontent.com/offixa/pdfixa-examples/main/previews/spring-boot-download.png) |
| table-report | pagination-table-report | spring-boot-download |

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
| Full Unicode rendering (CIDFont, ToUnicode) | — | ✅ |
| Font embedding | — | ✅ |
| Font subsetting | — | ✅ |
| Advanced layout engine | — | ✅ |

Learn more about **PDFixa Pro**: https://offixa.io/pdfixa-pro

---

## Documentation

Full documentation: https://offixa.io/pdfixa

* Getting Started
* Pages & Page Sizes
* Fonts (Base-14)
* Images (JPEG & PNG)
* Metadata
* Deterministic Output

---

## Requirements

Java **17+**

---

## License

Apache License 2.0
