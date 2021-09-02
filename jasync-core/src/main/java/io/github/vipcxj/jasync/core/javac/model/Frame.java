package io.github.vipcxj.jasync.core.javac.model;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import io.github.vipcxj.jasync.core.javac.IJAsyncInstanceContext;
import io.github.vipcxj.jasync.core.javac.JavacUtils;
import io.github.vipcxj.jasync.core.javac.context.JAsyncSymbols;
import io.github.vipcxj.jasync.core.javac.translate.TransFrameHolderContext;
import io.github.vipcxj.jasync.core.javac.translate.TransPlaceHolderContext;
import io.github.vipcxj.jasync.core.javac.translate.TransWriteExprContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransIdentContext;
import io.github.vipcxj.jasync.core.javac.translate.context.TransVarDeclContext;

import javax.lang.model.element.ElementKind;
import java.util.*;

public class Frame {
    private final Frame root;
    private final Frame parent;
    private final Map<VarKey, DeclInfo> declaredVars;
    private final Map<VarKey, CapturedInfo> capturedVars;
    private final Map<VarKey, CapturedInfo> debugCapturedVars;
    private final Map<JCTree, PlaceHolderInfo> declaredPlaceHolders;
    private final Map<JCTree, PlaceHolderInfo> capturedPlaceHolders;
    private final TransFrameHolderContext<?> holder;
    private long orderBase;
    private long order;

    public Frame(Frame parent, TransFrameHolderContext<?> holder) {
        this.parent = parent;
        this.root = parent != null ? parent.root : this;
        this.holder = holder;
        this.declaredVars = new LinkedHashMap<>();
        this.capturedVars = new LinkedHashMap<>();
        this.debugCapturedVars = new LinkedHashMap<>();
        this.declaredPlaceHolders = new LinkedHashMap<>();
        this.capturedPlaceHolders = new LinkedHashMap<>();
        this.orderBase = 0;
        this.order = 0;
    }

    public Frame getParent() {
        return parent;
    }

    public void bind(Symbol.MethodSymbol symbol) {
        for (DeclInfo declInfo : declaredVars.values()) {
            declInfo.bind(symbol);
        }
        for (CapturedInfo capturedInfo : capturedVars.values()) {
            capturedInfo.bind(symbol);
        }
        for (CapturedInfo capturedInfo : debugCapturedVars.values()) {
            capturedInfo.bind(symbol);
        }

        for (PlaceHolderInfo info : declaredPlaceHolders.values()) {
            info.bind(symbol);
        }
        for (PlaceHolderInfo info : capturedPlaceHolders.values()) {
            info.bind(symbol);
        }
    }

    public void addPlaceHolder(JCTree expression, Name name, boolean param) {
        declaredPlaceHolders.put(expression, new PlaceHolderInfo(expression, name, param));
    }

    public void markOrder() {
        this.order = this.root.orderBase++;
    }

    public void addLocal(TransVarDeclContext declContext) {
        DeclInfo declInfo = new DeclInfo(declContext);
        VarKey varKey = new VarKey(declInfo.symbol);
        declaredVars.put(varKey, declInfo);
        declContext.setDeclInfo(declInfo);
    }

    public boolean isLocal(Symbol symbol) {
        return declaredVars.containsKey(new VarKey(symbol));
    }

    public Map<VarKey, DeclInfo> getDeclaredVars() {
        return declaredVars;
    }

    public Map<VarKey, CapturedInfo> getCapturedVars() {
        return capturedVars;
    }

    public Map<VarKey, CapturedInfo> getDebugCapturedVars() {
        return debugCapturedVars;
    }

    public Map<JCTree, PlaceHolderInfo> getDeclaredPlaceHolders() {
        return declaredPlaceHolders;
    }

    public Map<JCTree, PlaceHolderInfo> getCapturedPlaceHolders() {
        return capturedPlaceHolders;
    }

    private static boolean isVarSymbol(Symbol symbol) {
        ElementKind kind = symbol.getKind();
        return kind == ElementKind.LOCAL_VARIABLE
                || kind == ElementKind.PARAMETER
                || kind == ElementKind.EXCEPTION_PARAMETER;
    }

    private void readPlaceHolder(TransPlaceHolderContext<?> context, boolean direct) {
        PlaceHolderInfo placeHolder = context.getDeclPlaceHolder();
        PlaceHolderInfo declInfo = declaredPlaceHolders.get(placeHolder.tree);
        if (declInfo == null) {
            if (holder.hasAwait()) {
                PlaceHolderInfo capturedInfo = capturedPlaceHolders.get(placeHolder.tree);
                if (capturedInfo == null) {
                    capturedInfo = new PlaceHolderInfo(placeHolder.tree, placeHolder.symbol.name, true);
                    capturedPlaceHolders.put(placeHolder.tree, capturedInfo);
                }
                if (direct) {
                    direct = false;
                    context.setCapturedPlaceHolder(capturedInfo);
                }
            }
            if (parent != null) {
                parent.readPlaceHolder(context, direct);
            }
        }
    }

    public void readPlaceHolder(TransPlaceHolderContext<?> context) {
        readPlaceHolder(context, true);
    }

    private DeclInfo getDeclInfo(VarKey key) {
        DeclInfo declInfo = declaredVars.get(key);
        if (declInfo != null) {
            return declInfo;
        }
        if (parent != null) {
            return parent.getDeclInfo(key);
        }
        return null;
    }

    private void readVar(TransIdentContext context, DeclInfo declInfo, boolean direct, boolean fromAwait) {
        Symbol symbol = context.getTree().sym;
        if (isVarSymbol(symbol)) {
            VarKey varKey = new VarKey(symbol);
            DeclInfo localDeclInfo = declaredVars.get(varKey);
            if (localDeclInfo != null) {
                if (direct) {
                    localDeclInfo.getReadAts().add(context);
                } else if (fromAwait) {
                    localDeclInfo.setCaptured(true);
                }
                context.setDeclInfo(localDeclInfo);
            } else {
                if (holder.hasAwait()) {
                    CapturedInfo capturedInfo = capturedVars.get(varKey);
                    if (capturedInfo == null) {
                        capturedInfo = new CapturedInfo(declInfo);
                        capturedVars.put(varKey, capturedInfo);
                    }
                    if (direct) {
                        direct = false;
                        capturedInfo.readAts.add(context);
                        context.setCapturedInfo(capturedInfo);
                    }
                }
                if (parent != null) {
                    parent.readVar(context, declInfo, direct, holder.hasAwait());
                }
            }
        }
    }

    public void readVar(TransIdentContext context) {
        VarKey key = new VarKey(context.getTree().sym);
        DeclInfo declInfo = getDeclInfo(key);
        readVar(context, declInfo, true, holder.hasAwait());
    }

    private void writeVar(TransWriteExprContext<?> context, DeclInfo declInfo, boolean direct, boolean fromAwait) {
        Symbol symbol = context.getSymbol();
        if (isVarSymbol(symbol)) {
            VarKey varKey = new VarKey(symbol);
            DeclInfo localDeclInfo = declaredVars.get(varKey);
            if (localDeclInfo != null) {
                if (direct) {
                    localDeclInfo.getWriteAts().add(context);
                } else if (fromAwait) {
                    localDeclInfo.setCaptured(true);
                    localDeclInfo.setReadOnly(false);
                }
                context.setDeclInfo(localDeclInfo);
            } else {
                if (holder.hasAwait()) {
                    CapturedInfo capturedInfo = capturedVars.get(varKey);
                    if (capturedInfo == null) {
                        capturedInfo = new CapturedInfo(declInfo);
                        capturedVars.put(varKey, capturedInfo);
                    }
                    if (direct) {
                        direct = false;
                        capturedInfo.writeAts.add(context);
                        context.setCapturedInfo(capturedInfo);
                    }
                }
                if (parent != null) {
                    parent.writeVar(context, declInfo, direct, holder.hasAwait());
                }
            }
        }
    }

    public void writeVar(TransWriteExprContext<?> context) {
        VarKey key = new VarKey(context.getSymbol());
        DeclInfo declInfo = getDeclInfo(key);
        writeVar(context, declInfo, true, holder.hasAwait());
    }

    private List<DeclInfo> collectParentDecls() {
        Set<VarKey> keys = new HashSet<>(capturedVars.keySet());
        Frame frame = getParent();
        List<DeclInfo> declInfos = new ArrayList<>();
        while (frame != null) {
            for (Map.Entry<VarKey, DeclInfo> entry : frame.getDeclaredVars().entrySet()) {
                VarKey key = entry.getKey();
                if (!keys.contains(key)) {
                    keys.add(key);
                    DeclInfo declInfo = entry.getValue();
                    if (declInfo.order < order) {
                        declInfos.add(declInfo);
                    }
                }
            }
            frame = frame.getParent();
        }
        return declInfos;
    }

    public void lock() {
        for (DeclInfo declInfo : declaredVars.values()) {
            declInfo.lock();
        }
        for (CapturedInfo capturedInfo : capturedVars.values()) {
            capturedInfo.lock();
        }
        for (DeclInfo declInfo : collectParentDecls()) {
            CapturedInfo capturedInfo = new CapturedInfo(declInfo);
            capturedInfo.lock();
            debugCapturedVars.put(capturedInfo.createKey(), capturedInfo);
        }
    }

    public class DeclInfo {
        private final TransVarDeclContext context;
        private final Symbol.VarSymbol symbol;
        private Symbol.VarSymbol usedSymbol;
        private JCTree.JCVariableDecl remapDecl;
        private JCTree.JCVariableDecl referenceDecl;
        private final List<TransIdentContext> readAts;
        private final List<TransWriteExprContext<?>> writeAts;
        private boolean readOnly;
        private boolean captured;
        private boolean lock;
        private long order;

        public DeclInfo(TransVarDeclContext context) {
            JCTree.JCVariableDecl decl = context.getTree();
            this.context = context;
            this.symbol = decl.sym;
            this.readAts = new ArrayList<>();
            this.writeAts = new ArrayList<>();
            this.readOnly = true;
            this.captured = false;
            this.lock = false;
            this.order = root.orderBase++;
        }

        private void lock() {
            IJAsyncInstanceContext jasyncContext = holder.getContext().getJasyncContext();
            TreeMaker maker = jasyncContext.getTreeMaker();
            if (context.isAsyncParam()) {
                Symbol.VarSymbol remapSymbol = new Symbol.VarSymbol(Flags.PARAMETER, symbol.name, symbol.type, null);
                remapSymbol.pos = context.getTree().pos;
                int prePos = maker.pos;
                try {
                    maker.pos = remapSymbol.pos;
                    remapDecl = maker.VarDef(remapSymbol, null);
                } finally {
                    maker.pos = prePos;
                }
            }
            if (!readOnly) {
                assert captured;
                referenceDecl = JavacUtils.makeReferenceDeclTree(
                        jasyncContext,
                        remapDecl != null ? remapDecl : context.getTree(),
                        jasyncContext.nextVar()
                );
            }
            usedSymbol = remapDecl != null ? remapDecl.sym : symbol;
            lock = true;
        }

        public void bind(Symbol.MethodSymbol owner) {
            if (!lock) {
                throw new IllegalStateException("lock first.");
            }
            if (remapDecl != null) {
                remapDecl.sym.owner = owner;
            }
            if (referenceDecl != null) {
                referenceDecl.sym.owner = owner;
            }
            usedSymbol.owner = owner;
        }

        public boolean isRefed() {
            if (!lock) {
                throw new IllegalStateException("lock first.");
            }
            return !readOnly;
        }

        public JCTree.JCVariableDecl getRemapDecl() {
            return remapDecl;
        }

        public JCTree.JCVariableDecl getReferenceDecl() {
            return referenceDecl;
        }

        public TransVarDeclContext getContext() {
            return context;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public List<TransIdentContext> getReadAts() {
            return readAts;
        }

        public List<TransWriteExprContext<?>> getWriteAts() {
            return writeAts;
        }

        public boolean isAsyncParam() {
            return context.isAsyncParam();
        }

        // if readonly == false，captured must be true. because only captured var is account to be not readonly.
        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public void setCaptured(boolean captured) {
            this.captured = captured;
        }

        public Symbol.VarSymbol getDeclSymbol() {
            return referenceDecl != null ? referenceDecl.sym : (remapDecl != null ? remapDecl.sym : symbol);
        }

        public Symbol.VarSymbol getUsedSymbol() {
            return usedSymbol;
        }
    }

    private IJAsyncInstanceContext getContext() {
        return Frame.this.holder.getContext().getJasyncContext();
    }

    private JAsyncSymbols symbols() {
        return getContext().getJAsyncSymbols();
    }

    private TreeMaker safeMaker() {
        return getContext().safeMaker();
    }

    public class CapturedInfo {
        private final DeclInfo declInfo;
        private Symbol.VarSymbol declSymbol;
        private Symbol.VarSymbol usedSymbol;
        private boolean lock;
        private final List<TransIdentContext> readAts;
        private final List<TransWriteExprContext<?>> writeAts;

        public CapturedInfo(DeclInfo declInfo) {
            this.declInfo = declInfo;
            this.lock = false;
            this.readAts = new ArrayList<>();
            this.writeAts = new ArrayList<>();
        }

        private void lock() {
            Symbol origSymbol = declInfo.symbol;
            if (!isNotReadOnly()) {
                declSymbol = new Symbol.VarSymbol(Flags.FINAL | Flags.PARAMETER, origSymbol.name, origSymbol.type, null);
                usedSymbol = declSymbol;
            } else {
                IJAsyncInstanceContext jasyncContext = holder.getContext().getJasyncContext();
                declSymbol = new Symbol.VarSymbol(
                        Flags.FINAL | Flags.PARAMETER,
                        jasyncContext.nextVarName(),
                        (Type) JavacUtils.getReferenceType(jasyncContext, origSymbol.type),
                        null
                );
                usedSymbol = new Symbol.VarSymbol(Flags.PARAMETER, origSymbol.name, origSymbol.type, null);
            }
            lock = true;
        }

        public void bind(Symbol.MethodSymbol owner) {
            declSymbol.owner = owner;
            usedSymbol.owner = owner;
            TreeMaker maker = holder.getContext().getJasyncContext().getTreeMaker();
            declSymbol.pos = maker.pos;
            usedSymbol.pos = maker.pos;
        }

        private VarKey createKey() {
            return new VarKey(declInfo.symbol);
        }

        public JCTree.JCExpression makeInputExpr() {
            Frame frame = parent;
            if (frame != null) {
                do {
                    VarKey key = createKey();
                    DeclInfo declInfo = frame.declaredVars.get(key);
                    if (declInfo != null) {
                        Symbol.VarSymbol declSymbol = declInfo.getDeclSymbol();
                        return safeMaker().Ident(declSymbol);
                    }
                    CapturedInfo capturedInfo = frame.capturedVars.get(key);
                    if (capturedInfo == null) {
                        capturedInfo = frame.debugCapturedVars.get(key);
                    }
                    if (capturedInfo != null) {
                        Symbol.VarSymbol declSymbol = capturedInfo.getDeclSymbol();
                        if (declSymbol == null) {
                            throw new IllegalStateException();
                        }
                        return safeMaker().Ident(declSymbol);
                    }
                    frame = frame.parent;
                } while (frame != null);
            }
            throw new IllegalStateException();
        }

        public JCTree.JCVariableDecl makeUsedDecl() {
            TreeMaker maker = getContext().getTreeMaker();
            int pos = maker.pos;
            try {
                return safeMaker().VarDef(
                        getUsedSymbol(),
                        symbols().makeRefGet(getDeclSymbol())
                );
            } finally {
                maker.pos = pos;
            }
        }

        public boolean isNotReadOnly() {
            return !declInfo.isReadOnly();
        }

        public Symbol.VarSymbol getDeclSymbol() {
            return declSymbol;
        }

        public Symbol.VarSymbol getUsedSymbol() {
            return usedSymbol;
        }
    }

    public class PlaceHolderInfo {
        private JCTree tree;
        private boolean param;
        private Symbol.VarSymbol symbol;

        public PlaceHolderInfo(JCTree tree, Name name, boolean param) {
            this.tree = tree;
            this.param = param;
            long flag = Flags.FINAL;
            if (param) {
                flag |= Flags.PARAMETER;
            }
            this.symbol = new Symbol.VarSymbol(flag, name, tree.type, null);
        }

        public boolean isParam() {
            return param;
        }

        public Symbol.VarSymbol getSymbol() {
            return symbol;
        }

        public void bind(Symbol.MethodSymbol owner) {
            TreeMaker maker = holder.getContext().getJasyncContext().getTreeMaker();
            symbol.owner = owner;
            symbol.pos = maker.pos;
        }

        public JCTree.JCExpression makeInputExpr() {
            Frame frame = parent;
            if (frame != null) {
                do {
                    PlaceHolderInfo declInfo = frame.getDeclaredPlaceHolders().get(tree);
                    if (declInfo != null) {
                        return safeMaker().Ident(declInfo.getSymbol());
                    }
                    PlaceHolderInfo capturedInfo = frame.getCapturedPlaceHolders().get(tree);
                    if (capturedInfo != null) {
                        return safeMaker().Ident(capturedInfo.getSymbol());
                    }
                    frame = frame.parent;
                } while (frame != null);
            }
            throw new IllegalStateException();
        }
    }
}
