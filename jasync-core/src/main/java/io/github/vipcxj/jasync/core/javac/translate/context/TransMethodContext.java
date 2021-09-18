package io.github.vipcxj.jasync.core.javac.translate.context;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.*;
import io.github.vipcxj.jasync.core.javac.Constants;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.AnalyzerContext;
import io.github.vipcxj.jasync.core.javac.model.Frame;
import io.github.vipcxj.jasync.core.javac.translate.TransFrameHolderContext;
import io.github.vipcxj.jasync.core.javac.translate.TranslateContext;
import io.github.vipcxj.jasync.core.javac.utils.SymbolHelpers;
import io.github.vipcxj.jasync.spec.JPromise;

import javax.lang.model.util.Elements;
import java.util.*;
import java.util.function.Predicate;

public class TransMethodContext extends AbstractTransFrameHolderContext<JCTree.JCMethodDecl> {

    private final JCTree.JCClassDecl enclosingClassTree;
    private final boolean enclosingClassTopLevel;
    private final String enclosingClassName;
    private final static Map<String, String> indyHelperMap = new HashMap<>();
    private final static Map<String, String> indyHelpersMap = new HashMap<>();
    private Symbol.VarSymbol indyHelpersVarSymbol;
    private Symbol.VarSymbol indyHelperVarSymbol;
    private final List<TransVarDeclContext> paramsContext;
    private TransBlockContext bodyContext;
    private final boolean staticMethod;

    public TransMethodContext(AnalyzerContext analyzerContext, JCTree.JCMethodDecl tree) {
        super(analyzerContext, tree);
        JCTree.JCCompilationUnit cu = JavacUtils.getJCCompilationUnitTree(analyzerContext.getJasyncContext());
        EnclosingClassFinder finder = new EnclosingClassFinder(tree);
        cu.accept(finder);
        enclosingClassTree = finder.getEnclosingClassTree();
        enclosingClassTopLevel = finder.isTopLevel();
        String packageName = cu.packge.fullname.toString();
        if (packageName.isEmpty()) {
            enclosingClassName = finder.getTargetName();
        } else {
            enclosingClassName = packageName + "." + finder.getTargetName();
        }
        paramsContext = List.nil();
        staticMethod = (tree.getModifiers().flags & Flags.STATIC) != 0;
    }

    @Override
    public TransMethodContext getEnclosingMethodContext() {
        return this;
    }

    @Override
    public TranslateContext<JCTree.JCMethodDecl> enter() {
        super.enter();
        return this;
    }

    @Override
    public boolean isAwaitGap() {
        return true;
    }

    @Override
    protected void addNormalChildContext(TranslateContext<?> child) {
        int index = paramsContext.size();
        if (tree.params != null && index < tree.params.size() && tree.params.get(index) == child.getTree()) {
            childContextMustBeVarDecl(child);
            paramsContext.append((TransVarDeclContext) child);
        } else if (tree.body != null && tree.body == child.getTree()) {
            childContextMustBeBlock(child);
            bodyContext = (TransBlockContext) child;
            bodyContext.setProxyFrame(true);
            bodyContext.setNude(true);
            bodyContext.setDirect(true);
        }
    }

    private static String decideNameIn(JCTree.JCClassDecl classDecl, String prefix, int index) {
        String name = prefix + "$$" + index;
        boolean found = false;
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) def;
                if (name.equals(decl.name.toString())) {
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            name = decideNameIn(classDecl, prefix, index + 1);
        }
        return name;
    }

    private static JCTree.JCVariableDecl findVarDecl(JCTree.JCClassDecl classDecl, Name name) {
        for (JCTree def : classDecl.defs) {
            if (def instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) def;
                if (decl.name.equals(name)) {
                    return decl;
                }
            }
        }
        return null;
    }

    private boolean methodNameHasUsed(String name) {
        for (JCTree def : enclosingClassTree.defs) {
            if (def instanceof JCTree.JCMethodDecl) {
                if (((JCTree.JCMethodDecl) def).name.toString().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Name nextMethodName() {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        String nextMethod = tree.name.toString() + "$$" + jasyncContext.nextVar();
        while (methodNameHasUsed(nextMethod)) {
            nextMethod = tree.name.toString() + "$$" + jasyncContext.nextVar();
        }
        return jasyncContext.getNames().fromString(nextMethod);
    }

    private boolean isEnclosingClassStatic() {
        return enclosingClassTopLevel || (enclosingClassTree.getModifiers().flags & Flags.STATIC) != 0;
    }

    public Symbol.VarSymbol getIndyHelpers() {
        if (indyHelpersVarSymbol == null) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            Names names = jasyncContext.getNames();
            Elements elements = jasyncContext.getEnvironment().getElementUtils();
            String indyHelpersVar = indyHelpersMap.get(enclosingClassName);
            if (indyHelpersVar != null) {
                Name indyHelpersName = names.fromString(indyHelpersVar);
                JCTree.JCVariableDecl indyHelpersDecl = findVarDecl(enclosingClassTree, indyHelpersName);
                if (indyHelpersDecl == null || indyHelpersDecl.sym == null) {
                    throw new IllegalStateException("Unable to find the symbol of indyHelpers in class tree: " + enclosingClassTree.name + ".");
                }
                indyHelpersVarSymbol = indyHelpersDecl.sym;
            } else {
                indyHelpersVar = decideNameIn(enclosingClassTree, "indyHelpers", 0);
                indyHelpersMap.put(enclosingClassName, indyHelpersVar);
                Name indyHelpersName = names.fromString(indyHelpersVar);
                TreeMaker maker = jasyncContext.getTreeMaker();
                int prePos = maker.pos;
                try {
                    Symbol.ClassSymbol indyHelpersSymbol = (Symbol.ClassSymbol) elements.getTypeElement("io.github.vipcxj.jasync.runtime.java8.helpers.IndyHelpers");
                    long flags = Flags.FINAL | Flags.PRIVATE;
                    if (isEnclosingClassStatic()) {
                        flags |= Flags.STATIC;
                    }
                    indyHelpersVarSymbol = new Symbol.VarSymbol(
                            flags,
                            indyHelpersName,
                            indyHelpersSymbol.type,
                            enclosingClassTree.sym
                    );
                    Symbol.MethodSymbol indyHelpersCtrSymbol = (Symbol.MethodSymbol) SymbolHelpers.INSTANCE.getSymbol(indyHelpersSymbol, names.init, null);
                    Symbol.ClassSymbol methodHandlesSymbol = (Symbol.ClassSymbol) elements.getTypeElement("java.lang.invoke.MethodHandles");
                    Symbol lookupSymbol = SymbolHelpers.INSTANCE.getSymbol(
                            methodHandlesSymbol,
                            names.fromString("lookup"),
                            symbol -> symbol instanceof Symbol.MethodSymbol
                                    && Flags.isStatic(symbol)
                                    && ((Symbol.MethodSymbol) symbol).params().isEmpty()
                    );
                    JCTree.JCExpression lookupExpr = safeMaker().Select(safeMaker().QualIdent(methodHandlesSymbol), lookupSymbol);
                    JCTree.JCMethodInvocation argExpr = safeMaker().Apply(List.nil(), lookupExpr, List.nil());
                    argExpr.polyKind = JCTree.JCPolyExpression.PolyKind.STANDALONE;
                    JCTree.JCNewClass newIndyHelpers = safeMaker().NewClass(
                            null, List.nil(),
                            safeMaker().QualIdent(indyHelpersSymbol),
                            List.of(argExpr),
                            null
                    );
                    addFieldDecl(indyHelpersSymbol, indyHelpersCtrSymbol, indyHelpersVarSymbol, newIndyHelpers);
                } finally {
                    maker.pos = prePos;
                }
            }
        }
        return indyHelpersVarSymbol;
    }

    public Symbol.VarSymbol getIndyHelper() {
        Symbol.VarSymbol indyHelpersVarSymbol = getIndyHelpers();
        if (indyHelperVarSymbol == null) {
            IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
            Names names = jasyncContext.getNames();
            Elements elements = jasyncContext.getEnvironment().getElementUtils();
            String indyHelperVarName = indyHelperMap.get(enclosingClassName);
            if (indyHelperVarName != null) {
                Name indyHelperName = names.fromString(indyHelperVarName);
                JCTree.JCVariableDecl indyHelperDecl = findVarDecl(enclosingClassTree, indyHelperName);
                if (indyHelperDecl == null || indyHelperDecl.sym == null) {
                    throw new IllegalStateException("Unable to find the symbol of indyHelper in class tree: " + enclosingClassTree.name + ".");
                }
                indyHelperVarSymbol = indyHelperDecl.sym;
            } else {
                indyHelperVarName = decideNameIn(enclosingClassTree, "indyHelper", 0);
                indyHelperMap.put(enclosingClassName, indyHelperVarName);
                Name indyHelperName = names.fromString(indyHelperVarName);
                TreeMaker maker = jasyncContext.getTreeMaker();
                int prePos = maker.pos;
                try {
                    Symbol.ClassSymbol indyHelperSymbol = (Symbol.ClassSymbol) elements.getTypeElement("io.github.vipcxj.jasync.runtime.java8.helpers.IndyHelper");
                    indyHelperVarSymbol = new Symbol.VarSymbol(
                            Flags.FINAL | Flags.PRIVATE,
                            indyHelperName,
                            indyHelperSymbol.type,
                            enclosingClassTree.sym
                    );
                    Symbol.MethodSymbol indyHelperCtrSymbol = (Symbol.MethodSymbol) SymbolHelpers.INSTANCE.getSymbol(indyHelperSymbol, names.init, null);
                    JCTree.JCIdent indyHelpersVar = safeMaker().Ident(indyHelpersVarSymbol);
                    JCTree.JCExpression thisVar = safeMaker().This(enclosingClassTree.type);
                    JCTree.JCNewClass newIndyHelper = safeMaker().NewClass(
                            null, List.nil(),
                            safeMaker().QualIdent(indyHelperSymbol),
                            List.of(indyHelpersVar, thisVar),
                            null
                    );
                    addFieldDecl(indyHelperSymbol, indyHelperCtrSymbol, indyHelperVarSymbol, newIndyHelper);
                } finally {
                    safeMaker().pos = prePos;
                }
            }
        }
        return indyHelperVarSymbol;
    }

    private List<Symbol.VarSymbol> getParamSymbols(Frame frame) {
        ListBuffer<Symbol.VarSymbol> symbols = new ListBuffer<>();
        for (Frame.CapturedInfo capturedInfo : frame.getCapturedVars().values()) {
            symbols = symbols.append(capturedInfo.getDeclSymbol());
        }
        for (Frame.PlaceHolderInfo placeHolder : frame.getCapturedPlaceHolders().values()) {
            symbols = symbols.append(placeHolder.getSymbol());
        }
        if (isDebug()) {
            for (Frame.CapturedInfo capturedInfo : frame.getDebugCapturedVars().values()) {
                symbols = symbols.append(capturedInfo.getDeclSymbol());
            }
        }
        for (Frame.DeclInfo declInfo : frame.getDeclaredVars().values()) {
            if (declInfo.isAsyncParam()) {
                symbols = symbols.append(declInfo.getRemapDecl().sym);
            }
        }
        for (Frame.PlaceHolderInfo placeHolder : frame.getDeclaredPlaceHolders().values()) {
            if (placeHolder.isParam()) {
                symbols = symbols.append(placeHolder.getSymbol());
            }
        }
        return symbols.toList();
    }

    public JCTree.JCMethodDecl addMethodDecl(Frame frame, Type resType, JCTree.JCBlock body) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        TreeMaker maker = jasyncContext.getTreeMaker();
        Symtab symbols = jasyncContext.getSymbols();
        int flag = Flags.PRIVATE /*| Flags.SYNTHETIC*/;
        if (staticMethod) {
            flag |= Flags.STATIC;
        }
        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                flag,
                nextMethodName(),
                null,
                enclosingClassTree.sym
        );
        frame.bind(methodSymbol);
        methodSymbol.params = getParamSymbols(frame);
        methodSymbol.type = new Type.MethodType(
                JavacUtils.mapList(methodSymbol.params, param -> param.type),
                resType,
                List.of(symbols.throwableType),
                enclosingClassTree.sym
        );
        int prePos = maker.pos;
        boolean isVoid = resType.getTag() == TypeTag.VOID;
        try {
            JCTree.JCMethodDecl methodDecl = safeMaker().MethodDef(methodSymbol, isVoid ? body : JavacUtils.forceBlockReturn(jasyncContext, body));
            enclosingClassTree.defs = enclosingClassTree.defs.append(methodDecl);
            return methodDecl;
        } finally {
            maker.pos = prePos;
        }
    }

    public JCTree.JCMethodDecl addCondSupplier(TransBlockContext condContext) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        JCTree condTree = condContext.buildTree(false);
        if (condContext.innerHasAwait()) {
            Symtab symbols = jasyncContext.getSymbols();
            Types types = jasyncContext.getTypes();
            Type booleanType = types.boxedTypeOrType(symbols.booleanType);
            return addMethodDecl(
                    condContext.getFrame(),
                    JavacUtils.getType(jasyncContext, JPromise.class, booleanType),
                    (JCTree.JCBlock) condTree
            );
        } else {
            return addMethodDecl(
                    condContext.getFrame(),
                    JavacUtils.getType(jasyncContext, boolean.class),
                    (JCTree.JCBlock) condTree
            );
        }
    }

    public JCTree.JCExpression makeCondSupplier(TransBlockContext condContext) {
        JCTree.JCMethodDecl methodDecl = addCondSupplier(condContext);
        if (condContext.innerHasAwait()) {
            return makeFunctional(condContext.getFrame(), Constants.INDY_MAKE_PROMISE_SUPPLIER, methodDecl);
        } else {
            return makeFunctional(condContext.getFrame(), Constants.INDY_MAKE_BOOLEAN_SUPPLIER, methodDecl);
        }
    }

    public JCTree.JCMethodDecl addVoidPromiseFunction(Frame frame, JCTree.JCBlock body) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        return addMethodDecl(frame, JavacUtils.getType(jasyncContext, JPromise.class, JavacUtils.getBoxedVoidType(jasyncContext)), body);
    }

    public JCTree.JCMethodDecl addVoidPromiseFunction(TransBlockContext context) {
        return addVoidPromiseFunction(context.getFrame(), (JCTree.JCBlock) context.buildTree(false));
    }

    public JCTree.JCExpression makeVoidPromiseFunction(TransBlockContext context) {
        JCTree.JCMethodDecl methodDecl = addVoidPromiseFunction(context);
        return makeFunctional(context.getFrame(), Constants.INDY_MAKE_VOID_PROMISE_FUNCTION, methodDecl);
    }

    public JCTree.JCMethodDecl addPromiseFunction(TransBlockContext context, Type type) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        return addMethodDecl(
                context.getFrame(),
                JavacUtils.getType(jasyncContext, JPromise.class, type),
                (JCTree.JCBlock) context.buildTree(false)
        );
    }

    public JCTree.JCExpression makePromiseFunction(TransBlockContext context, Type type) {
        JCTree.JCMethodDecl methodDecl = addPromiseFunction(context, type);
        return makeFunctional(context.getFrame(), Constants.INDY_MAKE_PROMISE_FUNCTION, methodDecl);
    }

    public JCTree.JCMethodDecl addVoidPromiseSupplier(Frame frame, JCTree.JCBlock body) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        return addMethodDecl(frame, JavacUtils.getType(jasyncContext, JPromise.class, JavacUtils.getBoxedVoidType(jasyncContext)), body);
    }

    public JCTree.JCMethodDecl addVoidPromiseSupplier(TransBlockContext bodyContext) {
        return addVoidPromiseSupplier(bodyContext.getFrame(), (JCTree.JCBlock) bodyContext.buildTree(false));
    }

    public JCTree.JCExpression makeVoidPromiseSupplier(TransBlockContext bodyContext) {
        JCTree.JCMethodDecl methodDecl = addVoidPromiseSupplier(bodyContext);
        return makeFunctional(bodyContext.getFrame(), Constants.INDY_MAKE_VOID_PROMISE_SUPPLIER, methodDecl);
    }

    public JCTree.JCMethodDecl addThrowableConsumer(Frame frame, JCTree.JCBlock body) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        Symtab symbols = jasyncContext.getSymbols();
        return addMethodDecl(frame, symbols.voidType, body);
    }

    public JCTree.JCMethodDecl addThrowableConsumer(TransCatchContext catchContext) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        JCTree.JCBlock body = (JCTree.JCBlock) catchContext.getBodyContext().buildTree(false);
        body = new TreeCopier<Void>(jasyncContext.getTreeMaker()).copy(body);
        return addThrowableConsumer(catchContext.getFrame(), body);
    }

    public JCTree.JCExpression makeCatchCallback(TransCatchContext catchContext) {
        if (catchContext.hasAwait()) {
            JCTree.JCMethodDecl methodDecl = addPromiseFunction(catchContext.getBodyContext(), JavacUtils.getBoxedVoidType(getContext().getJasyncContext()));
            return makeFunctional(catchContext.getFrame(), Constants.INDY_MAKE_PROMISE_FUNCTION, methodDecl);
        } else {
            JCTree.JCMethodDecl methodDecl = addThrowableConsumer(catchContext);
            return makeFunctional(catchContext.getFrame(), Constants.INDY_MAKE_THROWABLE_CONSUMER, methodDecl);
        }
    }

    private void addFieldDecl(Symbol.ClassSymbol classSymbol, Symbol.MethodSymbol classCtrSymbol, Symbol.VarSymbol varSymbol, JCTree.JCNewClass newClass) {
        newClass.type = classSymbol.type;
        newClass.constructor = classCtrSymbol;
        newClass.constructorType = classCtrSymbol.type;
        newClass.polyKind = JCTree.JCPolyExpression.PolyKind.STANDALONE;
        JCTree.JCVariableDecl decl = safeMaker().VarDef(
                varSymbol,
                newClass
        );
        enclosingClassTree.defs = enclosingClassTree.defs.append(decl);
    }

    public JCTree.JCExpression makeMethodType(JCTree.JCMethodDecl methodDecl) {
        IJAsyncInstanceContext context = getContext().getJasyncContext();
        TreeMaker maker = context.getTreeMaker();
        Names names = context.getNames();
        Symtab symbols = context.getSymbols();
        Types types = context.getTypes();
        int prePos = maker.pos;
        try {
            List<JCTree.JCExpression> args = JavacUtils.mapList(methodDecl.params, param -> safeMaker().ClassLiteral(types.erasure(param.type)));
            Symbol.TypeSymbol methodTypeSymbol = symbols.methodTypeType.tsym;
            Predicate<Symbol> filter;
            if (args.isEmpty()) {
                filter = symbol -> {
                    if (symbol instanceof Symbol.MethodSymbol) {
                        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
                        if (methodSymbol.params.size() == 1) {
                            Symbol.VarSymbol varSymbol = methodSymbol.params.get(0);
                            return varSymbol.type.tsym == symbols.classType.tsym;
                        }
                    }
                    return false;
                };
            } else if (args.size() == 1) {
                filter = symbol -> {
                    if (symbol instanceof Symbol.MethodSymbol) {
                        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
                        if (methodSymbol.params.size() == 2) {
                            for (Symbol.VarSymbol param : methodSymbol.params) {
                                if (param.type.tsym != symbols.classType.tsym) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                    return false;
                };
            } else {
                filter = symbol -> {
                    if (symbol instanceof Symbol.MethodSymbol) {
                        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
                        if (methodSymbol.params.size() == 3) {
                            Symbol.VarSymbol arg0Symbol = methodSymbol.params.get(0);
                            Symbol.VarSymbol arg1Symbol = methodSymbol.params.get(1);
                            Symbol.VarSymbol arg2Symbol = methodSymbol.params.get(2);
                            return methodSymbol.isVarArgs()
                                    && arg0Symbol.type.tsym == symbols.classType.tsym
                                    && arg1Symbol.type.tsym == symbols.classType.tsym
                                    && types.isArray(arg2Symbol.type)
                                    && types.elemtype(arg2Symbol.type).tsym == symbols.classType.tsym;
                        }
                    }
                    return false;
                };
            }
            Symbol methodSymbol = SymbolHelpers.INSTANCE.getSymbol(methodTypeSymbol, names.fromString("methodType"), filter);
            JCTree.JCMethodInvocation invocation = safeMaker().Apply(
                    List.nil(),
                    safeMaker().Select(safeMaker().QualIdent(methodTypeSymbol), methodSymbol),
                    args.prepend(safeMaker().ClassLiteral(types.erasure(methodDecl.type.getReturnType())))
            );
            invocation.polyKind = JCTree.JCPolyExpression.PolyKind.STANDALONE;
            return invocation;
        } finally {
            maker.pos = prePos;
        }
    }

    public JCTree.JCExpression makeFunctional(Frame frame, String functionType, JCTree.JCMethodDecl implMethod) {
        IJAsyncInstanceContext jasyncContext = analyzerContext.getJasyncContext();
        TreeMaker maker = jasyncContext.getTreeMaker();
        Names names = jasyncContext.getNames();
        Symbol.VarSymbol indyHelper = staticMethod ? getIndyHelpers() : getIndyHelper();
        ListBuffer<JCTree.JCExpression> areExps = new ListBuffer<>();
        int prePos = maker.pos;
        try {
            areExps = areExps.append(safeMaker().Literal(implMethod.name.toString()));
            areExps = areExps.append(makeMethodType(implMethod));
            for (Frame.CapturedInfo capturedInfo : frame.getCapturedVars().values()) {
                areExps = areExps.append(capturedInfo.makeInputExpr());
            }
            for (Frame.PlaceHolderInfo placeHolderInfo : frame.getCapturedPlaceHolders().values()) {
                areExps = areExps.append(placeHolderInfo.makeInputExpr());
            }
            if (isDebug()) {
                for (Frame.CapturedInfo capturedInfo : frame.getDebugCapturedVars().values()) {
                    areExps = areExps.append(capturedInfo.makeInputExpr());
                }
            }
            return safeMaker().Apply(
                    List.nil(),
                    safeMaker().Select(safeMaker().Ident(indyHelper), names.fromString(functionType)),
                    areExps.toList()
            );
        } finally {
            maker.pos = prePos;
        }
    }

    @Override
    protected JCTree buildTreeWithoutThen(boolean replaceSelf) {
        if (bodyContext != null) {
            tree.body = (JCTree.JCBlock) bodyContext.buildTree(false);
        }
        return tree;
    }

    private static void lockContext(TranslateContext<?> context) {
        if (context instanceof TransFrameHolderContext && context.getFrame() != null) {
            context.getFrame().lock();
        }
        for (TranslateContext<?> child : context.getChildren()) {
            lockContext(child);
        }
        if (context.getThen() != null) {
            lockContext(context.getThen());
        }
    }

    public void lock() {
        lockContext(this);
    }

    static class PathData {
        public String name;
        public int index;
    }

    static class EnclosingClassFinder extends TreeScanner {
        private final JCTree.JCMethodDecl methodDecl;
        private JCTree.JCClassDecl targetClassDecl;
        private boolean topLevel;
        private String targetName;
        private final Stack<JCTree.JCClassDecl> classDeclStack;
        private final Stack<PathData> path;
        private int index;

        EnclosingClassFinder(JCTree.JCMethodDecl methodDecl) {
            this.methodDecl = methodDecl;
            this.classDeclStack = new Stack<>();
            this.path = new Stack<>();
            this.index = 0;
        }

        public JCTree.JCClassDecl getEnclosingClassTree() {
            return targetClassDecl;
        }

        public String getTargetName() {
            return targetName;
        }

        public boolean isTopLevel() {
            return topLevel;
        }

        @Override
        public void scan(JCTree tree) {
            if (targetClassDecl == null)
                super.scan(tree);
        }

        private void iterIndex() {
            if (path.isEmpty()) {
                ++index;
            } else {
                ++path.peek().index;
            }
        }

        private int currentIndex() {
            if (path.isEmpty()) {
                return index;
            } else {
                return path.peek().index;
            }
        }

        @Override
        public void visitClassDef(JCTree.JCClassDecl tree) {
            classDeclStack.push(tree);
            try {
                iterIndex();
                PathData pathData = new PathData();
                pathData.name = tree.name == null || tree.name.isEmpty() ? ("class$$" + currentIndex()) : (tree.name.toString());
                pathData.index = 0;
                path.push(pathData);
                try {
                    super.visitClassDef(tree);
                } finally {
                    path.pop();
                }
            } finally {
                classDeclStack.pop();
            }
        }

        @Override
        public void visitMethodDef(JCTree.JCMethodDecl tree) {
            if (methodDecl == tree) {
                targetClassDecl = classDeclStack.peek();
                topLevel = classDeclStack.size() == 1;
                StringBuilder sb = new StringBuilder();
                ListIterator<PathData> iterator = path.listIterator(path.size());
                while (iterator.hasPrevious()) {
                    PathData pd = iterator.previous();
                    if (sb.length() == 0) {
                        sb.append(pd.name);
                    } else {
                        sb.append(".").append(pd.name);
                    }
                }
                targetName = sb.toString();
            } else {
                PathData pathData = new PathData();
                pathData.name = tree.name.toString();
                pathData.index = 0;
                path.push(pathData);
                try {
                    super.visitMethodDef(tree);
                } finally {
                    path.pop();
                }
            }
        }
    }
}
