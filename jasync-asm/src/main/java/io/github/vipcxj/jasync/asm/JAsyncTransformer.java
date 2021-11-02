package io.github.vipcxj.jasync.asm;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class JAsyncTransformer {
    public static byte[] transform(byte[] input) {
        ClassReader reader = new ClassReader(input);
        ClassChecker checker = new ClassChecker(null);
        reader.accept(checker, 0);
        if (checker.hasAsyncMethod()) {
            ClassWriter writer = new ClassWriter(0);
            ClassAnalyzer classAnalyzer = new ClassAnalyzer(checker, writer);
            reader.accept(classAnalyzer, 0);
            return writer.toByteArray();
        } else {
            return input;
        }
    }
}
