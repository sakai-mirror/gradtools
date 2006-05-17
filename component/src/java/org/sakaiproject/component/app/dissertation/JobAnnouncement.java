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

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionException;
import org.sakaiproject.api.kernel.session.Session;
import org.sakaiproject.api.kernel.session.cover.SessionManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.service.framework.log.Logger;
import org.sakaiproject.service.legacy.announcement.AnnouncementChannel;
import org.sakaiproject.service.legacy.announcement.AnnouncementChannelEdit;
import org.sakaiproject.service.legacy.announcement.AnnouncementMessageEdit;
import org.sakaiproject.service.legacy.announcement.AnnouncementMessageHeaderEdit;
import org.sakaiproject.service.legacy.announcement.cover.AnnouncementService;
import org.sakaiproject.service.legacy.entity.Entity;
import org.sakaiproject.service.legacy.notification.cover.NotificationService;
import org.sakaiproject.service.legacy.site.cover.SiteService;

/**
 * <p>
 * Utility class to add a Message to the main AnnouncementChannel.
 * </p>
 * 
 * @author University of Michigan, Sakai Software Development Team
 * @version $Revision: $
 */
public class JobAnnouncement
{
	private Logger m_logger = null;
	private JobDetail m_jobDetail = null;
	private String m_message = null;
		
	public JobAnnouncement(JobDetail jobDetail)
	{
		m_jobDetail = jobDetail;
	}
		
	protected void addAnnouncementMessage(String message) throws JobExecutionException
	{
		if(message == null || message.equals(""))
			return;
		else
			this.m_message = message;
			
		AnnouncementChannel channel = null;
		AnnouncementMessageHeaderEdit header = null;
		AnnouncementMessageEdit msg = null;
		String jobName = null;
		String site = null;
		try
		{
			//add an announcement message to the main channel of the current site

			//get job detail
			jobName = m_jobDetail.getName();
			JobDataMap dataMap = m_jobDetail.getJobDataMap();
				
			//get the announcement context
			site = (String)dataMap.get("CURRENT_SITE");
				
			//make sure we have all the parameters we need
			if(jobName == null || site == null)
				throw new JobExecutionException("A required parameters was null.");
	
			//get the main channel reference
			String channelRef = AnnouncementService.REFERENCE_ROOT + Entity.SEPARATOR +
				AnnouncementService.REF_TYPE_CHANNEL + Entity.SEPARATOR + 
				site + Entity.SEPARATOR + SiteService.MAIN_CONTAINER;
			try
			{
				if(AnnouncementService.allowGetChannel(channelRef))
				{
					//add a message to the main channel
					channel = AnnouncementService.getAnnouncementChannel(channelRef);
				}
				else
				{
					m_logger.warn(this + ".addAnnouncementMessage() allowGetChannel(" + 
							channelRef + ") no permission");
				}
			}
			catch(IdUnusedException e)
			{
				if(AnnouncementService.allowAddChannel(channelRef))
				{
					try
					{
						AnnouncementChannelEdit edit = AnnouncementService.addAnnouncementChannel(channelRef);
						AnnouncementService.commitChannel(edit);
						channel = edit;
					}
					catch(Exception error)
					{
						m_logger.warn(this + ".addAnnouncementMessage() addAnnouncementChannel(" + channelRef + ") " + error);
					}
				}
			}
			if(channel == null)
			{
				m_logger.warn(this + ".addAnnouncementMessage: channel is null");
			}
			else
			{
				msg = (AnnouncementMessageEdit)channel.addMessage();
				msg.setBody(m_message);
				header = (AnnouncementMessageHeaderEdit)msg.getHeaderEdit();
				header.setSubject("JOB NAME: " + jobName);
				
				//set as high priority so email notification is certain
				int noti = NotificationService.NOTI_REQUIRED;
				channel.commitMessage(msg, noti);
			}
		}
		catch(Exception e)
		{
			m_logger.warn(this + ".addAnnouncementMessage() add an announcement message to the main channel " + e);
		}
	}//addAnnouncementMessage

	/* (non-Javadoc)
	* @see org.sakaiproject.component.app.dissertation.AnnouncementJob#init()
	*/
	public void init() 
	{
		/* (non-Javadoc)
		 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
		 */
			try
			{
				//Spring injection of Logger was getting lost when Quartz instantiated job
				m_logger = org.sakaiproject.service.framework.log.cover.Logger.getInstance();
				if(m_logger == null)
					System.out.println("logger is null");
					
				// set the current user to admin
				Session s = SessionManager.getCurrentSession();
				if (s != null)
				{
					//TODO set user to current user
					s.setUserId("admin");
				}
				else
				{
					m_logger.warn(this + ".execute() could not setUserId to admin");
						throw new JobExecutionException("Could not get permission to execute job.");
				}
			}
			catch(Exception e)
			{
				m_logger.warn(this + ".execute() " + e.toString());
			}
		}

		/* (non-Javadoc)
		 * @see org.sakaiproject.component.app.dissertation.AnnouncementJob#destroy()
		 */
		public void destroy() 
		{
			// TODO Auto-generated method stub
		}

}//JobAnnouncement
