package care.smith.top.backend.repository;

import care.smith.top.backend.model.UserDao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserDao, String> {
  Optional<UserDao> findByUsername(String username);
}
