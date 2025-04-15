package com.mozdeth.kanjikards;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
//import android.util.Log;

public class TouchDetector
{
	public interface TouchDetectionListener
	{
		public void onSwipeRightToLeft();
		public void onSwipeLeftToRight();
		public void onSwipeUp();
		public void onSwipeDown();
		public void onSingleTap();
		public void onLongTap();
	}
	
	private TouchDetectionListener m_listener = null;
	private int PIXEL_THRESHOLD = 0;
	private float VELOCITY_THRESHOLD = 0;
	
	private float m_startX = -1.0f;
	private float m_startY = -1.0f;
	private long m_startTime = -1;
	private boolean m_detectionStarted = false;
	
    //*********************************************************************************************
	
	public TouchDetector(TouchDetectionListener listener, Context baseContext)
	{
		m_listener = listener;
		
		// get the amount for swipe/fling gestures which accounts for the phone's pixel density
		// HTC One V:
		//           24 pixels for scaled touch slop
		//           48 pixels for scaled paging touch slop
		//           75 pixels per second for scaled minimum fling velocity
		// Moto G:
		//           32 pixels for scaled touch slop
		//           64 pixels for scaled paging touch slop
		//           100 pixels per second for scaled minimum fling velocity
		PIXEL_THRESHOLD = ViewConfiguration.get(baseContext).getScaledTouchSlop();
		//PIXEL_THRESHOLD = ViewConfiguration.get(baseContext).getScaledPagingTouchSlop();
		VELOCITY_THRESHOLD = ViewConfiguration.get(baseContext).getScaledMinimumFlingVelocity();
		
		//Log.v("TouchDetector", "PIXEL_THRESHOLD = " + PIXEL_THRESHOLD);
		//Log.v("TouchDetector", "VELOCITY_THRESHOLD = " + VELOCITY_THRESHOLD);

		//Log.v("TouchDetector", "getScaledTouchSlop = " + ViewConfiguration.get(baseContext).getScaledTouchSlop());
		//Log.v("TouchDetector", "getScaledPagingTouchSlop = " + ViewConfiguration.get(baseContext).getScaledPagingTouchSlop());
		//Log.v("TouchDetector", "getScaledMinimumFlingVelocity = " + ViewConfiguration.get(baseContext).getScaledMinimumFlingVelocity());
	}
	
	//*********************************************************************************************
	
	public boolean handleNewMotionEvent(MotionEvent event)
	{
		if (m_listener == null)
		{
			return false;
		}
		
		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{
			m_startX = event.getX();
			m_startY = event.getY();
			m_startTime = System.currentTimeMillis();
			m_detectionStarted = true;
			return true;
		}
		else if (event.getAction() == MotionEvent.ACTION_UP)
		{
			float endX = event.getX();
			float endY = event.getY();
			float absDiffX = Math.abs(endX - m_startX);
			float absDiffY = Math.abs(endY - m_startY);
			float timeDiffSecs = (System.currentTimeMillis() - m_startTime) / 1000.0f;
			if (timeDiffSecs == 0.0f)
			{
				timeDiffSecs = 1.0f; // ensure there is no divide by 0 error
			}
			float velocityX = absDiffX / timeDiffSecs;
			float velocityY = absDiffY / timeDiffSecs;
			//Log.v("TouchDetector", "timeDiffSecs = " + timeDiffSecs);
			//Log.v("TouchDetector", "X Start = " + m_startX + " End = " + endX + " Diff = " + absDiffX);
			//Log.v("TouchDetector", "Y Start = " + m_startY + " End = " + endY + " Diff = " + absDiffY);
			//Log.v("TouchDetector", "current velocity x,y = " + velocityX + "," + velocityY);

			if ((absDiffX >= PIXEL_THRESHOLD && velocityX > VELOCITY_THRESHOLD) && (absDiffX > absDiffY) && m_detectionStarted)
			{
				if (m_startX > endX) // user swiped from right to left
				{
					m_listener.onSwipeRightToLeft();
				}
				else // user swiped from left to right
				{
					m_listener.onSwipeLeftToRight();
				}
			}
			else if ((absDiffY >= PIXEL_THRESHOLD && velocityY > VELOCITY_THRESHOLD) && m_detectionStarted)
			{
				if (m_startY > endY) // user swiped up
				{
					m_listener.onSwipeUp();
				}
				else // user swiped down
				{
					m_listener.onSwipeDown();
				}
			}
			else
			{
				if (m_detectionStarted)
				{
					if (timeDiffSecs < 0.55)
					{
						m_listener.onSingleTap();
					}
					else
					{
						m_listener.onLongTap();
					}
				}
			}
			m_detectionStarted = false;
			m_startTime = -1;
			return true; // we are consuming the event
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE)
		{
			if (m_detectionStarted)
			{
				float timeDiffSecs = (System.currentTimeMillis() - m_startTime) / 1000.0f;
				if (timeDiffSecs >= 0.55 && timeDiffSecs < 1.5)
				{
					float endX = event.getX();
					float endY = event.getY();
					float absDiffX = Math.abs(endX - m_startX);
					float absDiffY = Math.abs(endY - m_startY);
					
					if (absDiffX < PIXEL_THRESHOLD && absDiffY < PIXEL_THRESHOLD)
					{
						m_startTime = -1;
						m_detectionStarted = false;
						m_listener.onLongTap();
						return true;
					}
				}
			}
		}
		
		return false; // we did not consume the event
	}
}
