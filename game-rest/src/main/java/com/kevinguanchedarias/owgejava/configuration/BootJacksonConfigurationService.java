package com.kevinguanchedarias.owgejava.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.Set;

@Service
public class BootJacksonConfigurationService {
    @Lazy
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Set<Converter<?, ?>> converters;

    @Autowired
    private ConversionService conversionService;

    @PostConstruct
    public ObjectMapper configureMapper() {
        mapper.setDefaultPropertyInclusion(Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        // LocalDateTime must serialize as an ISO string, never the Jackson
        // [y,m,d,…] array — the frontend doesn't consume arrays (it derives
        // countdowns from pendingMillis) and the Rust backend emits ISO.
        // Scoped to LocalDateTime only: java.util.Date/Instant keep epoch
        // millis (mission reports and the lastSent watermark depend on it).
        mapper.configOverride(java.time.LocalDateTime.class)
                .setFormat(com.fasterxml.jackson.annotation.JsonFormat.Value
                        .forShape(com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING));
        DefaultFormattingConversionService defaultFormattingConversionService = (DefaultFormattingConversionService) conversionService;
        converters.forEach(defaultFormattingConversionService::addConverter);
        return mapper;
    }
}
