package io.offixa.pdfixa.core.document;

/**
 * Immutable value object for PDF document information dictionary entries.
 *
 * <p>All fields are optional. {@code /Producer} is intentionally excluded and
 * is always written by {@link PdfDocument} as {@code (PDFixa)}.
 */
public final class PdfInfo {

    private final String title;
    private final String author;
    private final String subject;
    private final String keywords;
    private final String creator;
    private final String creationDate;
    private final String modDate;

    private PdfInfo(Builder builder) {
        this.title = builder.title;
        this.author = builder.author;
        this.subject = builder.subject;
        this.keywords = builder.keywords;
        this.creator = builder.creator;
        this.creationDate = builder.creationDate;
        this.modDate = builder.modDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getSubject() {
        return subject;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getCreator() {
        return creator;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getModDate() {
        return modDate;
    }

    public static final class Builder {
        private String title;
        private String author;
        private String subject;
        private String keywords;
        private String creator;
        private String creationDate;
        private String modDate;

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder keywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder creator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder creationDate(String creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder modDate(String modDate) {
            this.modDate = modDate;
            return this;
        }

        public PdfInfo build() {
            return new PdfInfo(this);
        }
    }
}
