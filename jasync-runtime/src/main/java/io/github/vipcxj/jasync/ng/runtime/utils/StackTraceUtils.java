package io.github.vipcxj.jasync.ng.runtime.utils;

public class StackTraceUtils {

    @SuppressWarnings("unused")
    public static StackTraceElement createStackFrame(String classLoaderName,
                                                     String moduleName, String moduleVersion,
                                                     String declaringClass, String methodName,
                                                     String fileName, int lineNumber) {
        return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
    }

    public static StackTraceElement updateStackFrame(StackTraceElement base, String classLoaderName,
                                                     String moduleName, String moduleVersion,
                                                     String declaringClass, String methodName,
                                                     String fileName, int lineNumber) {
        declaringClass = declaringClass != null ? declaringClass : base.getClassName();
        methodName = methodName != null ? methodName : base.getMethodName();
        fileName = fileName != null ? fileName : base.getFileName();
        lineNumber = lineNumber != Integer.MIN_VALUE ? lineNumber : base.getLineNumber();
        return createStackFrame(classLoaderName, moduleName, moduleVersion, declaringClass, methodName, fileName, lineNumber);
    }
}
