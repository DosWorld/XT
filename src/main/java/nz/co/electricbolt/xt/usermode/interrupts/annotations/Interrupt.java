package nz.co.electricbolt.xt.usermode.interrupts.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Interrupt {
    int interrupt() default 0x21;
    int function();
    int subfunction() default -1;
    String description();
}
