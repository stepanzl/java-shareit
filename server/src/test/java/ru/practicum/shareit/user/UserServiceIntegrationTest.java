package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void create_whenEmailAlreadyExists_thenThrowConflict() {
        UserCreateDto first = new UserCreateDto();
        first.setName("Alice");
        first.setEmail("alice@mail.com");

        UserDto created = userService.create(first);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("alice@mail.com");

        UserCreateDto second = new UserCreateDto();
        second.setName("Bob");
        second.setEmail("alice@mail.com");

        assertThatThrownBy(() -> userService.create(second))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }
}
