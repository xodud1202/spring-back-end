package com.xodud1202.springbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xodud1202.springbackend.domain.UserBase;

@Repository
public interface UserRepository extends JpaRepository<UserBase, Long> {
    Optional<UserBase> findByLoginId(String loginId);
}
