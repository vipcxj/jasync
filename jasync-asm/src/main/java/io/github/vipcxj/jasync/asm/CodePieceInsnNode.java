package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Map;

public class CodePieceInsnNode extends AbstractInsnNode {

    private final CodePiece codePiece;

    /**
     * Constructs a new {@link AbstractInsnNode}.
     * @param codePiece
     */
    protected CodePieceInsnNode(CodePiece codePiece) {
        super(-1);
        this.codePiece = codePiece;
    }

    public CodePiece getCodePiece() {
        return codePiece;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public void accept(MethodVisitor methodVisitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractInsnNode clone(Map<LabelNode, LabelNode> clonedLabels) {
        throw new UnsupportedOperationException();
    }
}
