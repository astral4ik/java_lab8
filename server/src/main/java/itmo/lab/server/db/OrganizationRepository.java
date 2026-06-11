package itmo.lab.server.db;

import itmo.lab.data.Address;
import itmo.lab.data.Location;
import itmo.lab.data.Organization;
import itmo.lab.server.storage.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Репозиторий для CRUD-операций с организациями в таблице organizations.
 */
public class OrganizationRepository {

    /**
     * Вставляет организацию в DB и возвращает сгенерированный ID.
     *
     * @param org организация для INSERT
     * @return сгенерированный ID
     */
    public long insert(Organization org) {
        String sql = """
            INSERT INTO organizations(full_name, annual_turnover, employees_count, street, loc_x, loc_y, loc_z)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (full_name) DO UPDATE SET
                annual_turnover = EXCLUDED.annual_turnover,
                employees_count = EXCLUDED.employees_count,
                street = EXCLUDED.street,
                loc_x = EXCLUDED.loc_x,
                loc_y = EXCLUDED.loc_y,
                loc_z = EXCLUDED.loc_z
            RETURNING id
            """;
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, org.getFullName());
            if (org.getAnnualTurnover() != null) {
                ps.setInt(2, org.getAnnualTurnover());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setInt(3, org.getEmployeesCount());
            Address addr = org.getOfficialAddress();
            ps.setString(4, addr.getStreet());
            if (addr.getTown() != null) {
                ps.setLong(5, addr.getTown().getX());
                ps.setObject(6, addr.getTown().getY(), Types.INTEGER);
                ps.setObject(7, addr.getTown().getZ(), Types.INTEGER);
            } else {
                ps.setNull(5, Types.BIGINT);
                ps.setNull(6, Types.INTEGER);
                ps.setNull(7, Types.INTEGER);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка INSERT организации", e);
        }
    }

    /**
     * Обновляет данные организации с указанным ID.
     *
     * @param orgId ID организации
     * @param org   новые данные организации
     */
    public void update(long orgId, Organization org) {
        String sql = """
            UPDATE organizations
            SET full_name=?, annual_turnover=?, employees_count=?, street=?, loc_x=?, loc_y=?, loc_z=?
            WHERE id=?
            """;
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, org.getFullName());
            if (org.getAnnualTurnover() != null) {
                ps.setInt(2, org.getAnnualTurnover());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setInt(3, org.getEmployeesCount());
            Address addr = org.getOfficialAddress();
            ps.setString(4, addr.getStreet());
            if (addr.getTown() != null) {
                ps.setLong(5, addr.getTown().getX());
                ps.setObject(6, addr.getTown().getY(), Types.INTEGER);
                ps.setObject(7, addr.getTown().getZ(), Types.INTEGER);
            } else {
                ps.setNull(5, Types.BIGINT);
                ps.setNull(6, Types.INTEGER);
                ps.setNull(7, Types.INTEGER);
            }
            ps.setLong(8, orgId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка UPDATE организации", e);
        }
    }

    /**
     * Удаляет организацию с указанным ID из DB.
     *
     * @param orgId ID организации
     */
    public void delete(long orgId) {
        String sql = "DELETE FROM organizations WHERE id=?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orgId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка DELETE организации", e);
        }
    }

    /**
     * Находит организацию по ID или возвращает {@code null}, если не найдена.
     *
     * @param orgId ID организации
     * @return найденная организация или {@code null}
     */
    public Organization findById(long orgId) {
        String sql = "SELECT * FROM organizations WHERE id=?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка поиска организации", e);
        }
    }

    /**
     * Преобразует текущую строку ResultSet в объект Organization.
     *
     * @param rs результирующий набор, установленный на нужную строку
     * @return заполненный объект Organization
     */
    public Organization mapRow(ResultSet rs) throws SQLException {
        Organization org = new Organization();
        org.setFullName(rs.getString("full_name"));
        org.setAnnualTurnover(rs.getObject("annual_turnover", Integer.class));
        org.setEmployeesCount(rs.getInt("employees_count"));

        String street = rs.getString("street");
        Address address = new Address();
        address.setStreet(street);
        long locX = rs.getLong("loc_x");
        if (!rs.wasNull()) {
            Location loc = new Location();
            loc.setX(locX);
            loc.setY(rs.getObject("loc_y", Integer.class));
            loc.setZ(rs.getObject("loc_z", Integer.class));
            address.setTown(loc);
        }
        org.setOfficialAddress(address);
        return org;
    }
}
