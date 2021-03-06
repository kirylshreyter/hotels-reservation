package com.kirylshreyter.hotel.daodb.impl;

import com.kirylshreyter.hotel.commons.AvailableRoom;
import com.kirylshreyter.hotel.commons.NotAvailableRoom;
import com.kirylshreyter.hotel.commons.RoomWithAdditionalInfo;
import com.kirylshreyter.hotel.daoapi.IRoomDao;
import com.kirylshreyter.hotel.daodb.mapper.AvailableRoomMapper;
import com.kirylshreyter.hotel.daodb.mapper.NotAvailableRoomMapper;
import com.kirylshreyter.hotel.daodb.mapper.RoomWithAdditionalInfoMapper;
import com.kirylshreyter.hotel.daodb.util.DateConverter;
import com.kirylshreyter.hotel.daodb.util.NotNullChecker;
import com.kirylshreyter.hotel.datamodel.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

@Repository
public class RoomDaoDbImpl implements IRoomDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomDaoDbImpl.class);

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private NotNullChecker notNullChecker;

    @Inject
    private DateConverter dateConverter;

    @Override
    public Long create(Room entity) {
        LOGGER.info("Trying to create room in table room ...");
        if (notNullChecker.RoomNotNullChecker(entity)) {
            final String INSERT_SQL = "INSERT INTO room (number, room_details_id, status) VALUES (?,?,?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(INSERT_SQL, new String[]{"id"});
                    ps.setString(1, entity.getNumber());
                    ps.setLong(2, entity.getRoomDetailsId());
                    ps.setString(3, entity.getStatus());
                    return ps;
                }
            }, keyHolder);
            ;
            entity.setId(keyHolder.getKey().longValue());
            Long insertedId = entity.getId();
            LOGGER.info("Room was created, id = {}", insertedId);
            return insertedId;
        } else {
            return null;
        }
    }

    @Override
    public Room read(Long id) {
        return null;
    }

    @Override
    public Boolean update(Room entity) {

        LOGGER.info("Trying to update room with id = {} in table room.", entity.getId());
        if (notNullChecker.RoomNotNullChecker(entity)) {
            jdbcTemplate.update("UPDATE room SET number = ?, room_details_id = ?, status = ? where id = ?",
                    entity.getNumber(), entity.getRoomDetailsId(), entity.getStatus(), entity.getId());
            LOGGER.info("Room was updated, id = {}", entity.getId());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Integer delete(Long id) {
        return null;
    }

    @Override
    public List<Room> getAll() {
        return null;
    }

    @Override
    public RoomWithAdditionalInfo getWithAdditionalInfo(Long id) {
        RoomWithAdditionalInfo roomWithAdditionalInfo = new RoomWithAdditionalInfo();
        try {
            roomWithAdditionalInfo = jdbcTemplate.queryForObject(
                    "SELECT r.id,r.number,r.status,rd.room_type,rd.number_of_places,rd.cost_per_night,rd.additional_information FROM room r JOIN room_details rd ON (r.room_details_id=r.id) WHERE r.id=?",
                    new Object[]{id}, new RoomWithAdditionalInfoMapper());
            return roomWithAdditionalInfo;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (CannotGetJdbcConnectionException e) {
            return null;
        }
    }

    @Override
    public List<NotAvailableRoom> getAllNotAvailableRoom(Date arrivalDate, Date departureDate) {

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT room_id FROM booking_request WHERE ((arrival_date = '");
        sb.append(dateConverter.javaUtilDateToJavaSqlDateConverter(departureDate));
        sb.append("') OR (arrival_date<'");
        sb.append(dateConverter.javaUtilDateToJavaSqlDateConverter(departureDate));
        sb.append("')) AND ((departure_date='");
        sb.append(dateConverter.javaUtilDateToJavaSqlDateConverter(arrivalDate));
        sb.append("') OR (departure_date>'");
        sb.append(dateConverter.javaUtilDateToJavaSqlDateConverter(arrivalDate));
        sb.append("'))");
        return jdbcTemplate.query(sb.toString(), new NotAvailableRoomMapper());
    }

    @Override
    public List<AvailableRoom> getAllAvailableRoom(Date arrivalDate, Date departureDate, Integer numberOfPlaces) {
        List<NotAvailableRoom> listOfNotAvailableRoom = new ArrayList<NotAvailableRoom>(
                getAllNotAvailableRoom(arrivalDate, departureDate));
        ListIterator<NotAvailableRoom> a = listOfNotAvailableRoom.listIterator();
        StringBuilder sb = new StringBuilder();
        sb.append(
                "SELECT r.id as room_id,r.number,rd.room_type,rd.cost_per_night,rd.additional_information FROM room r JOIN room_details rd ON (r.room_details_id=rd.id) WHERE status='available' AND rd.number_of_places='");
        sb.append(numberOfPlaces);
        sb.append("'");
        try {
            while (a.hasNext() == true) {
                sb.append(" AND r.id<>'");
                int i = a.nextIndex();
                sb.append(listOfNotAvailableRoom.get(i).getRoomId());
                sb.append("'");
                a.next();
            }
        } catch (Exception e) {
            sb.append(";");
        } finally {
            sb.append(";");
        }
        List<AvailableRoom> listAvailableRoom = new ArrayList<AvailableRoom>();
        try {
            listAvailableRoom = jdbcTemplate.query(sb.toString(), new AvailableRoomMapper());
        } catch (Exception e) {
            return null;
        }
        if (listAvailableRoom.isEmpty()) {
            return null;
        } else {
            return listAvailableRoom;
        }
    }
}
