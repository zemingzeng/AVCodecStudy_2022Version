package com.mingzz__.util;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {

    public static void writeBytes(byte[] bytes, String filePath) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filePath, true);
            outputStream.write(bytes);
            outputStream.write('\n');
        } catch (Exception e) {
            L.i(e.toString());
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    L.i(e.toString());
                }
            }
        }
    }

    //编码好的数据以16进制字符方式写到文件中
    public static void writeBytesTo16Chars(byte[] bytes, String filePath) {
        char[] HEX_CHAR_TABLE = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            stringBuilder.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        L.i("写入的16进制数据：" + stringBuilder.toString());

        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(
                    filePath,
                    true);
            fileWriter.write(stringBuilder.toString());
            fileWriter.write("\n");
        } catch (IOException e) {
            L.i(e.toString());
        } finally {
            try {
                if (fileWriter != null)
                    fileWriter.close();
            } catch (IOException e) {
                L.i(e.toString());
            }
        }
    }

}
