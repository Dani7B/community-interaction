package rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hbase.query.AtLeast;
import hbase.query.AtLeastTimes;
import hbase.query.Author;
import hbase.query.Authors;
import hbase.query.HQuery;
import hbase.query.Mention;
import hbase.query.time.LastMonth;
import hbase.query.time.LastMonthFromNow;
import hbase.query.time.LastYear;
import hbase.query.time.LastYearFromNow;
import hbase.query.time.MonthsAgo;
import hbase.query.time.ThisYear;
import hbase.query.time.WeeksAgo;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/** Class to handle REST http requests
 * @author Daniele Morgantini */
@Path("/search")
public class RestManager {
	
	private Authors authors;
	
	private HQuery query;
	
	
	public RestManager() {
		this.query = new HQuery();
		this.authors = new Authors(query);
	}
	
	@GET
	@Path("/users")
	/** Method to answer any kind of query, also composed queries */
	public Response generic(
		@DefaultValue("1") @QueryParam("tm_atLeast") int tm_al,
		@DefaultValue("1") @QueryParam("tm_minTimes") int tm_times,
		@QueryParam("tm_when") String tm_when,
		@QueryParam("tm_back") int tm_back,
		@QueryParam("tm_users") String tm_users,
		@DefaultValue("1") @QueryParam("m_minTimes") int m_times,
		@QueryParam("m_when") String m_when,
		@QueryParam("m_back") int m_back,
		@QueryParam("m_users") String m_users,
		@DefaultValue("1") @QueryParam("wf_atLeast") int wf_al,
		@QueryParam("wf_users") String wf_users,
		@QueryParam("wff_users") String wff_users,
		@QueryParam("wfafb_users") String wfafb_users,
		@DefaultValue("1") @QueryParam("wfm_atLeast") int wfm_al,
		@QueryParam("wfm_when") String wfm_when,
		@QueryParam("wfm_back") int wfm_back,
		@DefaultValue("1") @QueryParam("wfm_minTimes") int wfm_minTimes,
		@QueryParam("wfm_users") String wfm_users,
		@QueryParam("byId") String byId,
		@DefaultValue("true") @QueryParam("byHits") boolean byHits,
		@DefaultValue("10") @QueryParam("take") int take) throws JSONException, IOException{
		
		String output = "Users";
		if(tm_users!=null && tm_when!=null){
			this.authorsThatMentionedSubquery(tm_al, tm_times, tm_when, tm_back, tm_users);
			output += " that mentioned at least: " + tm_al + " user";
			if(tm_al>1)
				output += "s";
			output += " no less than " + tm_times + " time";
			if(tm_times>1)
				output += "s";
			output += " when: " + tm_when;
			if(tm_back>0)
				output += " (" + tm_back + ")";
			output += " amongst: " + tm_users + ",";
		}
		
		if(m_users!=null && m_when!=null){
			this.authorsMentionedSubquery(m_times, m_when, m_back, m_users);
			output += " mentioned no less than " + m_times + " time";
			if(m_times>1)
				output += "s";
			output += " when: " + m_when;
			if(m_back>0)
				output += " (" + m_back + ")";
			output += " amongst: " + m_users + ",";
		}
		
		if(wf_users!=null){
			this.authorsWhoFollowSubquery(wf_al, wf_users);
			output += " who follow " +
					"at least: " + wf_al + " user";
			if(wf_al>1)
				output += "s";
			output += " amongst: " + wf_users + ",";
		}
		
		if(wff_users!=null) {
			this.authorsWhoseFollowersFollowSubquery(wff_users);
			output += " whose followers follow one amongst " + wff_users + ",";
		}
		
		if(wfafb_users!=null) {
			this.authorsWhoseFollowersAreFollowedBySubquery(wfafb_users);
			output += " whose followers are followed by one amongst " + wfafb_users + ",";
		}
		
		if(wfm_users!=null) {
			this.authorsWhoseFollowersMentionedSubquery(wfm_al, wfm_when, wfm_back, wfm_users, wfm_minTimes);
			output += " whose followers mentioned" + 
						" at least: " + wfm_al + " user";
			if(wfm_al>1)
				output += "s";
			output += " when: " + wfm_when;
			if(wfm_back>0)
				output += " (" + wfm_back + ")";
			output += " amongst: " + wfm_users +
						" no less than " + wfm_minTimes + " time";
			if(wfm_minTimes>1)
				output += "s";
			output += ",";
		}
		
		if(byId!=null) { // if byId is present, it's got priority over byHits
			boolean b = Boolean.parseBoolean(byId);
			this.authors.rankedById(b);
		}
		else {
			this.authors.rankedByHits(byHits);
		}
		this.authors.take(take);
		this.authors = this.query.answer();
		
		String queryString = output.substring(0, output.length()-1);
		JSONObject response = formatResponse(queryString, this.authors);
		if(this.authors.size()==0)
			return Response.status(Response.Status.NOT_FOUND).entity(response).build();
		return Response.ok(response.toString(4), MediaType.APPLICATION_JSON_TYPE).build();
	}
	
	@GET
	@Path("/users/thatMentioned")
	/** Method to answer AuthorsThatMentioned subquery */
	public Response usersThatMentioned(
		@DefaultValue("1") @QueryParam("atLeast") int tm_al,
		@DefaultValue("1") @QueryParam("minTimes") int tm_times,
		@QueryParam("when") String tm_when,
		@QueryParam("back") int tm_back,
		@QueryParam("users") String tm_users,
		@QueryParam("byId") String byId,
		@DefaultValue("true") @QueryParam("byHits") boolean byHits,
		@DefaultValue("10") @QueryParam("take") int take) throws JSONException, IOException{
		
		this.authorsThatMentionedSubquery(tm_al, tm_times, tm_when, tm_back, tm_users);
		if(byId!=null) { // if byId is present, it's got priority over byHits
			boolean b = Boolean.parseBoolean(byId);
			this.authors.rankedById(b);
		}
		else {
			this.authors.rankedByHits(byHits);
		}
		if(take>0) {
			this.authors.take(take);
		}
		this.authors = this.query.answer();
		
		List<Long> result = new ArrayList<Long>();
		for(Author a : this.authors.getAuthors())
			result.add(a.getId());
		String queryString = "Users that mentioned" +
						" at least: " + tm_al + " user";
		if(tm_al>1)
			queryString += "s";
		queryString += " no less than " + tm_times + " time";
		if(tm_times>1)
			queryString += "s";
		queryString += " when: " + tm_when;
		if(tm_back>0)
			queryString += " (" + tm_back + ")";
		queryString += " amongst: " + tm_users;
		
		JSONObject response = formatResponse(queryString, this.authors);
		if(this.authors.size()==0)
			return Response.status(Response.Status.NOT_FOUND).entity(response).build();
		return Response.ok(response.toString(4), MediaType.APPLICATION_JSON_TYPE).build();
	}
	
	
	@GET
	@Path("/users/mentioned")
	/** Method to answer AuthorsMentioned subquery */
	public Response usersMentioned(
		@DefaultValue("1") @QueryParam("minTimes") int tm_times,
		@QueryParam("when") String tm_when,
		@QueryParam("back") int tm_back,
		@QueryParam("users") String tm_users,
		@QueryParam("byId") String byId,
		@DefaultValue("true") @QueryParam("byHits") boolean byHits,
		@DefaultValue("10") @QueryParam("take") int take) throws JSONException, IOException{
		
		this.authorsMentionedSubquery(tm_times, tm_when, tm_back, tm_users);
		if(byId!=null) { // if byId is present, it's got priority over byHits
			boolean b = Boolean.parseBoolean(byId);
			this.authors.rankedById(b);
		}
		else {
			this.authors.rankedByHits(byHits);
		}
		if(take>0) {
			this.authors.take(take);
		}
		this.authors = this.query.answer();
		
		List<Long> result = new ArrayList<Long>();
		for(Author a : this.authors.getAuthors())
			result.add(a.getId());
		String queryString = "Users that mentioned no less than " + tm_times + " time";
		if(tm_times>1)
			queryString += "s";
		queryString += " when: " + tm_when;
		if(tm_back>0)
			queryString += " (" + tm_back + ")";
		queryString += " amongst: " + tm_users;
		
		JSONObject response = formatResponse(queryString, this.authors);
		if(this.authors.size()==0)
			return Response.status(Response.Status.NOT_FOUND).entity(response).build();
		return Response.ok(response.toString(4), MediaType.APPLICATION_JSON_TYPE).build();
	}
	
	
	@GET
	@Path("/users/whoFollow")
	/** Method to answer AuthorsWhoFollow subquery */
	public Response usersWhoFollow(
		@DefaultValue("1") @QueryParam("atLeast") int wf_al,
		@QueryParam("users") String wf_users,
		@QueryParam("byId") String byId,
		@DefaultValue("true") @QueryParam("byHits") boolean byHits,
		@DefaultValue("10") @QueryParam("take") int take) throws JSONException, IOException{
			
		this.authorsWhoFollowSubquery(wf_al, wf_users);
		if(byId!=null) { // if byId is present, it's got priority over byHits
			boolean b = Boolean.parseBoolean(byId);
			this.authors.rankedById(b);
		}
		else {
			this.authors.rankedByHits(byHits);
		}
		if(take>0) {
			this.authors.take(take);
		}
		this.authors = this.query.answer();
		
		List<Long> result = new ArrayList<Long>();
		for(Author a : this.authors.getAuthors())
			result.add(a.getId());
		String queryString = "Users who follow" +
						" at least: " + wf_al + " user";
		if(wf_al>1)
			queryString += "s";
		queryString += " amongst: " + wf_users;
		
		JSONObject response = formatResponse(queryString, this.authors);
		if(this.authors.size()==0)
			return Response.status(Response.Status.NOT_FOUND).entity(response).build();
		return Response.ok(response.toString(4), MediaType.APPLICATION_JSON_TYPE).build();
	}
	
	@GET
	@Path("/users/whoseFollowersFollow")
	/** Method to answer AuthorsWhoseFollowersFollow subquery */
	public Response usersWhoseFollowersFollow(
		@QueryParam("users") String users,
		@QueryParam("byId") String byId,
		@DefaultValue("true") @QueryParam("byHits") boolean byHits,
		@DefaultValue("10") @QueryParam("take") int take) throws JSONException, IOException{
			
		this.authorsWhoseFollowersFollowSubquery(users);
		if(byId!=null) { // if byId is present, it's got priority over byHits
			boolean b = Boolean.parseBoolean(byId);
			this.authors.rankedById(b);
		}
		else {
			this.authors.rankedByHits(byHits);
		}
		if(take>0) {
			this.authors.take(take);
		}
		this.authors = this.query.answer();
		
		List<Long> result = new ArrayList<Long>();
		for(Author a : this.authors.getAuthors())
			result.add(a.getId());
		
		String queryString = "Users whose followers follow one amongst " + users;
		JSONObject response = formatResponse(queryString, this.authors);
		if(this.authors.size()==0)
			return Response.status(Response.Status.NOT_FOUND).entity(response).build();
		return Response.ok(response.toString(4), MediaType.APPLICATION_JSON_TYPE).build();
	}
	
	@GET
	@Path("/users/whoseFollowersAreFollowedBy")
	/** Method to answer AuthorsWhoseFollowersAreFollowedBy subquery */
	public Response usersWhoseFollowersAreFollowedBy(
		@QueryParam("users") String users,
		@QueryParam("byId") String byId,
		@DefaultValue("true") @QueryParam("byHits") boolean byHits,
		@DefaultValue("10") @QueryParam("take") int take) throws JSONException, IOException{
			
		this.authorsWhoseFollowersAreFollowedBySubquery(users);
		if(byId!=null) { // if byId is present, it's got priority over byHits
			boolean b = Boolean.parseBoolean(byId);
			this.authors.rankedById(b);
		}
		else {
			this.authors.rankedByHits(byHits);
		}
		if(take>0) {
			this.authors.take(take);
		}
		this.authors = this.query.answer();
		
		List<Long> result = new ArrayList<Long>();
		for(Author a : this.authors.getAuthors())
			result.add(a.getId());
		String queryString = "Users whose followers are followed by one amongst" + users;
		JSONObject response = formatResponse(queryString, this.authors);
		if(this.authors.size()==0)
			return Response.status(Response.Status.NOT_FOUND).entity(response).build();
		return Response.ok(response.toString(4), MediaType.APPLICATION_JSON_TYPE).build();
	}
	
	@GET
	@Path("/users/whoseFollowersMentioned")
	/** Method to answer AuthorsWhoseFollowersMentioned subquery */
	public Response usersWhoseFollowersMentioned(
		@QueryParam("when") String when,
		@QueryParam("back") int back,
		@DefaultValue("1") @QueryParam("atLeast") int al,
		@DefaultValue("1") @QueryParam("minTimes") int minTimes,
		@QueryParam("users") String users,
		@QueryParam("byId") String byId,
		@DefaultValue("true") @QueryParam("byHits") boolean byHits,
		@DefaultValue("10") @QueryParam("take") int take) throws JSONException, IOException{
			
		this.authorsWhoseFollowersMentionedSubquery(al, when, back, users, minTimes);
		if(byId!=null) { // if byId is present, it's got priority over byHits
			boolean b = Boolean.parseBoolean(byId);
			this.authors.rankedById(b);
		}
		else {
			this.authors.rankedByHits(byHits);
		}
		if(take>0) {
			this.authors.take(take);
		}
		this.authors = this.query.answer();
		
		List<Long> result = new ArrayList<Long>();
		for(Author a : this.authors.getAuthors())
			result.add(a.getId());
		String queryString = "Users whose followers mentioned" + 
						" at least: " + al + " user";
		if(al>1)
			queryString += "s";
		queryString += " when: " + when;
		if(back>0)
			queryString += " (" + back + ")";
		queryString += " no less than " + minTimes + " time";
		if(minTimes>1)
			queryString += "s";
		queryString += " amongst: " + users;
		
		JSONObject response = formatResponse(queryString, this.authors);
		if(this.authors.size()==0)
			return Response.status(Response.Status.NOT_FOUND).entity(response).build();
		return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
	}
	
	private void authorsThatMentionedSubquery(int atLeast, int minTimes, String when, int back, String users) {
		Mention[] mentions = this.mentionsFromString(users);
		switch(when) {
			case "last_month":
				this.authors.thatMentioned(new LastMonth(), new AtLeast(atLeast), new AtLeastTimes(minTimes), mentions);
				break;
			case "very_last_month":
				this.authors.thatMentioned(new LastMonthFromNow(), new AtLeast(atLeast), mentions);
				break;
			case "very_last_year":
				this.authors.thatMentioned(new LastYearFromNow(), new AtLeast(atLeast), mentions);
				break;
			case "last_year":
				this.authors.thatMentioned(new LastYear(), new AtLeast(atLeast), new AtLeastTimes(minTimes), mentions);
				break;
			case "this_year":
				this.authors.thatMentioned(new ThisYear(), new AtLeast(atLeast), new AtLeastTimes(minTimes), mentions);
				break;
			case "months_ago":
				this.authors.thatMentioned(new MonthsAgo(back), new AtLeast(atLeast),
						new AtLeastTimes(minTimes), mentions);
				break;
			case "weeks_ago":
				this.authors.thatMentioned(new WeeksAgo(back), new AtLeast(atLeast),
						new AtLeastTimes(minTimes), mentions);
				break;
		}
	}
	
	private void authorsMentionedSubquery(int minTimes, String when, int back, String users) {
		Mention[] mentions = this.mentionsFromString(users);
		switch(when) {
			case "last_month":
				this.authors.mentioned(new LastMonth(), new AtLeastTimes(minTimes), mentions);
				break;
			case "very_last_month":
				this.authors.mentioned(new LastMonthFromNow(), new AtLeastTimes(minTimes), mentions);
				break;
			case "very_last_year":
				this.authors.mentioned(new LastYearFromNow(), new AtLeastTimes(minTimes), mentions);
				break;
			case "last_year":
				this.authors.mentioned(new LastYear(), new AtLeastTimes(minTimes), mentions);
				break;
			case "this_year":
				this.authors.mentioned(new ThisYear(), new AtLeastTimes(minTimes), mentions);
				break;
			case "months_ago":
				this.authors.mentioned(new MonthsAgo(back), new AtLeastTimes(minTimes), mentions);
				break;
			case "weeks_ago":
				this.authors.mentioned(new WeeksAgo(back), new AtLeastTimes(minTimes), mentions);
				break;
		}
	}
	
	private void authorsWhoFollowSubquery(int atLeast, String users) {
		Author[] auths = authorsFromString(users);
		this.authors.whoFollow(new AtLeast(atLeast), auths);
	}
	
	private void authorsWhoseFollowersFollowSubquery(String users) {
		Author[] followed = authorsFromString(users);
		this.authors.whoseFollowersFollow(followed);
	}
	
	private void authorsWhoseFollowersAreFollowedBySubquery(String users) {
		Author[] followers = authorsFromString(users);
		this.authors.whoseFollowersAreFollowedBy(followers);
	}
	
	private void authorsWhoseFollowersMentionedSubquery(int atLeast, String when, 
										int back, String users, int minTimes) {
		
		Mention[] mentions = mentionsFromString(users);
		switch(when){
			case "last_month":
				this.authors.whoseFollowersMentioned(new LastMonth(), new AtLeast(atLeast),
						new AtLeastTimes(minTimes), mentions);
				break;
			case "last_year":
				this.authors.whoseFollowersMentioned(new LastYear(), new AtLeast(atLeast),
						new AtLeastTimes(minTimes), mentions);
				break;
			case "this_month":
				this.authors.whoseFollowersMentioned(new ThisYear(), new AtLeast(atLeast),
						new AtLeastTimes(minTimes), mentions);
				break;
			case "months_ago":
				this.authors.whoseFollowersMentioned(new MonthsAgo(back), new AtLeast(atLeast),
						new AtLeastTimes(minTimes), mentions);
				break;
			case "weeks_ago":
				this.authors.whoseFollowersMentioned(new WeeksAgo(back), new AtLeast(atLeast),
						new AtLeastTimes(minTimes), mentions);
				break;
		}
	}
	
	private Author[] authorsFromString(String users) {
		String[] splitted = users.split(",");
		Author[] authors = new Author[splitted.length];
		for(int i=0; i<splitted.length; i++) {
			authors[i] = new Author(Long.parseLong(splitted[i]));
		}
		return authors;
	}
	
	private Mention[] mentionsFromString(String users) {
		String[] splitted = users.split(",");
		Mention[] mentions = new Mention[splitted.length];
		for(int i=0; i<splitted.length; i++) {
			mentions[i] = new Mention(Long.parseLong(splitted[i]));
		}
		return mentions;
	}
	
	private JSONObject formatResponse(String queryMessage, Authors authors) throws IOException, JSONException {
		
		JSONObject response = new JSONObject()
									.put("query", queryMessage);
		for(Author a : authors.getAuthors()) {
			JSONObject user = new JSONObject();
			user.put("id", a.getId());
			user.put("hits", a.getHits());
			response.accumulate("users", user);
		}
		return response;
	}
 
}