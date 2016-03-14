package org.robolectric.shadows;

import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.os.Build;
import android.text.format.DateUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.TestRunners;
import org.robolectric.annotation.Config;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import libcore.icu.DateIntervalFormat;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestRunners.MultiApiWithDefaults.class)
@Config(sdk = {
    Build.VERSION_CODES.M })
public class ShadowDateIntervalFormatTest {
  @Test
  public void testDateInterval_FormatDateRange() throws ParseException {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.YEAR, 2013);
    calendar.set(Calendar.MONTH, Calendar.JANUARY);
    calendar.set(Calendar.DAY_OF_MONTH, 20);

    long timeInMillis = calendar.getTimeInMillis();
    String actual = DateIntervalFormat.formatDateRange(ULocale.getDefault(), TimeZone.getDefault(), timeInMillis, timeInMillis, DateUtils.FORMAT_NUMERIC_DATE);

    DateFormat format = new SimpleDateFormat("MM/dd/yyyy", ULocale.getDefault());
    Date date = format.parse(actual);

    assertThat(date).isNotNull();
  }
}
