package com.library.dto;

import com.library.model.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response payload representing a reservation.
 */
@Schema(description = "Reservation details")
public class ReservationResponse {

    @Schema(description = "Unique reservation ID", example = "42")
    private Long id;

    @Schema(description = "ID of the reserved book", example = "1")
    private Long bookId;

    @Schema(description = "Title of the reserved book", example = "Clean Code")
    private String bookTitle;

    @Schema(description = "ID of the user who made the reservation", example = "7")
    private Long userId;

    @Schema(description = "Username of the person who made the reservation", example = "john.doe")
    private String username;

    @Schema(description = "Current status of the reservation", example = "ACTIVE")
    private ReservationStatus status;

    @Schema(description = "Date and time the reservation was created")
    private LocalDateTime reservedAt;

    @Schema(description = "Date and time the reservation expires")
    private LocalDateTime expiresAt;

    // ---- Getters and setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
