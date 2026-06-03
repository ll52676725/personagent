package com.example.agentplatform.agent.techreport.service;

import com.example.agentplatform.agent.techreport.dto.TechReportRequestDTO;
import com.example.agentplatform.agent.techreport.dto.TechReportResultDTO;
import reactor.core.publisher.Flux;

public interface TechReportService {

    TechReportResultDTO generateReport(TechReportRequestDTO request);

    TechReportResultDTO generateOutline(TechReportRequestDTO request);

    TechReportResultDTO generateFullReport(TechReportRequestDTO request);

    Flux<String> generateReportStream(TechReportRequestDTO request);

    String exportToMarkdown(TechReportResultDTO result);

    String exportToPptOutline(TechReportResultDTO result);
}
