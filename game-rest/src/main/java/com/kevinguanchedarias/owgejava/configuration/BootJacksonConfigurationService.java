package com.kevinguanchedarias.owgejava.configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BootJacksonConfigurationService {
	@Lazy
	@Autowired
	private ObjectMapper mapper;

	@PostConstruct
	public ObjectMapper configureMapper() {
		mapper.setDefaultPropertyInclusion(Include.NON_NULL);
	    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	    return mapper;
	}
}
