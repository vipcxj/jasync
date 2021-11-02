package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.tree.InsnList;

public class PortalCodePiece extends CodePiece {

    protected PortalCodePiece(MethodContext methodContext, CodePiece parent, int from, int to) {
        super(methodContext, parent, from, to);
    }

    @Override
    public InsnList transform() {
        return null;
    }
}
