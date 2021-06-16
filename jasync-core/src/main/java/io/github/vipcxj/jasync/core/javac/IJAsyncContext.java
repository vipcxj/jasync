package io.github.vipcxj.jasync.core.javac;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.ProcessingEnvironment;

public interface IJAsyncContext {

    ProcessingEnvironment getEnvironment();
    Context getContext();
    TreeMaker getTreeMaker();
    Names getNames();
    JavacTrees getTrees();
    Attr getAttr();
    Log getLog();
    Symtab getSymbols();
    Types getTypes();
    String nextVar();
}
