package com.xcommerce.user_service.dto;

public record ProfileStatusEvent(String username, String status, String reason) {}