package online.yudream.minecraft.mod.common;

public interface LogSink {
    void info(String message);

    void warn(String message);

    void warn(String message, Throwable throwable);
}
