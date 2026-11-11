package com.team.corporate.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TextValueValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TextValue {
    @NotNull
    String message() default "Строка должна содержать не более 255 символов.";

    boolean required() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
