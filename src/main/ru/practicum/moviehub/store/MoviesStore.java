package ru.practicum.moviehub.store;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import ru.practicum.moviehub.model.Movie;
import com.google.gson.Gson;

public class MoviesStore {
    private static HashMap<Long, Movie> movies;

    public MoviesStore() {
        movies = new HashMap<>();
    }

    public static void addMovie(String title, int year) {
        Movie movie = new Movie(title, year);
        movies.put(movie.getId(), movie);
    }

    public void addMovie(Movie movie) {
        movies.put(movie.getId(), movie);
    }

    public Movie getMovie(long id) {
        return movies.get(id);
    }

    public void deleteMovie(long id) {
        movies.remove(id);
    }

    public static String getAllMoviesAsJson() {
        List<Movie> movieList = new ArrayList<>(movies.values());
        Gson gson = new Gson();
        return gson.toJson(movieList);
    }

    public static void clearMovies() {
        movies.clear();
    }


    public HashMap<Long, Movie> getAllMovies() {
        return movies;
    }

}