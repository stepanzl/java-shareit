package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void create_whenRequestIdProvided_thenPersistLinkAndReturnDtoWithEmptyComments() {
        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@mail.com");
        owner = userRepository.save(owner);

        User requestor = new User();
        requestor.setName("Requestor");
        requestor.setEmail("requestor@mail.com");
        requestor = userRepository.save(requestor);

        ItemRequest req = new ItemRequest();
        req.setDescription("Need a drill");
        req.setRequestor(requestor);
        req.setCreated(LocalDateTime.now().minusDays(1));
        req = itemRequestRepository.save(req);

        ItemCreateDto createDto = new ItemCreateDto();
        createDto.setName("Drill");
        createDto.setDescription("Power drill");
        createDto.setAvailable(true);
        createDto.setRequestId(req.getId());

        ItemDto created = itemService.create(owner.getId(), createDto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Drill");
        assertThat(created.getRequestId()).isEqualTo(req.getId());
        assertThat(created.getComments()).isNotNull();
        assertThat(created.getComments()).isEmpty();

        Item savedItem = itemRepository.findById(created.getId()).orElseThrow();
        assertThat(savedItem.getOwner()).isNotNull();
        assertThat(savedItem.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(savedItem.getRequest()).isNotNull();
        assertThat(savedItem.getRequest().getId()).isEqualTo(req.getId());
    }

    @Test
    void addComment_whenNoPastApprovedBooking_thenThrow_thenAfterApprovedPastBooking_thenSuccess() {
        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner2@mail.com");
        owner = userRepository.save(owner);

        User booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@mail.com");
        booker = userRepository.save(booker);

        Item item = new Item();
        item.setName("Item");
        item.setDescription("Desc");
        item.setAvailable(true);
        item.setOwner(owner);
        item = itemRepository.save(item);

        CommentCreateDto commentDto = new CommentCreateDto();
        commentDto.setText("Nice item");

        User finalBooker = booker;
        Item finalItem = item;
        assertThatThrownBy(() -> itemService.addComment(finalBooker.getId(), finalItem.getId(), commentDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("has not completed approved booking");

        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.APPROVED);
        booking.setStart(LocalDateTime.now().minusDays(2));
        booking.setEnd(LocalDateTime.now().minusDays(1));
        bookingRepository.save(booking);

        var savedComment = itemService.addComment(booker.getId(), item.getId(), commentDto);

        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getText()).isEqualTo("Nice item");
        assertThat(savedComment.getAuthorName()).isEqualTo(booker.getName());
        assertThat(savedComment.getCreated()).isNotNull();
    }

    @Test
    void getById_whenOwner_thenIncludesLastNextBookingsAndComments_andCreateWithRequestIdIsMapped() {
        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@mail.com");
        owner = userRepository.save(owner);

        User booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@mail.com");
        booker = userRepository.save(booker);

        ItemRequest req = new ItemRequest();
        req.setDescription("Need a drill");
        req.setRequestor(booker);
        req = itemRequestRepository.save(req);

        ItemCreateDto createDto = new ItemCreateDto();
        createDto.setName("Drill");
        createDto.setDescription("Power drill");
        createDto.setAvailable(true);
        createDto.setRequestId(req.getId());

        ItemDto created = itemService.create(owner.getId(), createDto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRequestId()).isEqualTo(req.getId());
        assertThat(created.getComments()).isEmpty();

        Long itemId = created.getId();
        Item itemEntity = itemRepository.findById(itemId).orElseThrow();

        Comment comment = new Comment();
        comment.setText("Great item");
        comment.setAuthor(booker);
        comment.setItem(itemEntity);
        commentRepository.save(comment);

        LocalDateTime now = LocalDateTime.now();

        Booking past = new Booking();
        past.setItem(itemEntity);
        past.setBooker(booker);
        past.setStatus(BookingStatus.APPROVED);
        past.setStart(now.minusDays(2));
        past.setEnd(now.minusDays(1));
        past = bookingRepository.save(past);

        Booking future = new Booking();
        future.setItem(itemEntity);
        future.setBooker(booker);
        future.setStatus(BookingStatus.APPROVED);
        future.setStart(now.plusDays(1));
        future.setEnd(now.plusDays(2));
        future = bookingRepository.save(future);

        ItemDto dto = itemService.getById(owner.getId(), itemId);

        assertThat(dto.getComments()).hasSize(1);
        assertThat(dto.getComments().get(0).getText()).isEqualTo("Great item");
        assertThat(dto.getComments().get(0).getAuthorName()).isEqualTo("Booker");

        assertThat(dto.getLastBooking()).isNotNull();
        assertThat(dto.getNextBooking()).isNotNull();

        assertThat(dto.getLastBooking().getId()).isEqualTo(past.getId());
        assertThat(dto.getNextBooking().getId()).isEqualTo(future.getId());
    }
}
