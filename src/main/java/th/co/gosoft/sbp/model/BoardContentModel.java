package th.co.gosoft.sbp.model;

import java.util.List;

public class BoardContentModel {

    private List<LastTopicModel> boardContentList;
    private List<PollModel> pollModel;
    private Integer countAcceptPoll;
    private Boolean donePoll;
    
    public Integer getCountAcceptPoll() {
        return countAcceptPoll;
    }
    public void setCountAcceptPoll(Integer countAcceptPoll) {
        this.countAcceptPoll = countAcceptPoll;
    }
    public List<LastTopicModel> getBoardContentList() {
        return boardContentList;
    }
    public void setBoardContentList(List<LastTopicModel> boardContentList) {
        this.boardContentList = boardContentList;
    }
	public List<PollModel> getPollModel() {
		return pollModel;
	}
	public void setPollModel(List<PollModel> pollModel) {
		this.pollModel = pollModel;
	}
    public Boolean getDonePoll() {
        return donePoll;
    }
    public void setDonePoll(Boolean donePoll) {
        this.donePoll = donePoll;
    }
}
