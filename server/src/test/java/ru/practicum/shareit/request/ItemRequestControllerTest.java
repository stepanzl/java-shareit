package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.ItemAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ItemRequestController.class)
@Import(ErrorHandler.class)
class ItemRequestControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestService requestService;

    @Test
    void create_whenValidRequest_thenOkAndReturnsBody() throws Exception {
        ItemRequestCreateDto in = new ItemRequestCreateDto();
        in.setDescription("Need a drill");

        ItemRequestDto out = new ItemRequestDto();
        out.setId(1L);
        out.setDescription("Need a drill");
        out.setCreated(LocalDateTime.of(2026, 1, 15, 12, 0));
        out.setItems(List.of());

        when(requestService.create(ArgumentMatchers.eq(10L), ArgumentMatchers.any(ItemRequestCreateDto.class)))
                .thenReturn(out);

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description").value("Need a drill"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void getOwn_whenHasRequests_thenOkAndReturnsList() throws Exception {
        ItemRequestDto r1 = new ItemRequestDto();
        r1.setId(1L);
        r1.setDescription("Req1");
        r1.setCreated(LocalDateTime.of(2026, 1, 15, 12, 0));
        r1.setItems(List.of());

        ItemRequestDto r2 = new ItemRequestDto();
        r2.setId(2L);
        r2.setDescription("Req2");
        r2.setCreated(LocalDateTime.of(2026, 1, 15, 13, 0));
        r2.setItems(List.of(new ItemAnswerDto(100L, "Item", 10L)));

        when(requestService.getOwn(ArgumentMatchers.eq(10L))).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/requests")
                        .header(USER_HEADER, 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].items", hasSize(1)))
                .andExpect(jsonPath("$[1].items[0].itemId").value(100L))
                .andExpect(jsonPath("$[1].items[0].name").value("Item"))
                .andExpect(jsonPath("$[1].items[0].ownerId").value(10L));
    }

    @Test
    void getAll_whenHasRequests_thenOkAndReturnsList() throws Exception {
        ItemRequestDto r = new ItemRequestDto();
        r.setId(3L);
        r.setDescription("Other request");
        r.setCreated(LocalDateTime.of(2026, 1, 15, 14, 0));
        r.setItems(List.of());

        when(requestService.getAll(ArgumentMatchers.eq(10L), ArgumentMatchers.eq(0), ArgumentMatchers.eq(10)))
                .thenReturn(List.of(r));

        mockMvc.perform(get("/requests/all")
                        .header(USER_HEADER, 10L)
                        .param("from", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(3L))
                .andExpect(jsonPath("$[0].description").value("Other request"));
    }

    @Test
    void getById_whenExists_thenOkAndReturnsBody() throws Exception {
        ItemRequestDto out = new ItemRequestDto();
        out.setId(7L);
        out.setDescription("Need a ladder");
        out.setCreated(LocalDateTime.of(2026, 1, 15, 15, 0));
        out.setItems(List.of(new ItemAnswerDto(200L, "Ladder", 11L)));

        when(requestService.getById(ArgumentMatchers.eq(10L), ArgumentMatchers.eq(7L)))
                .thenReturn(out);

        mockMvc.perform(get("/requests/7")
                        .header(USER_HEADER, 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.description").value("Need a ladder"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].itemId").value(200L))
                .andExpect(jsonPath("$.items[0].name").value("Ladder"))
                .andExpect(jsonPath("$.items[0].ownerId").value(11L));
    }

    @Test
    void getById_whenNotFound_then404AndErrorBody() throws Exception {
        when(requestService.getById(ArgumentMatchers.eq(10L), ArgumentMatchers.eq(404L)))
                .thenThrow(new NotFoundException("Request not found: 404"));

        mockMvc.perform(get("/requests/404")
                        .header(USER_HEADER, 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Request not found: 404"));
    }
}
