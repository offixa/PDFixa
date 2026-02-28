package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.internal.ObjectRegistry;
import io.offixa.pdfixa.core.internal.PdfWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ObjectRegistryTest {

    private ByteArrayOutputStream baos;
    private PdfWriter writer;
    private ObjectRegistry registry;

    @BeforeEach
    void setUp() {
        baos = new ByteArrayOutputStream();
        writer = new PdfWriter(baos);
        registry = new ObjectRegistry();
    }

    private String output() {
        return baos.toString(StandardCharsets.US_ASCII);
    }

    // ── allocate() ─────────────────────────────────────────────────

    @Nested
    class Allocation {

        @Test
        void firstAllocateReturnsOne() {
            assertEquals(1, registry.allocate());
        }

        @Test
        void allocateReturnsSequentialNumbers() {
            assertEquals(1, registry.allocate());
            assertEquals(2, registry.allocate());
            assertEquals(3, registry.allocate());
        }

        @Test
        void objectCountStartsAtZero() {
            assertEquals(0, registry.getObjectCount());
        }

        @Test
        void objectCountReflectsEachAllocation() {
            registry.allocate();
            assertEquals(1, registry.getObjectCount());
            registry.allocate();
            assertEquals(2, registry.getObjectCount());
        }

        @Test
        void allocateAfterSealedThrows() throws IOException {
            int num = registry.allocate();
            registry.setBody(num, w -> w.writeInt(1));
            registry.setRoot(num);
            registry.writeAll(writer);
            assertThrows(IllegalStateException.class, () -> registry.allocate());
        }
    }

    // ── setBody() ──────────────────────────────────────────────────

    @Nested
    class SetBody {

        @Test
        void nullBodyThrows() {
            int num = registry.allocate();
            assertThrows(IllegalArgumentException.class, () -> registry.setBody(num, null));
        }

        @Test
        void unknownObjNumThrows() {
            assertThrows(IllegalArgumentException.class, () -> registry.setBody(99, w -> {}));
        }

        @Test
        void setBodyAfterSealedThrows() throws IOException {
            int num = registry.allocate();
            registry.setBody(num, w -> w.writeInt(1));
            registry.setRoot(num);
            registry.writeAll(writer);
            assertThrows(IllegalStateException.class, () -> registry.setBody(num, w -> {}));
        }
    }

    // ── setRoot() ──────────────────────────────────────────────────

    @Nested
    class SetRoot {

        @Test
        void unknownObjNumThrows() {
            assertThrows(IllegalArgumentException.class, () -> registry.setRoot(99));
        }

        @Test
        void objNumZeroThrows() {
            assertThrows(IllegalArgumentException.class, () -> registry.setRoot(0));
        }
    }

    // ── writeAll() ─────────────────────────────────────────────────

    @Nested
    class WriteAll {

        @Test
        void withoutRootThrows() {
            registry.allocate();
            assertThrows(IllegalStateException.class, () -> registry.writeAll(writer));
        }

        @Test
        void withUnsetBodyThrows() {
            int first = registry.allocate();
            registry.allocate(); // second object — body intentionally omitted
            registry.setBody(first, w -> w.writeInt(1));
            registry.setRoot(first);
            assertThrows(IllegalStateException.class, () -> registry.writeAll(writer));
        }

        @Test
        void calledTwiceThrows() throws IOException {
            int num = registry.allocate();
            registry.setBody(num, w -> w.writeInt(1));
            registry.setRoot(num);
            registry.writeAll(writer);
            assertThrows(IllegalStateException.class, () -> registry.writeAll(writer));
        }

        @Test
        void singleObjectOutputSyntax() throws IOException {
            int num = registry.allocate();
            registry.setBody(num, w -> w.writeInt(42));
            registry.setRoot(num);
            registry.writeAll(writer);
            // beginObject writes "1 0 obj\n", body writes "42",
            // writeNewline() writes "\n", endObject writes "endobj\n"
            assertEquals("1 0 obj\n42\nendobj\n", output());
        }

        @Test
        void multipleObjectsWrittenInRegistrationOrder() throws IOException {
            int a = registry.allocate();
            int b = registry.allocate();
            registry.setBody(a, w -> w.writeInt(1));
            registry.setBody(b, w -> w.writeInt(2));
            registry.setRoot(a);
            registry.writeAll(writer);
            assertEquals(
                    "1 0 obj\n1\nendobj\n" +
                    "2 0 obj\n2\nendobj\n",
                    output());
        }

        @Test
        void offsetsAreCapturedCorrectly() throws IOException {
            int a = registry.allocate();
            int b = registry.allocate();
            registry.setBody(a, w -> w.writeInt(1));
            registry.setBody(b, w -> w.writeInt(2));
            registry.setRoot(a);
            registry.writeAll(writer);
            // "1 0 obj\n" (8) + "1" (1) + "\n" (1) + "endobj\n" (7) = 17 bytes
            assertEquals(0L,  registry.getOffset(1));
            assertEquals(17L, registry.getOffset(2));
        }

        @Test
        void offsetsArrayIndexZeroIsMinusOne() throws IOException {
            int num = registry.allocate();
            registry.setBody(num, w -> w.writeInt(0));
            registry.setRoot(num);
            registry.writeAll(writer);
            assertEquals(-1L, registry.getOffsets()[0]);
        }

        @Test
        void getOffsetsReturnsCopyNotInternalArray() throws IOException {
            int num = registry.allocate();
            registry.setBody(num, w -> w.writeInt(0));
            registry.setRoot(num);
            registry.writeAll(writer);
            long[] copy = registry.getOffsets();
            copy[1] = 999_999L; // mutate the copy
            assertEquals(0L, registry.getOffset(1)); // original must be unchanged
        }
    }

    // ── Post-serialization queries ─────────────────────────────────

    @Nested
    class PostSerializationQueries {

        @Test
        void getOffsetsBeforeWriteAllThrows() {
            assertThrows(IllegalStateException.class, () -> registry.getOffsets());
        }

        @Test
        void getOffsetBeforeWriteAllThrows() {
            registry.allocate();
            assertThrows(IllegalStateException.class, () -> registry.getOffset(1));
        }

        @Test
        void getRootObjectNumberBeforeWriteAllThrows() {
            int num = registry.allocate();
            registry.setRoot(num);
            assertThrows(IllegalStateException.class, () -> registry.getRootObjectNumber());
        }

        @Test
        void getRootObjectNumberReturnsSetRoot() throws IOException {
            int num = registry.allocate();
            registry.setBody(num, w -> w.writeInt(0));
            registry.setRoot(num);
            registry.writeAll(writer);
            assertEquals(num, registry.getRootObjectNumber());
        }

        @Test
        void getOffsetWithInvalidObjNumThrows() throws IOException {
            int num = registry.allocate();
            registry.setBody(num, w -> w.writeInt(0));
            registry.setRoot(num);
            registry.writeAll(writer);
            assertThrows(IllegalArgumentException.class, () -> registry.getOffset(99));
        }

        @Test
        void getObjectCountIsStableAfterSealing() throws IOException {
            registry.allocate();
            registry.allocate();
            int num = registry.allocate();
            registry.setBody(1, w -> w.writeInt(1));
            registry.setBody(2, w -> w.writeInt(2));
            registry.setBody(num, w -> w.writeInt(3));
            registry.setRoot(1);
            registry.writeAll(writer);
            assertEquals(3, registry.getObjectCount());
        }
    }
}
