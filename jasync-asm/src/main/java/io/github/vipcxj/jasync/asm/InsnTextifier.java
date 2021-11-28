package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InsnTextifier extends Textifier {

    private final List<AbstractInsnNode> insnNodes;
    private final List<Frame<? extends BasicValue>> frames;
    private int index;
    private boolean withFrame;
    private int tab;
    private final Set<AbstractInsnNode> targets;
    private final VarNameHelper helper;

    public InsnTextifier(List<AbstractInsnNode> insnNodes, List<Frame<? extends BasicValue>> frames) {
        super(Constants.ASM_VERSION);
        this.insnNodes = insnNodes;
        this.frames = frames;
        this.index = 0;
        this.withFrame = true;
        this.tab = 2;
        this.helper = new VarNameHelper();
        for (Frame<? extends BasicValue> frame : frames) {
            if (frame != null) {
                helper.collectVarNames(frame);
            }
        }
        this.targets = new HashSet<>();
    }

    public void setTab(int tab) {
        this.tab = tab;
    }

    public int getTab() {
        return tab;
    }

    public void setWithFrame(boolean withFrame) {
        this.withFrame = withFrame;
    }

    public VarNameHelper getHelper() {
        return helper;
    }

    public Set<AbstractInsnNode> getTargets() {
        return targets;
    }

    public void print(PrintWriter printWriter) {
        this.text.clear();
        this.index = 0;
        MethodNode methodNode = new MethodNode();
        LabelMap labelMap = new LabelMap();
        for (AbstractInsnNode insnNode : insnNodes) {
            methodNode.instructions.add(insnNode.clone(labelMap));
        }
        MethodVisitor methodVisitor = new TraceMethodVisitor(this);
        methodNode.accept(methodVisitor);
        if (printWriter != null) {
            super.print(printWriter);
            printWriter.flush();
        }
    }

    private static String printFramePart(Frame<? extends BasicValue> frame, VarNameHelper helper, boolean stack) {
        StringBuilder sb = new StringBuilder();
        int to = stack ? frame.getStackSize() : frame.getLocals();
        for (int i = 0; i < to;) {
            sb.append(i);
            sb.append(":");
            BasicValue value = stack ? frame.getStack(i) : frame.getLocal(i);
            if (value != null) {
                Type type = value.getType();
                if (type != null) {
                    if (type.getSort() == Type.OBJECT) {
                        String n = type.getInternalName();
                        String s = helper.addVarName(n);
                        sb.append(s);
                    } else if (type.getSort() == Type.ARRAY) {
                        Type elementType = type.getElementType();
                        String s = helper.addVarName(elementType.getInternalName());
                        sb.append(s);
                        for (int j = 0; j < type.getDimensions(); ++j) {
                            sb.append("[]");
                        }
                    } else {
                        sb.append(value);
                    }
                    i += stack ? 1 : type.getSize();
                } else {
                    sb.append(value);
                    ++i;
                }
            } else {
                sb.append("!");
                ++i;
            }
            if (i < to) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private static String tabs(int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; ++i) {
            sb.append(" ");
        }
        return sb.toString();
    }

    public static String printFrame(Frame<? extends BasicValue> frame, VarNameHelper helper, int tab) {
        return tabs(tab) +
                "[" +
                printFramePart(frame, helper, false) +
                "] [" +
                printFramePart(frame, helper, true) +
                "]" +
                System.lineSeparator();
    }

    protected void beforeInsn(int opcode) {
        AbstractInsnNode insnNode = insnNodes.get(index);
        Frame<? extends BasicValue> frame = frames.get(index);
        assert insnNode.getOpcode() == opcode;
        if (withFrame && frame != null) {
            text.add(printFrame(frame, helper, tab));
        }
    }

    private static Object tagText(Object text) {
        if (text instanceof List) {
            //noinspection unchecked
            List<Object> list = (List<Object>) text;
            if (!list.isEmpty()) {
                list.set(0, tagText(list.get(0)));
            }
            return list;
        } else if (text != null) {
            String strText = text.toString();
            Matcher matcher = PT_SPACE_START.matcher(strText);
            if (matcher.find()) {
                String spaces = matcher.group();
                int length = spaces.length();
                String lefts = strText.substring(length);
                if (length <= 2) {
                    strText = "> " + lefts;
                } else {
                    strText = spaces.substring(0, length - 2) + "> " + lefts;
                }
            } else {
                strText = "> " + strText;
            }
            return strText;
        } else {
            return null;
        }
    }

    protected void afterInsn(int opcode, int from, int to) {
        AbstractInsnNode insnNode = insnNodes.get(index);
        forceMinTab(tab, from, to);
        if (to > from && targets.contains(insnNode)) {
            text.set(from, tagText(text.get(from)));
        }
        ++index;
    }

    @Override
    public void visitInsn(int opcode) {
        beforeInsn(opcode);
        int before = text.size();
        super.visitInsn(opcode);
        int after = text.size();
        afterInsn(opcode, before, after);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        beforeInsn(opcode);
        int before = text.size();
        super.visitIntInsn(opcode, operand);
        int after = text.size();
        afterInsn(opcode, before, after);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        beforeInsn(opcode);
        int before = text.size();
        super.visitFieldInsn(opcode, owner, name, descriptor);
        int after = text.size();
        afterInsn(opcode, before, after);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        beforeInsn(Opcodes.IINC);
        int before = text.size();
        super.visitIincInsn(var, increment);
        int after = text.size();
        afterInsn(Opcodes.IINC, before, after);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        beforeInsn(opcode);
        int before = text.size();
        super.visitJumpInsn(opcode, label);
        int after = text.size();
        afterInsn(opcode, before, after);
    }

    @Override
    public void visitLdcInsn(Object value) {
        beforeInsn(Opcodes.LDC);
        int before = text.size();
        super.visitLdcInsn(value);
        int after = text.size();
        afterInsn(Opcodes.LDC, before, after);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        beforeInsn(opcode);
        int before = text.size();
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        int after = text.size();
        afterInsn(opcode, before, after);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        beforeInsn(opcode);
        int before = text.size();
        super.visitTypeInsn(opcode, type);
        int after = text.size();
        afterInsn(opcode, before, after);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        beforeInsn(opcode);
        int before = text.size();
        super.visitVarInsn(opcode, var);
        int after = text.size();
        afterInsn(opcode, before, after);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        beforeInsn(Opcodes.INVOKEDYNAMIC);
        int before = text.size();
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        int after = text.size();
        afterInsn(Opcodes.INVOKEDYNAMIC, before, after);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        beforeInsn(Opcodes.LOOKUPSWITCH);
        int before = text.size();
        super.visitLookupSwitchInsn(dflt, keys, labels);
        int after = text.size();
        afterInsn(Opcodes.LOOKUPSWITCH, before, after);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        beforeInsn(Opcodes.TABLESWITCH);
        int before = text.size();
        super.visitTableSwitchInsn(min, max, dflt, labels);
        int after = text.size();
        afterInsn(Opcodes.TABLESWITCH, before, after);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        beforeInsn(Opcodes.MULTIANEWARRAY);
        int before = text.size();
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        int after = text.size();
        afterInsn(Opcodes.MULTIANEWARRAY, before, after);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) { }

    protected void traverseText(Function<Object, Object> mapper, int from, int to) {
        for (int i = from; i < to; ++i) {
            text.set(i, traverseText(text.get(i), mapper));
        }
    }

    protected void traverseText(Function<Object, Object> mapper) {
        traverseText(mapper, 0, text.size());
    }

    protected Object traverseText(Object text, Function<Object, Object> mapper) {
        if (text instanceof List) {
            //noinspection unchecked
            List<Object> list = (List<Object>) text;
            ListIterator<Object> iterator = list.listIterator();
            while (iterator.hasNext()) {
                Object next = iterator.next();
                Object newNext = traverseText(next, mapper);
                if (newNext != next) {
                    iterator.set(newNext);
                }
            }
            return list;
        } else {
            return mapper.apply(text);
        }
    }

    private static final Pattern PT_SPACE_START = Pattern.compile("^\\s+");
    private static final Pattern PT_BREAK_LINE = Pattern.compile("[\\r\\n]+");
    private static final Pattern PT_END_BREAK_LINE = Pattern.compile("[\\r\\n]+$");

    private int calcTab(String line) {
        Matcher matcher = PT_SPACE_START.matcher(line);
        if (matcher.find()) {
            String tab = matcher.group();
            return tab.length();
        } else {
            return 0;
        }
    }

    private int calcMinTab(Object obj) {
        int[] result = new int[] { Integer.MAX_VALUE };
        traverseText(obj, t -> {
            if (t != null) {
                String s = t.toString();
                String[] lines = s.split(PT_BREAK_LINE.pattern());
                result[0] = Math.min(result[0], Arrays.stream(lines).mapToInt(this::calcTab).min().orElse(0));
            }
            return t;
        });
        return result[0];
    }

    private String addTab(String text, int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; ++i) {
            sb.append(" ");
        }
        sb.append(text);
        return sb.toString();
    }

    protected String join(String[] parts, String by) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; ++i) {
            sb.append(parts[i]);
            if (i != parts.length - 1) {
                sb.append(by);
            }
        }
        return sb.toString();
    }

    protected void forceMinTab(int tab, int from, int to) {
        int cTab = Integer.MAX_VALUE;
        for (int i = from; i < to; ++i) {
            Object o = text.get(i);
            cTab = Math.min(cTab, calcMinTab(o));
        }
        if (cTab != Integer.MAX_VALUE) {
            int diff = tab - cTab;
            if (diff != 0) {
                traverseText(t -> {
                    String s = t.toString();
                    boolean endBreak = PT_END_BREAK_LINE.matcher(s).find();
                    String[] lines = s.split(PT_BREAK_LINE.pattern());
                    for (int i = 0; i < lines.length; ++i) {
                        if (diff > 0) {
                            lines[i] = addTab(lines[i], diff);
                        } else {
                            lines[i] = lines[i].substring(-diff);
                        }
                    }
                    s = join(lines, System.lineSeparator());
                    if (endBreak) {
                        s += System.lineSeparator();
                    }
                    return s;
                }, from, to);
            }
        }
    }
}
