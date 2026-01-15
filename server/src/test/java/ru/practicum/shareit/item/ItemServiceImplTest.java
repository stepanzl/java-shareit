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
}
