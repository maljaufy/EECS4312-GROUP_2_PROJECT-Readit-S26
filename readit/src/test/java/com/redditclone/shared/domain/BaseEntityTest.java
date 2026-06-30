package com.redditclone.shared.domain;

import com.redditclone.shared.config.JpaAuditingConfig;
import com.redditclone.user.domain.User;
import com.redditclone.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = "spring.sql.init.enabled=false")
public class BaseEntityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void baseEntity_AuditingFields_ShouldBePopulated() {
        User user = new User();
        user.setUsername("audituser");
        user.setEmail("audit@example.com");
        user.setPasswordHash("hashed");
        user.setRoles(Set.of());

        userRepository.save(user);
        entityManager.flush();

        User saved = userRepository.findByUsername("audituser").orElseThrow();

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        // createdBy and updatedBy are set by AuditorAware – may be "system" if no authentication
        assertNotNull(saved.getCreatedBy());
        assertNotNull(saved.getUpdatedBy());
        // Version should be initialized
        assertNotNull(saved.getVersion());
    }

    @Test
    public void baseEntity_Version_ShouldIncrementOnUpdate() {
        User user = new User();
        user.setUsername("versionuser");
        user.setEmail("version@example.com");
        user.setPasswordHash("hashed");
        user.setRoles(Set.of());

        userRepository.save(user);
        entityManager.flush();
        User saved = userRepository.findByUsername("versionuser").orElseThrow();
        Long initialVersion = saved.getVersion();

        saved.setBio("Updated bio");
        userRepository.save(saved);
        entityManager.flush();

        User updated = userRepository.findByUsername("versionuser").orElseThrow();
        assertEquals(initialVersion + 1, updated.getVersion());
    }


}
