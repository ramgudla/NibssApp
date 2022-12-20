package com.nibss.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
public class PaymentRequest {

	@JsonProperty("scheduleId")
	private String scheduleId;
	@JsonProperty("serialNumber")
	private int serialNumber;

	@JsonProperty("accountNumber")
	private String accountNumber;

	@JsonProperty("bank")
	private String bank;

	@JsonProperty("amount")
	private double amount;

	@JsonProperty("accountName")
	private String accountName;

	@JsonProperty("narration")
	private int narration;
}
