package com.nibss.domain;

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
public class ScheduleResponse {
	@JsonProperty("success")
	private String success;
	@JsonProperty("message")
	private String message;
	@JsonProperty("data")
	private Body data;

	@Data
	@Builder
	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Body {
		public int scheduleId;

		public String referenceNo;

		public String scheduleIdExt;
	}
}


