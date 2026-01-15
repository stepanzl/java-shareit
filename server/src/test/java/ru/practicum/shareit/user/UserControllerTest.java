package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(ErrorHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void create_whenValidRequest_thenOkAndReturnsBody() throws Exception {
        UserCreateDto in = new UserCreateDto();
        in.setName("Alice");
        in.setEmail("alice@mail.com");

        UserDto out = new UserDto();
        out.setId(1L);
        out.setName("Alice");
        out.setEmail("alice@mail.com");

        when(userService.create(ArgumentMatchers.any(UserCreateDto.class))).thenReturn(out);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@mail.com"));
    }

    @Test
    void getAll_whenHasUsers_thenOkAndReturnsList() throws Exception {
        UserDto u1 = new UserDto();
        u1.setId(1L);
        u1.setName("A");
        u1.setEmail("a@mail.com");

        UserDto u2 = new UserDto();
        u2.setId(2L);
        u2.setName("B");
        u2.setEmail("b@mail.com");

        when(userService.getAll()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void getById_whenExists_thenOkAndReturnsUser() throws Exception {
        UserDto out = new UserDto();
        out.setId(10L);
        out.setName("User");
        out.setEmail("u@mail.com");

        when(userService.getById(10L)).thenReturn(out);

        mockMvc.perform(get("/users/10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("User"))
                .andExpect(jsonPath("$.email").value("u@mail.com"));
    }

    @Test
    void update_whenValidRequest_thenOkAndReturnsUpdatedUser() throws Exception {
        UserUpdateDto in = new UserUpdateDto();
        in.setName("NewName");
        in.setEmail("new@mail.com");

        UserDto out = new UserDto();
        out.setId(5L);
        out.setName("NewName");
        out.setEmail("new@mail.com");

        when(userService.update(ArgumentMatchers.eq(5L), ArgumentMatchers.any(UserUpdateDto.class)))
                .thenReturn(out);

        mockMvc.perform(patch("/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("NewName"))
                .andExpect(jsonPath("$.email").value("new@mail.com"));
    }

    @Test
    void delete_whenOk_thenOk() throws Exception {
        doNothing().when(userService).delete(7L);

        mockMvc.perform(delete("/users/7"))
                .andExpect(status().isOk())
                .andExpect(content().string("")); // void -> empty body
    }

    @Test
    void getById_whenNotFound_then404AndErrorBody() throws Exception {
        when(userService.getById(404L)).thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(get("/users/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void create_whenConflict_then409AndErrorBody() throws Exception {
        UserCreateDto in = new UserCreateDto();
        in.setName("Alice");
        in.setEmail("alice@mail.com");

        when(userService.create(ArgumentMatchers.any(UserCreateDto.class)))
                .thenThrow(new ConflictException("Email already exists"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void update_whenBadRequest_then400AndErrorBody() throws Exception {
        UserUpdateDto in = new UserUpdateDto();
        in.setName("");
        in.setEmail("x@mail.com");

        when(userService.update(ArgumentMatchers.eq(1L), ArgumentMatchers.any(UserUpdateDto.class)))
                .thenThrow(new BadRequestException("Name must not be blank"));

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Name must not be blank"));
    }

    @Test
    void delete_whenForbidden_then403AndErrorBody() throws Exception {
        doThrow(new ForbiddenException("Forbidden"))
                .when(userService).delete(1L);

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
