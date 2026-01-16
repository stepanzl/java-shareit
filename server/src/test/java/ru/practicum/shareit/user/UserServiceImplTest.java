package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_whenEmailUnique_thenSavesAndReturnsDto() {
        UserCreateDto in = new UserCreateDto();
        in.setName("Alice");
        in.setEmail("alice@mail.com");

        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(false);

        User toSave = new User();
        toSave.setName("Alice");
        toSave.setEmail("alice@mail.com");
        when(userMapper.toModel(any(UserCreateDto.class))).thenReturn(toSave);

        User saved = new User();
        saved.setId(1L);
        saved.setName("Alice");
        saved.setEmail("alice@mail.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserDto out = new UserDto();
        out.setId(1L);
        out.setName("Alice");
        out.setEmail("alice@mail.com");
        when(userMapper.toDto(saved)).thenReturn(out);

        UserDto result = userService.create(in);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice@mail.com");

        verify(userRepository).existsByEmail("alice@mail.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Alice");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@mail.com");

        verify(userMapper).toModel(in);
        verify(userMapper).toDto(saved);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void create_whenEmailExists_thenThrowsConflict() {
        UserCreateDto in = new UserCreateDto();
        in.setName("Alice");
        in.setEmail("alice@mail.com");

        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(in))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository).existsByEmail("alice@mail.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void update_whenUserNotFound_thenThrowsNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        UserUpdateDto in = new UserUpdateDto();
        in.setName("NewName");
        in.setEmail("new@mail.com");

        assertThatThrownBy(() -> userService.update(404L, in))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(404L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void update_whenBlankName_thenThrowsBadRequest() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserUpdateDto in = new UserUpdateDto();
        in.setName("   ");
        in.setEmail(null);

        assertThatThrownBy(() -> userService.update(1L, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Name must not be blank");

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void update_whenEmailBlank_thenThrowsBadRequest() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserUpdateDto in = new UserUpdateDto();
        in.setName(null);
        in.setEmail("   ");

        assertThatThrownBy(() -> userService.update(1L, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must not be blank");

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void update_whenEmailChangedAndExistsElsewhere_thenThrowsConflict() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("new@mail.com", 1L)).thenReturn(true);

        UserUpdateDto in = new UserUpdateDto();
        in.setName(null);
        in.setEmail("new@mail.com");

        assertThatThrownBy(() -> userService.update(1L, in))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmailAndIdNot("new@mail.com", 1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void update_whenValid_thenAppliesPatchAndReturnsDto() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot(anyString(), anyLong())).thenReturn(false);

        UserUpdateDto in = new UserUpdateDto();
        in.setName("NewName");
        in.setEmail("new@mail.com");

        doAnswer(invocation -> {
            UserUpdateDto dto = invocation.getArgument(0);
            User model = invocation.getArgument(1);
            if (dto.getName() != null) model.setName(dto.getName());
            if (dto.getEmail() != null) model.setEmail(dto.getEmail());
            return null;
        }).when(userMapper).updateUserFromDto(any(UserUpdateDto.class), any(User.class));

        User saved = new User();
        saved.setId(1L);
        saved.setName("NewName");
        saved.setEmail("new@mail.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserDto out = new UserDto();
        out.setId(1L);
        out.setName("NewName");
        out.setEmail("new@mail.com");
        when(userMapper.toDto(saved)).thenReturn(out);

        UserDto result = userService.update(1L, in);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getEmail()).isEqualTo("new@mail.com");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmailAndIdNot("new@mail.com", 1L);
        verify(userMapper).updateUserFromDto(in, existing);

        ArgumentCaptor<User> saveCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getName()).isEqualTo("NewName");
        assertThat(saveCaptor.getValue().getEmail()).isEqualTo("new@mail.com");

        verify(userMapper).toDto(saved);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void getById_whenExists_thenReturnsDto() {
        User user = new User();
        user.setId(1L);
        user.setName("Alice");
        user.setEmail("alice@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto out = new UserDto();
        out.setId(1L);
        out.setName("Alice");
        out.setEmail("alice@mail.com");
        when(userMapper.toDto(user)).thenReturn(out);

        UserDto result = userService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice@mail.com");

        verify(userRepository).findById(1L);
        verify(userMapper).toDto(user);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void getById_whenNotFound_thenThrowsNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(404L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void getAll_whenHasUsers_thenMapsAndReturnsList() {
        User u1 = new User();
        u1.setId(1L);
        u1.setName("A");
        u1.setEmail("a@mail.com");

        User u2 = new User();
        u2.setId(2L);
        u2.setName("B");
        u2.setEmail("b@mail.com");

        when(userRepository.findAll()).thenReturn(java.util.List.of(u1, u2));

        UserDto d1 = new UserDto();
        d1.setId(1L);
        d1.setName("A");
        d1.setEmail("a@mail.com");

        UserDto d2 = new UserDto();
        d2.setId(2L);
        d2.setName("B");
        d2.setEmail("b@mail.com");

        when(userMapper.toDto(u1)).thenReturn(d1);
        when(userMapper.toDto(u2)).thenReturn(d2);

        var result = userService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        verify(userRepository).findAll();
        verify(userMapper).toDto(u1);
        verify(userMapper).toDto(u2);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void delete_whenUserExists_thenDeletesById() {
        User existing = new User();
        existing.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(userRepository).deleteById(1L);

        userService.delete(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).deleteById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void delete_whenUserNotFound_thenThrowsNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(404L);
        verify(userRepository, never()).deleteById(anyLong());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void update_whenEmailNull_thenDoesNotCheckUniqueness() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserUpdateDto in = new UserUpdateDto();
        in.setName("NewName");
        in.setEmail(null);

        doAnswer(invocation -> {
            UserUpdateDto dto = invocation.getArgument(0);
            User model = invocation.getArgument(1);
            if (dto.getName() != null) model.setName(dto.getName());
            if (dto.getEmail() != null) model.setEmail(dto.getEmail());
            return null;
        }).when(userMapper).updateUserFromDto(any(UserUpdateDto.class), any(User.class));

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto out = new UserDto();
        out.setId(1L);
        out.setName("NewName");
        out.setEmail("old@mail.com");
        when(userMapper.toDto(any(User.class))).thenReturn(out);

        UserDto result = userService.update(1L, in);

        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getEmail()).isEqualTo("old@mail.com");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(userMapper).updateUserFromDto(in, existing);
        verify(userRepository).save(existing);
        verify(userMapper).toDto(existing);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void update_whenEmailSameAsCurrent_thenDoesNotCheckUniqueness() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("same@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserUpdateDto in = new UserUpdateDto();
        in.setName(null);
        in.setEmail("same@mail.com"); // такое же, как текущее

        doNothing().when(userMapper).updateUserFromDto(any(UserUpdateDto.class), any(User.class));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto out = new UserDto();
        out.setId(1L);
        out.setName("Old");
        out.setEmail("same@mail.com");
        when(userMapper.toDto(any(User.class))).thenReturn(out);

        UserDto result = userService.update(1L, in);

        assertThat(result.getEmail()).isEqualTo("same@mail.com");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(userMapper).updateUserFromDto(in, existing);
        verify(userRepository).save(existing);
        verify(userMapper).toDto(existing);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

}
