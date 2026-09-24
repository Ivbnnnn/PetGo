package ru.mirea.petgo.exception;

public class BusinessException extends Exception {
    public BusinessException(String message) {
        super(message);
    }
}