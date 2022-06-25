package io.github.vipcxj.jasync.ng.asm;


import io.github.vipcxj.jasync.ng.utils.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class JAsyncTransformer {
    public static byte[] transform(byte[] input) {
        try {
            ClassReader reader = new ClassReader(input);
            ClassNestChecker nestChecker = new ClassNestChecker(null);
            reader.accept(nestChecker, ClassReader.SKIP_CODE);
            if (nestChecker.isSkip()) {
                return input;
            }
            nestChecker.updateTypeInfoMap();
            ClassChecker checker = new ClassChecker(nestChecker, null);
            reader.accept(checker, ClassReader.SKIP_CODE);
            if (checker.hasAsyncMethod()) {
                ClassWriter writer = new JAsyncClassWriter(ClassWriter.COMPUTE_FRAMES);
                StaticInitMerger staticInitMerger = new StaticInitMerger(writer);
                ClassAnalyzer classAnalyzer = new ClassAnalyzer(checker, staticInitMerger);
                reader.accept(classAnalyzer, 0);
                return writer.toByteArray();
            } else {
                return input;
            }
        } catch (Throwable t) {
            Logger.error(t);
            return input;
        }
    }
}
