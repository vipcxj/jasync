package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class VarNameHelper {
    private final Map<String, String> map;

    public VarNameHelper() {
        this.map = new HashMap<>();
    }

    private void collectVarNames(Frame<? extends BasicValue> frame, boolean stack) {
        int to = stack ? frame.getStackSize() : frame.getLocals();
        for (int i = 0; i < to;) {
            BasicValue value = stack ? frame.getStack(i) : frame.getLocal(i);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                if (type.getSort() == Type.OBJECT) {
                    addVarName(type.getInternalName());
                } else if (type.getSort() == Type.ARRAY) {
                    addVarName(type.getElementType().getInternalName());
                }
                i += stack ? 1 : type.getSize();
            } else {
                ++i;
            }
        }
    }

    public void collectVarNames(Frame<? extends BasicValue> frame) {
        collectVarNames(frame, false);
        collectVarNames(frame, true);
    }

    public String addVarName(String input) {
        String s = map.get(input);
        if (s != null) {
            return s;
        }
        if (input.equals("nil") || input.equals("!") || input.equals("O")) {
            return "<" + input + ">";
        }
        s = generateShorthand(input, 2);
        if (map.containsValue(s)) {
            for (int i = 3; i < input.length(); ++i) {
                s = generateShorthand(input, i);
                if (!map.containsValue(s)) {
                    break;
                }
            }
        }
        map.put(input, s);
        return s;
    }

    private static final String TAB = "  ";

    public void print(PrintWriter pw, boolean withSeparator) {
        if (!map.isEmpty()) {
            pw.print("Var Name Map:");
            pw.println();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                pw.print(TAB);
                pw.print(entry.getKey());
                pw.print(" = ");
                pw.print(entry.getValue());
                pw.println();
            }
            if (withSeparator) {
                pw.println("============");
            }
        }
    }

    private String generateShorthand(String input, int minLen) {
        if (input == null) {
            return "nil";
        }
        if (input.equals(Constants.OBJECT_NAME)) {
            return "O";
        }
        input = input.trim();
        if (input.isEmpty() || input.matches("/+")) {
            return "!";
        }
        String[] parts = input.split("/");
        String lastPart = parts[parts.length - 1];
        int flagged = 0;
        int length = lastPart.length();
        boolean[] flags = new boolean[length];
        Arrays.fill(flags, false);
        flags[0] = true;
        ++flagged;
        for (int i = 1; i < length; ++i) {
            if (!flags[i]) {
                char c = lastPart.charAt(i);
                if (Character.isUpperCase(c)) {
                    flags[i] = true;
                    ++flagged;
                }
            }
        }
        if (flagged < minLen) {
            for (int i = 1; i < length; ++i) {
                if (!flags[i]) {
                    flags[i] = true;
                    ++flagged;
                }
                if (flagged >= minLen) {
                    break;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(lastPart.charAt(0)));
        for (int i = 1; i < length; ++i) {
            if (flags[i]) {
                char c = lastPart.charAt(i);
                sb.append(c);
            }
        }
        int selected = flagged;
        String prefix = "";
        if (selected < minLen && parts.length > 1) {
            String[] newParts = new String[parts.length - 1];
            for (int i = newParts.length - 1; i >= 0; --i) {
                String part = parts[i];
                int sub = Math.min(minLen - selected, part.length());
                if (sub >= 1) {
                    String head = Character.toString(Character.toUpperCase(part.charAt(0)));
                    if (sub == 1) {
                        newParts[i] = head;
                    } else {
                        newParts[i] = head + part.substring(1, sub);
                    }
                } else {
                    newParts[i] = "";
                }
                selected += sub;
                if (selected >= minLen) {
                    break;
                }
            }
            prefix = Arrays.stream(newParts).filter(Objects::nonNull).collect(Collectors.joining("."));
        }
        if (!prefix.isEmpty()) {
            return prefix + "." + sb;
        } else {
            return sb.toString();
        }
    }
}
