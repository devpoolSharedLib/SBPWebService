package th.co.gosoft.sbp.model;

public class ChoiceMasterModel {

    private String choiceKey;
    private String choiceTitle;

    private Integer countChoice;
    
    public Integer getCountChoice() {
		return countChoice;
	}
	public void setCountChoice(Integer countChoice) {
		this.countChoice = countChoice;
	}
	public String getChoiceKey() {
        return choiceKey;
    }
    public void setChoiceKey(String choiceKey) {
        this.choiceKey = choiceKey;
    }
    public String getChoiceTitle() {
        return choiceTitle;
    }
    public void setChoiceTitle(String choiceTitle) {
        this.choiceTitle = choiceTitle;
    }
    
}
