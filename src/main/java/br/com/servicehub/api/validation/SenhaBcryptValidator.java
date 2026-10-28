package br.com.servicehub.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class SenhaBcryptValidator implements ConstraintValidator<SenhaBcrypt, String> {

    private static final int LIMITE_BYTES_BCRYPT = 72;

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext context) {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= LIMITE_BYTES_BCRYPT;
    }
}
