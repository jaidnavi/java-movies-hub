package ru.practicum.moviehub.store;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import ru.practicum.moviehub.exception.ValidateException;
import ru.practicum.moviehub.model.Movie;
import com.google.gson.Gson;
import java.util.stream.Collectors;

public class MoviesStore {

    private static HashMap<Long, Movie> movies;

    public MoviesStore() {
        movies = new HashMap<>();
    }

    public static long addMovie(String title, int year) throws ValidateException {
        Movie movie = new Movie(title, year);
        movies.put(movie.getId(), movie);
        return movie.getId();
    }

    public static void deleteMovie(long id) {
        movies.remove(id);
    }


    public static String getAllMoviesAsJson() {
        List<Movie> movieList = new ArrayList<>(movies.values());
        Gson gson = new Gson();
        return gson.toJson(movieList);
    }

    public static String getYearMoviesAsJson(int year) {
        List<Movie> filteredMovies = movies.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
        Gson gson = new Gson();
        return gson.toJson(filteredMovies);
    }

    public static String getMovieAsJson(long id) {
        Movie movie = movies.get(id);
        List<Movie> movieList = new ArrayList<>();
        movieList.add(movie);
        Gson gson = new Gson();
        return gson.toJson(movieList);
    }

    public static boolean movieIdIsExists(long id) {
        return movies.containsKey(id);
    }

    public static void clearMovies() {
        movies.clear();
    }

}