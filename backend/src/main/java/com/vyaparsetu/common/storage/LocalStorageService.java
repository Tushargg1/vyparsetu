package com.vyaparsetu.common.storage;

import com.vyaparsetu.common.config.AppProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(AppProperties props) {
        this.root = Paths.get(props.getStorage().getLocalPath()).toAbsolutePath().normalize();
    }

    @Override
    public String store(String key, String content, String contentType) {
        try {
            Path target = root.resolve(key).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("Invalid storage key");
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return "/files/" + key;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String read(String key) {
        try {
            Path target = root.resolve(key).normalize();
            // SECURITY: prevent path traversal outside the storage root.
            if (!target.startsWith(root) || !Files.exists(target)) {
                return null;
            }
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
