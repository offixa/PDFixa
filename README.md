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

* Base-14 fonts
* Word wrapping
* `drawTextBox` API
* Unicode-aware API (UTF-16 literals)

### Graphics

* Paths and shapes
* Fill & stroke
* RGB and grayscale colors

### Images

* JPEG embedding
* PNG (8-bit RGB)

### Document

* Standard page sizes
* Metadata builder
* Deterministic SHA-256 `/ID`
* PDF 1.7 compliant output

---

## Examples

See full working examples:\
👉 https://github.com/offixa/pdfixa-examples

Example projects include:
* invoice-generator
* report-generator
* multi-language-pdf
* batch-pdf
* images-demo

Each example contains runnable code and generated PDFs.

---

## Core vs Pro

| Capability                                  | Core | Pro |
| ------------------------------------------- | ---- | --- |
| Deterministic output                        | ✅    | ✅   |
| Zero dependencies                           | ✅    | ✅   |
| Unicode-aware API                           | ✅    | ✅   |
| Full Unicode rendering (CIDFont, ToUnicode) | —    | ✅   |
| Font embedding                              | —    | ✅   |
| Font subsetting                             | —    | ✅   |
| Advanced layout engine                      | —    | ✅   |

Learn more about **PDFixa Pro**:

https://offixa.io/pdfixa-pro

---

## Documentation

Full documentation:

https://offixa.io/pdfixa

Documentation sections include:

* Getting Started
* Pages
* Fonts
* Images
* Metadata
* Deterministic output

---

## Requirements

Java **17+**

---

## License
Apache License 2.0
