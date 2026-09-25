package com.library.service;

import com.library.dto.ReservationRequest;
import com.library.dto.ReservationResponse;

import java.util.List;

/**
 * Service interface for managing book reservations.
 */
public interface ReservationService {

    /**
     * Creates a new reservation for the given book on behalf of the authenticated user.
     *
     * @param request   the reservation request containing the book ID
     * @param userId    the ID of the authenticated user making the reservation
     * @return the created reservation details
     * @throws com.library.exception.BookNotFoundException       if no book exists with the given ID
     * @throws com.library.exception.BookAlreadyReservedException if the book is already actively reserved
     */
    ReservationResponse reserveBook(ReservationRequest request, Long userId);

    /**
     * Cancels an existing reservation.
     *
     * @param reservationId the ID of the reservation to cancel
     * @param userId        the ID of the user requesting cancellation (must own the reservation)
     * @throws com.library.exception.ReservationNotFoundException if no reservation exists with the given ID
     * @throws org.springframework.security.access.AccessDeniedException if the user does not own the reservation
     */
    void cancelReservation(Long reservationId, Long userId);

    /**
     * Retrieves all reservations belonging to a specific user.
     *
     * @param userId the ID of the user
     * @return list of the user's reservations, ordered by reservation date descending
     */
    List<ReservationResponse> getReservationsByUser(Long userId);

    /**
     * Retrieves all active reservations in the system (admin/librarian use).
     *
     * @return list of all active reservations ordered by reservation date ascending
     */
    List<ReservationResponse> getAllActiveReservations();
}
