package io.github.vipcxj.jasync.core.javac;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.util.*;

import static io.github.vipcxj.jasync.core.javac.ElementUtils.toElement;

public class AnnotationUtils {

    public enum AnnotationValueKind {
        BOXED, STRING, TYPE, ENUM, ANNOTATION, ARRAY
    }

    @NonNull
    private static AnnotationValueKind getAnnotationValueType(@NonNull AnnotationValue value) {
        Object v = value.getValue();
        if (v instanceof Boolean
                || v instanceof Integer
                || v instanceof Long
                || v instanceof Float
                || v instanceof Short
                || v instanceof Character
                || v instanceof Double
                || v instanceof Byte
        ) {
            return AnnotationValueKind.BOXED;
        } else if (v instanceof String) {
            return AnnotationValueKind.STRING;
        } else if (v instanceof TypeMirror) {
            return AnnotationValueKind.TYPE;
        } else if (v instanceof VariableElement) {
            return AnnotationValueKind.ENUM;
        } else if (v instanceof AnnotationMirror) {
            return AnnotationValueKind.ANNOTATION;
        } else if (v instanceof List) {
            return AnnotationValueKind.ARRAY;
        }
        throw new IllegalArgumentException("This is impossible.");
    }

    private static void throwCastAnnotationValueTypeError(
            @NonNull AnnotationMirror annotation,
            @NonNull String attributeName,
            @NonNull AnnotationValueKind fromKind,
            @NonNull AnnotationValueKind toKind
    ) {
        throw new IllegalArgumentException(
                "Unable to cast attribute named \""
                        + attributeName
                        + "\" in annotation "
                        + getAnnotationName(annotation)
                        + " from "
                        + fromKind
                        + " to "
                        + toKind
                        + "."
        );
    }

    @NonNull
    public static AnnotationValue getAnnotationValue(AnnotationMirror annotation, @NonNull Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues, @NonNull String name) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationValues.entrySet()) {
            if (name.equals(entry.getKey().getSimpleName().toString())) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("There is no attribute named \"" + name + "\" in annotation " + getAnnotationName(annotation));
    }

    @NonNull
    public static String getStringAnnotationValue(@NonNull AnnotationMirror annotation, @NonNull Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues, @NonNull String name) {
        AnnotationValue annotationValue = getAnnotationValue(annotation, annotationValues, name);
        AnnotationValueKind kind = getAnnotationValueType(annotationValue);
        if (kind == AnnotationValueKind.STRING) {
            return (String) annotationValue.getValue();
        }
        throwCastAnnotationValueTypeError(annotation, name, kind, AnnotationValueKind.STRING);
        throw new IllegalArgumentException("This is impossible.");
    }

    @NonNull
    private static String toEnumString(@NonNull AnnotationValue annotationValue) {
        VariableElement variableElement = (VariableElement) annotationValue.getValue();
        TypeElement enumClass = (TypeElement) variableElement.getEnclosingElement();
        return enumClass.getQualifiedName() + "." + variableElement.getSimpleName();
    }

    @CheckForNull
    private static <T extends Enum<T>> T getEnum(@NonNull String qName, @NonNull Class<T> type) {
        String typeName = type.getCanonicalName();
        for (T constant : type.getEnumConstants()) {
            if (qName.equals(typeName + "." + constant.name())) {
                return constant;
            }
        }
        return null;
    }

    @NonNull
    private static String getEnumAnnotationValue(@NonNull AnnotationMirror annotation, @NonNull Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues, @NonNull String name) {
        AnnotationValue annotationValue = getAnnotationValue(annotation, annotationValues, name);
        AnnotationValueKind kind = getAnnotationValueType(annotationValue);
        if (kind == AnnotationValueKind.ENUM) {
            return toEnumString(annotationValue);
        }
        throwCastAnnotationValueTypeError(annotation, name, kind, AnnotationValueKind.ENUM);
        throw new IllegalArgumentException("This is impossible.");
    }

    @NonNull
    public static <T extends Enum<T>> T getEnumAnnotationValue(@NonNull AnnotationMirror annotation, @NonNull Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues, @NonNull String name, @NonNull Class<T> enumType) {
        String qName = getEnumAnnotationValue(annotation, annotationValues, name);
        T anEnum = getEnum(qName, enumType);
        if (anEnum == null) {
            throw new IllegalArgumentException("Invalid enum value: " + qName + ".");
        }
        return anEnum;
    }

    public static boolean getBooleanAnnotationValue(@NonNull AnnotationMirror annotation, @NonNull Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues, @NonNull String name) {
        AnnotationValue annotationValue = getAnnotationValue(annotation, annotationValues, name);
        AnnotationValueKind kind = getAnnotationValueType(annotationValue);
        if (kind == AnnotationValueKind.BOXED) {
            return (Boolean) annotationValue.getValue();
        }
        throwCastAnnotationValueTypeError(annotation, name, kind, AnnotationValueKind.BOXED);
        throw new IllegalArgumentException("This is impossible.");
    }

    public static long getLongAnnotationValue(@NonNull AnnotationMirror annotation, @NonNull Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues, @NonNull String name) {
        AnnotationValue annotationValue = getAnnotationValue(annotation, annotationValues, name);
        AnnotationValueKind kind = getAnnotationValueType(annotationValue);
        if (kind == AnnotationValueKind.BOXED) {
            return (Long) annotationValue.getValue();
        }
        throwCastAnnotationValueTypeError(annotation, name, kind, AnnotationValueKind.BOXED);
        throw new IllegalArgumentException("This is impossible.");
    }

    @NonNull
    public static String getAnnotationName(@NonNull AnnotationMirror mirror) {
        return toElement(mirror.getAnnotationType()).getQualifiedName().toString();
    }

    public static boolean isThisAnnotation(@NonNull AnnotationMirror annotation, @NonNull String typeName) {
        TypeElement element = (TypeElement) annotation.getAnnotationType().asElement();
        return element.getQualifiedName().toString().equals(typeName);
    }

    public static boolean isAnnotationRepeatable(@NonNull Elements elements, AnnotationMirror annotation) {
        return !getAnnotationsOn(elements, annotation.getAnnotationType().asElement(), Repeatable.class, null, false, false).isEmpty();
    }

    public static boolean isAnnotationInheritable(@NonNull Elements elements, AnnotationMirror annotation) {
        List<AnnotationMirror> inheritedList = getAnnotationsOn(elements, annotation.getAnnotationType().asElement(), Inherited.class, null, false, false);
        if (!inheritedList.isEmpty()) {
            return true;
        }
        TypeElement componentElement = getRepeatableAnnotationComponentElement(elements, annotation);
        if (componentElement != null) {
            return !getAnnotationsOn(elements, componentElement, Inherited.class, null, false, false).isEmpty();
        } else {
            return false;
        }
    }

    private static TypeElement getRepeatableAnnotationContainerElement(@NonNull Elements elements, @NonNull TypeElement element) {
        if (element.getKind() == ElementKind.ANNOTATION_TYPE) {
            List<AnnotationMirror> repeatableList = getAnnotationsOn(elements, element, Repeatable.class, null, false, false);
            if (!repeatableList.isEmpty()) {
                AnnotationMirror repeatable = repeatableList.get(0);
                Map<? extends ExecutableElement, ? extends AnnotationValue> annValues = repeatable.getElementValues();
                AnnotationValue annValue = annValues.values().iterator().next();
                DeclaredType repeatableAnnotationType = (DeclaredType) annValue.getValue();
                return toElement(repeatableAnnotationType);
            }
        }
        return null;
    }

    public static TypeElement getRepeatableAnnotationComponentElement(@NonNull Elements elements, AnnotationMirror annotation) {
        TypeElement typeElement = toElement(annotation.getAnnotationType());
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = elements.getElementValuesWithDefaults(annotation);
        if (elementValues.size() == 1) {
            ExecutableElement key = elementValues.keySet().iterator().next();
            if (key.getSimpleName().toString().equals("value")) {
                TypeMirror returnType = key.getReturnType();
                if (returnType.getKind() == TypeKind.ARRAY) {
                    ArrayType arrayType = (ArrayType) returnType;
                    TypeMirror componentType = arrayType.getComponentType();
                    if (componentType.getKind() == TypeKind.DECLARED) {
                        TypeElement componentElement = toElement((DeclaredType) componentType);
                        TypeElement containerElement = getRepeatableAnnotationContainerElement(elements, componentElement);
                        if (containerElement != null && containerElement.getQualifiedName().toString().equals(typeElement.getQualifiedName().toString())) {
                            return componentElement;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static List<AnnotationMirror> getAllAnnotationMirrors(@NonNull Elements elements, @NonNull Element element) {
        return getAllAnnotationMirrors(elements, element, true);
    }

    private static List<AnnotationMirror> getAllAnnotationMirrors(@NonNull Elements elements, @NonNull Element element, boolean direct) {
        List<AnnotationMirror> results = new LinkedList<>();
        if (direct) {
            results.addAll(element.getAnnotationMirrors());
        } else {
            for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                if (isAnnotationInheritable(elements, annotationMirror)) {
                    results.add(annotationMirror);
                }
            }
        }
        if (element.getKind() == ElementKind.CLASS) {
            TypeElement typeElement = (TypeElement) element;
            TypeMirror superclass = typeElement.getSuperclass();
            if (superclass.getKind() != TypeKind.NONE) {
                DeclaredType declaredType = (DeclaredType) superclass;
                results.addAll(0, getAllAnnotationMirrors(elements, declaredType.asElement(), false));
            }
        }
        return results;
    }

    public static List<AnnotationMirror> getAnnotationsOn(@NonNull Elements elements, @NonNull Element element, @NonNull Class<?> type, @CheckForNull Class<?> repeatContainerType) {
        return getAnnotationsOn(elements, element, type, repeatContainerType, new HashSet<>(), true, true);
    }

    public static List<AnnotationMirror> getAnnotationsOn(@NonNull Elements elements, @NonNull Element element, @NonNull Class<?> type, @CheckForNull Class<?> repeatContainerType, boolean indirect) {
        return getAnnotationsOn(elements, element, type, repeatContainerType, new HashSet<>(), indirect, true);
    }

    public static List<AnnotationMirror> getAnnotationsOn(@NonNull Elements elements, @NonNull Element element, @NonNull Class<?> type, @CheckForNull Class<?> repeatContainerType, boolean indirect, boolean metaExtends) {
        return getAnnotationsOn(elements, element, type, repeatContainerType, new HashSet<>(), indirect, metaExtends);
    }

    public static List<AnnotationMirror> getAnnotationsOn(@NonNull Elements elements, @NonNull Element element, @NonNull String typeName, @CheckForNull String repeatContainerTypeName, boolean indirect, boolean metaExtends) {
        return getAnnotationsOn(elements, element, typeName, repeatContainerTypeName, new HashSet<>(), indirect, metaExtends);
    }

    private static List<AnnotationMirror> getAnnotationsOn(@NonNull Elements elements, @NonNull Element element, @NonNull Class<?> type, @CheckForNull Class<?> repeatContainerType, @NonNull Set<Element> visited, boolean indirect, boolean metaExtends) {
        return getAnnotationsOn(elements, element, type.getName(), repeatContainerType != null ? repeatContainerType.getName() : null, visited, indirect, metaExtends);
    }

    private static List<AnnotationMirror> getAnnotationsOn(@NonNull Elements elements, @NonNull Element element, @NonNull String typeName, @CheckForNull String repeatContainerTypeName, @NonNull Set<Element> visited, boolean indirect, boolean metaExtends) {
        if (visited.contains(element)) {
            return Collections.emptyList();
        }
        visited.add(element);
        List<AnnotationMirror> result = new ArrayList<>();
        List<? extends AnnotationMirror> allAnnotations = indirect ? getAllAnnotationMirrors(elements, element) : element.getAnnotationMirrors();
        for (AnnotationMirror annotation : allAnnotations) {
            if (isThisAnnotation(annotation, typeName)) {
                result.add(annotation);
            } else if (repeatContainerTypeName != null && isThisAnnotation(annotation, repeatContainerTypeName)){
                Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = elements.getElementValuesWithDefaults(annotation);
                List<AnnotationMirror> annotations = getAnnotationElement(annotation, attributes);
                result.addAll(annotations);
            } else if (metaExtends) {
                result.addAll(getAnnotationsOn(elements, annotation.getAnnotationType().asElement(), typeName, repeatContainerTypeName, visited, indirect, true));
            }
        }
        return result;
    }

    private static List<AnnotationMirror> getAnnotationElement(AnnotationMirror annotation, @NonNull Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationValues.entrySet()) {
            if ("value".equals(entry.getKey().getSimpleName().toString())) {
                //noinspection unchecked
                List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) entry.getValue().getValue();
                List<AnnotationMirror> annotations = new ArrayList<>();
                for (AnnotationValue value : values) {
                    annotations.add((AnnotationMirror) value.getValue());
                }
                return annotations;
            }
        }
        throw new IllegalArgumentException("There is no attribute named value in annotation " + getAnnotationName(annotation));
    }

    @CheckForNull
    public static AnnotationMirror getAnnotationDirectOn(@NonNull Element element, Class<? extends Annotation> annotationType) {
        if (annotationType == null) return null;
        return getAnnotationDirectOn(element, annotationType.getCanonicalName());
    }

    @CheckForNull
    public static AnnotationMirror getAnnotationDirectOn(@NonNull Element element, String qName) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (toElement(annotationMirror.getAnnotationType()).getQualifiedName().toString().equals(qName)) {
                return annotationMirror;
            }
        }
        return null;
    }
}
