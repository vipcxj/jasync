package io.github.vipcxj.jasync.core.javac.utils;

import java.io.StringWriter;

public class IndentSupportStringWriter extends StringWriter {

    private final String lineSep;
    private final String indent;
    private int matchedLineSep;

    public IndentSupportStringWriter(String indent, String lineSep) {
        this.indent = indent;
        this.lineSep = lineSep != null ? lineSep : System.getProperty("line.separator");
        matchedLineSep = 0;
    }

    public IndentSupportStringWriter(String indent) {
        this(indent, null);
    }

    public IndentSupportStringWriter() {
        this("    ");
    }

    @Override
    public void write(int c) {
        boolean append = false;
        if (matchedLineSep < lineSep.length()) {
            if ((char) c == lineSep.charAt(matchedLineSep)) {
                ++matchedLineSep;
                if (matchedLineSep == lineSep.length()) {
                    append = true;
                    matchedLineSep = 0;
                }
            } else {
                matchedLineSep = 0;
            }
        }
        super.write(c);
        if (append) {
            super.write(indent);
        }
    }

    @Override
    public void write(String str) {
        int length = str.length();
        for (int i = 0; i < length; ++i) {
            write(str.charAt(i));
        }
    }

    @Override
    public void write(String str, int off, int len) {
        for (int i = off; i < off + len; ++i) {
            write(str.charAt(i));
        }
    }

    @Override
    public void write(char[] cbuf) {
        for (char c : cbuf) {
            write(c);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        for (int i = off; i < off + len; ++i) {
            write(cbuf[i]);
        }
    }
}
