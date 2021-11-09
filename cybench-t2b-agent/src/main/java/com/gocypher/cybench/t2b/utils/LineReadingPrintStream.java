/*
 * Copyright (C) 2020-2021, K2N.IO.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

package com.gocypher.cybench.t2b.utils;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class LineReadingPrintStream extends PrintStream {
    private static final byte CR = '\r';
    private static final byte LF = '\n';

    private final Consumer<String> consumer;
    private final StringBuilder stringBuilder = new StringBuilder();
    private boolean lastCR = false;

    public LineReadingPrintStream(Consumer<String> consumer, PrintStream originalStream) {
        super(originalStream);

        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void write(int b) {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(byte[] b, int start, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        int end = start + len;
        if ((start < 0) || (start > b.length) || (end < 0) || (end > b.length)) {
            throw new IndexOutOfBoundsException();
        }

        if (lastCR && start < end && b[start] == LF) {
            start++;
            lastCR = false;
        } else if (start < end) {
            lastCR = b[end - 1] == CR;
        }

        int base = start;
        for (int i = start; i < end; i++) {
            if (b[i] == LF || b[i] == CR) {
                String chunk = asString(b, base, i);
                stringBuilder.append(chunk);
                consume();
            }
            if (b[i] == LF) {
                base = i + 1;
            } else if (b[i] == CR) {
                if (i < end - 1 && b[i + 1] == LF) {
                    base = i + 2;
                    i++;
                } else {
                    base = i + 1;
                }
            }
        }
        String chunk = asString(b, base, end);
        stringBuilder.append(chunk);
    }

    @Override
    public void close() {
        if (stringBuilder.length() > 0) {
            consume();
        }
    }

    private static String asString(byte[] b, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        if (start == end) {
            return "";
        }
        byte[] copy = Arrays.copyOfRange(b, start, end);
        return new String(copy, Charset.defaultCharset());
    }

    private void consume() {
        consumer.accept(stringBuilder.toString());
        stringBuilder.setLength(0);
    }
}
