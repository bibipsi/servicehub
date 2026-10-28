package br.com.servicehub.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejeita textos com os caracteres {@code <} e {@code >}, impedindo que marcações HTML ou
 * scripts sejam armazenados e depois exibidos por um front-end (XSS persistente).
 * Valores nulos são aceitos; combine com {@code @NotBlank} quando o campo for obrigatório.
 */
@Documented
@Pattern(regexp = "^[^<>]*$")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SemHtml {

    String message() default "O campo não pode conter os caracteres < e >";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
