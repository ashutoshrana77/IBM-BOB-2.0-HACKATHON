package com.library.service;

import com.library.dto.ReservationRequest;
import com.library.dto.ReservationResponse;
import com.library.exception.BookAlreadyReservedException;
import com.library.exception.BookNotFoundException;
import com.library.exception.ReservationNotFoundException;
import com.library.model.*;
import com.library.repository.BookRepository;
import com.library.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Book testBook;
    private User testUser;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Clean Code");
        testBook.setAuthor("Robert C. Martin");
        testBook.setIsbn("978-0132350884");
        testBook.setAvailable(true);

        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("john.doe");
        testUser.setEmail("john@example.com");

        testReservation = new Reservation();
        testReservation.setId(100L);
        testReservation.setBook(testBook);
        testReservation.setUser(testUser);
        testReservation.setStatus(ReservationStatus.ACTIVE);
        testReservation.setExpiresAt(LocalDateTime.now().plusDays(14));
    }

    // =========================================================================
    // reserveBook tests
    // =========================================================================

    @Test
    void reserveBook_whenBookAvailable_returnsReservationResponse() {
        // given
        ReservationRequest request = new ReservationRequest();
        request.setBookId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reservationRepository.findByBookIdAndStatus(1L, ReservationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // when
        ReservationResponse response = reservationService.reserveBook(request, 10L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBookId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        verify(bookRepository).save(argThat(book -> !book.isAvailable()));
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserveBook_whenBookNotFound_throwsBookNotFoundException() {
        // given
        ReservationRequest request = new ReservationRequest();
        request.setBookId(999L);

        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> reservationService.reserveBook(request, 10L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void reserveBook_whenBookAlreadyReserved_throwsBookAlreadyReservedException() {
        // given
        ReservationRequest request = new ReservationRequest();
        request.setBookId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reservationRepository.findByBookIdAndStatus(1L, ReservationStatus.ACTIVE))
                .thenReturn(Optional.of(testReservation));

        // when / then
        assertThatThrownBy(() -> reservationService.reserveBook(request, 10L))
                .isInstanceOf(BookAlreadyReservedException.class)
                .hasMessageContaining("already reserved");
    }

    // =========================================================================
    // cancelReservation tests
    // =========================================================================

    @Test
    void cancelReservation_whenOwnerCancels_setsStatusCancelledAndMakesBookAvailable() {
        // given
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        // when
        reservationService.cancelReservation(100L, 10L);

        // then
        assertThat(testReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(bookRepository).save(argThat(Book::isAvailable));
        verify(reservationRepository).save(testReservation);
    }

    @Test
    void cancelReservation_whenReservationNotFound_throwsReservationNotFoundException() {
        // given
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> reservationService.cancelReservation(999L, 10L))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void cancelReservation_whenDifferentUserCancels_throwsAccessDeniedException() {
        // given
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));

        // when / then — user 99 tries to cancel reservation owned by user 10
        assertThatThrownBy(() -> reservationService.cancelReservation(100L, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // getReservationsByUser tests
    // =========================================================================

    @Test
    void getReservationsByUser_returnsUserReservations() {
        // given
        when(reservationRepository.findByUserId(10L)).thenReturn(List.of(testReservation));

        // when
        List<ReservationResponse> results = reservationService.getReservationsByUser(10L);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBookId()).isEqualTo(1L);
    }

    @Test
    void getReservationsByUser_whenNoReservations_returnsEmptyList() {
        // given
        when(reservationRepository.findByUserId(10L)).thenReturn(List.of());

        // when
        List<ReservationResponse> results = reservationService.getReservationsByUser(10L);

        // then
        assertThat(results).isEmpty();
    }
}
