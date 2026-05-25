package com.example.agentplatform.platform.service;

import com.example.agentplatform.platform.entity.Agent;

import java.util.List;

public interface AgentService {
    List<Agent> getAllAgents();
    Agent getAgentById(Long id);
    Agent getAgentByCode(String code);
}