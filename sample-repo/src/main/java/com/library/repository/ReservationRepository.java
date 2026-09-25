package com.library.repository;

import com.library.model.Reservation;
import com.library.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Finds an active reservation for a specific book.
     *
     * @param bookId the ID of the book
     * @return an Optional containing the active reservation, or empty if none
     */
    @Query("SELECT r FROM Reservation r WHERE r.book.id = :bookId AND r.status = :status")
    Optional<Reservation> findByBookIdAndStatus(
            @Param("bookId") Long bookId,
            @Param("status") ReservationStatus status);

    /**
     * Finds all reservations belonging to a specific user.
     *
     * @param userId the ID of the user
     * @return list of reservations for the user
     */
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId ORDER BY r.reservedAt DESC")
    List<Reservation> findByUserId(@Param("userId") Long userId);

    /**
     * Finds all active reservations (for admin/librarian use).
     */
    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' ORDER BY r.reservedAt ASC")
    List<Reservation> findAllActive();
}
