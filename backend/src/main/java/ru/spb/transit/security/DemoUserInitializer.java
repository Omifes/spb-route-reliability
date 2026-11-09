package ru.spb.transit.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.spb.transit.domain.User;
import ru.spb.transit.repository.UserRepository;

/**
 * Создаёт демонстрационного пользователя для локального запуска.
 * В production отключается через APP_DEMO_USER_ENABLED=false.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-user", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public DemoUserInitializer(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.demo-user.email:demo@spb.local}") String email,
                               @Value("${app.demo-user.password:demo}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        userRepository.save(new User(email, passwordEncoder.encode(password), "Demo User"));
        log.info("Создан демонстрационный пользователь {}", email);
    }
}
