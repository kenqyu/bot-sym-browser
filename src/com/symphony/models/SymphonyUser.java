package com.symphony.models;

/**
 * Created by ryan.dsouza on 7/26/16.
 */
public class SymphonyUser implements ISymphonyUser {

  private final Long userId;
  private final String emailAddress;
  private final String displayName;

  public SymphonyUser(Long userId, String emailAddress, String displayName) {
    this.userId = userId;
    this.emailAddress = emailAddress;
    this.displayName = displayName;
  }

  public Long getUserId() {
    return userId;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return "SymphonyUser{" +
        "userId=" + userId +
        ", emailAddress='" + emailAddress + '\'' +
        ", displayName='" + displayName + '\'' +
        '}';
  }
}