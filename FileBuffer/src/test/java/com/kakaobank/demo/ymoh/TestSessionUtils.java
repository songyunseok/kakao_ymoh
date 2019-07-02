package com.kakaobank.demo.ymoh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;

public class TestSessionUtils {

    @Test
    public void testReadWithByteArray() throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile("src/test/resources/test_read_with_byte_array.in", "r");
             FileChannel channel = raf.getChannel()) {
            byte[] byteArray = new byte[SessionUtils.OP_NETHOD_SIZE];
            int length = SessionUtils.read(channel, byteArray);
            assertEquals(SessionUtils.OP_NETHOD_SIZE, length);
            assertEquals("PUSH", SessionUtils.parseString(byteArray));
            byteArray = new byte[SessionUtils.OP_LENGTH_SIZE];
            length = SessionUtils.read(channel, byteArray);
            assertEquals(SessionUtils.OP_LENGTH_SIZE, length);
            assertEquals(123, SessionUtils.parseInt(byteArray));
        }
    }

    @Test
    public void testReadWithOutputStream() throws Exception {
        File inputFile = new File("src/test/resources/test_read_with_output_stream.in");
        File outputFile = new File("src/test/resources/test_read_with_output_stream.out");
        try (RandomAccessFile raf = new RandomAccessFile(inputFile, "r");
             FileChannel channel = raf.getChannel();
             FileOutputStream outputStream = new FileOutputStream(outputFile, false)) {
            long length = SessionUtils.read(channel, inputFile.length(), outputStream, true);
            assertEquals(inputFile.length(), length);
        }
    }

    @Test
    public void testWriteWithByteArray() throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile("src/test/resources/test_write_with_byte_array.out", "rw");
             FileChannel channel = raf.getChannel()) {
            byte[] byteArray = new byte[SessionUtils.OP_NETHOD_SIZE];
            SessionUtils.putString(byteArray, "PULL");
            assertEquals("PULL", SessionUtils.parseString(byteArray));
            int length = SessionUtils.write(channel, byteArray);
            assertEquals(SessionUtils.OP_NETHOD_SIZE, length);
            byteArray = new byte[SessionUtils.OP_LENGTH_SIZE];
            SessionUtils.putInt(byteArray, 123);
            assertEquals(123, SessionUtils.parseInt(byteArray));
            length = SessionUtils.write(channel, byteArray);
            assertEquals(SessionUtils.OP_LENGTH_SIZE, length);
        }
    }

    @Test
    public void testWriteWithInputStream() throws Exception {
        File inputFile = new File("src/test/resources/test_write_with_input_stream.in");
        File outputFile = new File("src/test/resources/test_write_with_input_stream.out");
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw");
             FileChannel channel = raf.getChannel();
             FileInputStream inputStream = new FileInputStream(inputFile)) {
            long length = SessionUtils.write(channel, inputStream);
            assertEquals(inputFile.length(), length);
        }
    }

}
