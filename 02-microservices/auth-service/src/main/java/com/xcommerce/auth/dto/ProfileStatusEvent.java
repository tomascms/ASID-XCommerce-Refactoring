package com.xcommerce.auth.dto;

public record ProfileStatusEvent(String username, String status, String reason) {}