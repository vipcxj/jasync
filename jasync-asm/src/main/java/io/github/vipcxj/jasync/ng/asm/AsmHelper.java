package io.github.vipcxj.jasync.ng.asm;

import io.github.vipcxj.jasync.ng.utils.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings({"unchecked"})
public class AsmHelper {

    public static boolean isStatic(MethodNode mv) {
        return isStatic(mv.access);
    }

    public static int AWAIT_METHOD_TYPE_NOT_AWAIT = 0;
    public static int AWAIT_METHOD_TYPE_AUTO = 1;
    public static int AWAIT_METHOD_TYPE_WITH_TYPE = 2;
    /**
     * check await type
     * @param opcode method opcode
     * @param owner method owner
     * @param name method name
     * @param desc method desc
     * @return 0 for not await, 1 for auto await, 2 for await with await type
     */
    public static int isAwait(int opcode, String owner, String name, String desc) {
        if (Constants.AWAIT.equals(name) || Constants.AWAIT_INTERRUPTABLE.equals(name)) {
            if (opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL) {
                Type[] argumentTypes = Type.getArgumentTypes(desc);
                int awaitType = AWAIT_METHOD_TYPE_NOT_AWAIT;
                if (argumentTypes.length == 0) {
                    awaitType = AWAIT_METHOD_TYPE_AUTO;
                } else if (argumentTypes.length == 1 && Objects.equals(argumentTypes[0], Constants.AWAIT_TYPE_DESC)) {
                    awaitType = AWAIT_METHOD_TYPE_WITH_TYPE;
                } else {
                    return awaitType;
                }
                if (Utils.isJPromise(Type.getObjectType(owner).getClassName())) {
                    return awaitType;
                } else {
                    return AWAIT_METHOD_TYPE_NOT_AWAIT;
                }
            }
        }
        return AWAIT_METHOD_TYPE_NOT_AWAIT;
    }

    /**
     * check await type
     * @param insnNode the insn node to be checked
     * @return 0 for not await, 1 for auto await, 2 for await with await type
     */
    public static int isAwait(AbstractInsnNode insnNode) {
        if (insnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            return isAwait(methodInsnNode.getOpcode(), methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
        }
        return AWAIT_METHOD_TYPE_NOT_AWAIT;
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

    private static List<AbstractInsnNode> getInsnList(MethodNode methodNode) {
        return Arrays.asList(methodNode.instructions.toArray());
    }

    private static void printFrameProblem(
            PrintWriter printWriter,
            String owner,
            MethodNode methodNode,
            AbstractInsnNode errorNode,
            List<AbstractInsnNode> insnNodes,
            List<BranchAnalyzer.Node<BasicValue>> frames,
            List<Integer> map,
            int byteCodeOption
    ) {
        if (!JAsyncInfo.isLogByteCode(byteCodeOption)) {
            return;
        }
        if (owner != null) {
            printWriter.println("Method in " + Type.getObjectType(owner).getClassName() + ":");
        } else {
            printWriter.println("Method:");
        }
        Textifier methodPrinter = new Textifier();
        printMethodSign(methodNode, methodPrinter);
        cleanMethodSign(methodPrinter);
        methodPrinter.print(printWriter);
        printWriter.println("============");

        InsnTextifier textifier = new InsnTextifier(insnNodes, frames, map);
        if (JAsyncInfo.isLogByteCodeWithFrame(byteCodeOption)) {
            textifier.getHelper().print(printWriter, true);
        }
        if (errorNode != null) {
            textifier.getTargets().add(errorNode);
        }
        printWriter.println("Instructions:");
        textifier.print(printWriter, methodNode, byteCodeOption);
        printWriter.flush();
    }

    public static void printFrameProblem(String owner, MethodNode methodNode, BranchAnalyzer.Node<BasicValue>[] frames, List<Integer> map, int byteCodeOption) {
        printFrameProblem(owner, methodNode, frames, map, null, byteCodeOption, -1, 1);
    }

    public static void printFrameProblem(String owner, MethodNode methodNode, BranchAnalyzer.Node<BasicValue>[] frames, List<Integer> map, AnalyzerException error, int byteCodeOption, int from, int to) {
        if (!JAsyncInfo.isLogByteCode(byteCodeOption)) {
            return;
        }
        if (from > 0 || to < 0) {
            throw new IllegalArgumentException("The arg from should greater than 0, and the arg to should less than 0.");
        }
        StringWriter sw = new StringWriter();
        PrintWriter printWriter = new PrintWriter(sw);
        if (frames == null && owner != null) {
            BranchAnalyzer analyzer = new BranchAnalyzer(false);
            try {
                analyzer.analyzeAndComputeMaxs(owner, methodNode);
            } catch (AnalyzerException e) {
                if (error == null) {
                    error = e;
                    from = Integer.MIN_VALUE + 1;
                    to = Integer.MAX_VALUE - 1;
                }
            } catch (Throwable ignored) { } finally {
                frames = analyzer.getNodes();
            }
        }
        if (frames == null) {
            frames = new BranchAnalyzer.Node[0];
        }
        AbstractInsnNode errorNode = error != null ? error.node : null;
        if (errorNode == null) {
            printFrameProblem(
                    printWriter,
                    owner,
                    methodNode,
                    null,
                    getInsnList(methodNode),
                    Arrays.asList(frames),
                    map,
                    byteCodeOption
            );
        } else {
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
            List<BranchAnalyzer.Node<BasicValue>> frameList = new ArrayList<>();
            List<Integer> mapList = new ArrayList<>();
            AbstractInsnNode firstInsn = insnNodes.get(0);
            int firstIndex = methodNode.instructions.indexOf(firstInsn);
            for (i = firstIndex; i < firstIndex + insnNodes.size(); ++i) {
                if (i < frames.length) {
                    frameList.add(frames[i]);
                    if (map != null && i < map.size()) {
                        mapList.add(map.get(i));
                    }
                }
            }
            printFrameProblem(
                    printWriter,
                    owner,
                    methodNode,
                    errorNode,
                    insnNodes,
                    frameList,
                    map != null ? mapList : null,
                    byteCodeOption
            );
        }
        Logger.info(sw.toString());
        printWriter.close();
    }

    public static int storeStackToLocal(int validLocals, BranchAnalyzer.Node<? extends BasicValue> frame, List<AbstractInsnNode> results, AbstractInsnNode[] insnNodes) {
        int iLocal = validLocals;
        int stackSize = frame.getStackSize();
        for (int i = stackSize - 1; i >= 0; --i) {
            BasicValue value = frame.getStack(i);
            if (value instanceof JAsyncValue) {
                JAsyncValue asyncValue = (JAsyncValue) value;
                if (asyncValue.isUninitialized()) {
                    AsmHelper.removeNewInsnNodes(insnNodes, asyncValue, frame);
                    continue;
                }
            }
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

    public static int pushLocalToStack(int validLocals, boolean isStatic, Frame<? extends BasicValue> frame, List<AbstractInsnNode> results) {
        int num = 0;
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
            ++ num;
        }
        return num;
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

    private static boolean isIntLikeType(Type type) {
        return type.getSort() == Type.BOOLEAN
                || type.getSort() == Type.CHAR
                || type.getSort() == Type.BYTE
                || type.getSort() == Type.SHORT
                || type.getSort() == Type.INT;
    }

    public static boolean isSubTypeOf(Type a, Type b, boolean jvm) {
        if (Objects.equals(a, b)) {
            return true;
        }
        if (a.getSort() == Type.OBJECT && b.getSort() == Type.OBJECT) {
            return Utils.isSubTypeOf(a.getClassName(), b.getClassName()) == 1;
        }
        if (Constants.OBJECT_DESC.equals(b)) {
            return a.getSort() == Type.OBJECT || a.getSort() == Type.ARRAY;
        }
        if (Constants.CLONEABLE_DESC.equals(b) || Constants.SERIALIZABLE_DESC.equals(b)) {
            return a.getSort() == Type.ARRAY;
        }
        if (b.getSort() == Type.ARRAY) {
            return a.getSort() == Type.ARRAY && isSubTypeOf(getComponentType(a, false), getComponentType(b, false), false);
        }
        if (jvm && isIntLikeType(b)) {
            return isIntLikeType(a);
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
        return isSubTypeOf(value.getType(), expected.getType(), true);
    }

    public static boolean needCastTo(Type from, Type to) {
        return !isSubTypeOf(from, to, true);
    }

    public static <V extends Value> boolean executeConstruction(Frame<V> frame, AbstractInsnNode insnNode, Interpreter<V> interpreter, FrameExecutor<V> delegated) throws AnalyzerException {
        if (insnNode.getOpcode() == Opcodes.INVOKESPECIAL) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            if ("<init>".equals(methodInsnNode.name)) {
                Type[] argumentTypes = Type.getArgumentTypes(methodInsnNode.desc);
                Value ownerValue = frame.getStack(frame.getStackSize() - argumentTypes.length - 1);
                if (ownerValue instanceof JAsyncValue) {
                    JAsyncValue jAsyncValue = (JAsyncValue) ownerValue;
                    delegated.execute(insnNode, interpreter);
                    for (int i = 0; i < frame.getStackSize(); ++i) {
                        V stack = frame.getStack(i);
                        if (stack == ownerValue) {
                            frame.setStack(i, interpreter.newValue(jAsyncValue.getType()));
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public interface FrameExecutor<V extends Value> {
        void execute(AbstractInsnNode insn, Interpreter<V> interpreter) throws AnalyzerException;
    }

    public static void removeNewInsnNodes(AbstractInsnNode[] insnNodes, JAsyncValue asyncValue, BranchAnalyzer.Node<? extends BasicValue> node) {
        for (BranchAnalyzer.Node<? extends BasicValue> precursor : node.getPrecursors()) {
            AbstractInsnNode insnNode = precursor.getInsnNode();
            if (insnNode == asyncValue.getNewInsnNode() || (asyncValue.getCopyInsnNodes() != null && asyncValue.getCopyInsnNodes().contains(insnNode))) {
                insnNodes[precursor.getIndex()] = null;
            }
            removeNewInsnNodes(insnNodes, asyncValue, precursor);
        }
    }

    /**
     * calc method init local size. include this (for non static method) and arguments.
     * @param methodNode method node
     * @return the init local size.
     */
    public static int calcMethodArgLocals(MethodNode methodNode) {
        return (Type.getMethodType(methodNode.desc).getArgumentsAndReturnSizes() >> 2) - (isStatic(methodNode.access) ? 1 : 0);
    }

    /**
     * find next not used local index.
     * @param methodNode the method node
     * @param start the index to find start from
     * @param size the variable size, must be 1 ro 2
     * @return the next not used local index
     */
    public static int calcFreeVarIndex(MethodNode methodNode, int start, int size) {
        if (size != 1 && size != 2) {
            throw new IllegalArgumentException("size must be 1 or 2.");
        }
        Set<Integer> used = new HashSet<>();
        for (AbstractInsnNode insnNode : methodNode.instructions) {
            if (insnNode instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) insnNode;
                if (varInsnNode.var >= 0) {
                    used.add(varInsnNode.var);
                    switch (varInsnNode.getOpcode()) {
                        case Opcodes.LLOAD:
                        case Opcodes.LSTORE:
                        case Opcodes.DLOAD:
                        case Opcodes.DSTORE:
                            used.add(varInsnNode.var + 1);
                    }
                }
            } else if (insnNode instanceof IincInsnNode) {
                IincInsnNode iincInsnNode = (IincInsnNode) insnNode;
                if (iincInsnNode.var >= 0) {
                    used.add(iincInsnNode.var);
                }
            }
        }
        int max = used.stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        int j = start;
        for (; j < max; ++j) {
            if (!used.contains(j)) {
                if (size == 1 || !used.contains(j + 1)) {
                    return j;
                }
            }
        }
        return j;
    }

    public static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    // Used for debug. Because asm package is sharded, so the get method can not be invoked by the debugger.
    /** @noinspection unused, RedundantSuppression */
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

    private static Class<?> getTargetClassByPattern(Class<?> toTest, String regex) {
        if (toTest == null) {
            return null;
        }
        if (toTest.getName().matches(regex)) {
            return toTest;
        }
        for (Class<?> anInterface : toTest.getInterfaces()) {
            Class<?> found = getTargetClassByPattern(anInterface, regex);
            if (found != null) {
                return found;
            }
        }
        return getTargetClassByPattern(toTest.getSuperclass(), regex);
    }

    // Used for debug. Because asm package is sharded, so the indexOf method can not be invoked by the debugger.
    /** @noinspection unused, RedundantSuppression */
    public static int indexOfInsn(Object methodNode, Object insn) {
        try {
            Field field = methodNode.getClass().getField("instructions");
            Object insnList = field.get(methodNode);
            Method indexOf = insnList.getClass().getMethod("indexOf", getTargetClassByPattern(insn.getClass(), ".*org\\.objectweb\\.asm\\.tree\\.AbstractInsnNode"));
            return (Integer) indexOf.invoke(insnList, insn);
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    // Used for debug. Because asm package is sharded, so the method can not be invoked by the debugger.
    /** @noinspection unused, RedundantSuppression */
    public static String getMethodName(Object insnNode) {
        Class<?> methodInsnClass = getTargetClassByPattern(insnNode.getClass(), ".*org\\.objectweb\\.asm\\.tree\\.MethodInsnNode");
        if (methodInsnClass == null) {
            return null;
        }
        try {
            Field name = methodInsnClass.getField("name");
            return (String) name.get(insnNode);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    // Used for debug. Because asm package is sharded, so the method can not be invoked by the debugger.
    /** @noinspection unused, RedundantSuppression */
    public static String getMethodDesc(Object insnNode) {
        Class<?> methodInsnClass = getTargetClassByPattern(insnNode.getClass(), ".*org\\.objectweb\\.asm\\.tree\\.MethodInsnNode");
        if (methodInsnClass == null) {
            return null;
        }
        try {
            Field name = methodInsnClass.getField("desc");
            return (String) name.get(insnNode);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    public static int objectToPrimitive(MethodNode methodNode, Type type) {
        // stack: ..., Integer
        if (type.getSort() == Type.INT) {
            // t.intValue()
            // stack: ..., Integer -> ..., int
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.INTEGER_NAME,
                    Constants.INTEGER_INT_VALUE_NAME,
                    Constants.INTEGER_INT_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Float
        else if (type.getSort() == Type.FLOAT) {
            // t.floatValue()
            // stack: ..., Float -> ..., float
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.FLOAT_NAME,
                    Constants.FLOAT_FLOAT_VALUE_NAME,
                    Constants.FLOAT_FLOAT_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Long
        else if (type.getSort() == Type.LONG) {
            // t.longValue()
            // stack: ..., Long -> ..., long
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.LONG_NAME,
                    Constants.LONG_LONG_VALUE_NAME,
                    Constants.LONG_LONG_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Double
        else if (type.getSort() == Type.DOUBLE) {
            // t.doubleValue()
            // stack: ..., Double -> ..., double
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.DOUBLE_NAME,
                    Constants.DOUBLE_DOUBLE_VALUE_NAME,
                    Constants.DOUBLE_DOUBLE_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Boolean
        else if (type.getSort() == Type.BOOLEAN) {
            // t.booleanValue()
            // stack: ..., Boolean -> ..., boolean
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.BOOLEAN_NAME,
                    Constants.BOOLEAN_BOOLEAN_VALUE_NAME,
                    Constants.BOOLEAN_BOOLEAN_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Short
        else if (type.getSort() == Type.SHORT) {
            // t.shortValue()
            // stack: ..., Short -> ..., short
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.SHORT_NAME,
                    Constants.SHORT_SHORT_VALUE_NAME,
                    Constants.SHORT_SHORT_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Character
        else if (type.getSort() == Type.CHAR) {
            // t.charValue()
            // stack: ..., Character -> ..., char
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.CHARACTER_NAME,
                    Constants.CHARACTER_CHAR_VALUE_NAME,
                    Constants.CHARACTER_CHAR_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        // stack: ..., Byte
        else if (type.getSort() == Type.BYTE) {
            // t.byteValue()
            // stack: ..., Byte -> ..., byte
            methodNode.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Constants.BYTE_NAME,
                    Constants.BYTE_BYTE_VALUE_NAME,
                    Constants.BYTE_BYTE_VALUE_DESC.getDescriptor(),
                    false
            );
            return 1;
        }
        return 0;
    }

    public static int objectToType(MethodNode methodNode, Type type) {
        if (needCastTo(Constants.OBJECT_DESC, type)) {
            methodNode.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
            return 1;
        } else {
            return 0;
        }
    }

    public static void primitiveToObject(List<AbstractInsnNode> insnNodes, Type type) {
        // stack: ..., int
        if (type.getSort() == Type.INT) {
            // Integer.valueOf(top)
            // stack: ..., int -> ..., Integer
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.INTEGER_NAME,
                    Constants.INTEGER_VALUE_OF_NAME,
                    Constants.INTEGER_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., float
        else if (type.getSort() == Type.FLOAT) {
            // Float.valueOf(top)
            // stack: ..., float -> ..., Float
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.FLOAT_NAME,
                    Constants.FLOAT_VALUE_OF_NAME,
                    Constants.FLOAT_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., long
        else if (type.getSort() == Type.LONG) {
            // Long.valueOf(top)
            // stack: ..., long -> ..., Long
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.LONG_NAME,
                    Constants.LONG_VALUE_OF_NAME,
                    Constants.LONG_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., double
        else if (type.getSort() == Type.DOUBLE) {
            // Double.valueOf(top)
            // stack: ..., double -> ..., Double
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.DOUBLE_NAME,
                    Constants.DOUBLE_VALUE_OF_NAME,
                    Constants.DOUBLE_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., boolean
        else if (type.getSort() == Type.BOOLEAN) {
            // Boolean.valueOf(top)
            // stack: ..., boolean -> ..., Boolean
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.BOOLEAN_NAME,
                    Constants.BOOLEAN_VALUE_OF_NAME,
                    Constants.BOOLEAN_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., short
        else if (type.getSort() == Type.SHORT) {
            // Short.valueOf(top)
            // stack: ..., short -> ..., Short
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.SHORT_NAME,
                    Constants.SHORT_VALUE_OF_NAME,
                    Constants.SHORT_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., char
        else if (type.getSort() == Type.CHAR) {
            // Character.valueOf(top)
            // stack: ..., char -> ..., Character
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.CHARACTER_NAME,
                    Constants.CHARACTER_VALUE_OF_NAME,
                    Constants.CHARACTER_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
        // stack: ..., byte
        else if (type.getSort() == Type.BYTE) {
            // Byte.valueOf(top)
            // stack: ..., byte -> ..., Byte
            insnNodes.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Constants.BYTE_NAME,
                    Constants.BYTE_VALUE_OF_NAME,
                    Constants.BYTE_VALUE_OF_DESC.getDescriptor(),
                    false
            ));
        }
    }

    public static void visitConstantInt(MethodNode methodNode, int i) {
        if (i == 0) {
            methodNode.visitInsn(Opcodes.ICONST_0);
        } else if (i == 1) {
            methodNode.visitInsn(Opcodes.ICONST_1);
        } else if (i == 2) {
            methodNode.visitInsn(Opcodes.ICONST_2);
        } else if (i == 3) {
            methodNode.visitInsn(Opcodes.ICONST_3);
        } else if (i == 4) {
            methodNode.visitInsn(Opcodes.ICONST_4);
        } else if (i == 5) {
            methodNode.visitInsn(Opcodes.ICONST_5);
        } else if (i == -1){
            methodNode.visitInsn(Opcodes.ICONST_M1);
        } else if ((i & 0xffffff00) == 0) {
            methodNode.visitIntInsn(Opcodes.BIPUSH, i);
        } else if ((i & 0xffff0000) == 0) {
            methodNode.visitIntInsn(Opcodes.SIPUSH, i);
        } else {
            methodNode.visitLdcInsn(i);
        }
    }

    public static AbstractInsnNode loadConstantInt(int i) {
        if (i == 0) {
            return new InsnNode(Opcodes.ICONST_0);
        } else if (i == 1) {
            return new InsnNode(Opcodes.ICONST_1);
        } else if (i == 2) {
            return new InsnNode(Opcodes.ICONST_2);
        } else if (i == 3) {
            return new InsnNode(Opcodes.ICONST_3);
        } else if (i == 4) {
            return new InsnNode(Opcodes.ICONST_4);
        } else if (i == 5) {
            return new InsnNode(Opcodes.ICONST_5);
        } else if (i == -1){
            return new InsnNode(Opcodes.ICONST_M1);
        } else if ((i & 0xffffff00) == 0) {
            return new IntInsnNode(Opcodes.BIPUSH, i);
        } else if ((i & 0xffff0000) == 0) {
            return new IntInsnNode(Opcodes.SIPUSH, i);
        } else {
            return new LdcInsnNode(i);
        }
    }

    public static boolean isStoreInsn(AbstractInsnNode insn) {
        return insn.getOpcode() == Opcodes.ISTORE
                || insn.getOpcode() == Opcodes.LSTORE
                || insn.getOpcode() == Opcodes.FSTORE
                || insn.getOpcode() == Opcodes.DSTORE
                || insn.getOpcode() == Opcodes.ASTORE;
    }
}
