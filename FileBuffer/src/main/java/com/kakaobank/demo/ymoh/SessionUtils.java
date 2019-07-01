package com.kakaobank.demo.ymoh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SessionUtils {

    public final static int OP_NETHOD_SIZE = 32;

    public final static int OP_LENGTH_SIZE = 6;

    //public final static int OP_TOKEN_SIZE = 64;

    public final static int BUFFER_SIZE = 1024;

    public static int read(SocketChannel socketChannel, byte[] byteArray) throws Exception {
        int arrayLength = byteArray.length;
        ByteBuffer buffer = ByteBuffer.allocate(arrayLength);
        int length = 0;
        while (length < arrayLength) {
            int n = socketChannel.read(buffer);
            if (n < 0) {
                return n;
            } else if (n == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            } else {
                length += n;
            }
        }
        buffer.flip();
        buffer.get(byteArray);
        return length;
    }

    public static long read(SocketChannel socketChannel, long length, OutputStream outputStream) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        byte[] byteArray = new byte[BUFFER_SIZE];
        long numBytesRead = 0;
        int len = 0;
        loop: while (numBytesRead < length) {
            buffer.clear();
            len = 0;
            while (len < BUFFER_SIZE) {
                int n = socketChannel.read(buffer);
                if (n < 0) {
                    return n;
                } else if (n == 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break loop;
                    }
                } else {
                    len += n;
                }
            }
            buffer.flip();
            buffer.get(byteArray, 0, len);
            outputStream.write(byteArray, 0, len);
        }
        return numBytesRead;
    }

    public static String parseString(byte[] bytes) throws Exception {
        if (bytes != null && bytes.length > 0) {
            return new String(bytes, "UTF-8").trim();
        } else if (bytes != null) {
            return "";
        }
        return null;
    }

    public static int parseInt(byte[] bytes) throws Exception {
        if (bytes != null && bytes.length > 0) {
            return Integer.parseInt(new String(bytes, "UTF-8").trim());
        } else if (bytes != null) {
            return 0;
        }
        return -1;
    }

    /*public static long parseLong(byte[] bytes) throws Exception {
        if (bytes != null && bytes.length > 0) {
            return Long.parseLong(new String(bytes, "UTF-8").trim());
        } else if (bytes != null) {
            return 0;
        }
        return -1;
    }*/

    public static int write(SocketChannel socketChannel, byte[] byteArray) throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(byteArray.length);
        buf.put(byteArray);
        buf.flip();
        int numBytesWritten = 0;
        int written = 0;
        while (buf.hasRemaining()) {
            written = socketChannel.write(buf);
            if (written < 0) {
                throw new IOException("Socket output has been broken");
            } else if (written == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                   throw new IOException("Socket output has been interrupted");
                }
            } else {
                numBytesWritten += written;
            }
        }
        return numBytesWritten;
    }

    public static long write(SocketChannel socketChannel, InputStream inputStream) throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
        byte[] byteArray = new byte[BUFFER_SIZE];
        int length = 0;
        long numBytesWritten = 0;
        int written = 0;
        while ((length = inputStream.read(byteArray)) > 0) {
            buf.clear();
            buf.put(byteArray, 0, length);
            buf.flip();
            while (buf.hasRemaining()) {
                written = socketChannel.write(buf);
                if (written < 0) {
                    throw new IOException("Socket output has been broken");
                } else if (written == 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        throw new IOException("Socket output has been interrupted");
                    }
                } else {
                    numBytesWritten += written;
                }
            }
        }
        return numBytesWritten;
    }

    public static void putString(byte[] dest, String value) throws Exception {
        for (int i = 0; i < dest.length; i++) {
            dest[i] = 0x20;
        }
        byte[] src = value.getBytes("UTF-8");
        System.arraycopy(src, 0, dest, 0, src.length);
    }

    public static void putInt(byte[] dest, int value) throws Exception {
        for (int i = 0; i < dest.length; i++) {
            dest[i] = 0x30;
        }
        byte[] src = String.valueOf(value).getBytes();
        int destPos = dest.length - src.length;
        if (destPos < 0) {
            destPos = 0;
        }
        System.arraycopy(src, 0, dest, destPos, src.length);
    }

}
