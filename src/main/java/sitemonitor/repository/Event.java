package sitemonitor.repository;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

@Entity
public class Event implements Serializable {
	private static final long serialVersionUID = 2632067586747574697L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	private String description;
	private Date eventTime;
	private String state;
	private long responseTime;
	private String statusChange = "N";
	
	@ManyToOne
    @JoinColumn(name="SITE_ID", nullable=false)
	private Site site;
	
	public Event() {
	}
	
	public String getSiteName() {
		if (site == null) {
			return "";
		}
		return site.getName();
	}
	
	public String getEventTimeDisplay() {
		return DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(new DateTime(eventTime));
	}

	public String getChartTimeDisplay() {
		return DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ssaa").print(new DateTime(eventTime));
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getEventTime() {
		return eventTime;
	}

	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public String getStatusChange() {
		return statusChange;
	}

	public void setStatusChange(String statusChange) {
		this.statusChange = statusChange;
	}
	
}
