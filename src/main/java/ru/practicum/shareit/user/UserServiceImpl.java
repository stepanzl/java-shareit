package ru.practicum.shareit.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();
    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDto create(UserDto userDto) {
        if (userDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User body is required");
        }

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be empty");
        }

        boolean emailExists = users.values().stream()
                .anyMatch(u -> u.getEmail().equals(userDto.getEmail()));
        if (emailExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = userMapper.toModel(userDto);
        long id = idGenerator.incrementAndGet();
        user.setId(id);
        users.put(id, user);

        return userMapper.toDto(user);
    }

    @Override
    public UserDto getById(Long userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        return users.values().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        User existing = users.get(userId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (userDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User body is required");
        }

        if (userDto.getEmail() != null) {
            if (userDto.getEmail().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be empty");
            }

            boolean emailExists = users.values().stream()
                    .anyMatch(u -> !u.getId().equals(userId)
                            && u.getEmail().equals(userDto.getEmail()));
            if (emailExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }

            existing.setEmail(userDto.getEmail());
        }

        if (userDto.getName() != null) {
            if (userDto.getName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be empty");
            }
            existing.setName(userDto.getName());
        }

        return userMapper.toDto(existing);
    }

    @Override
    public void delete(Long userId) {
        User removed = users.remove(userId);
        if (removed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }
}
