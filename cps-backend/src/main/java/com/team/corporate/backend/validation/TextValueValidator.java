package com.team.corporate.backend.validation;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TextValueValidator implements ConstraintValidator<TextValue, String> {
    private boolean required;

    @Override
    public void initialize(TextValue annotation) {
        required = annotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        try {
            if (required) {
                EntityValidation.requireText(value, "Поле");
            } else {
                EntityValidation.optionalText(value, "Поле");
            }
            return true;
        } catch (IllegalArgumentException exception) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(exception.getMessage()).addConstraintViolation();
            return false;
        }
    }
}
