package ro.unibuc.hello.data;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import ro.unibuc.hello.data.entity.UserEntity;

/**
 * Spring Data MongoDB automatically creates a repository implementation
 * when the application starts.
 */
@Repository
public interface UserRepository extends MongoRepository<UserEntity, String> {
    UserEntity findByUsername(String username);
    UserEntity findByEmail(String email);
    List<UserEntity> findByLastName(String lastName);
}