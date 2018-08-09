/**
 * *******************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *******************************************************************************
 */
package org.insightcentre.nlp.saffron.term.lda;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Used by CPLSA and LDA to do topic assignment on disk
 * 
 * @author John McCrae
 */
public class AssignmentBuffer {

    private final long fileSize;
    private final FileChannel channel;
    private final int bufSize;
    private long pos = 0;
    private MappedByteBuffer buf;

    public AssignmentBuffer(FileChannel buffer, int bufSize, long fileSize) {
        this.channel = buffer;
        this.bufSize = bufSize;
        this.fileSize = fileSize;
    }

    public int getNext() throws IOException {
        if (buf == null || (pos != 0 && pos % bufSize == 0)) {
            loadBuf();
        }
        final int i = buf.getInt();
        pos += 4;
        return i;
    }

    public boolean hasNext() {
        return pos < fileSize;
    }

    public void update(int i) {
        if (buf == null || buf.position() == 0) {
            throw new IllegalArgumentException("buf.position()=" +buf.position() + " pos=" + pos + "/" + fileSize);
        }
        buf.position(buf.position() - 4);
        buf.putInt(i);
    }
    
    public void reset() {
        buf = null;
        pos = 0;
    }

    public static AssignmentBuffer interleavedFrom(File corpus) throws IOException {
        final DataInputStream data = new DataInputStream(openInputAsMaybeZipped(corpus));
        final File tmpFile = File.createTempFile("assign", ".buf");
        tmpFile.deleteOnExit();
        final DataOutputStream out = new DataOutputStream(new FileOutputStream(tmpFile));
        while(data.available() > 0) {
            try {
                int i = data.readInt();
                out.writeInt(i);
                out.writeInt(0);
            } catch(EOFException x) {
                break;
            }
        }
        out.flush();
        out.close();
        return new AssignmentBuffer(new RandomAccessFile(tmpFile, "rw").getChannel(), 4194304, tmpFile.length());
    }

    private void loadBuf() throws IOException {
        // The current buffer is not large enough
        final long toRead = Math.min(fileSize - pos, bufSize);
        buf = channel.map(FileChannel.MapMode.READ_WRITE, pos, toRead);
    }
    
        /**
     * Return a file as an input stream, that unzips if the file ends in .gz or
     * .bz2.
     *
     * @param file The file
     * @return File as an input stream
     * @throws IOException If the file is not found or is not a correct zipped
     * file or some other reason
     */
    public static InputStream openInputAsMaybeZipped(File file) throws IOException {
        if (file.getName().endsWith(".gz")) {
            return new GZIPInputStream(new FileInputStream(file));
        } else if (file.getName().endsWith(".bz2")) {
            return new BZip2CompressorInputStream(new FileInputStream(file));
        } else {
            return new FileInputStream(file);
        }
    }
}
