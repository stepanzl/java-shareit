package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRequestMapper requestMapper;
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl requestService;

    @Test
    void create_whenUserExists_thenSavesAndReturnsDtoWithEmptyItems() {
        long userId = 1L;

        User user = new User();
        user.setId(userId);

        ItemRequestCreateDto in = new ItemRequestCreateDto();
        in.setDescription("Need a drill");

        ItemRequest entity = new ItemRequest();
        entity.setDescription("Need a drill");

        ItemRequest saved = new ItemRequest();
        saved.setId(10L);
        saved.setDescription("Need a drill");
        saved.setCreated(LocalDateTime.now());

        ItemRequestDto mapped = new ItemRequestDto();
        mapped.setId(10L);
        mapped.setDescription("Need a drill");
        mapped.setCreated(saved.getCreated());
        mapped.setItems(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(requestMapper.toEntity(any(ItemRequestCreateDto.class))).thenReturn(entity);
        when(requestRepository.save(any(ItemRequest.class))).thenReturn(saved);
        when(requestMapper.toDto(any(ItemRequest.class))).thenReturn(mapped);

        ItemRequestDto out = requestService.create(userId, in);

        assertThat(out.getId()).isEqualTo(10L);
        assertThat(out.getDescription()).isEqualTo("Need a drill");
        assertThat(out.getItems()).isNotNull();
        assertThat(out.getItems()).isEmpty();

        ArgumentCaptor<ItemRequest> captor = ArgumentCaptor.forClass(ItemRequest.class);
        verify(requestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestor()).isSameAs(user);
    }

    @Test
    void create_whenUserNotFound_thenThrowsNotFound() {
        long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ItemRequestCreateDto in = new ItemRequestCreateDto();
        in.setDescription("Need");

        assertThatThrownBy(() -> requestService.create(userId, in))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
        verify(requestRepository, never()).save(any());
        verifyNoInteractions(requestMapper, itemRepository);
    }

    @Test
    void getOwn_whenHasRequests_thenAttachesItemsToEachRequest() {
        long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        ItemRequest r1 = new ItemRequest();
        r1.setId(101L);
        r1.setDescription("r1");
        r1.setCreated(LocalDateTime.now().minusDays(1));

        ItemRequest r2 = new ItemRequest();
        r2.setId(102L);
        r2.setDescription("r2");
        r2.setCreated(LocalDateTime.now());

        ItemRequestDto d1 = new ItemRequestDto();
        d1.setId(101L);
        d1.setDescription("r1");
        d1.setCreated(r1.getCreated());

        ItemRequestDto d2 = new ItemRequestDto();
        d2.setId(102L);
        d2.setDescription("r2");
        d2.setCreated(r2.getCreated());

        when(requestRepository.findAllByRequestor_IdOrderByCreatedDesc(userId)).thenReturn(List.of(r2, r1));
        when(requestMapper.toDto(r2)).thenReturn(d2);
        when(requestMapper.toDto(r1)).thenReturn(d1);

        User owner1 = new User();
        owner1.setId(501L);
        Item item1 = new Item();
        item1.setId(201L);
        item1.setName("Item-201");
        item1.setOwner(owner1);
        item1.setRequest(r1);

        User owner2 = new User();
        owner2.setId(502L);
        Item item2 = new Item();
        item2.setId(202L);
        item2.setName("Item-202");
        item2.setOwner(owner2);
        item2.setRequest(r1);

        User owner3 = new User();
        owner3.setId(503L);
        Item item3 = new Item();
        item3.setId(203L);
        item3.setName("Item-203");
        item3.setOwner(owner3);
        item3.setRequest(r2);

        when(itemRepository.findAllByRequest_IdIn(argThat(ids -> ids.containsAll(List.of(101L, 102L)) && ids.size() == 2)))
                .thenReturn(List.of(item1, item2, item3));

        List<ItemRequestDto> out = requestService.getOwn(userId);

        assertThat(out).hasSize(2);
        ItemRequestDto outFirst = out.get(0);
        ItemRequestDto outSecond = out.get(1);

        assertThat(outFirst.getId()).isEqualTo(102L);
        assertThat(outFirst.getItems()).hasSize(1);
        assertThat(outFirst.getItems().get(0))
                .usingRecursiveComparison()
                .isEqualTo(new ItemResponseDto(203L, "Item-203", 503L));

        assertThat(outSecond.getId()).isEqualTo(101L);
        assertThat(outSecond.getItems()).hasSize(2);
        assertThat(outSecond.getItems())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        new ItemResponseDto(201L, "Item-201", 501L),
                        new ItemResponseDto(202L, "Item-202", 502L)
                );

        verify(itemRepository).findAllByRequest_IdIn(anyList());
    }

    @Test
    void getOwn_whenNoRequests_thenReturnsEmptyAndDoesNotQueryItems() {
        long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(requestRepository.findAllByRequestor_IdOrderByCreatedDesc(userId)).thenReturn(List.of());

        List<ItemRequestDto> out = requestService.getOwn(userId);

        assertThat(out).isEmpty();

        verify(itemRepository, never()).findAllByRequest_IdIn(anyList());
    }

    @Test
    void getOwn_whenUserNotFound_thenThrowsNotFound() {
        long userId = 404L;

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getOwn(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).existsById(userId);
        verifyNoInteractions(requestRepository, requestMapper, itemRepository);
    }

    @Test
    void getAll_whenOtherUsersRequests_thenUsesPageableAndAttachesItems() {
        long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        int from = 20;
        int size = 10;
        int expectedPage = 2;

        ItemRequest r = new ItemRequest();
        r.setId(301L);
        r.setDescription("r");
        r.setCreated(LocalDateTime.now());

        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(301L);
        dto.setDescription("r");
        dto.setCreated(r.getCreated());

        when(requestMapper.toDto(r)).thenReturn(dto);

        when(requestRepository.findAllByRequestor_IdNotOrderByCreatedDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(r)));

        Item item = new Item();
        item.setId(401L);
        item.setName("Answer");
        User owner = new User();
        owner.setId(901L);
        item.setOwner(owner);
        item.setRequest(r);

        when(itemRepository.findAllByRequest_IdIn(eq(List.of(301L)))).thenReturn(List.of(item));

        List<ItemRequestDto> out = requestService.getAll(userId, from, size);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getItems())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new ItemResponseDto(401L, "Answer", 901L));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(requestRepository).findAllByRequestor_IdNotOrderByCreatedDesc(eq(userId), pageableCaptor.capture());

        Pageable actual = pageableCaptor.getValue();
        assertThat(actual.getPageNumber()).isEqualTo(expectedPage);
        assertThat(actual.getPageSize()).isEqualTo(size);
        assertThat(actual.getSort().getOrderFor("created")).isNotNull();
        assertThat(actual.getSort().getOrderFor("created").isDescending()).isTrue();
    }

    @Test
    void getAll_whenDtosHaveNullIds_thenItemsEmptyAndNoItemQuery() {
        long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        ItemRequest r = new ItemRequest();
        r.setId(123L);

        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(null);
        dto.setDescription("desc");
        dto.setCreated(LocalDateTime.now());
        dto.setItems(null);

        when(requestRepository.findAllByRequestor_IdNotOrderByCreatedDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(r)));
        when(requestMapper.toDto(r)).thenReturn(dto);

        List<ItemRequestDto> out = requestService.getAll(userId, 0, 10);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getItems()).isNotNull();
        assertThat(out.get(0).getItems()).isEmpty();

        verify(itemRepository, never()).findAllByRequest_IdIn(anyList());
    }

    @Test
    void getAll_whenUserNotFound_thenThrowsNotFound() {
        long userId = 404L;

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getAll(userId, 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).existsById(userId);
        verifyNoInteractions(requestRepository, requestMapper, itemRepository);
    }

    @Test
    void getById_whenRequestNotFound_thenThrowsNotFound() {
        long userId = 1L;
        long requestId = 999L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.getById(userId, requestId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Request not found");

        verify(itemRepository, never()).findAllByRequest_IdIn(anyList());
    }

    @Test
    void getById_whenUserNotFound_thenThrowsNotFound() {
        long userId = 404L;
        long requestId = 1L;

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getById(userId, requestId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).existsById(userId);
        verifyNoInteractions(requestRepository, requestMapper, itemRepository);
    }

    @Test
    void getById_whenExists_thenReturnsDtoWithAttachedItems() {
        long userId = 1L;
        long requestId = 77L;

        when(userRepository.existsById(userId)).thenReturn(true);

        ItemRequest r = new ItemRequest();
        r.setId(requestId);
        r.setDescription("need");
        r.setCreated(LocalDateTime.now());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(r));

        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(requestId);
        dto.setDescription("need");
        dto.setCreated(r.getCreated());
        dto.setItems(null);

        when(requestMapper.toDto(r)).thenReturn(dto);

        Item item = new Item();
        item.setId(10L);
        item.setName("drill");
        User owner = new User();
        owner.setId(5L);
        item.setOwner(owner);
        item.setRequest(r);

        when(itemRepository.findAllByRequest_IdIn(eq(List.of(requestId)))).thenReturn(List.of(item));

        ItemRequestDto out = requestService.getById(userId, requestId);

        assertThat(out.getId()).isEqualTo(requestId);
        assertThat(out.getItems())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new ItemResponseDto(10L, "drill", 5L));
    }
}
