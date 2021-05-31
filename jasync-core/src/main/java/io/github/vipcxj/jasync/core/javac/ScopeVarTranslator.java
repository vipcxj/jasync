package io.github.vipcxj.jasync.core.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Element;
import java.util.Map;

public class ScopeVarTranslator extends TreeTranslator {

    private final IJAsyncCuContext context;
    private final Map<VarKey, VarInfo> varData;

    public ScopeVarTranslator(IJAsyncCuContext context, Map<VarKey, VarInfo> varData) {
        this.context = context;
        this.varData = varData;
    }

    private VarInfo getVarInfo(JCTree tree) {
        Element element = context.getElement(tree);
        if (element instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) element;
            for (VarInfo info : varData.values()) {
                if (JavacUtils.equalSymbol(varSymbol, info.getSymbol())) {
                    return info;
                }
            }
        }
        return null;
    }

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent) {
        VarInfo varInfo = getVarInfo(jcIdent);
        if (varInfo != null && varInfo.getState() == VarUseState.WRITE) {
            TreeMaker maker = context.getTreeMaker();
            Names names = context.getNames();
            int prePos = maker.pos;
            result = maker.at(jcIdent).Apply(
                    List.nil(),
                    maker.Select(
                            maker.Ident(jcIdent.name),
                            names.fromString(Constants.REFERENCE_GET)
                    ),
                    List.nil()
            );
            maker.pos = prePos;
            return;
        }
        super.visitIdent(jcIdent);
    }

    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        VarInfo varInfo = getVarInfo(jcAssign.lhs);
        if (varInfo != null) {
            TreeMaker maker = context.getTreeMaker();
            Names names = context.getNames();
            if (varInfo.getState() == VarUseState.WRITE) {
                JCTree.JCExpression rhs = translate(jcAssign.rhs);
                int prePos = maker.pos;
                result = maker.at(jcAssign).Apply(
                        List.nil(),
                        maker.at(jcAssign.lhs).Select(
                                maker.Ident(varInfo.getSymbol().name),
                                names.fromString(Constants.REFERENCE_ASSIGN)
                        ),
                        List.of(rhs)
                );
                maker.pos = prePos;
                return;
            }
        }
        super.visitAssign(jcAssign);
    }

    private String getAssignMethod(JCTree.Tag tag) {
        switch (tag) {
            case PREINC:
                return Constants.REFERENCE_PRE_INC;
            case PREDEC:
                return Constants.REFERENCE_PRE_DEC;
            case POSTINC:
                return Constants.REFERENCE_POST_INC;
            case POSTDEC:
                return Constants.REFERENCE_POST_DEC;
            case PLUS_ASG:
                return Constants.REFERENCE_PLUS_ASSIGN;
            case MINUS_ASG:
                return Constants.REFERENCE_MINUS_ASSIGN;
            case DIV_ASG:
                return Constants.REFERENCE_DIVIDE_ASSIGN;
            case MUL_ASG:
                return Constants.REFERENCE_MULTIPLY_ASSIGN;
            case MOD_ASG:
                return Constants.REFERENCE_MOD_ASSIGN;
            case SL_ASG:
                return Constants.REFERENCE_LEFT_SHIFT_ASSIGN;
            case SR_ASG:
                return Constants.REFERENCE_RIGHT_SHIFT_ASSIGN;
            case USR_ASG:
                return Constants.REFERENCE_UNSIGNED_RIGHT_SHIFT_ASSIGN;
            case BITAND_ASG:
                return Constants.REFERENCE_LOGIC_AND_ASSIGN;
            case BITOR_ASG:
                return Constants.REFERENCE_LOGIC_OR_ASSIGN;
            case BITXOR_ASG:
                return Constants.REFERENCE_LOGIC_XOR_ASSIGN;
            default:
                throw new IllegalArgumentException("unrecognized assign tag " + tag);
        }
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
        VarInfo varInfo = getVarInfo(jcAssignOp.lhs);
        if (varInfo != null && varInfo.getState() == VarUseState.WRITE) {
            String assignMethod = getAssignMethod(jcAssignOp.getTag());
            JCTree.JCExpression rhs = translate(jcAssignOp.rhs);
            TreeMaker maker = context.getTreeMaker();
            Names names = context.getNames();
            int prePos = maker.pos;
            result = maker.at(jcAssignOp).Apply(
                    List.nil(),
                    maker.at(jcAssignOp.lhs).Select(
                            maker.Ident(varInfo.getSymbol().name),
                            names.fromString(assignMethod)
                    ),
                    List.of(rhs)
            );
            maker.pos = prePos;
            return;
        }
        super.visitAssignop(jcAssignOp);
    }

    @Override
    public void visitUnary(JCTree.JCUnary jcUnary) {
        VarInfo varInfo = getVarInfo(jcUnary.arg);
        if (varInfo != null && varInfo.getState() == VarUseState.WRITE) {
            String assignMethod = getAssignMethod(jcUnary.getTag());
            TreeMaker maker = context.getTreeMaker();
            Names names = context.getNames();
            int prePos = maker.pos;
            result = maker.at(jcUnary).Apply(
                    List.nil(),
                    maker.at(jcUnary.arg).Select(
                            maker.Ident(varInfo.getSymbol().name),
                            names.fromString(assignMethod)
                    ),
                    List.nil()
            );
            maker.pos = prePos;
            return;
        }
        super.visitUnary(jcUnary);
    }
}
