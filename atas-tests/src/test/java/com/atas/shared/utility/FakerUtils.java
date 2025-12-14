package com.atas.shared.utility;

import com.github.javafaker.Faker;

public class FakerUtils {

  private FakerUtils() {}

  private static final Faker faker = new Faker();

  public static Faker getFaker() {
    return faker;
  }

  public static String generateCompanyName() {
    return faker.company().name();
  }

  public static String generateCompanySlug() {
    return faker.company().name().toLowerCase().replaceAll("[^a-z0-9]", "-");
  }

  public static String generateCompanyCatchPhrase() {
    return faker.company().catchPhrase();
  }

  public static String generateCountryCode() {
    return faker.address().countryCode();
  }

  public static String generateStreetAddress() {
    return faker.address().streetAddress();
  }

  public static String generateFullAddress() {
    return faker.address().fullAddress();
  }

  public static String generateTeamName() {
    return faker.team().name();
  }

  public static String generateTeamSlug() {
    return faker.team().name().toLowerCase().replaceAll("[^a-z0-9]", "-");
  }

  public static String generateSentence() {
    return faker.lorem().sentence();
  }

  public static String generateRestaurantName() {
    return faker.food().dish() + " Restaurant";
  }

  public static String generateRestaurantSlug() {
    return faker.food().dish().toLowerCase().replaceAll("[^a-z0-9]", "-") + "-restaurant";
  }

  public static String generateRestaurantType() {
    return faker.food().dish() + " Cuisine";
  }
}
