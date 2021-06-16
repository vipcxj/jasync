package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.source.tree.*;

public class ReturnScanner extends ShallowTreeBooleanScanner {

    @Override
    public Boolean visitThrow(ThrowTree throwTree, Void aVoid) {
        return true;
    }

    @Override
    public Boolean visitReturn(ReturnTree returnTree, Void aVoid) {
        return true;
    }

    @Override
    public Boolean visitTry(TryTree tryTree, Void aVoid) {
        Boolean blockReturned = scan(tryTree.getBlock(), null);
        if (Boolean.TRUE.equals(blockReturned)) {
            for (CatchTree aCatch : tryTree.getCatches()) {
                Boolean catchReturned = scan(aCatch, null);
                if (!Boolean.TRUE.equals(catchReturned)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean visitIf(IfTree ifTree, Void aVoid) {
        Boolean thenReturned = scan(ifTree.getThenStatement(), null);
        Boolean elseReturned = scan(ifTree.getElseStatement(), null);
        return Boolean.TRUE.equals(thenReturned) && Boolean.TRUE.equals(elseReturned);
    }

    @Override
    public Boolean visitBlock(BlockTree blockTree, Void aVoid) {
        for (StatementTree statement : blockTree.getStatements()) {
            Boolean returned = scan(statement, null);
            if (returned != null && returned) {
                return true;
            }
        }
        return false;
    }

    public static boolean scanBlock(BlockTree blockTree) {
        return blockTree.accept(new ReturnScanner(), null);
    }
}
