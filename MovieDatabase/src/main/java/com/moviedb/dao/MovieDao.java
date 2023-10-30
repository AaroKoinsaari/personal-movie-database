package com.moviedb.dao;

import java.io.*;
import java.sql.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

import com.moviedb.models.Movie;


/**
 * Data Access Object for the Movie class.
 */
public class MovieDao {

    /** Connection used to execute SQL queries and interact with the database. */
    private Connection connection;

    /** The URL pointing to the SQLite database location. */
    private static final String DB_URL = "jdbc:sqlite:database/moviedatabase.db";


    /**
     * Default constructor that initializes the connection to the default SQLite database.
     */
    public MovieDao() {
        this(DB_URL);
    }


    /**
     * Constructor that accepts a specific database URL.
     *
     * @param dbUrl The URL to the SQLite database.
     */
    public MovieDao(String dbUrl) {
        try {
            connection = DriverManager.getConnection(dbUrl);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates and adds a movie to the SQLite database.
     *
     * @param movie The movie to be added.
     */
    public void create(Movie movie) {
        String sql = "INSERT INTO movies(title, release_year, director) VALUES(?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, movie.getTitle());
            pstmt.setInt(2, movie.getReleaseYear());
            pstmt.setString(3, movie.getDirector());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Fetches a list of either actor or genre IDs based on a specified movie ID and table name.
     *
     * @param movieId The ID of the movie for which the actor or genre IDs are to be fetched.
     * @param tableName The name of the table (either "actors" or "genres") to fetch IDs from.
     * @return A List of Integers representing actor or genre IDs associated with the given movie ID.
     * @throws SQLException If there's an error during the database operation.
     */
    private List<Integer> fetchAssociatedIds(int movieId, String tableName, String columnName) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT" + columnName + "FROM" + tableName + "WHERE movie_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ids.add(rs.getInt(columnName));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }


    /**
     * Retrieves a movie based on its ID.
     *
     * @param id The unique identifier of the movie to be fetched.
     * @return The Movie object if found, null otherwise.
     * @throws SQLException If there's an error during the database operation.
     */
    public Movie read(int id) throws IOException {
        String sql = "SELECT title, release_year, director FROM movies WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            // If result is found, convert it to a Movie object
            if (rs.next()) {
                String title = rs.getString("title");
                int releaseYear = rs.getInt("release_year");
                String director = rs.getString("director");

                // Fetch the list of actors and genres
                List<Integer> actorIds = fetchAssociatedIds(id, "movie_actors", "actor_id");
                List<Integer> genreIds = fetchAssociatedIds(id, "movie_genres", "genre_id");

                return new Movie(id, title, releaseYear, director, actorIds, genreIds);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Updates the details of a specified movie in the SQLite database.
     * If associated actors or genres are modified, the relevant links in the database are updated accordingly.
     *
     * @param updatedMovie The movie object containing updated information.
     * @throws SQLException If there's an error during the database operation.
     *
     * TODO:
     * - Modify deleting and inserting into their own methods
     * - Improve efficiency by checking first if dependencies need updating at all
     */
    public void update(Movie updatedMovie) {
        // Update movie information
        String sqlUpdateMovie = "UPDATE movies SET title = ?, release_year = ?, director = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlUpdateMovie)) {
            pstmt.setString(1, updatedMovie.getTitle());
            pstmt.setInt(2, updatedMovie.getReleaseYear());
            pstmt.setString(3, updatedMovie.getDirector());
            pstmt.setInt(4, updatedMovie.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Delete old genre dependencies
        String sqlDeleteGenre = "DELETE FROM movie_genres WHERE movie_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteGenre)) {
            pstmt.setInt(1, updatedMovie.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Add new genre dependencies
        for (Integer genreId : updatedMovie.getGenreIds()) {
            String sqlInsertGenre = "INSERT INTO movie_genres(movie_id, genre_id) VALUES(?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlInsertGenre)) {
                pstmt.setInt(1, updatedMovie.getId());
                pstmt.setInt(2, genreId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Delete old actor dependencies
        String sqlDeleteActor = "DELETE FROM movie_actors WHERE movie_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteActor)) {
            pstmt.setInt(1, updatedMovie.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Add new actor dependencies
        for (Integer actorId : updatedMovie.getActorIds()) {
            String sqlInsertActor = "INSERT INTO movie_actors(movie_id, actor_id) VALUES(?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlInsertActor)) {
                pstmt.setInt(1, updatedMovie.getId());
                pstmt.setInt(2, actorId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public void delete(Movie deletedMovie) {
        String sql = "DELETE FROM movies WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deletedMovie.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO:
     *  - delete method
     *  - search methods
     *  - sorting methods
     *  - find methods
     */
}