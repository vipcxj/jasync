package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public class NormalCodePiece extends CodePiece {

    protected NormalCodePiece(MethodContext methodContext, CodePiece parent, int from, int to) {
        super(methodContext, parent, from, to);
    }

    @Override
    public InsnList transform() {
        InsnList newList = new InsnList();
        for (InsnWithIndex insnWithIndex : insnNodes) {
            AbstractInsnNode node = insnWithIndex.getInsnNode();
            if (node instanceof CodePieceInsnNode) {
                CodePiece codePiece = ((CodePieceInsnNode) node).getCodePiece();
                newList.add(codePiece.transform());
            } else {
                newList.add(node);
            }
        }
        return newList;
    }
}
