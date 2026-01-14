package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRequestMapper requestMapper;

    @Override
    public ItemRequestDto create(long userId, ItemRequestCreateDto dto) {
        User requestor = getUserOrThrow(userId);

        ItemRequest request = requestMapper.toEntity(dto);
        request.setRequestor(requestor);

        ItemRequest saved = requestRepository.save(request);

        return toDtoWithEmptyItems(saved);
    }

    @Override
    public List<ItemRequestDto> getOwn(long userId) {
        getUserOrThrow(userId);

        return requestRepository.findAllByRequestor_IdOrderByCreatedDesc(userId)
                .stream()
                .map(this::toDtoWithEmptyItems)
                .toList();
    }

    @Override
    public List<ItemRequestDto> getAll(long userId, int from, int size) {
        getUserOrThrow(userId);
        int page = (size == 0) ? 0 : from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());
        return requestRepository.findAllByRequestor_IdNotOrderByCreatedDesc(userId, pageable)
                .stream()
                .map(this::toDtoWithEmptyItems)
                .toList();
    }

    @Override
    public ItemRequestDto getById(long userId, long requestId) {
        getUserOrThrow(userId);
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found: " + requestId));
        return toDtoWithEmptyItems(request);
    }

    private User getUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private ItemRequestDto toDtoWithEmptyItems(ItemRequest request) {
        ItemRequestDto dto = requestMapper.toDto(request);
        dto.setItems(List.of());
        return dto;
    }
}
