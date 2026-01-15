package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ItemRequestServiceIntegrationTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User requestor;
    private User owner;

    @BeforeEach
    void setUp() {
        requestor = new User();
        requestor.setName("requestor");
        requestor.setEmail("requestor@mail.com");
        requestor = userRepository.save(requestor);

        owner = new User();
        owner.setName("owner");
        owner.setEmail("owner2@mail.com");
        owner = userRepository.save(owner);
    }

    @Test
    void getOwn_shouldReturnRequestsWithItemsAnswersAttached() {
        ItemRequest req = new ItemRequest();
        req.setDescription("need a drill");
        req.setRequestor(requestor);
        req.setCreated(LocalDateTime.now());
        req = itemRequestRepository.save(req);

        Item item = new Item();
        item.setName("drill");
        item.setDescription("powerful drill");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setRequest(req);
        item = itemRepository.save(item);

        List<ItemRequestDto> own = itemRequestService.getOwn(requestor.getId());

        assertThat(own).hasSize(1);
        ItemRequestDto dto = own.get(0);

        assertThat(dto.getId()).isEqualTo(req.getId());
        assertThat(dto.getDescription()).isEqualTo(req.getDescription());
        assertThat(dto.getItems()).hasSize(1);

        var answer = dto.getItems().get(0);
        assertThat(answer.getItemId()).isEqualTo(item.getId());
        assertThat(answer.getName()).isEqualTo(item.getName());
        assertThat(answer.getOwnerId()).isEqualTo(owner.getId());
    }
}
