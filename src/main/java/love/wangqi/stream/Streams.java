package love.wangqi.stream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author: wangqi
 * @description:
 * @Version:
 * @date: Created in 2019/12/31 2:40 下午
 */
public abstract class Streams {

    public static final int BUFFER_SIZE = 1024 * 8;


    //---------------------------------------------------------------------
    // Copy methods for java.io.InputStream / java.io.OutputStream
    //---------------------------------------------------------------------


    public static long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, new byte[BUFFER_SIZE]);
    }

    /**
     * Copy the contents of the given InputStream to the given OutputStream.
     * Closes both streams when done.
     *
     * @param in  the stream to copy from
     * @param out the stream to copy to
     * @return the number of bytes copied
     * @throws IOException in case of I/O errors
     */
    public static long copy(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        Objects.requireNonNull(in, "No InputStream specified");
        Objects.requireNonNull(out, "No OutputStream specified");
        // Leverage try-with-resources to close in and out so that exceptions in close() are either propagated or added as suppressed
        // exceptions to the main exception
        try (InputStream in2 = in; OutputStream out2 = out) {
            return doCopy(in2, out2, buffer);
        }
    }

    private static long doCopy(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        long byteCount = 0;
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }
        out.flush();
        return byteCount;
    }

    /**
     * Copy the contents of the given byte array to the given OutputStream.
     * Closes the stream when done.
     *
     * @param in  the byte array to copy from
     * @param out the OutputStream to copy to
     * @throws IOException in case of I/O errors
     */
    public static void copy(byte[] in, OutputStream out) throws IOException {
        Objects.requireNonNull(in, "No input byte array specified");
        Objects.requireNonNull(out, "No OutputStream specified");
        try (OutputStream out2 = out) {
            out2.write(in);
        }
    }


    //---------------------------------------------------------------------
    // Copy methods for java.io.Reader / java.io.Writer
    //---------------------------------------------------------------------

    /**
     * Copy the contents of the given Reader to the given Writer.
     * Closes both when done.
     *
     * @param in  the Reader to copy from
     * @param out the Writer to copy to
     * @return the number of characters copied
     * @throws IOException in case of I/O errors
     */
    public static int copy(Reader in, Writer out) throws IOException {
        Objects.requireNonNull(in, "No Reader specified");
        Objects.requireNonNull(out, "No Writer specified");
        // Leverage try-with-resources to close in and out so that exceptions in close() are either propagated or added as suppressed
        // exceptions to the main exception
        try (Reader in2 = in; Writer out2 = out) {
            return doCopy(in2, out2);
        }
    }

    private static int doCopy(Reader in, Writer out) throws IOException {
        int byteCount = 0;
        char[] buffer = new char[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }
        out.flush();
        return byteCount;
    }

    /**
     * Copy the contents of the given String to the given output Writer.
     * Closes the write when done.
     *
     * @param in  the String to copy from
     * @param out the Writer to copy to
     * @throws IOException in case of I/O errors
     */
    public static void copy(String in, Writer out) throws IOException {
        Objects.requireNonNull(in, "No input String specified");
        Objects.requireNonNull(out, "No Writer specified");
        try (Writer out2 = out) {
            out2.write(in);
        }
    }

    /**
     * Copy the contents of the given Reader into a String.
     * Closes the reader when done.
     *
     * @param in the reader to copy from
     * @return the String that has been copied to
     * @throws IOException in case of I/O errors
     */
    public static String copyToString(Reader in) throws IOException {
        StringWriter out = new StringWriter();
        copy(in, out);
        return out.toString();
    }

    public static int readFully(Reader reader, char[] dest) throws IOException {
        return readFully(reader, dest, 0, dest.length);
    }

    public static int readFully(Reader reader, char[] dest, int offset, int len) throws IOException {
        int read = 0;
        while (read < len) {
            final int r = reader.read(dest, offset + read, len - read);
            if (r == -1) {
                break;
            }
            read += r;
        }
        return read;
    }

    public static int readFully(InputStream reader, byte[] dest) throws IOException {
        return readFully(reader, dest, 0, dest.length);
    }

    public static int readFully(InputStream reader, byte[] dest, int offset, int len) throws IOException {
        int read = 0;
        while (read < len) {
            final int r = reader.read(dest, offset + read, len - read);
            if (r == -1) {
                break;
            }
            read += r;
        }
        return read;
    }

    public static List<String> readAllLines(InputStream input) throws IOException {
        final List<String> lines = new ArrayList<>();
        readAllLines(input, lines::add);
        return lines;
    }

    public static void readAllLines(InputStream input, Consumer<String> consumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(line);
            }
        }
    }
}
