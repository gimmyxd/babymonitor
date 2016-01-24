package babymonitor.app;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by gimmy on 1/24/16.
 */
public final class SessionIdentifierGenerator {
    private static final char[] symbols;

    static {
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch)
            tmp.append(ch);
        for (char ch = 'a'; ch <= 'z'; ++ch)
            tmp.append(ch);
        symbols = tmp.toString().toCharArray();
    }

    private final Random random = new Random();

    private final char[] buf;

    public SessionIdentifierGenerator(int length) {
        if (length < 1)
            throw new IllegalArgumentException("length < 1: " + length);
        buf = new char[length];
    }

    public String nextSessionId() {
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }
}