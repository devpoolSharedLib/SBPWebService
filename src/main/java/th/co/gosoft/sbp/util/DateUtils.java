package th.co.gosoft.sbp.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import th.co.gosoft.sbp.model.LastTopicModel;
import th.co.gosoft.sbp.model.LikeModel;
import th.co.gosoft.sbp.model.ReadModel;

public class DateUtils {

    public static DateFormat dbFormat = createSimpleDateFormat("yyyy/MM/dd HH:mm:ss", "GMT+7");
    public static DateFormat clientFormat = createSimpleDateFormat("dd/MM/yyyy HH:mm:ss", "GMT+7");
    
    public static List<LastTopicModel> formatDBDateToClientDate(List<LastTopicModel> lastTopicModelList) {
        List<LastTopicModel> resultList = new ArrayList<LastTopicModel>();
        for (LastTopicModel lastTopicModel : lastTopicModelList) {
            LastTopicModel resultModel = lastTopicModel;
            resultModel.setDate(clientFormat.format(parseStringToDate(lastTopicModel.getDate())));
            resultList.add(resultModel);
        }
        return resultList;
    }
    
    public static List<ReadModel> formatDBDateToClientDateForReadModel(List<ReadModel> readModelList) {
        List<ReadModel> resultList = new ArrayList<ReadModel>();
        for (ReadModel readModel : readModelList) {
            ReadModel resultModel = readModel;
            resultModel.setDate(clientFormat.format(parseStringToDate(readModel.getDate())));
            resultList.add(resultModel);
        }
        return resultList;
    }
    
    public static List<LikeModel> formatDBDateToClientDateForLikeModel(List<LikeModel> likeModelList) {
        List<LikeModel> resultList = new ArrayList<LikeModel>();
        for (LikeModel likeModel : likeModelList) {
            LikeModel resultModel = likeModel;
            resultModel.setDate(clientFormat.format(parseStringToDate(likeModel.getDate())));
            resultList.add(resultModel);
        }
        return resultList;
    }
    
    private static Date parseStringToDate(String dateString){
        try {
            return dbFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    private static DateFormat createSimpleDateFormat(String formatString, String timeZone) {
        DateFormat dateFormat = new SimpleDateFormat(formatString, Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        return dateFormat;
    }
    
    public static boolean isNextDay(String dateInDBString, String currentDateString) throws ParseException {
        Calendar dateInDBCalendar = Calendar.getInstance();
        Calendar currentCalendar = Calendar.getInstance();
        dateInDBCalendar.setTime(DateUtils.dbFormat.parse(dateInDBString));
        currentCalendar.setTime(DateUtils.dbFormat.parse(currentDateString));
        return (dateInDBCalendar.get(Calendar.YEAR) <= currentCalendar.get(Calendar.YEAR)) && (dateInDBCalendar.get(Calendar.DAY_OF_YEAR) < currentCalendar.get(Calendar.DAY_OF_YEAR));
    }
    
    public static boolean isAfterDate(String firstDateString, String secondDateString) {
        try {
            Date firstDate = DateUtils.dbFormat.parse(firstDateString);
            Date secondDate = DateUtils.dbFormat.parse(secondDateString);
            if (secondDate.after(firstDate)) {
                return true;
            } else {
                return false;
            }
        } catch (ParseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


}
