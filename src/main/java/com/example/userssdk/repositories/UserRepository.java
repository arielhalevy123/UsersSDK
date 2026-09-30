package com.example.userssdk.repositories;

import com.example.userssdk.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByAdminId(Long adminId);

    /** Load users by admin including custom fields (e.g. Appointment) so admin profile can show appointment count. */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.customFields WHERE u.admin.id = :adminId")
    List<User> findByAdminIdWithCustomFields(@Param("adminId") Long adminId);
}