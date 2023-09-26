package io.github.vipcxj.jasync.ng.runtime.utils;

public class StackTraceUtils {

    public static StackTraceElement createStackFrame(String classLoaderName,
                                                     String moduleName, String moduleVersion,
                                                     String declaringClass, String methodName,
                                                     String fileName, int lineNumber) {
        return new StackTraceElement(classLoaderName, moduleName, moduleVersion, declaringClass, methodName, fileName, lineNumber);
    }

    public static StackTraceElement updateStackFrame(StackTraceElement base, String classLoaderName,
                                                     String moduleName, String moduleVersion,
                                                     String declaringClass, String methodName,
                                                     String fileName, int lineNumber) {
        classLoaderName = classLoaderName != null ? classLoaderName : base.getClassLoaderName();
        moduleName = moduleName != null ? moduleName : base.getModuleName();
        moduleVersion = moduleVersion != null ? moduleVersion : base.getModuleVersion();
        declaringClass = declaringClass != null ? declaringClass : base.getClassName();
        methodName = methodName != null ? methodName : base.getMethodName();
        fileName = fileName != null ? fileName : base.getFileName();
        lineNumber = lineNumber != Integer.MIN_VALUE ? lineNumber : base.getLineNumber();
        return createStackFrame(classLoaderName, moduleName, moduleVersion, declaringClass, methodName, fileName, lineNumber);
    }
}
