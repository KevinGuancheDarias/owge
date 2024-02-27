package com.kevinguanchedarias.owgejava.business.wiki.generator;

import java.nio.file.Path;
import java.util.List;

public interface WikiGenerator {
    List<Path> generateFrontend(String backendUrl);
}
