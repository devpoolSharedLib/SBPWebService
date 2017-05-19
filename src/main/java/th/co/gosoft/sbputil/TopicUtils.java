package th.co.gosoft.sbputil;

import java.util.ArrayList;
import java.util.List;

import th.co.gosoft.sbp.model.LastTopicModel;

public class TopicUtils {

    public static List<LastTopicModel> concatList(List<LastTopicModel> firstList, List<LastTopicModel> secondList){
        List<LastTopicModel> resultList = new ArrayList<>(firstList);
        resultList.addAll(secondList);
        return resultList;
    }
}
