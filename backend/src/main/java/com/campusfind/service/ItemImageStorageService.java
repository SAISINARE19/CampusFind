package com.campusfind.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ItemImageStorageService {
  private static final long MAX_FILE_BYTES = 5 * 1024 * 1024;
  private final Path uploadDirectory;

  public ItemImageStorageService(@Value("${campusfind.upload-dir:uploads}") String uploadDirectory) {
    this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
  }

  public String store(MultipartFile image) throws IOException {
    if (image == null || image.isEmpty()) {
      return null;
    }
    if (image.getSize() > MAX_FILE_BYTES) {
      throw new IllegalArgumentException("Image must be 5 MB or smaller.");
    }

    String contentType = image.getContentType();
    String extension;
    if ("image/jpeg".equalsIgnoreCase(contentType)) {
      extension = ".jpg";
    } else if ("image/png".equalsIgnoreCase(contentType)) {
      extension = ".png";
    } else {
      throw new IllegalArgumentException("Choose a JPEG or PNG image.");
    }

    if (ImageIO.read(image.getInputStream()) == null) {
      throw new IllegalArgumentException("The uploaded file is not a valid image.");
    }

    Files.createDirectories(uploadDirectory);
    String filename = UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + extension;
    Path destination = uploadDirectory.resolve(filename).normalize();
    if (!destination.startsWith(uploadDirectory)) {
      throw new IllegalArgumentException("Invalid image path.");
    }
    image.transferTo(destination);
    return "/uploads/" + filename;
  }

  public void delete(String imageUrl) throws IOException {
    if (imageUrl == null || imageUrl.isBlank()) {
      return;
    }
    if (!imageUrl.startsWith("/uploads/")) {
      throw new IllegalArgumentException("Invalid stored image URL.");
    }
    Path imagePath = uploadDirectory.resolve(imageUrl.substring("/uploads/".length())).normalize();
    if (!imagePath.startsWith(uploadDirectory)) {
      throw new IllegalArgumentException("Invalid stored image path.");
    }
    Files.deleteIfExists(imagePath);
  }
}
