package com.kevinguanchedarias.owgejava.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TorSummaryResult {
    private String version;
    private String buildRevision;
    private String relaysPublished;
    private List<Object> relays;
}
