package mate.academy.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mate.academy.dao.BookDao;
import mate.academy.exception.DataProcessingException;
import mate.academy.lib.Dao;
import mate.academy.model.Book;
import mate.academy.util.db.ConnectionUtil;

@Dao
public class BookDaoImpl implements BookDao {
    private static final String CANT_CREATE = "Can't store the book to the DB";
    private static final String CANT_FIND_BY_ID = "Can't find the book with id = ";
    private static final String CANT_FIND_ALL = "Can't find all books";
    private static final String CANT_UPDATE = "Can't update the book with id = ";
    private static final String CANT_DELETE = "Can't delete the book with id = ";

    @Override
    public Book create(Book book) {
        String insertQuery = "INSERT INTO books (title,price) VALUES (?,?);";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, book.getTitle());
            statement.setBigDecimal(2, book.getPrice());
            statement.executeUpdate();
            ResultSet generatedKey = statement.getGeneratedKeys();
            if (generatedKey.next()) {
                Long id = generatedKey.getObject(1, Long.class);
                book.setId(id);
            }
        } catch (SQLException e) {
            throw new DataProcessingException(CANT_CREATE, e);
        }
        return book;
    }

    @Override
    public Optional<Book> findById(Long id) {
        String getByIdQuery = "SELECT * FROM books WHERE id = ? AND is_deleted = 0";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement = connection.prepareStatement(getByIdQuery)) {
            statement.setLong(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(createBook(resultSet));
            }
        } catch (SQLException e) {
            throw new DataProcessingException(CANT_FIND_BY_ID + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Book> findAll() {
        String getAllQuery = "SELECT * FROM books WHERE is_deleted = 0";
        List<Book> books = new ArrayList<>();
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement = connection.prepareStatement(getAllQuery)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                books.add(createBook(resultSet));
            }
        } catch (SQLException e) {
            throw new DataProcessingException(CANT_FIND_ALL, e);
        }
        return books;
    }

    @Override
    public Book update(Book book) {
        String updateQuery = "UPDATE books SET title = ?, price = ? "
                + "WHERE id = ? AND is_deleted = 0";
        Long id = book.getId();
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement = connection.prepareStatement(updateQuery)) {
            statement.setString(1, book.getTitle());
            statement.setBigDecimal(2, book.getPrice());
            statement.setLong(3, id);
            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                return book;
            }
        } catch (SQLException e) {
            throw new DataProcessingException(CANT_UPDATE + id, e);
        }
        throw new DataProcessingException(CANT_FIND_BY_ID + id);
    }

    @Override
    public boolean deleteById(Long id) {
        String deletedQuery = "UPDATE books SET is_deleted = 1 WHERE id = ?";
        try (Connection connection = ConnectionUtil.getConnection();
                PreparedStatement statement = connection.prepareStatement(deletedQuery)) {
            statement.setLong(1, id);
            return statement.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new DataProcessingException(CANT_DELETE + id, e);
        }
    }

    private Book createBook(ResultSet resultSet) throws SQLException {
        return new Book(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getBigDecimal("price"));
    }
}
