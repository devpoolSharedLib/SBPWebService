package th.co.gosoft.sbp.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.FindByIndexOptions;

import th.co.gosoft.sbp.model.UserAuthenModel;
import th.co.gosoft.sbp.model.UserModel;
import th.co.gosoft.sbputil.CloudantClientUtils;
import th.co.gosoft.sbputil.EmailUtils;
import th.co.gosoft.sbputil.KeyStoreUtils;
import th.co.gosoft.sbputil.PropertiesUtils;

@Path("server/user")
public class UserService {
	private static final String SUBJECT = "GO10, reset your password";
	private static final String EMAIL_CONTENT = "\nPlease copy and paste the following link in Google Chrome Browser. \n\n";
	
	private static final String FROM_EMAIL = PropertiesUtils.getProperties("send_email");
    private static final String PASSWORD = PropertiesUtils.getProperties("send_email_password");
    private static final String DOMAIN_LINK_RESET_PASSWORD = PropertiesUtils.getProperties("domain_reset_password");
    
    @GET
    @Path("/getUserByAccountId")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<UserModel> getUserByAccountId(@QueryParam("accountId") String accountId) {
        System.out.println(">>>>>>>>>>>>>>>>>>> getUserByAccountId() // accountId : "+accountId);
        Database db = CloudantClientUtils.getDBNewInstance();
        List<UserModel> userModelList = db.findByIndex(getUserByAccountIdJsonString(accountId), UserModel.class);
        System.out.println("GET Complete");
        return userModelList;
    }
    
    @GET
    @Path("/getUserByToken")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<UserModel> getUserByToken(@QueryParam("token") String token) {
        System.out.println(">>>>>>>>>>>>>>>>>>> getUserByToken() // token : "+token);
        Database db = CloudantClientUtils.getDBNewInstance();
        List<UserModel> userModelList = db.findByIndex(getUserByTokenJsonString(token), UserModel.class);
        System.out.println("GET Complete");
        return userModelList;
    }
    
    @GET
    @Path("/getUserByUserPassword")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<UserModel> getUserByUserPassword(@QueryParam("email") String email, @QueryParam("password") String password) {
        String localEmail = email.toLowerCase();
        System.out.println(">>>>>>>>>>>>>>>>>>> getUserByUserPassword() // user : "+localEmail+", password : "+password);
        Database db = CloudantClientUtils.getDBNewInstance();
        List<UserAuthenModel> userAuthenModelList = db.findByIndex(getUserAuthenByEmailJsonString(localEmail), UserAuthenModel.class);
        
        if(userAuthenModelList != null && !userAuthenModelList.isEmpty() && !KeyStoreUtils.authenPassword(userAuthenModelList.get(0).getPassword(), password)){
            System.out.println("Invalid Authentication");
            return new ArrayList<UserModel>();
        } else {
            System.out.println("GET Complete");
            List<UserModel> userModelList = db.findByIndex(getUserByEmailJsonString(localEmail), UserModel.class);
            return userModelList;
        }
    }
    
    @GET
    @Path("/activateUserByToken")
    public String activateUserByToken(@QueryParam("token") String token) {
        System.out.println(">>>>>>>>>>>>>>>>>>> activateUserByToken() // token : " + token);
        Database db = CloudantClientUtils.getDBNewInstance();
        List<UserAuthenModel> userAuthenModelList = db.findByIndex(activateUserByTokenJsonString(token), UserAuthenModel.class);
        if(userAuthenModelList != null && !userAuthenModelList.isEmpty()){
        	List<UserModel> userModelList =  db.findByIndex(getUserByEmailJsonString(userAuthenModelList.get(0).getEmpEmail()), UserModel.class);
       	 		if(userModelList.get(0).isActivate()){
       	 			return "This account has been activated.";
       	 		}else{
       	 			UserModel userModel = userModelList.get(0);
		       		userModel.setActivate(true);
		       		db.update(userModel);
		       		System.out.println("You have update the user activate true");
		       		return "Your account is activated. You can search \"GO10\" to download the application on App Store and Play Store."; 
       	 		}
        }else{
        	System.out.println("Invalid Authentication");
            return "Invalid Authentication, Please contact development team : thanomcho@gosoft.co.th, manitkan@gosoft.co.th, jirapaschi@gosoft.co.th";
        }
    }
    
    @GET
    @Path("/resetPasswordByEmail")
    public String resetPasswordByEmail(@QueryParam("email") String email) {
        System.out.println(">>>>>>>>>>>>>>>>>>> resetPasswordByEmail() // email : " + email);
        Database db = CloudantClientUtils.getDBNewInstance();
        List<UserModel> userModelList = db.findByIndex(getUserByEmailJsonString(email), UserModel.class);
        if(userModelList != null && !userModelList.isEmpty()){
        	List<UserAuthenModel> userAuthenModelList =  db.findByIndex(getUserAuthenByEmailJsonString(userModelList.get(0).getEmpEmail()), UserAuthenModel.class);
   	 		if(userModelList.get(0).isActivate()){
   	 			System.out.println("Send Email");
       	 		String tokenVar = "?token=";
       	 		String token = userAuthenModelList.get(0).getToken();
       	 		System.out.println(">>>>>>>>>"+userAuthenModelList.get(0).getToken() + " " + token);
       	 		String body = EMAIL_CONTENT + DOMAIN_LINK_RESET_PASSWORD+tokenVar+token;
                body += "\n\n\nBest Regards,";
                EmailUtils.sendFromGMail(FROM_EMAIL, PASSWORD, email, SUBJECT, body);
       	 		return "You can check e-mail for reset password.";
   	 		}else{
   	 		    System.out.println("User has not been activated.");
	       		return "User does not exist on the system."; 
   	 		}
        }else{
        	System.out.println("User does not exist on the system.");
       		return "User does not exist on the system."; 
        }
    }
    
    @PUT
    @Path("/updateUser")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response updateUser(UserModel userModel) {
        System.out.println(">>>>>>>>>>>>>>>>>>> updateUser()");
        Database db = CloudantClientUtils.getDBNewInstance();
        userModel.setActivate(true);
        com.cloudant.client.api.model.Response response = db.update(userModel);
        String result = response.getRev();
        System.out.println(">>>>>>>>>>>>>>>>>>> post result Rev : "+result);
        System.out.println("PUT Complete");
        return Response.status(201).entity(result).build();
    }
    
    @GET
    @Path("/checkAvatarName")
    public Response checkAvatarName(@QueryParam("avatarName") String avatarName) {
        System.out.println(">>>>>>>>>>>>>>>>>>> checkAvatarName() avatarName : "+avatarName);
        Database db = CloudantClientUtils.getDBNewInstance();
        List<UserModel> userModelList = db.findByIndex(getUserByAvatarNameJsonString(avatarName), UserModel.class,
                new FindByIndexOptions().useIndex("_design/avatar-name-design-doc"));
        if(userModelList == null || userModelList.isEmpty()){
            System.out.println("Not found this avatarName in db");
            return Response.status(201).entity("This avatar name has not been used.").build();
        } else {
            System.out.println("found this avatarName in db");
            return Response.status(404).entity("This avatar name has been already used.").build();
        }
    }
    
    @GET
    @Path("/checkUserActivation")
    public Response checkUserActivation(@QueryParam("empEmail") String empEmail) {
        System.out.println(">>>>>>>>>>>>>>>>>>> checkAvatarName() empEmail : "+empEmail);
        Database db = CloudantClientUtils.getDBNewInstance();
        List<UserModel> userModelList = db.findByIndex(getUserByEmailJsonString(empEmail), UserModel.class);
        if(userModelList.get(0).isActivate()){
            System.out.println("This user account is activate.");
            return Response.status(201).entity("This user account is activate.").build();
        } else {
            System.out.println("This user account is not activate.");
            return Response.status(404).entity("This user account is not activate.").build();
        }
    }
    
    @GET
    @Path("/getEmailFullTextSerch")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<UserModel> getEmailFulTextSerch(@QueryParam("empEmail") String empEmail) {
        System.out.println(">>>>>>>>>>>>>>>>>>> getEmailFullTextSerch() empEmail : "+empEmail);
        Database db = CloudantClientUtils.getDBNewInstance();
        List<UserModel> userModelList = db.findByIndex(getEmailFullTextSerchJsonString(empEmail), UserModel.class, 
                new FindByIndexOptions().useIndex("_design/emp-email-design-doc").fields("empName").fields("empEmail"));
        System.out.println("list size : "+userModelList.size());
        return userModelList;
    }
    
    private String getEmailFullTextSerchJsonString(String empEmail) {
        StringBuilder stingBuilder = new StringBuilder();
        stingBuilder.append("{\"selector\": {");
        stingBuilder.append("\"empEmail\": {\"$regex\": \""+empEmail+"\"}");
        stingBuilder.append(",\"type\": \"user\"");
        stingBuilder.append("}, \"use_index\": \"_design/emp-email-design-doc\"");
        stingBuilder.append("}");
        
        return stingBuilder.toString();
    }

    private String getUserByAvatarNameJsonString(String avatarName){
        StringBuilder stingBuilder = new StringBuilder();
        stingBuilder.append("{\"selector\": {");
        stingBuilder.append("\"$text\": \""+avatarName+"\"");
        stingBuilder.append(",\"type\": \"user\"");
        stingBuilder.append("}, \"use_index\": \"_design/avatar-name-design-doc\"");
        stingBuilder.append("}");
        
        return stingBuilder.toString();
    }
    
    private String getUserByAccountIdJsonString(String accountId){
        StringBuilder stingBuilder = new StringBuilder();
        stingBuilder.append("{\"selector\": {");
        stingBuilder.append("\"_id\": {\"$gt\": 0},");
        stingBuilder.append("\"$and\": [{\"type\": \"user\"}, {\"accountId\":\""+accountId+"\"} ] ");
        stingBuilder.append("},");
        stingBuilder.append("\"fields\": [\"_id\",\"_rev\",\"accountId\",\"empName\",\"empEmail\",\"avatarName\",\"avatarPic\",\"token\",\"activate\",\"type\"]}");
        
        return stingBuilder.toString();
    }
    
    private String getUserByTokenJsonString(String token){
        StringBuilder stingBuilder = new StringBuilder();
        stingBuilder.append("{\"selector\": {");
        stingBuilder.append("\"_id\": {\"$gt\": 0},");
        stingBuilder.append("\"$and\": [{\"type\": \"user\"}, {\"token\":\""+token+"\"} ] ");
        stingBuilder.append("},");
        stingBuilder.append("\"fields\": [\"_id\",\"_rev\",\"accountId\",\"empName\",\"empEmail\",\"avatarName\",\"avatarPic\",\"token\",\"activate\",\"type\"]}");
        
        return stingBuilder.toString();
    }
    
    private String getUserAuthenByEmailJsonString(String email){
        StringBuilder stingBuilder = new StringBuilder();
        stingBuilder.append("{\"selector\": {");
        stingBuilder.append("\"_id\": {\"$gt\": 0},");
        stingBuilder.append("\"$and\": [{\"type\": \"authen\"}, {\"empEmail\":\""+email+"\"}] ");
        stingBuilder.append("},");
        stingBuilder.append("\"fields\": [\"_id\",\"_rev\",\"empEmail\",\"password\",\"type\",\"token\"]}");
        
        return stingBuilder.toString();
    }
    
    private String getUserByEmailJsonString(String email){
        StringBuilder stingBuilder = new StringBuilder();
        stingBuilder.append("{\"selector\": {");
        stingBuilder.append("\"_id\": {\"$gt\": 0},");
        stingBuilder.append("\"$and\": [{\"type\": \"user\"}, {\"empEmail\":\""+email+"\"}] ");
        stingBuilder.append("},");
        stingBuilder.append("\"fields\": [\"_id\",\"_rev\",\"empName\",\"empEmail\",\"avatarName\",\"avatarPic\",\"activate\",\"type\",\"birthday\"]}");
        
        return stingBuilder.toString();
    }
    
    private String activateUserByTokenJsonString(String token){
        StringBuilder stingBuilder = new StringBuilder();
        stingBuilder.append("{\"selector\": {");
        stingBuilder.append("\"_id\": {\"$gt\": 0},");
        stingBuilder.append("\"$and\": [{\"type\": \"authen\"}, {\"token\":\""+token+"\"}] ");
        stingBuilder.append("},");
        stingBuilder.append("\"fields\": [\"_id\",\"_rev\",\"empEmail\",\"password\",\"type\",\"token\"]}");
        
        return stingBuilder.toString();
    }
    
}
