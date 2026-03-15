# Offixa

**Deterministic document engines for backend developers.**

We build libraries that turn your data into pixel-exact, byte-for-byte reproducible documents — with zero dependencies and no runtime surprises.

---

## Products

### PDFixa Core

Deterministic PDF generation library for Java. Zero dependencies. Byte-for-byte reproducible output.

[![Maven Central](https://img.shields.io/maven-central/v/io.offixa/pdfixa-core?label=Maven%20Central&color=blue)](https://central.sonatype.com/artifact/io.offixa/pdfixa-core)
[![Java 17+](https://img.shields.io/badge/Java-17+-blue?logo=openjdk&logoColor=white)](#)
[![License](https://img.shields.io/badge/License-Apache_2.0-orange?logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)

```xml
<dependency>
    <groupId>io.offixa</groupId>
    <artifactId>pdfixa-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

**[View repository →](https://github.com/offixa/PDFixa)**

### PDFixa Examples

10 runnable example projects — invoices, reports, tables, pagination, images, Spring Boot HTTP delivery.

**[View repository →](https://github.com/offixa/pdfixa-examples)**

### PDFixa Pro

Full Unicode rendering, font embedding, font subsetting and an advanced layout engine — built on top of Core.

**[Learn more →](https://offixa.io/pdfixa-pro)**

---

## Start here

```java
PdfDocument doc = new PdfDocument();
PdfPage page = doc.addPage();

page.drawTextBox(72, 760, 468, 16, "Helvetica-Bold", 18, "Hello from PDFixa!");

try (var out = new FileOutputStream("hello.pdf")) {
    doc.save(out);
}
```

Same input → same bytes. Every time.

**Next steps:**

| I want to… | Go to |
|---|---|
| Install the library | [PDFixa Core → Installation](https://github.com/offixa/PDFixa#installation) |
| Run a working example | [pdfixa-examples → Getting Started](https://github.com/offixa/pdfixa-examples#getting-started) |
| Generate an invoice | [invoice-generator example](https://github.com/offixa/pdfixa-examples/tree/main/invoice-generator) |
| Return PDF from Spring Boot | [spring-boot-download example](https://github.com/offixa/pdfixa-examples/tree/main/spring-boot-download) |
| Compare Core vs Pro | [Feature comparison](https://github.com/offixa/PDFixa#core-vs-pro) |
| Read documentation | [offixa.io/pdfixa](https://offixa.io/pdfixa) |

---

## Links

- [Documentation](https://offixa.io/pdfixa)
- [Maven Central](https://central.sonatype.com/artifact/io.offixa/pdfixa-core)
- [PDFixa Pro](https://offixa.io/pdfixa-pro)
