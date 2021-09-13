package io.github.vipcxj.jasync.spec;

public class UnwrapUnsupportedException extends JAsyncException {

    private static final long serialVersionUID = 2618582481073106611L;
    private final Class<?> targetType;
    private final Class<?>[] supportedTypes;

    public UnwrapUnsupportedException(Class<?> targetType, Class<?>... supportedTypes) {
        this.targetType = targetType;
        this.supportedTypes = supportedTypes;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Unable to be unwrapped to the " + targetType + ". The supported types are ");
        if (supportedTypes == null || supportedTypes.length == 0) {
            return sb.append("none.").toString();
        } else {
            int i = 0;
            for (Class<?> supportedType : supportedTypes) {
                sb.append(supportedType);
                if (++i == supportedTypes.length) {
                    sb.append(".");
                } else {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }
}
