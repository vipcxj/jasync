package io.github.vipcxj.jasync.core.javac.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.JavacUtils;

import java.util.Objects;

public class SimplifiedTranslator extends TreeTranslator {

    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation) {
        JCTree.JCMethodInvocation nested = null;
        if (isDeferVoid(jcMethodInvocation) && jcMethodInvocation.args.size() == 1) {
            JCTree.JCExpression arg = jcMethodInvocation.args.head;
            if (arg instanceof JCTree.JCLambda) {
                JCTree.JCLambda lambdaArg = (JCTree.JCLambda) arg;
                if (lambdaArg.body instanceof JCTree.JCBlock) {
                    JCTree.JCBlock body = (JCTree.JCBlock) lambdaArg.body;
                    nested = extractDeferStartFromBody(body);
                } else if (lambdaArg.body instanceof JCTree.JCMethodInvocation) {
                    JCTree.JCMethodInvocation expr = (JCTree.JCMethodInvocation) lambdaArg.body;
                    if (isDeferStart(expr)) {
                        nested = expr;
                    }
                }
            } else if (arg instanceof JCTree.JCNewClass) {
                JCTree.JCNewClass newClass = (JCTree.JCNewClass) arg;
                if (newClass.def != null && newClass.def.defs != null) {
                    for (JCTree def : newClass.def.defs) {
                        if (def instanceof JCTree.JCMethodDecl) {
                            JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) def;
                            if (methodDecl.name.toString().equals("get")) {
                                JCTree.JCBlock body = methodDecl.body;
                                nested = extractDeferStartFromBody(body);
                            }
                        }
                    }
                }
            }
        }
        if (nested != null) {
            nested = translate(nested);
            result = nested;
        } else {
            super.visitApply(jcMethodInvocation);
        }
    }

    private JCTree.JCMethodInvocation extractDeferStartFromBody(JCTree.JCBlock body) {
        if (body.stats.size() == 1) {
            JCTree.JCStatement statement = body.stats.head;
            if (statement instanceof JCTree.JCReturn) {
                JCTree.JCReturn jcReturn = (JCTree.JCReturn) statement;
                if (jcReturn.expr instanceof JCTree.JCMethodInvocation) {
                    JCTree.JCMethodInvocation expr = (JCTree.JCMethodInvocation) jcReturn.expr;
                    if (isDeferStart(expr)) {
                        return expr;
                    }
                }
            }
        }
        return null;
    }

    private boolean isDeferVoid(JCTree.JCMethodInvocation jcMethodInvocation) {
        JCTree.JCExpression methodSelect = jcMethodInvocation.getMethodSelect();
        if (methodSelect instanceof JCTree.JCFieldAccess) {
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodSelect;
            return Objects.equals(JavacUtils.getQualifiedName(fieldAccess), Constants.JASYNC_DEFER_VOID);
        }
        return false;
    }

    private boolean isDeferStart(JCTree.JCMethodInvocation jcMethodInvocation) {
        if (isDeferVoid(jcMethodInvocation)) {
            return true;
        }
        JCTree.JCExpression methodSelect = jcMethodInvocation.getMethodSelect();
        if (methodSelect instanceof JCTree.JCFieldAccess) {
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodSelect;
            if (fieldAccess.getExpression() instanceof JCTree.JCMethodInvocation) {
                return isDeferStart((JCTree.JCMethodInvocation) fieldAccess.getExpression());
            }
        }
        return false;
    }
}
