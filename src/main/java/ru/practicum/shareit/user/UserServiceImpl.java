package ru.practicum.shareit.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserDto create(UserDto userDto) {
        log.info("Create user: email={}", userDto.getEmail());

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        User user = userMapper.toModel(userDto);
        user.setId(null);
        User saved = userRepository.save(user);

        return userMapper.toDto(saved);
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        log.info("Update user id={}", userId);

        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (userDto.getEmail() != null && !userDto.getEmail().equals(existing.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(userDto.getEmail(), userId)) {
                throw new ConflictException("Email already exists");
            }
            existing.setEmail(userDto.getEmail());
        }

        if (userDto.getName() != null) {
            existing.setName(userDto.getName());
        }

        User saved = userRepository.save(existing);
        return userMapper.toDto(saved);
    }

    @Override
    public UserDto getById(Long userId) {
        log.info("Get user by id={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        log.info("Get all users");

        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long userId) {
        log.info("Delete user id={}", userId);

        boolean deleted = userRepository.deleteById(userId);
        if (!deleted) {
            throw new NotFoundException("User not found");
        }
    }
}
