package io.github.vipcxj.jasync.core.javac;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.concurrent.atomic.AtomicInteger;

public class JAsyncContext implements IJAsyncContext {

    private static final AtomicInteger iter = new AtomicInteger(0);

    protected final ProcessingEnvironment environment;
    protected final Context context;
    protected final TreeMaker treeMaker;
    protected final Names names;
    protected final JavacTrees trees;
    protected final Attr attr;
    protected final Log log;
    protected final Types types;
    protected final Symtab syms;
    protected final JAsyncSymbols jAsyncSymbols;

    public JAsyncContext(final ProcessingEnvironment environment) {
        this.environment = environment;
        this.trees = JavacTrees.instance(environment);
        this.context = ((JavacProcessingEnvironment) environment).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
        this.attr = Attr.instance(context);
        this.syms = Symtab.instance(context);
        this.log = Log.instance(context);
        this.types = Types.instance(context);
        this.jAsyncSymbols = JAsyncSymbols.instance(context);
    }

    public JAsyncContext(IJAsyncContext asyncContext) {
        this.environment = asyncContext.getEnvironment();
        this.trees = asyncContext.getTrees();
        this.context = asyncContext.getContext();
        this.treeMaker = asyncContext.getTreeMaker();
        this.names = asyncContext.getNames();
        this.attr = asyncContext.getAttr();
        this.log = asyncContext.getLog();
        this.syms = asyncContext.getSymbols();
        this.types = asyncContext.getTypes();
        this.jAsyncSymbols = asyncContext.getJAsyncSymbols();
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
    public Attr getAttr() {
        return attr;
    }

    @Override
    public Log getLog() {
        return log;
    }

    @Override
    public Symtab getSymbols() {
        return syms;
    }

    @Override
    public JAsyncSymbols getJAsyncSymbols() {
        return jAsyncSymbols;
    }

    @Override
    public Types getTypes() {
        return types;
    }

    @Override
    public String nextVar() {
        return "tmp$$" + iter.getAndIncrement();
    }
}
