package com.example.agentplatform.platform.repository;

import com.example.agentplatform.platform.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByCode(String code);
    List<Agent> findByStatus(Integer status);
    boolean existsByCode(String code);
}