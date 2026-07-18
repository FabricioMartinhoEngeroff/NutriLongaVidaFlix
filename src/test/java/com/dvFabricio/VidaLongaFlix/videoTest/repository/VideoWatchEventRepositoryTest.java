package com.dvFabricio.VidaLongaFlix.videoTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoWatchEvent;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoWatchEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class VideoWatchEventRepositoryTest {

    @Autowired private VideoWatchEventRepository watchEventRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    private Video video;
    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        Category category = categoryRepository.saveAndFlush(
                new Category("Receitas-" + UUID.randomUUID(), CategoryType.VIDEO));

        video = videoRepository.saveAndFlush(Video.builder()
                .title("Frango").description("Desc")
                .url("http://url.com").category(category)
                .views(0).watchTime(0).build());

        user1 = userRepository.saveAndFlush(
                new User("User1-" + UUID.randomUUID(), UUID.randomUUID() + "@email.com", "pass", "11999999991"));
        user2 = userRepository.saveAndFlush(
                new User("User2-" + UUID.randomUUID(), UUID.randomUUID() + "@email.com", "pass", "11999999992"));
    }

    @Test
    void shouldCountTotalEventsByVideo() {
        watchEventRepository.save(new VideoWatchEvent(video, user1));
        watchEventRepository.save(new VideoWatchEvent(video, user2));
        watchEventRepository.save(new VideoWatchEvent(video, user1));

        assertEquals(3, watchEventRepository.countByVideo_Id(video.getId()));
    }

    @Test
    void shouldCountDistinctUsersExcludingAnonymous() {
        watchEventRepository.save(new VideoWatchEvent(video, user1));
        watchEventRepository.save(new VideoWatchEvent(video, user2));
        watchEventRepository.save(new VideoWatchEvent(video, user1));
        watchEventRepository.save(new VideoWatchEvent(video, null));

        assertEquals(2, watchEventRepository.countDistinctUsersByVideoId(video.getId()));
    }

    @Test
    void shouldReturnZeroWhenNoEventsExist() {
        assertEquals(0, watchEventRepository.countByVideo_Id(video.getId()));
        assertEquals(0, watchEventRepository.countDistinctUsersByVideoId(video.getId()));
    }

    @Test
    void shouldCountOnlyEventsForSpecificVideo() {
        Category cat2 = categoryRepository.saveAndFlush(
                new Category("Cat2-" + UUID.randomUUID(), CategoryType.VIDEO));
        Video otherVideo = videoRepository.saveAndFlush(Video.builder()
                .title("Outro").description("Desc")
                .url("http://other.com").category(cat2)
                .views(0).watchTime(0).build());

        watchEventRepository.save(new VideoWatchEvent(video, user1));
        watchEventRepository.save(new VideoWatchEvent(otherVideo, user2));

        assertEquals(1, watchEventRepository.countByVideo_Id(video.getId()));
        assertEquals(1, watchEventRepository.countByVideo_Id(otherVideo.getId()));
    }
}
