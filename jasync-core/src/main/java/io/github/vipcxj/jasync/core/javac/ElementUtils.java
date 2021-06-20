package io.github.vipcxj.jasync.core.javac;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public class ElementUtils {

    public static TypeElement toElement(DeclaredType type) {
        return (TypeElement) type.asElement();
    }
}
