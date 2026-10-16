package com.reloop.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.reloop.config.AppProperties;
import com.reloop.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Public images go to Cloudinary when configured, otherwise to {upload-dir}/public (served at /uploads/**).
 * Private documents (receipts) are ALWAYS stored locally under {upload-dir}/private and only served through
 * an authenticated, owner-checked endpoint.
 */
@Service
public class StorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private final AppProperties props;
    private final Cloudinary cloudinary;

    public StorageService(AppProperties props) {
        this.props = props;
        var c = props.getCloudinary();
        this.cloudinary = c.enabled()
                ? new Cloudinary(ObjectUtils.asMap("cloud_name", c.getCloudName(), "api_key", c.getApiKey(),
                        "api_secret", c.getApiSecret(), "secure", true))
                : null;
        if (cloudinary == null) log.info("Cloudinary not configured: using local disk for public images");
    }

    public boolean cloudinaryEnabled() { return cloudinary != null; }

    /** Only URLs produced by this application (or bundled demo images) may be stored on records. */
    public boolean isTrustedImageUrl(String url) {
        return url != null && (url.startsWith("/uploads/") || url.startsWith("/images/")
                || url.startsWith("https://res.cloudinary.com/"));
    }

    public String requireTrusted(String url) {
        if (url == null || url.isBlank()) return null;
        if (!isTrustedImageUrl(url)) throw ApiException.badRequest("Image URL is not from an allowed source");
        return url;
    }

    public String storePublicImage(MultipartFile file) {
        byte[] bytes = read(file);
        String ext = detect(bytes);
        if (ext == null || ext.equals("pdf")) throw ApiException.badRequest("Only JPG, PNG or WEBP images are allowed");
        try {
            if (cloudinary != null) {
                Map<?, ?> res = cloudinary.uploader().upload(bytes, ObjectUtils.asMap("folder", "reloop", "resource_type", "image"));
                return String.valueOf(res.get("secure_url"));
            }
            Path dir = Path.of(props.getUploadDir(), "public").toAbsolutePath();
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "." + ext;
            Files.write(dir.resolve(name), bytes);
            return "/uploads/" + name;
        } catch (IOException e) {
            log.error("Image upload failed", e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not store image");
        }
    }

    /** Returns the stored file name (never a path). */
    public String storePrivate(MultipartFile file) {
        byte[] bytes = read(file);
        String ext = detect(bytes);
        if (ext == null) throw ApiException.badRequest("Only JPG, PNG, WEBP or PDF files are allowed");
        try {
            Path dir = Path.of(props.getUploadDir(), "private").toAbsolutePath();
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "." + ext;
            Files.write(dir.resolve(name), bytes);
            return name;
        } catch (IOException e) {
            log.error("Receipt upload failed", e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
    }

    public Resource loadPrivate(String name) {
        if (name == null || !name.matches("[a-f0-9\\-]{36}\\.(jpg|png|webp|pdf)")) throw ApiException.notFound("File");
        Path p = Path.of(props.getUploadDir(), "private", name).toAbsolutePath();
        if (!Files.exists(p)) throw ApiException.notFound("File");
        return new PathResource(p);
    }

    public static String contentTypeFor(String name) {
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private byte[] read(MultipartFile f) {
        if (f == null || f.isEmpty()) throw ApiException.badRequest("No file provided");
        try { return f.getBytes(); } catch (IOException e) { throw ApiException.badRequest("Could not read file"); }
    }

    /** Detects type from magic bytes (the client-supplied content type / file name is never trusted). */
    private String detect(byte[] b) {
        if (b.length < 12) return null;
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        if (b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "pdf";
        return null;
    }
}
