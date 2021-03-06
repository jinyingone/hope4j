package fun.jinying.hope.doc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestParms {
    String path();

    String[] method();

    String[] required();

    boolean isCheckToken() default false;

    boolean isCheckReplay() default false;
}
