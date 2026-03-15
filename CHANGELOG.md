# Changelog

## 1.0.0 — 2026-03-15

**First stable release.**

- API frozen for the 1.x line
- Deterministic SHA-256 `/ID` — byte-for-byte reproducible output across all runs and environments
- Base-14 fonts with full metrics and word-wrapping via `drawTextBox`
- Unicode raw API (`showTextUnicodeRaw` — UTF-16 hex literals)
- JPEG embedding (DCTDecode, zero re-compression)
- PNG embedding (8-bit RGB, FlateDecode with predictor)
- Standard page sizes: A3, A4, A5, Letter, Legal and custom sizes
- Metadata builder: title, author, subject, keywords, dates
- RGB and grayscale color support
- Paths, lines and rectangles via `ContentStream`
- JPMS modular design (`module-info.java`)
- Zero runtime dependencies — pure Java 17+
- PDF 1.7 compliant output

## 0.7.0

- Deterministic /ID support
- Metadata (/Info)
- Base-14 font metrics
- Minimal word wrapping
- drawTextBox API
- Page size parametrization
- Memory hygiene improvements
- Encoding determinism fix