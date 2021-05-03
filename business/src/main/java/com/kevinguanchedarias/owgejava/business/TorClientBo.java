package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.TorIpData;
import com.kevinguanchedarias.owgejava.pojo.TorSummaryResult;
import com.kevinguanchedarias.owgejava.repository.TorIpDataRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Log4j2
public class TorClientBo {
    private static  final String TOR_URL = "https://onionoo.torproject.org/summary?limit=1&search=";

    private final TorIpDataRepository torIpDataRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public TorClientBo(TorIpDataRepository torIpDataRepository) {
        this.torIpDataRepository = torIpDataRepository;
    }

    public boolean isTor(String ip) {
        var torIpDataOptional = torIpDataRepository.findById(ip);
        TorIpData torIpData;
        if(torIpDataOptional.isPresent()) {
            torIpData = torIpDataOptional.get();
            if(torIpData.getLastCheckedDate().minus(15, ChronoUnit.DAYS).compareTo(LocalDateTime.now()) > 0) {
                torIpData.setTor(doCheckIsTor(ip));
                torIpData.setLastCheckedDate(LocalDateTime.now());
            }
        } else {
            torIpData = torIpDataRepository.save(TorIpData.builder()
                    .ip(ip)
                    .isTor(doCheckIsTor(ip))
                    .lastCheckedDate(LocalDateTime.now())
                    .build()
            );
        }
        return torIpData.isTor();
    }

    private boolean doCheckIsTor(String ip) {
        var response = restTemplate.getForObject(TOR_URL + ip, TorSummaryResult.class);
        return response != null && !response.getRelays().isEmpty();
    }
}
