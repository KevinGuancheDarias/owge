package com.kevinguanchedarias.owgejava.business.wiki.generator;


import com.github.dockerjava.api.DockerClient;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DockerWikiGenerator {
    private final DockerClient dockerClient;
}
