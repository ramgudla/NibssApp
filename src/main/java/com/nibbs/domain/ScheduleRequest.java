package com.nibbs.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
public class ScheduleRequest {
	@JsonProperty("title")
	private String title;

	@JsonProperty("debitBankCode")
	private String debitBankCode;

	@JsonProperty("debitAccountNumber")
	private String debitAccountNumber;

	@JsonProperty("debitDescription")
	private String debitDescription;

	@JsonProperty("paymentMode")
	private String paymentMode;

	@JsonProperty("referenceNo")
	private String referenceNo;

	@JsonProperty("scheduleType")
	private String scheduleType;

}
