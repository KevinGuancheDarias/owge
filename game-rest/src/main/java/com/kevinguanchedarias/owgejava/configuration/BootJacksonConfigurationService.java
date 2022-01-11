package com.kevinguanchedarias.owgejava.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        DefaultFormattingConversionService defaultFormattingConversionService = (DefaultFormattingConversionService) conversionService;
        converters.forEach(defaultFormattingConversionService::addConverter);
        return mapper;
    }
}
