package com.nibbs.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.nibbs.domain.Schedule;
import com.nibbs.domain.ScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nibbs.domain.ScheduleResponse;
import com.nibbs.exception.NibbsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class ScheduleDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	 @Autowired
	 private ObjectMapper objectMapper;

	 private static final String PROCESSED = "Processed";

	 private static final String JSON_CONVERSION_FAILED = "JSON_RESPONSE_CONVERSION_FAILED";

	 private static Logger log = LoggerFactory.getLogger(ScheduleDAO.class);

	/**
	 * Retrieve all the transaction records
	 *
	 * @return
	 * @throws NibbsException
	 */
	public List<ScheduleRequest> findAllSchedules_dummy(String status) throws NibbsException {
		ScheduleRequest schedule = ScheduleRequest.builder()
				.title("Test0221Jul2022")
				.debitBankCode("998")
				.debitAccountNumber("0000014575")
				.debitDescription("Test")
				.paymentMode("Any")
				.referenceNo("121232324242426666666")
				.scheduleType("None")
				.build();

		List<ScheduleRequest> list = new ArrayList<ScheduleRequest>();
		list.add(schedule);

		return list;
	}

	/**
	 * Retrieve all the transaction records
	 *
	 * @return
	 * @throws NibbsException
	 */
	public List<ScheduleRequest> findAllSchedules(String status) throws NibbsException {
		try {
			String sqlQuery = "select * from ASI_NIBSSPAY_PAYMENTS_TBL where SCHEDULE_ID is null";
		return jdbcTemplate.query(sqlQuery, this::mapRowToScheduleRecord);
		} catch (DataAccessException dae) {
			log.error("Exception occurred while querying for the transactions. The error is: " + dae.getMessage(), dae);
			throw new NibbsException(dae);
		}
	}

	/**
	 * Updates the Transaction Response.
	 *
	 * @throws NibbsException
	 */
	public void updateScheduleRecord(ScheduleRequest scheduleRequest, ScheduleResponse scheduleResponse) throws NibbsException {
		try {
		String sqlQuery = "update ASI_NIBSSPAY_PAYMENTS_TBL set " + "SCHEDULE_ID = ? , LAST_UPDATION_DATE = ? " + "where DEBITACCOUNTNUMBER = ?";
		String responseJson =  objToJson(scheduleResponse);
		jdbcTemplate.update(sqlQuery, scheduleResponse.getData().scheduleId, getCurrentTimestamp(), scheduleRequest.getDebitAccountNumber());
		} catch (DataAccessException dae) {
			log.error("Exception occurred while updating the schedule response. The error is: " + dae.getMessage(), dae);
			throw new NibbsException(dae);
		}
	}

	private ScheduleRequest mapRowToScheduleRecord(ResultSet resultSet, int rowNum) throws SQLException {
		// This method is an implementation of the functional interface RowMapper.
		// It is used to map each row of a ResultSet to an object.
		ScheduleRequest schedule = ScheduleRequest.builder()
				.title(resultSet.getString("TITLE"))
				.debitBankCode("998")
				.debitAccountNumber(resultSet.getString("DEBITACCOUNTNUMBER"))
				.debitDescription(resultSet.getString("DEBITDESCRIPTION"))
				.paymentMode(resultSet.getString("PAYMENTMODE"))
				.referenceNo(resultSet.getString("REFERENCENO"))
				.scheduleType(resultSet.getString("SCHEDULETYPE"))
				.build();

		return schedule;
	}

	private String objToJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.debug("failed conversion: object to Json", e);
            return JSON_CONVERSION_FAILED;
        }
	}

	private Timestamp getCurrentTimestamp() {
		return Timestamp.valueOf(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())
				.withZoneSameInstant(ZoneId.of("UTC+3")).toLocalDateTime());
	}

}
