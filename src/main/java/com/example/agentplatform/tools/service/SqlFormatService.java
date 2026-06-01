package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.SqlFormatRequestDTO;
import com.example.agentplatform.tools.dto.SqlFormatResultDTO;

public interface SqlFormatService {

    SqlFormatResultDTO format(SqlFormatRequestDTO request);

    SqlFormatResultDTO compact(SqlFormatRequestDTO request);

    SqlFormatResultDTO validate(SqlFormatRequestDTO request);

    SqlFormatResultDTO fixWithAI(SqlFormatRequestDTO request);

    SqlFormatResultDTO optimizeWithAI(SqlFormatRequestDTO request);
}
