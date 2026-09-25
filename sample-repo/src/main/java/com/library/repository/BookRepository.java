package com.library.repository;

import com.library.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    /**
     * Finds all books that are currently available for reservation.
     */
    @Query("SELECT b FROM Book b WHERE b.isAvailable = true ORDER BY b.title ASC")
    List<Book> findAllAvailable();

    /**
     * Finds books by title containing the search term (case-insensitive).
     *
     * @param title partial title search string
     * @return list of matching books
     */
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY b.title ASC")
    List<Book> findByTitleContainingIgnoreCase(@Param("title") String title);
}
