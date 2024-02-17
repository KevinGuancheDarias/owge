package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.entity.TrackBrowser;
import com.kevinguanchedarias.owgejava.repository.TrackBrowserRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.time.Instant;

@RestController
@RequestMapping("game/track-browser")
@ApplicationScope
@AllArgsConstructor
public class TrackBrowserRestService {
    private final TrackBrowserRepository trackBrowserRepository;

    @PostMapping("warn")
    public void warn(@RequestBody String body) {
        doTrack("warn", body);
    }

    @PostMapping("error")
    public void error(@RequestBody String body) {
        doTrack("error", body);
    }

    private void doTrack(String method, String content) {
        trackBrowserRepository.save(
                TrackBrowser.builder()
                        .method(method)
                        .jsonContent(content)
                        .createdAt(Instant.now())
                        .build()
        );
    }
}
