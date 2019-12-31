package love.wangqi.stream;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * @author: wangqi
 * @description:
 * @Version:
 * @date: Created in 2019/12/31 2:34 下午
 */
public interface Streamable {
    /**
     * Set this object's fields from a {@linkplain StreamInput}.
     */
    void readFrom(StreamInput in) throws IOException;

    /**
     * Write this object's fields to a {@linkplain StreamOutput}.
     */
    void writeTo(StreamOutput out) throws IOException;

    static <T extends Streamable> Writeable.Reader<T> newWriteableReader(Supplier<T> supplier) {
        return (StreamInput in) -> {
            T request = supplier.get();
            request.readFrom(in);
            return request;
        };
    }
}
