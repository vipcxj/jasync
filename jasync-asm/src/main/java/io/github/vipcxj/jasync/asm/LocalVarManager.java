package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.Type;

import java.util.Arrays;

public class LocalVarManager {
    private int[] all;
    private int[] used;
    private int[] units;
    private int newVar;

    public LocalVarManager() {
        all = new int[9];
        Arrays.fill(all, 0);
        used = new int[9];
        Arrays.fill(used, 0);
        units = new int[9];
        Arrays.fill(units, 1);
        units[7] = 2;
        units[8] = 2;
        newVar = 0;
    }

    private int getIndex(int sort) {
        switch (sort) {
            case Type.ARRAY:
            case Type.OBJECT:
            case Type.VOID:
                return 0;
            case Type.BOOLEAN:
                return 1;
            case Type.CHAR:
                return 2;
            case Type.BYTE:
                return 3;
            case Type.SHORT:
                return 4;
            case Type.INT:
                return 5;
            case Type.FLOAT:
                return 6;
            case Type.LONG:
                return 7;
            case Type.DOUBLE:
                return 8;
            default:
                throw new IllegalArgumentException("Invalid local var type: " + sort + ".");
        }
    }

    private int calcOffset(int sort) {
        int index = getIndex(sort);
        int offset = 0;
        for (int i = 0; i < index; ++i) {
            offset += all[i] * units[i];
        }
        return offset;
    }

    public int newLocal(Type type) {
        int sort = type.getSort();
        int index = getIndex(sort);
        ++used[index];
        if (all[index] < used[index]) {
            all[index] = used[index];
            ++newVar;
        }
        return calcOffset(sort) + used[index] - 1;
    }

    public void reset() {
        Arrays.fill(used, 0);
    }

    public int used() {
        return newVar;
    }
}
