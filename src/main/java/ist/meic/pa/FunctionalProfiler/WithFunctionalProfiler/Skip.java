package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Skip {}
