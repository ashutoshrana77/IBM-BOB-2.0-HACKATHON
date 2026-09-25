package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.ReservationRequest;
import com.library.dto.ReservationResponse;
import com.library.exception.BookAlreadyReservedException;
import com.library.exception.BookNotFoundException;
import com.library.exception.GlobalExceptionHandler;
import com.library.model.ReservationStatus;
import com.library.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    private ReservationResponse buildResponse() {
        ReservationResponse r = new ReservationResponse();
        r.setId(1L);
        r.setBookId(1L);
        r.setBookTitle("Clean Code");
        r.setUserId(10L);
        r.setUsername("10");
        r.setStatus(ReservationStatus.ACTIVE);
        r.setReservedAt(LocalDateTime.now());
        r.setExpiresAt(LocalDateTime.now().plusDays(14));
        return r;
    }

    @Test
    @WithMockUser(username = "10", roles = {"MEMBER"})
    void createReservation_withValidRequest_returns201() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setBookId(1L);

        when(reservationService.reserveBook(any(), eq(10L))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/v1/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "10", roles = {"MEMBER"})
    void createReservation_withNullBookId_returns400() throws Exception {
        ReservationRequest req = new ReservationRequest();
        // bookId intentionally null — should fail validation

        mockMvc.perform(post("/api/v1/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.bookId").exists());
    }

    @Test
    @WithMockUser(username = "10", roles = {"MEMBER"})
    void createReservation_whenBookNotFound_returns404() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setBookId(999L);

        when(reservationService.reserveBook(any(), eq(10L)))
                .thenThrow(new BookNotFoundException(999L));

        mockMvc.perform(post("/api/v1/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found with id: 999"));
    }

    @Test
    @WithMockUser(username = "10", roles = {"MEMBER"})
    void createReservation_whenBookAlreadyReserved_returns409() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setBookId(1L);

        when(reservationService.reserveBook(any(), eq(10L)))
                .thenThrow(new BookAlreadyReservedException(1L));

        mockMvc.perform(post("/api/v1/reservations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void createReservation_whenUnauthenticated_returns401() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setBookId(1L);

        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "10", roles = {"MEMBER"})
    void cancelReservation_withValidId_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/reservations/1")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "10", roles = {"MEMBER"})
    void getMyReservations_returnsListOfReservations() throws Exception {
        when(reservationService.getReservationsByUser(10L)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/v1/reservations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookTitle").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "10", roles = {"MEMBER"})
    void getAllActiveReservations_whenMember_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "10", roles = {"LIBRARIAN"})
    void getAllActiveReservations_whenLibrarian_returns200() throws Exception {
        when(reservationService.getAllActiveReservations()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/v1/reservations/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
}
