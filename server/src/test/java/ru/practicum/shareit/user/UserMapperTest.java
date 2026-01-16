package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toDto_mapsAllFields() {
        User user = new User();
        user.setId(10L);
        user.setName("Alice");
        user.setEmail("alice@mail.com");

        UserDto dto = mapper.toDto(user);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Alice");
        assertThat(dto.getEmail()).isEqualTo("alice@mail.com");
    }

    @Test
    void toModel_ignoresId() {
        UserCreateDto dto = new UserCreateDto();
        dto.setName("Bob");
        dto.setEmail("bob@mail.com");

        User user = mapper.toModel(dto);

        assertThat(user.getId()).isNull(); // @Mapping(target="id", ignore = true)
        assertThat(user.getName()).isEqualTo("Bob");
        assertThat(user.getEmail()).isEqualTo("bob@mail.com");
    }

    @Test
    void updateUserFromDto_ignoresNulls() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@mail.com");

        UserUpdateDto patch = new UserUpdateDto();
        patch.setName("NewName");
        patch.setEmail(null); // must be ignored

        mapper.updateUserFromDto(patch, existing);

        assertThat(existing.getName()).isEqualTo("NewName");
        assertThat(existing.getEmail()).isEqualTo("old@mail.com");
    }
}
