package ru.practicum.moviehub.store;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import ru.practicum.moviehub.model.Movie;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class MoviesStore {
    private final HashMap<Long, Movie> movies;

    public MoviesStore() {
        movies = new HashMap<>();
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

    public String getAllMoviesAsJson() {
        List<Movie> movieList = new ArrayList<>(movies.values());
        Gson gson = new Gson();
        return gson.toJson(movieList);
    }


    public HashMap<Long, Movie> getAllMovies() {
        return movies;
    }

}