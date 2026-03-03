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

## 1.0.0 Highlights

- **Stable release** — production-ready engine foundation.
- **API frozen** for the 1.x line; public API compatibility is guaranteed.
- **Deterministic by design** — identical bytes on every run, every platform.
- **Unicode-aware** — UTF-16 literal support in Core; full Unicode rendering available in Pro.
- **Zero dependencies, JPMS modular** — drop-in for modular and non-modular projects alike.

---

## Why PDFixa?

PDFixa Core is a deterministic PDF engine built for predictability, reproducibility and clean architecture.
Every byte of output is fully determined by your input — no timestamps, no UUIDs, no ambient state.
The same code always produces the same file, bit for bit.

### Key Guarantees

- **Byte-for-byte deterministic output** — object numbers, xref offsets and `/ID` are stable across runs, platforms and JVM versions.
- Zero runtime dependencies (pure JDK 17+)
- JPMS modular design (`io.offixa.pdfixa.core`)
- No hidden timestamps or UUID pollution
- Strict lifecycle (allocate → write → seal)

---

## API Stability

PDFixa Core 1.0.0 marks the beginning of the stable 1.x line.
All public API surfaces are frozen: **source-compatible changes only** for future 1.x releases.
You can depend on `pdfixa-core` in production without risk of breaking upgrades within the 1.x series.

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
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.offixa:pdfixa-core:1.0.0'
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
| Unicode-aware API (UTF-16 literals) | Yes | Yes |
| Full Unicode rendering (CIDFont, ToUnicode) | — | Yes |
| Font embedding | — | Yes |
| Font subsetting | — | Yes |
| Advanced layout engine | — | Yes |

> **[Get PDFixa Pro → offixa.io](https://offixa.io)**

---

## Requirements

Java 17+

## License

Apache License 2.0
