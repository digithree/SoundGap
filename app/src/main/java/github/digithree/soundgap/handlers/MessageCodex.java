/* THIS FILE NOT UNDER COPYRIGHT
 *
 * @author digithree
 */

package github.digithree.soundgap.handlers;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * Created by simonkenny on 10/12/2016.
 */

public class MessageCodex {

    private static final int NUM_BLOCK_CODE_OFFSET = 10;
    private static final int UP_BLOCK_ASCII_OFFSET = 20;
    private static final int LOW_BLOCK_ASCII_OFFSET = 50;
    private static final int EQUALS_CHAR_OFFSET = 80;

    // TODO : add checksum or similar to verify integrity of message

    public static String encode(String message) {
        String encoded;
        try {
            encoded = Base64.encodeToString(message.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0 ; i < encoded.length() ; i++) {
            char ch = encoded.charAt(i);
            if (ch == '=') {
                stringBuilder.append(String.format(Locale.getDefault(), "%d", EQUALS_CHAR_OFFSET));
            } else if (ch >= '0' && ch <= '9') {
                stringBuilder.append(String.format(Locale.getDefault(), "%2d", (NUM_BLOCK_CODE_OFFSET + ch - '0')));
            } else if (ch >= 'A' && ch <= 'Z') {
                stringBuilder.append(String.format(Locale.getDefault(), "%2d", (UP_BLOCK_ASCII_OFFSET + ch - 'A')));
            } else if (ch >= 'a' && ch <= 'z') {
                stringBuilder.append(String.format(Locale.getDefault(), "%2d", (LOW_BLOCK_ASCII_OFFSET + ch - 'a')));
            }
            // else don't add, was error with encoding
        }
        return stringBuilder.toString();
    }

    public static String decode(String message) {
        if (message == null || message.isEmpty() || message.length() % 2 != 0) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0 ; i < (message.length() / 2) ; i++) {
            try {
                int num = (Integer.parseInt(""+message.charAt(i * 2)) * 10)
                        + Integer.parseInt(""+message.charAt((i * 2) + 1));
                char ch;
                if (num == EQUALS_CHAR_OFFSET) {
                    ch = '=';
                } else if (num >= NUM_BLOCK_CODE_OFFSET && num <= (NUM_BLOCK_CODE_OFFSET + '9' - '0')) {
                    ch = (char) (num - NUM_BLOCK_CODE_OFFSET + '0');
                } else if (num >= UP_BLOCK_ASCII_OFFSET && num <= (UP_BLOCK_ASCII_OFFSET + 'Z' - 'A')) {
                    ch = (char) (num - UP_BLOCK_ASCII_OFFSET + 'A');
                } else if (num >= LOW_BLOCK_ASCII_OFFSET && num <= (LOW_BLOCK_ASCII_OFFSET + 'z' - 'Z')) {
                    ch = (char) (num - LOW_BLOCK_ASCII_OFFSET + 'a');
                } else {
                    throw new NumberFormatException("Decode block not found");
                }
                stringBuilder.append(ch);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
        }
        try {
            byte []decodedBytes = Base64.decode(stringBuilder.toString().getBytes("UTF-8"), Base64.NO_WRAP);
            return new String(decodedBytes);
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
}
