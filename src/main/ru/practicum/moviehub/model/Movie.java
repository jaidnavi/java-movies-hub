package ru.practicum.moviehub.model;

import java.util.ArrayList;
import java.util.Objects;
import ru.practicum.moviehub.exception.ValidateException;
import java.util.List;
import java.time.Year;

public class Movie {

    private static long sequence = 0;
    private final String title;
    private int year;
    private final long id;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MIN_YEAR = 1888;
    private static final int MAX_YEAR = Year.now().getValue() + 1;

    public Movie(String title, int year) throws ValidateException {
        List<String> validateExceptions = new ArrayList<>();
        if (title.isEmpty()) {
            validateExceptions.add("Название не должно быть пустым");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            validateExceptions.add("Название не должно превышать " + MAX_TITLE_LENGTH + " символов");
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            validateExceptions.add("Год должен быть между " + MIN_YEAR + " и " + MAX_YEAR);
        }
        if (!validateExceptions.isEmpty()) {
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