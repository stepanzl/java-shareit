package ru.practicum.shareit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;

@RestController
class ThrowingController {

    @GetMapping("/test/not-found")
    public void notFound() {
        throw new NotFoundException("User not found");
    }

    @GetMapping("/test/forbidden")
    public void forbidden() {
        throw new ForbiddenException("Forbidden");
    }

    @GetMapping("/test/conflict")
    public void conflict() {
        throw new ConflictException("Email already exists");
    }

    @GetMapping("/test/bad-request")
    public void badRequest() {
        throw new BadRequestException("Bad request");
    }

    @GetMapping("/test/unexpected")
    public void unexpected() {
        throw new RuntimeException("Boom");
    }
}
