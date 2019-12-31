package love.wangqi.stream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author: wangqi
 * @description:
 * @Version:
 * @date: Created in 2019/12/31 2:46 下午
 */
public class OutputStreamStreamOutput extends StreamOutput {

    private final OutputStream out;

    public OutputStreamStreamOutput(OutputStream out) {
        this.out = out;
    }

    @Override
    public void writeByte(byte b) throws IOException {
        out.write(b);
    }

    @Override
    public void writeBytes(byte[] b, int offset, int length) throws IOException {
        out.write(b, offset, length);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException();
    }
}
