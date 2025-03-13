package ro.unibuc.hello.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.hello.data.UserRepository;
import ro.unibuc.hello.data.entity.UserEntity;
import ro.unibuc.hello.dto.User;
import ro.unibuc.hello.exception.EntityNotFoundException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        List<UserEntity> entities = userRepository.findAll();
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public User getUserById(String id) throws EntityNotFoundException {
        Optional<UserEntity> optionalEntity = userRepository.findById(id);
        UserEntity entity = optionalEntity.orElseThrow(() -> new EntityNotFoundException("User with ID: " + id));
        return convertToDto(entity);
    }

    public User getUserByUsername(String username) throws EntityNotFoundException {
        UserEntity entity = userRepository.findByUsername(username);
        if (entity == null) {
            throw new EntityNotFoundException("User with username: " + username);
        }
        return convertToDto(entity);
    }

    public User createUser(User user) {
        UserEntity entity = convertToEntity(user);
        entity.setId(null); // Ensure we create a new user
        UserEntity savedEntity = userRepository.save(entity);
        return convertToDto(savedEntity);
    }

    public User updateUser(String id, User user) throws EntityNotFoundException {
        Optional<UserEntity> optionalEntity = userRepository.findById(id);
        UserEntity entity = optionalEntity.orElseThrow(() -> new EntityNotFoundException("User with ID: " + id));
        
        // Update fields
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            entity.setPassword(user.getPassword());
        }
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        
        UserEntity updatedEntity = userRepository.save(entity);
        return convertToDto(updatedEntity);
    }

    public void deleteUser(String id) throws EntityNotFoundException {
        Optional<UserEntity> optionalEntity = userRepository.findById(id);
        UserEntity entity = optionalEntity.orElseThrow(() -> new EntityNotFoundException("User with ID: " + id));
        userRepository.delete(entity);
    }

    private User convertToDto(UserEntity entity) {
        return new User(
            entity.getId(),
            entity.getUsername(),
            entity.getEmail(),
            entity.getFirstName(),
            entity.getLastName()
        );
    }

    private UserEntity convertToEntity(User dto) {
        return new UserEntity(
            dto.getId(),
            dto.getUsername(),
            dto.getEmail(),
            dto.getPassword(),
            dto.getFirstName(),
            dto.getLastName()
        );
    }
}