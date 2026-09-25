# Changelog

All notable changes to this project will be documented in this file.
Format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

- **Added**: `POST /api/v1/reservations` — create a book reservation for the authenticated user
- **Added**: `DELETE /api/v1/reservations/{id}` — cancel a reservation owned by the authenticated user
- **Added**: `GET /api/v1/reservations/my` — list all reservations for the authenticated user
- **Added**: `GET /api/v1/reservations/active` — list all active reservations (librarian/admin only)

## [0.1.0] - 2024-01-01

### Added
- Initial library management system with book CRUD operations
- User management and authentication
