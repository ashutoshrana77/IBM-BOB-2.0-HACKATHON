package com.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class BookAlreadyReservedException extends RuntimeException {

    public BookAlreadyReservedException(Long bookId) {
        super("Book with id " + bookId + " is already reserved");
    }
}
