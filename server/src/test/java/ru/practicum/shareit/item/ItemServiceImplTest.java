package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemMapper itemMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    void create_whenValidWithoutRequestId_thenSavesAndReturnsDtoWithEmptyComments() {
        long ownerId = 1L;

        ItemCreateDto in = new ItemCreateDto();
        in.setName("Drill");
        in.setDescription("Good drill");
        in.setAvailable(true);
        in.setRequestId(null);

        User owner = new User();
        owner.setId(ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        Item model = new Item();
        model.setName("Drill");
        model.setDescription("Good drill");
        model.setAvailable(true);
        when(itemMapper.toModel(in)).thenReturn(model);

        Item saved = new Item();
        saved.setId(10L);
        saved.setOwner(owner);
        saved.setName("Drill");
        saved.setDescription("Good drill");
        saved.setAvailable(true);
        when(itemRepository.save(any(Item.class))).thenReturn(saved);

        ItemDto out = new ItemDto();
        out.setId(10L);
        out.setName("Drill");
        out.setDescription("Good drill");
        out.setAvailable(true);
        when(itemMapper.toDto(saved)).thenReturn(out);

        ItemDto result = itemService.create(ownerId, in);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getComments()).isNotNull();
        assertThat(result.getComments()).isEmpty();

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isSameAs(owner);

        verify(itemRequestRepository, never()).findById(anyLong());
        verifyNoMoreInteractions(itemRepository, userRepository, itemRequestRepository, itemMapper, commentRepository, bookingRepository, commentMapper);
    }

    @Test
    void create_whenRequestIdProvidedButNotFound_thenThrowsNotFound() {
        long ownerId = 1L;

        ItemCreateDto in = new ItemCreateDto();
        in.setName("Drill");
        in.setDescription("Good drill");
        in.setAvailable(true);
        in.setRequestId(99L);

        User owner = new User();
        owner.setId(ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        Item model = new Item();
        when(itemMapper.toModel(in)).thenReturn(model);

        when(itemRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(ownerId, in))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Request not found: 99");

        verify(itemRequestRepository).findById(99L);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void getById_whenOwnerRequests_thenSetsLastAndNextBookingsFromApproved() {
        long ownerId = 1L;
        long itemId = 10L;

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setId(itemId);
        item.setOwner(owner);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItem_Id(eq(itemId), any(Sort.class))).thenReturn(List.of());

        Booking past = new Booking();
        past.setId(1L);
        past.setItem(item);
        past.setStart(LocalDateTime.now().minusDays(5));
        past.setEnd(LocalDateTime.now().minusDays(1));
        past.setStatus(BookingStatus.APPROVED);

        Booking future = new Booking();
        future.setId(2L);
        future.setItem(item);
        future.setStart(LocalDateTime.now().plusDays(1));
        future.setEnd(LocalDateTime.now().plusDays(2));
        future.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findAllByItem_IdAndStatus(eq(itemId), eq(BookingStatus.APPROVED), any(Sort.class)))
                .thenReturn(List.of(past, future));

        ItemDto base = new ItemDto();
        base.setId(itemId);
        when(itemMapper.toDto(item)).thenReturn(base);

        ItemDto result = itemService.getById(ownerId, itemId);

        assertThat(result.getId()).isEqualTo(itemId);
        assertThat(result.getComments()).isNotNull();
        assertThat(result.getLastBooking()).isNotNull();
        assertThat(result.getLastBooking().getId()).isEqualTo(1L);
        assertThat(result.getNextBooking()).isNotNull();
        assertThat(result.getNextBooking().getId()).isEqualTo(2L);
    }

    @Test
    void getById_whenNotOwner_thenDoesNotSetLastNextBookings() {
        long userId = 999L;
        long itemId = 10L;

        User owner = new User();
        owner.setId(1L);

        Item item = new Item();
        item.setId(itemId);
        item.setOwner(owner);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItem_Id(eq(itemId), any(Sort.class))).thenReturn(List.of());

        ItemDto base = new ItemDto();
        base.setId(itemId);
        when(itemMapper.toDto(item)).thenReturn(base);

        ItemDto result = itemService.getById(userId, itemId);

        assertThat(result.getLastBooking()).isNull();
        assertThat(result.getNextBooking()).isNull();

        verify(bookingRepository, never()).findAllByItem_IdAndStatus(anyLong(), any(), any());
    }

    @Test
    void search_whenBlankText_thenReturnsEmpty() {
        assertThat(itemService.search("   ")).isEmpty();
        assertThat(itemService.search(null)).isEmpty();

        verifyNoInteractions(itemRepository, commentRepository, bookingRepository, userRepository, itemMapper, commentMapper, itemRequestRepository);
    }

    @Test
    void addComment_whenNoPastApprovedBooking_thenThrowsBadRequest() {
        long authorId = 1L;
        long itemId = 10L;

        CommentCreateDto in = new CommentCreateDto();
        in.setText("Nice");

        User author = new User();
        author.setId(authorId);

        Item item = new Item();
        item.setId(itemId);

        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        when(bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                eq(itemId), eq(authorId), eq(BookingStatus.APPROVED), any(LocalDateTime.class)
        )).thenReturn(false);

        assertThatThrownBy(() -> itemService.addComment(authorId, itemId, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("has not completed approved booking");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_whenHasPastApprovedBooking_thenSavesAndReturnsDto() {
        long authorId = 1L;
        long itemId = 10L;

        CommentCreateDto in = new CommentCreateDto();
        in.setText("Nice");

        User author = new User();
        author.setId(authorId);
        author.setName("Author");

        Item item = new Item();
        item.setId(itemId);

        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        when(bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                eq(itemId), eq(authorId), eq(BookingStatus.APPROVED), any(LocalDateTime.class)
        )).thenReturn(true);

        Comment saved = new Comment();
        saved.setId(100L);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentDto out = new CommentDto();
        out.setId(100L);
        out.setText("Nice");
        when(commentMapper.toDto(saved)).thenReturn(out);

        CommentDto result = itemService.addComment(authorId, itemId, in);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getText()).isEqualTo("Nice");

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthor()).isSameAs(author);
        assertThat(captor.getValue().getItem()).isSameAs(item);
        assertThat(captor.getValue().getText()).isEqualTo("Nice");
    }

    @Test
    void create_whenNameBlank_thenBadRequest() {
        long ownerId = 1L;

        ItemCreateDto in = new ItemCreateDto();
        in.setName("  ");
        in.setDescription("d");
        in.setAvailable(true);

        assertThatThrownBy(() -> itemService.create(ownerId, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Item name");
    }

    @Test
    void create_whenDescriptionBlank_thenBadRequest() {
        long ownerId = 1L;

        ItemCreateDto in = new ItemCreateDto();
        in.setName("n");
        in.setDescription("");
        in.setAvailable(true);

        assertThatThrownBy(() -> itemService.create(ownerId, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Item description");
    }

    @Test
    void create_whenAvailableNull_thenBadRequest() {
        long ownerId = 1L;

        ItemCreateDto in = new ItemCreateDto();
        in.setName("n");
        in.setDescription("d");
        in.setAvailable(null);

        assertThatThrownBy(() -> itemService.create(ownerId, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("availability");
    }

    @Test
    void create_whenOwnerNotFound_thenNotFound() {
        long ownerId = 1L;

        ItemCreateDto in = new ItemCreateDto();
        in.setName("n");
        in.setDescription("d");
        in.setAvailable(true);

        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(ownerId, in))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void create_whenRequestIdProvidedAndFound_thenSavesWithRequest() {
        long ownerId = 1L;

        ItemCreateDto in = new ItemCreateDto();
        in.setName("Drill");
        in.setDescription("Good drill");
        in.setAvailable(true);
        in.setRequestId(99L);

        User owner = new User();
        owner.setId(ownerId);

        ru.practicum.shareit.request.model.ItemRequest req = new ru.practicum.shareit.request.model.ItemRequest();
        req.setId(99L);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(99L)).thenReturn(Optional.of(req));

        Item model = new Item();
        model.setName("Drill");
        model.setDescription("Good drill");
        model.setAvailable(true);
        when(itemMapper.toModel(in)).thenReturn(model);

        Item saved = new Item();
        saved.setId(10L);
        saved.setOwner(owner);
        saved.setRequest(req);
        saved.setName("Drill");
        saved.setDescription("Good drill");
        saved.setAvailable(true);
        when(itemRepository.save(any(Item.class))).thenReturn(saved);

        ItemDto out = new ItemDto();
        out.setId(10L);
        out.setName("Drill");
        out.setDescription("Good drill");
        out.setAvailable(true);
        out.setRequestId(99L);
        when(itemMapper.toDto(saved)).thenReturn(out);

        ItemDto result = itemService.create(ownerId, in);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getComments()).isNotNull();
        assertThat(result.getComments()).isEmpty();

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        assertThat(captor.getValue().getRequest()).isNotNull();
        assertThat(captor.getValue().getRequest().getId()).isEqualTo(99L);
    }

    @Test
    void update_whenItemNotFound_thenNotFound() {
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.update(1L, 10L, new ItemUpdateDto()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Item not found");
    }

    @Test
    void update_whenNotOwner_thenNotFound() {
        long ownerId = 1L;
        long otherId = 2L;

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setId(10L);
        item.setOwner(owner);

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.update(otherId, 10L, new ItemUpdateDto()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("for this owner");
    }

    @Test
    void update_whenNameBlank_thenBadRequest() {
        long ownerId = 1L;

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setId(10L);
        item.setOwner(owner);

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName("   ");

        assertThatThrownBy(() -> itemService.update(ownerId, 10L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Item name");
    }

    @Test
    void update_whenDescriptionBlank_thenBadRequest() {
        long ownerId = 1L;

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setId(10L);
        item.setOwner(owner);

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setDescription("");

        assertThatThrownBy(() -> itemService.update(ownerId, 10L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Item description");
    }

    @Test
    void addComment_whenTextBlank_thenBadRequest() {
        CommentCreateDto in = new CommentCreateDto();
        in.setText("   ");

        assertThatThrownBy(() -> itemService.addComment(1L, 10L, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void getByOwner_whenNoItems_thenReturnsEmptyAndDoesNotQueryBookingsComments() {
        long ownerId = 1L;
        User owner = new User();
        owner.setId(ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(itemRepository.findAllByOwner_Id(ownerId)).thenReturn(List.of());

        List<ItemOwnerDto> result = itemService.getByOwner(ownerId);

        assertThat(result).isEmpty();

        verify(commentRepository, never()).findAllByItem_IdIn(anyList(), any());
        verify(bookingRepository, never()).findByItem_IdInAndStatus(anyList(), any(), any());
    }

    @Test
    void getByOwner_whenHasItems_thenAttachesCommentsAndBookings() {
        long ownerId = 1L;
        User owner = new User();
        owner.setId(ownerId);

        Item item1 = new Item();
        item1.setId(10L);
        item1.setOwner(owner);
        item1.setName("Drill");
        item1.setDescription("Good");
        item1.setAvailable(true);

        Item item2 = new Item();
        item2.setId(11L);
        item2.setOwner(owner);
        item2.setName("Saw");
        item2.setDescription("Fine");
        item2.setAvailable(true);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(itemRepository.findAllByOwner_Id(ownerId)).thenReturn(List.of(item1, item2));

        // comments for both items (через item внутри Comment, чтобы groupingBy c.getItem().getId() работал)
        Comment c1 = new Comment();
        c1.setId(1L);
        c1.setItem(item1);
        Comment c2 = new Comment();
        c2.setId(2L);
        c2.setItem(item2);

        when(commentRepository.findAllByItem_IdIn(eq(List.of(10L, 11L)), any(Sort.class)))
                .thenReturn(List.of(c1, c2));

        CommentDto cd1 = new CommentDto();
        cd1.setId(1L);
        CommentDto cd2 = new CommentDto();
        cd2.setId(2L);
        when(commentMapper.toDto(c1)).thenReturn(cd1);
        when(commentMapper.toDto(c2)).thenReturn(cd2);

        // bookings approved for both items
        Booking pastFor1 = new Booking();
        pastFor1.setId(100L);
        pastFor1.setItem(item1);
        pastFor1.setStart(LocalDateTime.now().minusDays(3));
        pastFor1.setEnd(LocalDateTime.now().minusDays(2));
        pastFor1.setStatus(BookingStatus.APPROVED);

        Booking futureFor2 = new Booking();
        futureFor2.setId(200L);
        futureFor2.setItem(item2);
        futureFor2.setStart(LocalDateTime.now().plusDays(2));
        futureFor2.setEnd(LocalDateTime.now().plusDays(3));
        futureFor2.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findByItem_IdInAndStatus(eq(List.of(10L, 11L)), eq(BookingStatus.APPROVED), any(Sort.class)))
                .thenReturn(List.of(pastFor1, futureFor2));

        List<ItemOwnerDto> result = itemService.getByOwner(ownerId);

        assertThat(result).hasSize(2);

        ItemOwnerDto r1 = result.stream().filter(x -> x.getId().equals(10L)).findFirst().orElseThrow();
        assertThat(r1.getComments()).hasSize(1);
        assertThat(r1.getLastBooking()).isNotNull(); // past -> lastBooking

        ItemOwnerDto r2 = result.stream().filter(x -> x.getId().equals(11L)).findFirst().orElseThrow();
        assertThat(r2.getComments()).hasSize(1);
        assertThat(r2.getNextBooking()).isNotNull(); // future -> nextBooking
    }

    @Test
    void update_whenNameBlank_thenThrowsBadRequest() {
        long ownerId = 1L;
        long itemId = 10L;

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setId(itemId);
        item.setOwner(owner);

        ItemUpdateDto in = new ItemUpdateDto();
        in.setName("   "); // ветка validateNameUpdate

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.update(ownerId, itemId, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Item name must not be blank");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void create_whenMissingAvailable_thenThrowsBadRequest() {
        long ownerId = 1L;
        ItemCreateDto in = new ItemCreateDto();
        in.setName("Drill");
        in.setDescription("Good");
        in.setAvailable(null); // ветка validateCreate

        assertThatThrownBy(() -> itemService.create(ownerId, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("availability");

        verifyNoInteractions(userRepository, itemRepository, itemMapper);
    }

    @Test
    void addComment_whenBlankText_thenThrowsBadRequest() {
        CommentCreateDto in = new CommentCreateDto();
        in.setText("   "); // ветка в addComment до БД

        assertThatThrownBy(() -> itemService.addComment(1L, 2L, in))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be blank");

        verifyNoInteractions(userRepository, itemRepository, bookingRepository, commentRepository);
    }

    @Test
    void search_whenRepositoryReturnsItems_thenMapsSortedByIdAndAttachesComments() {
        String text = "drill";

        Item item2 = new Item();
        item2.setId(2L);
        Item item1 = new Item();
        item1.setId(1L);

        when(itemRepository.search(text)).thenReturn(List.of(item2, item1));

        Comment c1 = new Comment();
        Item i1ref = new Item();
        i1ref.setId(1L);
        c1.setItem(i1ref);

        Comment c2 = new Comment();
        Item i2ref = new Item();
        i2ref.setId(2L);
        c2.setItem(i2ref);

        // важно: не фиксируем порядок id в стабе, чтобы не ловить strict-stubbing
        when(commentRepository.findAllByItem_IdIn(anyList(), any(Sort.class)))
                .thenReturn(List.of(c1, c2));

        CommentDto cd1 = new CommentDto();
        cd1.setId(11L);
        cd1.setText("c1");
        when(commentMapper.toDto(c1)).thenReturn(cd1);

        CommentDto cd2 = new CommentDto();
        cd2.setId(22L);
        cd2.setText("c2");
        when(commentMapper.toDto(c2)).thenReturn(cd2);

        ItemDto dto1 = new ItemDto();
        dto1.setId(1L);
        when(itemMapper.toDto(item1)).thenReturn(dto1);

        ItemDto dto2 = new ItemDto();
        dto2.setId(2L);
        when(itemMapper.toDto(item2)).thenReturn(dto2);

        List<ItemDto> result = itemService.search(text);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        assertThat(result.get(0).getComments()).containsExactly(cd1);
        assertThat(result.get(1).getComments()).containsExactly(cd2);

        // проверяем, что в репозиторий ушли именно отсортированные id [1,2]
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(commentRepository).findAllByItem_IdIn(idsCaptor.capture(), any(Sort.class));
        assertThat(idsCaptor.getValue()).containsExactly(1L, 2L);

        verify(itemRepository).search(text);
    }

    @Test
    void search_whenRepositoryReturnsEmpty_thenReturnsEmptyAndNoCommentLoad() {
        when(itemRepository.search("x")).thenReturn(List.of());

        List<ItemDto> result = itemService.search("x");

        assertThat(result).isEmpty();
        verify(itemRepository).search("x");
        verifyNoInteractions(commentRepository, itemMapper, commentMapper);
    }




}
