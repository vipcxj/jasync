package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class AsmHelper {

    public static boolean isAwait(int opcode, String owner, String name, String desc) {
        if (Constants.AWAIT.equals(name)) {
            if (opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL) {
                if (Type.getArgumentTypes(desc).length == 0) {
                    return Utils.isJPromise(Type.getObjectType(owner).getClassName());
                }
            }
        }
        return false;
    }

    public static boolean isAwait(AbstractInsnNode insnNode) {
        if (insnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            return isAwait(methodInsnNode.getOpcode(), methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
        }
        return false;
    }

    public static void collectLabels(MethodNode mv, Map<LabelNode, LabelNode> cloneLabels) {
        if (mv.instructions != null) {
            for (AbstractInsnNode instruction : mv.instructions) {
                if (instruction instanceof LabelNode) {
                    LabelNode labelNode = (LabelNode) instruction;
                    cloneLabels.put(labelNode, new LabelNode(new Label()));
                }
            }
        }
    }

    private static void printMethodSign(MethodNode methodNode, Printer printer) {
        printer.visitMethod(
                methodNode.access,
                methodNode.name,
                methodNode.desc,
                methodNode.signature,
                methodNode.exceptions != null ? methodNode.exceptions.toArray(new String[0]) : null
        );
    }

    public static void printMethod(MethodNode methodNode, Printer printer, PrintWriter printWriter, boolean visitMethod) {
        if (visitMethod) {
            printMethodSign(methodNode, printer);
        }
        MethodVisitor methodVisitor = new TraceMethodVisitor(printer);
        methodNode.accept(methodVisitor);
        if (printWriter != null) {
            printer.print(printWriter);
            printWriter.flush();
        }
    }

    private static void cleanMethodSign(Printer printer) {
        List<Object> newText = new ArrayList<>();
        boolean ok = false;
        for (Object o : printer.getText()) {
            if (!ok && o instanceof String) {
                String line = (String) o;
                String newLine = line.replaceAll("^[\\r\\n]+", "");
                if (line.equals(newLine)) {
                    ok = true;
                }
                newText.add(newLine);
            } else {
                ok = true;
                newText.add(o);
            }
        }
        printer.getText().clear();
        printer.getText().addAll(newText);
    }

    public static void printFrameProblem(MethodNode methodNode, Analyzer<? extends BasicValue> analyzer, AnalyzerException error, int from, int to) {
        PrintWriter printWriter = new PrintWriter(System.out);
        if (from > 0 || to < 0) {
            throw new IllegalArgumentException("The arg from should greater than 0, and the arg to should less than 0.");
        }
        AbstractInsnNode errorNode = error.node;
        if (errorNode == null) {
            return;
        }
        List<AbstractInsnNode> insnNodes = new LinkedList<>();
        insnNodes.add(errorNode);
        int i = 0;
        AbstractInsnNode node = errorNode.getPrevious();
        while (i++ < -from && node != null) {
            insnNodes.add(0, node);
            node = node.getPrevious();
        }
        i = 0;
        node = errorNode.getNext();
        while (i ++ < to && node != null) {
            insnNodes.add(node);
            node = node.getNext();
        }
        List<Frame<? extends BasicValue>> frames = new ArrayList<>();
        AbstractInsnNode firstInsn = insnNodes.get(0);
        int firstIndex = methodNode.instructions.indexOf(firstInsn);
        for (i = firstIndex; i < firstIndex + insnNodes.size(); ++i) {
            frames.add(analyzer.getFrames()[i]);
        }

        printWriter.println("Method:");
        Textifier methodPrinter = new Textifier();
        printMethodSign(methodNode, methodPrinter);
        cleanMethodSign(methodPrinter);
        methodPrinter.print(printWriter);
        printWriter.println("============");

        InsnTextifier textifier = new InsnTextifier(insnNodes, frames);
        textifier.getHelper().print(printWriter, true);
        textifier.getTargets().add(errorNode);
        printWriter.println("Instructions:");
        textifier.print(printWriter);
        printWriter.flush();
    }

    public static int storeStackToLocal(int validLocals, Frame<? extends BasicValue> frame, List<AbstractInsnNode> results) {
        int iLocal = validLocals;
        int stackSize = frame.getStackSize();
        for (int i = stackSize - 1; i >= 0; --i) {
            BasicValue value = frame.getStack(i);
            Type type = value.getType();
            if (type != null) {
                results.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), iLocal));
                iLocal += type.getSize();
            } else {
                results.add(new VarInsnNode(Opcodes.ASTORE, iLocal));
                iLocal += 1;
            }
        }
        return iLocal;
    }

    public static void pushLocalToStack(int validLocals, boolean isStatic, Frame<? extends BasicValue> frame, List<AbstractInsnNode> results) {
        int start = isStatic ? 0 : 1;
        for (int i = start; i < validLocals;) {
            BasicValue value = frame.getLocal(i);
            if (value != null && value.getType() != null) {
                Type type = value.getType();
                results.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), i));
                i += type.getSize();
            } else {
                results.add(new InsnNode(Opcodes.ACONST_NULL));
                ++i;
            }
        }
    }

    public static void appendStack(MethodNode methodNode, Frame<?> frame, int appendSize) {
        methodNode.maxStack = Math.max(methodNode.maxStack, frame.getStackSize() + appendSize);
    }

    public static void updateStack(MethodNode methodNode, int newSize) {
        methodNode.maxStack = Math.max(methodNode.maxStack, newSize);
    }

    public static void appendLocal(MethodNode methodNode, Frame<?> frame, int appendSize) {
        methodNode.maxLocals = Math.max(methodNode.maxLocals, frame.getLocals() + appendSize);
    }

    public static void updateLocal(MethodNode methodNode, int newSize) {
        methodNode.maxLocals = Math.max(methodNode.maxLocals, newSize);
    }

    public static LocalReverseIterator reverseIterateLocal(Frame<? extends BasicValue> frame) {
        return new LocalReverseIterator(frame);
    }

    public static class LocalReverseIterator implements Iterator<Integer> {

        private final Frame<? extends BasicValue> frame;
        private int pos;

        public LocalReverseIterator(Frame<? extends BasicValue> frame) {
            this.frame = frame;
            this.pos = this.frame.getLocals();
        }

        @Override
        public boolean hasNext() {
            return pos > 0;
        }

        @Override
        public Integer next() {
            --pos;
            BasicValue value = frame.getLocal(pos);
            if (value != null && value.getType() != null) {
                return pos;
            } else if (pos > 0){
                BasicValue preValue = frame.getLocal(pos - 1);
                if (preValue != null && preValue.getType() != null && preValue.getSize() == 2) {
                    --pos;
                }
                return pos;
            } else {
                return pos;
            }
        }
    }

    public static Type getArrayType(Type elementType, int dim) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dim; ++i) {
            sb.append('[');
        }
        sb.append(elementType.getDescriptor());
        return Type.getType(sb.toString());
    }

    public static Type getComponentType(Type arrayType, boolean recursion) {
        if (arrayType == null || arrayType.getSort() != Type.ARRAY) {
            return null;
        }
        String descriptor = arrayType.getDescriptor();
        if (recursion) {
            String newDescriptor = descriptor.replaceAll("^\\[+", "");
            return Type.getType(newDescriptor);
        } else {
            return Type.getType(descriptor.substring(1));
        }
    }

    public static boolean isSubTypeOf(Type a, Type b) {
        if (Objects.equals(a, b)) {
            return true;
        }
        if (a.getSort() == Type.OBJECT && b.getSort() == Type.OBJECT) {
            return Utils.isSubTypeOf(a.getClassName(), b.getClassName()) == 1;
        }
        if (Constants.OBJECT_DESC.equals(b)) {
            return a.getSort() == Type.OBJECT || a.getSort() == Type.ARRAY;
        }
        if (b.getSort() == Type.ARRAY) {
            return a.getSort() == Type.ARRAY && isSubTypeOf(getComponentType(a, false), getComponentType(b, false));
        }
        return false;
    }

    private static String getInternalName(String binaryName) {
        return binaryName.replaceAll("\\.", "/");
    }

    private static boolean isObjectOrArray(Type type) {
        return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
    }

    public static Type getNearestCommonAncestorType(Type a, Type b) {
        if (Objects.equals(a, b)) {
            return a;
        }
        if (isObjectOrArray(a) && isObjectOrArray(b)) {
            if (a.getSort() == Type.OBJECT && b.getSort() == Type.OBJECT) {
                String ancestor = Utils.getNearestCommonAncestor(a.getClassName(), b.getClassName());
                return ancestor != null ? Type.getObjectType(getInternalName(ancestor)) : Constants.OBJECT_DESC;
            }
            if (a.getSort() == Type.ARRAY && b.getSort() == Type.ARRAY) {
                Type ancestor = getNearestCommonAncestorType(getComponentType(a, false), getComponentType(b, false));
                return getArrayType(ancestor, 1);
            }
            return Constants.OBJECT_DESC;
        }
        return null;
    }

    public static boolean isSubTypeOf(BasicValue value, BasicValue expected) {
        if (Objects.equals(value, expected)) {
            return true;
        }
        if (value.getType() == null || expected.getType() == null) {
            return false;
        }
        boolean isSub = isSubTypeOf(value.getType(), expected.getType());
        if (value instanceof JAsyncValue) {
            JAsyncValue jAsyncValue = (JAsyncValue) value;
            return !jAsyncValue.isUninitialized() && isSub;
        }
        if (expected instanceof JAsyncValue) {
            JAsyncValue jAsyncValue = (JAsyncValue) expected;
            return !jAsyncValue.isUninitialized() && isSub;
        }
        return isSub;
    }

    public static boolean needCastTo(Type from, Type to) {
        return !isSubTypeOf(from, to);
    }

    public static void processConstruct(AbstractInsnNode insn, List<? extends BasicValue> values) {
        if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
            if ("<init>".equals(methodInsnNode.name)) {
                BasicValue newInstValue = values.get(0);
                if (newInstValue instanceof JAsyncValue) {
                    ((JAsyncValue) newInstValue).setUninitialized(false);
                }
            }
        }
    }

    // Used for debug. Because asm package is sharded, so the get method can not be invoked by the debugger.
    @SuppressWarnings("unused")
    public static AbstractInsnNode getInsn(Object methodNode, int i) {
        try {
            Field field = methodNode.getClass().getField("instructions");
            Object insnList = field.get(methodNode);
            Method get = insnList.getClass().getMethod("get", int.class);
            return (AbstractInsnNode) get.invoke(insnList, i);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
