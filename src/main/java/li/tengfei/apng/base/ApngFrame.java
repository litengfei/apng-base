package li.tengfei.apng.base;

import java.io.InputStream;

/**
 * Apng Frame Data
 *
 * @author ltf
 * @since 16/11/28, 下午1:15
 */
public class ApngFrame extends ApngFCTLChunk {

    InputStream imageStream;

    public InputStream getImageStream() {
        return imageStream;
    }
}
