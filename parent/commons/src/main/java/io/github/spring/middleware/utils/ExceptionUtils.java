package io.github.spring.middleware.utils;

public class ExceptionUtils {

    public static String getExceptionMessage(Throwable ex) {

        return getAllCausesWithMessage(ex, new StringBuffer());
    }

    public static String getExceptionMessage(Throwable ex, int lines) {

        return getStackTraceLines(ex, lines) + getAllCausesWithMessage(ex, new StringBuffer());
    }

    public static String getStackTraceLines(Throwable ex, int lines) {

        StackTraceElement[] stackTrace = ex.getStackTrace();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < lines; i++) {
            if (stackTrace.length > i) {
                buffer.append(stackTrace[i].toString()).append("\n");
            }
        }
        return buffer.toString();
    }

    public static String getAllCausesWithMessage(Throwable ex, StringBuffer mesasges) {

        if (ex == null) {
            return mesasges.toString();
        } else {
            mesasges.append(ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\n");
            return getAllCausesWithMessage(ex.getCause(), mesasges);
        }
    }

    public static Throwable getExceptionFromRuntimeException(Throwable ex) {

        if (ex.getClass().getSimpleName().equals("RuntimeException") && ex.getCause() != null) {
            return ex.getCause();
        } else {
            return ex;
        }
    }

}
