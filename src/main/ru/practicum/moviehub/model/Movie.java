package ru.practicum.moviehub.model;

import java.util.ArrayList;
import java.util.Objects;

import ru.practicum.moviehub.exception.ValidateException;
import java.util.List;

import java.time.Year;

public class Movie {
    private static long sequence = 0;
    private String title;
    private int year;
    private long id;

    public Movie(String title, int year) throws ValidateException {

        List<String> validateExceptions = new ArrayList<>();

        if (title.isEmpty()) {
            validateExceptions.add("Название не должно быть пустым");
        }

        if (title.length() > 100) {
            validateExceptions.add("Название не должно превышать 100 символов");
        }

        if (year < 1888 || year > Year.now().getValue() + 1) {
            validateExceptions.add("Год должен быть между 1888 и " + (Year.now().getValue() + 1));
        }

        if (validateExceptions.size() > 0) {
            throw new ValidateException(validateExceptions, "Ошибка валидации");
        }

        this.title = title;
        this.year = year;
        this.id = ++sequence;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return id == movie.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", year=" + year +
                '}';
    }
}