package com.example.agentplatform.platform.service.impl;

import com.example.agentplatform.platform.entity.Agent;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.platform.repository.AgentRepository;
import com.example.agentplatform.platform.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {
    
    private final AgentRepository agentRepository;
    
    @Override
    public List<Agent> getAllAgents() {
        return agentRepository.findByStatus(1);
    }
    
    @Override
    public Agent getAgentById(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Agent不存在"));
    }
    
    @Override
    public Agent getAgentByCode(String code) {
        return agentRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Agent不存在"));
    }
}