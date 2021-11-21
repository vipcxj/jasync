package io.github.vipcxj.jasync.asm;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class JAsyncInfo {
    public static final JAsyncInfo DEFAULT = new JAsyncInfo();
    private final String debugId;
    private final boolean disabled;
    private final boolean logByteCode;
    private final boolean logAsm;
    private final boolean verify;

    public JAsyncInfo() {
        this.debugId = "";
        this.disabled = false;
        this.logByteCode = false;
        this.logAsm = false;
        this.verify = false;
    }

    public JAsyncInfo(String debugId, boolean disabled, boolean logByteCode, boolean logAsm, boolean verify) {
        this.debugId = debugId;
        this.disabled = disabled;
        this.logByteCode = logByteCode;
        this.logAsm = logAsm;
        this.verify = verify;
    }

    public String getDebugId() {
        return debugId;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isLogByteCode() {
        return logByteCode;
    }

    public boolean isLogAsm() {
        return logAsm;
    }

    public boolean isVerify() {
        return verify;
    }

    private static JAsyncInfo of(List<AnnotationNode> annotationNodes) {
        if (annotationNodes == null) {
            return null;
        }
        for (AnnotationNode node : annotationNodes) {
            if (Constants.ANN_ASYNC_DESC.equals(node.desc)) {
                int size = node.values != null ? node.values.size() : 0;
                String debugId = null;
                boolean disabled = false;
                boolean logByteCode = false;
                boolean logAsm = false;
                boolean verify = false;
                for (int i = 0; i < size; i += 2) {
                    String name = (String) node.values.get(i);
                    Object value = node.values.get(i + 1);
                    if ("debugId".equals(name)) {
                        debugId = (String) value;
                    } else if ("disabled".equals(name)) {
                        disabled = (Boolean) value;
                    } else if ("logByteCode".equals(name)) {
                        logByteCode = (Boolean) value;
                    } else if ("verify".equals(name)) {
                        verify = (Boolean) value;
                    } else if ("logAsm".equals(name)) {
                        logAsm = (Boolean) value;
                    }
                }
                return new JAsyncInfo(debugId, disabled, logByteCode, logAsm, verify);
            }
        }
        return null;
    }

    public static JAsyncInfo of(MethodNode methodNode) {
        JAsyncInfo info = of(methodNode.visibleAnnotations);
        if (info == null) {
            info = of(methodNode.invisibleAnnotations);
        }
        return info != null ? info : DEFAULT;
    }
}
