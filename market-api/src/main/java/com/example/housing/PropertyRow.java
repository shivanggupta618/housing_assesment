package com.example.housing;

public record PropertyRow(double square_footage, int bedrooms, double bathrooms,
        int year_built, double lot_size, double distance_to_city_center,
        double school_rating, double price) {}
