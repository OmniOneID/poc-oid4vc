package org.omnione.did.sdk.mdoc.proximity.holder.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for mDoc data transfer, including chunking.
 */
public class MdocTransferUtil {

    /**
     * Chunks a data array into multiple chunks with a 1-byte header (ISO 18013-5).
     * 0x01: More chunks coming.
     * 0x00: Last chunk.
     *
     * @param data The complete data to be chunked.
     * @param maxChunkSize The maximum size of each chunk (excluding the header byte).
     * @return A list of chunked byte arrays.
     */
    public static List<byte[]> chunkData(byte[] data, int maxChunkSize) {
        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < data.length) {
            int end = Math.min(offset + maxChunkSize, data.length);
            byte[] chunkData = java.util.Arrays.copyOfRange(data, offset, end);

            byte[] chunk = new byte[chunkData.length + 1];
            chunk[0] = (end < data.length) ? (byte) 0x01 : (byte) 0x00;
            System.arraycopy(chunkData, 0, chunk, 1, chunkData.length);
            
            chunks.add(chunk);
            offset += maxChunkSize;
        }
        return chunks;
    }
}
