package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating a new book reservation.
 */
@Schema(description = "Request to reserve a book")
public class ReservationRequest {

    @NotNull(message = "Book ID must not be null")
    @Min(value = 1, message = "Book ID must be a positive number")
    @Schema(description = "ID of the book to reserve", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookId;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
}
