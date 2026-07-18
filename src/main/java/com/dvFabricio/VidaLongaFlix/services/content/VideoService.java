package com.dvFabricio.VidaLongaFlix.services.content;

import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoWatchEvent;
import com.dvFabricio.VidaLongaFlix.infra.config.CacheConfig;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import com.dvFabricio.VidaLongaFlix.infra.messaging.VideoEventPublisher;
import com.dvFabricio.VidaLongaFlix.repositories.VideoWatchEventRepository;
import com.dvFabricio.VidaLongaFlix.services.interaction.NotificationService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;
    private final VideoWatchEventRepository watchEventRepository;
    private final VideoEventPublisher videoEventPublisher;

    public VideoService(VideoRepository videoRepository, CategoryRepository categoryRepository,
                        NotificationService notificationService,
                        VideoWatchEventRepository watchEventRepository,
                        VideoEventPublisher videoEventPublisher) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
        this.notificationService = notificationService;
        this.watchEventRepository = watchEventRepository;
        this.videoEventPublisher = videoEventPublisher;
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.VIDEOS,            allEntries = true),
        @CacheEvict(value = CacheConfig.MOST_WATCHED,      allEntries = true),
        @CacheEvict(value = CacheConfig.LEAST_WATCHED,     allEntries = true),
        @CacheEvict(value = CacheConfig.VIEWS_BY_CATEGORY, allEntries = true)
    })
    @Transactional
    public void create(VideoRequestDTO request) {
        Video video = Video.builder()
                .title(request.title())
                .description(request.description())
                .url(request.url())
                .cover(request.cover())
                .category(findCategoryById(request.categoryId()))
                .recipe(request.recipe())
                .protein(request.protein())
                .carbs(request.carbs())
                .fat(request.fat())
                .fiber(request.fiber())
                .calories(request.calories())
                .build();

        saveVideo(video);
        videoEventPublisher.publishVideoPublished(video);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.VIDEOS,            key = "#id"),
        @CacheEvict(value = CacheConfig.MOST_WATCHED,      allEntries = true),
        @CacheEvict(value = CacheConfig.LEAST_WATCHED,     allEntries = true),
        @CacheEvict(value = CacheConfig.VIEWS_BY_CATEGORY, allEntries = true)
    })
    @Transactional
    public void update(UUID id, VideoRequestDTO request) {
        Video video = findVideoById(id);

        if (!isBlank(request.title()))       video.setTitle(request.title());
        if (!isBlank(request.description())) video.setDescription(request.description());
        if (!isBlank(request.url()))         video.setUrl(request.url());
        if (!isBlank(request.cover()))       video.setCover(request.cover());
        if (request.categoryId() != null)    video.setCategory(findCategoryById(request.categoryId()));
        if (request.recipe() != null)        video.setRecipe(request.recipe());
        if (request.protein() != null)       video.setProtein(request.protein());
        if (request.carbs() != null)         video.setCarbs(request.carbs());
        if (request.fat() != null)           video.setFat(request.fat());
        if (request.fiber() != null)         video.setFiber(request.fiber());
        if (request.calories() != null)      video.setCalories(request.calories());

        saveVideo(video);
    }

    @Cacheable(value = CacheConfig.VIDEOS, key = "#id")
    public VideoDTO findById(UUID id) {
        return new VideoDTO(findVideoById(id));
    }

    @Cacheable(value = CacheConfig.VIDEOS)
    public List<VideoDTO> findAll() {
        return videoRepository.findAll().stream()
                .map(VideoDTO::new)
                .toList();
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.VIDEOS,            key = "#id"),
        @CacheEvict(value = CacheConfig.VIDEOS,            allEntries = true),
        @CacheEvict(value = CacheConfig.MOST_WATCHED,      allEntries = true),
        @CacheEvict(value = CacheConfig.LEAST_WATCHED,     allEntries = true),
        @CacheEvict(value = CacheConfig.VIEWS_BY_CATEGORY, allEntries = true)
    })
    @Transactional
    public void delete(UUID id) {
        Video video = findVideoById(id);
        try {
            videoRepository.delete(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while deleting video with ID " + id + ": " + e.getMessage());
        }
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.MOST_WATCHED,      allEntries = true),
        @CacheEvict(value = CacheConfig.LEAST_WATCHED,     allEntries = true),
        @CacheEvict(value = CacheConfig.VIEWS_BY_CATEGORY, allEntries = true)
    })
    @Transactional
    public void registerView(UUID id, User currentUser) {
        Video video = findVideoById(id);
        video.setViews(video.getViews() + 1);
        saveVideo(video);
        watchEventRepository.save(new VideoWatchEvent(video, currentUser));
    }

    @Cacheable(value = CacheConfig.MOST_WATCHED, key = "#limit")
    public List<VideoDTO> getMostWatchedVideos(int limit) {
        return videoRepository.findTopByOrderByViewsDesc(Pageable.ofSize(limit)).stream()
                .map(VideoDTO::new)
                .toList();
    }

    @Cacheable(value = CacheConfig.LEAST_WATCHED, key = "#limit")
    public List<VideoDTO> getLeastWatchedVideos(int limit) {
        return videoRepository.findTopByOrderByViewsAsc(Pageable.ofSize(limit)).stream()
                .map(VideoDTO::new)
                .toList();
    }

    @Cacheable(value = CacheConfig.VIEWS_BY_CATEGORY)
    public Map<String, Long> getTotalViewsByCategory() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Category::getName,
                        category -> {
                            Long views = videoRepository.countViewsByCategoryId(category.getId());
                            return views != null ? views : 0L;
                        },
                        Long::sum
                ));
    }


    public List<VideoDTO> searchByText(String query) {
        return videoRepository.searchByText(query).stream()
                .map(VideoDTO::new)
                .toList();
    }

    public double getAverageWatchTime(UUID videoId) {
        return videoRepository.findAverageWatchTimeByVideoId(videoId)
                .orElseThrow(() -> new ResourceNotFoundExceptions(
                        "Video with ID " + videoId + " has no watch time data."));
    }

    public List<VideoDTO> getVideosWithMostComments(int limit) {
        return videoRepository.findTopByOrderByCommentsCountDesc(Pageable.ofSize(limit)).stream()
                .map(VideoDTO::new)
                .toList();
    }

    // --- Métodos privados ---

    private void saveVideo(Video video) {
        try {
            videoRepository.save(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while saving video: " + e.getMessage());
        }
    }

    private Video findVideoById(UUID id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExceptions(
                        "Video with ID " + id + " not found."));
    }

    private Category findCategoryById(UUID categoryId) {
        if (categoryId == null) {
            throw new MissingRequiredFieldException("category", "The video category is required.");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundExceptions(
                        "Category with ID " + categoryId + " not found."));
    }

    private boolean isBlank(String field) {
        return field == null || field.isBlank();
    }
}
