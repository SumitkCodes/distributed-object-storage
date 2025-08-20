package com.example.storagenode.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Utility class for safe path operations.
 * Prevents path traversal attacks and ensures secure file access.
 */
public class PathUtil {
    
    // Pattern to detect potentially dangerous path components
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        ".*[<>:\"|?*].*|.*\\.\\..*|.*//.*"
    );
    
    /**
     * Safely resolves a relative path against a base directory.
     * Prevents path traversal attacks by ensuring the resolved path
     * remains within the base directory boundaries.
     * 
     * @param base the base directory path
     * @param relativePath the relative path to resolve
     * @return the resolved absolute path
     * @throws IllegalArgumentException if path traversal is detected
     */
    public static Path safeResolve(Path base, String relativePath) {
        if (base == null) {
            throw new IllegalArgumentException("Base path cannot be null");
        }
        
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Relative path cannot be null or empty");
        }
        
        // Check for dangerous path patterns
        if (DANGEROUS_PATTERN.matcher(relativePath).matches()) {
            throw new IllegalArgumentException("Path contains potentially dangerous characters or patterns: " + relativePath);
        }
        
        // Normalize the relative path to remove any ".." components
        Path normalizedRelative = Paths.get(relativePath).normalize();
        
        // Resolve against base and normalize the result
        Path resolvedPath = base.resolve(normalizedRelative).normalize();
        
        // Security check: ensure the resolved path is within the base directory
        if (!resolvedPath.startsWith(base)) {
            throw new IllegalArgumentException(
                "Path traversal attempt detected. Resolved path '" + resolvedPath + 
                "' is outside base directory '" + base + "'"
            );
        }
        
        return resolvedPath;
    }
    
    /**
     * Validates if a path string is safe for file operations.
     * 
     * @param path the path string to validate
     * @return true if the path is safe, false otherwise
     */
    public static boolean isPathSafe(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Try to resolve against a dummy base to check for dangerous patterns
            Path dummyBase = Paths.get("/tmp");
            safeResolve(dummyBase, path);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Gets the file extension from a path safely.
     * 
     * @param path the file path
     * @return the file extension (without dot) or empty string if no extension
     */
    public static String getFileExtension(Path path) {
        if (path == null) {
            return "";
        }
        
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }
}
