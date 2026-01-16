package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ItemController.class)
@Import(ErrorHandler.class)
class ItemControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void create_whenValidRequest_thenOkAndReturnsBody() throws Exception {
        ItemCreateDto in = new ItemCreateDto();
        in.setName("Drill");
        in.setDescription("Good drill");
        in.setAvailable(true);
        in.setRequestId(null);

        ItemDto out = new ItemDto();
        out.setId(1L);
        out.setName("Drill");
        out.setDescription("Good drill");
        out.setAvailable(true);
        out.setOwnerId(10L);
        out.setRequestId(null);
        out.setComments(List.of());

        when(itemService.create(ArgumentMatchers.eq(10L), ArgumentMatchers.any(ItemCreateDto.class)))
                .thenReturn(out);

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Drill"))
                .andExpect(jsonPath("$.description").value("Good drill"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void update_whenValidRequest_thenOkAndReturnsBody() throws Exception {
        ItemUpdateDto in = new ItemUpdateDto();
        in.setName("NewName");
        in.setDescription("NewDesc");
        in.setAvailable(false);

        ItemDto out = new ItemDto();
        out.setId(5L);
        out.setName("NewName");
        out.setDescription("NewDesc");
        out.setAvailable(false);
        out.setOwnerId(10L);
        out.setComments(List.of());

        when(itemService.update(ArgumentMatchers.eq(10L), ArgumentMatchers.eq(5L), ArgumentMatchers.any(ItemUpdateDto.class)))
                .thenReturn(out);

        mockMvc.perform(patch("/items/5")
                        .header(USER_HEADER, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("NewName"))
                .andExpect(jsonPath("$.description").value("NewDesc"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void getById_whenExists_thenOkAndReturnsBody() throws Exception {
        ItemDto out = new ItemDto();
        out.setId(7L);
        out.setName("Item");
        out.setDescription("Desc");
        out.setAvailable(true);
        out.setOwnerId(10L);
        out.setComments(List.of());

        when(itemService.getById(ArgumentMatchers.eq(10L), ArgumentMatchers.eq(7L)))
                .thenReturn(out);

        mockMvc.perform(get("/items/7")
                        .header(USER_HEADER, 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.name").value("Item"));
    }

    @Test
    void getById_whenNotFound_then404AndErrorBody() throws Exception {
        when(itemService.getById(ArgumentMatchers.eq(10L), ArgumentMatchers.eq(404L)))
                .thenThrow(new NotFoundException("Item not found"));

        mockMvc.perform(get("/items/404")
                        .header(USER_HEADER, 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Item not found"));
    }

    @Test
    void getByOwner_whenHasItems_thenOkAndReturnsList() throws Exception {
        ItemOwnerDto o1 = new ItemOwnerDto();
        o1.setId(1L);
        o1.setName("A");
        o1.setDescription("D");
        o1.setAvailable(true);
        o1.setComments(List.of());

        ItemOwnerDto o2 = new ItemOwnerDto();
        o2.setId(2L);
        o2.setName("B");
        o2.setDescription("D2");
        o2.setAvailable(false);
        o2.setComments(List.of());

        when(itemService.getByOwner(ArgumentMatchers.eq(10L)))
                .thenReturn(List.of(o1, o2));

        mockMvc.perform(get("/items")
                        .header(USER_HEADER, 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void search_whenTextProvided_thenOkAndReturnsList() throws Exception {
        ItemDto i1 = new ItemDto();
        i1.setId(1L);
        i1.setName("Drill");
        i1.setDescription("Good");
        i1.setAvailable(true);
        i1.setComments(List.of());

        when(itemService.search(ArgumentMatchers.eq("dr")))
                .thenReturn(List.of(i1));

        mockMvc.perform(get("/items/search")
                        .param("text", "dr")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Drill"));
    }

    @Test
    void addComment_whenValidRequest_thenOkAndReturnsBody() throws Exception {
        CommentCreateDto in = new CommentCreateDto();
        in.setText("Nice");

        CommentDto out = new CommentDto();
        out.setId(100L);
        out.setText("Nice");
        out.setAuthorName("Alice");
        out.setCreated(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(itemService.addComment(ArgumentMatchers.eq(10L), ArgumentMatchers.eq(5L), ArgumentMatchers.any(CommentCreateDto.class)))
                .thenReturn(out);

        mockMvc.perform(post("/items/5/comment")
                        .header(USER_HEADER, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.text").value("Nice"))
                .andExpect(jsonPath("$.authorName").value("Alice"))
                .andExpect(jsonPath("$.created").exists());
    }

    @Test
    void addComment_whenBadRequest_then400AndErrorBody() throws Exception {
        CommentCreateDto in = new CommentCreateDto();
        in.setText("");

        when(itemService.addComment(ArgumentMatchers.eq(10L), ArgumentMatchers.eq(5L), ArgumentMatchers.any(CommentCreateDto.class)))
                .thenThrow(new BadRequestException("Comment text must not be blank"));

        mockMvc.perform(post("/items/5/comment")
                        .header(USER_HEADER, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Comment text must not be blank"));
    }
}
