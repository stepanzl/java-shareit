package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto create(UserCreateDto userDto) {
        log.info("Create user: email={}", userDto.getEmail());

        validateEmailCreate(userDto.getEmail());

        User user = userMapper.toModel(userDto);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public UserDto update(Long userId, UserUpdateDto userDto) {
        log.info("Update user id={}", userId);

        User existing = getUserOrThrow(userId);

        validateNameUpdate(userDto.getName());
        validateEmailUpdate(userDto.getEmail(), userId, existing.getEmail());

        userMapper.updateUserFromDto(userDto, existing);

        User saved = userRepository.save(existing);
        return userMapper.toDto(saved);
    }

    @Override
    public UserDto getById(Long userId) {
        log.info("Get user id={}", userId);
        User user = getUserOrThrow(userId);
        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        log.info("Get all users");
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public void delete(Long userId) {
        log.info("Delete user id={}", userId);
        getUserOrThrow(userId);
        userRepository.deleteById(userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void validateEmailCreate(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
    }

    private void validateNameUpdate(String name) {
        if (name != null && name.isBlank()) {
            throw new BadRequestException("Name must not be blank");
        }
    }

    private void validateEmailUpdate(String newEmail, Long userId, String currentEmail) {
        if (newEmail == null) {
            return;
        }
        if (newEmail.isBlank()) {
            throw new BadRequestException("Email must not be blank");
        }
        if (!newEmail.equals(currentEmail)
                && userRepository.existsByEmailAndIdNot(newEmail, userId)) {
            throw new ConflictException("Email already exists");
        }
    }
}
