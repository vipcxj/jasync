package io.github.vipcxj.jasync.ng.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;

public class Arguments {
    private final List<ExtendType> types;
    private final List<String> names;
    private int uninitializedNum;

    public Arguments() {
        this.types = new ArrayList<>();
        this.names = new ArrayList<>();
        this.uninitializedNum = 0;
    }

    Arguments(Arguments arguments, int from, int to) {
        this.types = new ArrayList<>();
        this.names = new ArrayList<>();
        for (int i = from; i < to; ++i) {
            ExtendType type = arguments.types.get(i);
            String name = arguments.names.get(i);
            this.types.add(type);
            this.names.add(name);
            if (!type.isInitialized()) {
                ++this.uninitializedNum;
            }
        }
    }

    public void addArgument(BasicValue value, String name) {
        boolean uninitialized = false;
        if (value instanceof JAsyncValue) {
            JAsyncValue asyncValue = (JAsyncValue) value;
            if (asyncValue.isUninitialized()) {
                uninitialized = true;
            }
        }
        Type type = value.getType();
        if (uninitialized) {
            addArgument(type, true, value, name);
        } else {
            addArgument(type, false, null, name);
        }
    }

    private void addArgument(Type type, boolean uninitialized, Object ref, String name) {
        type = (type != null && !type.equals(Constants.NULL_DESC)) ? type : Constants.OBJECT_DESC;
        ExtendType newType = null;
        if (ref != null) {
            for (int i = types.size() - 1; i >= 0; --i) {
                ExtendType extendType = types.get(i);
                if (extendType.ref == ref) {
                    newType = new ExtendType(type, uninitialized, ref);
                    newType.offset = types.size() - i;
                    break;
                }
            }
        }
        if (newType == null) {
            newType = new ExtendType(type ,uninitialized, ref);
        }
        types.add(newType);
        names.add(name);
        if (uninitialized) {
            ++uninitializedNum;
        }
    }

    public void addArgument(Type type, String name) {
        addArgument(type, false, null, name);
    }

    public List<ExtendType> getTypes() {
        return types;
    }

    public int argumentSize() {
        int size = 0;
        for (ExtendType type : types) {
            if (type.isInitialized()) {
                size += type.getType().getSize();
            }
        }
        return size;
    }

    public int argumentLocalOffset(boolean isStatic, int i) {
        int offset = isStatic ? 0 : 1;
        int index = i >= 0 ? i : (types.size() - uninitializedNum + i);
        int localOffset = 0;
        int j = 0;
        for (ExtendType type : types) {
            if (type.isInitialized()) {
                if (j++ == index) {
                    return localOffset + offset;
                } else {
                    localOffset += type.getType().getSize();
                }
            }
        }
        throw new IllegalArgumentException("Invalid index: " + i);
    }

    public Type[] argTypes(int strip) {
        Type[] results = new Type[types.size() - uninitializedNum - strip];
        int i = 0;
        Iterator<ExtendType> iterator = types.iterator();
        while (i < results.length) {
            ExtendType extendType = iterator.next();
            if (extendType.isInitialized()) {
                results[i++] = extendType.getType();
            }
        }
        return results;
    }

    public LocalVariableNode[] toLocalVariables(boolean isStatic) {
        int offset = isStatic ? 0 : 1;
        LocalVariableNode[] variableNodes = new LocalVariableNode[types.size() - uninitializedNum + offset];
        int i = offset;
        Iterator<ExtendType> iterator = types.iterator();
        Iterator<String> nameIterator = names.iterator();
        while (i < variableNodes.length) {
            ExtendType extendType = iterator.next();
            if (extendType.isInitialized()) {
                String name = nameIterator.next();
                variableNodes[i] = new LocalVariableNode(name, extendType.getType().getDescriptor(), null, null, null, i);
                ++i;
            }
        }
        return variableNodes;
    }

    public static Arguments of(Object... typeAndNames) {
        if (typeAndNames.length % 2 != 0) {
            throw new IllegalArgumentException("The number of inputs must be even.");
        }
        Arguments arguments = new Arguments();
        for (int i = 0; i < typeAndNames.length;) {
            Type type = (Type) typeAndNames[i++];
            String name = (String) typeAndNames[i++];
            arguments.addArgument(type, false, null, name);
        }
        return arguments;
    }

    public static Arguments from(Arguments arguments, int from, int to) {
        return new Arguments(arguments, from, to);
    }

    public static class ExtendType {
        private final Type type;
        private final boolean uninitialized;
        private final Object ref;
        private int offset;

        public ExtendType(Type type, boolean uninitialized, Object ref) {
            this.type = Objects.requireNonNull(type);
            this.uninitialized = uninitialized;
            this.ref = ref;
            this.offset = 0;
        }

        public Type getType() {
            return type;
        }

        public boolean isInitialized() {
            return !uninitialized;
        }

        /**
         * 引用对象在堆栈的偏移。
         * 例如 NEW 指令后，堆栈压入一个未初始化对象，因为是刚 new 的，所以在这之前堆栈内不存在其他引用，所以
         * @return offset
         */
        public int getOffset() {
            return offset;
        }

        public int getSize() {
            return type.getSize();
        }

        public int getOpcode(int opcode) {
            return type.getOpcode(opcode);
        }
    }
}
