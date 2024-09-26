package com.dvFabricio.NutriLongaVidaFlix.services;

import com.dvFabricio.NutriLongaVidaFlix.domain.category.Category;
import com.dvFabricio.NutriLongaVidaFlix.domain.rating.Rating;
import com.dvFabricio.NutriLongaVidaFlix.domain.video.VideoDTO;
import com.dvFabricio.NutriLongaVidaFlix.domain.video.Video;
import com.dvFabricio.NutriLongaVidaFlix.infra.exception.ResourceNotFoundException;
import com.dvFabricio.NutriLongaVidaFlix.repositories.CategoryRepository;
import com.dvFabricio.NutriLongaVidaFlix.repositories.RatingRepository;
import com.dvFabricio.NutriLongaVidaFlix.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StorageService storageService;

    @Transactional
    public VideoDTO createVideo(VideoDTO videoDTO, MultipartFile file) throws IOException {
        Video video = new Video();
        populateVideoDetails(video, videoDTO);

        if (file != null && !file.isEmpty()) {
            String videoUrl = storageService.uploadFile(file);
            video.setUrl(videoUrl);
        }

        video = videoRepository.save(video);
        return new VideoDTO(video, calcularMediaAvaliacoes(video.getId()));
    }

    @Transactional
    public VideoDTO updateVideo(Long id, VideoDTO videoDTO, MultipartFile file) throws IOException {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id " + id));
        populateVideoDetails(video, videoDTO);

        if (file != null && !file.isEmpty()) {
            String videoUrl = storageService.uploadFile(file);
            video.setUrl(videoUrl);
        }

        video = videoRepository.save(video);
        return new VideoDTO(video, calcularMediaAvaliacoes(video.getId()));
    }

    @Transactional(readOnly = true)
    public Page<VideoDTO> getAllVideos(Pageable pageable) {
        return videoRepository.findAll(pageable)
                .map(video -> new VideoDTO(video, calcularMediaAvaliacoes(video.getId())));
    }

    @Transactional(readOnly = true)
    public Page<VideoDTO> getVideosByCategory(String category, Pageable pageable) {
        return videoRepository.findByCategory_Name(category, pageable)
                .map(video -> new VideoDTO(video, calcularMediaAvaliacoes(video.getId())));
    }

    @Transactional(readOnly = true)
    public Page<VideoDTO> searchVideosByTitle(String title, Pageable pageable) {
        return videoRepository.findByTitleContainingIgnoreCase(title, pageable)
                .map(video -> new VideoDTO(video, calcularMediaAvaliacoes(video.getId())));
    }

    @Transactional(readOnly = true)
    public Page<VideoDTO> getVideosByMinimumScore(int score, Pageable pageable) {
        return videoRepository.findByRatings_ScoreGreaterThanEqual(score, pageable)
                .map(video -> new VideoDTO(video, calcularMediaAvaliacoes(video.getId())));
    }

    @Transactional(readOnly = true)
    public VideoDTO getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id " + id));
        return new VideoDTO(video, calcularMediaAvaliacoes(video.getId()));
    }

    @Transactional
    public void deleteVideo(Long id) {
        if (!videoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Video not found with id " + id);
        }
        videoRepository.deleteById(id);
    }

    private void populateVideoDetails(Video video, VideoDTO videoDTO) {
        video.setTitle(videoDTO.title());
        video.setDescription(videoDTO.description());
        video.setUrl(videoDTO.url());

        Category category = categoryRepository.findByName(videoDTO.categoryName())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + videoDTO.categoryName()));
        video.setCategory(category);
    }

    private double calcularMediaAvaliacoes(Long videoId) {
        List<Rating> ratings = ratingRepository.findByVideo_Id(videoId);
        return ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);
    }
}