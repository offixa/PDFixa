package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.writer.PdfSerializable;
import io.offixa.pdfixa.core.writer.PdfWriter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Tracks all indirect PDF objects through a two-phase lifecycle:
 * allocation (before content is known) then body assignment, followed by
 * a single serialization pass that captures byte offsets for the xref table.
 *
 * <p>Object numbers are assigned sequentially starting at 1; object 0 is
 * the PDF-reserved free-list head and is never allocated here.
 *
 * <p>This class is not thread-safe.
 */
public final class ObjectRegistry {

    private static final class Entry {
        final int objNum;
        PdfSerializable body;
        long offset;

        Entry(int objNum) {
            this.objNum = objNum;
        }
    }

    private final ArrayList<Entry> entries = new ArrayList<>();
    private int nextObjNum = 1;
    private int rootObjNum = -1;
    private boolean sealed = false;
    private long[] offsets;

    // ── Phase 1: allocation & body assignment ─────────────────────────

    /**
     * Reserves the next object number and returns it.
     * The body must be supplied later via {@link #setBody(int, PdfSerializable)}.
     */
    public int allocate() {
        if (sealed) {
            throw new IllegalStateException("registry is sealed; cannot allocate after writeAll()");
        }
        int objNum = nextObjNum++;
        entries.add(new Entry(objNum));
        return objNum;
    }

    /**
     * Assigns the serialization body to a previously allocated object.
     *
     * @param objNum object number returned by {@link #allocate()}
     * @param body   non-null serializer for the object's content tokens
     */
    public void setBody(int objNum, PdfSerializable body) {
        if (sealed) {
            throw new IllegalStateException("registry is sealed; cannot setBody after writeAll()");
        }
        if (objNum < 1 || objNum >= nextObjNum) {
            throw new IllegalArgumentException("unknown object number: " + objNum);
        }
        if (body == null) {
            throw new IllegalArgumentException("body must not be null");
        }
        entries.get(objNum - 1).body = body;
    }

    /**
     * Declares which object is the document catalog (PDF /Root).
     *
     * @param objNum object number of the catalog object
     */
    public void setRoot(int objNum) {
        if (objNum < 1 || objNum >= nextObjNum) {
            throw new IllegalArgumentException("unknown object number: " + objNum);
        }
        this.rootObjNum = objNum;
    }

    // ── Phase 2: one-shot serialization ───────────────────────────────

    /**
     * Writes every registered object to {@code writer} in allocation order,
     * recording each object's byte offset for later xref construction.
     *
     * <p>This method may only be called once. Subsequent calls throw.
     *
     * @throws IllegalStateException if root is unset or any object has a null body
     */
    public void writeAll(PdfWriter writer) throws IOException {
        if (sealed) {
            throw new IllegalStateException("writeAll() has already been called");
        }
        if (rootObjNum == -1) {
            throw new IllegalStateException("root object not set; call setRoot() before writeAll()");
        }
        for (Entry entry : entries) {
            if (entry.body == null) {
                throw new IllegalStateException("object " + entry.objNum + " has no body; call setBody() before writeAll()");
            }
        }

        sealed = true;

        offsets = new long[nextObjNum];
        offsets[0] = -1L;

        for (Entry entry : entries) {
            entry.offset = writer.beginObject(entry.objNum, 0);
            offsets[entry.objNum] = entry.offset;
            entry.body.writeTo(writer);
            writer.writeNewline();
            writer.endObject();
        }
    }

    // ── Post-serialization queries ─────────────────────────────────────

    /**
     * Returns the number of allocated objects (object 0 is not counted).
     * Callable before or after {@link #writeAll()}.
     */
    public int getObjectCount() {
        return nextObjNum - 1;
    }

    /**
     * Returns a copy of the offset array, indexed by object number.
     * {@code offsets[0]} is {@code -1} (unused); {@code offsets[i]} is the
     * byte offset of object {@code i}.
     *
     * @throws IllegalStateException if called before {@link #writeAll()}
     */
    public long[] getOffsets() {
        requireSealed();
        return offsets.clone();
    }

    /**
     * Returns the byte offset of the given object.
     *
     * @throws IllegalStateException if called before {@link #writeAll()}
     */
    public long getOffset(int objNum) {
        requireSealed();
        if (objNum < 1 || objNum >= nextObjNum) {
            throw new IllegalArgumentException("unknown object number: " + objNum);
        }
        return offsets[objNum];
    }

    /**
     * Returns the object number of the document catalog set via {@link #setRoot(int)}.
     *
     * @throws IllegalStateException if called before {@link #writeAll()}
     */
    public int getRootObjectNumber() {
        requireSealed();
        return rootObjNum;
    }

    private void requireSealed() {
        if (!sealed) {
            throw new IllegalStateException("call writeAll() first");
        }
    }
}
