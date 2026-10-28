package br.com.servicehub.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Garante que a senha caiba no limite de 72 bytes do BCrypt. O {@code @Size} conta caracteres,
 * mas caracteres acentuados ocupam mais de um byte em UTF-8; sem esta validação, uma senha com
 * 72 caracteres acentuados faria o BCryptPasswordEncoder lançar exceção (HTTP 500).
 */
@Documented
@Constraint(validatedBy = SenhaBcryptValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaBcrypt {

    String message() default "A senha deve ter no máximo 72 bytes (caracteres acentuados ocupam mais de um byte)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
