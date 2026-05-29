package com.juriconstat.exception;

/**
 * Exception levée lorsque le quota mensuel de consultations d'un utilisateur est dépassé.
 */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
