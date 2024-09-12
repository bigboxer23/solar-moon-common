package com.bigboxer23.solar_moon.search;

/** */
public interface OpenSearchConstants {
	String TIMESTAMP = "@timestamp";
	String INDEX_NAME = "generation-meter";

	String TIME_SERIES_SEARCH_TYPE = "timeseries";

	String TIME_SERIES_WITH_ERRORS_SEARCH_TYPE = "timeseriesWithErrors";

	String TIME_SERIES_MAX_SEARCH_TYPE = "timeSeriesMax";
	String AVG_TOTAL_SEARCH_TYPE = "avgTotal";

	String AVG_SEARCH_TYPE = "avg";

	String TOTAL_SEARCH_TYPE = "total";

	String MAX_CURRENT_SEARCH_TYPE = "maxCurrent";

	String MAX_ENERGY_CONSUMED_SEARCH_TYPE = "maxEnergyConsumed";

	String STACKED_TIME_SERIES_SEARCH_TYPE = "stackedTimeSeries";
	String GROUPED_BAR_SEARCH_TYPE = "groupedBarGraph";

	String DATA_SEARCH_TYPE = "data";

	String TOTAL_ENERGY_CONSUMED_SEARCH_TYPE = "totalEnergyConsumed";
}
