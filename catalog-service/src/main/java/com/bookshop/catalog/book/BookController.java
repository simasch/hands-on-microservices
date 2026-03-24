package com.bookshop.catalog.book;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookRepository repository;

    public BookController(BookRepository repository) {
        this.repository = repository;
    }

    // ============================================================
    // Section 2 - Exercise: Catalog REST API
    // ============================================================

    // TODO 1: Return all books
    //   Create a GET method that returns List<Book>
    //   Hint: Use repository.findAll()

    // TODO 2: Return a single book by ISBN
    //   Create a GET method mapped to "/{isbn}" that returns a Book
    //   Hint: Use repository.findByIsbn(isbn)
    //   Throw a BookNotFoundException if the book is not found
}
