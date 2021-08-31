package io.github.vipcxj.jasync.core.javac.visitor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import io.github.vipcxj.jasync.core.javac.model.Ancestors;

public abstract class AncestorsAnalyzer extends TreeScanner {

    protected Ancestors ancestors = new Ancestors();

    protected List<Object> preScan(JCTree tree) {
        ancestors.enter(tree);
        return List.nil();
    }
    protected void postScan(JCTree tree, List<Object> preScanResults) {
        ancestors.exit();
    }

    @Override
    public void scan(JCTree tree) {
        List<Object> res = preScan(tree);
        try {
            super.scan(tree);
        } finally {
            postScan(tree, res);
        }
    }


}
