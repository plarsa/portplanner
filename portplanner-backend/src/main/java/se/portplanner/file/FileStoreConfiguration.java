package se.portplanner.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Picks the FileStore implementation named by {@code portplanner.file-store.type}. */
@Configuration
@EnableConfigurationProperties(FileStoreProperties.class)
public class FileStoreConfiguration {

    @Bean
    @ConditionalOnProperty(name = "portplanner.file-store.type", havingValue = "local", matchIfMissing = true)
    public FileStore localFileStore(FileStoreProperties props) {
        return new LocalFileStore(props.getLocal().getPath());
    }
}
