package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommentMapperTest {

    private final CommentMapper mapper = new CommentMapper();

    @Test
    void toDto_mapsAuthorNameAndCreated() {
        User author = new User();
        author.setId(2L);
        author.setName("Alice");

        Comment comment = new Comment();
        comment.setId(10L);
        comment.setText("Nice!");
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.of(2026, 1, 15, 10, 0, 0));

        CommentDto dto = mapper.toDto(comment);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getText()).isEqualTo("Nice!");
        assertThat(dto.getAuthorName()).isEqualTo("Alice");
        assertThat(dto.getCreated()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0, 0));
    }

    @Test
    void toDto_whenNull_thenNull() {
        assertThat(mapper.toDto(null)).isNull();
    }
}
