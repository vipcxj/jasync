package io.github.vipcxj.jasync.core.javac;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.concurrent.atomic.AtomicInteger;

public class JAsyncContext implements IJAsyncContext {

    private static final AtomicInteger iter = new AtomicInteger(0);

    protected final ProcessingEnvironment environment;
    protected final Context context;
    protected final TreeMaker treeMaker;
    protected final Names names;
    protected final JavacTrees trees;

    public JAsyncContext(final ProcessingEnvironment environment) {
        this.environment = environment;
        this.trees = JavacTrees.instance(environment);
        this.context = ((JavacProcessingEnvironment) environment).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    public JAsyncContext(IJAsyncContext asyncContext) {
        this.environment = asyncContext.getEnvironment();
        this.trees = asyncContext.getTrees();
        this.context = asyncContext.getContext();
        this.treeMaker = asyncContext.getTreeMaker();
        this.names = asyncContext.getNames();
    }

    @Override
    public ProcessingEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public TreeMaker getTreeMaker() {
        return treeMaker;
    }

    @Override
    public Names getNames() {
        return names;
    }

    @Override
    public JavacTrees getTrees() {
        return trees;
    }

    @Override
    public String nextVar() {
        return "tmp$$" + iter.getAndIncrement();
    }
}
