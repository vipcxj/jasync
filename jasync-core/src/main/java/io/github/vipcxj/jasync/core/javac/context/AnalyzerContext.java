package io.github.vipcxj.jasync.core.javac.context;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.*;
import io.github.vipcxj.jasync.core.javac.translate.context.TransAwaitContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransIdentContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransVarDeclContext;

public class AnalyzerContext {

    private final IJAsyncInstanceContext jasyncContext;
    public List<Frame> frameStack;
    public TranslateContext<?> currentTranslateContext;

    public AnalyzerContext(IJAsyncInstanceContext jasyncContext) {
        this.jasyncContext = jasyncContext;
        this.frameStack = List.nil();
        this.currentTranslateContext = null;
    }

    public IJAsyncInstanceContext getJasyncContext() {
        return jasyncContext;
    }

    public Frame currentFrame() {
        return frameStack.head;
    }

    public void addLocal(TransVarDeclContext context) {
        currentFrame().addLocal(context);
    }

    public boolean isLocal(Symbol.VarSymbol symbol) {
        return currentFrame().isLocal(symbol);
    }

    public void addPlaceHolder(JCTree tree, boolean param) {
        currentFrame().addPlaceHolder(tree, getJasyncContext().nextVarName(), param);
    }

    public void readVar(TransIdentContext context) {
        currentFrame().readVar(context);
    }

    public void writeVar(TransWriteExprContext<?> context) {
        currentFrame().writeVar(context);
    }

    public void readPlaceHolder(TransPlaceHolderContext<?> context) {
        currentFrame().readPlaceHolder(context);
    }

    public boolean shouldBeReplaced(JCTree tree) {
        TranslateContext<?> translateContext = currentTranslateContext;
        while (translateContext != null) {
            if (translateContext instanceof TransAwaitContext) {
                if (((TransAwaitContext) translateContext).shouldBeReplaced(tree)) {
                    return true;
                }
            }
            translateContext = translateContext.getParent();
        }
        return false;
    }

    public Frame.PlaceHolderInfo getDeclPlaceHolder(JCTree tree) {
        Frame frame = currentFrame();
        while (frame != null) {
            Frame.PlaceHolderInfo placeHolder = frame.getDeclaredPlaceHolders().get(tree);
            if (placeHolder != null) {
                return placeHolder;
            }
            frame = frame.getParent();
        }
        return null;
    }

    public List<Frame> stack() {
        return frameStack;
    }

    public Frame enter(TransFrameHolderContext<?> holder) {
        Frame preFrame = this.frameStack.head;
        this.frameStack = this.frameStack.prepend(new Frame(preFrame, holder));
        return preFrame;
    }

    public void exit() {
        this.frameStack = this.frameStack.tail;
    }

    public void exitTo(Frame frame) {
        while (!frameStack.isEmpty() && currentFrame() != frame) {
            exit();
        }
        if (currentFrame() != frame) {
            throw new IllegalStateException("The frame stack is broken.");
        }
    }

}
