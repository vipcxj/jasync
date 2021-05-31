package io.github.vipcxj.jasync.core.javac;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;

public class ReturnScanner extends TreeScanner<Boolean, Void> {

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
        return Boolean.TRUE.equals(r1) || Boolean.TRUE.equals(r2);
    }

    @Override
    public Boolean visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitClass(ClassTree classTree, Void aVoid) {
        return false;
    }

    @Override
    public Boolean visitMethod(MethodTree methodTree, Void aVoid) {
        return false;
    }

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
        if (blockReturned != null && blockReturned) {
            for (CatchTree aCatch : tryTree.getCatches()) {
                Boolean catchReturned = scan(aCatch, null);
                if (catchReturned == null || !catchReturned) {
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
        return thenReturned != null && elseReturned != null && thenReturned && elseReturned;
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
