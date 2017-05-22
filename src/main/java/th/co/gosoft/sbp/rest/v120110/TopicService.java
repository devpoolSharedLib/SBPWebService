package th.co.gosoft.sbp.rest.v120110;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.IndexField.SortOrder;

import th.co.gosoft.sbp.model.AccessAppModel;
import th.co.gosoft.sbp.model.LastLikeModel;
import th.co.gosoft.sbp.model.LastTopicModel;
import th.co.gosoft.sbp.model.LogDeleteModel;
import th.co.gosoft.sbp.model.ReadModel;
import th.co.gosoft.sbp.model.ReadRoomModel;
import th.co.gosoft.sbp.model.RoomModel;
import th.co.gosoft.sbp.model.RoomNotificationModel;
import th.co.gosoft.sbp.util.CloudantClientUtils;
import th.co.gosoft.sbp.util.ConcatDomainUtils;
import th.co.gosoft.sbp.util.DateUtils;
import th.co.gosoft.sbp.util.PushNotificationUtils;
import th.co.gosoft.sbp.util.TopicUtils;

@Path("v120110/topic")
public class TopicService {

	private static final String NOTIFICATION_MESSAGE = "You have new topic.";
	private static Database db = CloudantClientUtils.getDBNewInstance();
	private String stampDate;

	@POST
	@Path("/post")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response createTopic(LastTopicModel lastTopicModel) {
		System.out.println(">>>>>>>>>>>>>>>>>>> topicModel()");
		System.out.println("topic subject : " + lastTopicModel.getSubject());
		System.out.println("topic content : " + lastTopicModel.getContent());
		System.out.println("topic type : " + lastTopicModel.getType());
		lastTopicModel.setContent(ConcatDomainUtils.deleteDomainImagePath(lastTopicModel.getContent()));
		stampDate = DateUtils.dbFormat.format(new Date());
		System.out.println("StampDate : " + stampDate);
		com.cloudant.client.api.model.Response response = null;
		if (lastTopicModel.getType().equals("host")) {
			lastTopicModel.setDate(stampDate);
			lastTopicModel.setUpdateDate(stampDate);
			response = db.save(lastTopicModel);
			lastTopicModel.set_id(response.getId());
			lastTopicModel.set_rev(response.getRev());
			updateTotalTopicInRoomModel(lastTopicModel.getRoomId());
			increaseReadCount(lastTopicModel, lastTopicModel.getEmpEmail());
			PushNotificationUtils.sendMessagePushNotification(NOTIFICATION_MESSAGE);
		} else if (lastTopicModel.getType().equals("comment")) {
			lastTopicModel.setDate(stampDate);
			response = db.save(lastTopicModel);
			LastTopicModel hostTopicModel = db.find(LastTopicModel.class, lastTopicModel.getTopicId());
			hostTopicModel.setUpdateDate(stampDate);
			db.update(hostTopicModel);
		}
		String result = response.getId();
		System.out.println(">>>>>>>>>>>>>>>>>>> post result id : " + result);
		System.out.println("POST Complete");
		return Response.status(201).entity(result).build();
	}

	@POST
	@Path("/deleteObj")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response deleteObj(LastTopicModel lastTopicModel) {
		System.out.println("deleteObj() _id : " + lastTopicModel.get_id());
		db.remove(lastTopicModel);
		System.out.println("DELETE " + lastTopicModel.getEmpEmail() + " Complete");
		if (lastTopicModel.getType().equals("host")) {
			updateTotalDeleteInRoomModel(lastTopicModel.getRoomId());
			deleteAllInTopic(lastTopicModel.get_id(), lastTopicModel.getEmpEmail());
		}
		insertLogDelete(lastTopicModel, lastTopicModel.getEmpEmail());
		return Response.status(201).build();
	}

	@POST
	@Path("/newLike")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response newLike(LastLikeModel lastLikeModel) {
		System.out.println("newLike() topic id : " + lastLikeModel.getTopicId());
		stampDate = DateUtils.dbFormat.format(new Date());
		System.out.println("StampDate : " + stampDate);
		LastTopicModel lastTopicModel = db.find(LastTopicModel.class, lastLikeModel.getTopicId());
		lastTopicModel.setCountLike(lastTopicModel.getCountLike() == null ? 1 : lastTopicModel.getCountLike() + 1);
		System.out.println("count like : " + lastTopicModel.getCountLike());
		db.update(lastTopicModel);
		lastLikeModel.setDate(stampDate);
		db.save(lastLikeModel);
		System.out.println("POST Complete");
		return Response.status(201).build();
	}

	@PUT
	@Path("/updateLike")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response updateLike(LastLikeModel lastLikeModel) {
		System.out.println("updateLike() topic id : " + lastLikeModel.getTopicId());
		stampDate = DateUtils.dbFormat.format(new Date());
		System.out.println("StampDate : " + stampDate);
		LastTopicModel lastTopicModel = db.find(LastTopicModel.class, lastLikeModel.getTopicId());
		lastTopicModel.setCountLike(lastTopicModel.getCountLike() + 1);
		db.update(lastTopicModel);
		lastLikeModel.setDate(stampDate);
		db.update(lastLikeModel);
		System.out.println("POST Complete");
		return Response.status(201).build();
	}

	@PUT
	@Path("/updateDisLike")
	@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response updateDisLike(LastLikeModel lastLikeModel) {
		System.out.println("updateDisLike() topic id : " + lastLikeModel.getTopicId());
		stampDate = DateUtils.dbFormat.format(new Date());
		System.out.println("StampDate : " + stampDate);
		LastTopicModel lastTopicModel = db.find(LastTopicModel.class, lastLikeModel.getTopicId());
		lastTopicModel.setCountLike(lastTopicModel.getCountLike() - 1);
		db.update(lastTopicModel);
		lastLikeModel.setDate(stampDate);
		db.update(lastLikeModel);
		System.out.println("POST Complete");
		return Response.status(201).build();
	}

	@GET
	@Path("/checkLikeTopic")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public List<LastLikeModel> checkLikeTopic(@QueryParam("topicId") String topicId,
			@QueryParam("empEmail") String empEmail) {
		List<LastLikeModel> lastLikeModelList = db
				.findByIndex(getLikeModelByTopicIdAndEmpEmailJsonString(topicId, empEmail), LastLikeModel.class);
		System.out.println("GET Complete");
		return lastLikeModelList;
	}

	@GET
	@Path("/gettopicbyid")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public List<LastTopicModel> getTopicById(@QueryParam("topicId") String topicId,
			@QueryParam("empEmail") String empEmail) {
		System.out.println(">>>>>>>>>>>>>>>>>>> getTopicById() //topcic id : " + topicId);
		List<LastTopicModel> topicModelList = db.findByIndex(getTopicByIdJsonString(topicId), LastTopicModel.class,
				new FindByIndexOptions().sort(new IndexField("date", SortOrder.asc)));
		String newRev = increaseReadCount(topicModelList.get(0), empEmail);
		if (newRev != null) {
			topicModelList.get(0).set_rev(newRev);
		}
		concatDomainImagePath(topicModelList);
		List<LastTopicModel> resultList = DateUtils.formatDBDateToClientDate(topicModelList);
		System.out.println("GET Complete");
		return resultList;
	}

	@GET
	@Path("/gethottopiclist")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public List<LastTopicModel> getHotTopicList(@QueryParam("empEmail") String empEmail,
			@QueryParam("startDate") String startDate) {
		System.out
				.println(">>>>>>>>>>>>>>>>>>> getHotTopicList() empEmail : " + empEmail + " startDate : " + startDate);
		List<RoomModel> roomModelList = db.findByIndex(getRoomJsonStringReadUser(empEmail), RoomModel.class,
				new FindByIndexOptions().sort(new IndexField("_id", SortOrder.asc)).fields("_id").fields("_rev")
						.fields("name").fields("desc").fields("type"));

		List<LastTopicModel> lastTopicModelList = db.findByIndex(getHotTopicListJsonString(roomModelList),
				LastTopicModel.class,
				new FindByIndexOptions().sort(new IndexField("updateDate", SortOrder.desc)).limit(20));
		List<LastTopicModel> completeList = checkStatusRead(lastTopicModelList, empEmail, startDate);
		System.out.println("status read : " + completeList.get(0).getStatusRead());
		List<LastTopicModel> resultList = DateUtils.formatDBDateToClientDate(completeList);
		System.out.println("getHotTopicList list size : " + resultList.size());
		return lastTopicModelList;
	}

	@GET
	@Path("/gettopiclistbyroom")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public List<LastTopicModel> getTopicListByRoomId(@QueryParam("roomId") String roomId,
			@QueryParam("empEmail") String empEmail, @QueryParam("startDate") String startDate) {
		System.out.println(">>>>>>>>>>>>>>>>>>> getTopicListByRoomId() //room id : " + roomId + " empEmail : "
				+ empEmail + " startDate : " + startDate);
		List<LastTopicModel> resultList = new ArrayList<>();
		List<LastTopicModel> lastTopicModelList = db.findByIndex(getTopicListByRoomIdJsonString(roomId),
				LastTopicModel.class, new FindByIndexOptions().sort(new IndexField("date", SortOrder.desc)));
		List<LastTopicModel> roomRuleList = getRoomRuleToppic(roomId);
		List<LastTopicModel> fullList = TopicUtils.concatList(roomRuleList, lastTopicModelList);

		if (fullList != null && !fullList.isEmpty()) {
			List<LastTopicModel> completeList = checkStatusRead(fullList, empEmail, startDate);
			resultList = DateUtils.formatDBDateToClientDate(completeList);
			updateCountTopicInNotificationModel(roomId, empEmail);
		}
		System.out.println("size : " + resultList.size());
		System.out.println("GET Complete");
		return resultList;
	}
	
	private List<LastTopicModel> getRoomRuleToppic(@QueryParam("roomId") String roomId) {
		List<LastTopicModel> topicModelList = db.findByIndex(getRoomRuleToppicJsonString(roomId), LastTopicModel.class,
				new FindByIndexOptions().fields("_id").fields("_rev").fields("avatarName").fields("avatarPic")
						.fields("subject").fields("content").fields("date").fields("type").fields("roomId")
						.fields("countLike"));
		return topicModelList;
	}

	@GET
	@Path("/getbadgenumbernotification")
	@Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public Response getBadgeNumberNotification(@QueryParam("empEmail") String empEmail) {
		System.out.println(">>>>>>>>>>>>>>>>>>> getBadgeNumberNotification() // empEmail : " + empEmail);
		RoomService roomService = new RoomService();
		List<RoomModel> roomModelList = roomService.getRooms(empEmail);
		int totalBadge = sumBadgeNumber(roomModelList);
		System.out.println("Badge Number : " + totalBadge);
		return Response.status(201).entity(totalBadge).build();
	}

	@GET
	@Path("/accessapp")
	@Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public Response setAccessApp(@QueryParam("empEmail") String empEmail) {
		System.out.println(">>>>>>>>>>>>>>>>>>> setAccessApp() // empEmail : " + empEmail);
		stampDate = DateUtils.dbFormat.format(new Date());
		AccessAppModel accessAppModel = createAccessAppModelMap(empEmail);
		db.save(accessAppModel);
		return Response.status(201).build();
	}
	
	@GET
	@Path("/readroom")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response readRoom(@QueryParam("empEmail") String empEmail,@QueryParam("roomId") String roomId) {
		System.out.println(">>>>>>>>>>>>>>>>>>> readRoom() // empEmail : " + empEmail + " roomId : " + roomId);
		stampDate = DateUtils.dbFormat.format(new Date());
		ReadRoomModel readRoomModel = createReadRoomModelMap(empEmail, roomId);
		db.save(readRoomModel);
		return Response.status(201).build();
	}

	@GET
	@Path("/readtopic")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response readTopic(@QueryParam("empEmail") String empEmail,@QueryParam("topicId") String topicId) {
		System.out.println(">>>>>>>>>>>>>>>>>>> readRoom() // empEmail : " + empEmail + " topicId : " + topicId);
		stampDate = DateUtils.dbFormat.format(new Date());
		ReadModel readModel = createReadModelMap(empEmail, topicId);
		db.save(readModel);
		return Response.status(201).build();
	}
	
	private int sumBadgeNumber(List<RoomModel> roomModelList) {
		int totalNumber = 0;
		for (RoomModel roomModel : roomModelList) {
			totalNumber += roomModel.getBadgeNumber();
		}
		return totalNumber;
	}

	private void plusCountTopicInNotificationModel(String roomId, String empEmail) {
		String stampDate = DateUtils.dbFormat.format(new Date());
		List<RoomNotificationModel> roomNotificationModelList = db.findByIndex(
				getRoomNotificationModelByRoomIdAndEmpEmail(roomId, empEmail), RoomNotificationModel.class);
		RoomNotificationModel roomNotificationModel = roomNotificationModelList.get(0);
		roomNotificationModel.setCountTopic(roomNotificationModel.getCountTopic() + 1);
		roomNotificationModel.setUpdateDate(stampDate);
		System.out.println("room noti countTopic : " + roomNotificationModel.getCountTopic());
		db.update(roomNotificationModel);
	}

	private void updateCountTopicInNotificationModel(String roomId, String empEmail) {
		String stampDate = DateUtils.dbFormat.format(new Date());
		List<RoomNotificationModel> roomNotificationModelList = db.findByIndex(
				getRoomNotificationModelByRoomIdAndEmpEmail(roomId, empEmail), RoomNotificationModel.class);
		RoomNotificationModel roomNotificationModel = roomNotificationModelList.get(0);
		List<RoomModel> roomModelList = db.findByIndex(getRoomModelByRoomId(roomId), RoomModel.class);
		RoomModel roomModel = roomModelList.get(0);
		roomNotificationModel.setCountTopic(roomModel.getTotalTopic());
		roomNotificationModel.setUpdateDate(stampDate);
		db.update(roomNotificationModel);
	}

	private void updateTotalTopicInRoomModel(String roomId) {
		RoomModel roomModel = db.find(RoomModel.class, roomId);
		roomModel.setTotalTopic(roomModel.getTotalTopic() == null ? 1 : roomModel.getTotalTopic() + 1);
		db.update(roomModel);
	}

	private void updateTotalDeleteInRoomModel(String roomId) {
		RoomModel roomModel = db.find(RoomModel.class, roomId);
		roomModel.setTotalDelete(roomModel.getTotalDelete() == null ? 1 : roomModel.getTotalDelete() + 1);
		db.update(roomModel);
	}

	private void deleteAllInTopic(String _id, String actionEmail) {
		List<LastTopicModel> topicModelList = db.findByIndex(getAllByIdJsonString(_id), LastTopicModel.class);
		for (int i = 0; i < topicModelList.size(); i++) {
			if (topicModelList.get(i).getType().equals("comment")) {
				insertLogDelete(topicModelList.get(i), actionEmail);
			}
			db.remove(topicModelList.get(i));
		}
		System.out.println("delete all in topic complete");
	}

	private List<LastTopicModel> checkStatusRead(List<LastTopicModel> lastTopicModelList, String empEmail,
			String startDate) {
		String topicIdString = generateTopicIdString(lastTopicModelList);
		List<ReadModel> readModelList = db.findByIndex(getReadModelByEmpEmailString(empEmail, topicIdString),
				ReadModel.class, new FindByIndexOptions().sort(new IndexField("date", SortOrder.desc)).limit(30));
		System.out.println("readModelList size : " + readModelList.size());
		List<LastTopicModel> resultList = new ArrayList<>();
		for (LastTopicModel lastTopicModel : lastTopicModelList) {
			if (DateUtils.isAfterDate(startDate, lastTopicModel.getDate())) {
				if (hasReadModel(readModelList, lastTopicModel)) {
					lastTopicModel.setStatusRead(true);
				} else {
					lastTopicModel.setStatusRead(false);
				}
			} else {
				lastTopicModel.setStatusRead(true);
			}
			resultList.add(lastTopicModel);
		}
		return resultList;
	}

	private boolean hasReadModel(List<ReadModel> readModelList, LastTopicModel lastTopicModel) {
		boolean result = false;
		for (ReadModel readModel : readModelList) {
			if (readModel.getTopicId().equals(lastTopicModel.get_id())) {
				result = true;
				break;
			}
		}
		return result;
	}

	private String increaseReadCount(LastTopicModel lastTopicModel, String empEmail) {
		try {
			String rev = null;
			System.out.println(">>>>>>>>>>>>>>>>>> increaseReadCount() topicModelMap : " + lastTopicModel.get_id()
					+ ", empEmail : " + empEmail);
			stampDate = DateUtils.dbFormat.format(new Date());
			System.out.println("StampDate : " + stampDate);
			LastTopicModel localLastTopicModel = lastTopicModel;
			List<ReadModel> readModelList = db.findByIndex(
					getReadModelByTopicIdAndEmpEmailString(localLastTopicModel.get_id(), empEmail), ReadModel.class);
			
			if (haveNotBeenReadTopic(readModelList)) {
                System.out.println("This user is never read this topic");
                localLastTopicModel.setCountRead(getCountRead(localLastTopicModel) + 1);
                rev = db.update(localLastTopicModel).getRev();
                plusCountTopicInNotificationModel(lastTopicModel.getRoomId(), empEmail);
            }
			
			return rev;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	private boolean haveNotBeenReadTopic(List<ReadModel> readModelList) {
        return readModelList == null || readModelList.isEmpty();
    }

	private void insertLogDelete(LastTopicModel lastTopicModel, String actionEmail) {
		LogDeleteModel logDeletModelList = new LogDeleteModel();
		stampDate = DateUtils.dbFormat.format(new Date());
		logDeletModelList.setEmpEmail(lastTopicModel.getEmpEmail());
		logDeletModelList.setContent(lastTopicModel.getContent());
		logDeletModelList.setSubject(lastTopicModel.getSubject() == null ? "" : lastTopicModel.getSubject());
		logDeletModelList.setRoomId(lastTopicModel.getRoomId());
		logDeletModelList.setTypeDel(lastTopicModel.getType());
		logDeletModelList.setTopId((lastTopicModel.getTopicId() == null || lastTopicModel.getTopicId().equals(""))
				? lastTopicModel.get_id() : lastTopicModel.getTopicId());
		logDeletModelList.setType("log");
		logDeletModelList.setDate(stampDate);
		logDeletModelList.setActionEmail(actionEmail);
		db.save(logDeletModelList);
		System.out.println("Insert Log Delete Complete");
	}

	private String getRoomNotificationModelByRoomIdAndEmpEmail(String roomId, String empEmail) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"$and\": [{\"type\":\"roomNotification\"}, {\"roomId\":\"" + roomId + "\"}, {\"empEmail\":\""
				+ empEmail + "\"}]");
		sb.append("}}");
		System.out.println("query string : " + sb.toString());
		return sb.toString();
	}

	private String getRoomModelByRoomId(String roomId) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"$and\": [{\"type\":\"room\"}, {\"_id\":\"" + roomId + "\"}]");
		sb.append("}}");
		return sb.toString();
	}

	private String getReadModelByEmpEmailString(String empEmail, String topicIdString) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"$and\": [{\"type\":\"read\"}, {\"empEmail\":\"" + empEmail + "\"}, {\"topicId\":{\"$or\": ["
				+ topicIdString + "]}}]");
		sb.append("},");
		sb.append("\"fields\": [\"_id\",\"_rev\",\"topicId\",\"empEmail\",\"type\",\"date\"]}");
		System.out.println("query : " + sb.toString());
		return sb.toString();
	}

	private String getReadModelByTopicIdAndEmpEmailString(String topicId, String empEmail) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"$and\": [{\"type\":\"read\"}, {\"topicId\":\"" + topicId + "\"}, {\"empEmail\":\"" + empEmail
				+ "\"}]");
		sb.append("},");
		sb.append("\"fields\": [\"_id\",\"_rev\",\"topicId\",\"empEmail\",\"type\",\"date\"]}");
		return sb.toString();
	}

	private String getHotTopicListJsonString(List<RoomModel> roomModelList) {
		String roomIdString = generateRoomIdString(roomModelList);
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"updateDate\": {\"$gt\": 0},");
		sb.append("\"pin\": {\"$exists\": false},");
		sb.append("\"$and\": [{\"type\":\"host\"},");
		sb.append("{\"roomId\":{\"$or\": [" + roomIdString + "]}}]");
		sb.append("},");
		sb.append(
				"\"fields\": [\"_id\",\"_rev\",\"avatarName\",\"avatarPic\",\"subject\",\"content\",\"date\",\"type\",\"roomId\",\"countLike\",\"updateDate\"]}");
		return sb.toString();
	}

	private String getTopicByIdJsonString(String topicId) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"date\": {\"$gt\": 0},");
		sb.append("\"$and\": [");
		sb.append("{\"$or\": [ {\"type\": \"host\"}, {\"type\": \"comment\"}]},");
		sb.append("{\"$or\":[{\"_id\":\""+topicId+"\"},{\"topicId\": \""+topicId+"\"}]");
		sb.append("}]},");
		sb.append("\"fields\": [\"_id\",\"_rev\",\"avatarName\",\"avatarPic\",\"subject\",\"content\",\"date\",\"type\",\"roomId\",\"countLike\",\"updateDate\"]}");
		return sb.toString();
	}

	private String getRoomJsonStringReadUser(String empEmail) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"$and\": [");
		sb.append("{\"type\":\"room\"},");
		sb.append("{\"show\":true},");
		sb.append("{\"readUser\":{\"$elemMatch\": {");
		sb.append("\"$or\": [\"all\", \"" + empEmail + "\"]");
		sb.append("}}}]");
		sb.append("},");
		sb.append("\"fields\": [\"_id\",\"_rev\",\"name\",\"desc\", \"type\"]}");
		return sb.toString();
	}

	private String getTopicListByRoomIdJsonString(String roomId) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"date\": {\"$gt\": 0},");
		sb.append("\"pin\": {\"$exists\": false},");
		sb.append("\"$and\": [{\"type\":\"host\"}, {\"roomId\":\"" + roomId + "\"}]");
		sb.append("},");
		sb.append("\"fields\": [\"_id\",\"_rev\",\"avatarName\",\"avatarPic\",\"subject\",\"content\",\"date\",\"type\",\"roomId\"]}");
		return sb.toString();
	}

	private String getLikeModelByTopicIdAndEmpEmailJsonString(String topicId, String empEmail) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"$and\": [{\"type\":\"like\"}, {\"topicId\":\"" + topicId + "\"}, {\"empEmail\":\"" + empEmail + "\"}]");
		sb.append("},");
		sb.append("\"fields\": [\"_id\",\"_rev\",\"topicId\",\"empEmail\",\"isLike\",\"type\"]}");
		return sb.toString();
	}

	private String getRoomRuleToppicJsonString(String roomId) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"pin\": {\"$gte\": 0},");
		sb.append("\"$and\": [{\"type\":\"host\"}, {\"roomId\":\"" + roomId + "\"}]");
		sb.append("}}");
		return sb.toString();
	}

	private String getAllByIdJsonString(String _id) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"selector\": {");
		sb.append("\"_id\": {\"$gt\": 0},");
		sb.append("\"$and\": [{\"topicId\":\"" + _id + "\"}]");
		sb.append("}}");
		return sb.toString();
	}

	private int getCountRead(LastTopicModel lastTopicModel) {
		return lastTopicModel.getCountRead() == null ? 0 : lastTopicModel.getCountRead();
	}

	private ReadModel createReadModelMap(String empEmail, String topicId) {
		System.out.println("createReadModelMap >>>> empEmail : " + empEmail + ", topicId : " + topicId);
		ReadModel readModel = new ReadModel();
		readModel.setTopicId(topicId);
		readModel.setEmpEmail(empEmail);
		readModel.setType("read");
		readModel.setDate(stampDate);
		return readModel;
	}

	private ReadRoomModel createReadRoomModelMap(String empEmail, String roomId) {
		System.out.println("empEmail : " + empEmail + ", roomId : " + roomId);
		ReadRoomModel readRoomModel = new ReadRoomModel();
		readRoomModel.setRoomId(roomId);
		readRoomModel.setEmpEmail(empEmail);
		readRoomModel.setType("readRoom");
		readRoomModel.setDate(stampDate);
		return readRoomModel;
	}
	
	private AccessAppModel createAccessAppModelMap(String empEmail) {
		System.out.println("empEmail : " + empEmail);
		AccessAppModel accessAppModel = new AccessAppModel();
		accessAppModel.setVersionId("v120110");
		accessAppModel.setEmpEmail(empEmail);
		accessAppModel.setType("accessApp");
		accessAppModel.setDate(stampDate);
		return accessAppModel;
	}
	
	public void concatDomainImagePath(List<LastTopicModel> lastTopicModelList) {
		for (int i = 0; i < lastTopicModelList.size(); i++) {
			String content = (String) lastTopicModelList.get(i).getContent();
			if (content != null) {
				lastTopicModelList.get(i).setContent(ConcatDomainUtils.concatDomainImagePath(content));
			}
		}
	}

	public String generateRoomIdString(List<RoomModel> roomModelList) {
		StringBuilder stingBuilder = new StringBuilder();
		String prefix = "";
		for (RoomModel roomModel : roomModelList) {
			stingBuilder.append(prefix);
			prefix = ",";
			stingBuilder.append("\"" + roomModel.get_id() + "\"");
		}
		return stingBuilder.toString();
	}

	private String generateTopicIdString(List<LastTopicModel> lastTopicModelList) {
		StringBuilder stingBuilder = new StringBuilder();
		String prefix = "";
		for (LastTopicModel lastTopicModel : lastTopicModelList) {
			stingBuilder.append(prefix);
			prefix = ",";
			stingBuilder.append("\"" + lastTopicModel.get_id() + "\"");
		}
		return stingBuilder.toString();
	}

}
