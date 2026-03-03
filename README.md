# PDFixa Core

**Deterministic, zero-dependency PDF engine foundation for Java.**
Byte-for-byte reproducible output. No bloat. No surprises.

[![Java 17+](https://img.shields.io/badge/Java-17+-blue?logo=openjdk&logoColor=white)](#)
[![Dependencies: 0](https://img.shields.io/badge/Dependencies-0-brightgreen)](#)
[![JPMS](https://img.shields.io/badge/JPMS-modular-blueviolet)](#)
[![License](https://img.shields.io/badge/License-Apache_2.0-orange?logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Deterministic](https://img.shields.io/badge/Output-Deterministic-success)](#)
[![Maven Central](https://img.shields.io/maven-central/v/io.offixa/pdfixa-core?label=Maven%20Central&color=blue)](https://central.sonatype.com/artifact/io.offixa/pdfixa-core)

---

## Why PDFixa?

PDFixa Core is a deterministic PDF engine built for predictability, reproducibility and clean architecture.

### Key Guarantees

- Byte-for-byte deterministic output
- Zero runtime dependencies (pure JDK 17+)
- JPMS modular design
- No hidden timestamps or UUID pollution
- Strict lifecycle (allocate → write → seal)

---

## Determinism Example

```java
byte[] first = generate();
byte[] second = generate();

assert Arrays.equals(first, second); // Always true
```

Object numbers, xref offsets and `/ID` remain identical across runs.

---

## Quick Start

### Maven

```xml
<dependency>
    <groupId>io.offixa</groupId>
    <artifactId>pdfixa-core</artifactId>
    <version>0.8.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.offixa:pdfixa-core:0.8.0'
```

### Example

```java
PdfDocument doc = new PdfDocument();
PdfPage page = doc.addPage();

page.drawTextBox(
    72, 760, 468, 16,
    "Helvetica-Bold", 18,
    "Hello from PDFixa Core!"
);

try (var out = new FileOutputStream("hello.pdf")) {
    doc.save(out);
}
```

---

## Features

### Text

- Base-14 fonts
- Word wrapping
- drawTextBox API
- Unicode-aware API (UTF-16 literal support)

### Graphics

- Paths and shapes
- Fill & stroke
- RGB & grayscale

### Images

- JPEG embedding
- PNG (8-bit RGB)

### Document

- Standard page sizes
- Metadata builder
- SHA-256 deterministic `/ID`
- PDF 1.7 compliant

---

## Core vs Pro

| Capability | Core | Pro |
|:---|:---:|:---:|
| Deterministic output | Yes | Yes |
| Zero dependencies | Yes | Yes |
| Unicode-aware API | Yes | Yes |
| Full Unicode rendering | — | Yes |
| Font embedding | — | Yes |
| Font subsetting | — | Yes |
| Advanced layout engine | — | Yes |

> **[Get PDFixa Pro → offixa.io](https://offixa.io)**

---

## Requirements

Java 17+

## License

Apache License 2.0
