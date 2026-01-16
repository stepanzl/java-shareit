package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemResponseDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRequestMapper requestMapper;
    private final ItemRepository itemRepository;

    @Override
    public ItemRequestDto create(long userId, ItemRequestCreateDto dto) {
        User requestor = getUserOrThrow(userId);

        ItemRequest request = requestMapper.toEntity(dto);
        request.setRequestor(requestor);

        ItemRequest savedRequest = requestRepository.save(request);

        ItemRequestDto requestDto = requestMapper.toDto(savedRequest);
        requestDto.setItems(List.of());
        return requestDto;
    }

    @Override
    public List<ItemRequestDto> getOwn(long userId) {
        checkUserExists(userId);

        List<ItemRequestDto> dtos = requestRepository.findAllByRequestor_IdOrderByCreatedDesc(userId)
                .stream()
                .map(this::toDtoWithoutItems)
                .toList();

        attachItems(dtos);
        return dtos;
    }

    @Override
    public List<ItemRequestDto> getAll(long userId, int from, int size) {
        checkUserExists(userId);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());

        List<ItemRequestDto> dtos = requestRepository.findAllByRequestor_IdNotOrderByCreatedDesc(userId, pageable)
                .stream()
                .map(this::toDtoWithoutItems)
                .toList();

        attachItems(dtos);
        return dtos;
    }

    @Override
    public ItemRequestDto getById(long userId, long requestId) {
        checkUserExists(userId);

        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found: " + requestId));

        ItemRequestDto dto = toDtoWithoutItems(request);
        attachItems(List.of(dto));
        return dto;
    }

    private User getUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void checkUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }
    }

    private ItemRequestDto toDtoWithoutItems(ItemRequest request) {
        return requestMapper.toDto(request);
    }

    private void attachItems(List<ItemRequestDto> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<Long> requestIds = requests.stream()
                .map(ItemRequestDto::getId)
                .filter(Objects::nonNull)
                .toList();

        if (requestIds.isEmpty()) {
            requests.forEach(r -> r.setItems(List.of()));
            return;
        }

        List<Item> items = itemRepository.findAllByRequest_IdIn(requestIds);

        Map<Long, List<ItemResponseDto>> itemsByRequestId = items.stream()
                .filter(i -> i.getRequest() != null && i.getRequest().getId() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getRequest().getId(),
                        Collectors.mapping(
                                i -> new ItemResponseDto(
                                        i.getId(),
                                        i.getName(),
                                        i.getOwner() == null ? null : i.getOwner().getId()
                                ),
                                Collectors.toList()
                        )
                ));

        requests.forEach(r -> r.setItems(itemsByRequestId.getOrDefault(r.getId(), List.of())));
    }


}
