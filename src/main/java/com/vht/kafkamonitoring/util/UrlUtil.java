package com.vht.kafkamonitoring.util;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlUtil {

    /**
     * Trích xuất instance URL (protocol://host:port) từ chuỗi đầu vào.
     *
     * @param inputUrl Chuỗi đầu vào có thể chứa instance URL và đường dẫn bổ sung.
     * @return Instance URL dạng "protocol://host:port".
     * @throws IllegalArgumentException Nếu đầu vào không hợp lệ hoặc không thể phân tích.
     */
    public static String extractInstanceUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Input URL cannot be null or empty.");
        }

        try {
            // Tạo URI từ chuỗi đầu vào để phân tích cú pháp
            URI uri = new URI(inputUrl.trim());

            // Kiểm tra xem URI có chứa host không
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Invalid URL: Host is missing.");
            }

            // Xác định protocol
            String protocol = uri.getScheme();
            if (protocol == null || protocol.isEmpty()) {
                throw new IllegalArgumentException("Invalid URL: Protocol is missing.");
            }

            // Xây dựng instance URL dạng "protocol://host:port"
            StringBuilder instanceUrl = new StringBuilder(protocol + "://" + host);
            if (port != -1) {
                instanceUrl.append(":").append(port);
            }

            return instanceUrl.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + inputUrl, e);
        }
    }

}