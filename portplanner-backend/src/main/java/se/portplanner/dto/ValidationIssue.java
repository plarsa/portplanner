package se.portplanner.dto;

public record ValidationIssue(String type, Long placementId, String boatModel, String message) {}
