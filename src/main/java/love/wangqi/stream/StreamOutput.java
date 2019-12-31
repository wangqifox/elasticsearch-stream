package love.wangqi.stream;

import love.wangqi.common.ArrayUtil;
import love.wangqi.common.BitUtil;
import love.wangqi.common.Nullable;
import love.wangqi.stream.Writeable.Writer;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.IntFunction;

/**
 * @author: wangqi
 * @description:
 * @Version:
 * @date: Created in 2019/12/31 2:20 下午
 */
public abstract class StreamOutput extends OutputStream {
    public long position() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void seek(long position) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes a single byte.
     */
    public abstract void writeByte(byte b) throws IOException;

    /**
     * Writes an array of bytes.
     *
     * @param b the bytes to write
     */
    public void writeBytes(byte[] b) throws IOException {
        writeBytes(b, 0, b.length);
    }

    /**
     * Writes an array of bytes.
     *
     * @param b      the bytes to write
     * @param length the number of bytes to write
     */
    public void writeBytes(byte[] b, int length) throws IOException {
        writeBytes(b, 0, length);
    }

    /**
     * Writes an array of bytes.
     *
     * @param b      the bytes to write
     * @param offset the offset in the byte array
     * @param length the number of bytes to write
     */
    public abstract void writeBytes(byte[] b, int offset, int length) throws IOException;

    /**
     * Writes an array of bytes.
     *
     * @param b the bytes to write
     */
    public void writeByteArray(byte[] b) throws IOException {
        writeVInt(b.length);
        writeBytes(b, 0, b.length);
    }

    public final void writeShort(short v) throws IOException {
        writeByte((byte) (v >> 8));
        writeByte((byte) v);
    }

    /**
     * Writes an int as four bytes.
     */
    public void writeInt(int i) throws IOException {
        writeByte((byte) (i >> 24));
        writeByte((byte) (i >> 16));
        writeByte((byte) (i >> 8));
        writeByte((byte) i);
    }

    /**
     * Writes an int in a variable-length format.  Writes between one and
     * five bytes.  Smaller values take fewer bytes.  Negative numbers
     * will always use all 5 bytes and are therefore better serialized
     * using {@link #writeInt}
     */
    public void writeVInt(int i) throws IOException {
        while ((i & ~0x7F) != 0) {
            writeByte((byte) ((i & 0x7f) | 0x80));
            i >>>= 7;
        }
        writeByte((byte) i);
    }

    /**
     * Writes a long as eight bytes.
     */
    public void writeLong(long i) throws IOException {
        writeInt((int) (i >> 32));
        writeInt((int) i);
    }

    /**
     * Writes a non-negative long in a variable-length format. Writes between one and ten bytes. Smaller values take fewer bytes. Negative
     * numbers use ten bytes and trip assertions (if running in tests) so prefer {@link #writeLong(long)} or {@link #writeZLong(long)} for
     * negative numbers.
     */
    public void writeVLong(long i) throws IOException {
        if (i < 0) {
            throw new IllegalStateException("Negative longs unsupported, use writeLong or writeZLong for negative numbers [" + i + "]");
        }
        writeVLongNoCheck(i);
    }

    /**
     * Writes a long in a variable-length format without first checking if it is negative. Package private for testing. Use
     * {@link #writeVLong(long)} instead.
     */
    void writeVLongNoCheck(long i) throws IOException {
        while ((i & ~0x7F) != 0) {
            writeByte((byte) ((i & 0x7f) | 0x80));
            i >>>= 7;
        }
        writeByte((byte) i);
    }

    /**
     * Writes a long in a variable-length format. Writes between one and ten bytes.
     * Values are remapped by sliding the sign bit into the lsb and then encoded as an unsigned number
     * e.g., 0 -;&gt; 0, -1 -;&gt; 1, 1 -;&gt; 2, ..., Long.MIN_VALUE -;&gt; -1, Long.MAX_VALUE -;&gt; -2
     * Numbers with small absolute value will have a small encoding
     * If the numbers are known to be non-negative, use {@link #writeVLong(long)}
     */
    public void writeZLong(long i) throws IOException {
        // zig-zag encoding cf. https://developers.google.com/protocol-buffers/docs/encoding?hl=en
        long value = BitUtil.zigZagEncode(i);
        while ((value & 0xFFFFFFFFFFFFFF80L) != 0L) {
            writeByte((byte)((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        writeByte((byte) (value & 0x7F));
    }

    public void writeOptionalLong(@Nullable Long l) throws IOException {
        if (l == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeLong(l);
        }
    }

    public void writeOptionalString(@Nullable String str) throws IOException {
        if (str == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeString(str);
        }
    }

    /**
     * Writes an optional {@link Integer}.
     */
    public void writeOptionalInt(@Nullable Integer integer) throws IOException {
        if (integer == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeInt(integer);
        }
    }

    public void writeOptionalVInt(@Nullable Integer integer) throws IOException {
        if (integer == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeVInt(integer);
        }
    }

    public void writeOptionalFloat(@Nullable Float floatValue) throws IOException {
        if (floatValue == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeFloat(floatValue);
        }
    }

    private byte[] convertStringBuffer = new byte[0]; // TODO should we reduce it to 0 bytes once the stream is closed?

    public void writeString(String str) throws IOException {
        final int charCount = str.length();
        final int bufferSize = Math.min(3 * charCount, 1024); // at most 3 bytes per character is needed here
        if (convertStringBuffer.length < bufferSize) { // we don't use ArrayUtils.grow since copying the bytes is unnecessary
            convertStringBuffer = new byte[ArrayUtil.oversize(bufferSize, Byte.BYTES)];
        }
        byte[] buffer = convertStringBuffer;
        int offset = 0;
        writeVInt(charCount);
        for (int i = 0; i < charCount; i++) {
            final int c = str.charAt(i);
            if (c <= 0x007F) {
                buffer[offset++] = ((byte) c);
            } else if (c > 0x07FF) {
                buffer[offset++] = ((byte) (0xE0 | c >> 12 & 0x0F));
                buffer[offset++] = ((byte) (0x80 | c >> 6 & 0x3F));
                buffer[offset++] = ((byte) (0x80 | c >> 0 & 0x3F));
            } else {
                buffer[offset++] = ((byte) (0xC0 | c >> 6 & 0x1F));
                buffer[offset++] = ((byte) (0x80 | c >> 0 & 0x3F));
            }
            // make sure any possible char can fit into the buffer in any possible iteration
            // we need at most 3 bytes so we flush the buffer once we have less than 3 bytes
            // left before we start another iteration
            if (offset > buffer.length - 3) {
                writeBytes(buffer, offset);
                offset = 0;
            }
        }
        writeBytes(buffer, offset);
    }

    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeOptionalDouble(@Nullable Double v) throws IOException {
        if (v == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeDouble(v);
        }
    }

    private static byte ZERO = 0;
    private static byte ONE = 1;
    private static byte TWO = 2;

    /**
     * Writes a boolean.
     */
    public void writeBoolean(boolean b) throws IOException {
        writeByte(b ? ONE : ZERO);
    }

    public void writeOptionalBoolean(@Nullable Boolean b) throws IOException {
        if (b == null) {
            writeByte(TWO);
        } else {
            writeBoolean(b);
        }
    }

    /**
     * Forces any buffered output to be written.
     */
    @Override
    public abstract void flush() throws IOException;

    /**
     * Closes this stream to further operations.
     */
    @Override
    public abstract void close() throws IOException;

    public abstract void reset() throws IOException;

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeBytes(b, off, len);
    }

    public void writeStringArray(String[] array) throws IOException {
        writeVInt(array.length);
        for (String s : array) {
            writeString(s);
        }
    }

    /**
     * Writes a string array, for nullable string, writes it as 0 (empty string).
     */
    public void writeStringArrayNullable(@Nullable String[] array) throws IOException {
        if (array == null) {
            writeVInt(0);
        } else {
            writeVInt(array.length);
            for (String s : array) {
                writeString(s);
            }
        }
    }

    /**
     * Writes a string array, for nullable string, writes false.
     */
    public void writeOptionalStringArray(@Nullable String[] array) throws IOException {
        if (array == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeStringArray(array);
        }
    }

    public void writeMap(@Nullable Map<String, Object> map) throws IOException {
        writeGenericValue(map);
    }

    /**
     * write map to stream with consistent order
     * to make sure every map generated bytes order are same.
     * This method is compatible with {@code StreamInput.readMap} and {@code StreamInput.readGenericValue}
     * This method only will handle the map keys order, not maps contained within the map
     */
    public void writeMapWithConsistentOrder(@Nullable Map<String, ? extends Object> map)
            throws IOException {
        if (map == null) {
            writeByte((byte) -1);
            return;
        }
        assert false == (map instanceof LinkedHashMap);
        this.writeByte((byte) 10);
        this.writeVInt(map.size());
        Iterator<? extends Map.Entry<String, ?>> iterator =
                map.entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ?> next = iterator.next();
            this.writeString(next.getKey());
            this.writeGenericValue(next.getValue());
        }
    }

    /**
     * Write a {@link Map} of {@code K}-type keys to {@code V}-type {@link List}s.
     * <pre><code>
     * Map&lt;String, List&lt;String&gt;&gt; map = ...;
     * out.writeMapOfLists(map, StreamOutput::writeString, StreamOutput::writeString);
     * </code></pre>
     *
     * @param keyWriter The key writer
     * @param valueWriter The value writer
     */
    public final <K, V> void writeMapOfLists(final Map<K, List<V>> map, final Writer<K> keyWriter, final Writer<V> valueWriter)
            throws IOException {
        writeMap(map, keyWriter, (stream, list) -> {
            writeVInt(list.size());
            for (final V value : list) {
                valueWriter.write(this, value);
            }
        });
    }

    /**
     * Write a {@link Map} of {@code K}-type keys to {@code V}-type.
     * <pre><code>
     * Map&lt;String, String&gt; map = ...;
     * out.writeMap(map, StreamOutput::writeString, StreamOutput::writeString);
     * </code></pre>
     *
     * @param keyWriter The key writer
     * @param valueWriter The value writer
     */
    public final <K, V> void writeMap(final Map<K, V> map, final Writer<K> keyWriter, final Writer<V> valueWriter)
            throws IOException {
        writeVInt(map.size());
        for (final Map.Entry<K, V> entry : map.entrySet()) {
            keyWriter.write(this, entry.getKey());
            valueWriter.write(this, entry.getValue());
        }
    }

    /**
     * Writes an {@link Instant} to the stream with nanosecond resolution
     */
    public final void writeInstant(Instant instant) throws IOException {
        writeLong(instant.getEpochSecond());
        writeInt(instant.getNano());
    }

    /**
     * Writes an {@link Instant} to the stream, which could possibly be null
     */
    public final void writeOptionalInstant(@Nullable Instant instant) throws IOException {
        if (instant == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeInstant(instant);
        }
    }

    private static final Map<Class<?>, Writer> WRITERS;

    static {
        Map<Class<?>, Writer> writers = new HashMap<>();
        writers.put(String.class, (o, v) -> {
            o.writeByte((byte) 0);
            o.writeString((String) v);
        });
        writers.put(Integer.class, (o, v) -> {
            o.writeByte((byte) 1);
            o.writeInt((Integer) v);
        });
        writers.put(Long.class, (o, v) -> {
            o.writeByte((byte) 2);
            o.writeLong((Long) v);
        });
        writers.put(Float.class, (o, v) -> {
            o.writeByte((byte) 3);
            o.writeFloat((float) v);
        });
        writers.put(Double.class, (o, v) -> {
            o.writeByte((byte) 4);
            o.writeDouble((double) v);
        });
        writers.put(Boolean.class, (o, v) -> {
            o.writeByte((byte) 5);
            o.writeBoolean((boolean) v);
        });
        writers.put(byte[].class, (o, v) -> {
            o.writeByte((byte) 6);
            final byte[] bytes = (byte[]) v;
            o.writeVInt(bytes.length);
            o.writeBytes(bytes);
        });
        writers.put(List.class, (o, v) -> {
            o.writeByte((byte) 7);
            final List list = (List) v;
            o.writeVInt(list.size());
            for (Object item : list) {
                o.writeGenericValue(item);
            }
        });
        writers.put(Object[].class, (o, v) -> {
            o.writeByte((byte) 8);
            final Object[] list = (Object[]) v;
            o.writeVInt(list.length);
            for (Object item : list) {
                o.writeGenericValue(item);
            }
        });
        writers.put(Map.class, (o, v) -> {
            if (v instanceof LinkedHashMap) {
                o.writeByte((byte) 9);
            } else {
                o.writeByte((byte) 10);
            }
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) v;
            o.writeVInt(map.size());
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                o.writeString(entry.getKey());
                o.writeGenericValue(entry.getValue());
            }
        });
        writers.put(Byte.class, (o, v) -> {
            o.writeByte((byte) 11);
            o.writeByte((Byte) v);
        });
        writers.put(Date.class, (o, v) -> {
            o.writeByte((byte) 12);
            o.writeLong(((Date) v).getTime());
        });
        writers.put(Short.class, (o, v) -> {
            o.writeByte((byte) 16);
            o.writeShort((Short) v);
        });
        writers.put(int[].class, (o, v) -> {
            o.writeByte((byte) 17);
            o.writeIntArray((int[]) v);
        });
        writers.put(long[].class, (o, v) -> {
            o.writeByte((byte) 18);
            o.writeLongArray((long[]) v);
        });
        writers.put(float[].class, (o, v) -> {
            o.writeByte((byte) 19);
            o.writeFloatArray((float[]) v);
        });
        writers.put(double[].class, (o, v) -> {
            o.writeByte((byte) 20);
            o.writeDoubleArray((double[]) v);
        });
        writers.put(ZonedDateTime.class, (o, v) -> {
            o.writeByte((byte) 23);
            final ZonedDateTime zonedDateTime = (ZonedDateTime) v;
            o.writeString(zonedDateTime.getZone().getId());
            o.writeLong(zonedDateTime.toInstant().toEpochMilli());
        });
        WRITERS = Collections.unmodifiableMap(writers);
    }

    /**
     * Notice: when serialization a map, the stream out map with the stream in map maybe have the
     * different key-value orders, they will maybe have different stream order.
     * If want to keep stream out map and stream in map have the same stream order when stream,
     * can use {@code writeMapWithConsistentOrder}
     */
    public void writeGenericValue(@Nullable Object value) throws IOException {
        if (value == null) {
            writeByte((byte) -1);
            return;
        }
        final Class type;
        if (value instanceof List) {
            type = List.class;
        } else if (value instanceof Object[]) {
            type = Object[].class;
        } else if (value instanceof Map) {
            type = Map.class;
        } else {
            type = value.getClass();
        }
        final Writer writer = WRITERS.get(type);
        if (writer != null) {
            writer.write(this, value);
        } else {
            throw new IOException("can not write type [" + type + "]");
        }
    }

    public void writeIntArray(int[] values) throws IOException {
        writeVInt(values.length);
        for (int value : values) {
            writeInt(value);
        }
    }

    public void writeVIntArray(int[] values) throws IOException {
        writeVInt(values.length);
        for (int value : values) {
            writeVInt(value);
        }
    }

    public void writeLongArray(long[] values) throws IOException {
        writeVInt(values.length);
        for (long value : values) {
            writeLong(value);
        }
    }

    public void writeVLongArray(long[] values) throws IOException {
        writeVInt(values.length);
        for (long value : values) {
            writeVLong(value);
        }
    }

    public void writeFloatArray(float[] values) throws IOException {
        writeVInt(values.length);
        for (float value : values) {
            writeFloat(value);
        }
    }

    public void writeDoubleArray(double[] values) throws IOException {
        writeVInt(values.length);
        for (double value : values) {
            writeDouble(value);
        }
    }

    /**
     * Writes the specified array to the stream using the specified {@link Writer} for each element in the array. This method can be seen as
     * writer version of {@link StreamInput#readArray(Writeable.Reader, IntFunction)}. The length of array encoded as a variable-length
     * integer is first written to the stream, and then the elements of the array are written to the stream.
     *
     * @param writer the writer used to write individual elements
     * @param array  the array
     * @param <T>    the type of the elements of the array
     * @throws IOException if an I/O exception occurs while writing the array
     */
    public <T> void writeArray(final Writer<T> writer, final T[] array) throws IOException {
        writeVInt(array.length);
        for (T value : array) {
            writer.write(this, value);
        }
    }

    /**
     * Same as {@link #writeArray(Writer, Object[])} but the provided array may be null. An additional boolean value is
     * serialized to indicate whether the array was null or not.
     */
    public <T> void writeOptionalArray(final Writer<T> writer, final @Nullable T[] array) throws IOException {
        if (array == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeArray(writer, array);
        }
    }

    /**
     * Writes the specified array of {@link Writeable}s. This method can be seen as
     * writer version of {@link StreamInput#readArray(Writeable.Reader, IntFunction)}. The length of array encoded as a variable-length
     * integer is first written to the stream, and then the elements of the array are written to the stream.
     */
    public <T extends Writeable> void writeArray(T[] array) throws IOException {
        writeArray((out, value) -> value.writeTo(out), array);
    }

    /**
     * Same as {@link #writeArray(Writeable[])} but the provided array may be null. An additional boolean value is
     * serialized to indicate whether the array was null or not.
     */
    public <T extends Writeable> void writeOptionalArray(@Nullable T[] array) throws IOException {
        writeOptionalArray((out, value) -> value.writeTo(out), array);
    }

    /**
     * Serializes a potential null value.
     */
    public void writeOptionalStreamable(@Nullable Streamable streamable) throws IOException {
        if (streamable != null) {
            writeBoolean(true);
            streamable.writeTo(this);
        } else {
            writeBoolean(false);
        }
    }

    public void writeOptionalWriteable(@Nullable Writeable writeable) throws IOException {
        if (writeable != null) {
            writeBoolean(true);
            writeable.writeTo(this);
        } else {
            writeBoolean(false);
        }
    }

    /**
     * Writes a list of {@link Streamable} objects
     */
    public void writeStreamableList(List<? extends Streamable> list) throws IOException {
        writeVInt(list.size());
        for (Streamable obj: list) {
            obj.writeTo(this);
        }
    }

    /**
     * Writes a collection to this stream. The corresponding collection can be read from a stream input using
     * {@link StreamInput#readList(Writeable.Reader)}.
     *
     * @param collection the collection to write to this stream
     * @throws IOException if an I/O exception occurs writing the collection
     */
    public void writeCollection(final Collection<? extends Writeable> collection) throws IOException {
        writeCollection(collection, (o, v) -> v.writeTo(o));
    }

    /**
     * Writes a list of {@link Writeable} objects
     */
    public void writeList(List<? extends Writeable> list) throws IOException {
        writeCollection(list);
    }

    /**
     * Writes a collection of objects via a {@link Writer}.
     *
     * @param collection the collection of objects
     * @throws IOException if an I/O exception occurs writing the collection
     */
    public <T> void writeCollection(final Collection<T> collection, final Writer<T> writer) throws IOException {
        writeVInt(collection.size());
        for (final T val: collection) {
            writer.write(this, val);
        }
    }

    /**
     * Writes a collection of a strings. The corresponding collection can be read from a stream input using
     * {@link StreamInput#readList(Writeable.Reader)}.
     *
     * @param collection the collection of strings
     * @throws IOException if an I/O exception occurs writing the collection
     */
    public void writeStringCollection(final Collection<String> collection) throws IOException {
        writeCollection(collection, StreamOutput::writeString);
    }

    /**
     * Writes an enum with type E based on its ordinal value
     */
    public <E extends Enum<E>> void writeEnum(E enumValue) throws IOException {
        writeVInt(enumValue.ordinal());
    }

    /**
     * Writes an EnumSet with type E that by serialized it based on it's ordinal value
     */
    public <E extends Enum<E>> void writeEnumSet(EnumSet<E> enumSet) throws IOException {
        writeVInt(enumSet.size());
        for (E e : enumSet) {
            writeEnum(e);
        }
    }
}
