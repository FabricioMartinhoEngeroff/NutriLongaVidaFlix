package com.dvFabricio.VidaLongaFlix.controllers.content;

import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoDTO;
import com.dvFabricio.VidaLongaFlix.services.content.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public ResponseEntity<List<VideoDTO>> getAllVideos() {
        return ResponseEntity.ok(videoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO> getVideoById(@PathVariable UUID id) {
        return ResponseEntity.ok(videoService.findById(id));
    }

    @PatchMapping("/{id}/view")
    public ResponseEntity<Void> registerView(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        videoService.registerView(id, user);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/search")
    public ResponseEntity<List<VideoDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(videoService.searchByText(q));
    }

    @GetMapping("/most-viewed")
    public ResponseEntity<List<VideoDTO>> getMostViewed(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(videoService.getMostWatchedVideos(limit));
    }

    @GetMapping("/least-viewed")
    public ResponseEntity<List<VideoDTO>> getLeastViewed(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(videoService.getLeastWatchedVideos(limit));
    }

    @GetMapping("/views-by-category")
    public ResponseEntity<Map<String, Long>> getViewsByCategory() {
        return ResponseEntity.ok(videoService.getTotalViewsByCategory());
    }
}