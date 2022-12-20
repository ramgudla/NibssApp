package com.nibss.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nibss.domain.PaymentRequest;
import com.nibss.domain.PaymentsResponse;
import com.nibss.domain.ScheduleRequest;
import com.nibss.domain.ScheduleResponse;
import com.nibss.exception.NibssException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * @throws NibssException
	 */
	public List<ScheduleRequest> findAllSchedules_dummy(String status) throws NibssException {
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
	 * @throws NibssException
	 */
	public List<ScheduleRequest> findAllSchedules(String status) throws NibssException {
		try {
			String sqlQuery = "select * from ASI_NIBSSPAY_PAYMENTS_TBL where SCHEDULE_ID is null";
		return jdbcTemplate.query(sqlQuery, this::mapRowToScheduleRecord);
		} catch (DataAccessException dae) {
			log.error("Exception occurred while querying for the transactions. The error is: " + dae.getMessage(), dae);
			throw new NibssException(dae);
		}
	}

	public Map<String, Path> findAllPayments(String status) throws NibssException {
		try {
			String sqlQuery = "select * from ASI_NP_UPLOAD_PAYMENT_FILE_TBL WHERE STATUS = ?";
			List<PaymentRequest> payments = jdbcTemplate.query(sqlQuery, new Object[] {status}, this::mapRowToPaymentRecord);
			return extractToCsv(payments);
		} catch (DataAccessException dae) {
			log.error("Exception occurred while querying for the transactions. The error is: " + dae.getMessage(), dae);
			throw new NibssException(dae);
		}
	}

	public Path findAllPayments_dymmy(String status) throws NibssException {
		try {
			String sqlQuery = "select * from ASI_NIBSSPAY_PAYMENTS_TBL where SCHEDULE_ID is null";
			PaymentRequest payment = PaymentRequest.builder()
					.serialNumber(1)
					.accountNumber("0000014575")
					.bank("998")
					.amount(123.34)
					.accountName("ram")
					.narration(123)
					.build();

			List<PaymentRequest> payments = new ArrayList<PaymentRequest>();
			payments.add(payment);
			return extractToCsv_old(payments);
		} catch (DataAccessException dae) {
			log.error("Exception occurred while querying for the transactions. The error is: " + dae.getMessage(), dae);
			throw new NibssException(dae);
		}
	}

	public static Path extractToCsv_old(List<PaymentRequest> payments) {
		String thisTime = Instant.now().toString();
		String local_path = LOCAL_DOWNLOAD_FOLDER + "test.csv";
		Path path = Paths.get(local_path);
		path.getParent().toFile().mkdirs();
		try {
			//CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()));
			CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			String[] header="SERIAL NUMBER,ACCOUNT NO,BANK,AMOUNT,ACCOUNT NAME,NARRATION".split(",");
			writer.writeNext(header);
			String data[] = new String[6];
			for (PaymentRequest payment : payments) {
				data[0] = String.valueOf(payment.getSerialNumber());
				data[1] = payment.getAccountNumber();
				data[2] = payment.getBank();
				data[3] = String.valueOf(payment.getAmount());
				data[4] = payment.getAccountName();
				data[5] = String.valueOf(payment.getNarration());
				writer.writeNext(data);
			}
			writer.close();
			log.info("CSV file {} created succesfully.", path.toAbsolutePath());
		} catch (Exception e) {
			System.out.println("exception :" + e.getMessage());
		}
		return path;
	}

	public static Map<String, Path> extractToCsv(List<PaymentRequest> payments) {
		Map<String, Path> scheduleIdPaymentsFileMap = new HashMap<>();
		try {
			//CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()));
			for (PaymentRequest payment : payments) {
				String data[] = new String[6];
				String scheduleId = payment.getScheduleId();
				data[0] = String.valueOf(payment.getSerialNumber());
				data[1] = payment.getAccountNumber();
				data[2] = payment.getBank();
				data[3] = String.valueOf(payment.getAmount());
				data[4] = payment.getAccountName();
				data[5] = String.valueOf(payment.getNarration());
				String local_path = LOCAL_DOWNLOAD_FOLDER + scheduleId + ".csv";
				Path path = Paths.get(local_path);
				path.getParent().toFile().mkdirs();
				CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
				String[] header="SERIAL NUMBER,ACCOUNT NO,BANK,AMOUNT,ACCOUNT NAME,NARRATION".split(",");
				writer.writeNext(header);
				writer.writeNext(data);
				writer.close();
				log.info("CSV file {} created succesfully.", path.toAbsolutePath());
				scheduleIdPaymentsFileMap.put(scheduleId, path);
			}

		} catch (Exception e) {
			log.info("Exception occurred while creating schedule payment files. The exception is :" + e);
		}
		return scheduleIdPaymentsFileMap;
	}

	/**
	 * Updates the Transaction Response.
	 *
	 * @throws NibssException
	 */
	public void updateScheduleRecord(ScheduleRequest scheduleRequest, ScheduleResponse scheduleResponse) throws NibssException {
		try {
		String sqlQuery = "update ASI_NIBSSPAY_PAYMENTS_TBL set " + "SCHEDULE_ID = ? , LAST_UPDATE_DATE = ? , RESPONSE_CODE = ?, STATUS = ? " + "where REFERENCENO = ?";
		jdbcTemplate.update(sqlQuery, scheduleResponse.getData().scheduleId, getCurrentTimestamp(), "SUCCESS", "PROCESSED", scheduleRequest.getReferenceNo());
		} catch (DataAccessException dae) {
			log.error("Exception occurred while updating the schedule response. The error is: " + dae.getMessage(), dae);
			throw new NibssException(dae);
		}
	}

	public void updateFailedScheduleRecord(ScheduleRequest scheduleRequest, String response) throws NibssException {
		try {
			String sqlQuery = "update ASI_NIBSSPAY_PAYMENTS_TBL set " + "RESPONSE_MSG = ? , LAST_UPDATE_DATE = ? , RESPONSE_CODE = ?, STATUS = ? " + "where REFERENCENO = ?";
			jdbcTemplate.update(sqlQuery, response, getCurrentTimestamp(), "REJECTED", "PROCESSED", scheduleRequest.getReferenceNo());
		} catch (DataAccessException dae) {
			log.error("Exception occurred while updating the schedule response. The error is: " + dae.getMessage(), dae);
			throw new NibssException(dae);
		}
	}

	public void updatePaymentsRecord(String scheduleId, String CLOSE_SCHEDULE_STATUS, String CLOSE_SCHEDULE_RESCODE, String CLOSE_SCHEDULE_RESMSG, PaymentsResponse paymentsResponse) throws NibssException {
		try {
			String sqlQuery = "update ASI_NP_UPLOAD_PAYMENT_FILE_TBL set " + "STATUS = ? , RESPONSE_CODE = ? , LAST_UPDATE_DATE = ? , CLOSE_SCHEDULE_STATUS = ? , CLOSE_SCHEDULE_RESCODE = ? , CLOSE_SCHEDULE_RESMSG = ? " + "where SCHEDULE_ID = ?";
			jdbcTemplate.update(sqlQuery, "PROCESSED", "SUCCESS", getCurrentTimestamp(), CLOSE_SCHEDULE_STATUS, CLOSE_SCHEDULE_RESCODE, CLOSE_SCHEDULE_RESMSG, scheduleId);
		} catch (DataAccessException dae) {
			log.error("Exception occurred while updating the schedule response. The error is: " + dae.getMessage(), dae);
			throw new NibssException(dae);
		}
	}

	public void updateFailedPaymentsRecord(String scheduleId, String response) throws NibssException {
		try {
			String sqlQuery = "update ASI_NP_UPLOAD_PAYMENT_FILE_TBL set " + "RESPONSE_MSG = ? , LAST_UPDATE_DATE = ? , RESPONSE_CODE = ?, STATUS = ? " + "where SCHEDULE_ID = ?";
			jdbcTemplate.update(sqlQuery, response, getCurrentTimestamp(), "REJECTED", "PROCESSED", scheduleId);
		} catch (DataAccessException dae) {
			log.error("Exception occurred while updating the schedule response. The error is: " + dae.getMessage(), dae);
			throw new NibssException(dae);
		}
	}

	private ScheduleRequest mapRowToScheduleRecord(ResultSet resultSet, int rowNum) throws SQLException {
		// This method is an implementation of the functional interface RowMapper.
		// It is used to map each row of a ResultSet to an object.
		ScheduleRequest schedule = ScheduleRequest.builder()
				.title(resultSet.getString("TITLE"))
				.debitBankCode(resultSet.getString("DEBITBANKCODE"))
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
				.scheduleId(resultSet.getString("SCHEDULE_ID"))
				.serialNumber(resultSet.getInt("SERIAL_NUMBER"))
				.accountNumber(resultSet.getString("ACCOUNT_NO"))
				.bank(resultSet.getString("BANK"))
				.amount(resultSet.getDouble("AMOUNT"))
				.accountName(resultSet.getString("ACCOUNT_NAME"))
				.narration(resultSet.getInt("NARRATION"))
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
