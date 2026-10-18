package com.campusfind.repository;
import com.campusfind.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface StudentRepository extends JpaRepository<Student,Long>{
 Optional<Student> findByEmailIgnoreCase(String email);
 Optional<Student> findByPasswordResetTokenHash(String passwordResetTokenHash);
 Optional<Student> findBySessionTokenHash(String sessionTokenHash);
 boolean existsByEmailIgnoreCase(String email);
}