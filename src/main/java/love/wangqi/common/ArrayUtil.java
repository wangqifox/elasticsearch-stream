package love.wangqi.common;

/**
 * @author: wangqi
 * @description:
 * @Version:
 * @date: Created in 2019/12/31 2:26 下午
 */
public class ArrayUtil {
    public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - RamUsageEstimator.NUM_BYTES_ARRAY_HEADER;

    public static int oversize(int minTargetSize, int bytesPerElement) {

        if (minTargetSize < 0) {
            // catch usage that accidentally overflows int
            throw new IllegalArgumentException("invalid array size " + minTargetSize);
        }

        if (minTargetSize == 0) {
            // wait until at least one element is requested
            return 0;
        }

        if (minTargetSize > MAX_ARRAY_LENGTH) {
            throw new IllegalArgumentException("requested array size " + minTargetSize + " exceeds maximum array in java (" + MAX_ARRAY_LENGTH + ")");
        }

        // asymptotic exponential growth by 1/8th, favors
        // spending a bit more CPU to not tie up too much wasted
        // RAM:
        int extra = minTargetSize >> 3;

        if (extra < 3) {
            // for very small arrays, where constant overhead of
            // realloc is presumably relatively high, we grow
            // faster
            extra = 3;
        }

        int newSize = minTargetSize + extra;

        // add 7 to allow for worst case byte alignment addition below:
        if (newSize+7 < 0 || newSize+7 > MAX_ARRAY_LENGTH) {
            // int overflowed, or we exceeded the maximum array length
            return MAX_ARRAY_LENGTH;
        }

        if (Constants.JRE_IS_64BIT) {
            // round up to 8 byte alignment in 64bit env
            switch(bytesPerElement) {
                case 4:
                    // round up to multiple of 2
                    return (newSize + 1) & 0x7ffffffe;
                case 2:
                    // round up to multiple of 4
                    return (newSize + 3) & 0x7ffffffc;
                case 1:
                    // round up to multiple of 8
                    return (newSize + 7) & 0x7ffffff8;
                case 8:
                    // no rounding
                default:
                    // odd (invalid?) size
                    return newSize;
            }
        } else {
            // round up to 4 byte alignment in 64bit env
            switch(bytesPerElement) {
                case 2:
                    // round up to multiple of 2
                    return (newSize + 1) & 0x7ffffffe;
                case 1:
                    // round up to multiple of 4
                    return (newSize + 3) & 0x7ffffffc;
                case 4:
                case 8:
                    // no rounding
                default:
                    // odd (invalid?) size
                    return newSize;
            }
        }
    }
}
