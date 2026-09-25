package com.library.service;

import com.library.dto.ReservationRequest;
import com.library.dto.ReservationResponse;
import com.library.exception.BookAlreadyReservedException;
import com.library.exception.BookNotFoundException;
import com.library.exception.ReservationNotFoundException;
import com.library.model.Book;
import com.library.model.Reservation;
import com.library.model.ReservationStatus;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReservationService.
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    /**
     * Number of days a reservation remains valid before expiry.
     */
    private static final int RESERVATION_TTL_DAYS = 14;

    private final BookRepository bookRepository;
    private final ReservationRepository reservationRepository;

    public ReservationServiceImpl(BookRepository bookRepository,
                                   ReservationRepository reservationRepository) {
        this.bookRepository = bookRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional
    public ReservationResponse reserveBook(ReservationRequest request, Long userId) {
        Long bookId = request.getBookId();

        // Verify the book exists
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // Check if there is already an active reservation for this book
        Optional<Reservation> existing = reservationRepository
                .findByBookIdAndStatus(bookId, ReservationStatus.ACTIVE);

        if (existing.isPresent()) {
            throw new BookAlreadyReservedException(bookId);
        }

        // Mark the book as unavailable
        book.setAvailable(false);
        bookRepository.save(book);

        // Create the reservation
        User user = new User();
        user.setId(userId);

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().plusDays(RESERVATION_TTL_DAYS));

        Reservation saved = reservationRepository.save(reservation);

        log.info("Book {} reserved by user {} until {}", bookId, userId, saved.getExpiresAt());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only cancel your own reservations");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        // Mark the book as available again
        Book book = reservation.getBook();
        book.setAvailable(true);
        bookRepository.save(book);

        reservationRepository.save(reservation);

        log.info("Reservation {} cancelled by user {}", reservationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByUser(Long userId) {
        return reservationRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllActiveReservations() {
        return reservationRepository.findAllActive()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ReservationResponse toResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setBookId(reservation.getBook().getId());
        response.setBookTitle(reservation.getBook().getTitle());
        response.setUserId(reservation.getUser().getId());
        response.setUsername(reservation.getUser().getUsername());
        response.setStatus(reservation.getStatus());
        response.setReservedAt(reservation.getReservedAt());
        response.setExpiresAt(reservation.getExpiresAt());
        return response;
    }
}
