package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DataAccessException;

public class ConfigurationSqlDao implements ConfigurationDao {
    @Override
    public <T> void setConfiguration(String key, T value, Class<T> type) {
        try (var connection = SqlDb.getConnection()) {
            var statement = connection.prepareStatement("INSERT INTO configuration (key, value) VALUES (?, ?)");
            statement.setString(1, key);
            statement.setString(2, value.toString());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error setting configuration", e);
        }
    }

    @Override
    public <T> T getConfiguration(String key, T value, Class<T> type) {
        try (var connection = SqlDb.getConnection();
             var statement = connection.prepareStatement("SELECT value FROM configuration WHERE key = ?")) {
            statement.setString(1, key);
            var rs = statement.executeQuery();
            if (rs.next()) {
                return type.cast(rs.getString("value"));
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new DataAccessException("Error getting configuration", e);
        }
    }
}
