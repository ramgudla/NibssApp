package com.nibbs.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nibbs.domain.PaymentRequest;
import com.nibbs.domain.ScheduleRequest;
import com.nibbs.domain.ScheduleResponse;
import com.nibbs.exception.NibbsException;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ScheduleDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	 @Autowired
	 private ObjectMapper objectMapper;

	 private static final String PROCESSED = "Processed";

	 private static final String JSON_CONVERSION_FAILED = "JSON_RESPONSE_CONVERSION_FAILED";

	public static final String TMP_FOLDER = "tmp";
	public static final String FILE_SEPERATOR = System.getProperty("file.separator");
	public static final String LOCAL_DOWNLOAD_FOLDER = TMP_FOLDER +  FILE_SEPERATOR + "download" + FILE_SEPERATOR;

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

	public Path findAllPayments(String status) throws NibbsException {
		try {
			String sqlQuery = "select * from ASI_NIBSSPAY_PAYMENTS_TBL where SCHEDULE_ID is null";
			List<PaymentRequest> payments = jdbcTemplate.query(sqlQuery, this::mapRowToPaymentRecord);
			return extractToCsv(payments);
		} catch (DataAccessException dae) {
			log.error("Exception occurred while querying for the transactions. The error is: " + dae.getMessage(), dae);
			throw new NibbsException(dae);
		}
	}

	public static Path extractToCsv(List<PaymentRequest> payments) {
		String thisTime = Instant.now().toString();
		String local_path = LOCAL_DOWNLOAD_FOLDER + thisTime;
		Path path = Paths.get(local_path);
		path.getParent().toFile().mkdirs();
		try {
			//CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()));
			CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			String[] header="FIRSTNAME,LASTNAME,AGE".split(",");
			writer.writeNext(header);
			String data[] = new String[3];
			for (PaymentRequest payment: payments
			) {
				data[0] = payment.getDebitAccountNumber();
				data[1] = payment.getPaymentMode();
				data[2] = payment.getScheduleType();
				writer.writeNext(data);
			}
			writer.close();
			System.out.println("CSV file created succesfully.");
		} catch (Exception e) {
			System.out.println("exception :" + e.getMessage());
		}
		return path;
	}

	/**
	 * Updates the Transaction Response.
	 *
	 * @throws NibbsException
	 */
	public void updateScheduleRecord(ScheduleRequest scheduleRequest, ScheduleResponse scheduleResponse) throws NibbsException {
		try {
		String sqlQuery = "update ASI_NIBSSPAY_PAYMENTS_TBL set " + "SCHEDULE_ID = ? , LAST_UPDATE_DATE = ? " + "where DEBITACCOUNTNUMBER = ?";
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

	private PaymentRequest mapRowToPaymentRecord(ResultSet resultSet, int rowNum) throws SQLException {
		// This method is an implementation of the functional interface RowMapper.
		// It is used to map each row of a ResultSet to an object.
		PaymentRequest payment = PaymentRequest.builder()
				.title(resultSet.getString("TITLE"))
				.debitBankCode("998")
				.debitAccountNumber(resultSet.getString("DEBITACCOUNTNUMBER"))
				.debitDescription(resultSet.getString("DEBITDESCRIPTION"))
				.paymentMode(resultSet.getString("PAYMENTMODE"))
				.referenceNo(resultSet.getString("REFERENCENO"))
				.scheduleType(resultSet.getString("SCHEDULETYPE"))
				.build();

		return payment;
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
