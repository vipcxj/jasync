package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
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
    private final List<Integer> map;
    private int index;
    private boolean withFrame;
    private int tab;
    private int numberWidth;
    private int mapWidth;
    private int offset;
    private final Set<AbstractInsnNode> targets;
    private final VarNameHelper helper;

    public InsnTextifier(List<AbstractInsnNode> insnNodes, List<Frame<? extends BasicValue>> frames, List<Integer> map) {
        super(Constants.ASM_VERSION);
        this.insnNodes = insnNodes;
        this.frames = frames;
        this.map = map;
        this.index = 0;
        this.withFrame = true;
        this.tab = 2;
        this.numberWidth = 0;
        this.mapWidth = 0;
        this.offset = 0;
        this.helper = new VarNameHelper();
        for (Frame<? extends BasicValue> frame : frames) {
            if (frame != null) {
                helper.collectVarNames(frame);
            }
        }
        this.targets = new HashSet<>();
    }

    private static int calcNumberWidth(int number) {
        if (number == 0) {
            return 0;
        }
        int res = 1;
        while ((number /= 10) != 0) {
            ++res;
        }
        return res;
    }

    public VarNameHelper getHelper() {
        return helper;
    }

    public Set<AbstractInsnNode> getTargets() {
        return targets;
    }

    public void adapt(MethodNode methodNode, int option) {
        if (methodNode != null) {
            if (insnNodes != null && !insnNodes.isEmpty()) {
                AbstractInsnNode firstNode = insnNodes.get(0);
                if (methodNode.instructions.contains(firstNode)) {
                    this.offset = methodNode.instructions.indexOf(firstNode);
                    this.numberWidth = JAsyncInfo.isLogByteCodeWithIndex(option) ? calcNumberWidth(methodNode.instructions.size()) : 0;
                    this.mapWidth = (map != null && JAsyncInfo.isLogByteCodeWithMap(option)) ? calcNumberWidth(map.stream().mapToInt(i -> i).max().orElse(0)) : 0;
                    this.tab = Math.max(this.tab, 3);
                    this.withFrame = JAsyncInfo.isLogByteCodeWithFrame(option);
                    return;
                }
            }
        }
        this.offset = 0;
        this.numberWidth = 0;
        this.mapWidth = 0;
    }

    public void print(PrintWriter printWriter, MethodNode methodNode, int option) {
        this.text.clear();
        this.index = 0;
        adapt(methodNode, option);
        MethodNode tempNode = new MethodNode();
        LabelMap labelMap = new LabelMap();
        for (AbstractInsnNode insnNode : insnNodes) {
            tempNode.instructions.add(insnNode.clone(labelMap));
        }
        MethodVisitor methodVisitor = new TraceMethodVisitor(this);
        tempNode.accept(methodVisitor);
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

    private static String tabs(int num, int numberWidth, int number, int mapWidth, int map) {
        String tab = String.format("%" + num + "s", " ");
        if (numberWidth > 0) {
            if (mapWidth > 0) {
                if (map >= 0) {
                    tab = String.format("%0" + numberWidth + "d<-%0" + mapWidth + "d", number, map) + tab;
                } else {
                    tab = String.format("%0" + numberWidth + "d%" + mapWidth + "s", number, " ") + tab;
                }
            } else {
                tab = String.format("%0" + numberWidth + "d", number) + tab;
            }
        }
        return tab;
    }

    public static String printFrame(Frame<? extends BasicValue> frame, VarNameHelper helper, int tab, int numberWidth, int number, int mapWidth, int map) {
        return tabs(tab, numberWidth, number, mapWidth, map) +
                "[" +
                printFramePart(frame, helper, false) +
                "] [" +
                printFramePart(frame, helper, true) +
                "]" +
                System.lineSeparator();
    }

    private String printFrame(int i, VarNameHelper helper) {
        Frame<? extends BasicValue> frame = i < frames.size() ? frames.get(i) : null;
        int mappedIndex = getMappedIndex(i);
        if (frame != null) {
            return printFrame(frame, helper, tab, numberWidth, i + offset, mapWidth, mappedIndex);
        } else {
            return null;
        }
    }

    private int getMappedIndex(int i) {
        return (map != null && i < map.size()) ? map.get(i) : -1;
    }

    protected AbstractInsnNode beforeInsn(int opcode) {
        return beforeInsn(opcode, false, false, false);
    }

    protected AbstractInsnNode beforeInsn(int opcode, boolean frameNode, boolean lineNode, boolean labelNode) {
        AbstractInsnNode insnNode = insnNodes.get(index);
        if (frameNode) {
            assert insnNode instanceof FrameNode;
        } else if (lineNode) {
            assert insnNode instanceof LineNumberNode;
        } else if (labelNode) {
            assert insnNode instanceof LabelNode;
        } else {
            assert insnNode.getOpcode() == opcode;
        }
        if (withFrame) {
            String frameText = printFrame(index, helper);
            if (frameText != null) {
                text.add(frameText);
            }
        }
        return insnNode;
    }

    private Object tagText(Object text) {
        if (text instanceof List) {
            //noinspection unchecked
            List<Object> list = (List<Object>) text;
            if (!list.isEmpty()) {
                list.set(0, tagText(list.get(0)));
            }
            return list;
        } else if (text != null) {
            String strText = text.toString();
            String number = strText.substring(0, numberWidth + mapWidth);
            String strTab = strText.substring(numberWidth + mapWidth, numberWidth + mapWidth + tab);
            String content = strText.substring(numberWidth + mapWidth + tab);
            if (tab <= 2) {
                if (numberWidth > 0) {
                    return number + " >" + content;
                } else {
                    return number + "> " + content;
                }
            }
            if (tab == 3) {
                return number + " > " + content;
            } else {
                return number + " > " + strTab.substring(3) + content;
            }
        } else {
            return null;
        }
    }

    protected void afterInsn(AbstractInsnNode insnNode, int from, int to) {
        forceMinTab(index, from, to);
        if (to > from && targets.contains(insnNode)) {
            text.set(from, tagText(text.get(from)));
        }
        ++index;
    }

    @Override
    public void visitInsn(int opcode) {
        AbstractInsnNode insnNode = beforeInsn(opcode);
        int before = text.size();
        super.visitInsn(opcode);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        AbstractInsnNode insnNode = beforeInsn(opcode);
        int before = text.size();
        super.visitIntInsn(opcode, operand);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        AbstractInsnNode insnNode = beforeInsn(opcode);
        int before = text.size();
        super.visitFieldInsn(opcode, owner, name, descriptor);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        AbstractInsnNode insnNode = beforeInsn(Opcodes.IINC);
        int before = text.size();
        super.visitIincInsn(var, increment);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        AbstractInsnNode insnNode = beforeInsn(opcode);
        int before = text.size();
        super.visitJumpInsn(opcode, label);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitLdcInsn(Object value) {
        AbstractInsnNode insnNode = beforeInsn(Opcodes.LDC);
        int before = text.size();
        super.visitLdcInsn(value);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        AbstractInsnNode insnNode = beforeInsn(opcode);
        int before = text.size();
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        AbstractInsnNode insnNode = beforeInsn(opcode);
        int before = text.size();
        super.visitTypeInsn(opcode, type);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        AbstractInsnNode insnNode = beforeInsn(opcode);
        int before = text.size();
        super.visitVarInsn(opcode, var);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        AbstractInsnNode insnNode = beforeInsn(Opcodes.INVOKEDYNAMIC);
        int before = text.size();
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        AbstractInsnNode insnNode = beforeInsn(Opcodes.LOOKUPSWITCH);
        int before = text.size();
        super.visitLookupSwitchInsn(dflt, keys, labels);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        AbstractInsnNode insnNode = beforeInsn(Opcodes.TABLESWITCH);
        int before = text.size();
        super.visitTableSwitchInsn(min, max, dflt, labels);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        AbstractInsnNode insnNode = beforeInsn(Opcodes.MULTIANEWARRAY);
        int before = text.size();
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        AbstractInsnNode insnNode = beforeInsn(-1, false, true, false);
        int before = text.size();
        super.visitLineNumber(line, start);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        AbstractInsnNode insnNode = beforeInsn(-1, true, false, false);
        int before = text.size();
        super.visitFrame(type, numLocal, local, numStack, stack);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitLabel(Label label) {
        AbstractInsnNode insnNode = beforeInsn(-1, false, false, true);
        int before = text.size();
        super.visitLabel(label);
        int after = text.size();
        afterInsn(insnNode, before, after);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) { }

    protected void traverseText(Function<Object, Object> mapper, int from, int to) {
        for (int i = from; i < to; ++i) {
            text.set(i, traverseText(text.get(i), mapper));
        }
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

    private String adjustTab(String text, int currentTab, int index) {
        return tabs(tab, numberWidth, index + offset, mapWidth, getMappedIndex(index)) + text.substring(currentTab);
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

    protected void forceMinTab(int index, int from, int to) {
        int cTab = Integer.MAX_VALUE;
        for (int i = from; i < to; ++i) {
            Object o = text.get(i);
            cTab = Math.min(cTab, calcMinTab(o));
        }
        if (cTab != Integer.MAX_VALUE) {
            int theTab = cTab;
            traverseText(t -> {
                String s = t.toString();
                boolean endBreak = PT_END_BREAK_LINE.matcher(s).find();
                String[] lines = s.split(PT_BREAK_LINE.pattern());
                for (int i = 0; i < lines.length; ++i) {
                        lines[i] = adjustTab(lines[i], theTab, index);
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
