/**********************************************************************************
 * $URL$
 * $Id$
 **********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005 The Regents of the University of Michigan, Trustees of Indiana University,
 *                  Board of Trustees of the Leland Stanford, Jr., University, and The MIT Corporation
 * 
 * Licensed under the Educational Community License Version 1.0 (the "License");
 * By obtaining, using and/or copying this Original Work, you agree that you have read,
 * understand, and will comply with the terms and conditions of the Educational Community License.
 * You may obtain a copy of the License at:
 * 
 *      http://cvs.sakaiproject.org/licenses/license_1_0.html
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 **********************************************************************************/

package org.sakaiproject.component.app.dissertation;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sakaiproject.api.app.dissertation.CandidateInfoEdit;
import org.sakaiproject.api.app.dissertation.CandidatePath;
import org.sakaiproject.api.app.dissertation.CandidatePathEdit;
import org.sakaiproject.api.app.dissertation.Dissertation;
import org.sakaiproject.api.app.dissertation.StepStatus;
import org.sakaiproject.api.app.dissertation.StepStatusEdit;
import org.sakaiproject.api.app.dissertation.cover.DissertationService;
import org.sakaiproject.api.kernel.session.Session;
import org.sakaiproject.api.kernel.session.cover.SessionManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.service.framework.log.Logger;
import org.sakaiproject.service.legacy.time.Time;
import org.sakaiproject.service.legacy.time.TimeBreakdown;
import org.sakaiproject.service.legacy.time.cover.TimeService;
import org.sakaiproject.service.legacy.user.cover.UserDirectoryService;
import org.sakaiproject.util.java.StringUtil;
import org.sakaiproject.util.web.Web;

/**
 * <p>
 * Uses Rackham Graduate School institutional data to create new CandidatePath
 * objects and set the status of StepStatus objects.
 * </p>
 * 
 * @author University of Michigan, Sakai Software Development Team
 * @version $Revision: $
 */
public class UploadExtractsJobImpl implements UploadExtractsJob
{
	private Logger m_logger = null;
	private String jobName = null;
	private StringBuffer buf = new StringBuffer();
	private JobDetail jobDetail = null;
	private JobDataMap dataMap = null;
	private JobAnnouncement announcement = null;
	private Calendar cal = Calendar.getInstance();
	private SimpleDateFormat formatter = new SimpleDateFormat(DissertationService.STEP_JOB_DATE_FORMAT);
	static final String NEWLINE = Web.escapeHtmlFormattedText("<br/>");
	static final String START_ITALIC = Web.escapeHtmlFormattedText("<i>");
	static final String END_ITALIC = Web.escapeHtmlFormattedText("</i>");
	
	//key = emplid value = uniqname
	private Hashtable ids = new Hashtable();
	
	//collection of OARD records
	List OARDRecords = new Vector();
	
	//collection of MP records
	List MPRecords = new Vector();
	
	//flag set if there is a validation error
	private boolean vErrors = false;
	
	//execution parameters
	private String m_currentUser = null;
	
	//private String m_currentSite = null;
	private String[] m_oardRecords;
	private String[] m_mpRecords;
	
	//TODO get from ServerConfiguration
	//private String m_schoolSite = null;
	private String m_musicPerformanceSite = null;
	
	/** Holds the School administrative Block Grant Group numbers. */
	protected Hashtable m_schoolGroups = null;
	
	/** regular expressions used in data validation **/
	Matcher matcher = null;
	
	//general patterns
	private static Pattern  m_patternDate = Pattern.compile("(^([0-9]|[0-9][0-9])/([0-9]|[0-9][0-9])/[0-9]{4}$|)");
	
	//required (not null) data common to both database extracts
	private static Pattern m_patternUMId = Pattern.compile("^\"[0-9]{8}\"$");
	private static Pattern m_patternCampusId = Pattern.compile("(^\"[A-Za-z0-9]{1,8}\"\r?$)|(^\".+@.+\"\r?$)");
	 
	//MPathways fields
	private static Pattern m_patternAcadProg = Pattern.compile("(^\"[0-9]{5}\"$|^\"\"$)");
	private static Pattern m_patternAnticipate = Pattern.compile("(^\"[A-Z]{2}[- ][0-9]{4}\"$|^\"[A-Za-z]*( |,)[0-9]{4}\"$|^\"\"$)");
	private static Pattern m_patternDateCompl = Pattern.compile("(^([0-9]|[0-9][0-9])/([0-9]|[0-9][0-9])/[0-9]{4}$|)");
	private static Pattern m_patternMilestone = Pattern.compile("(^\"[A-Za-z]*\"$|^\"\"$)");
	private static Pattern m_patternAcademicPlan = Pattern.compile("(^\"[0-9]{4}[A-Z0-9]*\"|^\"[0-9]{4}[A-Z0-9]*\"\r?$|^\"\"$|^\"\"\r?$)");
		
	//Rackham OARD database fields
	private static Pattern 	m_patternFOS = Pattern.compile("(^\"[0-9]{4}\"$|^\"\"$)");
	private static Pattern 	m_patternDegreeTermTrans = Pattern.compile("(^\"[A-Za-z]{2}( |-)[0-9]{4}\"$|^\"\"$)");
	private static Pattern 	m_patternOralExamTime = Pattern.compile("(^\".*\"$|^\"\"$)"); //not restrictive
	private static Pattern	m_patternOralExamPlace = Pattern.compile("(^\".*\"$|^\"\"$)"); //not restrictive
	private static Pattern 	m_patternRole = Pattern.compile("(^\".*\"$|^\"\"$|^\"#EMPTY\"$)"); //not restrictive
	private static Pattern  m_patternMember = Pattern.compile("(^\".*\"$|^\"\"$|^\"#EMPTY\"$)"); //not restrictive
	private static Pattern 	m_patternEvalDate = Pattern.compile("(^\"([0-9]|[0-9][0-9])/([0-9]|[0-9][0-9])/([0-9]{4} 0:00)\"$|^\"([0-9]|[0-9][0-9])/([0-9]|[0-9][0-9])/([0-9]{4} 0:00)\"\r$|^\"#EMPTY\"$|^\"\"\r$|)");

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException 
	{
		//execute() is the main method of a Quartz job
		try
		{
			//Spring injection of Logger was getting lost when Quartz instantiated job
			m_logger = org.sakaiproject.service.framework.log.cover.Logger.getInstance();
			if(m_logger == null)
				System.out.println(this + ".execute() couldn't get a logger");
			
			//TODO set user to current user
			Session s = SessionManager.getCurrentSession();
			if (s != null)
				s.setUserId("admin");
			else
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() could not setUserId to admin");
		
			//get the job detail
			jobDetail = context.getJobDetail();
			jobName = jobDetail.getName();
			dataMap = jobDetail.getJobDataMap();
			if(jobDetail == null)
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() jobDetail is null");
			
			//make sure we can report on the job's execution
			announcement = new JobAnnouncement(jobDetail);
			if(announcement == null)
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() could not get instance of JobAnnouncement");
			
			//set job execution parameters
			if(!setJobExecutionParameters(context))
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() could not get all required job execution parameters");
			
			//TODO make sure we have permission
			
			//TODO methods to
			//validate String[]'s
			//set CandidateInfo's
			//set StepStatus's
		
			//note job is starting
			buf.append(getTime() + " JOB NAME: " + jobName + " - START" + NEWLINE);
				
			if(m_logger.isInfoEnabled())
				m_logger.info(getTime() + " " + jobName + " - START");
			
			//one or two extract files might have been uploaded
			
			String validErrors = null;
			
			//validate the String[]'s passed as job detail data
			if(m_oardRecords != null && m_oardRecords.length > 1)
			{
				validErrors = createOARDRecords(m_oardRecords);
				if(validErrors != null && validErrors.length()!=0)
				{
					vErrors = true;
					buf.append(validErrors);
				}
			}
			if(m_mpRecords != null && m_mpRecords.length > 1)
			{
				validErrors = createMPRecords(m_mpRecords);
				if(validErrors != null && validErrors.length()!=0)
				{
					vErrors = true;
					buf.append(validErrors);
				}
			}
			
			//if there are no validation errors, convert and use the data
			if(!vErrors)
			{
				//if we have no ids at this point bail out
				if(!ids.elements().hasMoreElements())
				{
					//note exception in job execution report
					buf.append(getTime() + " " + jobName + " exception - no employee id-uniquename map" + NEWLINE);
					
					if(m_logger.isWarnEnabled())
						m_logger.warn(getTime() + " " + jobName + " exception - no employee id-uniquename map");
				}
				
				/** for each id in ids, use data to initialize 
				 * and update CandidateInfo, add to Collection
				 * and at end pass Collection to dumpData(data)
				 * returning alerts etc.*/
				
				String loadErrors = null;
				
				//load the extract data to update path step status
				loadErrors = queryLists(OARDRecords, MPRecords, ids);
				
				//there were load errors
				if(loadErrors != null && loadErrors.length()!=0)
				{
					buf.append(loadErrors);
				}
			}
			else
			{	
				//there were validation errors
				buf.append(getTime() + " JOB NAME: " + jobName + " - data validation errors" + NEWLINE);
			}
		}
		catch(Exception e)
		{
			//note exception in job execution report
			buf.append(getTime() + " JOB NAME: " + jobName + " exception - " + NEWLINE + e  + NEWLINE);
			
			if(m_logger.isWarnEnabled())
				m_logger.warn(getTime() + " " + jobName + " exception - " + e  + NEWLINE);
		}
		finally
		{
			//add ids to job report
			if(ids != null && !ids.isEmpty())
			{
				buf.append("ids of students in upload:" + NEWLINE);
				buf.append(ids.toString() + NEWLINE);
			}
			
			//note that job is done
			buf.append(getTime() + " JOB NAME: " + jobName + " - DONE" + NEWLINE);
			
			if(m_logger.isInfoEnabled())
				m_logger.info(getTime() + " " + jobName + " - DONE");
			
			//send report of job execution to Announcements
			String announce = buf.toString();
			try
			{
				if(announce != null && !announce.equals(""))
					announcement.addAnnouncementMessage(announce);
			}
			catch(Exception e)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() addAnnouncementMessage " + e);
			}
			
			//clean up db lock at the end
			try
			{
				CandidateInfoEdit lock = DissertationService.editCandidateInfo(DissertationService.IS_LOADING_LOCK_REFERENCE);
				if(lock != null && lock.isActiveEdit())
					DissertationService.removeCandidateInfo(lock);
			}
			catch(Exception e)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() removeCandidateInfo(lock) " + e);

				buf.append(getTime() + " JOB NAME: " + jobName + " exception removing lock " + e + NEWLINE);
			}
		}
	}//execute
	
	protected boolean setJobExecutionParameters(JobExecutionContext context)
	{
		//m_schoolSite = DissertationService.getSchoolSite();
		m_musicPerformanceSite = DissertationService.getMusicPerformanceSite();
		
		//get job execution parameters from job data map
		m_currentUser = (String)dataMap.get("CURRENT_USER");
		m_schoolGroups = (Hashtable)dataMap.get("SCHOOL_GROUPS");
		m_oardRecords = (String[])dataMap.get("OARD_RECORDS");
		m_mpRecords = (String[])dataMap.get("MP_RECORDS");
		//m_currentSite = (String)dataMap.get("CURRENT_SITE");
		
		//check that not-null parameters are not null
		if(m_currentUser == null || m_schoolGroups == null)
		{
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".setJobExecutionParameters " + jobName +
					" - one or more required job execution parameters null");
			return false;
		}
		return true;
		
	}//setJobExecutionParameters
	
	/**
	* Access the alphabetical candidate chooser letter for this student. 
	* @param chefid – The user's id.
	* @return The alphabetical candidate chooser letter, A-Z, or "".
	*/
	public String getSortLetter(String chefId)
	{
		String retVal = "";
		if(chefId != null)
		{
			try
			{
				String sortName = UserDirectoryService.getUser(chefId).getSortName();
				if(sortName != null)
				{
					retVal = sortName.substring(0,1).toUpperCase();
				}
			}
			catch(IdUnusedException e) 
			{
				m_logger.warn(this + ".getSortLetter for " + chefId + " IdUnusedException " + e.toString());
			}
			catch(Exception e)
			{
				m_logger.warn(this + ".getSortLetter for " + chefId + " Exception " + e.toString());
			}
		}
		return retVal;
		
	}//getSortLetter
	
	protected String getTime()
	{
		String now = null;
		long millis = System.currentTimeMillis();
		cal.setTimeInMillis(millis);
		now = formatter.format(cal.getTime());
		return now;
	}
	
	/**
	* Create a Time object from the raw time string.
	* Based on DissertationDataListenerService.parseOracleTimeString()
	* @param timeString The raw oracle time string.
	* @return The CHEF Time object.
	*/
	protected Time parseTimeString(String timeString)
	{
		Time retVal = null;
		if(timeString != null && !timeString.equals(""))
		{
			int year = 0;
			int month = 0;
			int day = 0;
			String[] parts = timeString.split("/");
			String[] yearParts = new String[2];
			for(int x = 0; x < parts.length; x++)
			try
			{
				if(parts[2].indexOf(" ")!= -1)
				{
					//"1/11/2006 0:00"
					yearParts = parts[2].split(" ");
					year = Integer.parseInt(yearParts[0]);
				}
				else
					year = Integer.parseInt(parts[2]);
			}
			catch(NumberFormatException nfe)
			{
				m_logger.warn("DISSERTATION : SERVICE: PARSE TIME STRING : YEAR : NumberFormatException  " + timeString);
			}
			
			try
			{
				month = Integer.parseInt(parts[0]);
			}
			catch(NumberFormatException nfe)
			{
				m_logger.warn("DISSERTATION : SERVICE: PARSE TIME STRING : MONTH : NumberFormatException  " + timeString);
			}
			
			try
			{
				day = Integer.parseInt(parts[1]);
			}
			catch(NumberFormatException nfe)
			{
				m_logger.warn("DISSERTATION : SERVICE: PARSE TIME STRING : DAY : NumberFormatException  " + timeString);
			}
			TimeBreakdown tb = TimeService.newTimeBreakdown(year, month, day, 0, 0, 0, 0);
			retVal = TimeService.newTimeLocal(tb);
		}
		return retVal;

	}//parseTimeString
	
	/**
	*
	* Parse and validate a Rackham OARD data extract file
	* @param String[] lns - lines read from the OARD extract data file
	* @return rv - a Vector of String error messages
	*/
	private String createOARDRecords(String[] lns)
	{
		String message = "";
		String prefix = "";
		int lineNumber = 0;
		StringBuffer bufO = new StringBuffer();

		for (int i = 0; i < lns.length; i++)
		{
			try
			{
				//skip last line which contains a single hex value 0x1A
				if(lns[i].length() > 1)
				{
					//get the fields
					String[] flds = lns[i].split(",");
					
					lineNumber = i + 1;
					prefix = "Source: OARD File: Location: line " + lineNumber + ", field ";
					message = "";
						
					//check that we have the right number of fields
					if((flds.length==15))
					{
						OARDRecord oard = new OARDRecord();
						
						//* 1 ¦  Emplid   ¦  A8 - Student's emplid
						matcher = m_patternUMId.matcher(flds[0]);
						if(!matcher.matches())
						{
							vErrors = true;
							bufO.append(prefix + "1  Explanation: umid = " + flds[0] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							oard.m_umid = flds[0].substring(1,flds[0].length()-1);
						}
							
						//* 2 ¦  Fos      ¦  A4    - Students field of study code
						matcher = m_patternFOS.matcher(flds[1]);
						if(!matcher.matches())
						{
							vErrors = true;
							bufO.append(prefix + "2  Explanation:  fos = " + flds[1] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							oard.m_fos = flds[1].substring(1,flds[1].length()-1);
						}
							
						//we are not checking lname or fname - these come from UMIAC
						
						//* 3 ¦  Lname    ¦  A25 - Students last name
						//strip quotation marks from quoted field
						oard.m_lname = flds[2].substring(1,flds[2].length()-1);
						
						//* 4 ¦  Fname    ¦  A30 - Students first name
						//strip quotation marks from quoted field
						oard.m_fname = flds[3].substring(1,flds[3].length()-1);
						
						//* 5 ¦  Degterm trans   ¦  A7 - Students degree term as TT-CCYY (e.g. FA-2003)
						matcher = m_patternDegreeTermTrans.matcher(flds[4]);
						if(!(flds[4] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "5  Explanation: degreeterm_trans = " + flds[4] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							oard.m_degreeterm_trans = flds[4].substring(1,flds[4].length()-1);
						}
							
						//* 6 ¦  Oral exam date  ¦  D - Date of oral defense
						matcher = m_patternDate.matcher(flds[5]);
						if(!(flds[5] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "6  Explanation: oral_exam_date_time = " + flds[5] + NEWLINE);
						}
						else
						{
							oard.m_oral_exam_date = flds[5];
						}
							
						//* 7 ¦  Oral exam time  ¦  A7 - Time of oral defense
						matcher = m_patternOralExamTime.matcher(flds[6]);
						if(!(flds[6] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "7  Explanation: oral_exam_time = " + flds[6] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							oard.m_oral_exam_time = flds[6].substring(1,flds[6].length()-1);
						}
							
						//* 8 ¦  Oral exam place ¦  A25 - Place of oral defense
						matcher = m_patternOralExamPlace.matcher(flds[7]);
						if(!(flds[7] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "8  Explanation: oral_exam_place = " + flds[7] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							oard.m_oral_exam_place = flds[7].substring(1,flds[7].length()-1);
						}
							
						//* 9 ¦  Committee approved date ¦  D - date committee was approved
						matcher = m_patternDate.matcher(flds[8]);
						if(!(flds[8] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "9  Explanation: committee_approved_date = " + flds[8] + NEWLINE);
						}
						else
						{
							oard.m_committee_approved_date = flds[8];
						}
							 
						//*10 ¦  First format date  ¦  D - date of pre defense meeting in Rackham
						matcher.reset(flds[9]);
						if(!(flds[9] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "10  Explanation: first_format_date = " + flds[9] + NEWLINE);
						}
						else
						{
							oard.m_first_format_date = flds[9];
						}
						 
						//*11 ¦  Oral report return date ¦  D
						matcher.reset(flds[10]);
						if(!(flds[10] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "11  Explanation: oral_report_return_date = " + flds[14] + NEWLINE);
						}
						else
						{
							oard.m_oral_report_return_date = flds[10];
						}

						//*12 ¦  Degree conferred date ¦  D - date the degree was conferred in OARD system
						matcher.reset(flds[11]);
						if(!(flds[11] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "12  Explanation: degree_conferred_date = " + flds[21] + NEWLINE);
						}
						else
						{
							oard.m_degree_conferred_date = flds[11];	
						}
						
						//*13 ¦  Update date  ¦  D - date record was last modified
						matcher = m_patternDate.matcher(flds[12]);
						if(!(flds[12] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "13  Explanation: update_date = " + flds[23] + NEWLINE);
						}
						else
						{
							oard.m_update_date = flds[12];	
						}
							
						//*14 ¦  Comm cert date  ¦  D -
						matcher.reset(flds[13]);
						if(!(flds[13] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "14  Explanation: comm_cert_date = " + flds[24] + NEWLINE);
						}
						else
						{
							oard.m_comm_cert_date = flds[13];
						}
							 
						//*15 ¦  Campus id  ¦  A1-A8 - uniqname
						matcher = m_patternCampusId.matcher(flds[14]);
						if(!(flds[14] == null || matcher.matches()))
						{
							vErrors = true;
							bufO.append(prefix + "15  Explanation: campus_id = " + flds[28] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							if(flds[14].indexOf("\r") != -1)
							{
								oard.m_campus_id = flds[14].substring(1,flds[14].length()-2);
							}
							else
							{
								oard.m_campus_id = flds[14].substring(1,flds[14].length()-1);
							}
							
						}
							
						//check that there is a group roll-up code that matches this field of study value
						String field_of_study = flds[1].substring(1,flds[1].length()-1);
						String rollup = getProgram(field_of_study);
						
						//no matching group roll-up code
						if(rollup==null)
						{
							vErrors = true;
							bufO.append(prefix + "1  Explanation: fos " + field_of_study + " does not match an existing group roll-up code.");
						}
						if(!vErrors)
						{
							//add a record
							OARDRecords.add(oard);
							
							//add a uniqname
							ids.put(oard.m_umid, oard.m_campus_id);
						}
					}
					else
					{
						//number of fields exception
						lineNumber = i + 1;
						message = "Source: OARD File: Location: line " + lineNumber + " Explanation: has " 
							+ flds.length + " fields: 15 expected." + NEWLINE;
						if(m_logger.isInfoEnabled())
							m_logger.info(this + ".validateOARD: " + message);
						bufO.append(message);
					}
				}//contains a single hex value 0x1A
			}
			catch (Exception e)
			{
				message = "Source: OARD File: Explanation: unexpected problem: "  + e.getMessage();
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".validateOARD: " + message);
				bufO.append(message);
				
				//keep going
				continue;
			}
		}//for each line
	
		return new String(bufO.toString());
		
	}//validateOARD
	
	/**
	*
	* Parse and validate a Rackham MP data extract file
	* @param String[] lns - lines read from the MP data extract file
	* @return String - validation errors
	*/
	private String createMPRecords(String[] lns)
	{
		boolean replace = true;
		String field_of_study = "";
		String message = "";
		String prefix = "";
		int lineNumber = 0;
		StringBuffer bufM = new StringBuffer();
		
		//for each line in the MPathways extract file
		for (int i = 0; i < lns.length; i++)
		{
			try
			{
				//skip last line which might contain a single hex 0x1A
				if(lns[i].length() > 1)
				{
					//replace commas used as field separators, but leave commas within quoted fields
					StringBuffer bufL = new StringBuffer(lns[i]);
					for (int j = 0; j < ((String) lns[i]).length(); j++)
					{
						char ch = lns[i].charAt(j);
						if(ch == '"') 
							replace = !replace;
						if(ch == ',')
						{
							if(replace) 
								bufL.setCharAt(j, '%');
							else
								bufL.setCharAt(j, ch);
						}
						else
							bufL.setCharAt(j, ch);
					}
						
					String line = bufL.toString();
					bufL.setLength(0);
									
					//get the fields
					String[] flds = line.split("[%]");
					
					lineNumber = i + 1;
					prefix = "Source: MP File: Location: line " + lineNumber + ", field ";
					message = "";

					//check that we have the right number of fields
					if(flds.length == 10)
					{
						MPRecord mp = new MPRecord();
						
						//* 1 ¦  Emplid   ¦  A8 - Student's emplid
						matcher = m_patternUMId.matcher(flds[0]);
						if(!matcher.matches())
						{
							vErrors = true;
							bufM.append(prefix + "1  Explanation: umid = " + flds[0] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted fields
							mp.m_umid = flds[0].substring(1,flds[0].length()-1);
						}
						
						//* 2 ¦  Acad_prog ¦  A9	| Academic Program Code
						matcher = m_patternAcadProg.matcher(flds[1]);
						if(!matcher.matches())
						{
							vErrors = true;
							bufM.append(prefix + "2  Explanation: acad_prog = " + flds[1] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							mp.m_acad_prog = flds[1].substring(1,flds[1].length()-1);
						}
						
						//* 3 ¦  Anticipate_Anticipate_1 ¦  A15	| Adv to cand term code
						matcher = m_patternAnticipate.matcher(flds[2]);
						if(!(flds[2] == null || matcher.matches()))
						{
							vErrors = true;
							bufM.append(prefix + "3  Explanation: anticipate = " + flds[2] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted fields
							mp.m_anticipate = flds[2].substring(1,flds[2].length()-1);
						}
						
						//* 4 ¦  Date_compl ¦  D	| Date milestone was completed 
						matcher = m_patternDateCompl.matcher(flds[3]);
						if(!(flds[3] == null || matcher.matches()))
						{
							vErrors = true;
							bufM.append(prefix + "4  Explanation:  date_compl = " + flds[3] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							mp.m_date_compl = StringUtil.trimToNull(flds[3]);
						}
						
						//* 5 ¦  Milestone ¦  A10	| name of milestone PRELIM or ADVCAND or DISSERT
						matcher = m_patternMilestone.matcher(flds[4]);
						if(!(flds[4] == null || matcher.matches()))
						{
							vErrors = true;
							bufM.append(prefix + "5  Explanation: milestone = " + flds[4] + NEWLINE);
						}
						else
						{
							//strip quotation marks from quoted field
							mp.m_milestone = flds[4].substring(1,flds[4].length()-1);
						}
						
						//* 6 | Academic plan |  A4	| Field of study and degree (e.g. 1220PHD1)
						matcher = m_patternAcademicPlan.matcher(flds[5]);
						if(!matcher.matches())
						{
							vErrors = true;
							bufM.append(prefix + "6  Explanation: academic_plan = " + flds[5] + NEWLINE);
						}
						else
						{
							//get the field of study part of the academic plan for roll-up
							mp.m_academic_plan = flds[5].substring(1,flds[5].length()-1);
							
							//get the field of study part of the academic plan for roll-up
							field_of_study = flds[5].substring(1,5);
						}

						if(flds[4].equalsIgnoreCase("\"DISSERT\""))
						{
							//* 7 | Committee role
							matcher = m_patternRole.matcher(flds[6]);
							if(!matcher.matches())
							{
								vErrors = true;
								bufM.append(prefix + "7  Explanation: role = " + flds[25] + NEWLINE);
							}
							else
							{
								//strip quotation marks from quoted field
								mp.m_role = flds[6].substring(1,flds[6].length()-1);
								if(mp.m_role.equals("#EMPTY"))
									mp.m_role = "";
							}
							
							//* 8 | Committee member name
							matcher = m_patternMember.matcher(flds[7]);
							if(!matcher.matches())
							{
								vErrors = true;
								bufM.append(prefix + "8  Explanation: member = " + flds[26] + NEWLINE);
							}
							else
							{
								//strip quotation marks from quoted field
								mp.m_member = flds[7].substring(1,flds[7].length()-1);
								if(mp.m_member.equals("#EMPTY"))
									mp.m_member = "";
							}
							
							//* 9 | Eval_recvd, e.g., "12/16/2005 0:0" "1/4/2006 0:00"
							matcher = m_patternEvalDate.matcher(flds[8]);
							if(!matcher.matches())
							{
								vErrors = true;
								bufM.append(prefix + "9  Explanation: eval_date = " + flds[8] + NEWLINE);
							}
							else
							{
								//strip quotation marks from quoted field
								mp.m_eval_date = flds[8].substring(1,flds[8].length()-1);
								if(mp.m_eval_date.equals("#EMPTY"))
									mp.m_eval_date = "";
							}
						}
						
						//*10  | Campus id  | A1-A8   | uniqname
						matcher = m_patternCampusId.matcher(flds[9]);
						if(!matcher.matches())
						{
							vErrors = true;
							bufM.append(prefix + "10  Explanation: campus_id = " + flds[9] + NEWLINE);
						}
						else
						{
							if(flds[9].indexOf("\r") != -1)
								mp.m_campus_id = flds[9].substring(1,flds[9].length()-2);
							else
								mp.m_campus_id = flds[9].substring(1,flds[9].length()-1);
						}
							
						//check that there is a group roll-up code that matches this field of study value
						String rollup = getProgram(field_of_study);
						
						//no matching group roll-up code
						if(rollup == null)
						{
							bufM.append(prefix + "1  Explanation: field of study " + field_of_study + 
									" does not match an existing group roll-up code."  + NEWLINE);
							vErrors = true;
						}
						//add a record
						if(!vErrors)
						{
							MPRecords.add(mp);
						
							//add a uniqname
							ids.put(mp.m_umid, mp.m_campus_id);
						}
					}
					else
					{
						//alert that there is not the right number of fields
						vErrors = true;
						lineNumber = i + 1;
						message = "Source: MP File: Location: line " + lineNumber + " Explanation: has " 
							+ flds.length + " fields: 10 expected." + NEWLINE;
						if(m_logger.isInfoEnabled())
							m_logger.info(this + ".validateMP: " + message);
						bufM.append(message  + NEWLINE);
					}
				}//not a line with a single hex 0x1A
			}
			catch (Exception e)
			{
				message = "Source: MP File: Explanation: unexpected problem with data: "  + e;
				if(m_logger.isWarnEnabled())
						m_logger.warn(this + ".createMPRecord: " + message);
				bufM.append(message + NEWLINE + e + NEWLINE);
				
				//keep going
				continue;
			}
		}//for each line
	
		return new String(bufM.toString());
		
	}//createMPRecord
	
	/**
	* Use Rackham data to initialize and update CandidateInfo.
	* Based on DissertationDataListenerService.queryDatabase().
	* @param OARDRecs - Vector of OARDRecords
	* @param  MPRecs - Vector of MPRecords
	* @param  ids - Hashtable of umid keys and chefid elements
	* @return rv - a Vector of error messages
	*/
	private String queryLists(List OARDRecords, List MPRecords, Hashtable ids)
	{
		Vector data = new Vector();
		List OARDrecs = new Vector();
		List MPrecs = new Vector();
		String oneEmplid = null;
		String chefId = null;
		String program = null;
		String msg = null;
		String committeeEvalCompleted = null;
		String name = "";
		CandidateInfoEdit infoEdit = null;
		boolean commitEdit = false, existsOARDrecs = false, existsMPrecs = false;
		Map membersEvals = new TreeMap();
		Collection requiredFrom = new Vector();
		StringBuffer bufQ = new StringBuffer();
		
		//get all the emplids in the upload
		Enumeration emplids = ids.keys();
		if(emplids != null)
		{
			//for each emplid
			while(emplids.hasMoreElements())
			{
				commitEdit = false;
				committeeEvalCompleted = "";
				
				//get student's emplid
				oneEmplid = (String)emplids.nextElement();
				
				//clear recs for student between emplids
				OARDrecs.clear();
				MPrecs.clear();
				
				//if there are MP records
				if((MPRecords!=null) && (MPRecords.size() > 0))
				{
					for(ListIterator i = MPRecords.listIterator(); i.hasNext(); )
					{
						//get all the MP recs for this student
						MPRecord rec = (MPRecord) i.next();
						if(rec.m_umid.equals(oneEmplid)) 
							MPrecs.add(rec);
					}
					
					//set flag for MP recs for this student
					if((MPrecs != null) && (MPrecs.size() > 0))
						existsMPrecs = true;
					else
						existsMPrecs = false;
				}
				
				//if there are OARD records
				if((OARDRecords != null) && (OARDRecords.size() > 0))
				{
					for(ListIterator i = OARDRecords.listIterator(); i.hasNext(); )
					{
						//get all the OARD recs for this student
						OARDRecord rec = (OARDRecord) i.next();
						if(rec.m_umid.equals(oneEmplid)) 
							OARDrecs.add(rec);
					}
					
					//set flag for OARD recs for this student
					if((OARDrecs != null) && (OARDrecs.size() > 0)) 
						existsOARDrecs = true;
					else
						existsOARDrecs = false;
				}
				try
				{
					//get CandidateInfo for this student
					infoEdit = DissertationService.getCandidateInfoEditForEmplid(oneEmplid);
					if(infoEdit == null)
					{
						//there is no CandidateInfo for this student, so create one
						infoEdit = DissertationService.addCandidateInfo(jobName);
						//infoEdit = addCandidateInfoFromListener("datalistener");
						infoEdit.setEmplid(oneEmplid);
						
						//set flag to save it later
						commitEdit = true;
					}
					
					//check for uniqname
					try
					{
						chefId = infoEdit.getChefId();
						if(chefId.equals(""))
						{
							chefId = (String)ids.get(oneEmplid);
							if(chefId != null)
								infoEdit.setChefId(chefId.toLowerCase());
							
							//set flag to save it later
							commitEdit = true;
						}
					}
					catch(Exception e)
					{
						msg = "// CHECK FOR CHEF ID " + oneEmplid + NEWLINE + e.getMessage();
						bufQ.append(msg + NEWLINE);
						m_logger.warn(this + ".queryLists()  " + msg);
						
						//keep going
						continue;
					}
			
					//check for student's Field of Study
					try
					{
						//try to get it from CandidateInfo
						program = infoEdit.getProgram();
						
						//if we didn't get it
						if(program == null || program.equals(""))
						{
							//if we have OARD data for this student, try there
							if(existsOARDrecs)
							{
								//for each OARD rec for this student
								for(ListIterator i = OARDrecs.listIterator(); i.hasNext(); )
								{
									OARDRecord rec = (OARDRecord) i.next();
									if(rec.m_fos != null) 
										program = rec.m_fos;
								}
							}
							if(program != null)
							{
								//set student's program
								infoEdit.setProgram(getProgram(program));
								infoEdit.setParentSite("diss" +  getProgram(program));
							}
							else if(existsMPrecs)
							{
								//otherwise try getting it from the Field of Study part of Academic Plan
								for(ListIterator i = MPrecs.listIterator(); i.hasNext(); )
								{
										MPRecord rec = (MPRecord) i.next();
										if(rec.m_academic_plan != null) 
											program = rec.m_academic_plan.substring(0,4);
								}
								if(program != null)
								{
									//set student's program
									infoEdit.setProgram(getProgram(program));
									infoEdit.setParentSite("diss" +  getProgram(program));
								}
								else
								{
									//TODO note exception
									
									//continue with next student
									continue;
								}
									
							}//else if
						
						}//if we didn't get program
							
						//set flag to save this edit later
						commitEdit = true;
						
					}//check for student's Field of Study
					catch(Exception e)
					{
						msg = "// CHECK FOR PROGRAM " + oneEmplid + NEWLINE + e.getMessage();
						bufQ.append(msg + NEWLINE);
						m_logger.warn(this + ".queryLists() " + msg);
						
						//keep going
						continue;
					}
					
					//set CandidateInfo properties used later
					if(existsMPrecs)
					{
						//we have MPathways data for this student
						infoEdit.setMPRecInExtract(true);
						for(ListIterator i = MPrecs.listIterator(); i.hasNext(); )
						{
							MPRecord rec = (MPRecord) i.next();
						
							//	ANTICIPATE
							infoEdit.setAdvCandDesc(rec.m_anticipate);
						
							//	DATE_COMPL
							if(rec.m_milestone.equalsIgnoreCase("prelim"))
							{
								infoEdit.setPrelimTimeMilestoneCompleted(parseTimeString(rec.m_date_compl));
								infoEdit.setPrelimMilestone(rec.m_milestone);
							}
							else if(rec.m_milestone.equalsIgnoreCase("advcand"))
							{
								infoEdit.setAdvcandTimeMilestoneCompleted(parseTimeString(rec.m_date_compl));
								infoEdit.setAdvcandMilestone(rec.m_milestone);
							}
							else if(rec.m_milestone.equalsIgnoreCase("dissert"))
							{
								//	EVAL_DATE
								if(!rec.m_eval_date.equals(""))
									infoEdit.addTimeCommitteeEval(parseTimeString(rec.m_eval_date));
								
								//Add Committee Members and their Role and Evaluation Dates
								if(rec.m_member != null && !rec.m_member.equals(""))
								{
									//reverse name parts for display
									String[] parts = rec.m_member.split(",");
									name = parts[1] + " " + parts[0];
										
									//if report has been received, add the date received
									if(rec.m_eval_date != null && !rec.m_eval_date.equals(""))
									{
										//format date
										Time memberDate = parseTimeString(rec.m_eval_date);
										String memberEvalDate = memberDate.toStringLocalDate();
										committeeEvalCompleted = name + ", " + rec.m_role + ", received on " + memberEvalDate;
									}
									else
									{
										committeeEvalCompleted = name + ", " + rec.m_role;
									}
									
									//sorted according to the key's natural order
									membersEvals.put(rec.m_member, committeeEvalCompleted);
									
								}//if(rec.m_member != null && !rec.m_member.equals(""))
								committeeEvalCompleted = "";
			
								//if there are committee evaluations
								if(!membersEvals.isEmpty())
								{
									requiredFrom = membersEvals.values();
									infoEdit.addCommitteeEvalsCompleted(requiredFrom);
										
									//clear for next infoEdit
									membersEvals.clear();
								}
								
							}//else if(rec.m_milestone.equalsIgnoreCase("dissert"))
							
						}//for(ListIterator i = MPrecs.listIterator(); i.hasNext(); )
						
					}//if(existsMPrecs)
					
					if(existsOARDrecs)
					{
						//we have OARD data for this student
						infoEdit.setOARDRecInExtract(true);
						for(ListIterator i = OARDrecs.listIterator(); i.hasNext(); )
						{
							OARDRecord rec = (OARDRecord) i.next();
							
							/** set object data based on upload data */
						
							//	DEGREETERM_TRANS
							infoEdit.setDegreeTermTrans(rec.m_degreeterm_trans);
						
							//	ORAL_EXAM_DATE
							infoEdit.setTimeOralExam(parseTimeString(rec.m_oral_exam_date));
						
							//	ORAL_EXAM_TIME
							infoEdit.setOralExamTime(rec.m_oral_exam_time);
						
							//	ORAL_EXAM_PLACE
							infoEdit.setOralExamPlace(rec.m_oral_exam_place);
						
							//	COMMITTE_APPROVED_DATE
							infoEdit.setTimeCommitteeApproval(parseTimeString(rec.m_committee_approved_date));
						
							//	FIRST_FORMAT_DATE
							infoEdit.setTimeFirstFormat(parseTimeString(rec.m_first_format_date));
						
							//	ORAL_REPORT_RETURN_DATE
							infoEdit.setTimeOralReportReturned(parseTimeString(rec.m_oral_report_return_date));
						
							//	COMM_CERT_DATE
							infoEdit.setTimeCommitteeCert(parseTimeString(rec.m_comm_cert_date));
							
						}//for(ListIterator i = OARDrecs.listIterator(); i.hasNext(); )
						
					}//if(existsOARDrecs)
					
				}//try to get CandidateInfo for this student
				catch(Exception e)
				{
					msg = "// GET CANDIDATE INFO FOR ID " + oneEmplid + NEWLINE + e.getMessage();
					bufQ.append(msg + NEWLINE);
					m_logger.warn(this + ".queryLists() " + msg);
					
					if(infoEdit != null && infoEdit.isActiveEdit())
						DissertationService.cancelEdit(infoEdit);
					
					//keep going with next student
					continue;
				}
				
				//commit change if new data
				if(infoEdit != null && infoEdit.isActiveEdit())
				{
					if(commitEdit)
						DissertationService.commitEdit(infoEdit);
					else
						DissertationService.cancelEdit(infoEdit);
				}
				
				/** not all infoEdit properties persistent 
		 		*  so pass local instance rather than from db */
		
				//add this CandidateInfo to the collection
				data.add(infoEdit);
				
			}//while(emplids.hasMoreElements())
			
			//on to apply business logic and data values to set step status
			msg = dumpData(new String(bufQ.toString()), data);
			
		}//if(emplids != null)

		//return a String of messages suitable for HTML display
		return msg;
			
	} //queryLists
	
	/** 
	* Send in a load of data from Rackham upload.
	* @param data Vector of CandidateInfo objects.
	*/
	public String dumpData(String errors, Vector data)
	{
		CandidateInfoEdit info = null;
		CandidatePath path = null;
		CandidatePathEdit pathEdit = null;
		Hashtable orderedStatus = null;
		Collection statusRefs = null;
		String statusRef = null;
		String autoValidationId = null;
		String oralExamText = null;
		String degreeTerm = null;
		String newDegreeTerm = null;
		int autoValidNumber = 0;
		StepStatus status = null;
		StepStatusEdit statusEdit = null;
		Time completionTime = null;
		StringBuffer bufD = new StringBuffer();
		Vector memberRole = new Vector();
		
		//if there were errors creating CandidateInfos, note that
		if(errors != null && !errors.equals(""))
			bufD.append(errors);

		//for each CandidateInfo in data
		for(int x = 0; x < data.size(); x++)
		{
			try
			{
				/** infoEdit was committed or cancelled in queryLists
				 *  so no need to commit or cancel here */
				
				info = (CandidateInfoEdit)data.get(x);
				
				//get the candidate path for this student
				path = DissertationService.getCandidatePathForCandidate(info.getChefId());
				if(path == null)
				{
					//if there is no path, create one
					
					/** there is no student site id yet or we don't know it
					 *  site attribute is set to uniqname initially */
					
					String currentSite = info.getChefId();
					String parentSite = info.getParentSite();
					Dissertation dissertation = DissertationService.getDissertationForSite(parentSite);
					if(dissertation == null)
					{
						//if there is no dissertation for the student's parent site, use school's
						
						//set the dissertation type
						String schoolSite = DissertationService.getSchoolSite();
						if(parentSite.equals(m_musicPerformanceSite))
							dissertation = DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
						else
							dissertation = DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
						if(dissertation == null)
						{
							m_logger.warn(this + ". dumpData dissertation for school is null");
							bufD.append(info.getChefId() + ": cannot create student's path, because dissertation for school is null" + NEWLINE);
							
							//note execption and continue with next student
							continue;
						}
					}
					try
					{
						//TODO %%% get through Dissertation Service get a new path based on this dissertation
						//pathEdit = addCandidatePathFromListener(dissertation, currentSite);
						pathEdit = DissertationService.addCandidatePath(dissertation, currentSite);
						if(pathEdit == null)
						{
							m_logger.warn(this + ". dumpData pathEdit from addCandidatePath is null");
							bufD.append(info.getChefId() + ": cannot create CandidatePath, because path from addCandidatePath was null" + NEWLINE);
							
							//continue with next student
							continue;
						}
						
						//set attributes of the path
						pathEdit.setCandidate(info.getChefId());
						
						//set alphabetical candidate chooser letter
						pathEdit.setSortLetter(getSortLetter(info.getChefId()));
						
						//if sortletter can't be set, alphabetical candidate chooser
						//won't show this student under a letter
						if(pathEdit.getSortLetter().equals("") || !((String)pathEdit.getSortLetter()).matches("[A-Z]"))
						{							
							bufD.append(info.getChefId() + ": problem setting student's alphabetical letter, path was not created" + NEWLINE);
							
							//clean up by removing step statuses
							statusRefs = pathEdit.getOrderedStatus().values();
							Iterator i = statusRefs.iterator();
							while(i.hasNext())
							{
								statusRef = (String)i.next();
								statusEdit = DissertationService.editStepStatus(statusRef);
								DissertationService.removeStepStatus(statusEdit);
							}
							statusRefs = null;
							statusEdit = null;
							statusRef = null;
							
							//clean up by removing the path
							DissertationService.removeCandidatePath(pathEdit);
							try
							{
								CandidateInfoEdit ci = DissertationService.getCandidateInfoEditForEmplid(info.getEmplid());
								
								//remove candidate info
								DissertationService.removeCandidateInfo(ci);
							}
							catch(Exception e)
							{
								bufD.append(info.getChefId() + ": problem removing corresponding CandidateInfo - " + NEWLINE + e.toString() + NEWLINE);
							}
							
							//continue with the next student
							continue;
						}
						
						//set parent department site id
						pathEdit.setParentSite(info.getParentSite());
						if(!(pathEdit.getParentSite().matches("^diss[0-9]*$")))
						{
							bufD.append(info.getChefId() + ": problem setting student's parent site" + NEWLINE);
						}
						
						//set dissertation steps type
						if(parentSite.equals(m_musicPerformanceSite))
							pathEdit.setType(DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
						else
							pathEdit.setType(DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
					}
					catch(Exception e)
					{
						bufD.append(info.getChefId() + ": exception setting initial CandidatePath attributes: " + NEWLINE + e.toString() + NEWLINE);
						if(pathEdit != null && pathEdit.isActiveEdit())
							DissertationService.cancelEdit(pathEdit);
						
						//continue with next student
						continue;	
					}
					finally
					{
						//save path
						if(pathEdit != null && pathEdit.isActiveEdit())
							DissertationService.commitEdit(pathEdit);
					}
					
				}//created path for student
				
				//get path for student
				path = DissertationService.getCandidatePathForCandidate(info.getChefId());
				if(path == null)
				{
					m_logger.warn(this + ".dumpData path from getCandidatePathForCandidate for " + info.getChefId() + " is null");
					bufD.append(info.getChefId() + ": the path from getCandidatePathForCandidate was null" + NEWLINE); 
					
					//continue with next student
					continue;
				}
				
				/** Set the status of the steps in the path
				 *  for those steps that have auto-validation numbers.
				 *  See
				 *  CandidateInfo.getExternalValidation(autoValidNumber)
				 *  for business logic. */
				
				//get the ordered step statuses
				orderedStatus = path.getOrderedStatus();
				
				//for each ordered step status
				for(int y = 1; y < (orderedStatus.size()+1); y++)
				{
					//try to get the auto-validation number
					statusRef = (String)orderedStatus.get("" + y);
					//statusId = statusId(statusRef);
					//status = m_statusStorage.get(statusId);
					status = DissertationService.getStepStatus(statusRef);
					autoValidationId = status.getAutoValidationId();
					
					//if there is a used auto-validation number
					if((!"".equals(autoValidationId)) && (!"None".equals(autoValidationId))
							&& (!"9".equals(autoValidationId)) && (!"10".equals(autoValidationId)) 
							&& (!"12".equals(autoValidationId)))
					{
						try
						{
							//get a status edit
							//statusEdit = m_statusStorage.edit(statusId);
							statusEdit = DissertationService.editStepStatus(statusRef);
							
							//get an integer
							autoValidNumber = Integer.parseInt(autoValidationId);
							
							//remove prior auxiliary text so it can be changed
							if(statusEdit.getAuxiliaryText() != null)
								statusEdit.setAuxiliaryText(null);
							
							/** display committee member names/roles with step
							 *  "Approve dissertation committee"
							 */
							if(autoValidNumber == 3 && ((Vector)info.getCommitteeEvalsCompleted()).size() > 0)
							{
								memberRole.clear();
								memberRole.add(START_ITALIC + "Committee:" + END_ITALIC);
								for(int i = 0; i < info.getCommitteeEvalsCompleted().size(); i++)
								{
									String temp = (String)info.getCommitteeEvalsCompleted().get(i);
									if(temp.indexOf(", received on ")!= -1)
										temp = temp.substring(0,temp.indexOf(", received on "));
									memberRole.add(temp);
								}
								statusEdit.setAuxiliaryText(memberRole);
							}
							
							/** display committee member names/dates with step
							 *  "Submit completed evaluation forms to Rackham three days before defense"
							 *  previously was with "Return Final Oral Examination report to Rackham" */
							
							//set auxiliary text to appear with step
							if(autoValidNumber == 6 && ((Vector)info.getCommitteeEvalsCompleted()).size() > 0)
							{
								memberRole.clear();
								memberRole.add(START_ITALIC + "Evaluations required from:"+ END_ITALIC);
								memberRole.addAll(info.getCommitteeEvalsCompleted());
								
								//statusEdit.setAuxiliaryText(info.getCommitteeEvalsCompleted());
								statusEdit.setAuxiliaryText(memberRole);
							}
							
							completionTime = info.getExternalValidation(autoValidNumber);
							if(completionTime != null)
							{
								//We want autovalidation #9 to set TimeCompleted to new Time() once
								//if(autoValidNumber != 9)
								//{
									//statusEdit = m_statusStorage.edit(statusId);	
									statusEdit.setCompleted(true);
									statusEdit.setTimeCompleted(completionTime);
									
									//We want to display Advanced to Candidacy Term (e.g., 'Fall 2003') 
									//with time 'Approve advance to candidacy' completed
									if(autoValidNumber == 2)
									{
										degreeTerm = info.getAdvCandDesc();
										
										//translation of term for readability
										if(degreeTerm != null && !degreeTerm.equals(""))
										{
											if(degreeTerm.startsWith("FA"))
												newDegreeTerm = degreeTerm.replaceFirst("FA", "Fall ");
											else if(degreeTerm.startsWith("WN"))
												newDegreeTerm = degreeTerm.replaceFirst("WN", "Winter ");
											else if(degreeTerm.startsWith("SP"))
												newDegreeTerm = degreeTerm.replaceFirst("SP", "Spring ");
											else if(degreeTerm.startsWith("SU"))
												newDegreeTerm = degreeTerm.replaceFirst("SU", "Summer ");
											else if(degreeTerm.startsWith("SS"))
												newDegreeTerm = degreeTerm.replaceFirst("SS", "Spring-Summer ");
											if(newDegreeTerm != null)
												degreeTerm = newDegreeTerm;
											statusEdit.setTimeCompletedText(degreeTerm);
										}
									}//autoValidation == 2
									
									//We want to display Oral Exam Date, Oral Exam Time, Oral Exam Place 
									//with first format time 'Complete Rackham pre-defense meeting' completed
									if(autoValidNumber == 4)
									{
										oralExamText = info.getOralExamTime().toString() + " " + info.getOralExamPlace();
										statusEdit.setTimeCompletedText(oralExamText);
									}
									
									//We want to display student's Degree Term with 
									//date of approval of degree conferral
									if(autoValidNumber == 11)
									{
										//more editing for readability
										degreeTerm = info.getDegreeTermTrans();
										if(degreeTerm != null && !degreeTerm.equals(""))
										{
											if(degreeTerm.startsWith("FA-"))
												newDegreeTerm = degreeTerm.replaceFirst("FA-", "Fall ");
											else if(degreeTerm.startsWith("WN-"))
												newDegreeTerm = degreeTerm.replaceFirst("WN-", "Winter ");
											else if(degreeTerm.startsWith("SP-"))
												newDegreeTerm = degreeTerm.replaceFirst("SP-", "Spring ");
											else if(degreeTerm.startsWith("SU-"))
												newDegreeTerm = degreeTerm.replaceFirst("SU-", "Summer ");
											else if(degreeTerm.startsWith("SS-"))
												newDegreeTerm = degreeTerm.replaceFirst("SS-", "Spring-Summer ");
											if(newDegreeTerm != null)
												degreeTerm = newDegreeTerm;
											statusEdit.setTimeCompletedText(degreeTerm);
										}
									}//autoValidation = 11
									/*
								}//autoValidation != 9
								else
								{
									if(status.getTimeCompleted()==null || status.getTimeCompleted().equals(""))
									{
										statusEdit.setCompleted(true);
										statusEdit.setTimeCompleted(completionTime);
									}
								}//autoValidation == 9
								*/
							}//completion time not null
							else
							{
								/** Completion time is null
								 * if null is because there is a record with 
								 * date set to null, set status to null
								 * if null is because the record is missing, 
								 * do not change the existing status */
								
								if((((autoValidNumber == 1) || (autoValidNumber == 2)) && info.getMPRecInExtract()) ||
								((autoValidNumber != 1) && (autoValidNumber != 2) && info.getOARDRecInExtract()))
								{
									statusEdit.setCompleted(false);
									statusEdit.setTimeCompleted(completionTime);
								}
							}
							
							//save the changes to status
							DissertationService.commitEdit(statusEdit);
						}
						catch(Exception nfe)
						{
							//note the exception
							m_logger.warn("DISSERTATION : BASE SERVICE : DUMP DATA : EXCEPTION PROCESSING AUTOVALID NUMBER : " + autoValidationId + " " + nfe.toString());
							bufD.append(info.getChefId() + ": exception processing step with autovalidation number " + autoValidationId + ": " + NEWLINE + nfe.toString() + NEWLINE);
							
							if(statusEdit != null && statusEdit.isActiveEdit())
								DissertationService.cancelEdit(statusEdit);
						}
					}//there is an auto-validation number
				}//for each ordered step status
			}
			catch(Exception e)
			{
				m_logger.warn("DISSERTATION : BASE SERVICE : DUMP DATA : EXCEPTION STORING DATA FOR : " + info.getChefId() + ": " + e.toString());
				bufD.append(info.getChefId() + ": exception " + NEWLINE + e.toString() + NEWLINE);
				
				if(pathEdit != null && pathEdit.isActiveEdit())
					DissertationService.commitEdit(pathEdit);
				
				//keep going with next student
				continue;
			}
		}//for each CandidateInfo
		
		return new String(bufD.toString());
		
	}//dumpData
	
	/*******************************************************************************
	* OARDRecord implementation
	*******************************************************************************/	
	/**
	*
	* Contains a Rackham OARD data record
	* 
	*/
	private class OARDRecord
	{
		/**
		* corresponding to OARD db output fields
		* All dates are formatted as mm/dd/ccyy.  
		* 
		* OARDEXT.txt Data Structure:
		* STRUCT ¦ Field Name            ¦Field Type
		* 1 ¦  Emplid                    ¦  A8 - Student's emplid
		* 2 ¦  Fos                       ¦  A4    - Students field of study code
		* 3 ¦  Lname                     ¦  A25 - Students last name
		* 4 ¦  Fname                     ¦  A30 - Students first name
		* 5 ¦  Degterm trans             ¦  A7 - Students degree term as TT-CCYY (e.g. FA-2003)
		* 6 ¦  Oral exam date            ¦  D - Date of oral defense
		* 7 ¦  Oral exam time            ¦  A7 - Time of oral defense
		* 8 ¦  Oral exam place           ¦  A25 - Place of oral defense
		* 9 ¦  Committee approved date   ¦  D - date committee was approved
		*10 ¦  First format date         ¦  D - date of pre defense meeting in Rackham
		*11 ¦  Oral report return date   ¦  D
		*12 ¦  Degree conferred date     ¦  D - date the degree was conferred in OARD system
		*13 ¦  Update date               ¦  D - date record was last modified
		*14 ¦  Comm cert date            ¦  D -
		*15 |  Campus id                 |  A1-A8 - student's uniqname (Chef id)
		*/
		public String m_umid = null;
		public String m_fos = null;
		public String m_lname = null;
		public String m_fname = null;
		public String m_degreeterm_trans = null;
		public String m_oral_exam_date = null;
		public String m_oral_exam_time = null;
		public String m_oral_exam_place = null;
		public String m_committee_approved_date = null;
		public String m_first_format_date = null;
		//public String m_binder_receipt_date = null;
		//public String m_fee_requirement_met = null;
		//public String m_fee_date_receipt_seen = null;
		//public String m_pub_fee_date_received = null;
		public String m_oral_report_return_date = null;
		//public String m_unbound_date = null;
		//public String m_abstract_date = null;
		//public String m_bound_copy_received_date = null;
		//public String m_diploma_application_date = null;
		//public String m_contract_received_date = null;
		//public String m_nsf_survey_date = null;
		public String m_degree_conferred_date = null;
		//public String m_final_format_recorder = null;
		public String m_update_date = null;
		public String m_comm_cert_date = null;
		public String m_role = null;
		public String m_member = null;
		public String m_eval_date = null;
		public String m_campus_id = null;
		
		//methods
		public String getUmid(){ return m_umid; }
		public String getFos(){ return m_fos; }
		public String getLname(){ return m_lname; }
		public String getFname(){ return m_fname; }
		public String getDegreeterm_trans(){ return m_degreeterm_trans; }
		public String getOral_exam_date(){ return m_oral_exam_date; }
		public String getOral_exam_time(){ return m_oral_exam_time; }
		public String getOral_exam_place(){ return m_oral_exam_place; }
		public String getCommittee_approved_date(){ return m_committee_approved_date; }
		public String getFirst_format_date(){ return m_first_format_date; }
		//public String getBinder_receipt_date(){ return m_binder_receipt_date; }
		//public String getFee_requirement_met(){ return m_fee_requirement_met; }
		//public String getFee_date_receipt_seen(){ return m_fee_date_receipt_seen; }
		//public String getPub_fee_date_received(){ return m_pub_fee_date_received; }
		public String getOral_report_return_date(){ return m_oral_report_return_date; }
		//public String getUnbound_date(){ return m_unbound_date; }
		//public String getAbstract_date(){ return m_abstract_date; }
		//public String getBound_copy_received_date(){ return m_bound_copy_received_date; }
		//public String getDiploma_application_date(){ return m_diploma_application_date; }
		//public String getContract_received_date(){ return m_contract_received_date; }
		//public String getNsf_survey_date(){ return m_nsf_survey_date; }
		public String getDegree_conferred_date(){ return m_degree_conferred_date; }
		//public String getFinal_format_recorder(){ return m_final_format_recorder; }
		public String getUpdate_date(){ return m_update_date; }
		public String getComm_cert_date(){ return m_comm_cert_date; }
		public String getRole(){ return m_role; }
		public String getMember(){ return m_member; }
		public String getEval_date() { return m_eval_date; }
		public String getCampusId() { return m_campus_id; }
		
	} // OARDRecord
	

	
	/*******************************************************************************
	* MPRecord implementation
	*******************************************************************************/	
	/**
	*
	* Contains a Rackham MPathways data record
	* 
	*/
	private class MPRecord
	{
		/**
		* All dates are formatted as mm/dd/ccyy.  
		* MPEXT.txt Data Structure:
		* 1 ¦  Emplid					¦  A9	| Student's emplid
		* 2 ¦  Acad_prog				¦  A9	| Academic Program Code
		* 3 ¦  Anticipate_Anticipate_1	¦  A15	| Adv to cand term code
		* 4	¦  Date_compl				¦  D	| Date milestone was completed 
		* 5 ¦  Milestone				¦  A10	| name of milestone PRELIM or ADVCAND
		* 6 |  Academic plan			|  A4	| Field of study and degree (e.g. 1220PHD1)
		* 7 |  							|  A24  | Committee role
		* 8 ¦ 							¦  A23	| Member name
		* 9 | 							|  A14	| Eval received date
		*10 | Campus id					|  A1-A8| Student's uniqname (Chef id)
		*/
		private String m_umid = null;
		private String m_acad_prog = null;
		private String m_anticipate = null;
		private String m_date_compl = null;
		private String m_milestone = null;
		private String m_academic_plan = null;
		private String m_role = null;
		private String m_member = null;
		private String m_eval_date = null;
		private String m_campus_id = null;
		
		//methods
		protected String getUmid(){ return m_umid; }
		protected String getAcad_prog(){ return m_acad_prog; }
		protected String getAnticipate(){ return m_anticipate; }
		protected String getDate_compl(){ return m_date_compl; }
		protected String getMilestone(){ return m_milestone; }
		protected String getAcademicPlan() { return m_academic_plan; }
		protected String getCommitteeRole() {return m_role; }
		protected String getCommitteeMember() {return m_member; }
		protected String getEvalDate() {return m_eval_date; }
		protected String getCampusId() { return m_campus_id; }
		
	}//MPRecord
	
	/** 
	* Access the Rackham program id (Block Grant Group or BGG) 
	* for Rackham field of study id (FOS).
	* @return The program id.
	*/
	public String getProgram(String fos)
	{
		String retVal = "";
		if(fos != null)
			retVal = (String)m_schoolGroups.get(fos);
		return retVal;
	}
	
	/* (non-Javadoc)
	 * @see org.sakaiproject.component.app.dissertation.StepChangeJob#init()
	 */
	public void init() 
	{
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.sakaiproject.component.app.dissertation.StepChangeJob#destroy()
	 */
	public void destroy() 
	{
		// TODO Auto-generated method stub
		
	}
}
