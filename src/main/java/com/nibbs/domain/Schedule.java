package com.nibbs.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
@JsonInclude(Include.NON_NULL)
public class Schedule {
	// LFT fields
	@JsonProperty("BeneficiaryAccountNo")
	private String BeneficiaryAccountNo;
	@JsonProperty("RTGSCode")
	private String RTGSCode;
	@JsonProperty("BeneficiaryAccountNoCurrency")
	private String BeneficiaryAccountNoCurrency;
	@JsonProperty("Amount")
	private float Amount;
	@JsonProperty("Narration")
	private String Narration;
	@JsonProperty("BeneficiaryName")
	private String BeneficiaryName;
	@JsonProperty("PaymentRef")
	private String PaymentRef;
	
	// FFT fields
	@JsonProperty("SortCode")
	private String SortCode;
	@JsonProperty("BeneficiaryAddress")
	private String BeneficiaryAddress;
	@JsonProperty("BankName")
	private String BankName;
	//@JsonInclude(Include.ALWAYS)
	@JsonProperty("BankAddress")
	private String BankAddress;
	@JsonProperty("BankSWIFTCode")
	private String BankSWIFTCode;
}
